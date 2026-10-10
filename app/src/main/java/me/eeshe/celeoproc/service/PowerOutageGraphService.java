package me.eeshe.celeoproc.service;

import java.nio.file.Path;
import java.util.List;

import me.eeshe.celeoproc.model.PowerOutageLog;

/**
 * Renders power outage intervals as PNG charts.
 *
 * <p>
 * Two kinds of charts are produced for the server scoped logs: one chart per
 * user per month, and one combined chart per month containing every user.
 */
public interface PowerOutageGraphService {

    /**
     * Generates every graph for the given server logs. The charts are stored as
     * temporary files and must be removed with {@link #deleteGraphs(List)} once
     * they have been sent.
     *
     * @param guildId guild the logs belong to, used to resolve nicknames
     * @param logs    server scoped logs to draw
     * @return paths of the generated PNG files, empty when there is nothing to
     *         draw
     */
    List<Path> generateGraphs(long guildId, List<PowerOutageLog> logs);

    /**
     * Deletes the given graph files, along with the now empty directories that
     * held them.
     *
     * @param graphs paths previously returned by
     *               {@link #generateGraphs(long, List)}
     */
    void deleteGraphs(List<Path> graphs);
}
