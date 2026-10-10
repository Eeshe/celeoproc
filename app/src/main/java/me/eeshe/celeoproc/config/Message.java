package me.eeshe.celeoproc.config;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.modals.Modal;

/**
 * Registry of configurable message keys available in {@code messages.json}.
 *
 * <p>
 * Messages bound to a Discord component declare the component's character limit
 * so an over-long value is rejected while loading the configuration instead of
 * crashing when the component is built. Messages without a limit use
 * {@link #NO_LIMIT}.
 */
public enum Message {
    BOT_READY("bot_ready"),
    SHUTDOWN("shutdown"),
    GENERIC_ERROR("generic_error"),
    MISSING_ARGUMENT("missing_argument"),
    GUILD_ONLY("guild_only"),

    ELECTRICITY_STATUS_EMBED_BUTTON_OUT("electricity_status_embed_button_out", Button.LABEL_MAX_LENGTH),
    ELECTRICITY_STATUS_EMBED_BUTTON_IN("electricity_status_embed_button_in", Button.LABEL_MAX_LENGTH),
    ELECTRICITY_STATUS_ALREADY_HAS_ELECTRICITY("electricity_status_already_has_electricity"),
    ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY("electricity_status_already_no_electricity"),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_TITLE("electricity_status_embed_out_modal_title", Modal.MAX_TITLE_LENGTH),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_LABEL("electricity_status_embed_out_modal_label", Label.LABEL_MAX_LENGTH),
    ELECTRICITY_STATUS_EMBED_OUT_MODAL_PLACEHOLDER("electricity_status_embed_out_modal_placeholder",
            TextInput.MAX_PLACEHOLDER_LENGTH),
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
    ELECTRICITY_STATUS_REMINDER("electricity_status_reminder"),

    BOT_INFO("bot_info"),
    RELOAD_SUCCESS("reload_success"),
    RELOAD_ERROR("reload_error"),

    POST_ELECTRICITY_STATS_INVALID_DATE("post_electricity_stats_invalid_date"),
    POST_ELECTRICITY_STATS_INVALID_RANGE("post_electricity_stats_invalid_range"),
    ELECTRICITY_STATS_SERVER_NO_DATA("electricity_stats_server_no_data"),
    ELECTRICITY_STATS_GLOBAL_NO_DATA("electricity_stats_global_no_data"),
    ELECTRICITY_STATS_SERVER_TITLE("electricity_stats_server_title"),
    ELECTRICITY_STATS_SERVER_GENERAL_HEADER("electricity_stats_server_general_header"),
    ELECTRICITY_STATS_SERVER_TOTAL_OUTAGES("electricity_stats_server_total_outages"),
    ELECTRICITY_STATS_SERVER_TOTAL_TIME("electricity_stats_server_total_time"),
    ELECTRICITY_STATS_SERVER_LONGEST("electricity_stats_server_longest"),
    ELECTRICITY_STATS_SERVER_SHORTEST("electricity_stats_server_shortest"),
    ELECTRICITY_STATS_SERVER_AWARD("electricity_stats_server_award"),
    ELECTRICITY_STATS_SERVER_USER_HEADER("electricity_stats_server_user_header"),
    ELECTRICITY_STATS_SERVER_USER_ENTRY("electricity_stats_server_user_entry"),
    ELECTRICITY_STATS_GLOBAL_TITLE("electricity_stats_global_title"),
    ELECTRICITY_STATS_GLOBAL_TOTAL_OUTAGES("electricity_stats_global_total_outages"),
    ELECTRICITY_STATS_GLOBAL_TOTAL_TIME("electricity_stats_global_total_time"),
    ELECTRICITY_STATS_GLOBAL_AVERAGE_TIME("electricity_stats_global_average_time"),
    ELECTRICITY_STATS_GLOBAL_DISCLAIMER("electricity_stats_global_disclaimer"),

    POWER_OUTAGE_GRAPH_USER_TITLE("power_outage_graph_user_title"),
    POWER_OUTAGE_GRAPH_COMBINED_TITLE("power_outage_graph_combined_title"),
    POWER_OUTAGE_GRAPH_DAY_AXIS("power_outage_graph_day_axis"),
    POWER_OUTAGE_GRAPH_HOUR_AXIS("power_outage_graph_hour_axis"),

    POWER_OUTAGE_LOG_NOT_FOUND("power_outage_log_not_found"),
    POWER_OUTAGE_EDIT_BUTTON("power_outage_edit_button", Button.LABEL_MAX_LENGTH),
    POWER_OUTAGE_DELETE_BUTTON("power_outage_delete_button", Button.LABEL_MAX_LENGTH),
    DELETE_POWER_OUTAGE_CONFIRM("delete_power_outage_confirm"),
    DELETE_POWER_OUTAGE_CONFIRM_BUTTON("delete_power_outage_confirm_button", Button.LABEL_MAX_LENGTH),
    DELETE_POWER_OUTAGE_SUCCESS("delete_power_outage_success"),
    EDIT_POWER_OUTAGE_MODAL_TITLE("edit_power_outage_modal_title", Modal.MAX_TITLE_LENGTH),
    EDIT_POWER_OUTAGE_MODAL_DESCRIPTION("edit_power_outage_modal_description"),
    EDIT_POWER_OUTAGE_OUT_LABEL("edit_power_outage_out_label", Label.LABEL_MAX_LENGTH),
    EDIT_POWER_OUTAGE_IN_LABEL("edit_power_outage_in_label", Label.LABEL_MAX_LENGTH),
    EDIT_POWER_OUTAGE_INVALID_DATE("edit_power_outage_invalid_date"),
    EDIT_POWER_OUTAGE_INVALID_RANGE("edit_power_outage_invalid_range"),
    EDIT_POWER_OUTAGE_MISSING_FIELDS("edit_power_outage_missing_fields"),
    EDIT_POWER_OUTAGE_SUCCESS("edit_power_outage_success");

    private static final int NO_LIMIT = 0;

    private final String key;
    private final int maxLength;

    Message(final String key) {
        this(key, NO_LIMIT);
    }

    Message(final String key, final int maxLength) {
        this.key = key;
        this.maxLength = maxLength;
    }

    public String key() {
        return key;
    }

    /**
     * @return maximum allowed length of the message, or {@link #NO_LIMIT} when the
     *         message is not bound to a length-limited Discord component
     */
    public int maxLength() {
        return maxLength;
    }
}
