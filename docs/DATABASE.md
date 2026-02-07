# Documentación de Base de Datos

## Arquitectura de Datos

Este proyecto utiliza una arquitectura de **doble base de datos SQLite**:

- **universal.db**: Datos globales del servidor (economía, quests, rangos, logros, etc.)
- **{worldName}.db**: Datos específicos por mundo (jugadores, skills, inventarios, etc.)

Cada mundo tiene su propia base de datos para soportar múltiples mundos con progresión independiente.

---

## universal.db

### Tabla: `quests`
Almacena todas las quests disponibles en el servidor.

```sql
CREATE TABLE quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    objectives TEXT,           -- JSON: [{"type":"kill","target":"zombie","count":10}]
    rewards TEXT,              -- JSON: {"coins":500,"exp":100,"items":[...]}
    min_level INTEGER DEFAULT 1,
    repeatable BOOLEAN DEFAULT 0,
    cooldown INTEGER DEFAULT 0,
    npc_name TEXT,
    quest_type TEXT DEFAULT 'kill',
    status TEXT DEFAULT 'active'
);
```

**Índices**:
```sql
CREATE INDEX idx_quests_type ON quests(quest_type);
CREATE INDEX idx_quests_level ON quests(min_level);
```

---

### Tabla: `mob_stats`
Configuración de mobs personalizados.

```sql
CREATE TABLE mob_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mob_type TEXT NOT NULL UNIQUE,
    level INTEGER DEFAULT 1,
    health REAL DEFAULT 20.0,
    damage REAL DEFAULT 2.0,
    speed REAL DEFAULT 0.2,
    defense REAL DEFAULT 0.0,
    exp_reward INTEGER DEFAULT 10,
    coins_min INTEGER DEFAULT 5,
    coins_max INTEGER DEFAULT 15,
    is_boss BOOLEAN DEFAULT 0,
    is_aggressive BOOLEAN DEFAULT 1,
    drops TEXT,                -- JSON: [{"item":"DIAMOND","chance":0.05,"amount":1}]
    abilities TEXT             -- JSON: [{"type":"explosion","radius":3,"cooldown":10}]
);
```

**Índices**:
```sql
CREATE INDEX idx_mob_type ON mob_stats(mob_type);
CREATE INDEX idx_mob_boss ON mob_stats(is_boss);
```

---

### Tabla: `economy_log`
Registro de todas las transacciones económicas.

