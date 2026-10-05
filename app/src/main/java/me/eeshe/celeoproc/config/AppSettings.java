package me.eeshe.celeoproc.config;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import me.eeshe.celeoproc.util.DurationParser;

public final class AppSettings extends JsonConfigLoader {
    private static final Duration DEFAULT_ELECTRICITY_OUT_ESTIMATE = Duration.ofHours(6);
    private static final Duration DEFAULT_ELECTRICITY_STATUS_EMBED_UPDATE_FREQUENCY = Duration.ofMinutes(5);
    private static final Duration DEFAULT_ELECTRICITY_REMINDER_FREQUENCY = Duration.ofHours(2);

    private List<Long> guildIds = List.of();
    private ZoneId timezone = ZoneId.systemDefault();
    private Duration defaultElectricityOutEstimate = DEFAULT_ELECTRICITY_OUT_ESTIMATE;
    private Duration electricityStatusEmbedUpdateFrequency = DEFAULT_ELECTRICITY_STATUS_EMBED_UPDATE_FREQUENCY;
    private Duration electricityReminderFrequency = DEFAULT_ELECTRICITY_REMINDER_FREQUENCY;

    public AppSettings() {
        super("settings", "config", "settings.json", "/settings.json", "SETTINGS_PATH");
    }

    @Override
    protected void apply(final JsonNode root) {
        this.guildIds = loadGuildIds(root);
        this.timezone = loadTimezone(root);
        this.defaultElectricityOutEstimate = loadDefaultElectricityOutEstimate(root);
        this.electricityStatusEmbedUpdateFrequency = loadElectricityStatusEmbedUpdateFrequency(root);
        this.electricityReminderFrequency = loadElectricityReminderFrequency(root);
    }

    public List<Long> getGuildIds() {
        return guildIds;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public Duration getDefaultElectricityOutEstimate() {
        return defaultElectricityOutEstimate;
    }

    public Duration getElectricityStatusEmbedUpdateFrequency() {
        return electricityStatusEmbedUpdateFrequency;
    }

    public Duration getElectricityReminderFrequency() {
        return electricityReminderFrequency;
    }

    private List<Long> loadGuildIds(final JsonNode root) {
        final String path = "guild-ids";
        final JsonNode node = root.path(path);
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalStateException("Setting '%s' must be an array of guild ids".formatted(path));
        }
        final List<Long> guildIds = new ArrayList<>();
        for (final JsonNode element : node) {
            if (element.isNull() || element.asText().isBlank()) {
                throw new IllegalStateException("Setting '%s' contains a blank guild id".formatted(path));
            }
            try {
                guildIds.add(Long.parseLong(element.asText().trim()));
            } catch (NumberFormatException exception) {
                throw new IllegalStateException(
                        "Setting '%s' contains an invalid guild id '%s'".formatted(path, element.asText()), exception);
            }
        }
        return List.copyOf(guildIds);
    }

    private ZoneId loadTimezone(final JsonNode root) {
        final JsonNode node = root.path("timezone");
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(node.asText().trim());
        } catch (final DateTimeException exception) {
            throw new IllegalStateException(
                    "Setting 'timezone' contains an invalid zone id '%s'".formatted(node.asText()), exception);
        }
    }

    private Duration loadDefaultElectricityOutEstimate(final JsonNode root) {
        return loadDuration(root, "default-electricity-out-estimate", DEFAULT_ELECTRICITY_OUT_ESTIMATE);
    }

    private Duration loadElectricityStatusEmbedUpdateFrequency(final JsonNode root) {
        return loadDuration(root, "electricity-status-embed-update-frequency",
                DEFAULT_ELECTRICITY_STATUS_EMBED_UPDATE_FREQUENCY, true);
    }

    private Duration loadElectricityReminderFrequency(final JsonNode root) {
        return loadDuration(root, "electricity-reminder-frequency", DEFAULT_ELECTRICITY_REMINDER_FREQUENCY, true);
    }

    private Duration loadDuration(final JsonNode root, final String path, final Duration defaultValue) {
        return loadDuration(root, path, defaultValue, false);
    }

    private Duration loadDuration(final JsonNode root, final String path, final Duration defaultValue,
            final boolean requirePositive) {
        final JsonNode node = root.path(path);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }

        final Duration duration;
        try {
            duration = DurationParser.parse(node.asText());
        } catch (final DateTimeParseException exception) {
            throw new IllegalStateException(
                    "Setting '%s' contains an invalid duration '%s'".formatted(path, node.asText()), exception);
        }
        if (requirePositive && (duration.isZero() || duration.isNegative())) {
            throw new IllegalStateException("Setting '%s' must be greater than zero".formatted(path));
        }
        return duration;
    }
}
