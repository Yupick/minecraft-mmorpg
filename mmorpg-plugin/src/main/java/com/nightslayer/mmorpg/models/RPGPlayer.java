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
