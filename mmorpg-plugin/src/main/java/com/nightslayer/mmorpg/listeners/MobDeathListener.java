package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDeathListener implements Listener {
    public MobDeathListener(MMORPGPlugin plugin) {
    }
    
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            // TODO: Give experience and coins
            // TODO: Update bestiary
        }
    }
}
