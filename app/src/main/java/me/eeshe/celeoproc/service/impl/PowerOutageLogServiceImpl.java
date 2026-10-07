package me.eeshe.celeoproc.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.service.PowerOutageLogService;

public final class PowerOutageLogServiceImpl implements PowerOutageLogService {
    private final PowerOutageLogRepository powerOutageLogRepository;
    private final AppSettings appSettings;

    public PowerOutageLogServiceImpl(final PowerOutageLogRepository powerOutageLogRepository,
            final AppSettings appSettings) {
        this.powerOutageLogRepository = Objects.requireNonNull(powerOutageLogRepository,
                "PowerOutageLogRepository must not be null");
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
    public List<PowerOutageLog> getWithinRange(final LocalDate rangeStart, final LocalDate rangeEnd) {
        Objects.requireNonNull(rangeStart, "Range start must not be null");
        Objects.requireNonNull(rangeEnd, "Range end must not be null");

        final ZoneId zone = appSettings.getTimezone();
        final Instant startInstant = rangeStart.atStartOfDay(zone).toInstant();
        final Instant endInstant = rangeEnd.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        return powerOutageLogRepository.getWithinRange(startInstant, endInstant);
    }
}
