package com.nightslayer.mmorpg.squads;

import java.util.*;

/**
 * Represents a player squad/clan
 */
public class Squad {
    
    private final int id;
    private final String name;
    private final String tag;
    private UUID leaderUuid;
    private final List<SquadMember> members;
    private int sharedBank;
    private final long createdAt;
    private String description;
    private int maxMembers;
    
    public Squad(int id, String name, String tag, UUID leaderUuid) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leaderUuid = leaderUuid;
        this.members = new ArrayList<>();
        this.sharedBank = 0;
        this.createdAt = System.currentTimeMillis();
        this.description = "";
        this.maxMembers = 10;
        
        // Add leader as first member
        members.add(new SquadMember(leaderUuid, SquadMember.SquadRank.LEADER));
    }
    
    /**
     * Adds a member to the squad
     */
    public boolean addMember(UUID playerUuid, SquadMember.SquadRank rank) {
        if (members.size() >= maxMembers) {
            return false;
        }
        
        if (getMember(playerUuid) != null) {
            return false; // Already a member
        }
        
        members.add(new SquadMember(playerUuid, rank));
        return true;
    }
    
    /**
     * Removes a member from the squad
     */
    public boolean removeMember(UUID playerUuid) {
        if (playerUuid.equals(leaderUuid)) {
            return false; // Cannot remove leader
        }
        
        return members.removeIf(m -> m.getPlayerUuid().equals(playerUuid));
    }
    
    /**
     * Gets a member by UUID
     */
    public SquadMember getMember(UUID playerUuid) {
        return members.stream()
                .filter(m -> m.getPlayerUuid().equals(playerUuid))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Checks if a player is a member
     */
    public boolean isMember(UUID playerUuid) {
        return getMember(playerUuid) != null;
    }
    
    /**
     * Promotes a member
     */
    public boolean promoteMember(UUID playerUuid) {
        SquadMember member = getMember(playerUuid);
        if (member == null) return false;
        
        SquadMember.SquadRank currentRank = member.getRank();
        if (currentRank == SquadMember.SquadRank.MEMBER) {
            member.setRank(SquadMember.SquadRank.OFFICER);
            return true;
        }
        
        return false;
    }
    
    /**
     * Demotes a member
     */
    public boolean demoteMember(UUID playerUuid) {
        SquadMember member = getMember(playerUuid);
        if (member == null) return false;
        
        SquadMember.SquadRank currentRank = member.getRank();
        if (currentRank == SquadMember.SquadRank.OFFICER) {
            member.setRank(SquadMember.SquadRank.MEMBER);
            return true;
        }
        
        return false;
    }
    
    /**
     * Transfers leadership
     */
    public boolean transferLeadership(UUID newLeaderUuid) {
        SquadMember newLeader = getMember(newLeaderUuid);
        if (newLeader == null) return false;
        
        // Demote old leader to officer
        SquadMember oldLeader = getMember(leaderUuid);
        if (oldLeader != null) {
            oldLeader.setRank(SquadMember.SquadRank.OFFICER);
        }
        
        // Promote new leader
        newLeader.setRank(SquadMember.SquadRank.LEADER);
        this.leaderUuid = newLeaderUuid;
        
        return true;
    }
    
    /**
     * Deposits to shared bank
     */
    public void depositToBank(int amount) {
        this.sharedBank += amount;
    }
    
    /**
     * Withdraws from shared bank
     */
    public boolean withdrawFromBank(int amount) {
        if (sharedBank < amount) {
            return false;
        }
        
        sharedBank -= amount;
        return true;
    }
    
    /**
     * Gets all online members
     */
    public List<UUID> getOnlineMembers(org.bukkit.Server server) {
        List<UUID> online = new ArrayList<>();
        for (SquadMember member : members) {
            if (server.getPlayer(member.getPlayerUuid()) != null) {
                online.add(member.getPlayerUuid());
            }
        }
        return online;
    }
    
    /**
     * Gets member count
     */
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * Gets formatted squad tag
     */
    public String getFormattedTag() {
        return "[" + tag + "]";
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public UUID getLeaderUuid() {
        return leaderUuid;
    }
    
    public List<SquadMember> getMembers() {
        return new ArrayList<>(members);
    }
    
    public int getSharedBank() {
        return sharedBank;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
}
