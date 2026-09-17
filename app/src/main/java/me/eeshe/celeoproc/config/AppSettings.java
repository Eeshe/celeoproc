package me.eeshe.celeoproc.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public final class AppSettings extends JsonConfigLoader {
    private List<Long> guildIds = List.of();

    public AppSettings() {
        super("settings", "config", "settings.json", "/settings.json", "SETTINGS_PATH");
    }

    @Override
    protected void apply(final JsonNode root) {
        this.guildIds = loadGuildIds(root);
    }

    public List<Long> getGuildIds() {
        return guildIds;
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
}
