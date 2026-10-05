package me.eeshe.celeoproc.scheduler.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.scheduler.BotScheduler;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;

/**
 * Periodically refreshes every stored {@link ElectricityStatusEmbed} whose last
 * update is older than the configured update frequency.
 */
public final class ElectricityStatusEmbedScheduler implements BotScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedScheduler.class);

    private static final long CHECK_INTERVAL_MINUTES = 1L;

    private final AppSettings appSettings;
    private final ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;

    private volatile ScheduledExecutorService executor;

    public ElectricityStatusEmbedScheduler(
            final AppSettings appSettings,
            final ElectricityStatusEmbedRepository electricityStatusEmbedRepository,
            final ElectricityStatusEmbedService electricityStatusEmbedService) {
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.electricityStatusEmbedRepository = Objects.requireNonNull(electricityStatusEmbedRepository,
                "ElectricityStatusEmbedRepository must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
    }

    @Override
    public void start() {
        if (executor != null) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "electricity-status-embed-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(
                this::tick,
                0L,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
        LOGGER.info("Electricity status embed scheduler started");
    }

    @Override
    public void shutdown() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        executor = null;
        LOGGER.info("Electricity status embed scheduler stopped");
    }

    private void tick() {
        try {
            refreshStaleEmbeds();
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to refresh electricity status embeds", exception);
        }
    }

    private void refreshStaleEmbeds() {
        final Duration frequency = appSettings.getElectricityStatusEmbedUpdateFrequency();
        final Instant threshold = Instant.now().minus(frequency);

        for (final ElectricityStatusEmbed statusEmbed : electricityStatusEmbedRepository.getStaleEmbeds(threshold)) {
            electricityStatusEmbedService.updateElectricityStatusEmbed(statusEmbed);
        }
    }
}
