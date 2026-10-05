package me.eeshe.celeoproc.util;

/**
 * Builds Discord message links and their markdown representations.
 */
public final class MessageLinkFormatter {

    private static final String JUMP_URL_FORMAT = "https://discord.com/channels/%d/%d/%d";

    private MessageLinkFormatter() {
    }

    /**
     * @param guildId   id of the guild the message belongs to
     * @param channelId id of the channel the message belongs to
     * @param messageId id of the message
     * @return jump url pointing at the message
     */
    public static String jumpUrl(final long guildId, final long channelId, final long messageId) {
        return JUMP_URL_FORMAT.formatted(guildId, channelId, messageId);
    }
}
