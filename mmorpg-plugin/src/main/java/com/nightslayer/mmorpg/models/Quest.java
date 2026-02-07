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
