# 🗺️ ROADMAP - Sistema MMORPG Minecraft

Roadmap completo del desarrollo del sistema MMORPG para Minecraft Paper 1.20.6.

## 📋 Visión General

El desarrollo está dividido en **7 fases principales**, cada una con objetivos claros y entregables específicos.

**Progreso Total**: 7/7 fases completadas (100% del proyecto funcional) ✅

---

## 🎯 FASE 1: Estructura Base y Configuraciones

**Estado**: ✅ COMPLETADA  
**Prioridad**: Alta  
**Duración Estimada**: 1-2 horas  
**Progreso**: 100%

### Objetivos

- [x] Crear estructura de directorios completa
- [x] Generar archivos de configuración base
- [x] Crear archivos JSON de configuración
- [x] Configurar .gitignore y archivos de repo
- [x] Crear documentación base (README, LICENSE)

### Entregables

#### 1.1 Estructura de Directorios

```
minecraft-mmorpg/
├── mmorpg-plugin/
│   ├── src/main/java/com/nightslayer/mmorpg/
│   ├── src/main/resources/
│   ├── target/
│   └── docs/
├── web/
│   ├── templates/
│   ├── static/css/
│   ├── static/js/
│   └── venv/
├── config/
│   └── data/
├── minecraft-server/
│   ├── plugins/
│   ├── worlds/
│   └── logs/
├── scripts/
├── test/
└── docs/
```

#### 1.2 Archivos de Configuración

- [x] `README.md` - Documentación principal
- [x] `ROADMAP.md` - Este archivo
- [x] `LICENSE` - Licencia MIT
- [x] `.gitignore` - Archivos ignorados
- [x] `config/server.properties` - Configuración del servidor
- [x] `config/config.yml` - Configuración del plugin
- [x] `config/crafting_config.json` - Recetas de crafting
- [x] `config/enchanting_config.json` - Configuración de encantamientos
- [x] `config/respawn_config.json` - Zonas de respawn
- [x] `config/dungeon_config.json` - Configuración de dungeons
- [x] `config/events_config.json` - Eventos/invasiones
- [x] `config/pets_config.json` - Configuración de pets
- [x] `config/squad_config.json` - Configuración de squads
- [x] `config/panel_config.json` - Configuración del panel web

#### 1.3 Documentación

- [x] `INSTALL_GUIDE.md` - Guía de instalación completa
- [x] `docs/API.md` - Documentación de API (50+ endpoints REST)
- [x] `docs/DATABASE.md` - Esquema de base de datos (25 tablas completas)
- [x] `docs/PLUGIN_DEV.md` - Guía de desarrollo del plugin
- [x] `docs/WEB_PANEL.md` - Guía del panel web
- [x] `docs/COMMANDS.md` - Referencia completa de comandos
- [x] `CONTRIBUTING.md` - Guía de contribución
- [x] `STATUS.md` - Estado del proyecto

---

## 🔧 FASE 2: Plugin Java - Core y Database

**Estado**: ✅ COMPLETADA  
**Prioridad**: Alta  
**Duración Estimada**: 4-6 horas  
**Progreso**: 100%

### Objetivos

- [x] Configurar proyecto Maven (pom.xml)
- [x] Crear clase principal del plugin
- [x] Implementar sistema de base de datos
- [x] Implementar migración de datos JSON → SQLite
- [x] Crear sistema de gestión de mundos
- [x] Implementar sistema de internacionalización

### Entregables

#### 2.1 Configuración Maven

- [x] `mmorpg-plugin/pom.xml`
  - Dependencias: paper-api, sqlite-jdbc, gson, lombok
  - Configuración de compilación para Java 21
  - Plugin de shade para dependencias

#### 2.2 Core del Plugin

- [x] `MMORPGPlugin.java` - Clase principal
  - Método `onEnable()` - Inicialización
  - Método `onDisable()` - Limpieza
  - Registro de managers
  - Registro de listeners
  - Registro de comandos
- [x] `src/main/resources/plugin.yml` - Configuración del plugin

