#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# MMORPG Plugin - Auto-Generator Script
# Genera automáticamente todos los archivos restantes del sistema
# ═══════════════════════════════════════════════════════════════

set -e

PLUGIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/mmorpg-plugin"
SRC_DIR="$PLUGIN_DIR/src/main/java/com/nightslayer/mmorpg"

echo "═══════════════════════════════════════════════════════════════"
echo "  MMORPG Plugin Auto-Generator"
echo "  Generando archivos faltantes..."
echo "═══════════════════════════════════════════════════════════════"

# Crear directorios necesarios
mkdir -p "$SRC_DIR"/{npcs,quests,items,mobs,economy,crafting,enchanting,respawn,dungeons,invasions,pets,spawns,ranks,squads,achievements,bestiary,api,listeners,commands,events,utils,models}

echo "[1/7] Generando modelos de datos..."

# ═══════════════════════════════════════════════════════════════
# MODELS
# ═══════════════════════════════════════════════════════════════

cat > "$SRC_DIR/models/RPGPlayer.java" <<'EOF'
package com.nightslayer.mmorpg.models;

import java.util.UUID;

public class RPGPlayer {
    private UUID uuid;
    private String playerClass;
    private int level;
    private int experience;
    private int health;
    private int maxHealth;
    private int mana;
    private int maxMana;
    private int strength;
    private int intelligence;
    private int dexterity;
    private int vitality;
    private int coins;
    
    public RPGPlayer(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.experience = 0;
        this.playerClass = "none";
        this.health = 100;
        this.maxHealth = 100;
        this.mana = 50;
        this.maxMana = 50;
        this.strength = 10;
        this.intelligence = 10;
        this.dexterity = 10;
        this.vitality = 10;
        this.coins = 0;
    }
    
    public UUID getUuid() { return uuid; }
    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }
    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getIntelligence() { return intelligence; }
    public void setIntelligence(int intelligence) { this.intelligence = intelligence; }
    public int getDexterity() { return dexterity; }
    public void setDexterity(int dexterity) { this.dexterity = dexterity; }
    public int getVitality() { return vitality; }
    public void setVitality(int vitality) { this.vitality = vitality; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
}
EOF

cat > "$SRC_DIR/models/Quest.java" <<'EOF'
package com.nightslayer.mmorpg.models;

public class Quest {
    private int id;
    private String name;
    private String description;
    private int minLevel;
    private String type;
    private int coinReward;
    private int expReward;
    
    public Quest(int id, String name, String description, int minLevel, String type, int coinReward, int expReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minLevel = minLevel;
        this.type = type;
        this.coinReward = coinReward;
        this.expReward = expReward;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMinLevel() { return minLevel; }
    public String getType() { return type; }
    public int getCoinReward() { return coinReward; }
    public int getExpReward() { return expReward; }
}
EOF

echo "[2/7] Generando managers básicos (Fase 3)..."

# ═══════════════════════════════════════════════════════════════
# MANAGERS
# ═══════════════════════════════════════════════════════════════

cat > "$SRC_DIR/npcs/NPCManager.java" <<'EOF'
package com.nightslayer.mmorpg.npcs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NPCManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<Integer, NPC> npcs;
    
    public NPCManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.npcs = new HashMap<>();
        loadNPCs();
    }
    
    private void loadNPCs() {
        String sql = "SELECT * FROM npcs WHERE active = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                NPC npc = new NPC(id, name, type);
                npcs.put(id, npc);
            }
            plugin.getLogger().info("Loaded " + npcs.size() + " NPCs");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading NPCs", e);
        }
    }
    
    public NPC getNPC(int id) {
        return npcs.get(id);
    }
    
    public static class NPC {
        private final int id;
        private final String name;
        private final String type;
        
        public NPC(int id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
    }
}
EOF

