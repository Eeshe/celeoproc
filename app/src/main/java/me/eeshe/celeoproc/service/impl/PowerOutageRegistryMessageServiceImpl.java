package me.eeshe.celeoproc.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;
import me.eeshe.celeoproc.repository.PowerOutageRegistryMessageRepository;
import me.eeshe.celeoproc.service.PowerOutageRegistryMessageService;

public final class PowerOutageRegistryMessageServiceImpl implements PowerOutageRegistryMessageService {
    private final PowerOutageRegistryMessageRepository powerOutageRegistryMessageRepository;

    public PowerOutageRegistryMessageServiceImpl(
            final PowerOutageRegistryMessageRepository powerOutageRegistryMessageRepository) {
        this.powerOutageRegistryMessageRepository = Objects.requireNonNull(powerOutageRegistryMessageRepository,
                "PowerOutageRegistryMessageRepository must not be null");
    }

    @Override
    public void saveAll(final List<PowerOutageRegistryMessage> registryMessages) {
        powerOutageRegistryMessageRepository.saveAll(registryMessages);
    }

    @Override
    public void updateAll(final List<PowerOutageRegistryMessage> registryMessages) {
        powerOutageRegistryMessageRepository.updateAll(registryMessages);
    }

    @Override
    public List<PowerOutageRegistryMessage> getByPowerOutageLogId(final UUID powerOutageLogId) {
        return powerOutageRegistryMessageRepository.getByPowerOutageLogId(powerOutageLogId);
    }
}