```sql
CREATE TABLE economy_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    player_name TEXT,
    amount REAL NOT NULL,
    transaction_type TEXT,     -- 'earn', 'spend', 'admin_add', 'admin_remove'
    source TEXT,               -- 'quest', 'mob_kill', 'trade', 'shop', 'admin'
    description TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**Índices**:
```sql
CREATE INDEX idx_economy_player ON economy_log(player_uuid);
CREATE INDEX idx_economy_type ON economy_log(transaction_type);
CREATE INDEX idx_economy_timestamp ON economy_log(timestamp);
```

---

### Tabla: `global_ranks`
Sistema de rangos globales (Novice → Divine).

```sql
CREATE TABLE global_ranks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rank_name TEXT NOT NULL UNIQUE,
    rank_level INTEGER NOT NULL UNIQUE,
    coins_required INTEGER DEFAULT 0,
    items_required TEXT,       -- JSON: [{"item":"DIAMOND","amount":64}]
    damage_multiplier REAL DEFAULT 1.0,
    exp_multiplier REAL DEFAULT 1.0,
    color_code TEXT DEFAULT '&f'
);
```

**Datos iniciales**: 9 rangos (Novice, Apprentice, Adept, Expert, Master, Grandmaster, Legend, Mythic, Divine)

---

### Tabla: `achievements`
Sistema de logros del servidor.

```sql
CREATE TABLE achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    achievement_id TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    achievement_type TEXT,     -- 'kill', 'quest', 'craft', 'level', 'explore'
    target INTEGER DEFAULT 1,
    reward_coins INTEGER DEFAULT 0,
    reward_exp INTEGER DEFAULT 0,
    reward_items TEXT,         -- JSON: [{"item":"DIAMOND_SWORD","amount":1}]
    hidden BOOLEAN DEFAULT 0
);
```

---

### Tabla: `bestiary_types`
Tipos de mobs para el bestiario (12 categorías).

```sql
CREATE TABLE bestiary_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mob_type TEXT NOT NULL UNIQUE,
    category TEXT,             -- 'common', 'uncommon', 'rare', 'boss'
    kills_required INTEGER DEFAULT 100,
    reward_coins INTEGER DEFAULT 50,
    reward_exp INTEGER DEFAULT 20
);
```

**Categorías**: ZOMBIE, SKELETON, CREEPER, SPIDER, ENDERMAN, BLAZE, WITHER_SKELETON, PIGLIN, EVOKER, RAVAGER, WARDEN, ENDER_DRAGON

---

### Tabla: `crafting_recipes`
Recetas personalizadas del sistema de crafting.

```sql
CREATE TABLE crafting_recipes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id TEXT NOT NULL UNIQUE,
    result_item TEXT NOT NULL,
    result_amount INTEGER DEFAULT 1,
    materials TEXT NOT NULL,    -- JSON: [{"item":"DIAMOND","amount":4},{"item":"STICK","amount":2}]
    coins_cost INTEGER DEFAULT 0,
    exp_cost INTEGER DEFAULT 0,
    min_level INTEGER DEFAULT 1,
    category TEXT DEFAULT 'general'
);
```

---

### Tabla: `enchantment_shop`
Tienda de encantamientos personalizados.

```sql
CREATE TABLE enchantment_shop (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    enchantment_name TEXT NOT NULL,
    max_level INTEGER DEFAULT 1,
    base_cost INTEGER DEFAULT 100,
    cost_multiplier REAL DEFAULT 1.5,
    applicable_items TEXT       -- JSON: ["SWORD","AXE","PICKAXE"]
);
```

---

### Tabla: `respawn_zones`
Zonas de respawn personalizadas.

```sql
CREATE TABLE respawn_zones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zone_name TEXT NOT NULL UNIQUE,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    radius REAL DEFAULT 5.0,
    min_level INTEGER DEFAULT 1,
    invulnerability_time INTEGER DEFAULT 5
);
```

---

### Tabla: `dungeons`
Sistema de mazmorras instanciadas.

```sql
CREATE TABLE dungeons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dungeon_name TEXT NOT NULL UNIQUE,
    min_players INTEGER DEFAULT 1,
    max_players INTEGER DEFAULT 5,
    min_level INTEGER DEFAULT 10,
    waves INTEGER DEFAULT 3,
    mobs_per_wave INTEGER DEFAULT 5,
    boss_mob TEXT,
    boss_health_multiplier REAL DEFAULT 3.0,
    rewards TEXT,               -- JSON: {"coins":1000,"exp":500,"items":[...]}
    cooldown INTEGER DEFAULT 3600
);
```

---

### Tabla: `invasions`
Eventos de invasión servidor-completo.

```sql
CREATE TABLE invasions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invasion_name TEXT NOT NULL UNIQUE,
    schedule_interval INTEGER DEFAULT 7200,
    duration INTEGER DEFAULT 300,
    announcement_message TEXT,
    mob_types TEXT,             -- JSON: ["ZOMBIE","SKELETON","CREEPER"]
    mobs_per_wave INTEGER DEFAULT 10,
    waves INTEGER DEFAULT 5,
    rewards TEXT                -- JSON: {"coins":2000,"exp":1000}
);
```

---

### Tabla: `pet_types`
Tipos de mascotas adoptables.

```sql
CREATE TABLE pet_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pet_name TEXT NOT NULL UNIQUE,
    entity_type TEXT NOT NULL,
    adoption_cost INTEGER DEFAULT 500,
    max_level INTEGER DEFAULT 50,
    base_health REAL DEFAULT 20.0,
    base_damage REAL DEFAULT 2.0,
    can_mount BOOLEAN DEFAULT 0,
    special_abilities TEXT      -- JSON: [{"type":"heal","cooldown":60}]
);
```

---

### Tabla: `spawn_points`
Puntos de spawn personalizados con temporizadores.

```sql
CREATE TABLE spawn_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    location_name TEXT NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    mob_type TEXT NOT NULL,
    spawn_interval INTEGER DEFAULT 300,
    max_mobs INTEGER DEFAULT 3,
    radius REAL DEFAULT 10.0
);
```

---

## {worldName}.db

### Tabla: `players`
Datos completos de jugadores por mundo.

```sql
CREATE TABLE players (
    uuid TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    exp INTEGER DEFAULT 0,
    coins REAL DEFAULT 0.0,
    class TEXT DEFAULT 'warrior',
    strength INTEGER DEFAULT 10,
    defense INTEGER DEFAULT 10,
    speed INTEGER DEFAULT 10,
    max_health REAL DEFAULT 20.0,
    max_mana REAL DEFAULT 100.0,
    current_health REAL DEFAULT 20.0,
    current_mana REAL DEFAULT 100.0,
    skill_points INTEGER DEFAULT 0,
    spawn_world TEXT,
    spawn_x REAL DEFAULT 0.0,
    spawn_y REAL DEFAULT 64.0,
    spawn_z REAL DEFAULT 0.0,
    first_join DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    playtime INTEGER DEFAULT 0,
    rank_id INTEGER DEFAULT 1,
    is_online BOOLEAN DEFAULT 0
);
```

**Índices**:
```sql
CREATE INDEX idx_players_level ON players(level);
CREATE INDEX idx_players_class ON players(class);
CREATE INDEX idx_players_online ON players(is_online);
```

---

### Tabla: `player_quests`
Progreso de quests por jugador.

```sql
CREATE TABLE player_quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    quest_id INTEGER NOT NULL,
    progress TEXT,              -- JSON: {"zombie_killed":7,"total":10}
    status TEXT DEFAULT 'in_progress',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    times_completed INTEGER DEFAULT 0,
    FOREIGN KEY(quest_id) REFERENCES quests(id),
    UNIQUE(player_uuid, quest_id)
);
```

**Índices**:
```sql
CREATE INDEX idx_pq_player ON player_quests(player_uuid);
CREATE INDEX idx_pq_status ON player_quests(status);
```

---

### Tabla: `player_skills`
Habilidades y niveles de skills.

```sql
CREATE TABLE player_skills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    skill_name TEXT NOT NULL,
    skill_level INTEGER DEFAULT 1,
    skill_exp INTEGER DEFAULT 0,
    UNIQUE(player_uuid, skill_name)
);
```

---

### Tabla: `player_inventory`
Inventarios persistentes por jugador.

```sql
CREATE TABLE player_inventory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    inventory_type TEXT DEFAULT 'main',
    slot INTEGER NOT NULL,
    item_type TEXT NOT NULL,
    amount INTEGER DEFAULT 1,
    enchantments TEXT,          -- JSON: [{"type":"SHARPNESS","level":5}]
    custom_name TEXT,
    lore TEXT,                  -- JSON: ["Line 1","Line 2"]
    UNIQUE(player_uuid, inventory_type, slot)
);
```

---

### Tabla: `player_achievements`
Logros desbloqueados por jugador.

```sql
CREATE TABLE player_achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    achievement_id TEXT NOT NULL,
    progress INTEGER DEFAULT 0,
    unlocked BOOLEAN DEFAULT 0,
    unlocked_at DATETIME,
    UNIQUE(player_uuid, achievement_id)
);
```

---

### Tabla: `player_bestiary`
Registro de kills por tipo de mob.

```sql
CREATE TABLE player_bestiary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    mob_type TEXT NOT NULL,
    kills INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT 0,
    UNIQUE(player_uuid, mob_type)
);
```

---

### Tabla: `player_pets`
Mascotas adoptadas por jugador.

```sql
CREATE TABLE player_pets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    pet_name TEXT NOT NULL,
    pet_type TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    exp INTEGER DEFAULT 0,
    health REAL DEFAULT 20.0,
    is_active BOOLEAN DEFAULT 0,
    adopted_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### Tabla: `player_ranks`
