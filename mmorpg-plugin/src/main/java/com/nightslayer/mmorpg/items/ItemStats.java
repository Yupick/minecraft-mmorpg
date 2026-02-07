package com.nightslayer.mmorpg.items;

import com.nightslayer.mmorpg.MMORPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Represents RPG statistics for items (damage, defense, etc.)
 */
public class ItemStats {
    
    private int damage;
    private int defense;
    private int health;
    private int mana;
    private int critChance; // Percentage (0-100)
    private int critDamage; // Percentage multiplier
    private int speed; // Movement speed bonus
    private int magicDamage;
    private int magicDefense;
    private int lifesteal; // Percentage
    private int luck; // Affects drops and loot
    
    public ItemStats() {
        this.damage = 0;
        this.defense = 0;
        this.health = 0;
        this.mana = 0;
        this.critChance = 0;
        this.critDamage = 0;
        this.speed = 0;
        this.magicDamage = 0;
        this.magicDefense = 0;
        this.lifesteal = 0;
        this.luck = 0;
    }
    
    /**
     * Checks if this stats object has any non-zero stats
     */
    public boolean hasStats() {
        return damage != 0 || defense != 0 || health != 0 || mana != 0 || 
               critChance != 0 || critDamage != 0 || speed != 0 ||
               magicDamage != 0 || magicDefense != 0 || lifesteal != 0 || luck != 0;
    }
    
    /**
     * Converts stats to lore components
     */
    public List<Component> toLore() {
        List<Component> lore = new ArrayList<>();
        
        if (damage > 0) {
            lore.add(Component.text("  ⚔ Daño: +", NamedTextColor.GRAY)
                    .append(Component.text(damage, NamedTextColor.RED)));
        }
        if (defense > 0) {
            lore.add(Component.text("  🛡 Defensa: +", NamedTextColor.GRAY)
                    .append(Component.text(defense, NamedTextColor.BLUE)));
        }
        if (health > 0) {
            lore.add(Component.text("  ❤ Vida: +", NamedTextColor.GRAY)
                    .append(Component.text(health, NamedTextColor.RED)));
        }
        if (mana > 0) {
            lore.add(Component.text("  ✦ Maná: +", NamedTextColor.GRAY)
                    .append(Component.text(mana, NamedTextColor.AQUA)));
        }
        if (magicDamage > 0) {
            lore.add(Component.text("  ✨ Daño Mágico: +", NamedTextColor.GRAY)
                    .append(Component.text(magicDamage, NamedTextColor.LIGHT_PURPLE)));
        }
        if (magicDefense > 0) {
            lore.add(Component.text("  🌟 Defensa Mágica: +", NamedTextColor.GRAY)
                    .append(Component.text(magicDefense, NamedTextColor.LIGHT_PURPLE)));
        }
        if (critChance > 0) {
            lore.add(Component.text("  ⚡ Prob. Crítico: +", NamedTextColor.GRAY)
                    .append(Component.text(critChance + "%", NamedTextColor.YELLOW)));
        }
        if (critDamage > 0) {
            lore.add(Component.text("  💥 Daño Crítico: +", NamedTextColor.GRAY)
                    .append(Component.text(critDamage + "%", NamedTextColor.GOLD)));
        }
        if (speed > 0) {
            lore.add(Component.text("  👟 Velocidad: +", NamedTextColor.GRAY)
                    .append(Component.text(speed + "%", NamedTextColor.GREEN)));
        }
        if (lifesteal > 0) {
            lore.add(Component.text("  🩸 Robo de Vida: +", NamedTextColor.GRAY)
                    .append(Component.text(lifesteal + "%", NamedTextColor.DARK_RED)));
        }
        if (luck > 0) {
            lore.add(Component.text("  🍀 Suerte: +", NamedTextColor.GRAY)
                    .append(Component.text(luck, NamedTextColor.GREEN)));
        }
        
        return lore;
    }
    
    /**
     * Saves stats to persistent data container
     */
    public void saveToPersistentData(PersistentDataContainer container, MMORPGPlugin plugin) {
        if (damage != 0) {
            container.set(new NamespacedKey(plugin, "stat_damage"), PersistentDataType.INTEGER, damage);
        }
        if (defense != 0) {
            container.set(new NamespacedKey(plugin, "stat_defense"), PersistentDataType.INTEGER, defense);
        }
        if (health != 0) {
            container.set(new NamespacedKey(plugin, "stat_health"), PersistentDataType.INTEGER, health);
        }
        if (mana != 0) {
            container.set(new NamespacedKey(plugin, "stat_mana"), PersistentDataType.INTEGER, mana);
        }
        if (critChance != 0) {
            container.set(new NamespacedKey(plugin, "stat_crit_chance"), PersistentDataType.INTEGER, critChance);
        }
        if (critDamage != 0) {
            container.set(new NamespacedKey(plugin, "stat_crit_damage"), PersistentDataType.INTEGER, critDamage);
        }
        if (speed != 0) {
            container.set(new NamespacedKey(plugin, "stat_speed"), PersistentDataType.INTEGER, speed);
        }
        if (magicDamage != 0) {
            container.set(new NamespacedKey(plugin, "stat_magic_damage"), PersistentDataType.INTEGER, magicDamage);
        }
        if (magicDefense != 0) {
            container.set(new NamespacedKey(plugin, "stat_magic_defense"), PersistentDataType.INTEGER, magicDefense);
        }
        if (lifesteal != 0) {
            container.set(new NamespacedKey(plugin, "stat_lifesteal"), PersistentDataType.INTEGER, lifesteal);
        }
        if (luck != 0) {
            container.set(new NamespacedKey(plugin, "stat_luck"), PersistentDataType.INTEGER, luck);
        }
    }
    
