package me.eeshe.celeoproc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.OutageStats;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageStats;
import me.eeshe.celeoproc.model.UserOutageStats;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.support.StubNicknameResolver;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Verifies the timezone-aware range conversion, guild/global partitioning and
 * the aggregated stats returned by the service.
 */
class PowerOutageLogServiceImplTest {

    private static final long GUILD_ID = 100L;
    private static final LocalDate RANGE_START = LocalDate.of(2025, 5, 5);
    private static final LocalDate RANGE_END = LocalDate.of(2025, 5, 10);

    private StubPowerOutageLogRepository repository;
    private StubElectricityStatusEmbedService electricityStatusEmbedService;
    private StubNicknameResolver nicknameResolver;
    private PowerOutageLogServiceImpl service;

    @BeforeEach
    void setUp() {
        final AppSettings appSettings = new AppSettings();
        appSettings.load();

        repository = new StubPowerOutageLogRepository();
        electricityStatusEmbedService = new StubElectricityStatusEmbedService();
        nicknameResolver = new StubNicknameResolver();
        service = new PowerOutageLogServiceImpl(repository, electricityStatusEmbedService, nicknameResolver,
                appSettings);
    }

    @Test
    void getWithinRangeSpansStartOfStartDayToEndOfEndDay() {
        service.getWithinRange(GUILD_ID, RANGE_START, RANGE_END);

        // America/New_York is UTC-4 during this range (EDT).
        assertEquals(Instant.parse("2025-05-05T04:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-05-11T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeHandlesDaylightSavingTransition() {
        service.getWithinRange(GUILD_ID, LocalDate.of(2025, 3, 9), LocalDate.of(2025, 3, 9));

        // 2025-03-09 starts in EST (UTC-5) and ends in EDT (UTC-4).
        assertEquals(Instant.parse("2025-03-09T05:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-03-10T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeAggregatesServerAndGlobalStats() {
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 1L, 2L));
        final PowerOutageLog userOneLong = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        final PowerOutageLog userOneShort = log(1L, "2025-05-07T10:00:00Z", "2025-05-07T10:30:00Z");
        final PowerOutageLog userTwo = log(2L, "2025-05-08T10:00:00Z", "2025-05-08T11:00:00Z");
        final PowerOutageLog outsider = log(99L, "2025-05-09T10:00:00Z", "2025-05-09T14:00:00Z");
        repository.result = List.of(userOneLong, userOneShort, userTwo, outsider);

        final PowerOutageStats stats = service.getWithinRange(GUILD_ID, RANGE_START, RANGE_END);

        final OutageStats server = stats.server();
        assertEquals(3, server.logAmount());
        assertEquals(Duration.ofHours(3).plusMinutes(30), server.totalTime());
        assertEquals(Duration.ofMinutes(70), server.averageTime());
        assertEquals(Duration.ofHours(2), server.longestOutage().duration());
        assertEquals("1", server.longestOutage().nickname());
        assertEquals(Duration.ofMinutes(30), server.shortestOutage().duration());
        assertEquals(2, server.userStats().size());
        assertEquals(Duration.ofHours(2).plusMinutes(30), server.awardWinner().totalTime());

        assertEquals(List.of(userOneLong, userOneShort, userTwo), stats.serverLogs());

        final OutageStats global = stats.global();
        assertEquals(4, global.logAmount());
        assertEquals(Duration.ofHours(7).plusMinutes(30), global.totalTime());
        assertEquals(Duration.ofMinutes(112).plusSeconds(30), global.averageTime());
        assertNull(global.longestOutage());
        assertTrue(global.userStats().isEmpty());
    }

    @Test
    void getWithinRangeOrdersUsersByTotalTime() {
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 1L, 2L));
        repository.result = List.of(
                log(2L, "2025-05-06T10:00:00Z", "2025-05-06T10:45:00Z"),
                log(1L, "2025-05-07T10:00:00Z", "2025-05-07T13:00:00Z"));

        final List<UserOutageStats> userStats = service.getWithinRange(GUILD_ID, RANGE_START, RANGE_END)
                .server()
                .userStats();

        assertEquals(1L, userStats.get(0).userId());
        assertEquals(2L, userStats.get(1).userId());
    }

