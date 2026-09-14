package me.eeshe.celeoproc;

import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppConfig;
import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.database.impl.PostgreSQLDatabase;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public final class Bot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);

    private final AppConfig appConfig;

    private JDA bot;
    private Database database;

    public Bot(AppConfig appConfig) {
        Objects.requireNonNull(appConfig, "Config must not be null");

        this.appConfig = appConfig;
    }

    public void initialize() throws SQLException {
        initializeDatabase();

        LOGGER.info("Starting Discord bot");
        this.bot = JDABuilder.createDefault(appConfig.getDiscordBotToken()).build();
    }

    private void initializeDatabase() throws SQLException {
        this.database = new PostgreSQLDatabase(appConfig);

        database.connect();
    }

    public JDA getBot() {
        return bot;
    }

    public Database getDatabase() {
        return database;
    }

    public void shutdown() {
        if (bot != null) {
            bot.shutdown();
        }
        shutdownDatabase();
    }

    private void shutdownDatabase() {
        if (database == null) {
            return;
        }
        try {
            database.disconnect();
        } catch (SQLException exception) {
            LOGGER.error("Failed to disconnect from database", exception);
        }
    }
}
