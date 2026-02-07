# Guía de Desarrollo del Plugin

## Introducción

Este documento describe la arquitectura interna del plugin MMORPG y proporciona guías para desarrolladores que deseen:

- Entender la estructura del código
- Añadir nuevas características
- Modificar sistemas existentes
- Contribuir al proyecto

---

## Arquitectura General

### Patrón de Diseño

El plugin utiliza varios patrones de diseño:

1. **Singleton**: Para managers que requieren única instancia (DatabaseManager, QuestManager, etc.)
2. **Factory**: Para creación de mobs y items personalizados
3. **Observer**: Para eventos de Bukkit (Listener pattern)
4. **Strategy**: Para diferentes clases de jugador
5. **Repository**: Para acceso a datos (DatabaseManager)

### Estructura de Directorios

```
mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/
├── MMORPGPlugin.java              # Clase principal del plugin
├── managers/                      # Gestores de sistemas
│   ├── DatabaseManager.java       # Acceso a BD (Singleton)
│   ├── PlayerManager.java         # Gestión de jugadores
│   ├── QuestManager.java          # Sistema de quests
│   ├── EconomyManager.java        # Sistema económico
│   ├── SkillManager.java          # Skills y niveles
│   ├── ShopManager.java           # Tienda de items
│   ├── CombatManager.java         # Sistema de combate
│   ├── PartyManager.java          # Sistema de grupos
│   ├── CraftingManager.java       # Crafting personalizado
│   ├── EnchantmentManager.java    # Encantamientos
│   ├── RespawnManager.java        # Sistema de respawn
│   ├── DungeonManager.java        # Mazmorras instanciadas
│   ├── InvasionManager.java       # Eventos de invasión
│   ├── PetManager.java            # Sistema de mascotas
│   ├── SpawnManager.java          # Spawns personalizados
│   ├── RankManager.java           # Sistema de rangos
│   ├── AchievementManager.java    # Logros
│   └── BestiaryManager.java       # Bestiario de mobs
├── commands/                      # Comandos del plugin
│   ├── RPGCommand.java            # Comando principal /rpg
│   ├── QuestCommand.java          # /quest
│   ├── StatsCommand.java          # /stats
│   ├── ShopCommand.java           # /shop
│   ├── PartyCommand.java          # /party
│   └── SkillCommand.java          # /skill
├── listeners/                     # Event Listeners
│   ├── PlayerJoinListener.java
│   ├── PlayerQuitListener.java
│   ├── MobDeathListener.java
│   ├── PlayerDeathListener.java
│   └── DamageListener.java
├── models/                        # Clases de datos
│   ├── RPGPlayer.java             # Modelo de jugador
│   ├── Quest.java                 # Modelo de quest
│   ├── Skill.java                 # Modelo de skill
│   ├── CustomMob.java             # Modelo de mob
│   └── Party.java                 # Modelo de grupo
├── api/                           # API para web panel
│   └── RPGAdminAPI.java           # Métodos administrativos
└── utils/                         # Utilidades
    ├── ConfigUtil.java            # Gestión de config.yml
    ├── MessageUtil.java           # Mensajes formateados
    └── ItemBuilder.java           # Constructor de items
```

---

## Clase Principal: MMORPGPlugin.java

### Estructura Básica

```java
public class MMORPGPlugin extends JavaPlugin {
    private static MMORPGPlugin instance;
    
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private QuestManager questManager;
    // ... otros managers
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 1. Cargar configuración
        saveDefaultConfig();
        
        // 2. Inicializar base de datos
        databaseManager = DatabaseManager.getInstance(this);
        databaseManager.initTables();
        
        // 3. Inicializar managers
        playerManager = new PlayerManager(this);
        questManager = new QuestManager(this);
        // ... inicializar otros managers
        
        // 4. Registrar listeners
        registerListeners();
        
        // 5. Registrar comandos
        registerCommands();
        
        // 6. Tareas programadas
        startScheduledTasks();
        
        getLogger().info("MMORPG Plugin habilitado correctamente");
    }
    
    @Override
    public void onDisable() {
        // Guardar datos de jugadores online
        playerManager.saveAllPlayers();
        
        // Cerrar conexiones de BD
        databaseManager.close();
        
        getLogger().info("MMORPG Plugin deshabilitado");
    }
    
    public static MMORPGPlugin getInstance() {
        return instance;
    }
}
```

### Registro de Listeners

```java
private void registerListeners() {
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new PlayerJoinListener(this), this);
    pm.registerEvents(new PlayerQuitListener(this), this);
    pm.registerEvents(new MobDeathListener(this), this);
    pm.registerEvents(new PlayerDeathListener(this), this);
    pm.registerEvents(new DamageListener(this), this);
}
```

### Registro de Comandos

```java
private void registerCommands() {
    getCommand("rpg").setExecutor(new RPGCommand(this));
    getCommand("quest").setExecutor(new QuestCommand(this));
    getCommand("stats").setExecutor(new StatsCommand(this));
    getCommand("shop").setExecutor(new ShopCommand(this));
    getCommand("party").setExecutor(new PartyCommand(this));
    getCommand("skill").setExecutor(new SkillCommand(this));
}
```