#### 2.3 Sistema de Base de Datos

- [x] `database/DatabaseManager.java`
  - Singleton connection pool a SQLite
  - Métodos: `executeUpdate()`, `executeQuery()`, `executeQueryAsync()`
  - Gestión de transacciones
  - **CRÍTICO**: NO cerrar Connection (es singleton)

- [x] `database/DatabaseMigration.java`
  - Crear esquema de tablas en primera ejecución
  - Migrar datos de JSON a SQLite
  - Generar datos por defecto si no existen
  - **CRÍTICO**: NO usar try-with-resources en Connection
  - Métodos:
    - `migrate()` - Migración principal
    - `createTables()` - Crear todas las tablas
    - `migrateNPCs()`
    - `migrateQuests()`
    - `migrateCrafting()`
    - `migrateEnchantments()`
    - `migrateMobs()`
    - `migrateDungeons()`
    - `migrateInvasions()`
    - `migratePets()`
    - `migrateAchievements()`
    - `generateDefaultData()` - Datos por defecto

- [x] `database/WorldDatabaseManager.java`
  - Gestión de BD local por mundo
  - Resolver symlinks para mundo activo
  - **CRÍTICO**: NO usar `getCanonicalFile()`
  - Tablas: `player_stats`, `kills_tracking`, `deaths_tracking`, `world_events`

#### 2.4 Sistema de i18n

- [x] `i18n/LanguageManager.java`
  - Cargar archivos de idioma (es_ES, en_US)
  - Método `getMessage(key, args...)`
  - Soporte para placeholders

- [x] `src/main/resources/lang/es_ES.yml` (150+ traducciones)
- [x] `src/main/resources/lang/en_US.yml` (150+ traducciones)

#### 2.5 Esquema de Base de Datos

Implementar tablas en `universal.db`:

**Jugadores**:
- `players` - Datos básicos de jugadores
- `player_abilities` - Habilidades de jugadores
- `player_quests` - Progreso de quests
- `player_economy` - Balance económico
- `player_inventory_rpg` - Inventario RPG
- `player_achievements` - Logros desbloqueados
- `player_pets` - Pets del jugador
- `player_squads` - Membresía de squads

**Contenido RPG**:
- `npcs` - NPCs del mundo
- `quests` - Definiciones de quests
- `crafting_recipes` - Recetas de crafting
- `enchantments` - Definiciones de encantamientos
- `custom_mobs` - Mobs personalizados
- `dungeon_definitions` - Definiciones de dungeons
- `invasions` - Configuración de invasiones
- `pets` - Definiciones de pets
- `achievements_definitions` - Definiciones de logros
- `respawn_zones` - Zonas de respawn
- `squads` - Definiciones de squads

**Sistema**:
- `admin_users` - Usuarios del panel web
- `transactions` - Historial de transacciones
- `logs` - Logs del sistema

---

## 🎮 FASE 3: Plugin Java - Managers RPG Básicos

**Estado**: ✅ COMPLETADA  
**Prioridad**: Alta  
**Duración Estimada**: 6-8 horas  
**Progreso**: 100%

### Objetivos

- [x] Implementar sistema de NPCs
- [x] Implementar sistema de Quests
- [x] Implementar sistema de Items personalizados
- [x] Implementar sistema de Mobs personalizados
- [x] Implementar sistema de Economía
- [x] Implementar listeners básicos
- [x] Implementar comandos básicos

### Entregables

#### 3.1 Sistema de NPCs

- [x] `npcs/NPCManager.java`
  - Cargar NPCs de BD
  - Spawn/despawn de NPCs
  - Sistema de diálogos
  - Sistema de trades
  - Click interactions

- [x] `npcs/NPC.java` - Clase modelo (inner class)
- [x] `npcs/NPCDialogue.java` - Sistema de diálogos
- [x] `npcs/NPCTrade.java` - Sistema de comercio

#### 3.2 Sistema de Quests

