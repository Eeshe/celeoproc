package me.eeshe.celeoproc.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Links a {@link PowerOutageLog} to the registry messages that announced it.
 *
 * <p>
 * One row is stored per registry channel. The electricity out message is sent
 * (and therefore known) as soon as the outage starts, while the electricity in
 * message is only known once the user marks their electricity back in, so
 * {@link #registryInMessageId()} is {@code null} until then.
 *
 * @param registryOutMessageId id of the message announcing the electricity went
 *                             out
 * @param registryInMessageId  id of the message announcing the electricity came
 *                             back, or {@code null} while the outage is ongoing
 * @param powerOutageLogId     id of the outage the messages belong to
 * @param channelId            id of the channel the messages were sent to
 */
public record PowerOutageRegistryMessage(
        long registryOutMessageId,
        Long registryInMessageId,
        UUID powerOutageLogId,
        long channelId) {

    public PowerOutageRegistryMessage {
        Objects.requireNonNull(powerOutageLogId, "Power outage log id must not be null");
    }
}