    @Test
    void getWithinRangeResolvesNicknamesAndFallsBackToUserId() {
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 1L, 3L));
        nicknameResolver.setName(1L, "Alice");
        repository.result = List.of(
                log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z"),
                log(3L, "2025-05-07T10:00:00Z", "2025-05-07T11:00:00Z"));

        final OutageStats server = service.getWithinRange(GUILD_ID, RANGE_START, RANGE_END).server();

        assertEquals("Alice", server.longestOutage().nickname());
        assertEquals(List.of("Alice", "3"),
                server.userStats().stream().map(UserOutageStats::nickname).toList());
    }

    @Test
    void getWithinRangeWithoutLogsReturnsEmptyStats() {
        final PowerOutageStats stats = service.getWithinRange(GUILD_ID, RANGE_START, RANGE_END);

        final OutageStats server = stats.server();
        assertEquals(0, server.logAmount());
        assertEquals(Duration.ZERO, server.totalTime());
        assertEquals(Duration.ZERO, server.averageTime());
        assertNull(server.longestOutage());
        assertNull(server.shortestOutage());
        assertNull(server.awardWinner());
        assertTrue(server.userStats().isEmpty());

        assertEquals(0, stats.global().logAmount());
        assertTrue(stats.global().userStats().isEmpty());
    }

    @Test
    void getWithinRangeRejectsNullDates() {
        assertThrows(NullPointerException.class, () -> service.getWithinRange(GUILD_ID, null, RANGE_END));
        assertThrows(NullPointerException.class, () -> service.getWithinRange(GUILD_ID, RANGE_START, null));
    }

    @Test
    void getByIdDelegatesToRepository() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        repository.stored.put(log.id(), log);

        assertEquals(Optional.of(log), service.getById(log.id()));
        assertTrue(service.getById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void deleteRemovesStoredLog() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        repository.stored.put(log.id(), log);

        assertTrue(service.delete(log));
        assertTrue(repository.stored.isEmpty());
    }

    @Test
    void deleteReturnsFalseWhenLogIsMissing() {
        assertFalse(service.delete(log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z")));
    }

    @Test
    void startPowerOutagePersistsIncompleteLog() {
        final Instant electricityOut = Instant.parse("2025-05-06T10:00:00Z");

        final PowerOutageLog log = service.startPowerOutage(1L, electricityOut);

        assertNull(log.electricityIn());
        assertEquals(electricityOut, log.electricityOut());
        assertEquals(Optional.of(log), repository.getById(log.id()));
    }

    @Test
    void updateOverwritesStoredLog() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        repository.stored.put(log.id(), log);

        final PowerOutageLog updated = new PowerOutageLog(log.id(), log.userId(), log.electricityOut(),
                Instant.parse("2025-05-06T13:00:00Z"));
        service.update(updated);

        assertEquals(Optional.of(updated), repository.getById(log.id()));
    }

    @Test
    void updatePowerOutageConvertsInputsToConfiguredTimezone() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");

        final PowerOutageLog updated = service.updatePowerOutage(log,
                LocalDateTime.of(2025, 5, 6, 10, 0),
                LocalDateTime.of(2025, 5, 6, 12, 0));

        // America/New_York is UTC-4 during this date (EDT).
        assertEquals(Instant.parse("2025-05-06T14:00:00Z"), updated.electricityOut());
        assertEquals(Instant.parse("2025-05-06T16:00:00Z"), updated.electricityIn());
        assertEquals(log.id(), updated.id());
        assertEquals(log.userId(), updated.userId());
        assertEquals(updated, repository.lastSaved);
    }

    @Test
    void buildLogPlaceholdersResolvesNicknameAndFormatsValues() {
        nicknameResolver.setName(1L, "Alice");
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");

        final Map<String, String> placeholders = service.buildLogPlaceholders(log, GUILD_ID);

        assertEquals("Alice", placeholders.get("nickname"));
        assertEquals(log.id().toString(), placeholders.get("log_id"));
        assertEquals("2h", placeholders.get("power_outage_time"));
        assertEquals("<t:%d:t>".formatted(Instant.parse("2025-05-06T10:00:00Z").getEpochSecond()),
                placeholders.get("electricity_out"));
        assertEquals("<t:%d:t>".formatted(Instant.parse("2025-05-06T12:00:00Z").getEpochSecond()),
                placeholders.get("electricity_in"));
    }

    private static PowerOutageLog log(final long userId, final String electricityOut, final String electricityIn) {
        return new PowerOutageLog(UUID.randomUUID(), userId, Instant.parse(electricityOut),
                Instant.parse(electricityIn));
    }

    private static ElectricityStatusEmbed guildEmbed(final long guildId, final Long... participantUserIds) {
        return new ElectricityStatusEmbed(1L, guildId, 1L, List.of(participantUserIds),
                Instant.parse("2025-05-01T00:00:00Z"));
    }

    /**
     * Hand-written stubs because the project does not depend on a mocking library.
     */
    private static final class StubPowerOutageLogRepository implements PowerOutageLogRepository {
        private final Map<UUID, PowerOutageLog> stored = new HashMap<>();
        private Instant rangeStart;
        private Instant rangeEnd;
        private List<PowerOutageLog> result = List.of();
        private PowerOutageLog lastSaved;

        @Override
        public void initialize() throws SQLException {
        }

        @Override
        public void shutdown() throws SQLException {
        }

        @Override
        public void save(final PowerOutageLog powerOutageLog) {
            stored.put(powerOutageLog.id(), powerOutageLog);
            lastSaved = powerOutageLog;
        }

        @Override
        public void update(final PowerOutageLog powerOutageLog) {
            stored.put(powerOutageLog.id(), powerOutageLog);
            lastSaved = powerOutageLog;
        }

        @Override
        public Optional<PowerOutageLog> getIncompletePowerOutageLog(final long userId) {
            return stored.values().stream()
                    .filter(log -> log.userId() == userId && !log.hasElectricityIn())
                    .max(Comparator.comparing(PowerOutageLog::electricityOut));
        }

        @Override
        public Optional<PowerOutageLog> getPreviousCompletedPowerOutageLog(final long userId,
                final Instant beforeInstant) {
            return stored.values().stream()
                    .filter(log -> log.userId() == userId && log.hasElectricityIn()
                            && log.electricityOut().isBefore(beforeInstant))
                    .max(Comparator.comparing(PowerOutageLog::electricityOut));
        }

        @Override
        public Optional<PowerOutageLog> getById(final UUID id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public void delete(final UUID id) {
            stored.remove(id);
        }

        @Override
        public List<PowerOutageLog> getWithinRange(final Instant rangeStart, final Instant rangeEnd) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            return result;
        }
    }

    private static final class StubElectricityStatusEmbedService implements ElectricityStatusEmbedService {
        private final List<ElectricityStatusEmbed> embeds = new ArrayList<>();

        private void addEmbed(final ElectricityStatusEmbed embed) {
            embeds.add(embed);
        }

        @Override
        public boolean sendElectricityStatusEmbed(final TextChannel channel) {
            return true;
        }

        @Override
        public Optional<String> getJumpUrl(final long messageId) {
            return Optional.empty();
        }

        @Override
        public List<ElectricityStatusEmbed> getByParticipantId(final long userId) {
            return embeds.stream()
                    .filter(embed -> embed.getParticipantUserIds().contains(userId))
                    .toList();
        }

        @Override
        public List<ElectricityStatusEmbed> getByGuildId(final long guildId) {
            return embeds.stream()
                    .filter(embed -> embed.getGuildId() == guildId)
                    .toList();
        }

        @Override
        public boolean updateElectricityStatusEmbed(final ElectricityStatusEmbed statusEmbed) {
            return true;
        }

        @Override
        public void deleteElectricityStatusEmbed(final long messageId) {
        }
    }
}
