package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.OutageExtreme;
import me.eeshe.celeoproc.model.OutageStats;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageStats;
import me.eeshe.celeoproc.model.UserOutageStats;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.NicknameResolver;
import me.eeshe.celeoproc.service.PowerOutageLogService;

public final class PowerOutageLogServiceImpl implements PowerOutageLogService {
    private static final Comparator<UserOutageStats> USER_STATS_ORDER = Comparator
            .comparing(UserOutageStats::totalTime, Comparator.reverseOrder())
            .thenComparing(UserOutageStats::logAmount, Comparator.reverseOrder())
            .thenComparing(UserOutageStats::nickname);

    private final PowerOutageLogRepository powerOutageLogRepository;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final NicknameResolver nicknameResolver;
    private final AppSettings appSettings;

    public PowerOutageLogServiceImpl(
            final PowerOutageLogRepository powerOutageLogRepository,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final NicknameResolver nicknameResolver,
            final AppSettings appSettings) {
        this.powerOutageLogRepository = Objects.requireNonNull(powerOutageLogRepository,
                "PowerOutageLogRepository must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.nicknameResolver = Objects.requireNonNull(nicknameResolver, "NicknameResolver must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
    }

    @Override
    public PowerOutageLog logPowerOutage(
            final long userId,
            final Instant electricityOut,
            final Instant electricityIn) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(electricityOut, "Electricity out must not be null");
        Objects.requireNonNull(electricityIn, "Electricity in must not be null");

        final PowerOutageLog powerOutageLog = new PowerOutageLog(
                UUID.randomUUID(),
                userId,
                electricityOut,
                electricityIn);
        powerOutageLogRepository.save(powerOutageLog);

        return powerOutageLog;
    }

    @Override
    public PowerOutageStats getWithinRange(final long guildId, final LocalDate rangeStart, final LocalDate rangeEnd) {
        Objects.requireNonNull(rangeStart, "Range start must not be null");
        Objects.requireNonNull(rangeEnd, "Range end must not be null");

        final ZoneId zone = appSettings.getTimezone();
        final Instant startInstant = rangeStart.atStartOfDay(zone).toInstant();
        final Instant endInstant = rangeEnd.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        final List<PowerOutageLog> globalLogs = powerOutageLogRepository.getWithinRange(startInstant, endInstant);
        final Set<Long> participantUserIds = collectParticipantUserIds(guildId);
        final List<PowerOutageLog> serverLogs = globalLogs.stream()
                .filter(log -> participantUserIds.contains(log.userId()))
                .toList();

        final OutageStats serverStats = aggregate(serverLogs, resolveNicknames(guildId, serverLogs), true);
        final OutageStats globalStats = aggregate(globalLogs, Map.of(), false);
        return new PowerOutageStats(serverStats, globalStats);
    }

    private Set<Long> collectParticipantUserIds(final long guildId) {
        final Set<Long> participantUserIds = new HashSet<>();
        for (final ElectricityStatusEmbed statusEmbed : electricityStatusEmbedService.getByGuildId(guildId)) {
            participantUserIds.addAll(statusEmbed.getParticipantUserIds());
        }
        return participantUserIds;
    }

    private Map<Long, String> resolveNicknames(final long guildId, final List<PowerOutageLog> logs) {
        final Map<Long, String> nicknames = new LinkedHashMap<>();
        for (final PowerOutageLog log : logs) {
            nicknames.computeIfAbsent(log.userId(), userId -> {
                final String nickname = nicknameResolver.resolveNickname(guildId, userId);
                return nickname == null || nickname.isBlank() ? String.valueOf(userId) : nickname;
            });
        }
        return nicknames;
    }

    private OutageStats aggregate(
            final List<PowerOutageLog> logs,
            final Map<Long, String> nicknames,
            final boolean perUser) {
        if (logs.isEmpty()) {
            return new OutageStats(0, Duration.ZERO, Duration.ZERO, null, null, null, List.of());
        }

        final LogSummary summary = summarizeLogs(logs);
        if (!perUser) {
            return new OutageStats(
                    summary.count(),
                    summary.totalTime(),
                    summary.averageTime(),
                    null,
                    null,
                    null,
                    List.of());
        }
        final List<UserOutageStats> userStats = aggregatePerUser(logs, nicknames);
        final OutageExtreme longestOutage = new OutageExtreme(computeLogNickname(summary.longest(), nicknames),
                computeLogDuration(summary.longest()));
        final OutageExtreme shortestOutage = new OutageExtreme(computeLogNickname(summary.shortest(), nicknames),
                computeLogDuration(summary.shortest()));
        return new OutageStats(summary.count(), summary.totalTime(), summary.averageTime(), longestOutage,
                shortestOutage,
                userStats.getFirst(), userStats);
    }

    private List<UserOutageStats> aggregatePerUser(
            final List<PowerOutageLog> logs,
            final Map<Long, String> nicknames) {
        final Map<Long, List<PowerOutageLog>> logsByUser = new LinkedHashMap<>();
        for (final PowerOutageLog log : logs) {
            logsByUser.computeIfAbsent(log.userId(), userId -> new ArrayList<>()).add(log);
        }
        final List<UserOutageStats> userStats = new ArrayList<>();
        for (final Map.Entry<Long, List<PowerOutageLog>> entry : logsByUser.entrySet()) {
            final long userId = entry.getKey();
            final List<PowerOutageLog> userLogs = entry.getValue();

            final LogSummary summary = summarizeLogs(userLogs);
            final Duration averageTime = summary.totalTime().dividedBy(summary.count());
            userStats.add(new UserOutageStats(
                    userId,
                    computeLogNickname(userLogs.getFirst(), nicknames),
                    summary.count(),
                    summary.totalTime(),
                    averageTime,
                    computeLogDuration(summary.longest()),
                    computeLogDuration(summary.shortest())));
        }

        userStats.sort(USER_STATS_ORDER);
        return userStats;
    }

    /**
     * Single-pass summary of a non-empty log list: total duration, amount of logs
     * and the longest/shortest logs. Ties keep the first encountered log.
     */
    private LogSummary summarizeLogs(final List<PowerOutageLog> logs) {
        Duration totalTime = Duration.ZERO;
        PowerOutageLog longest = logs.getFirst();
        PowerOutageLog shortest = logs.getFirst();
        Duration longestDuration = computeLogDuration(longest);
        Duration shortestDuration = longestDuration;
        for (final PowerOutageLog log : logs) {
            final Duration duration = computeLogDuration(log);
            totalTime = totalTime.plus(duration);
            if (duration.compareTo(longestDuration) > 0) {
                longest = log;
                longestDuration = duration;
            }
            if (duration.compareTo(shortestDuration) < 0) {
                shortest = log;
                shortestDuration = duration;
            }
        }
        return new LogSummary(
                totalTime,
                totalTime.dividedBy(logs.size()),
                logs.size(),
                longest,
                shortest);
    }

    private Duration computeLogDuration(final PowerOutageLog log) {
        final Duration duration = Duration.between(log.electricityOut(), log.electricityIn());
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    private String computeLogNickname(final PowerOutageLog log, final Map<Long, String> nicknames) {
        return nicknames.getOrDefault(log.userId(), String.valueOf(log.userId()));
    }

    private record LogSummary(
            Duration totalTime,
            Duration averageTime,
            int count,
            PowerOutageLog longest,
            PowerOutageLog shortest) {
    }
}
