package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.combat.CombatEngine;

public class DamageHealListener implements Listener {

    private final MinecraftSurvivors plugin;

    public DamageHealListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        CombatEngine.handleDamageEvent(plugin, e);
    }
}