cat > "$SRC_DIR/quests/QuestManager.java" <<'EOF'
package com.nightslayer.mmorpg.quests;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.models.Quest;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class QuestManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<Integer, Quest> quests;
    
    public QuestManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.quests = new HashMap<>();
        loadQuests();
    }
    
    private void loadQuests() {
        String sql = "SELECT * FROM quests WHERE active = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                int minLevel = rs.getInt("min_level");
                String type = rs.getString("type");
                int coinReward = rs.getInt("coin_reward");
                int expReward = rs.getInt("exp_reward");
                
                Quest quest = new Quest(id, name, description, minLevel, type, coinReward, expReward);
                quests.put(id, quest);
            }
            plugin.getLogger().info("Loaded " + quests.size() + " quests");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading quests", e);
        }
    }
    
    public Quest getQuest(int id) {
        return quests.get(id);
    }
    
    public boolean acceptQuest(UUID playerId, int questId) {
        String sql = "INSERT INTO player_quests (player_uuid, quest_id, status, progress) VALUES (?, ?, 'active', 0)";
        return db.executeUpdate(sql, playerId.toString(), questId) > 0;
    }
    
    public boolean completeQuest(UUID playerId, int questId) {
        String sql = "UPDATE player_quests SET status = 'completed', completed_at = CURRENT_TIMESTAMP WHERE player_uuid = ? AND quest_id = ?";
        return db.executeUpdate(sql, playerId.toString(), questId) > 0;
    }
}
EOF

cat > "$SRC_DIR/items/ItemManager.java" <<'EOF'
package com.nightslayer.mmorpg.items;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private final MMORPGPlugin plugin;
    
    public ItemManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createCustomItem(String itemId, String name, List<String> lore, Material material) {
        ItemStack item = new ItemStack(material);
        ItemBuilder builder = new ItemBuilder(item, plugin)
            .setDisplayName(name)
            .setLore(lore)
            .setPersistentData("custom_item_id", itemId);
        return builder.build();
    }
    
    public boolean isCustomItem(ItemStack item, String itemId) {
        String id = ItemBuilder.getPersistentData(item, plugin, "custom_item_id");
        return id != null && id.equals(itemId);
    }
}
EOF

cat > "$SRC_DIR/mobs/MobManager.java" <<'EOF'
package com.nightslayer.mmorpg.mobs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MobManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<String, CustomMob> customMobs;
    
    public MobManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.customMobs = new HashMap<>();
        loadCustomMobs();
    }
    
    private void loadCustomMobs() {
        String sql = "SELECT * FROM custom_mobs WHERE active = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("mob_id");
                String entityType = rs.getString("entity_type");
                String displayName = rs.getString("display_name");
                int level = rs.getInt("level");
                double health = rs.getDouble("health");
                double damage = rs.getDouble("damage");
                
                CustomMob mob = new CustomMob(id, entityType, displayName, level, health, damage);
                customMobs.put(id, mob);
            }
            plugin.getLogger().info("Loaded " + customMobs.size() + " custom mobs");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading custom mobs", e);
        }
    }
    
    public void applyCustomMob(LivingEntity entity, String mobId) {
        CustomMob mob = customMobs.get(mobId);
        if (mob != null) {
            entity.setCustomName(mob.displayName);
            entity.setCustomNameVisible(true);
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mob.health);
            entity.setHealth(mob.health);
        }
    }
    
    public static class CustomMob {
        private final String id;
        private final String entityType;
        private final String displayName;
        private final int level;
        private final double health;
        private final double damage;
        
        public CustomMob(String id, String entityType, String displayName, int level, double health, double damage) {
            this.id = id;
            this.entityType = entityType;
            this.displayName = displayName;
            this.level = level;
            this.health = health;
            this.damage = damage;
        }
        
        public String getId() { return id; }
        public int getLevel() { return level; }
        public double getDamage() { return damage; }
    }
}
EOF

cat > "$SRC_DIR/economy/EconomyManager.java" <<'EOF'
package com.nightslayer.mmorpg.economy;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;

import java.sql.ResultSet;
import java.util.UUID;

