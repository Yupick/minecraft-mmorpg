# API Documentation - MMORPG Web Panel

## Descripción General

El Web Panel expone una REST API completa para administrar el servidor Minecraft MMORPG. La API utiliza **Flask 3.0.3** y se comunica con el plugin mediante:

1. **RCON**: Para ejecutar comandos en tiempo real
2. **SQLite**: Para leer/escribir datos persistentes
3. **RPGAdminAPI**: Clase Java que proporciona métodos administrativos

---

## Autenticación

Todas las rutas requieren autenticación mediante sesión Flask.

### POST `/login`
Inicia sesión en el panel.

**Request Body**:
```json
{
    "username": "admin",
    "password": "secure_password"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Login exitoso",
    "redirect": "/dashboard"
}
```

**Response** (401 Unauthorized):
```json
{
    "success": false,
    "message": "Credenciales inválidas"
}
```

---

### GET `/logout`
Cierra sesión y limpia la cookie.

**Response**: Redirect a `/login`

---

## Endpoints de Jugadores

### GET `/api/players`
Lista todos los jugadores registrados con estadísticas completas.

**Query Parameters**:
- `filters` (opcional): JSON serializado con filtros.
  - `min_level` (int)
  - `max_level` (int)
  - `class` (string)

**Response** (200 OK):
```json
[
    {
        "uuid": "550e8400-e29b-41d4-a716-446655440000",
        "username": "Steve",
        "class": "warrior",
        "level": 45,
        "experience": 12580
    }
]
```

---

### GET `/api/players/<uuid>`
Obtiene detalles completos de un jugador específico.

**Response** (200 OK):
```json
{
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "Steve",
    "class": "warrior",
    "level": 45,
    "experience": 12580,
    "health": 120.0,
    "max_health": 120.0,
    "mana": 150.0,
    "max_mana": 200.0
}
```

**Response** (404 Not Found):
```json
{
    "error": "Player not found"
}
```

**Response** (400 Bad Request):
```json
{
    "error": "UUID inválido"
}
```

---

### POST `/api/players/<uuid>/update`
Actualiza estadísticas básicas del jugador.

**Request Body**:
```json
{
    "level": 46,
    "experience": 13000,
    "health": 110,
    "mana": 180
}
```

**Response** (200 OK):
```json
{
    "success": true
}
```
---

## Endpoints de Economía

### GET `/api/economy/stats`
Obtiene estadísticas globales de economía.

**Response** (200 OK):
```json
{
    "success": true,
    "stats": {
        "total_circulation": 1542350.75,
        "total_earned_today": 45230.50,
        "total_spent_today": 32180.25,
        "transactions_today": 1523,
        "top_earners": [
            {"username": "Steve", "coins": 15420.50},
            {"username": "Alex", "coins": 12350.00}
        ],
        "top_spenders": [
            {"username": "Herobrine", "coins_spent": 8540.00},
            {"username": "Notch", "coins_spent": 6320.00}
        ]
    }
}
```

---

### GET `/api/economy/transactions`
Lista transacciones económicas recientes.

**Query Parameters**:
- `player_uuid` (opcional): Filtrar por jugador
- `type` (opcional): `earn`, `spend`, `admin_add`, `admin_remove`
- `limit` (opcional): Máximo de resultados (default: 50)

**Response** (200 OK):
```json
{
    "success": true,
    "total": 1523,
    "transactions": [
        {
            "id": 45231,
            "player_uuid": "550e8400-e29b-41d4-a716-446655440000",
            "player_name": "Steve",
            "amount": 250.0,
            "transaction_type": "earn",
            "source": "quest",
            "description": "Completó 'Zombie Slayer'",
            "timestamp": "2024-01-15T14:32:15"
        }
    ]
}
```

---

### POST `/api/economy/add`
Añade monedas a un jugador (comando administrativo).

**Request Body**:
```json
{
    "player_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 1000.0,
    "reason": "Compensación por bug"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "1000.0 monedas añadidas a Steve",
    "new_balance": 16420.50
}
```

---

### POST `/api/economy/remove`
Retira monedas de un jugador.

**Request Body**:
```json
{
    "player_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 500.0,
    "reason": "Sanción por infracción"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "500.0 monedas retiradas de Steve",
    "new_balance": 15920.50
}
```

---

## Endpoints de Quests

### GET `/api/quests`
Lista todas las quests disponibles.

**Response** (200 OK):
```json
{
    "success": true,
    "quests": [
        {
            "id": 1,
            "name": "Zombie Slayer",
            "description": "Mata 50 zombies",
            "objectives": [
                {"type": "kill", "target": "zombie", "count": 50}
            ],
            "rewards": {
                "coins": 500,
                "exp": 250,
                "items": [{"item": "DIAMOND_SWORD", "amount": 1}]
            },
            "min_level": 10,
            "repeatable": false,
            "quest_type": "kill",
            "status": "active"
        }
    ]
}
```

