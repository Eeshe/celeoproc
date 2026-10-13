package me.eeshe.celeoproc.registry;

import java.util.Collection;

import me.eeshe.celeoproc.command.BotCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Holds and dispatches the available slash commands.
 */
public interface CommandRegistry {

    boolean isCommand(String command);

    /**
     * Re-indexes the commands against the current configuration, picking up any
     * changed command names.
     */
    void refresh();

    void runCommand(String command, SlashCommandInteractionEvent event);

    Collection<BotCommand> getCommands();
}
