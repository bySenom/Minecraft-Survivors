package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class DamageHealListener implements Listener {

    private final MinecraftSurvivors plugin;

    public DamageHealListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        // Block input damage to paused players
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            try {
                if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(victim.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        Player damagerPlayer = null;
        if (e.getDamager() instanceof Player) {
            damagerPlayer = (Player) e.getDamager();
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) e.getDamager();
            Object shooter = proj.getShooter();
            if (shooter instanceof Player) damagerPlayer = (Player) shooter;
        }
        // Optional: block outgoing damage from paused players (keine Aktionen während Auswahl)
        if (damagerPlayer != null) {
            try {
                if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(damagerPlayer.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        if (damagerPlayer == null) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        double amount = e.getFinalDamage();
        if (amount <= 0) return;
        try { plugin.getStatsMeterManager().recordDamage(damagerPlayer.getUniqueId(), amount); } catch (Throwable ignored) {}
    }

    // For HPS, PaladinAbility will call plugin.getStatsMeterManager().recordHeal(...) directly
}
