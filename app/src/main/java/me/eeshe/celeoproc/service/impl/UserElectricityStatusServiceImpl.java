package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.ElectricityStatusUpdateResult;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;
import me.eeshe.celeoproc.model.RegistrySendResult;
import me.eeshe.celeoproc.model.SkippedChannel;
import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.service.PowerOutageRegistryMessageService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import net.dv8tion.jda.api.entities.Message;

public final class UserElectricityStatusServiceImpl implements UserElectricityStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserElectricityStatusServiceImpl.class);

    private final UserElectricityStatusRepository userElectricityStatusRepository;
    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final ElectricityRegistryService electricityRegistryService;
    private final PowerOutageLogService powerOutageLogService;
    private final PowerOutageRegistryMessageService powerOutageRegistryMessageService;
    private final AppSettings appSettings;

    public UserElectricityStatusServiceImpl(
            final UserElectricityStatusRepository userElectricityStatusRepository,
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final ElectricityRegistryService electricityRegistryService,
            final PowerOutageLogService powerOutageLogService,
            final PowerOutageRegistryMessageService powerOutageRegistryMessageService,
            final AppSettings appSettings) {
        this.userElectricityStatusRepository = Objects.requireNonNull(userElectricityStatusRepository,
                "UserElectricityStatusRepository must not be null");
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.electricityRegistryService = Objects.requireNonNull(electricityRegistryService,
                "ElectricityRegistryService must not be null");
        this.powerOutageLogService = Objects.requireNonNull(powerOutageLogService,
                "PowerOutageLogService must not be null");
        this.powerOutageRegistryMessageService = Objects.requireNonNull(powerOutageRegistryMessageService,
                "PowerOutageRegistryMessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
    }

    @Override
    public ElectricityStatusUpdateResult setUserElectricityIn(final long userId, final long embedMessageId) {
        final Optional<UserElectricityStatus> existing = userElectricityStatusRepository.get(userId);
        if (existing.isPresent() && existing.get().hasElectricity()) {
            return new ElectricityStatusUpdateResult(addParticipantIfAbsent(embedMessageId, userId), List.of());
        }

        final UserElectricityStatus status = existing
                .orElseGet(() -> new UserElectricityStatus(userId));
        status.setElectricityIn(Instant.now());
        userElectricityStatusRepository.save(status);

        List<SkippedChannel> skippedChannels = List.of();
        if (status.getElectricityOut() != null) {
            final Optional<PowerOutageLog> incompleteLog = powerOutageLogService
                    .getIncompletePowerOutageLog(userId);
            if (incompleteLog.isPresent()) {
                final PowerOutageLog completedLog = new PowerOutageLog(
                        incompleteLog.get().id(),
                        incompleteLog.get().userId(),
                        incompleteLog.get().electricityOut(),
                        status.getElectricityIn());
                powerOutageLogService.update(completedLog);
                skippedChannels = saveElectricityInRegistryMessages(completedLog);
            }
        }
        updateParticipantEmbeds(embedMessageId, userId);
        return new ElectricityStatusUpdateResult(true, skippedChannels);
    }

    @Override
    public ElectricityStatusUpdateResult setUserElectricityOut(
            final long userId,
            final Duration electricityInEstimate,
            final long embedMessageId) {
        final Optional<UserElectricityStatus> existing = userElectricityStatusRepository.get(userId);
        if (existing.isPresent() && !existing.get().hasElectricity()) {
            return new ElectricityStatusUpdateResult(addParticipantIfAbsent(embedMessageId, userId), List.of());
        }

        Objects.requireNonNull(electricityInEstimate, "Electricity in estimate must not be null");
        final UserElectricityStatus status = existing.orElseGet(() -> new UserElectricityStatus(userId));
        final Instant now = Instant.now();

        status.setElectricityOut(now);
        status.setElectricityInEstimate(now.plus(electricityInEstimate));
        status.setLastReminderAt(Instant.now());

        userElectricityStatusRepository.save(status);
        updateParticipantEmbeds(embedMessageId, userId);

        final PowerOutageLog powerOutageLog = powerOutageLogService.startPowerOutage(userId, now);
        final List<SkippedChannel> skippedChannels = saveElectricityOutRegistryMessages(powerOutageLog,
                status.getElectricityIn());
        return new ElectricityStatusUpdateResult(true, skippedChannels);
    }

    /**
     * Sends the electricity out registry messages and persists the message ids
     * that will later be linked to the electricity in messages.
     *
     * @param log                   incomplete log the messages announce
     * @param previousElectricityIn moment the user's electricity last came back
     * @return registry channels skipped because the bot lacks permissions
     */
    private List<SkippedChannel> saveElectricityOutRegistryMessages(
            final PowerOutageLog log,
            final Instant previousElectricityIn) {
        final RegistrySendResult sendResult = electricityRegistryService.sendElectricityOut(log.userId(),
                previousElectricityIn);
        final List<PowerOutageRegistryMessage> registryMessages = awaitRegistryMessages(sendResult.messages())
                .stream()
                .map(message -> new PowerOutageRegistryMessage(
                        message.getIdLong(),
                        null,
                        log.id(),
                        message.getChannel().getIdLong()))
                .toList();
        powerOutageRegistryMessageService.saveAll(registryMessages);
        return sendResult.skippedChannels();
    }

    /**
     * Sends the electricity in registry messages and links their ids to the
     * previously stored electricity out messages, matching them by channel.
     *
     * @param log completed log the messages announce
     * @return registry channels skipped because the bot lacks permissions
     */
    private List<SkippedChannel> saveElectricityInRegistryMessages(final PowerOutageLog log) {
        final RegistrySendResult sendResult = electricityRegistryService.sendElectricityIn(log);

        final Map<Long, PowerOutageRegistryMessage> storedByChannel = new HashMap<>();
        for (final PowerOutageRegistryMessage registryMessage : powerOutageRegistryMessageService
                .getByPowerOutageLogId(log.id())) {
            storedByChannel.put(registryMessage.channelId(), registryMessage);
        }
        final List<PowerOutageRegistryMessage> updatedMessages = new ArrayList<>();
        for (final Message message : awaitRegistryMessages(sendResult.messages())) {
            final PowerOutageRegistryMessage stored = storedByChannel.get(message.getChannel().getIdLong());
            if (stored == null) {
                continue;
            }
            updatedMessages.add(new PowerOutageRegistryMessage(
                    stored.registryOutMessageId(),
                    message.getIdLong(),
                    stored.powerOutageLogId(),
                    stored.channelId()));
        }
        powerOutageRegistryMessageService.updateAll(updatedMessages);
        return sendResult.skippedChannels();
    }

    /**
     * Waits for every registry message send to complete, dropping the ones that
     * failed instead of propagating the exception.
     *
     * @param futures send futures to await
     * @return the successfully sent messages
     */
    private List<Message> awaitRegistryMessages(final List<CompletableFuture<Message>> futures) {
        final List<Message> messages = new ArrayList<>();
        for (final CompletableFuture<Message> future : futures) {
            try {
                messages.add(future.join());
            } catch (final CompletionException exception) {
                LOGGER.error("Failed to send an electricity registry message", exception);
            }
        }
        return messages;
    }

    @Override
    public boolean hasNoElectricity(final long userId) {
        return userElectricityStatusRepository.get(userId)
                .map(userStatus -> !userStatus.hasElectricity())
                .orElse(false);
    }

    @Override
    public List<UserElectricityStatus> getPendingReminders() {
        final Instant threshold = Instant.now().minus(appSettings.getElectricityReminderFrequency());

        return userElectricityStatusRepository.getPendingReminders(threshold);
    }

    @Override
    public void markReminderSent(final UserElectricityStatus status) {
        Objects.requireNonNull(status, "UserElectricityStatus must not be null");

        status.setLastReminderAt(Instant.now());
        userElectricityStatusRepository.save(status);
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
