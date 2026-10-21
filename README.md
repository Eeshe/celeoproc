<p align="center">
  <img src="docs/assets/logo.svg" alt="Celeoproc logo">
  <br>
  <br>
  <a href="README.md"><img src="https://img.shields.io/badge/lang-English-blue" alt="English"></a>
  <a href="README.es.md"><img src="https://img.shields.io/badge/lang-Espa%C3%B1ol-red" alt="Español"></a>
</p>

Celeoproc is a Discord bot that allows server members to mark when power goes out and comes back. The bot then collects statistics about these power outages and displays them through messages and graphs.

## Features

- Embed showing electricity status of members
- Power outage alerts
- Log management
- Stat display with custom date ranges

## Bot Usage

Most of the usage is explained in the bot's [website](https://eeshe.github.io/celeoproc). While features like management and reminders are self-explanatory when used.

## Bot Installation

### 1. Configure the env files

Create two files next to where you'll put the compose file:

**`.env.db`**: Postgres credentials:

```
POSTGRES_DB=celeoproc
POSTGRES_USER=celeoproc
POSTGRES_PASSWORD=change-me
```

**`.env.bot`**: bot settings:

```
DISCORD_BOT_TOKEN=your-token-here
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
```

Get the token from the [Discord Developer Portal](https://discord.com/developers/applications) (your application → **Bot** → token). Change `POSTGRES_PASSWORD`. Never commit these files.

### 2. Docker compose file

Save as `docker-compose.yml`:

```yaml
services:
  db:
    image: postgres:18.6
    ports:
      - "127.0.0.1:5432:5432"
    env_file: .env.db
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "psql -U $$POSTGRES_USER -d $$POSTGRES_DB -c 'SELECT 1'"]
      interval: 5s
      timeout: 5s
      retries: 12
    restart: unless-stopped

  bot:
    image: eeshe/celeoproc:latest
    env_file:
      - .env.db
      - .env.bot
    environment:
      POSTGRES_HOST: db
    volumes:
      - ./config:/app/config
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

volumes:
  pgdata:
```

### 3. Run the bot

```bash
docker compose up -d
docker compose logs -f bot
```

Postgres starts first (healthcheck), then the bot logs in and creates its tables automatically. To stop: `docker compose down`, since data persists in the `pgdata` volume.

## Bot Configuration

Two JSON files in `./config` (mounted into the container at `/app/config`):

- `config/settings.json` — bot behavior
- `config/messages.json` — all bot replies and slash command names

Either file can be omitted: the bot falls back to defaults bundled in the image. Templates: [settings.json](https://github.com/Eeshe/celeoproc/blob/main/app/src/main/resources/settings.json) / [messages.json](https://github.com/Eeshe/celeoproc/blob/main/app/src/main/resources/messages.json). Files are read at startup — restart after edits: `docker compose restart bot`. Invalid values fail startup with the offending key named in the error.

### settings.json

| Key | Description | Default |
| --- | --- | --- |
| `guild-ids` | Array of server IDs. Registers commands per guild → instant updates. Unknown/skipped IDs are logged. | `[]` (global registration only) |
| `timezone` | IANA timezone (`America/New_York`) for stats and displays | system timezone |
| `embed-thumbnail-url` | Thumbnail URL on the electricity embed; `""` for none | Celeoproc logo |
| `default-electricity-out-estimate` | Outage estimate used when a user gives none | `6h` |
| `electricity-status-embed-update-frequency` | How often the status embed refreshes. Must be > 0. | `15m` |
| `electricity-reminder-frequency` | Electricity reminder interval. Must be > 0. | `2h` |
| `electricity-status-embed-participant-limit` | Max embed participants. Integer > 0. | `20` |

Durations use `XhYmZs` — units optional but at least one required (`6h`, `30m`, `1h30m`, `1h30m20s`; bare `6` invalid).

### messages.json

- Every key is required when the file exists — a missing key fails startup.
- `{placeholder}` tokens are substituted at send time (e.g. `{channel}`, `{argument}`).
- `command_*_name` keys set slash command/option names: must match `[a-z0-9_-]{1,32}` and be unique. Discord length limits apply to names, descriptions, button labels, modal titles, embed titles/footers — exceeding them fails startup.

This repo provides 2 localizations: [english](https://github.com/Eeshe/celeoproc/blob/main/languages/messages.en.json) and [spanish](https://github.com/Eeshe/celeoproc/blob/main/languages/messages.es.json).

### Custom file locations

Set `SETTINGS_PATH` / `MESSAGES_PATH` environment variables (e.g. in `.env.bot`) to load the JSON files from somewhere else.
