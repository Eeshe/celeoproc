package me.eeshe.celeoproc.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class PostElectricityStatsCommand implements BotCommand {
    public static final String NAME = "postelectricitystats";
    private static final String START_DATE_OPTION = "start_date";
    private static final String END_DATE_OPTION = "end_date";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MessageService messageService;
    private final PowerOutageLogService powerOutageLogService;

    public PostElectricityStatsCommand(
            final MessageService messageService,
            final PowerOutageLogService powerOutageLogService) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(powerOutageLogService, "PowerOutageLogService must not be null");

        this.messageService = messageService;
        this.powerOutageLogService = powerOutageLogService;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Show power outage stats for a date range")
                .addOption(OptionType.STRING, START_DATE_OPTION, "Start date in YYYY-MM-DD format", true)
                .addOption(OptionType.STRING, END_DATE_OPTION, "End date in YYYY-MM-DD format", true);
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        final String rawStartDate = readOption(event, START_DATE_OPTION);
        final LocalDate startDate = parseDate(rawStartDate);
        if (startDate == null) {
            replyInvalidDate(event, rawStartDate);
            return;
        }
        final String rawEndDate = readOption(event, END_DATE_OPTION);
        final LocalDate endDate = parseDate(rawEndDate);
        if (endDate == null) {
            replyInvalidDate(event, rawEndDate);
            return;
        }
        if (startDate.isAfter(endDate)) {
            event.reply(messageService.get(Message.POST_ELECTRICITY_STATS_INVALID_RANGE))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final List<PowerOutageLog> logs = powerOutageLogService.getWithinRange(startDate, endDate);
        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageService.get(Message.POST_ELECTRICITY_STATS_EMBED_TITLE))
                .setDescription(messageService.get(Message.POST_ELECTRICITY_STATS_EMBED_DESCRIPTION, Map.of(
                        "start_date", startDate.format(DATE_FORMATTER),
                        "end_date", endDate.format(DATE_FORMATTER),
                        "log_count", String.valueOf(logs.size()))));
        event.replyEmbeds(embed.build()).queue();
    }

    private String readOption(final SlashCommandInteractionEvent event, final String optionName) {
        final OptionMapping mapping = event.getOption(optionName);
        return mapping == null ? "" : mapping.getAsString().trim();
    }

    private LocalDate parseDate(final String rawDate) {
        try {
            return LocalDate.parse(rawDate, DATE_FORMATTER);
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private void replyInvalidDate(final SlashCommandInteractionEvent event, final String rawDate) {
        event.reply(messageService.get(Message.POST_ELECTRICITY_STATS_INVALID_DATE,
                Map.of("input", rawDate)))
                .setEphemeral(true)
                .queue();
    }
}
