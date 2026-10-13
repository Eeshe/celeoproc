package me.eeshe.celeoproc.command;

import java.util.Objects;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class PostElectricityStatusEmbedCommand implements BotCommand {
    private final MessageService messageService;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;

    public PostElectricityStatusEmbedCommand(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");

        this.messageService = messageService;
        this.electricityStatusEmbedService = electricityStatusEmbedService;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(messageService.get(Message.COMMAND_POSTEMBED_NAME),
                messageService.get(Message.COMMAND_POSTEMBED_DESCRIPTION));
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        final TextChannel channel = event.getChannel().asTextChannel();
        if (!electricityStatusEmbedService.sendElectricityStatusEmbed(channel)) {
            event.reply(messageService.get(Message.GENERIC_ERROR))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.deferReply(true).queue(hook -> hook.deleteOriginal().queue());
    }
}