public class EconomyManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public EconomyManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }
    
    public int getBalance(UUID playerId) {
        String sql = "SELECT coins FROM player_economy WHERE player_uuid = ?";
        try (ResultSet rs = db.executeQuery(sql, playerId.toString())) {
            if (rs.next()) {
                return rs.getInt("coins");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean deposit(UUID playerId, int amount) {
        String sql = "UPDATE player_economy SET coins = coins + ? WHERE player_uuid = ?";
        return db.executeUpdate(sql, amount, playerId.toString()) > 0;
    }
    
    public boolean withdraw(UUID playerId, int amount) {
        if (getBalance(playerId) < amount) {
            return false;
        }
        String sql = "UPDATE player_economy SET coins = coins - ? WHERE player_uuid = ?";
        return db.executeUpdate(sql, amount, playerId.toString()) > 0;
    }
    
    public boolean transfer(UUID from, UUID to, int amount) {
        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                // Log transaction
                String sql = "INSERT INTO transactions (from_player, to_player, amount, type) VALUES (?, ?, ?, 'transfer')";
                db.executeUpdate(sql, from.toString(), to.toString(), amount);
                return true;
            } else {
                // Rollback
                deposit(from, amount);
            }
        }
        return false;
    }
}
EOF

echo "[3/7] Generando listeners..."

# ═══════════════════════════════════════════════════════════════
# LISTENERS
# ═══════════════════════════════════════════════════════════════

cat > "$SRC_DIR/listeners/PlayerListener.java" <<'EOF'
package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public PlayerListener(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        
        // Create player record if not exists
        String sql = "INSERT OR IGNORE INTO players (uuid, username, player_class, level, experience) VALUES (?, ?, 'none', 1, 0)";
        db.executeUpdate(sql, uuid, event.getPlayer().getName());
        
        // Create economy record
        sql = "INSERT OR IGNORE INTO player_economy (player_uuid, coins) VALUES (?, 100)";
        db.executeUpdate(sql, uuid);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data
        // TODO: Implement auto-save
    }
}
EOF

cat > "$SRC_DIR/listeners/MobDeathListener.java" <<'EOF'
package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDeathListener implements Listener {
    private final MMORPGPlugin plugin;
    
    public MobDeathListener(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = event.getEntity().getKiller();
            // TODO: Give experience and coins
            // TODO: Update bestiary
        }
    }
}
EOF

echo "[4/7] Generando comandos..."

# ═══════════════════════════════════════════════════════════════
# COMMANDS
# ═══════════════════════════════════════════════════════════════

cat > "$SRC_DIR/commands/ClassCommand.java" <<'EOF'
package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public ClassCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("general.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(lang.getMessage("classes.choose_prompt"));
            return true;
        }
        
        String playerClass = args[0].toLowerCase();
        if (!playerClass.matches("warrior|mage|rogue|paladin")) {
            player.sendMessage(lang.getMessage("classes.choose_prompt"));
            return true;
        }
        
        // Update database
        String sql = "UPDATE players SET player_class = ? WHERE uuid = ?";
        plugin.getDatabaseManager().executeUpdate(sql, playerClass, player.getUniqueId().toString());
        
        player.sendMessage(lang.getMessage("classes.changed", "class", playerClass));
        return true;
    }
}
EOF

cat > "$SRC_DIR/commands/StatsCommand.java" <<'EOF'
package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

public class StatsCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public StatsCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("general.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        String sql = "SELECT * FROM players WHERE uuid = ?";
        
        try (ResultSet rs = plugin.getDatabaseManager().executeQuery(sql, player.getUniqueId().toString())) {
            if (rs.next()) {
                player.sendMessage(lang.getMessage("stats.title", "player", player.getName()));
                player.sendMessage(lang.getMessage("stats.level", "level", rs.getInt("level")));
                player.sendMessage(lang.getMessage("stats.experience", "exp", rs.getInt("experience"), "max_exp", 1000));
            }
        } catch (Exception e) {
            player.sendMessage(lang.getMessage("general.error"));
            e.printStackTrace();
        }
        
        return true;
    }
}
EOF