- [x] `quests/QuestManager.java`
  - Cargar quests de BD
  - Asignar quests a jugadores
  - Tracking de progreso
  - Completar quests
  - Dar recompensas

- [x] `models/Quest.java` - Clase modelo
- [x] `quests/QuestObjective.java` - Objetivos de quest
- [x] `quests/QuestReward.java` - Recompensas

#### 3.3 Sistema de Items

- [x] `items/ItemManager.java`
  - Crear items personalizados
  - Persistent data para items RPG
  - Items con estadísticas
  - Items con habilidades

- [x] `utils/ItemBuilder.java` - Constructor de items con persistent data
- [x] `items/RPGItem.java` - Clase modelo
- [x] `items/ItemStats.java` - Estadísticas de items

#### 3.4 Sistema de Mobs

- [x] `mobs/MobManager.java`
  - Cargar mobs de BD
  - Spawn de mobs personalizados
  - Aplicar estadísticas custom
  - Sistema de drops
  - Scaling por nivel

- [x] `mobs/CustomMob.java` - Clase modelo (inner class)
- [x] `mobs/MobDrops.java` - Sistema de drops

#### 3.5 Sistema de Economía

- [x] `economy/EconomyManager.java`
  - Gestión de balance de jugadores
  - Transacciones (deposit, withdraw, transfer)
  - Historial de transacciones
  - Top jugadores ricos

- [x] `economy/Transaction.java` - Clase modelo

#### 3.6 Event Listeners

- [x] `listeners/MobDeathListener.java`
  - Otorgar XP al matar mobs
  - Drops personalizados
  - Actualizar bestiario

- [x] `listeners/SpawnListener.java`
  - Control de spawns de mobs

- [x] `listeners/PlayerListener.java`
  - Join/quit
  - Cargar/guardar datos RPG

#### 3.7 Comandos

- [x] `commands/ClassCommand.java` - `/class <warrior|mage|rogue|paladin>`
- [x] `commands/QuestCommand.java` - `/quest <list|start|progress>`
- [x] `commands/StatsCommand.java` - `/stats`
- [x] `commands/BalanceCommand.java` - `/balance`

#### 3.8 Modelos de Datos

- [x] `models/RPGPlayer.java` - Modelo de jugador RPG
- [x] `models/Quest.java` - Modelo de quest

---

## ⚔️ FASE 4: Plugin Java - Sistemas Avanzados

**Estado**: ✅ COMPLETADA  
**Prioridad**: Media  
**Duración Estimada**: 8-10 horas  
**Progreso**: 100%

### Objetivos

- [x] Implementar sistema de Crafting
- [x] Implementar sistema de Encantamientos
- [x] Implementar sistema de Respawn
- [x] Implementar sistema de Dungeons
- [x] Implementar sistema de Invasiones
- [x] Implementar sistema de Pets
- [x] Implementar sistema de Spawns
- [x] Implementar sistema de Rangos
- [x] Implementar sistema de Squads
- [x] Implementar sistema de Achievements
- [x] Implementar sistema de Bestiario
- [x] Implementar API para panel web

### Entregables

#### 4.1 Sistema de Crafting

- [x] `crafting/CraftingManager.java`
  - Cargar recetas de BD
  - Validar recetas
  - Procesar crafteo
  - Cobrar costos (coins, XP)
  - Clase interna CraftingRecipe
  - Método getParsedMaterials()

- [x] `crafting/CraftingRecipe.java` - Clase modelo (inner class)
- [x] `crafting/CraftingGUI.java` - Interfaz de crafting

#### 4.2 Sistema de Encantamientos

- [x] `enchanting/EnchantmentManager.java`
  - Cargar encantamientos de BD
  - Aplicar encantamientos
  - Validar nivel requerido
  - Cobrar costos
  - Mapeo de encantamientos vanilla
  - Clase interna RPGEnchantment

- [x] `enchanting/RPGEnchantment.java` - Clase modelo (inner class)
- [x] `enchanting/EnchantingGUI.java` - Interfaz de encantamiento

#### 4.3 Sistema de Respawn

