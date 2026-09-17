package me.eeshe.celeoproc.config;

/**
 * Registry of configurable message keys available in {@code messages.json}.
 */
public enum Message {
    BOT_READY("bot_ready"),
    SHUTDOWN("shutdown"),
    GENERIC_ERROR("generic_error"),
    MISSING_ARGUMENT("missing_argument"),

    BOT_INFO("bot_info"),
    RELOAD_SUCCESS("reload_success"),
    RELOAD_ERROR("reload_error");

    private final String key;

    Message(final String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