---

### POST `/api/quests`
Crea una nueva quest.

**Request Body**:
```json
{
    "name": "Emerald Hunter",
    "description": "Recolecta 10 esmeraldas",
    "objectives": [
        {"type": "collect", "target": "EMERALD", "count": 10}
    ],
    "rewards": {
        "coins": 1000,
        "exp": 500
    },
    "min_level": 20,
    "repeatable": true,
    "cooldown": 86400,
    "quest_type": "collect"
}
```

**Response** (201 Created):
```json
{
    "success": true,
    "message": "Quest creada correctamente",
    "quest_id": 25
}
```

---

### PUT `/api/quests/<quest_id>`
Actualiza una quest existente.

**Request Body**:
```json
{
    "rewards": {
        "coins": 1500,
        "exp": 750
    },
    "status": "inactive"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Quest actualizada correctamente"
}
```

---

### DELETE `/api/quests/<quest_id>`
Elimina una quest (solo si ningún jugador la tiene activa).

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Quest eliminada correctamente"
}
```

**Response** (400 Bad Request):
```json
{
    "success": false,
    "message": "No se puede eliminar: 15 jugadores tienen esta quest activa"
}
```

---

## Endpoints de Mobs

### GET `/api/mobs`
Lista todos los mobs personalizados.

**Response** (200 OK):
```json
{
    "success": true,
    "mobs": [
        {
            "id": 1,
            "mob_type": "ZOMBIE_ELITE",
            "level": 25,
            "health": 150.0,
            "damage": 15.0,
            "speed": 0.3,
            "defense": 10.0,
            "exp_reward": 100,
            "coins_min": 50,
            "coins_max": 150,
            "is_boss": false,
            "is_aggressive": true,
            "drops": [
                {"item": "DIAMOND", "chance": 0.05, "amount": 1},
                {"item": "GOLD_INGOT", "chance": 0.2, "amount": 3}
            ],
            "abilities": [
                {"type": "speed_boost", "duration": 5, "cooldown": 20}
            ]
        }
    ]
}
```

---

### PUT `/api/mobs/<mob_id>`
Actualiza estadísticas de un mob.

**Request Body**:
```json
{
    "health": 200.0,
    "damage": 20.0,
    "exp_reward": 150,
    "drops": [
        {"item": "DIAMOND", "chance": 0.1, "amount": 2}
    ]
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Mob actualizado correctamente"
}
```

---

## Endpoints de Servidor

### GET `/api/server/stats`
Obtiene estadísticas generales del servidor.

**Response** (200 OK):
```json
{
    "success": true,
    "stats": {
        "status": "online",
        "online_players": 45,
        "max_players": 100,
        "tps": 19.8,
        "memory_used": 4096,
        "memory_max": 8192,
        "uptime": 345600,
        "version": "Paper 1.20.6",
        "world_size_mb": 2048,
        "total_players": 1523,
        "total_quests": 45,
        "total_mobs": 32
    }
}
```

---

### POST `/api/server/command`
Ejecuta un comando de consola mediante RCON.

**Request Body**:
```json
{
    "command": "say Hello from Web Panel!"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "output": "Server: Hello from Web Panel!"
}
```

---

### POST `/api/server/restart`
Reinicia el servidor de forma segura.

**Request Body**:
```json
{
    "delay": 60,
    "message": "Servidor reiniciando en 60 segundos"
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Reinicio programado en 60 segundos"
}
```

---

### POST `/api/optimization/backups/run`
Inicia un backup manual del servidor.

**Response** (200 OK):
```json
{
    "success": true,
    "path": "../backups/backup-20240115-143500.tar.gz",
    "size": 1048576
}
```

---

## Endpoints de Integraciones

### GET `/api/integrations/webhooks`
Lista webhooks configurados.

**Response** (200 OK):
```json
[
    {
        "id": 1,
        "name": "Discord",
        "url": "https://example.com/webhook",
        "events": "event_created,pvp_tournament"
    }
]
```

---

### POST `/api/integrations/webhooks/create`
Crea un webhook.

**Request Body**:
```json
{
    "name": "Discord",
    "url": "https://example.com/webhook",
    "events": "event_created,pvp_tournament"
}
```

**Response** (200 OK):
```json
{
    "success": true
}
```

---

### DELETE `/api/integrations/webhooks/<id>/delete`
Elimina un webhook.

**Response** (200 OK):
```json
{
    "success": true
}
```

---

### POST `/api/integrations/webhooks/dispatch`
Dispara webhooks manualmente.

**Request Body**:
```json
{
    "event": "event_created",
    "payload": {"id": 10, "name": "Evento"}
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "sent": 1
}
```

**Headers de entrega** (firmados HMAC):

- `X-Webhook-Event`
- `X-Webhook-Timestamp`
- `X-Webhook-Signature`

---

## Health Check

### GET `/api/health`
Verifica estado de BD, RCON y disco.

**Response** (200 OK):
```json
{
    "status": "ok",
    "db": "ok",
    "rcon": "ok",
    "disk_free": 123456789,
    "disk_total": 987654321
}
```

---

## Endpoints de Configuración

### GET `/api/config`
Obtiene la configuración actual del plugin.

**Response** (200 OK):
```json
{
    "success": true,
    "config": {
        "general": {
            "plugin_enabled": true,
            "debug_mode": false,
            "language": "es"
        },
        "gameplay": {
            "starting_coins": 100.0,
            "max_level": 100,
            "exp_multiplier": 1.0,
            "death_penalty": 0.05
        },
        "economy": {
            "enable_shops": true,
            "tax_rate": 0.0,
            "daily_login_reward": 50.0
        },
        "database": {
            "auto_backup": true,
            "backup_interval": 3600,
            "connection_pool_size": 10
        }
    }
}
```

---

### PUT `/api/config`
Actualiza configuración del plugin.

**Request Body**:
```json
{
    "gameplay": {
        "exp_multiplier": 1.5,
        "death_penalty": 0.1
    }
}
```

**Response** (200 OK):
```json
{
    "success": true,
    "message": "Configuración actualizada (requiere /reload)"
}
```

---

## Códigos de Error

| Código | Descripción |
|--------|-------------|
| 200 | Operación exitosa |
| 201 | Recurso creado correctamente |
| 400 | Petición inválida (parámetros faltantes o incorrectos) |
| 401 | No autenticado (requiere login) |
| 403 | Acceso denegado (permisos insuficientes) |
| 404 | Recurso no encontrado |
| 409 | Conflicto (ya existe un recurso con ese nombre) |
| 500 | Error interno del servidor |
| 503 | Servicio no disponible (servidor offline) |

---

## Rate Limiting

La API implementa rate limiting para prevenir abuso:

- **login_attempts_per_hour** (login)
- **api_requests_per_minute** (API general)
- **console_commands_per_minute** (consola)

Cuando se excede el límite:
```json
{
    "error": "Rate limit exceeded"
}
```

Los valores se configuran en [config/panel_config.json](config/panel_config.json).

---

## Ejemplos de Uso

### Python (requests)
```python
import requests
import json