- [x] `respawn/RespawnManager.java`
  - Gestión de zonas de respawn
  - Invulnerabilidad temporal
  - Teleport al respawn
  - Clase interna RespawnZone
  - Sistema de potion effects

- [x] `respawn/RespawnZone.java` - Clase modelo (inner class)

#### 4.4 Sistema de Dungeons

- [x] `dungeons/DungeonManager.java`
  - Cargar dungeons de BD
  - Instanciar dungeons
  - Sistema de oleadas
  - Recompensas por completar
  - Boss encounters con multiplicadores

- [x] `dungeons/Dungeon.java` - Clase modelo (inner class)
- [x] `dungeons/DungeonInstance.java` - Instancia de dungeon (inner class)
- [x] Métodos startNextWave(), spawnBoss()

#### 4.5 Sistema de Invasiones

- [x] `invasions/InvasionManager.java`
  - Cargar invasiones de BD
  - Programar invasiones
  - Iniciar/detener invasiones
  - Recompensas
  - Sistema de participantes
  - Spawn automático de oleadas

- [x] `invasions/Invasion.java` - Clase modelo (inner class)
- [x] `invasions/ActiveInvasion.java` - Instancia activa (inner class)

#### 4.6 Sistema de Pets

- [x] `pets/PetManager.java`
  - Cargar pets de BD
  - Adoptar/abandonar pets
  - Sistema de entrenamiento
  - Pets como mounts
  - Active pets tracking
  - Level y XP system

- [x] `pets/PetDefinition.java` - Clase modelo (inner class)
- [x] Map<UUID, LivingEntity> activePets

#### 4.7 Sistema de Spawns

- [x] `spawns/SpawnManager.java`
  - Gestión de puntos de spawn
  - Spawn rates personalizados
  - Scheduled spawning
  - Safe location finding
  - Clase interna SpawnPoint
  - Implements Listener

#### 4.8 Sistema de Rangos

- [x] `ranks/RankManager.java`
  - Sistema de rangos por progreso
  - Beneficios por rango (damage/exp multipliers)
  - Ascenso de rango
  - 9 rangos: Novice → Divine
  - Damage multipliers: 1.0x → 3.0x

- [x] `ranks/Rank.java` - Clase modelo (inner class)

#### 4.9 Sistema de Squads

- [x] `squads/SquadManager.java`
  - Crear/disolver squads
  - Invitar/expulsar miembros
  - Sistema de rangos (leader, officer, member)
  - Banco compartido

- [x] `squads/Squad.java` - Clase modelo
- [x] `squads/SquadMember.java` - Miembro de squad

#### 4.10 Sistema de Achievements

- [x] `achievements/AchievementManager.java`
  - Cargar achievements de BD
  - Tracking de progreso
  - Desbloquear achievements
  - Recompensas
  - Type-based tracking
  - Broadcast de unlocks

- [x] `achievements/Achievement.java` - Clase modelo (inner class)
- [x] Método trackProgress(type, player, value)

#### 4.11 Sistema de Bestiario

- [x] `bestiary/BestiaryManager.java`
  - Registrar kills de mobs
  - Estadísticas por mob
  - Recompensas por completar bestiario
  - 12 mobs predefinidos
  - Completion tracking

- [x] `bestiary/BestiaryEntry.java` - Clase modelo (inner class)

#### 4.12 API para Panel Web

- [x] `api/RPGAdminAPI.java`
  - Endpoints REST internos
  - Métodos para panel web:
    - `getPlayers()` - Obtener lista de jugadores
    - `getPlayerStats(uuid)` - Estadísticas de jugador
    - `updatePlayerBalance(uuid, amount)`
    - `updatePlayerLevel(uuid, level)`
    - `getEconomyStats()` - Estadísticas económicas
    - `getQuests()` - Lista de quests
    - `createQuest(quest)` - Crear quest
    - `updateQuest(id, quest)` - Actualizar quest
    - `getMobs()` - Lista de mobs
    - `updateMob(id, mob)` - Actualizar mob
    - `getServerStats()` - Estadísticas del servidor
    - `getRecentTransactions()` - Transacciones recientes

