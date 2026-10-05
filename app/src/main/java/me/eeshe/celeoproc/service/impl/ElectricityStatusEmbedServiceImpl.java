package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ElectricityStatusEmbedServiceImpl implements ElectricityStatusEmbedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedServiceImpl.class);

    private static final String TITLE = "Electricity Tracker";

    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final UserElectricityStatusRepository userElectricityStatusRepository;
    private final MessageService messageService;
    private final AppSettings appSettings;
    private final JDA bot;

    public ElectricityStatusEmbedServiceImpl(
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final UserElectricityStatusRepository userElectricityStatusRepository,
            final MessageService messageService,
            final AppSettings appSettings,
            final JDA bot) {
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.userElectricityStatusRepository = Objects.requireNonNull(userElectricityStatusRepository,
                "UserElectricityStatusRepository must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public boolean sendElectricityStatusEmbed(final TextChannel channel) {
        Objects.requireNonNull(channel, "TextChannel must not be null");

        final long channelId = channel.getIdLong();
        final long guildId = channel.getGuild().getIdLong();

        channel.sendMessageEmbeds(buildElectricityStatusEmbed(List.of()))
                .addComponents(buildButtons())
                .queue(message -> saveEmbed(message.getIdLong(), guildId, channelId),
                        throwable -> LOGGER.error(
                                "Failed to send electricity status embed to channel '{}'", channelId, throwable));
        return true;
    }

    @Override
    public boolean updateElectricityStatusEmbed(final ElectricityStatusEmbed statusEmbed) {
        Objects.requireNonNull(statusEmbed, "ElectricityStatusEmbed must not be null");

        final TextChannel channel = bot.getTextChannelById(statusEmbed.getChannelId());
        if (channel == null) {
            LOGGER.error("Failed to update electricity status embed: channel '{}' not found",
                    statusEmbed.getChannelId());
            return false;
        }
        channel.editMessageEmbedsById(statusEmbed.getMessageId(),
                buildElectricityStatusEmbed(statusEmbed.getParticipantUserIds()))
                .queue(
                        editedMessage -> {
                            statusEmbed.refreshUpdatedAt();
                            electricityStatusEmbedRepository.save(statusEmbed);
                        },
                        throwable -> LOGGER.error(
                                "Failed to update electricity status embed for message '{}'",
                                statusEmbed.getMessageId(), throwable));
        return true;
    }

    @Override
    public void deleteElectricityStatusEmbed(final long messageId) {
        electricityStatusEmbedRepository.delete(messageId);
    }

    private MessageEmbed buildElectricityStatusEmbed(final List<Long> participantUserIds) {
        final String description = userElectricityStatusRepository.get(participantUserIds).stream()
                .map(this::formatParticipant)
                .collect(Collectors.joining("\n\n"));

        final EmbedBuilder builder = new EmbedBuilder().setTitle(TITLE);
        if (!description.isBlank()) {
            builder.setDescription(description);
        }
        return builder.build();
    }

    private String formatParticipant(final UserElectricityStatus status) {
        if (!status.hasElectricity()) {
            return formatWithoutElectricity(status);
        }
        if (status.hasElectricityOutToday(appSettings.getTimezone())) {
            return formatWithOutageToday(status);
        }
        return formatWithElectricity(status);
    }

    private String formatWithElectricity(final UserElectricityStatus status) {
        final String header = messageService.get(Message.ELECTRICITY_STATUS_EMBED_HAS_ELECTRICITY,
                Map.of("nickname", status.getNickname()));
        if (status.getElectricityOut() == null) {
            return header;
        }

        final String lastOut = messageService.get(Message.ELECTRICITY_STATUS_EMBED_LAST_ELECTRICITY_OUT,
                Map.of("duration", formatRelativeTime(status.getElectricityOut())));
        return header + "\n" + lastOut;
    }

    private String formatWithOutageToday(final UserElectricityStatus status) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_OUTAGE_TODAY, Map.of(
                "nickname", status.getNickname(),
                "out", formatTimestamp(status.getElectricityOut()),
                "in", formatTimestamp(status.getElectricityIn())));
    }

    private String formatWithoutElectricity(final UserElectricityStatus status) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_NO_ELECTRICITY, Map.of(
                "nickname", status.getNickname(),
                "out", formatTimestamp(status.getElectricityOut()),
                "estimate", formatTimestamp(status.getElectricityInEstimate()),
                "estimate_relative", formatRelativeTimestamp(status.getElectricityInEstimate())));
    }

    private ActionRow buildButtons() {
        return ActionRow.of(
                Button.danger(
                        ELECTRICITY_OUT_BUTTON_ID,
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_BUTTON_OUT)),
                Button.success(
                        ELECTRICITY_IN_BUTTON_ID,
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_BUTTON_IN)));
    }

    private String formatTimestamp(final Instant instant) {
        return "<t:%d:t>".formatted(instant.getEpochSecond());
    }

    private String formatRelativeTimestamp(final Instant instant) {
        return "<t:%d:R>".formatted(instant.getEpochSecond());
    }

    private String formatRelativeTime(final Instant from) {
        return formatDuration(Duration.between(from, Instant.now()));
    }

    private String formatDuration(final Duration duration) {
        final long totalMinutes = Math.max(0L, duration.toMinutes());
        final long days = totalMinutes / (24 * 60);
        final long hours = (totalMinutes % (24 * 60)) / 60;
        final long minutes = totalMinutes % 60;

        final StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append('d');
        }
        if (hours > 0) {
            builder.append(hours).append('h');
        }
        if (minutes > 0 || builder.length() == 0) {
            builder.append(minutes).append('m');
        }
        return builder.toString();
    }

    private void saveEmbed(final long messageId, final long guildId, final long channelId) {
        final ElectricityStatusEmbed statusEmbed = new ElectricityStatusEmbed(
                messageId,
                guildId,
                channelId,
                List.of());
        electricityStatusEmbedRepository.save(statusEmbed);
    }
}
