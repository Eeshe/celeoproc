package me.eeshe.celeoproc.registry.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.Bot;
import me.eeshe.celeoproc.command.BotCommand;
import me.eeshe.celeoproc.command.CeleoprocCommand;
import me.eeshe.celeoproc.command.PostElectricityStatusEmbedCommand;
import me.eeshe.celeoproc.command.ReloadCommand;
import me.eeshe.celeoproc.registry.CommandRegistry;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class CommandRegistryImpl implements CommandRegistry {
    private final Map<String, BotCommand> commands;

    public CommandRegistryImpl(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final Bot bot) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        Objects.requireNonNull(bot, "Bot must not be null");

        this.commands = new HashMap<>();

        populateCommands(messageService, electricityStatusEmbedService, bot);
    }

    private void populateCommands(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final Bot bot) {
        commands.put(CeleoprocCommand.NAME, new CeleoprocCommand(messageService));
        commands.put(ReloadCommand.NAME, new ReloadCommand(messageService, bot));
        commands.put(PostElectricityStatusEmbedCommand.NAME,
                new PostElectricityStatusEmbedCommand(messageService, electricityStatusEmbedService));
    }

    @Override
    public boolean isCommand(final String command) {
        return commands.containsKey(command);
    }

    @Override
    public void runCommand(final String command, final SlashCommandInteractionEvent event) {
        final BotCommand botCommand = commands.get(command);
        if (botCommand == null) {
            return;
        }
        botCommand.run(event);
    }

    @Override
    public Collection<BotCommand> getCommands() {
        return commands.values();
    }
}