---

## 🌐 FASE 5: Panel Web Flask

**Estado**: ✅ COMPLETADA  
**Prioridad**: Media  
**Duración Estimada**: 6-8 horas  
**Progreso**: 100%

### Objetivos

- [x] Configurar aplicación Flask
- [x] Implementar sistema de autenticación
- [x] Crear templates HTML
- [x] Implementar estilos CSS
- [x] Implementar JavaScript para interactividad
- [x] Crear endpoints de API
- [x] Integrar con RCON
- [x] Implementar visualización de logs

### Entregables

#### 5.1 Aplicación Flask

- [x] `web/app.py` - Aplicación principal
  - Configuración de Flask
  - Rutas y endpoints
  - Manejo de sesiones
  - Conexión a BD SQLite

- [x] `web/requirements.txt`
  ```
  Flask==3.0.3
  bcrypt==4.1.3
  ```

- [x] `web/start-web.sh` - Script de inicio

#### 5.2 Sistema de Autenticación

- [x] Login con usuario/contraseña
- [x] Hash bcrypt de contraseñas
- [x] Sesiones con timeout
- [x] Middleware de autenticación

#### 5.3 Templates HTML

- [x] `templates/base.html` - Template base con sidebar y diseño oscuro
- [x] `templates/login.html` - Página de login
- [x] `templates/dashboard.html` - Dashboard principal
- [x] `templates/players.html` - Gestión de jugadores con gráficos Chart.js
- [x] `templates/economy.html` - Estadísticas económicas y transacciones
- [x] `templates/quests.html` - CRUD completo de quests
- [x] `templates/mobs.html` - Gestión de mobs personalizados
- [x] `templates/console.html` - Consola RCON con historial
- [x] `templates/config.html` - Editor de configuración por secciones
- [x] `templates/logs.html` - Visualización de logs con filtros

#### 5.4 Estilos CSS

- [x] `static/css/style.css`
  - Estilos globales
  - Responsive design
  - Dark mode
  - Componentes básicos

#### 5.5 JavaScript

- [x] `static/js/main.js`
  - Llamadas AJAX a API
  - Carga de datos de jugadores
  - Actualización dinámica
  - Validación de formularios

#### 5.6 Endpoints de API

- [x] `GET /` - Dashboard (requiere login)
- [x] `POST /login` - Autenticación
- [x] `GET /logout` - Cerrar sesión
- [x] `GET /api/players` - Lista de jugadores
- [x] `GET /api/players/<uuid>` - Datos de jugador
- [x] `POST /api/players/<uuid>/update` - Actualizar jugador
- [x] `GET /api/economy` - Estadísticas económicas
- [x] `GET /api/quests` - Lista de quests
- [x] `POST /api/quests/create` - Crear quest
- [x] `POST /api/quests/<id>/update` - Actualizar quest
- [x] `DELETE /api/quests/<id>` - Eliminar quest
- [x] `GET /api/mobs` - Lista de mobs
- [x] `POST /api/mobs/<id>/update` - Actualizar mob
- [x] `POST /api/console` - Ejecutar comando RCON
- [x] `GET /api/server/status` - Estado del servidor
- [x] `GET /api/server/logs` - Logs del servidor
- [x] `POST /api/config/save` - Guardar configuración

#### 5.7 Integración RCON

- [x] Conexión a servidor via RCON
- [x] Ejecutar comandos remotamente
- [x] Obtener respuestas
- [x] Manejo de errores

#### 5.8 Sistema de Logs

- [x] Leer logs del servidor
- [x] Filtrado por tipo (info, warning, error)
- [x] Búsqueda en logs
- [x] Tail en tiempo real

---

## 📜 FASE 6: Scripts de Instalación

**Estado**: ✅ COMPLETADA  
**Prioridad**: Alta  
**Duración Estimada**: 4-6 horas  
**Progreso**: 100%

### Objetivos

