package me.eeshe.celeoproc.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Executable Discord slash command.
 */
public interface BotCommand {

    CommandData createCommandData();

    void run(SlashCommandInteractionEvent event);
}
