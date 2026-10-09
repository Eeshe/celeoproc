package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.listener.PowerOutageManagementListener;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.NicknameResolver;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.util.DurationFormatter;
import me.eeshe.celeoproc.util.TimestampFormatter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public final class ElectricityRegistryServiceImpl implements ElectricityRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityRegistryServiceImpl.class);

    private static final String UNKNOWN_DURATION = "unknown";

    private final RegistryChannelService registryChannelService;
    private final MessageService messageService;
    private final NicknameResolver nicknameResolver;
    private final JDA bot;

    public ElectricityRegistryServiceImpl(
            final RegistryChannelService registryChannelService,
            final MessageService messageService,
            final NicknameResolver nicknameResolver,
            final JDA bot) {
        this.registryChannelService = Objects.requireNonNull(registryChannelService,
                "RegistryChannelService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.nicknameResolver = Objects.requireNonNull(nicknameResolver, "NicknameResolver must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public void sendElectricityOut(final long userId, final Instant previousElectricityIn) {
        final String lasted = previousElectricityIn == null
                ? UNKNOWN_DURATION
                : DurationFormatter.format(Duration.between(previousElectricityIn, Instant.now()));

        send(userId, Message.ELECTRICITY_REGISTRY_OUT, Map.of(
                "time_since_last_power_outage_relative", lasted), List.of());
    }

    @Override
    public void sendElectricityIn(final PowerOutageLog log) {
        Objects.requireNonNull(log, "PowerOutageLog must not be null");

        send(log.userId(), Message.ELECTRICITY_REGISTRY_IN, buildElectricityInPlaceholders(log),
                List.of(buildManagementButtons(log.id())));
    }

    @Override
    public void editElectricityIn(final net.dv8tion.jda.api.entities.Message message, final PowerOutageLog log) {
        Objects.requireNonNull(message, "Message must not be null");
        Objects.requireNonNull(log, "PowerOutageLog must not be null");

        if (!message.isFromGuild()) {
            LOGGER.error("Failed to edit electricity registry message '{}': message is not from a guild",
                    message.getIdLong());
            return;
        }
        final Map<String, String> placeholders = new HashMap<>(buildElectricityInPlaceholders(log));
        placeholders.put("nickname", nicknameResolver.resolveNickname(message.getGuildIdLong(), log.userId()));

        message.editMessage(messageService.get(Message.ELECTRICITY_REGISTRY_IN, placeholders))
                .queue(
                        ignored -> {
                        },
                        throwable -> LOGGER.error(
                                "Failed to edit electricity registry message '{}'", message.getIdLong(), throwable));
    }

    @Override
    public void deleteElectricityIn(final MessageChannel channel, final long messageId) {
        Objects.requireNonNull(channel, "Channel must not be null");

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

    private void send(
            final long userId,
            final Message message,
            final Map<String, String> placeholders,
            final List<ActionRow> components) {
        for (final TextChannel channel : resolveChannels(userId)) {
            final Map<String, String> channelPlaceholders = new HashMap<>(placeholders);
            channelPlaceholders.put("nickname",
                    nicknameResolver.resolveNickname(channel.getGuild().getIdLong(), userId));

            final String content = messageService.get(message, channelPlaceholders);
            channel.sendMessage(content)
                    .addComponents(components)
                    .queue(
                            ignored -> {
                            },
                            throwable -> LOGGER.error(
                                    "Failed to send electricity registry message to channel '{}'",
                                    channel.getIdLong(), throwable));
        }
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
