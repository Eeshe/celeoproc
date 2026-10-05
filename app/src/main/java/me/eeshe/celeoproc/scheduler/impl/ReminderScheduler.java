package me.eeshe.celeoproc.scheduler.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.scheduler.BotScheduler;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import me.eeshe.celeoproc.util.MessageLinkFormatter;
import net.dv8tion.jda.api.JDA;

/**
 * Periodically sends a private reminder to every user that still has no
 * electricity, linking the embeds they participate in so they can update their
 * status.
 */
public final class ReminderScheduler implements BotScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReminderScheduler.class);

    private static final long CHECK_INTERVAL_MINUTES = 1L;

    private final UserElectricityStatusService userElectricityStatusService;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final MessageService messageService;
    private final JDA bot;

    private volatile ScheduledExecutorService executor;

    public ReminderScheduler(
            final UserElectricityStatusService userElectricityStatusService,
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final MessageService messageService,
            final JDA bot) {
        this.userElectricityStatusService = Objects.requireNonNull(userElectricityStatusService,
                "UserElectricityStatusService must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public void start() {
        if (executor != null) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(
                this::tick,
                0L,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
        LOGGER.info("Reminder scheduler started");
    }

    @Override
    public void shutdown() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        executor = null;
        LOGGER.info("Reminder scheduler stopped");
    }

    private void tick() {
        try {
            sendPendingReminders();
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to send electricity reminders", exception);
        }
    }

    private void sendPendingReminders() {
        for (final UserElectricityStatus status : userElectricityStatusService.getPendingReminders()) {
            final List<ElectricityStatusEmbed> statusEmbeds = electricityStatusEmbedService
                    .getByParticipantId(status.getUserId());
            if (statusEmbeds.isEmpty()) {
                continue;
            }
            sendReminder(status, statusEmbeds);
        }
    }

    private void sendReminder(final UserElectricityStatus status, final List<ElectricityStatusEmbed> statusEmbeds) {
        final String content = messageService.get(Message.ELECTRICITY_STATUS_REMINDER,
                Map.of("embeds", formatEmbeds(statusEmbeds)));

        bot.retrieveUserById(status.getUserId()).queue(
                user -> user.openPrivateChannel().queue(
                        channel -> channel.sendMessage(content).queue(
                                ignored -> userElectricityStatusService.markReminderSent(status),
                                throwable -> LOGGER.error("Failed to send reminder to user '{}'",
                                        status.getUserId(), throwable)),
                        throwable -> LOGGER.error("Failed to open private channel with user '{}'",
                                status.getUserId(), throwable)),
                throwable -> LOGGER.error("Failed to retrieve user '{}'", status.getUserId(), throwable));
    }

    private String formatEmbeds(final List<ElectricityStatusEmbed> statusEmbeds) {
        return statusEmbeds.stream()
                .map(embed -> MessageLinkFormatter.jumpUrl(embed.getGuildId(), embed.getChannelId(),
                        embed.getMessageId()))
                .collect(Collectors.joining("\n"));
    }
}
