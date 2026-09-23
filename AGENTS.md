# Tech Stack:
- Java (programming language)
- PostgreSQL (database, initialized through docker compose)

# General Structure
- A main bot class that contains all other instances such as configs, services, repositories, etc.
- AppSecrets, which holds the sensible configurations fetched from environment variables
- AppSettings, which holds configurations fetched from `settings.json` file
- AppMessages, which holds the configurable messages fetched from `messages.json` file

# Conventions
- Always use PreparedStatement for JDBC operations

# Useful documentation:

JDA (Discord Bot API): https://docs.jda.wiki/index.html