    /**
     * Loads stats from persistent data container
     */
    public static ItemStats loadFromPersistentData(PersistentDataContainer container, MMORPGPlugin plugin) {
        ItemStats stats = new ItemStats();
        
        Integer damage = container.get(new NamespacedKey(plugin, "stat_damage"), PersistentDataType.INTEGER);
        if (damage != null) stats.damage = damage;
        
        Integer defense = container.get(new NamespacedKey(plugin, "stat_defense"), PersistentDataType.INTEGER);
        if (defense != null) stats.defense = defense;
        
        Integer health = container.get(new NamespacedKey(plugin, "stat_health"), PersistentDataType.INTEGER);
        if (health != null) stats.health = health;
        
        Integer mana = container.get(new NamespacedKey(plugin, "stat_mana"), PersistentDataType.INTEGER);
        if (mana != null) stats.mana = mana;
        
        Integer critChance = container.get(new NamespacedKey(plugin, "stat_crit_chance"), PersistentDataType.INTEGER);
        if (critChance != null) stats.critChance = critChance;
        
        Integer critDamage = container.get(new NamespacedKey(plugin, "stat_crit_damage"), PersistentDataType.INTEGER);
        if (critDamage != null) stats.critDamage = critDamage;
        
        Integer speed = container.get(new NamespacedKey(plugin, "stat_speed"), PersistentDataType.INTEGER);
        if (speed != null) stats.speed = speed;
        
        Integer magicDamage = container.get(new NamespacedKey(plugin, "stat_magic_damage"), PersistentDataType.INTEGER);
        if (magicDamage != null) stats.magicDamage = magicDamage;
        
        Integer magicDefense = container.get(new NamespacedKey(plugin, "stat_magic_defense"), PersistentDataType.INTEGER);
        if (magicDefense != null) stats.magicDefense = magicDefense;
        
        Integer lifesteal = container.get(new NamespacedKey(plugin, "stat_lifesteal"), PersistentDataType.INTEGER);
        if (lifesteal != null) stats.lifesteal = lifesteal;
        
        Integer luck = container.get(new NamespacedKey(plugin, "stat_luck"), PersistentDataType.INTEGER);
        if (luck != null) stats.luck = luck;
        
        return stats;
    }
    
    /**
     * Adds another ItemStats to this one
     */
    public void add(ItemStats other) {
        this.damage += other.damage;
        this.defense += other.defense;
        this.health += other.health;
        this.mana += other.mana;
        this.critChance += other.critChance;
        this.critDamage += other.critDamage;
        this.speed += other.speed;
        this.magicDamage += other.magicDamage;
        this.magicDefense += other.magicDefense;
        this.lifesteal += other.lifesteal;
        this.luck += other.luck;
    }
    
    /**
     * Multiplies all stats by a factor
     */
    public void multiply(double factor) {
        this.damage = (int) (this.damage * factor);
        this.defense = (int) (this.defense * factor);
        this.health = (int) (this.health * factor);
        this.mana = (int) (this.mana * factor);
        this.critChance = (int) (this.critChance * factor);
        this.critDamage = (int) (this.critDamage * factor);
        this.speed = (int) (this.speed * factor);
        this.magicDamage = (int) (this.magicDamage * factor);
        this.magicDefense = (int) (this.magicDefense * factor);
        this.lifesteal = (int) (this.lifesteal * factor);
        this.luck = (int) (this.luck * factor);
    }
    
    // Getters and setters
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    
    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }
    
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }
    
    public int getCritChance() { return critChance; }
    public void setCritChance(int critChance) { this.critChance = critChance; }
    
    public int getCritDamage() { return critDamage; }
    public void setCritDamage(int critDamage) { this.critDamage = critDamage; }
    
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    
    public int getMagicDamage() { return magicDamage; }
    public void setMagicDamage(int magicDamage) { this.magicDamage = magicDamage; }
    
    public int getMagicDefense() { return magicDefense; }
    public void setMagicDefense(int magicDefense) { this.magicDefense = magicDefense; }
    
    public int getLifesteal() { return lifesteal; }
    public void setLifesteal(int lifesteal) { this.lifesteal = lifesteal; }
    
    public int getLuck() { return luck; }
    public void setLuck(int luck) { this.luck = luck; }
    
    /**
     * Builder for creating item stats
     */
    public static class Builder {
        private final ItemStats stats = new ItemStats();
        
        public Builder damage(int damage) {
            stats.damage = damage;
            return this;
        }
        
        public Builder defense(int defense) {
            stats.defense = defense;
            return this;
        }
        
        public Builder health(int health) {
            stats.health = health;
            return this;
        }
        
        public Builder mana(int mana) {
            stats.mana = mana;
            return this;
        }
        
        public Builder critChance(int critChance) {
            stats.critChance = critChance;
            return this;
        }
        
        public Builder critDamage(int critDamage) {
            stats.critDamage = critDamage;
            return this;
        }
        
        public Builder speed(int speed) {
            stats.speed = speed;
            return this;
        }
        
        public Builder magicDamage(int magicDamage) {
            stats.magicDamage = magicDamage;
            return this;
        }
        
        public Builder magicDefense(int magicDefense) {
            stats.magicDefense = magicDefense;
            return this;
        }
        
        public Builder lifesteal(int lifesteal) {
            stats.lifesteal = lifesteal;
            return this;
        }
        
        public Builder luck(int luck) {
            stats.luck = luck;
            return this;
        }
        
        public ItemStats build() {
            return stats;
        }
    }
}
