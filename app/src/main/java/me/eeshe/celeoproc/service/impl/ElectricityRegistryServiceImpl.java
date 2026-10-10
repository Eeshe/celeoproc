package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.listener.PowerOutageManagementListener;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.NicknameResolver;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.service.PowerOutageRegistryMessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.util.DurationFormatter;
import me.eeshe.celeoproc.util.TimestampFormatter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ElectricityRegistryServiceImpl implements ElectricityRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityRegistryServiceImpl.class);

    private static final String UNKNOWN_DURATION = "unknown";

    private final RegistryChannelService registryChannelService;
    private final PowerOutageRegistryMessageService powerOutageRegistryMessageService;
    private final MessageService messageService;
    private final NicknameResolver nicknameResolver;
    private final PowerOutageLogService powerOutageLogService;
    private final JDA bot;

    public ElectricityRegistryServiceImpl(
            final RegistryChannelService registryChannelService,
            final PowerOutageRegistryMessageService powerOutageRegistryMessageService,
            final MessageService messageService,
            final NicknameResolver nicknameResolver,
            final PowerOutageLogService powerOutageLogService,
            final JDA bot) {
        this.registryChannelService = Objects.requireNonNull(registryChannelService,
                "RegistryChannelService must not be null");
        this.powerOutageRegistryMessageService = Objects.requireNonNull(powerOutageRegistryMessageService,
                "PowerOutageRegistryMessageService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.nicknameResolver = Objects.requireNonNull(nicknameResolver, "NicknameResolver must not be null");
        this.powerOutageLogService = Objects.requireNonNull(powerOutageLogService,
                "PowerOutageLogService must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public List<CompletableFuture<net.dv8tion.jda.api.entities.Message>> sendElectricityOut(final long userId,
            final Instant previousElectricityIn) {
        final String formattedDuration = previousElectricityIn == null
                ? UNKNOWN_DURATION
                : DurationFormatter.format(Duration.between(previousElectricityIn, Instant.now()));

        return send(userId, Message.ELECTRICITY_REGISTRY_OUT, Map.of(
                "time_since_last_power_outage_relative", formattedDuration), List.of());
    }

    @Override
    public List<CompletableFuture<net.dv8tion.jda.api.entities.Message>> sendElectricityIn(final PowerOutageLog log) {
        Objects.requireNonNull(log, "PowerOutageLog must not be null");

        return send(log.userId(), Message.ELECTRICITY_REGISTRY_IN, buildElectricityInPlaceholders(log),
                List.of(buildManagementButtons(log.id())));
    }

    @Override
    public void editRegistryMessages(final PowerOutageLog log) {
        Objects.requireNonNull(log, "PowerOutageLog must not be null");

        final String formattedDuration = powerOutageLogService
                .getPreviousCompletedPowerOutageLog(log.userId(), log.electricityOut())
                .map(previous -> DurationFormatter.format(
                        Duration.between(previous.electricityIn(), log.electricityOut())))
                .orElse(UNKNOWN_DURATION);

        for (final PowerOutageRegistryMessage registryMessage : powerOutageRegistryMessageService
                .getByPowerOutageLogId(log.id())) {
            final TextChannel channel = bot.getTextChannelById(registryMessage.channelId());
            if (channel == null) {
                LOGGER.error("Failed to edit electricity registry messages: channel '{}' not found",
                        registryMessage.channelId());
                continue;
            }
            final String nickname = nicknameResolver.resolveNickname(channel.getGuild().getIdLong(), log.userId());

            editMessage(
                    channel,
                    registryMessage.registryOutMessageId(),
                    Message.ELECTRICITY_REGISTRY_OUT,
                    Map.of(
                            "nickname", nickname,
                            "time_since_last_power_outage_relative", formattedDuration));

            if (registryMessage.registryInMessageId() != null) {
                final Map<String, String> inPlaceholders = new HashMap<>(buildElectricityInPlaceholders(log));
                inPlaceholders.put("nickname", nickname);

                editMessage(
                        channel,
                        registryMessage.registryInMessageId(),
                        Message.ELECTRICITY_REGISTRY_IN,
                        inPlaceholders);
            }
        }
    }

    private void editMessage(
            final TextChannel channel,
            final long messageId,
            final Message message,
            final Map<String, String> placeholders) {
        channel.editMessageById(messageId, messageService.get(message, placeholders))
                .queue(
                        ignored -> {
                        },
                        throwable -> LOGGER.error(
                                "Failed to edit electricity registry message '{}'", messageId, throwable));
    }

    @Override
    public void deleteRegistryMessages(final PowerOutageLog log) {
        Objects.requireNonNull(log, "PowerOutageLog must not be null");

        for (final PowerOutageRegistryMessage registryMessage : powerOutageRegistryMessageService
                .getByPowerOutageLogId(log.id())) {
            final TextChannel channel = bot.getTextChannelById(registryMessage.channelId());
            if (channel == null) {
                LOGGER.error("Failed to delete electricity registry messages: channel '{}' not found",
                        registryMessage.channelId());
                continue;
            }
            deleteMessage(channel, registryMessage.registryOutMessageId());
            if (registryMessage.registryInMessageId() != null) {
                deleteMessage(channel, registryMessage.registryInMessageId());
            }
        }
    }

    private void deleteMessage(final TextChannel channel, final long messageId) {
        channel.deleteMessageById(messageId)
                .queue(
                        ignored -> {
                        },
                        throwable -> LOGGER.error(
                                "Failed to delete electricity registry message '{}'", messageId, throwable));
    }

    private Map<String, String> buildElectricityInPlaceholders(final PowerOutageLog log) {
        return Map.of(
                "electricity_out_time", TimestampFormatter.format(log.electricityOut()),
                "electricity_in_time", TimestampFormatter.format(log.electricityIn()),
                "power_outage_time_relative",
                DurationFormatter.format(Duration.between(log.electricityOut(), log.electricityIn())));
    }

    /**
     * @param id id of the log the buttons manage
     * @return action row with the edit and delete buttons for the given log
     */
    private ActionRow buildManagementButtons(final UUID id) {
        return ActionRow.of(
                Button.primary(
                        PowerOutageManagementListener.computeEditButtonId(id),
                        messageService.get(Message.POWER_OUTAGE_EDIT_BUTTON)),
                Button.danger(
                        PowerOutageManagementListener.computeDeleteButtonId(id),
                        messageService.get(Message.POWER_OUTAGE_DELETE_BUTTON)));
    }

    private List<CompletableFuture<net.dv8tion.jda.api.entities.Message>> send(
            final long userId,
            final Message message,
            final Map<String, String> placeholders,
            final List<ActionRow> components) {
        final List<CompletableFuture<net.dv8tion.jda.api.entities.Message>> futures = new ArrayList<>();
        for (final TextChannel channel : resolveChannels(userId)) {
            final Map<String, String> channelPlaceholders = new HashMap<>(placeholders);
            channelPlaceholders.put("nickname",
                    nicknameResolver.resolveNickname(channel.getGuild().getIdLong(), userId));

            final String content = messageService.get(message, channelPlaceholders);
            futures.add(channel.sendMessage(content)
                    .addComponents(components)
                    .submit());
        }
        return futures;
    }

    /**
     * @param userId id of the user whose status changed
     * @return text channels whose embeds list the user as a participant, skipping
     *         channels the bot can no longer resolve
     */
    private List<TextChannel> resolveChannels(final long userId) {
        final List<TextChannel> channels = new ArrayList<>();
        for (final long channelId : registryChannelService.getByParticipantId(userId)) {
            final TextChannel channel = bot.getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.error("Failed to send electricity registry message: channel '{}' not found", channelId);
                continue;
            }
            channels.add(channel);
        }
        return channels;
    }
}
