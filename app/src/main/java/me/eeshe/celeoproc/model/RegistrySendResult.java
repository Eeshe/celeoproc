package me.eeshe.celeoproc.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.entities.Message;

/**
 * Outcome of broadcasting electricity registry messages to the configured
 * channels.
 *
 * @param messages        futures of the messages that were actually sent
 * @param skippedChannels channels skipped because the bot lacks the required
 *                        permissions
 */
public record RegistrySendResult(
        List<CompletableFuture<Message>> messages,
        List<SkippedChannel> skippedChannels) {

    public RegistrySendResult {
        messages = List.copyOf(Objects.requireNonNull(messages, "Messages must not be null"));
        skippedChannels = List.copyOf(Objects.requireNonNull(skippedChannels,
                "Skipped channels must not be null"));
    }
}
