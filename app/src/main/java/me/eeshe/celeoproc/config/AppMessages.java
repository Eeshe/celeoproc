package me.eeshe.celeoproc.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Configurable Discord messages sourced from {@code messages.json}.
 */
public final class AppMessages extends JsonConfigLoader {
    private static final Pattern DISCORD_NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");

    private JsonNode root;

    public AppMessages() {
        super("messages", "config", "messages.json", "/messages.json", "MESSAGES_PATH");
    }

    @Override
    protected void validate(final JsonNode root) {
        final Set<String> commandNames = new HashSet<>();
        for (final Message message : Message.values()) {
            final JsonNode node = root.path(message.key());
            if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
                throw new IllegalStateException("Missing required message '%s'".formatted(message.key()));
            }
            final int maxLength = message.maxLength();
            if (maxLength > 0 && node.asText().length() > maxLength) {
                throw new IllegalStateException(
                        "Message '%s' is %d characters long, exceeding the %d character limit: '%s'"
                                .formatted(message.key(), node.asText().length(), maxLength, node.asText()));
            }
            if (message.isCommandName() && !DISCORD_NAME_PATTERN.matcher(node.asText()).matches()) {
                throw new IllegalStateException(
                        "Command name '%s' is invalid: '%s'. Names must match [a-z0-9_-]{1,32}"
                                .formatted(message.key(), node.asText()));
            }
            if (message.isCommandName() && !message.key().contains("_option_") && !commandNames.add(node.asText())) {
                throw new IllegalStateException(
                        "Duplicate command name '%s' at '%s'".formatted(node.asText(), message.key()));
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
