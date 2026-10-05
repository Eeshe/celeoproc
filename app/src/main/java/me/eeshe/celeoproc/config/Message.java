package me.eeshe.celeoproc.config;

/**
 * Registry of configurable message keys available in {@code messages.json}.
 */
public enum Message {
    BOT_READY("bot_ready"),
    SHUTDOWN("shutdown"),
    GENERIC_ERROR("generic_error"),
    MISSING_ARGUMENT("missing_argument"),
    GUILD_ONLY("guild_only"),

    ELECTRICITY_STATUS_EMBED_BUTTON_OUT("electricity_status_embed_button_out"),
    ELECTRICITY_STATUS_EMBED_BUTTON_IN("electricity_status_embed_button_in"),
    ELECTRICITY_STATUS_ALREADY_HAS_ELECTRICITY("electricity_status_already_has_electricity"),
    ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY("electricity_status_already_no_electricity"),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_TITLE("electricity_status_embed_out_modal_title"),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_LABEL("electricity_status_embed_out_modal_label"),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_PLACEHOLDER("electricity_status_embed_out_modal_placeholder"),
    ELECTRICITY_STATUS_INVALID_DURATION("electricity_status_invalid_duration"),

    ELECTRICITY_REGISTRY_OUT("electricity_registry_out"),
    ELECTRICITY_REGISTRY_IN("electricity_registry_in"),
    ELECTRICITY_REGISTRY_EMBED_NOT_FOUND("electricity_registry_embed_not_found"),
    ELECTRICITY_REGISTRY_SET_SUCCESS("electricity_registry_set_success"),
    ELECTRICITY_REGISTRY_UNSET_NOT_REGISTERED("electricity_registry_unset_not_registered"),
    ELECTRICITY_REGISTRY_UNSET_SUCCESS("electricity_registry_unset_success"),

    ELECTRICITY_STATUS_EMBED_HAS_ELECTRICITY("electricity_status_embed_has_electricity"),
    ELECTRICITY_STATUS_EMBED_OUTAGE_TODAY("electricity_status_embed_outage_today"),
    ELECTRICITY_STATUS_EMBED_NO_ELECTRICITY("electricity_status_embed_no_electricity"),
    ELECTRICITY_STATUS_EMBED_NO_OUTAGE_HISTORY("electricity_status_embed_no_outage_history"),

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
