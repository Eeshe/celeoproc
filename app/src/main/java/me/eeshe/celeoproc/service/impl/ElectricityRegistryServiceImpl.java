package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.util.DurationFormatter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ElectricityRegistryServiceImpl implements ElectricityRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityRegistryServiceImpl.class);

    private static final String UNKNOWN_DURATION = "unknown";

    private final RegistryChannelService registryChannelService;
    private final MessageService messageService;
    private final JDA bot;

    public ElectricityRegistryServiceImpl(
            final RegistryChannelService registryChannelService,
            final MessageService messageService,
            final JDA bot) {
        this.registryChannelService = Objects.requireNonNull(registryChannelService,
                "RegistryChannelService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public void sendElectricityOut(final long userId, final String nickname,
            final Instant previousElectricityIn) {
        Objects.requireNonNull(nickname, "Nickname must not be null");

        final String lasted = previousElectricityIn == null
                ? UNKNOWN_DURATION
                : DurationFormatter.format(Duration.between(previousElectricityIn, Instant.now()));

        send(userId, Message.ELECTRICITY_REGISTRY_OUT, Map.of(
                "nickname", nickname,
                "time_since_last_power_outage_relative", lasted));
    }

    @Override
    public void sendElectricityIn(final long userId, final String nickname, final Instant electricityOut,
            final Instant electricityIn) {
        Objects.requireNonNull(nickname, "Nickname must not be null");
        Objects.requireNonNull(electricityIn, "Electricity in must not be null");
        if (electricityOut == null) {
            return;
        }

        send(userId, Message.ELECTRICITY_REGISTRY_IN, Map.of(
                "nickname", nickname,
                "electricity_out_time", formatTimestamp(electricityOut),
                "electricity_in_time", formatTimestamp(electricityIn),
                "power_outage_time_relative",
                DurationFormatter.format(Duration.between(electricityOut, electricityIn))));
    }

    private void send(final long userId, final Message message, final Map<String, String> placeholders) {
        final List<TextChannel> channels = resolveChannels(userId);
        if (channels.isEmpty()) {
            return;
        }
        final String content = messageService.get(message, placeholders);
        for (final TextChannel channel : channels) {
            channel.sendMessage(content)
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

    private String formatTimestamp(final Instant instant) {
        return "<t:%d:t>".formatted(instant.getEpochSecond());
    }
}
