package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageHealListener implements Listener {

    private final MinecraftSurvivors plugin;

    public DamageHealListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            if (e.getDamager() instanceof org.bukkit.projectiles.ProjectileSource) {
                // Could resolve shooter, but keep it simple for now
            }
            return;
        }
        Player p = (Player) e.getDamager();
        if (!(e.getEntity() instanceof LivingEntity)) return;
        double amount = e.getFinalDamage();
        if (amount <= 0) return;
        try { plugin.getStatsMeterManager().recordDamage(p.getUniqueId(), amount); } catch (Throwable ignored) {}
    }

    // For HPS, PaladinAbility will call plugin.getStatsMeterManager().recordHeal(...) directly
}

