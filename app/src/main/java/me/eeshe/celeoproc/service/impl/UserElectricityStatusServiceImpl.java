package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;

public final class UserElectricityStatusServiceImpl implements UserElectricityStatusService {
    private final UserElectricityStatusRepository userElectricityStatusRepository;
    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final ElectricityRegistryService electricityRegistryService;

    public UserElectricityStatusServiceImpl(
            final UserElectricityStatusRepository userElectricityStatusRepository,
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final ElectricityRegistryService electricityRegistryService) {
        this.userElectricityStatusRepository = Objects.requireNonNull(userElectricityStatusRepository,
                "UserElectricityStatusRepository must not be null");
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.electricityRegistryService = Objects.requireNonNull(electricityRegistryService,
                "ElectricityRegistryService must not be null");
    }

    @Override
    public boolean setUserElectricityIn(final long userId, final String nickname, final long embedMessageId) {
        final Optional<UserElectricityStatus> existing = userElectricityStatusRepository.get(userId);
        if (existing.isPresent() && existing.get().hasElectricity()) {
            return addParticipantIfAbsent(embedMessageId, userId);
        }

        final UserElectricityStatus status = existing
                .orElseGet(() -> new UserElectricityStatus(userId, nickname, null, null, null));
        status.setElectricityIn(Instant.now());
        userElectricityStatusRepository.save(status);
        updateParticipantEmbeds(embedMessageId, userId);
        electricityRegistryService.sendElectricityIn(userId, status.getNickname(), status.getElectricityOut(),
                status.getElectricityIn());
        return true;
    }

    @Override
    public boolean setUserElectricityOut(
            final long userId,
            final String nickname,
            final Duration electricityInEstimate,
            final long embedMessageId) {
        final Optional<UserElectricityStatus> existing = userElectricityStatusRepository.get(userId);
        if (existing.isPresent() && !existing.get().hasElectricity()) {
            return addParticipantIfAbsent(embedMessageId, userId);
        }

        Objects.requireNonNull(electricityInEstimate, "Electricity in estimate must not be null");
        final UserElectricityStatus status = existing
                .orElseGet(() -> new UserElectricityStatus(userId, nickname, null, null, null));
        final Instant now = Instant.now();

        status.setElectricityOut(now);
        status.setElectricityInEstimate(now.plus(electricityInEstimate));

        userElectricityStatusRepository.save(status);
        updateParticipantEmbeds(embedMessageId, userId);
        electricityRegistryService.sendElectricityOut(userId, status.getNickname(), status.getElectricityIn());
        return true;
    }

    @Override
    public boolean hasNoElectricity(final long userId) {
        return userElectricityStatusRepository.get(userId)
                .map(userStatus -> !userStatus.hasElectricity())
                .orElse(false);
    }

    private boolean addParticipantIfAbsent(final long messageId, final long userId) {
        final Optional<ElectricityStatusEmbed> existing = electricityStatusEmbedRepository.get(messageId);
        if (existing.isEmpty()) {
            return false;
        }
        final ElectricityStatusEmbed statusEmbed = existing.get();
        if (statusEmbed.getParticipantUserIds().contains(userId)) {
            return false;
        }
        statusEmbed.addParticipant(userId);
        electricityStatusEmbedService.updateElectricityStatusEmbed(statusEmbed);
        return true;
    }

    private void updateParticipantEmbeds(final long messageId, final long userId) {
        updateElectricityStatusEmbed(messageId, userId);
        for (final ElectricityStatusEmbed statusEmbed : electricityStatusEmbedService.getByParticipantId(userId)) {
            if (statusEmbed.getMessageId() == messageId) {
                continue;
            }
            electricityStatusEmbedService.updateElectricityStatusEmbed(statusEmbed);
        }
    }

    private void updateElectricityStatusEmbed(final long messageId, final long userId) {
        final Optional<ElectricityStatusEmbed> existing = electricityStatusEmbedRepository.get(messageId);
        if (existing.isEmpty()) {
            return;
        }
        final ElectricityStatusEmbed statusEmbed = existing.get();
        if (!statusEmbed.getParticipantUserIds().contains(userId)) {
            statusEmbed.addParticipant(userId);
        }
        electricityStatusEmbedService.updateElectricityStatusEmbed(statusEmbed);
    }
}
