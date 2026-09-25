package me.eeshe.celeoproc.service.impl;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ElectricityStatusEmbedServiceImpl implements ElectricityStatusEmbedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedServiceImpl.class);

    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final MessageService messageService;

    public ElectricityStatusEmbedServiceImpl(
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final MessageService messageService) {
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
    }

    @Override
    public boolean sendElectricityStatusEmbed(final TextChannel channel) {
        Objects.requireNonNull(channel, "TextChannel must not be null");

        final long channelId = channel.getIdLong();
        final long guildId = channel.getGuild().getIdLong();

        channel.sendMessageEmbeds(buildPlaceholderEmbed())
                .addComponents(buildPlaceholderButtons())
                .queue(message -> saveEmbed(message.getIdLong(), guildId, channelId),
                        throwable -> LOGGER.error(
                                "Failed to send electricity status embed to channel '{}'", channelId, throwable));
        return true;
    }

    @Override
    public void deleteElectricityStatusEmbed(final long messageId) {
        electricityStatusEmbedRepository.delete(messageId);
    }

    private MessageEmbed buildPlaceholderEmbed() {
        return new EmbedBuilder()
                .setTitle("Electricity Status")
                .build();
    }

    private ActionRow buildPlaceholderButtons() {
        return ActionRow.of(
                Button.danger(BUTTON_OUT_ID, messageService.get(Message.ELECTRICITY_STATUS_EMBED_BUTTON_OUT)),
                Button.success(BUTTON_IN_ID, messageService.get(Message.ELECTRICITY_STATUS_EMBED_BUTTON_IN)));
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
