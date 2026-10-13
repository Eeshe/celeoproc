package me.eeshe.celeoproc.registry.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.Bot;
import me.eeshe.celeoproc.command.BotCommand;
import me.eeshe.celeoproc.command.PostElectricityStatsCommand;
import me.eeshe.celeoproc.command.PostElectricityStatusEmbedCommand;
import me.eeshe.celeoproc.command.ReloadCommand;
import me.eeshe.celeoproc.command.SetElectricityRegistryCommand;
import me.eeshe.celeoproc.command.UnsetElectricityRegistryCommand;
import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.registry.CommandRegistry;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageGraphService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.util.BotPermissions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class CommandRegistryImpl implements CommandRegistry {
    private final List<BotCommand> commandList;
    private volatile Map<String, BotCommand> commands;
    private final MessageService messageService;

    public CommandRegistryImpl(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final RegistryChannelService registryChannelService,
            final PowerOutageLogService powerOutageLogService,
            final PowerOutageGraphService powerOutageGraphService,
            final AppSettings appSettings,
            final Bot bot) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        Objects.requireNonNull(registryChannelService, "RegistryChannelService must not be null");
        Objects.requireNonNull(powerOutageLogService, "PowerOutageLogService must not be null");
        Objects.requireNonNull(powerOutageGraphService, "PowerOutageGraphService must not be null");
        Objects.requireNonNull(appSettings, "AppSettings must not be null");
        Objects.requireNonNull(bot, "Bot must not be null");

        this.messageService = messageService;
        this.commandList = new ArrayList<>();

        populateCommands(messageService, electricityStatusEmbedService, registryChannelService, powerOutageLogService,
                powerOutageGraphService, appSettings, bot);
        indexCommands();
    }

    private void populateCommands(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final RegistryChannelService registryChannelService,
            final PowerOutageLogService powerOutageLogService,
            final PowerOutageGraphService powerOutageGraphService,
            final AppSettings appSettings,
            final Bot bot) {
        commandList.add(new ReloadCommand(messageService, bot));
        commandList.add(new PostElectricityStatusEmbedCommand(messageService, electricityStatusEmbedService));
        commandList.add(new PostElectricityStatsCommand(messageService, powerOutageLogService, powerOutageGraphService,
                appSettings));
        commandList.add(new SetElectricityRegistryCommand(messageService, electricityStatusEmbedService,
                registryChannelService));
        commandList.add(new UnsetElectricityRegistryCommand(messageService, electricityStatusEmbedService,
                registryChannelService));
    }

    /**
     * Indexes the commands by their configured names, replacing the previous map
     * atomically so a failed refresh leaves the old commands in place.
     */
    private void indexCommands() {
        final Map<String, BotCommand> indexed = new HashMap<>();
        for (final BotCommand command : commandList) {
            final String name = command.createCommandData().getName();
            final BotCommand previous = indexed.put(name, command);
            if (previous != null) {
                throw new IllegalStateException("Duplicate command name '%s'".formatted(name));
            }
        }
        this.commands = indexed;
    }

    @Override
    public void refresh() {
        indexCommands();
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
        if (!event.isFromGuild()) {
            event.reply(messageService.get(Message.GUILD_ONLY))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (!BotPermissions.isAllowed(event.getMember(), botCommand.requiredPermissions())) {
            event.reply(messageService.get(Message.NO_PERMISSION))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        botCommand.run(event);
    }

    @Override
    public Collection<BotCommand> getCommands() {
        return commandList;
    }
}
