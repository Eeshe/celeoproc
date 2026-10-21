<p align="center">
  <img src="docs/assets/logo.svg" alt="Logotipo de Celeoproc">
  <br>
  <br>
  <a href="README.md"><img src="https://img.shields.io/badge/lang-English-blue" alt="English"></a>
  <a href="README.es.md"><img src="https://img.shields.io/badge/lang-Espa%C3%B1ol-red" alt="Español"></a>
</p>

Celeoproc es un bot de Discord que permite a los miembros del servidor marcar cuándo se va y vuelve la luz. El bot recopila estadísticas sobre estos apagones y las muestra mediante mensajes y gráficos.

## Características

- Cartelera con el estado eléctrico de los miembros
- Alertas de apagones
- Gestión de registros de apagones
- Estadísticas con rangos de fechas personalizados

## Uso del bot

La mayor parte del uso se explica en la [página web](https://eeshe.github.io/celeoproc) del bot. Las funciones como la gestión y los recordatorios se explican por sí solas al usarse.

## Instalación del bot

### 1. Configura los archivos de entorno

Crea dos archivos junto al archivo compose:

**`.env.db`**: credenciales de Postgres:

```
POSTGRES_DB=celeoproc
POSTGRES_USER=celeoproc
POSTGRES_PASSWORD=change-me
```

**`.env.bot`**: configuración del bot:

```
DISCORD_BOT_TOKEN=your-token-here
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
```

Obtén el token en el [Portal de Desarrolladores de Discord](https://discord.com/developers/applications) (tu aplicación → **Bot** → token). Cambia `POSTGRES_PASSWORD`. Nunca subas estos archivos al repositorio.

### 2. Archivo de Docker Compose

Guárdalo como `docker-compose.yml`:

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

### 3. Ejecuta el bot

```bash
docker compose up -d
docker compose logs -f bot
```

Postgres arranca primero (healthcheck) y luego el bot inicia sesión y crea sus tablas automáticamente. Para detenerlo: `docker compose down`; los datos se conservan en el volumen `pgdata`.

## Configuración del bot

Dos archivos JSON en `./config` (montados en el contenedor en `/app/config`):

- `config/settings.json` — comportamiento del bot
- `config/messages.json` — todas las respuestas del bot y los nombres de los comandos de barra

Cualquiera de los dos archivos puede omitirse: el bot usa los valores por defecto incluidos en la imagen. Plantillas: [settings.json](https://github.com/Eeshe/celeoproc/blob/main/app/src/main/resources/settings.json) / [messages.json](https://github.com/Eeshe/celeoproc/blob/main/app/src/main/resources/messages.json). Los archivos se leen al arrancar; reinicia después de editarlos: `docker compose restart bot`. Un valor inválido impide el arranque y el error indica la clave afectada.

### settings.json

| Clave | Descripción | Valor por defecto |
| --- | --- | --- |
| `guild-ids` | Array de IDs de servidores. Registra los comandos por servidor → actualizaciones instantáneas. Los IDs desconocidos u omitidos se registran en el log. | `[]` (solo registro global) |
| `timezone` | Zona horaria IANA (`America/New_York`) para las estadísticas y los mensajes | zona horaria del sistema |
| `embed-thumbnail-url` | URL de la miniatura de la cartelera eléctrica; `""` para ninguna | logotipo de Celeoproc |
| `default-electricity-out-estimate` | Estimación de apagón usada cuando el usuario no indica ninguna | `6h` |
| `electricity-status-embed-update-frequency` | Cada cuánto se actualiza la cartelera de estado. Debe ser > 0. | `15m` |
| `electricity-reminder-frequency` | Intervalo de los recordatorios de luz. Debe ser > 0. | `2h` |
| `electricity-status-embed-participant-limit` | Máximo de participantes en la cartelera. Entero > 0. | `20` |

Las duraciones usan el formato `XhYmZs`: las unidades son opcionales pero se requiere al menos una (`6h`, `30m`, `1h30m`, `1h30m20s`; `6` a secas no es válido).

### messages.json

- Todas las claves son obligatorias si el archivo existe: una clave ausente impide el arranque.
- Los marcadores `{placeholder}` se sustituyen al enviar el mensaje (p. ej. `{channel}`, `{argument}`).
- Las claves `command_*_name` definen los nombres de comandos y opciones: deben coincidir con `[a-z0-9_-]{1,32}` y ser únicos. Se aplican los límites de longitud de Discord para nombres, descripciones, etiquetas de botones, títulos de modales y títulos/pies de carteleras: superarlos impide el arranque.

Este repositorio incluye 2 traducciones: [inglés](https://github.com/Eeshe/celeoproc/blob/main/languages/messages.en.json) y [español](https://github.com/Eeshe/celeoproc/blob/main/languages/messages.es.json).

### Ubicaciones personalizadas

Define las variables de entorno `SETTINGS_PATH` / `MESSAGES_PATH` (p. ej. en `.env.bot`) para cargar los archivos JSON desde otra ubicación.
