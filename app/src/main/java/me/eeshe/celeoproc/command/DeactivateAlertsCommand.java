package me.eeshe.celeoproc.command;

import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class DeactivateAlertsCommand implements BotCommand {
    private final MessageService messageService;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final RegistryChannelService registryChannelService;

    public DeactivateAlertsCommand(
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
        return Commands.slash(messageService.get(Message.COMMAND_DEACTIVATE_ALERTS_NAME),
                messageService.get(Message.COMMAND_DEACTIVATE_ALERTS_DESCRIPTION))
                .addOption(OptionType.STRING,
                        messageService.get(Message.COMMAND_DEACTIVATE_ALERTS_OPTION_EMBED_ID_NAME),
                        messageService.get(Message.COMMAND_DEACTIVATE_ALERTS_OPTION_EMBED_ID_DESCRIPTION), true);
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        final String rawEmbedId = readEmbedId(event);
        final Long embedId = parseEmbedId(rawEmbedId);
        final long channelId = event.getChannel().getIdLong();
        if (embedId == null || !registryChannelService.isRegistered(embedId, channelId)) {
            event.reply(messageService.get(Message.ELECTRICITY_REGISTRY_UNSET_NOT_REGISTERED,
                    Map.of("embed_id", rawEmbedId)))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        registryChannelService.delete(embedId);
        final String reference = electricityStatusEmbedService.getJumpUrl(embedId)
                .orElse(String.valueOf(embedId));
        event.reply(messageService.get(Message.ELECTRICITY_REGISTRY_UNSET_SUCCESS,
                Map.of("embed_id", reference)))
                .setEphemeral(true)
                .queue();
    }

    private String readEmbedId(final SlashCommandInteractionEvent event) {
        final OptionMapping mapping = event.getOption(
                messageService.get(Message.COMMAND_DEACTIVATE_ALERTS_OPTION_EMBED_ID_NAME));
        return mapping == null ? "" : mapping.getAsString().trim();
    }

    private Long parseEmbedId(final String rawEmbedId) {
        try {
            return Long.parseLong(rawEmbedId);
        } catch (final NumberFormatException exception) {
            return null;
        }
    }
}