---

## Managers

### DatabaseManager (Singleton)

```java
public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection universalConnection;
    private Map<String, Connection> worldConnections;
    
    private DatabaseManager(MMORPGPlugin plugin) {
        // Inicializar conexiones
    }
    
    public static DatabaseManager getInstance(MMORPGPlugin plugin) {
        if (instance == null) {
            instance = new DatabaseManager(plugin);
        }
        return instance;
    }
    
    public Connection getUniversalConnection() {
        return universalConnection;
    }
    
    public Connection getWorldConnection(String worldName) {
        return worldConnections.computeIfAbsent(worldName, 
            name -> createWorldConnection(name));
    }
}
```

### Patrón de Manager Típico

```java
public class QuestManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private Map<Integer, Quest> questCache;
    
    public QuestManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = DatabaseManager.getInstance(plugin);
        this.questCache = new HashMap<>();
        loadQuests();
    }
    
    private void loadQuests() {
        String sql = "SELECT * FROM quests WHERE status = 'active'";
        try (Connection conn = db.getUniversalConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Quest quest = new Quest(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    // ... otros campos
                );
                questCache.put(quest.getId(), quest);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando quests: " + e.getMessage());
        }
    }
    
    public Quest getQuest(int questId) {
        return questCache.get(questId);
    }
    
    public void assignQuest(UUID playerUuid, int questId) {
        // Lógica para asignar quest
    }
}
```

---

## Creación de un Nuevo Manager

### 1. Crear la Clase

```java
package com.nightslayer.mmorpg.managers;

import com.nightslayer.mmorpg.MMORPGPlugin;

public class MyNewManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public MyNewManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = DatabaseManager.getInstance(plugin);
        initialize();
    }
    
    private void initialize() {
        // Lógica de inicialización
        loadData();
        registerListeners();
    }
    
    private void loadData() {
        // Cargar datos desde BD
    }
    
    private void registerListeners() {
        // Si necesita escuchar eventos
    }
}
```

### 2. Registrar en MMORPGPlugin.java

```java
public class MMORPGPlugin extends JavaPlugin {
    private MyNewManager myNewManager;
    
    @Override
    public void onEnable() {
        // ... otras inicializaciones
        myNewManager = new MyNewManager(this);
    }
    
    public MyNewManager getMyNewManager() {
        return myNewManager;
    }
}
```

### 3. Añadir Tabla en DatabaseManager

```java
public void initTables() {
    // ... otras tablas
    
    createTable("CREATE TABLE IF NOT EXISTS my_new_table (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL, " +
        "value INTEGER DEFAULT 0" +
    ")");
}
```

---

## Creación de Comandos

### Estructura de Comando

```java
package com.nightslayer.mmorpg.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    
    public MyCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, 
                            String label, String[] args) {
        // Verificar que sea un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permisos
        if (!player.hasPermission("mmorpg.mycommand")) {
            player.sendMessage("§cNo tienes permiso para usar este comando");
            return true;
        }
        
        // Verificar argumentos
        if (args.length == 0) {
            player.sendMessage("§eUso: /mycommand <arg1> <arg2>");
            return true;
        }
        
        // Lógica del comando
        handleCommand(player, args);
        return true;
    }
    
    private void handleCommand(Player player, String[] args) {
        // Implementación
    }
}
```

### Registrar en plugin.yml

```yaml
commands:
  mycommand:
    description: Descripción del comando
    usage: /mycommand <arg1> <arg2>
    permission: mmorpg.mycommand
    aliases: [mc, mycmd]

permissions:
  mmorpg.mycommand:
    description: Permite usar /mycommand
    default: true
```

---

## Creación de Listeners

### Estructura de Listener

```java
package com.nightslayer.mmorpg.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class MyListener implements Listener {
    private final MMORPGPlugin plugin;
    
    public MyListener(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Lógica del evento
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.CHEST) {
                // Hacer algo con el cofre
            }
        }
    }
}
```

### Registrar en MMORPGPlugin.java

```java
pm.registerEvents(new MyListener(this), this);
```

---

## Uso de ConfigUtil

### Cargar Valores de config.yml

```java
public class MyManager {
    private boolean featureEnabled;
    private int maxValue;
    private String message;
    
    public MyManager(MMORPGPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        
        this.featureEnabled = config.getBoolean("feature.enabled", true);
        this.maxValue = config.getInt("feature.max_value", 100);
        this.message = config.getString("feature.message", "Default message");
    }
}
```

### Añadir en config.yml

```yaml
feature:
  enabled: true
  max_value: 100
  message: "§aFeature activada correctamente"
```

---

## Modelos de Datos

### Clase RPGPlayer