- [x] Crear script de instalación completo
- [x] Crear script de desinstalación
- [x] Crear scripts auxiliares
- [x] Configurar servicios systemd
- [x] Crear scripts de mantenimiento

### Entregables

#### 6.1 Script de Instalación Principal

- [x] `install-native.sh` - Script completo de instalación
- [x] `build.sh` - Script de compilación del plugin

**Flujo del Script**:

1. **Verificación de Dependencias**
   - Detectar SO y distribución
   - Verificar Java 21+
   - Verificar Maven 3.9+
   - Verificar Python 3.12+
   - Verificar Git

2. **Preparación**
   - Solicitar directorio de instalación
   - Crear estructura de directorios
   - Verificar permisos

3. **Compilación del Plugin**
   - Compilar con `mvn clean package`
   - Verificar JAR compilado
   - Copiar a directorio de plugins

4. **Descarga de Paper Server**
   - Descargar Paper 1.20.6 build 151
   - Verificar hash SHA256
   - Aceptar EULA

5. **Configuración del Servidor**
   - Crear `server.properties`
   - Habilitar RCON
   - Configurar `level-name=world`
   - Configurar puertos

6. **Configuración de Mundos**
   - Crear estructura de mundos
   - Crear symlink `worlds/active -> mundo-inicial`
   - Preparar BD local del mundo

7. **Instalación de Plugins Adicionales**
   - Descargar Geyser-Spigot
   - Descargar Floodgate
   - Descargar ViaVersion
   - Descargar ViaBackwards
   - Descargar ViaRewind

8. **Configuración de Python**
   - Crear entorno virtual
   - Instalar dependencias de requirements.txt
   - Generar hash de contraseña admin (bcrypt)
   - Crear script `start-web.sh`

9. **Scripts de Control**
   - Crear scripts en `scripts/`
   - Dar permisos de ejecución

10. **Servicios Systemd (Opcional)**
    - Crear `minecraft-server.service`
    - Crear `minecraft-web-panel.service`
    - Habilitar servicios

11. **Finalización**
    - Mostrar resumen de instalación
    - Mostrar credenciales de admin
    - Mostrar comandos de inicio

#### 6.2 Script de Desinstalación

- [x] `uninstall-native.sh` (280+ líneas)

**Funcionalidades**:
- Detener servidor y panel web
- Preguntar si mantener backups
- Remover directorios
- Remover servicios systemd
- Limpiar symlinks
- Optional user deletion

#### 6.3 Scripts Auxiliares

- [x] `scripts/update.sh` (240+ líneas)
  - Actualizar sistema completo
  - Pull de git con stash
  - Recompilar plugin
  - Reiniciar servicios

- [x] `scripts/change-server-version.sh` (270+ líneas)
  - Descargar nueva versión de Paper
  - Hacer backup de versión actual
  - Actualizar servidor
  - Verificación de integridad JAR

- [x] `scripts/check-panel.sh` (100+ líneas)
  - Verificar si panel web está corriendo
  - Mostrar URL de acceso
  - Estado de proceso y puerto

- [x] `scripts/logs-web-panel.sh` (50+ líneas)
  - Ver logs del panel web en tiempo real
  - Fallback a journalctl

- [x] `scripts/status-web-panel.sh` (200+ líneas)
  - Estado detallado del panel web
  - PID del proceso
  - Tiempo de ejecución
  - Recursos (CPU, memoria)
  - URLs de acceso

- [x] `scripts/backup.sh` (240+ líneas)
  - Crear backup completo
  - Incluir mundos, configs, BD, logs
  - Comprimir en .tar.gz
  - Cleanup automático (keep last 7)

- [x] `scripts/restore-backup.sh` (270+ líneas)
  - Restaurar desde backup
  - Validar integridad
  - Pre-restore backup
  - Service restart

---

## 🧪 FASE 7: Testing y Documentación Final

**Estado**: ✅ COMPLETADA  
**Prioridad**: Media  
**Duración Estimada**: 3-4 horas  
**Progreso**: 100%

### Objetivos

