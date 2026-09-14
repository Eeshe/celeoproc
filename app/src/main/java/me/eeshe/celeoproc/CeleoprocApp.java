package me.eeshe.celeoproc;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppConfig;

public class CeleoprocApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CeleoprocApp.class);

    public static void main(String[] args) throws InterruptedException, SQLException {
        final AppConfig config = new AppConfig();
        final Bot bot = new Bot(config);
        bot.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown, "bot-shutdown"));

        bot.getBot().awaitReady();
        LOGGER.info("Bot is ready");
    }
}
