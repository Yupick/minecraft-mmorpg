package com.nightslayer.mmorpg.economy;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a financial transaction in the economy system
 */
public class Transaction {
    
    private final int id;
    private final UUID playerUuid;
    private final TransactionType type;
    private final int amount;
    private final String description;
    private final Timestamp timestamp;
    private final int balanceBefore;
    private final int balanceAfter;
    
    public Transaction(int id, UUID playerUuid, TransactionType type, int amount, 
                      String description, Timestamp timestamp, int balanceBefore, int balanceAfter) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
    }
    
    /**
     * Creates a new transaction record (for insertion)
     */
    public Transaction(UUID playerUuid, TransactionType type, int amount, String description, 
                      int balanceBefore, int balanceAfter) {
        this(0, playerUuid, type, amount, description, new Timestamp(System.currentTimeMillis()), 
             balanceBefore, balanceAfter);
    }
    
    /**
     * Gets a formatted transaction description
     */
    public String getFormattedDescription() {
        String prefix = type == TransactionType.DEPOSIT ? "+" : "-";
        return String.format("%s%d coins - %s", prefix, amount, description);
    }
    
    /**
     * Gets a human-readable timestamp
     */
    public String getFormattedTimestamp() {
        return timestamp.toString();
    }
    
    /**
     * Checks if this is a recent transaction (within last hour)
     */
    public boolean isRecent() {
        long hourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        return timestamp.getTime() > hourAgo;
    }
    
    /**
     * Checks if this is a large transaction (>= 1000 coins)
     */
    public boolean isLarge() {
        return amount >= 1000;
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public int getBalanceBefore() {
        return balanceBefore;
    }
    
    public int getBalanceAfter() {
        return balanceAfter;
    }
    
    /**
     * Transaction types
     */
    public enum TransactionType {
        DEPOSIT("Depósito"),
        WITHDRAW("Retiro"),
        TRANSFER_SENT("Transferencia Enviada"),
        TRANSFER_RECEIVED("Transferencia Recibida"),
        PURCHASE("Compra"),
        SALE("Venta"),
        QUEST_REWARD("Recompensa de Quest"),
        ADMIN_GIVE("Admin - Dar"),
        ADMIN_TAKE("Admin - Quitar"),
        TAX("Impuesto"),
        PENALTY("Penalización");
        
        private final String displayName;
        
        TransactionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isPositive() {
            return this == DEPOSIT || this == TRANSFER_RECEIVED || 
                   this == SALE || this == QUEST_REWARD || this == ADMIN_GIVE;
        }
        
        public boolean isNegative() {
            return this == WITHDRAW || this == TRANSFER_SENT || 
                   this == PURCHASE || this == ADMIN_TAKE || this == TAX || this == PENALTY;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id=%d, player=%s, type=%s, amount=%d, desc='%s', time=%s}",
                id, playerUuid, type, amount, description, timestamp);
    }
    
    /**
     * Builder for creating transactions
     */
    public static class Builder {
        private UUID playerUuid;
        private TransactionType type;
        private int amount;
        private String description;
        private int balanceBefore;
        private int balanceAfter;
        
        public Builder playerUuid(UUID uuid) {
            this.playerUuid = uuid;
            return this;
        }
        
        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }
        
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder balanceBefore(int balanceBefore) {
            this.balanceBefore = balanceBefore;
            return this;
        }
        
        public Builder balanceAfter(int balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }
        
        public Transaction build() {
            if (playerUuid == null || type == null) {
                throw new IllegalStateException("playerUuid and type are required");
            }
            return new Transaction(playerUuid, type, amount, description, balanceBefore, balanceAfter);
        }
    }
    
    /**
     * Creates a deposit transaction
     */
    public static Transaction deposit(UUID playerUuid, int amount, String description, 
                                     int balanceBefore, int balanceAfter) {
        return new Builder()
                .playerUuid(playerUuid)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }
    
    /**
     * Creates a withdraw transaction
     */
    public static Transaction withdraw(UUID playerUuid, int amount, String description,
                                      int balanceBefore, int balanceAfter) {
        return new Builder()
                .playerUuid(playerUuid)
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }
    
    /**
     * Creates a purchase transaction
     */
    public static Transaction purchase(UUID playerUuid, int amount, String itemName,
                                      int balanceBefore, int balanceAfter) {
        return new Builder()
                .playerUuid(playerUuid)
                .type(TransactionType.PURCHASE)
                .amount(amount)
                .description("Compra: " + itemName)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }
    
    /**
     * Creates a quest reward transaction
     */
    public static Transaction questReward(UUID playerUuid, int amount, String questName,
                                         int balanceBefore, int balanceAfter) {
        return new Builder()
                .playerUuid(playerUuid)
                .type(TransactionType.QUEST_REWARD)
                .amount(amount)
                .description("Quest: " + questName)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }
}
