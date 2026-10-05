package me.eeshe.celeoproc.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class SetElectricityRegistryCommand implements BotCommand {
    public static final String NAME = "setelectricityregistry";
    private static final String EMBED_ID_OPTION = "embed_id";

    private final MessageService messageService;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final RegistryChannelService registryChannelService;

    public SetElectricityRegistryCommand(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final RegistryChannelService registryChannelService) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        Objects.requireNonNull(registryChannelService, "RegistryChannelService must not be null");

        this.messageService = messageService;
        this.electricityStatusEmbedService = electricityStatusEmbedService;
        this.registryChannelService = registryChannelService;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Register this channel to receive registry messages for an embed")
                .addOption(OptionType.STRING, EMBED_ID_OPTION, "ID of the electricity status embed", true);
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) {
            event.reply(messageService.get(Message.GUILD_ONLY))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        final String rawEmbedId = readEmbedId(event);
        final Long embedId = parseEmbedId(rawEmbedId);
        if (embedId == null) {
            replyEmbedNotFound(event, rawEmbedId);
            return;
        }
        final Optional<String> jumpUrl = electricityStatusEmbedService.getJumpUrl(embedId);
        if (jumpUrl.isEmpty()) {
            replyEmbedNotFound(event, rawEmbedId);
            return;
        }
        registryChannelService.save(embedId, event.getChannel().getIdLong());
        event.reply(messageService.get(Message.ELECTRICITY_REGISTRY_SET_SUCCESS,
                Map.of("embed_id", jumpUrl.get())))
                .setEphemeral(true)
                .queue();
    }

    private String readEmbedId(final SlashCommandInteractionEvent event) {
        final OptionMapping mapping = event.getOption(EMBED_ID_OPTION);
        return mapping == null ? "" : mapping.getAsString().trim();
    }

    private Long parseEmbedId(final String rawEmbedId) {
        try {
            return Long.parseLong(rawEmbedId);
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private void replyEmbedNotFound(final SlashCommandInteractionEvent event, final String rawEmbedId) {
        event.reply(messageService.get(Message.ELECTRICITY_REGISTRY_EMBED_NOT_FOUND,
                Map.of("embed_id", rawEmbedId)))
                .setEphemeral(true)
                .queue();
    }
}
