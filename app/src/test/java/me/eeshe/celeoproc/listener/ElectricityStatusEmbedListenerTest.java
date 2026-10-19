package me.eeshe.celeoproc.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import me.eeshe.celeoproc.model.SkippedChannel;

/**
 * Verifies the guild-filtered formatting of the registry channels the bot could
 * not post in.
 */
class ElectricityStatusEmbedListenerTest {

    @Test
    void formatsOnlyChannelsFromTheGivenGuild() {
        assertEquals("<#10>, <#12>", ElectricityStatusEmbedListener.formatSkippedChannels(
                List.of(
                        new SkippedChannel(10L, 1L),
                        new SkippedChannel(11L, 2L),
                        new SkippedChannel(12L, 1L),
                        new SkippedChannel(10L, 1L)),
                1L));
    }

    @Test
    void ignoresChannelsFromOtherGuilds() {
        assertEquals("", ElectricityStatusEmbedListener.formatSkippedChannels(
                List.of(new SkippedChannel(20L, 2L)), 1L));
    }

    @Test
    void returnsEmptyWhenNoChannelWasSkipped() {
        assertEquals("", ElectricityStatusEmbedListener.formatSkippedChannels(List.of(), 1L));
    }
}
