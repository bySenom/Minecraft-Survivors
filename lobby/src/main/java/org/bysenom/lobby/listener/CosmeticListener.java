package org.bysenom.lobby.listener;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bysenom.lobby.LobbySystem;

/**
 * Einfache Echtzeit-Cosmetics: Trails beim Bewegen, Emote bei Sneak-Toggle
 */
public class CosmeticListener implements Listener {

    private boolean enabled(Player p, String key) {
        try { return LobbySystem.get().getCosmeticManager().isUnlocked(p.getUniqueId(), key); } catch (Throwable t) { return false; }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().toVector().distanceSquared(e.getTo().toVector()) < 0.0001) return;
        Player p = e.getPlayer();
        if (enabled(p, "trail_sparkles")) {
            // Particle Ersatz END_ROD für leichten Funkel-Effekt
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().clone().add(0, 0.05, 0), 2, 0.15, 0.01, 0.15, 0.0);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (e.isSneaking() && enabled(p, "emote_wave")) {
            try { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.7f, 1.1f); } catch (Throwable ignored) {}
            p.spawnParticle(Particle.HEART, p.getLocation().clone().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.0);
        }
    }
}
