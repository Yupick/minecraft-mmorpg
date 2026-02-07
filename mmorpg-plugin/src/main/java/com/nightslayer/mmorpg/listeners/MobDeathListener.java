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