cat > "$SRC_DIR/commands/BalanceCommand.java" <<'EOF'
package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public BalanceCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("general.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        int balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        player.sendMessage(lang.getMessage("economy.balance", "coins", balance));
        
        return true;
    }
}
EOF

echo "[5/7] Generando gestores avanzados (Fase 4 - parcial)..."

cat > "$SRC_DIR/squads/SquadManager.java" <<'EOF'
package com.nightslayer.mmorpg.squads;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;

import java.util.UUID;

public class SquadManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public SquadManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }
    
    public boolean createSquad(UUID leaderId, String squadName) {
        String sql = "INSERT INTO squads (squad_name, leader_uuid, max_members, bank_balance) VALUES (?, ?, 10, 0)";
        if (db.executeUpdate(sql, squadName, leaderId.toString()) > 0) {
            // Add leader as member
            sql = "INSERT INTO squad_members (squad_id, player_uuid, rank) VALUES (last_insert_rowid(), ?, 'leader')";
            db.executeUpdate(sql, leaderId.toString());
            return true;
        }
        return false;
    }
    
    public boolean addMember(int squadId, UUID playerId) {
        String sql = "INSERT INTO squad_members (squad_id, player_uuid, rank) VALUES (?, ?, 'member')";
        return db.executeUpdate(sql, squadId, playerId.toString()) > 0;
    }
}
EOF

echo "[6/7] Generando panel web Flask (Fase 5)..."

mkdir -p web/templates web/static/css web/static/js

cat > "web/app.py" <<'EOF'
from flask import Flask, render_template, request, redirect, url_for, session, jsonify
import sqlite3
import bcrypt
from functools import wraps
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)

DB_PATH = '../config/data/universal.db'

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

@app.route('/')
@login_required
def index():
    return render_template('dashboard.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("SELECT id, password_hash FROM admin_users WHERE username = ?", (username,))
        user = c.fetchone()
        conn.close()
        
        if user and bcrypt.checkpw(password.encode(), user[1].encode()):
            session['user_id'] = user[0]
            return redirect(url_for('index'))
        return render_template('login.html', error='Invalid credentials')
    
    return render_template('login.html')

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('login'))

@app.route('/api/players')
@login_required
def api_players():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT uuid, username, player_class, level, experience FROM players ORDER BY level DESC LIMIT 100")
    players = c.fetchall()
    conn.close()
    
    return jsonify([{
        'uuid': p[0],
        'username': p[1],
        'class': p[2],
        'level': p[3],
        'experience': p[4]
    } for p in players])

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
EOF

cat > "web/requirements.txt" <<'EOF'
Flask==3.0.3
bcrypt==4.1.3
EOF

cat > "web/templates/login.html" <<'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>MMORPG Panel - Login</title>
    <link rel="stylesheet" href="/static/css/style.css">
</head>
<body>
    <div class="login-container">
        <h1>MMORPG Admin Panel</h1>
        <form method="POST">
            <input type="text" name="username" placeholder="Username" required>
            <input type="password" name="password" placeholder="Password" required>
            <button type="submit">Login</button>
        </form>
    </div>
</body>
</html>
EOF

cat > "web/templates/dashboard.html" <<'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>MMORPG Panel - Dashboard</title>
    <link rel="stylesheet" href="/static/css/style.css">
</head>
<body>
    <div class="dashboard">
        <h1>Server Dashboard</h1>
        <div id="players-list"></div>
    </div>
    <script src="/static/js/main.js"></script>
</body>
</html>
EOF

cat > "web/static/css/style.css" <<'EOF'
body {
    font-family: Arial, sans-serif;
    background: #1a1a1a;
    color: #fff;
}

.login-container {
    max-width: 400px;
    margin: 100px auto;
    padding: 20px;
    background: #2a2a2a;
    border-radius: 10px;
}

form input {
    width: 100%;
    padding: 10px;
    margin: 10px 0;
    border: none;
    border-radius: 5px;
}

button {
    width: 100%;
    padding: 10px;
    background: #4CAF50;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
}

button:hover {
    background: #45a049;
}
EOF

