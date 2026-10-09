package me.eeshe.celeoproc.service;

import java.time.Instant;

import me.eeshe.celeoproc.model.PowerOutageLog;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

/**
 * Sends electricity status updates to the registry channels.
 */
public interface ElectricityRegistryService {

    /**
     * Announces that the given user no longer has electricity.
     *
     * @param userId                 id of the user whose status changed
     * @param previousElectricityIn  moment the user's electricity last came back,
     *                               or {@code null} when it was never recorded
     */
    void sendElectricityOut(long userId, Instant previousElectricityIn);

    /**
     * Announces that the given user has electricity back.
     *
     * @param log the persisted log of the outage that just ended
     */
    void sendElectricityIn(PowerOutageLog log);

    /**
     * Edits the registry message that announced the given log so it reflects the
     * log's updated data, keeping its components intact.
     *
     * @param message registry message to edit
     * @param log     updated log
     */
    void editElectricityIn(Message message, PowerOutageLog log);

    /**
     * Deletes the registry message that announced a log.
     *
     * @param channel   channel the message belongs to
     * @param messageId id of the message to delete
     */
    void deleteElectricityIn(MessageChannel channel, long messageId);
}