```java
public class RPGPlayer {
    private final UUID uuid;
    private String username;
    private int level;
    private int exp;
    private double coins;
    private String playerClass;
    
    // Stats
    private int strength;
    private int defense;
    private int speed;
    private double maxHealth;
    private double maxMana;
    
    public RPGPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        // Valores por defecto
        this.level = 1;
        this.exp = 0;
        this.coins = 100.0;
        this.playerClass = "warrior";
        this.strength = 10;
        this.defense = 10;
        this.speed = 10;
        this.maxHealth = 20.0;
        this.maxMana = 100.0;
    }
    
    // Getters y Setters
    public UUID getUUID() { return uuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    // Métodos de utilidad
    public void addCoins(double amount) {
        this.coins += amount;
    }
    
    public boolean hasEnoughCoins(double amount) {
        return this.coins >= amount;
    }
    
    public void addExp(int amount) {
        this.exp += amount;
        checkLevelUp();
    }
    
    private void checkLevelUp() {
        int requiredExp = level * 100;
        if (exp >= requiredExp) {
            level++;
            exp -= requiredExp;
            // Incrementar stats
            strength += 2;
            defense += 2;
            speed += 1;
        }
    }
}
```

---

## Testing

### Estructura de Tests

```java
package com.nightslayer.mmorpg.tests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {
    
    @Test
    public void testConnection() {
        DatabaseManager db = DatabaseManager.getInstance(mockPlugin);
        assertNotNull(db.getUniversalConnection());
    }
    
    @Test
    public void testPlayerCreation() {
        PlayerManager pm = new PlayerManager(mockPlugin);
        UUID testUUID = UUID.randomUUID();
        
        pm.createPlayer(testUUID, "TestPlayer");
        RPGPlayer player = pm.getPlayer(testUUID);
        
        assertNotNull(player);
        assertEquals("TestPlayer", player.getUsername());
        assertEquals(1, player.getLevel());
    }
}
```

### Ejecutar Tests

```bash
mvn test
```

---

## Compilación

### Maven

```bash
# Compilar el plugin
mvn clean package

# Saltar tests
mvn clean package -DskipTests

# Compilar con dependencias
mvn clean package shade:shade
```

### Ubicación del JAR

El archivo compilado estará en:
```
target/MMORPG-1.0-SNAPSHOT.jar
```

---

## Debugging

### Habilitar Debug Mode

En config.yml:
```yaml
debug_mode: true
```

En código:
```java
if (plugin.getConfig().getBoolean("debug_mode")) {
    plugin.getLogger().info("[DEBUG] Valor de X: " + x);
}
```

### Logs

```java
// INFO
plugin.getLogger().info("Información general");

// WARNING
plugin.getLogger().warning("Advertencia");

// SEVERE (Error)
plugin.getLogger().severe("Error crítico: " + e.getMessage());
e.printStackTrace();
```

---

## Buenas Prácticas

### 1. Usar PreparedStatement (Seguridad SQL)

```java
// ❌ MAL (SQL Injection)
String sql = "SELECT * FROM players WHERE username = '" + username + "'";

// ✅ BIEN (Safe)
String sql = "SELECT * FROM players WHERE username = ?";
try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, username);
    ResultSet rs = stmt.executeQuery();
}
```

### 2. Cerrar Recursos (Try-with-resources)

```java
// ✅ BIEN (Auto-close)
try (Connection conn = db.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    // Usar recursos
} catch (SQLException e) {
    e.printStackTrace();
}
```

### 3. Validar Inputs

```java
public void setLevel(int level) {
    if (level < 1 || level > 100) {
        throw new IllegalArgumentException("Level debe estar entre 1 y 100");
    }
    this.level = level;
}
```

### 4. No Bloquear el Main Thread

```java
// ❌ MAL (Bloquea servidor)
connection.createStatement().execute(sql);

// ✅ BIEN (Async)
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    try (Connection conn = db.getConnection()) {
        // Operación de BD
    }
});
```

### 5. Cache de Datos

```java
private Map<UUID, RPGPlayer> playerCache = new HashMap<>();

public RPGPlayer getPlayer(UUID uuid) {
    // Primero revisar cache
    if (playerCache.containsKey(uuid)) {
        return playerCache.get(uuid);
    }
    
    // Si no está en cache, cargar desde BD
    RPGPlayer player = loadPlayerFromDB(uuid);
    playerCache.put(uuid, player);
    return player;
}
```

---

## Recursos Útiles

- [Spigot API Docs](https://hub.spigotmc.org/javadocs/spigot/)
- [Paper API Docs](https://jd.papermc.io/paper/1.20/)
- [Bukkit Forums](https://bukkit.org/forums/)
- [Maven Repository](https://mvnrepository.com/)

---

## Roadmap de Desarrollo

1. **Corto Plazo**:
   - Implementar tests unitarios completos
   - Mejorar sistema de logs
   - Optimizar queries de BD

2. **Mediano Plazo**:
   - Migrar a MySQL (opcional)
   - WebSocket para panel en tiempo real
   - API REST completa

3. **Largo Plazo**:
   - Sistema de clanes
   - PvP arenas
   - Sistema de comercio entre jugadores
