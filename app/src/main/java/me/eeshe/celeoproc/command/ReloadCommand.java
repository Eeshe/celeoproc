package me.eeshe.celeoproc.command;

import java.util.Objects;

import me.eeshe.celeoproc.Bot;
import me.eeshe.celeoproc.config.JsonConfigLoader;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class ReloadCommand implements BotCommand {
    public static final String NAME = "reload";

    private final MessageService messageService;
    private final Bot bot;

    public ReloadCommand(final MessageService messageService, final Bot bot) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(bot, "Bot must not be null");
        this.messageService = messageService;
        this.bot = bot;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Reload bot configuration");
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        boolean success = true;
        for (final JsonConfigLoader config : bot.getConfigs()) {
            if (!config.reload()) {
                success = false;
            }
        }

        event.reply(messageService.get(success ? Message.RELOAD_SUCCESS : Message.RELOAD_ERROR))
                .setEphemeral(true)
                .queue();
    }
}