- [x] Crear suite de tests para Python
- [x] Crear tests para plugin Java
- [x] Documentar API REST
- [x] Documentar esquema de BD
- [x] Crear guías de usuario
- [x] Crear guías de desarrollo

### Entregables

#### 7.1 Tests Python

- [x] `test/test_api_endpoints.py` (350+ líneas)
  - Test de endpoints del panel web (25 tests)
  - Autenticación y sesiones
  - CRUD completo de jugadores, quests, mobs
  - Tests de economía y servidor
  - Validación de datos

- [x] `test/test_backup_service.py` (450+ líneas)
  - Test de creación de backups (12 tests)
  - Test de restauración y validación
  - Test de cleanup de backups antiguos
  - Test de manejo de errores

- [x] `test/test_database.py` (500+ líneas)
  - Test de conexión a BD (15 tests)
  - Test de transacciones y rollback
  - Test de constraints (PK, FK)
  - Test de índices y VACUUM
  - Test de JSON handling

- [x] `test/run-tests.sh` (240+ líneas)
  - Ejecutar todos los tests Python y Bash
  - Generar reporte detallado (TXT + HTML)
  - Mostrar estadísticas de éxito
  - Exit code apropiado

#### 7.2 Tests Java

- [x] Tests unitarios básicos incluidos en estructura
  - DatabaseManager tests (estructura)
  - QuestManager tests (estructura)
  - EconomyManager tests (estructura)

#### 7.3 Documentación de API

- [x] `docs/API.md` (450+ líneas)
  - Documentar 50+ endpoints REST
  - Ejemplos de request/response completos
  - Códigos de error detallados
  - Autenticación y rate limiting
  - Ejemplos en Python, JavaScript y cURL

#### 7.4 Documentación de BD

- [x] `docs/DATABASE.md` (520+ líneas)
  - Esquema completo de `universal.db` (13 tablas)
  - Esquema completo de `world.db` (12 tablas)
  - Relaciones entre tablas con diagramas
  - Índices y optimizaciones
  - 10+ ejemplos de queries útiles
  - Estrategia de backup

#### 7.5 Guías de Usuario

- [x] `INSTALL_GUIDE.md`
  - Instalación paso a paso
  - Requisitos detallados
  - Troubleshooting completo
  - Configuración y mantenimiento

- [x] `docs/WEB_PANEL.md` (480+ líneas)
  - Guía completa de uso del panel web
  - Descripción de todas las secciones
  - Casos de uso comunes
  - Troubleshooting del panel
  - Configuración de seguridad

- [x] `docs/COMMANDS.md` (620+ líneas)
  - Lista completa de comandos del plugin
  - Comandos de jugador (10+)
  - Comandos de admin (8+)
  - Permisos detallados
  - Ejemplos de uso con outputs
  - Aliases y autocompletado

#### 7.6 Guías de Desarrollo

- [x] `docs/PLUGIN_DEV.md` (550+ líneas)
  - Arquitectura completa del plugin
  - Cómo crear nuevos managers
  - Cómo crear comandos y listeners
  - Estándares de código y buenas prácticas
  - Cómo compilar y testear
  - Debugging y logging

---

## 📊 Métricas del Proyecto

### Líneas de Código Estimadas

| Componente | Archivos | Líneas de Código |
|------------|----------|------------------|
| Plugin Java | 35+ | ~8,000 |
| Panel Web (Python) | 10+ | ~2,000 |
| Templates HTML | 10+ | ~1,500 |
| CSS/JS | 5+ | ~800 |
| Scripts Bash | 15+ | ~2,000 |
| Tests | 10+ | ~500 |
| Configuraciones | 15+ | ~1,000 |
| Documentación | 10+ | ~3,000 |
| **TOTAL** | **110+** | **~18,800** |

### Archivos a Generar

- **Total**: ~110+ archivos
- **Java**: ~35 archivos
- **Python**: ~10 archivos
- **HTML**: ~10 archivos
- **Bash**: ~15 archivos
- **JSON/YML**: ~15 archivos
- **Markdown**: ~10 archivos
- **Otros**: ~15 archivos

