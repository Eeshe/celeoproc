package me.eeshe.celeoproc.command;

import java.util.Objects;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class PostElectricityStatusEmbedCommand implements BotCommand {
    public static final String NAME = "postelectricitystatusembed";

    private final MessageService messageService;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;

    public PostElectricityStatusEmbedCommand(
            final MessageService messageService,
            final ElectricityStatusEmbedService electricityStatusEmbedService) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");

        this.messageService = messageService;
        this.electricityStatusEmbedService = electricityStatusEmbedService;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Post the electricity status embed");
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) {
            event.reply(messageService.get(Message.GUILD_ONLY))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        final TextChannel channel = event.getChannel().asTextChannel();
        if (!electricityStatusEmbedService.sendElectricityStatusEmbed(channel)) {
            event.reply(messageService.get(Message.GENERIC_ERROR))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.deferReply(true).queue(hook -> hook.deleteOriginal().queue());
    }
}
