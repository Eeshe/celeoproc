package me.eeshe.celeoproc.config;

import java.util.Objects;

/**
 * Secret configuration sourced exclusively from environment variables.
 *
 * <p>Contains credentials and connection details that must never be committed
 * to version control.
 */
public final class AppSecrets {
    private static final String DISCORD_BOT_TOKEN_ENV = "DISCORD_BOT_TOKEN";
    private static final String POSTGRES_HOST_ENV = "PGHOST";
    private static final String POSTGRES_PORT_ENV = "PGPORT";
    private static final String POSTGRES_DATABASE_ENV = "PGDATABASE";
    private static final String POSTGRES_USER_ENV = "PGUSER";
    private static final String POSTGRES_PASSWORD_ENV = "PGPASSWORD";
    private static final String DATABASE_POOL_SIZE_ENV = "DATABASE_POOL_SIZE";
    private static final String DATABASE_POOL_SIZE_DEFAULT = "10";

    private final String discordBotToken;
    private final String postgresHost;
    private final int postgresPort;
    private final String postgresDatabase;
    private final String postgresUser;
    private final String postgresPassword;
    private final String postgresUrl;
    private final int databasePoolSize;

    public AppSecrets() {
        this.discordBotToken = requireEnv(DISCORD_BOT_TOKEN_ENV);
        this.postgresHost = requireEnv(POSTGRES_HOST_ENV);
        this.postgresPort = parsePositiveInt(requireEnv(POSTGRES_PORT_ENV), POSTGRES_PORT_ENV);
        this.postgresDatabase = requireEnv(POSTGRES_DATABASE_ENV);
        this.postgresUser = requireEnv(POSTGRES_USER_ENV);
        this.postgresPassword = requireEnv(POSTGRES_PASSWORD_ENV);
        this.postgresUrl = buildPostgresUrl();
        this.databasePoolSize = parsePositiveInt(
                getEnvOrDefault(DATABASE_POOL_SIZE_ENV, DATABASE_POOL_SIZE_DEFAULT), DATABASE_POOL_SIZE_ENV);
    }

    public String getDiscordBotToken() {
        return discordBotToken;
    }

    public String getPostgresHost() {
        return postgresHost;
    }

    public int getPostgresPort() {
        return postgresPort;
    }

    public String getPostgresDatabase() {
        return postgresDatabase;
    }

    public String getPostgresUser() {
        return postgresUser;
    }

    public String getPostgresPassword() {
        return postgresPassword;
    }

    public String getPostgresUrl() {
        return postgresUrl;
    }

    public int getDatabasePoolSize() {
        return databasePoolSize;
    }

    private String buildPostgresUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(postgresHost, postgresPort, postgresDatabase);
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable '%s' is unset or blank".formatted(key));
        }
        return Objects.requireNonNull(value);
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static int parsePositiveInt(String value, String key) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Environment variable '%s' must be a positive integer".formatted(key), exception);
        }
    }
}
