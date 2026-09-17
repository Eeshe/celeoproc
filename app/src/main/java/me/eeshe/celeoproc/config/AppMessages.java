package me.eeshe.celeoproc.config;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Configurable Discord messages sourced from {@code messages.json}.
 */
public final class AppMessages extends JsonConfigLoader {
    private JsonNode root;

    public AppMessages() {
        super("messages", "config", "messages.json", "/messages.json", "MESSAGES_PATH");
    }

    @Override
    protected void validate(final JsonNode root) {
        for (final Message message : Message.values()) {
            final JsonNode node = root.path(message.key());
            if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
                throw new IllegalStateException("Missing required message '%s'".formatted(message.key()));
            }
        }
    }

    @Override
    protected void apply(final JsonNode root) {
        this.root = root;
    }

    public String getMessage(final Message message) {
        Objects.requireNonNull(message, "Message must not be null");
        if (root == null) {
            throw new IllegalStateException("Messages are not loaded, call load() first");
        }

        final JsonNode node = root.path(message.key());
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new IllegalStateException("Missing required message '%s'".formatted(message.key()));
        }
        return node.asText();
    }

    public String getMessage(final Message message, final Map<String, String> placeholders) {
        String value = getMessage(message);
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }

        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }
}
