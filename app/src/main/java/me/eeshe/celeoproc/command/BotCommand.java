package me.eeshe.celeoproc.command;

import java.util.Set;

import me.eeshe.celeoproc.util.BotPermissions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Executable Discord slash command.
 */
public interface BotCommand {

    /**
     * Permission a member must hold to run a command, unless the command
     * overrides {@link #requiredPermissions()}.
     */
    Permission REQUIRED_PERMISSION = BotPermissions.REQUIRED_PERMISSION;

    CommandData createCommandData();

    /**
     * @return permissions a member must hold to run this command
     */
    default Set<Permission> requiredPermissions() {
        return Set.of(REQUIRED_PERMISSION);
    }

    void run(SlashCommandInteractionEvent event);
}