# Login
session = requests.Session()
login_data = {"username": "admin", "password": "secure_password"}
session.post("http://localhost:5000/login", data=login_data)

# Obtener jugadores
filters = {"min_level": 10, "class": "warrior"}
response = session.get("http://localhost:5000/api/players", params={"filters": json.dumps(filters)})
players = response.json()

# Añadir monedas
session.post("http://localhost:5000/api/economy/add-coins", json={
    "player_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 1000.0,
    "reason": "Evento especial"
})
```

### JavaScript (fetch)
```javascript
// Login
const formData = new URLSearchParams();
formData.append('username', 'admin');
formData.append('password', 'secure_password');
await fetch('/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: formData.toString()
});

// Obtener stats del servidor
const response = await fetch('/api/health');
const health = await response.json();
console.log(`DB: ${health.db}`);

// Ejecutar backup
await fetch('/api/optimization/backups/run', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({})
});
```

### cURL
```bash
# Login
curl -c cookies.txt -X POST http://localhost:5000/login \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin&password=secure_password"

# Obtener jugadores con filtros
curl -b cookies.txt \
    "http://localhost:5000/api/players?filters=%7B%22min_level%22%3A10%2C%22class%22%3A%22warrior%22%7D"

# Crear quest
curl -b cookies.txt -X POST http://localhost:5000/api/quests/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dragon Slayer",
    "description": "Mata al Ender Dragon",
        "objectives": [{"type":"kill","target":"ENDER_DRAGON","count":1}],
    "rewards": {"coins":10000,"exp":5000},
        "required_level": 50
  }'

# Health check
curl http://localhost:5000/api/health

# Disparar webhooks
curl -b cookies.txt -X POST http://localhost:5000/api/integrations/webhooks/dispatch \
    -H "Content-Type: application/json" \
    -d '{"event":"event_created","payload":{"id":10,"name":"Evento"}}'
```

---

## WebSocket (Futuro)

Próximas versiones incluirán WebSocket para:
- Chat en tiempo real
- Notificaciones de eventos
- Logs del servidor en vivo
- Métricas de rendimiento en tiempo real

```javascript
const ws = new WebSocket('ws://localhost:5000/ws');
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Evento:', data.type, data.payload);
};
```
