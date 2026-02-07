package com.nightslayer.mmorpg.squads;

import java.util.UUID;

/**
 * Represents a member of a squad
 */
public class SquadMember {
    
    private final UUID playerUuid;
    private SquadRank rank;
    private final long joinedAt;
    private int contributedCoins;
    
    public SquadMember(UUID playerUuid, SquadRank rank) {
        this.playerUuid = playerUuid;
        this.rank = rank;
        this.joinedAt = System.currentTimeMillis();
        this.contributedCoins = 0;
    }
    
    /**
     * Squad member ranks
     */
    public enum SquadRank {
        LEADER("Líder", 3),
        OFFICER("Oficial", 2),
        MEMBER("Miembro", 1);
        
        private final String displayName;
        private final int permissionLevel;
        
        SquadRank(String displayName, int permissionLevel) {
            this.displayName = displayName;
            this.permissionLevel = permissionLevel;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getPermissionLevel() {
            return permissionLevel;
        }
        
        public boolean canInvite() {
            return permissionLevel >= 2;
        }
        
        public boolean canKick() {
            return permissionLevel >= 2;
        }
        
        public boolean canPromote() {
            return permissionLevel >= 3;
        }
        
        public boolean canWithdrawFromBank() {
            return permissionLevel >= 2;
        }
    }
    
    /**
     * Contributes coins to squad bank
     */
    public void contribute(int amount) {
        this.contributedCoins += amount;
    }
    
    /**
     * Gets days since joined
     */
    public long getDaysSinceJoined() {
        long diff = System.currentTimeMillis() - joinedAt;
        return diff / (1000 * 60 * 60 * 24);
    }
    
    // Getters and setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public SquadRank getRank() {
        return rank;
    }
    
    public void setRank(SquadRank rank) {
        this.rank = rank;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
    
    public int getContributedCoins() {
        return contributedCoins;
    }
    
    @Override
    public String toString() {
        return String.format("SquadMember{uuid=%s, rank=%s, days=%d, contributed=%d}",
                playerUuid, rank, getDaysSinceJoined(), contributedCoins);
    }
}
