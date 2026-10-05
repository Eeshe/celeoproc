package me.eeshe.celeoproc.service;

import java.util.List;

/**
 * Manages the registry channels that receive electricity status messages.
 */
public interface RegistryChannelService {

    /**
     * Registers the given text channel for the given electricity status embed,
     * replacing any previous registration for that embed.
     *
     * @param embedId   id of the electricity status embed
     * @param channelId id of the registry text channel
     */
    void save(long embedId, long channelId);

    /**
     * Removes the registry channel associated with the given embed, if present.
     *
     * @param embedId id of the electricity status embed
     */
    void delete(long embedId);

    /**
     * @param embedId id of the electricity status embed
     * @return ids of the text channels registered for the embed
     */
    List<Long> getChannelIdsByEmbedId(long embedId);

    /**
     * @param channelId id of the registry text channel
     * @return ids of the embeds registered to the channel
     */
    List<Long> getEmbedIdsByChannelId(long channelId);

    /**
     * @param userId id of a participating Discord user
     * @return distinct ids of the registry text channels whose embeds list the user
     *         as a participant
     */
    List<Long> getByParticipantId(long userId);

    /**
     * @param embedId   id of the electricity status embed
     * @param channelId id of the registry text channel
     * @return {@code true} when the embed is registered to the channel
     */
    boolean isRegistered(long embedId, long channelId);
}
