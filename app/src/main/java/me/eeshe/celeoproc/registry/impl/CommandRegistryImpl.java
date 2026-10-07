package me.eeshe.celeoproc.registry.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.Bot;
import me.eeshe.celeoproc.command.BotCommand;
import me.eeshe.celeoproc.command.CeleoprocCommand;
import me.eeshe.celeoproc.command.PostElectricityStatsCommand;
import me.eeshe.celeoproc.command.PostElectricityStatusEmbedCommand;
import me.eeshe.celeoproc.command.ReloadCommand;
import me.eeshe.celeoproc.command.SetElectricityRegistryCommand;
import me.eeshe.celeoproc.command.UnsetElectricityRegistryCommand;
import me.eeshe.celeoproc.registry.CommandRegistry;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class CommandRegistryImpl implements CommandRegistry {
    private final Map<String, BotCommand> commands;

    public CommandRegistryImpl(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final RegistryChannelService registryChannelService,
            final PowerOutageLogService powerOutageLogService,
            final Bot bot) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        Objects.requireNonNull(registryChannelService, "RegistryChannelService must not be null");
        Objects.requireNonNull(powerOutageLogService, "PowerOutageLogService must not be null");
        Objects.requireNonNull(bot, "Bot must not be null");

        this.commands = new HashMap<>();

        populateCommands(messageService, electricityStatusEmbedService, registryChannelService, powerOutageLogService,
                bot);
    }

    private void populateCommands(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final RegistryChannelService registryChannelService,
            final PowerOutageLogService powerOutageLogService,
            final Bot bot) {
        commands.put(CeleoprocCommand.NAME, new CeleoprocCommand(messageService));
        commands.put(ReloadCommand.NAME, new ReloadCommand(messageService, bot));
        commands.put(PostElectricityStatusEmbedCommand.NAME,
                new PostElectricityStatusEmbedCommand(messageService, electricityStatusEmbedService));
        commands.put(PostElectricityStatsCommand.NAME,
                new PostElectricityStatsCommand(messageService, powerOutageLogService));
        commands.put(SetElectricityRegistryCommand.NAME,
                new SetElectricityRegistryCommand(messageService, electricityStatusEmbedService,
                        registryChannelService));
        commands.put(UnsetElectricityRegistryCommand.NAME,
                new UnsetElectricityRegistryCommand(messageService, electricityStatusEmbedService,
                        registryChannelService));
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
