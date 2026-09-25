package me.eeshe.celeoproc.listener;

import java.util.Objects;

import me.eeshe.celeoproc.registry.CommandRegistry;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class CommandListener extends ListenerAdapter {
    private final CommandRegistry commandRegistry;

    public CommandListener(final CommandRegistry commandRegistry) {
        Objects.requireNonNull(commandRegistry, "CommandRegistry must not be null");

        this.commandRegistry = commandRegistry;
    }

    @Override
    public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
        final String name = event.getName();
        if (!commandRegistry.isCommand(name)) {
            return;
        }
        commandRegistry.runCommand(name, event);
    }
}
