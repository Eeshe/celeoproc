package me.eeshe.celeoproc.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.service.PowerOutageLogService;

public final class PowerOutageLogServiceImpl implements PowerOutageLogService {
    private final PowerOutageLogRepository powerOutageLogRepository;

    public PowerOutageLogServiceImpl(final PowerOutageLogRepository powerOutageLogRepository) {
        this.powerOutageLogRepository = Objects.requireNonNull(powerOutageLogRepository,
                "PowerOutageLogRepository must not be null");
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
    public List<PowerOutageLog> getWithinRange(final Instant rangeStart, final Instant rangeEnd) {
        Objects.requireNonNull(rangeStart, "Range start must not be null");
        Objects.requireNonNull(rangeEnd, "Range end must not be null");

        return powerOutageLogRepository.getWithinRange(rangeStart, rangeEnd);
    }
}
