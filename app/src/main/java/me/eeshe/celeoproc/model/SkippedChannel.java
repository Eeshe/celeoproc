package me.eeshe.celeoproc.model;

/**
 * Channel that was skipped because the bot lacks the permissions required to
 * perform an operation on it.
 *
 * @param channelId id of the skipped channel
 * @param guildId   id of the guild the channel belongs to
 */
public record SkippedChannel(long channelId, long guildId) {
}
