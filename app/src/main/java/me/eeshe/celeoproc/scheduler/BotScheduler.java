package me.eeshe.celeoproc.scheduler;

/**
 * Lifecycle contract for background schedulers managed by {@link me.eeshe.celeoproc.Bot}.
 */
public interface BotScheduler {

    /**
     * Starts the scheduler. Implementations should be resilient to repeated calls.
     */
    void start();

    /**
     * Stops the scheduler and releases its resources. Implementations should be
     * resilient to repeated calls.
     */
    void shutdown();
}
