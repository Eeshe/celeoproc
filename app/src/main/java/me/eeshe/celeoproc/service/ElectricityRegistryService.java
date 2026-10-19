package me.eeshe.celeoproc.service;

import java.time.Instant;

import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.RegistrySendResult;

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
     * @return the futures of every sent registry message together with the
     *         channels skipped because the bot lacks permissions, or empty lists
     *         when no registry channel is configured
     */
    RegistrySendResult sendElectricityOut(long userId, Instant previousElectricityIn);

    /**
     * Announces that the given user has electricity back.
     *
     * @param log the persisted log of the outage that just ended
     * @return the futures of every sent registry message together with the
     *         channels skipped because the bot lacks permissions, or empty lists
     *         when no registry channel is configured
     */
    RegistrySendResult sendElectricityIn(PowerOutageLog log);

    /**
     * Edits every registry message that announced the given log, across all
     * channels and channels it was sent to, so they reflect the log's updated
     * data. Components of the edited messages are left intact.
     *
     * <p>
     * Asynchronous: the registry message rows are snapshotted before returning,
     * while the channel and nickname resolution and the actual edits run on the
     * interaction executor.
     *
     * @param log updated log
     */
    void editRegistryMessages(PowerOutageLog log);

    /**
     * Deletes every registry message that announced the given log, across all
     * channels it was sent to.
     *
     * <p>
     * Asynchronous: the registry message rows are snapshotted before returning
     * (they cascade away with the log), while the actual deletions run on the
     * interaction executor.
     *
     * @param log log whose registry messages are deleted
     */
    void deleteRegistryMessages(PowerOutageLog log);
}
