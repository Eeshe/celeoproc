package me.eeshe.celeoproc.command;

import java.util.Objects;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class CeleoprocCommand implements BotCommand {
    public static final String NAME = "celeoproc";

    private final MessageService messageService;

    public CeleoprocCommand(final MessageService messageService) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        this.messageService = messageService;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Celeoproc command");
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        event.reply(messageService.get(Message.BOT_INFO))
                .setEphemeral(true)
                .queue();
    }
}
