package com.nightslayer.mmorpg.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;


/**
 * Represents a quest objective that must be completed
 */
public class QuestObjective {
    
    private final String objectiveId;
    private final ObjectiveType type;
    private final String target;
    private final int required;
    private int current;
    private final String description;
    private boolean completed;
    
    public QuestObjective(String objectiveId, ObjectiveType type, String target, int required, String description) {
        this.objectiveId = objectiveId;
        this.type = type;
        this.target = target;
        this.required = required;
        this.current = 0;
        this.description = description;
        this.completed = false;
    }
    
    /**
     * Adds progress to this objective
     */
    public void addProgress(int amount) {
        if (completed) return;
        
        current += amount;
        if (current >= required) {
            current = required;
            completed = true;
        }
    }
    
    /**
     * Sets the current progress
     */
    public void setProgress(int progress) {
        this.current = Math.min(progress, required);
        this.completed = (current >= required);
    }
    
    /**
     * Checks if this objective is completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Gets the progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (required == 0) return 100;
        return (int) ((current / (double) required) * 100);
    }
    
    /**
     * Gets a formatted progress string
     */
    public String getProgressString() {
        return current + "/" + required;
    }
    
    /**
     * Checks if a given target matches this objective
     */
    public boolean matchesTarget(String targetToCheck) {
        if (target == null || targetToCheck == null) return false;
        return target.equalsIgnoreCase(targetToCheck);
    }
    
    /**
     * Resets the objective progress
     */
    public void reset() {
        this.current = 0;
        this.completed = false;
    }
    
    // Getters
    public String getObjectiveId() {
        return objectiveId;
    }
    
    public ObjectiveType getType() {
        return type;
    }
    
    public String getTarget() {
        return target;
    }
    
    public int getRequired() {
        return required;
    }
    
    public int getCurrent() {
        return current;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Types of quest objectives
     */
    public enum ObjectiveType {
        KILL_MOBS("Matar mobs"),
        COLLECT_ITEMS("Recolectar items"),
        TALK_TO_NPC("Hablar con NPC"),
        REACH_LOCATION("Llegar a ubicación"),
        CRAFT_ITEMS("Craftear items"),
        MINE_BLOCKS("Minar bloques"),
        FISH_ITEMS("Pescar items"),
        GAIN_EXPERIENCE("Ganar experiencia"),
        DEAL_DAMAGE("Infligir daño"),
        COMPLETE_DUNGEON("Completar dungeon"),
        DEFEAT_BOSS("Derrotar boss"),
        USE_SKILL("Usar habilidad"),
        ENCHANT_ITEMS("Encantar items"),
        TRADE_WITH_NPC("Comerciar con NPC");
        
        private final String displayName;
        
        ObjectiveType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Builder for creating quest objectives
     */
    public static class Builder {
        private String objectiveId;
        private ObjectiveType type;
        private String target;
        private int required;
        private String description;
        
        public Builder(String objectiveId) {
            this.objectiveId = objectiveId;
        }
        
        public Builder type(ObjectiveType type) {
            this.type = type;
            return this;
        }
        
        public Builder target(String target) {
            this.target = target;
            return this;
        }
        
        public Builder target(Material material) {
            this.target = material.name();
            return this;
        }
        
        public Builder target(EntityType entityType) {
            this.target = entityType.name();
            return this;
        }
        
        public Builder required(int required) {
            this.required = required;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public QuestObjective build() {
            if (objectiveId == null || type == null) {
                throw new IllegalStateException("ObjectiveId and type are required");
            }
            return new QuestObjective(objectiveId, type, target, required, description);
        }
    }
    
    /**
     * Creates a kill mobs objective
     */
    public static QuestObjective killMobs(String objectiveId, EntityType mobType, int amount) {
        return new Builder(objectiveId)
                .type(ObjectiveType.KILL_MOBS)
                .target(mobType)
                .required(amount)
                .description("Mata " + amount + " " + mobType.name().replace("_", " "))
                .build();
    }
    
    /**
     * Creates a collect items objective
     */
    public static QuestObjective collectItems(String objectiveId, Material material, int amount) {
        return new Builder(objectiveId)
                .type(ObjectiveType.COLLECT_ITEMS)
                .target(material)
                .required(amount)
                .description("Recolecta " + amount + " " + material.name().replace("_", " "))
                .build();
    }
    
    /**
     * Creates a talk to NPC objective
     */
    public static QuestObjective talkToNPC(String objectiveId, String npcName) {
        return new Builder(objectiveId)
                .type(ObjectiveType.TALK_TO_NPC)
                .target(npcName)
                .required(1)
                .description("Habla con " + npcName)
                .build();
    }
    
    /**
     * Creates a craft items objective
     */
    public static QuestObjective craftItems(String objectiveId, Material material, int amount) {
        return new Builder(objectiveId)
                .type(ObjectiveType.CRAFT_ITEMS)
                .target(material)
                .required(amount)
                .description("Craftea " + amount + " " + material.name().replace("_", " "))
                .build();
    }
    
    /**
     * Creates a mine blocks objective
     */
    public static QuestObjective mineBlocks(String objectiveId, Material material, int amount) {
        return new Builder(objectiveId)
                .type(ObjectiveType.MINE_BLOCKS)
                .target(material)
                .required(amount)
                .description("Mina " + amount + " " + material.name().replace("_", " "))
                .build();
    }
}
