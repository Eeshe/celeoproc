package me.eeshe.celeoproc.repository;

import java.util.List;

/**
 * Persists the registry channel configuration linking electricity status
 * embeds to the text channels that receive their registry messages.
 */
public interface RegistryChannelRepository extends Repository {

    /**
     * Inserts the given registry channel, or updates the existing row when the
     * embed id is already present.
     *
     * @param embedId   id of the electricity status embed
     * @param channelId id of the registry text channel
     */
    void save(long embedId, long channelId);

    /**
     * Deletes the registry channel associated with the given embed, if present.
     *
     * @param embedId id of the electricity status embed
     */
    void delete(long embedId);

    /**
     * @param embedId id of the electricity status embed
     * @return ids of the text channels registered for the embed
     */
    List<Long> getByEmbedId(long embedId);

    /**
     * @param channelId id of the registry text channel
     * @return ids of the embeds registered to the channel
     */
    List<Long> getByChannelId(long channelId);
}
