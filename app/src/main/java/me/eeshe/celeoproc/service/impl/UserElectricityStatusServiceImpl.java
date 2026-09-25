package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.service.UserElectricityStatusService;

public final class UserElectricityStatusServiceImpl implements UserElectricityStatusService {
    private final UserElectricityStatusRepository userElectricityStatusRepository;

    public UserElectricityStatusServiceImpl(final UserElectricityStatusRepository userElectricityStatusRepository) {
        this.userElectricityStatusRepository = Objects.requireNonNull(userElectricityStatusRepository,
                "UserElectricityStatusRepository must not be null");
    }

    @Override
    public void setUserElectricityIn(final long userId, final String nickname) {
        final UserElectricityStatus status = getOrCreate(userId, nickname);
        status.setElectricityIn(Instant.now());

        userElectricityStatusRepository.save(status);
    }

    @Override
    public void setUserElectricityOut(final long userId, final String nickname, final Duration estimate) {
        final UserElectricityStatus status = getOrCreate(userId, nickname);
        status.setElectricityOut(Instant.now());
        status.setElectricityInEstimate(estimate);

        userElectricityStatusRepository.save(status);
    }

    private UserElectricityStatus getOrCreate(final long userId, final String nickname) {
        return userElectricityStatusRepository.get(userId)
                .orElseGet(() -> new UserElectricityStatus(userId, nickname, null, null, null));
    }
}
