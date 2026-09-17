package me.eeshe.celeoproc.service;

import java.util.Map;

import me.eeshe.celeoproc.config.Message;

/**
 * Provides configurable messages used across the bot.
 */
public interface MessageService {

    String get(Message message);

    String get(Message message, Map<String, String> placeholders);
}
