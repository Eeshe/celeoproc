package me.eeshe.celeoproc.repository;

import java.util.List;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;

/**
 * Persists {@link PowerOutageRegistryMessage} entities.
 */
public interface PowerOutageRegistryMessageRepository extends Repository {

    /**
     * Inserts every given registry message in a single batch. Rows whose
     * electricity out message id is already stored are overwritten.
     *
     * @param registryMessages rows to persist
     */
    void saveAll(List<PowerOutageRegistryMessage> registryMessages);

    /**
     * Updates the electricity in message id of every given row in a single batch.
     * Rows are matched by their electricity out message id.
     *
     * @param registryMessages rows to update
     */
    void updateAll(List<PowerOutageRegistryMessage> registryMessages);

    /**
     * @param powerOutageLogId id of the outage the rows belong to
     * @return every stored registry message of the given outage
     */
    List<PowerOutageRegistryMessage> getByPowerOutageLogId(UUID powerOutageLogId);
}
