package me.eeshe.celeoproc.service;

import java.util.List;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;

/**
 * Manages the registry messages that announce a {@link me.eeshe.celeoproc.model.PowerOutageLog}.
 */
public interface PowerOutageRegistryMessageService {

    /**
     * Inserts every given registry message in a single batch.
     *
     * @param registryMessages rows to persist
     */
    void saveAll(List<PowerOutageRegistryMessage> registryMessages);

    /**
     * Updates the electricity in message id of every given row in a single batch.
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
