package me.eeshe.celeoproc.model;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of an electricity status update triggered from an embed component.
 *
 * @param updated         whether the user's status or embed participation changed
 * @param skippedChannels registry channels skipped because the bot lacks the
 *                        required permissions
 */
public record ElectricityStatusUpdateResult(boolean updated, List<SkippedChannel> skippedChannels) {

    public ElectricityStatusUpdateResult {
        skippedChannels = List.copyOf(Objects.requireNonNull(skippedChannels,
                "Skipped channels must not be null"));
    }
}
