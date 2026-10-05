package me.eeshe.celeoproc.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.repository.RegistryChannelRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.RegistryChannelService;

public final class RegistryChannelServiceImpl implements RegistryChannelService {
    private final RegistryChannelRepository registryChannelRepository;
    private final ElectricityStatusEmbedService electricityStatusEmbedService;

    public RegistryChannelServiceImpl(
            final RegistryChannelRepository registryChannelRepository,
            final ElectricityStatusEmbedService electricityStatusEmbedService) {
        this.registryChannelRepository = Objects.requireNonNull(registryChannelRepository,
                "RegistryChannelRepository must not be null");
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
    }

    @Override
    public void save(final long embedId, final long channelId) {
        registryChannelRepository.save(embedId, channelId);
    }

    @Override
    public void delete(final long embedId) {
        registryChannelRepository.delete(embedId);
    }

    @Override
    public List<Long> getChannelIdsByEmbedId(final long embedId) {
        return registryChannelRepository.getByEmbedId(embedId);
    }

    @Override
    public List<Long> getEmbedIdsByChannelId(final long channelId) {
        return registryChannelRepository.getByChannelId(channelId);
    }

    @Override
    public List<Long> getByParticipantId(final long userId) {
        final Set<Long> channelIds = new LinkedHashSet<>();
        for (final ElectricityStatusEmbed statusEmbed : electricityStatusEmbedService.getByParticipantId(userId)) {
            channelIds.addAll(registryChannelRepository.getByEmbedId(statusEmbed.getMessageId()));
        }
        return new ArrayList<>(channelIds);
    }

    @Override
    public boolean isRegistered(final long embedId, final long channelId) {
        return getEmbedIdsByChannelId(channelId).contains(embedId);
    }
}
