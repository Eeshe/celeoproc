package me.eeshe.celeoproc.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import me.eeshe.celeoproc.model.PowerOutageLog;
import net.dv8tion.jda.api.entities.Message;

/**
 * Sends electricity status updates to the registry channels.
 */
public interface ElectricityRegistryService {

    /**
     * Announces that the given user no longer has electricity.
     *
     * @param userId                id of the user whose status changed
     * @param previousElectricityIn moment the user's electricity last came back,
     *                              or {@code null} when it was never recorded
     * @return a future of every sent registry message, or an empty list when no
     *         registry channel is configured
     */
    List<CompletableFuture<Message>> sendElectricityOut(long userId, Instant previousElectricityIn);

    /**
     * Announces that the given user has electricity back.
     *
     * @param log the persisted log of the outage that just ended
     * @return a future of every sent registry message, or an empty list when no
     *         registry channel is configured
     */
    List<CompletableFuture<Message>> sendElectricityIn(PowerOutageLog log);

    /**
     * Edits every registry message that announced the given log, across all
     * channels and channels it was sent to, so they reflect the log's updated
     * data. Components of the edited messages are left intact.
     *
     * @param log updated log
     */
    void editRegistryMessages(PowerOutageLog log);

    /**
     * Deletes every registry message that announced the given log, across all
     * channels it was sent to.
     *
     * @param log log whose registry messages are deleted
     */
    void deleteRegistryMessages(PowerOutageLog log);
}
