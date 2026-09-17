package me.eeshe.celeoproc.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.Bot;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class CommandRegistryImpl implements CommandRegistry {
    private final Map<String, BotCommand> commands;

    public CommandRegistryImpl(final MessageService messageService, final Bot bot) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(bot, "Bot must not be null");

        this.commands = new HashMap<>();

        populateCommands(messageService, bot);
    }

    private void populateCommands(final MessageService messageService, final Bot bot) {
        commands.put(CeleoprocCommand.NAME, new CeleoprocCommand(messageService));
        commands.put(ReloadCommand.NAME, new ReloadCommand(messageService, bot));
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