cat > "web/static/js/main.js" <<'EOF'
fetch('/api/players')
    .then(response => response.json())
    .then(data => {
        const list = document.getElementById('players-list');
        data.forEach(player => {
            list.innerHTML += `<p>${player.username} - Level ${player.level} ${player.class}</p>`;
        });
    });
EOF

cat > "web/start-web.sh" <<'EOF'
#!/bin/bash
source venv/bin/activate
python app.py
EOF
chmod +x "web/start-web.sh"

echo "[7/7] Generando script de instalación (Fase 6)..."

cat > "install-native.sh" <<'EOFINSTALL'
#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Minecraft MMORPG System - Native Installation Script
# ═══════════════════════════════════════════════════════════════

set -e

INSTALL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_VERSION="21"
PAPER_VERSION="1.20.6"
PAPER_BUILD="151"

echo "═══════════════════════════════════════════════════════════════"
echo "  Minecraft MMORPG System - Installer"
echo "  Version: 1.0.0"
echo "═══════════════════════════════════════════════════════════════"

# Check Java
echo "[1/8] Checking Java $JAVA_VERSION..."
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "version \"$JAVA_VERSION"; then
    echo "Java $JAVA_VERSION not found. Installing..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sudo apt update
        sudo apt install -y openjdk-21-jdk
    else
        echo "Please install Java $JAVA_VERSION manually"
        exit 1
    fi
fi

# Check Maven
echo "[2/8] Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Installing..."
    sudo apt install -y maven
fi

# Download Paper
echo "[3/8] Downloading Paper $PAPER_VERSION..."
mkdir -p server
cd server
if [ ! -f "paper-$PAPER_VERSION-$PAPER_BUILD.jar" ]; then
    wget "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
fi

# Accept EULA
echo "eula=true" > eula.txt

# Build plugin
echo "[4/8] Building MMORPG plugin..."
cd "$INSTALL_DIR/mmorpg-plugin"
mvn clean package -DskipTests
cp target/mmorpg-plugin-*.jar "$INSTALL_DIR/server/plugins/"

# Copy configs
echo "[5/8] Copying configuration files..."
cp -r "$INSTALL_DIR/config/"* "$INSTALL_DIR/server/"

# Setup Python environment
echo "[6/8] Setting up Python environment for web panel..."
cd "$INSTALL_DIR/web"
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Create systemd services
echo "[7/8] Creating systemd services..."
sudo tee /etc/systemd/system/mmorpg-server.service > /dev/null <<EOF
[Unit]
Description=Minecraft MMORPG Server
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$INSTALL_DIR/server
ExecStart=/usr/bin/java -Xms4G -Xmx4G -jar paper-$PAPER_VERSION-$PAPER_BUILD.jar nogui
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo tee /etc/systemd/system/mmorpg-web.service > /dev/null <<EOF
[Unit]
Description=MMORPG Web Panel
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$INSTALL_DIR/web
ExecStart=$INSTALL_DIR/web/venv/bin/python app.py
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload

echo "[8/8] Starting services..."
sudo systemctl start mmorpg-server
sudo systemctl enable mmorpg-server
sudo systemctl start mmorpg-web
sudo systemctl enable mmorpg-web

echo "═══════════════════════════════════════════════════════════════"
echo "  Installation Complete!"
echo "  Server: localhost:25565"
echo "  Web Panel: http://localhost:5000"
echo "  Default credentials: admin/admin"
echo "═══════════════════════════════════════════════════════════════"
EOFINSTALL

chmod +x install-native.sh

echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ Generación completa!"
echo "  Archivos generados en:"
echo "  - Managers: NPCManager, QuestManager, ItemManager, MobManager, EconomyManager, SquadManager"
echo "  - Listeners: PlayerListener, MobDeathListener"
echo "  - Commands: ClassCommand, StatsCommand, BalanceCommand"
echo "  - Models: RPGPlayer, Quest"
echo "  - Web Panel: Flask app con templates y API"
echo "  - Install Script: install-native.sh"
echo "═══════════════════════════════════════════════════════════════"
EOF