Progreso de rangos por jugador.

```sql
CREATE TABLE player_ranks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL UNIQUE,
    current_rank INTEGER DEFAULT 1,
    ascension_count INTEGER DEFAULT 0,
    last_ascension DATETIME
);
```

---

## Consultas de Ejemplo

### Obtener top 10 jugadores por nivel
```sql
SELECT username, level, exp, class, coins
FROM players
WHERE is_online = 1
ORDER BY level DESC, exp DESC
LIMIT 10;
```

### Obtener economía total del servidor
```sql
SELECT 
    SUM(CASE WHEN transaction_type = 'earn' THEN amount ELSE 0 END) AS total_earned,
    SUM(CASE WHEN transaction_type = 'spend' THEN amount ELSE 0 END) AS total_spent,
    COUNT(*) AS total_transactions
FROM economy_log
WHERE DATE(timestamp) = DATE('now');
```

### Quests activas de un jugador
```sql
SELECT q.name, q.description, pq.progress, pq.status
FROM player_quests pq
JOIN quests q ON pq.quest_id = q.id
WHERE pq.player_uuid = ?
  AND pq.status = 'in_progress';
```

### Bestiario completo de un jugador
```sql
SELECT bt.mob_type, bt.kills_required, pb.kills, pb.completed
FROM bestiary_types bt
LEFT JOIN player_bestiary pb ON bt.mob_type = pb.mob_type 
    AND pb.player_uuid = ?
ORDER BY bt.category, bt.mob_type;
```

