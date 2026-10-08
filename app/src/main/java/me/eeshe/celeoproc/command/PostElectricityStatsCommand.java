package me.eeshe.celeoproc.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.OutageExtreme;
import me.eeshe.celeoproc.model.OutageStats;
import me.eeshe.celeoproc.model.PowerOutageStats;
import me.eeshe.celeoproc.model.UserOutageStats;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.util.DurationFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
    private final AppSettings appSettings;

    public PostElectricityStatsCommand(
            final MessageService messageService,
            final PowerOutageLogService powerOutageLogService,
            final AppSettings appSettings) {
        Objects.requireNonNull(messageService, "MessageService must not be null");
        Objects.requireNonNull(powerOutageLogService, "PowerOutageLogService must not be null");
        Objects.requireNonNull(appSettings, "AppSettings must not be null");

        this.messageService = messageService;
        this.powerOutageLogService = powerOutageLogService;
        this.appSettings = appSettings;
    }

    @Override
    public CommandData createCommandData() {
        return Commands.slash(NAME, "Show power outage stats for a date range")
                .addOption(OptionType.STRING, START_DATE_OPTION, "Start date in YYYY-MM-DD format", true)
                .addOption(OptionType.STRING, END_DATE_OPTION, "End date in YYYY-MM-DD format", true);
    }

    @Override
    public void run(final SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(messageService.get(Message.GUILD_ONLY))
                    .setEphemeral(true)
                    .queue();
            return;
        }

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

        final PowerOutageStats stats = powerOutageLogService.getWithinRange(guild.getIdLong(), startDate, endDate);

        if (stats.server().logAmount() > 0) {
            event.replyEmbeds(buildEmbed(Message.ELECTRICITY_STATS_SERVER_TITLE,
                    buildServerDescription(stats.server()), startDate, endDate)).queue();
        } else {
            event.reply(messageService.get(Message.ELECTRICITY_STATS_SERVER_NO_DATA))
                    .setEphemeral(true)
                    .queue();
        }

        if (stats.global().logAmount() > 0) {
            event.getHook().sendMessageEmbeds(buildEmbed(Message.ELECTRICITY_STATS_GLOBAL_TITLE,
                    buildGlobalDescription(stats.global()), startDate, endDate)).queue();
        } else {
            event.getHook().sendMessage(messageService.get(Message.ELECTRICITY_STATS_GLOBAL_NO_DATA))
                    .setEphemeral(true)
                    .queue();
        }
    }

    private MessageEmbed buildEmbed(
            final Message titleMessage,
            final String description,
            final LocalDate startDate,
            final LocalDate endDate) {
        final EmbedBuilder builder = new EmbedBuilder()
                .setTitle(messageService.get(titleMessage))
                .setDescription(description)
                .setFooter("%s -> %s".formatted(startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER)));

        final String imageUrl = appSettings.getStatsImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            builder.setThumbnail(imageUrl);
        }
        return builder.build();
    }

    private String buildServerDescription(final OutageStats stats) {
        final List<String> generalLines = new ArrayList<>();
        generalLines.add(messageService.get(Message.ELECTRICITY_STATS_SERVER_TOTAL_OUTAGES, Map.of(
                "log_amount", String.valueOf(stats.logAmount()))));
        generalLines.add(messageService.get(Message.ELECTRICITY_STATS_SERVER_TOTAL_TIME, Map.of(
                "total_power_outage_time", DurationFormatter.format(stats.totalTime()))));

        final OutageExtreme longest = stats.longestOutage();
        generalLines.add(messageService.get(Message.ELECTRICITY_STATS_SERVER_LONGEST, Map.of(
                "nickname", longest.nickname(),
                "longest_power_outage_time", DurationFormatter.format(longest.duration()))));
        final OutageExtreme shortest = stats.shortestOutage();
        generalLines.add(messageService.get(Message.ELECTRICITY_STATS_SERVER_SHORTEST, Map.of(
                "nickname", shortest.nickname(),
                "shortest_power_outage_time", DurationFormatter.format(shortest.duration()))));

        final UserOutageStats awardWinner = stats.awardWinner();
        generalLines.add(messageService.get(Message.ELECTRICITY_STATS_SERVER_AWARD, Map.of(
                "nickname", awardWinner.nickname(),
                "user_total_power_outages", String.valueOf(awardWinner.logAmount()),
                "user_total_power_outage_time", DurationFormatter.format(awardWinner.totalTime()))));

        final StringBuilder description = new StringBuilder()
                .append(messageService.get(Message.ELECTRICITY_STATS_SERVER_GENERAL_HEADER))
                .append("\n\n")
                .append(String.join("\n", generalLines));

        if (!stats.userStats().isEmpty()) {
            final List<String> userEntries = stats.userStats().stream()
                    .map(this::formatUserEntry)
                    .toList();
            description.append("\n\n")
                    .append(messageService.get(Message.ELECTRICITY_STATS_SERVER_USER_HEADER))
                    .append("\n")
                    .append(String.join("\n\n", userEntries));
        }
        return description.toString();
    }

    private String buildGlobalDescription(final OutageStats stats) {
        final String totals = String.join("\n",
                messageService.get(Message.ELECTRICITY_STATS_GLOBAL_TOTAL_OUTAGES, Map.of(
                        "log_amount", String.valueOf(stats.logAmount()))),
                messageService.get(Message.ELECTRICITY_STATS_GLOBAL_TOTAL_TIME, Map.of(
                        "total_power_outage_time", DurationFormatter.format(stats.totalTime()))),
                messageService.get(Message.ELECTRICITY_STATS_GLOBAL_AVERAGE_TIME, Map.of(
                        "average_power_outage_time", DurationFormatter.format(stats.averageTime()))));
        return totals + "\n\n" + messageService.get(Message.ELECTRICITY_STATS_GLOBAL_DISCLAIMER);
    }

    private String formatUserEntry(final UserOutageStats user) {
        return messageService.get(Message.ELECTRICITY_STATS_SERVER_USER_ENTRY, Map.of(
                "user_nickname", user.nickname(),
                "user_log_amount", String.valueOf(user.logAmount()),
                "total_power_outage_time", DurationFormatter.format(user.totalTime()),
                "average_power_outage_time", DurationFormatter.format(user.averageTime()),
                "longest_power_outage_time", DurationFormatter.format(user.longestTime()),
                "shortest_power_outage_time", DurationFormatter.format(user.shortestTime())));
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
