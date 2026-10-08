package me.eeshe.celeoproc.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import me.eeshe.celeoproc.service.NicknameResolver;
import me.eeshe.celeoproc.util.DurationFormatter;
import me.eeshe.celeoproc.util.MessageLinkFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ElectricityStatusEmbedServiceImpl implements ElectricityStatusEmbedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedServiceImpl.class);

    private static final String TITLE = "Electricity Tracker";
    private static final String FOOTER_FORMAT = "Embed ID: %d";

    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final UserElectricityStatusRepository userElectricityStatusRepository;
    private final MessageService messageService;
    private final NicknameResolver nicknameResolver;
    private final AppSettings appSettings;
    private final JDA bot;

    public ElectricityStatusEmbedServiceImpl(
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final UserElectricityStatusRepository userElectricityStatusRepository,
            final MessageService messageService,
            final NicknameResolver nicknameResolver,
            final AppSettings appSettings,
            final JDA bot) {
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.userElectricityStatusRepository = Objects.requireNonNull(userElectricityStatusRepository,
                "UserElectricityStatusRepository must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.nicknameResolver = Objects.requireNonNull(nicknameResolver, "NicknameResolver must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public boolean sendElectricityStatusEmbed(final TextChannel channel) {
        Objects.requireNonNull(channel, "TextChannel must not be null");

        final long channelId = channel.getIdLong();
        final long guildId = channel.getGuild().getIdLong();

        channel.sendMessageEmbeds(buildElectricityStatusEmbed(List.of(), 0L, guildId))
                .addComponents(buildButtons())
                .queue(message -> {
                    final long messageId = message.getIdLong();
                    saveEmbed(messageId, guildId, channelId);
                    message.editMessageEmbeds(buildElectricityStatusEmbed(List.of(), messageId, guildId))
                            .queue(
                                    ignored -> {
                                    },
                                    throwable -> LOGGER.error(
                                            "Failed to set the footer on electricity status embed for message '{}'",
                                            messageId, throwable));
                },
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
        statusEmbed.refreshUpdatedAt();
        electricityStatusEmbedRepository.save(statusEmbed);
        channel.editMessageEmbedsById(statusEmbed.getMessageId(),
                buildElectricityStatusEmbed(statusEmbed.getParticipantUserIds(), statusEmbed.getMessageId(),
                        statusEmbed.getGuildId()))
                .queue(
                        ignored -> {
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

    @Override
    public Optional<String> getJumpUrl(final long messageId) {
        return electricityStatusEmbedRepository.get(messageId)
                .map(embed -> MessageLinkFormatter.jumpUrl(embed.getGuildId(), embed.getChannelId(), messageId));
    }

    @Override
    public List<ElectricityStatusEmbed> getByParticipantId(final long userId) {
        return electricityStatusEmbedRepository.getByParticipantId(userId);
    }

    @Override
    public List<ElectricityStatusEmbed> getByGuildId(final long guildId) {
        return electricityStatusEmbedRepository.getByGuildId(guildId);
    }

    private MessageEmbed buildElectricityStatusEmbed(
            final List<Long> participantUserIds,
            final long messageId,
            final long guildId) {
        final Map<Long, String> nicknames = nicknameResolver.resolveNicknames(guildId, participantUserIds);
        final String description = userElectricityStatusRepository.get(participantUserIds).stream()
                .map(status -> formatParticipant(status, nicknames))
                .collect(Collectors.joining("\n\n"));

        final EmbedBuilder builder = new EmbedBuilder().setTitle(TITLE);
        if (!description.isBlank()) {
            builder.setDescription(description);
        }
        if (messageId > 0L) {
            builder.setFooter(FOOTER_FORMAT.formatted(messageId));
        }
        return builder.build();
    }

    private String formatParticipant(final UserElectricityStatus status, final Map<Long, String> nicknames) {
        final String nickname = nicknames.getOrDefault(status.getUserId(), String.valueOf(status.getUserId()));
        if (status.getElectricityOut() == null) {
            return formatWithoutOutageHistory(nickname);
        }
        if (!status.hasElectricity()) {
            return formatWithoutElectricity(status, nickname);
        }
        if (status.hasElectricityOutToday(appSettings.getTimezone())) {
            return formatWithOutageToday(status, nickname);
        }
        return formatWithElectricity(status, nickname);
    }

    private String formatWithElectricity(final UserElectricityStatus status, final String nickname) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_HAS_ELECTRICITY, Map.of(
                "nickname", nickname,
                "time_since_electricity_outage_relative", formatRelativeTime(status.getElectricityIn())));
    }

    private String formatWithOutageToday(final UserElectricityStatus status, final String nickname) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_OUTAGE_TODAY, Map.of(
                "nickname", nickname,
                "out", formatTimestamp(status.getElectricityOut()),
                "in", formatTimestamp(status.getElectricityIn())));
    }

    private String formatWithoutOutageHistory(final String nickname) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_NO_OUTAGE_HISTORY, Map.of(
                "nickname", nickname));
    }

    private String formatWithoutElectricity(final UserElectricityStatus status, final String nickname) {
        return messageService.get(Message.ELECTRICITY_STATUS_EMBED_NO_ELECTRICITY, Map.of(
                "nickname", nickname,
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
        return DurationFormatter.format(Duration.between(from, Instant.now()));
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
