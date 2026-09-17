package me.eeshe.celeoproc.command;

import java.util.Collection;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Holds and dispatches the available slash commands.
 */
public interface CommandRegistry {

    boolean isCommand(String command);

    void runCommand(String command, SlashCommandInteractionEvent event);

    Collection<BotCommand> getCommands();
}