### Mobs más matados en el servidor
```sql
SELECT mob_type, SUM(kills) AS total_kills
FROM player_bestiary
GROUP BY mob_type
ORDER BY total_kills DESC
LIMIT 10;
```

---

## Relaciones Importantes

1. **players ↔ player_quests**: Un jugador puede tener múltiples quests (1:N)
2. **quests → player_quests**: Una quest puede ser tomada por múltiples jugadores (1:N)
3. **players → player_inventory**: Un jugador tiene múltiples items (1:N)
4. **players → player_skills**: Un jugador tiene múltiples skills (1:N)
5. **players → player_achievements**: Un jugador puede desbloquear múltiples logros (1:N)
6. **players → player_bestiary**: Un jugador tiene estadísticas de múltiples mobs (1:N)
7. **players → player_pets**: Un jugador puede tener múltiples mascotas (1:N)
8. **global_ranks ↔ player_ranks**: Relación mediante rank_level/current_rank (1:N)

---

## Estrategia de Backup

El script `backup.sh` respalda:
- Archivo `universal.db` completo
- Todos los archivos `{world}.db` de cada mundo
- Logs de transacciones recientes (últimos 7 días)

Recomendación: Ejecutar backup cada 6 horas con cron:
```bash
0 */6 * * * /opt/minecraft-mmorpg/scripts/backup.sh
```

---

## Optimización

### Índices Críticos
Todos los campos usados en WHERE, JOIN y ORDER BY tienen índices.

### Limpieza de Datos
- `economy_log`: Archivar registros > 90 días
- `player_quests`: Limpiar quests completadas > 30 días
- Vacuum regular: `VACUUM;` para optimizar espacio

### Transacciones
Todas las operaciones críticas usan transacciones:
```java
connection.setAutoCommit(false);
try {
    // ... operaciones ...
    connection.commit();
} catch (SQLException e) {
    connection.rollback();
}
```

---

## Total de Tablas

- **universal.db**: 13 tablas
- **{world}.db**: 12 tablas por mundo

Total: **25 tablas** en el sistema completo (asumiendo 1 mundo).