---

## 🎯 Hitos Clave

### Hito 1: MVP Funcional (Fases 1-3)
**Fecha Objetivo**: Semana 1-2  
**Descripción**: Sistema básico funcional con clases, quests, economía

### Hito 2: Sistema Completo (Fases 4-5)
**Fecha Objetivo**: Semana 3-4  
**Descripción**: Todos los sistemas RPG + panel web

### Hito 3: Producción (Fases 6-7)
**Fecha Objetivo**: Semana 5  
**Descripción**: Scripts de instalación + tests + documentación completa

#### Checklist de Hardening del Panel (Producción)
- [x] Sesiones seguras y timeout
- [x] Rate limiting (login, API, consola)
- [x] Auditoría de acciones admin
- [x] Validaciones de payloads críticos
- [x] Webhooks firmados con reintentos
- [x] Índices de rendimiento en BD
- [x] Telemetría de backups
- [x] Health check `/api/health`

---

## 🔄 Próximas Características (Post-Lanzamiento)

### v2.0 - Sistemas Sociales
- [x] Sistema de guilds/clanes
- [x] Sistema de amigos
- [x] Chat privado entre jugadores
- [x] Sistema de mail in-game

### v2.1 - Profesiones
- [x] Minería avanzada
- [x] Herrería
- [x] Alquimia
- [x] Encantamiento avanzado
- [x] Cocina

### v2.2 - PvP
- [x] Arenas PvP
- [x] Rankings de PvP
- [x] Recompensas de PvP
- [x] Torneos automáticos

### v2.3 - Eventos
- [x] Eventos estacionales automáticos
- [x] Boss raids
- [x] Eventos de servidor

### v2.4 - Integración
- [x] Integración con Discord (bots, notificaciones)
- [x] API REST pública
- [x] Webhooks para eventos

### v2.5 - Optimización
- [x] Sistema de backups automáticos
- [x] Caché de queries frecuentes
- [x] Optimización de rendimiento
- [x] Métricas y monitoreo

---

## 📝 Notas de Implementación

### Prioridades Críticas

1. **Base de Datos**
   - ⚠️ NUNCA cerrar Connection en DatabaseManager (es singleton)
   - ⚠️ NO usar try-with-resources en Connection
   - ✅ Solo usar try-with-resources en Statement/PreparedStatement/ResultSet

2. **Symlinks de Mundos**
   - ⚠️ NO usar `getCanonicalFile()` - rompe symlinks
   - ✅ Resolver symlinks manualmente si es necesario
   - ✅ Usar rutas relativas cuando sea posible

3. **Seguridad del Panel Web**
   - ⚠️ SIEMPRE usar HTTPS en producción
   - ⚠️ Validar TODAS las entradas de usuario
   - ⚠️ Implementar rate limiting en console
   - ✅ Hash bcrypt para contraseñas

4. **Scripts de Instalación**
   - ✅ Validar dependencias antes de continuar
   - ✅ Hacer backups antes de sobrescribir
   - ✅ Permitir cancelación en cada paso importante
   - ✅ Mensajes claros y amigables

### Convenciones de Código

#### Java
- Usar Java 21 features
- Seguir convenciones de Paper API
- JavaDoc para métodos públicos
- Usar Lombok cuando sea apropiado

#### Python
- PEP 8 style guide
- Type hints cuando sea posible
- Docstrings para funciones
- Flask best practices

#### Bash
- Usar `set -euo pipefail`
- Funciones para código reutilizable
- Mensajes de error claros
- Validar todas las entradas

---

## 🤝 Contribución

Este roadmap es un documento vivo. Si tienes sugerencias para mejorar el plan de desarrollo, por favor:

1. Abre un issue en GitHub
2. Propone cambios via Pull Request al ROADMAP.md
3. Discute en GitHub Discussions

---

**Última actualización**: 4 de febrero de 2026  
**Versión del Roadmap**: 1.0.0
