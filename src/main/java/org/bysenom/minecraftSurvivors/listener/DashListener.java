package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dash über Sneak: Wenn der Spieler den Skill "dash" besitzt, kann er alle X Sekunden dashen.
 */
public class DashListener implements Listener {

    private final MinecraftSurvivors plugin;
    private final Map<UUID, Long> lastDashSneak = new HashMap<>();
    private final Map<UUID, Long> lastDashRClick = new HashMap<>();

    public DashListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return; // nur beim Sneak-Beginn
        Player p = e.getPlayer();
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        if (sp == null || !sp.getSkills().contains("dash")) return;
        long now = System.currentTimeMillis();
        long last = lastDashSneak.getOrDefault(p.getUniqueId(), 0L);
        long cd = 2500; // 2.5s
        try { cd = plugin.getConfigUtil().getInt("skills.dash.cooldown-ms", 2500); } catch (Throwable ignored) {}
        if (now - last < cd) return;
        lastDashSneak.put(p.getUniqueId(), now);
        doDash(p);
    }

    @org.bukkit.event.EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        if (sp == null || !sp.getSkills().contains("dash")) return;
        long now = System.currentTimeMillis();
        long last = lastDashRClick.getOrDefault(p.getUniqueId(), 0L);
        long cd = 2500; // 2.5s
        try { cd = plugin.getConfigUtil().getInt("skills.dash.cooldown-rclick-ms", 3000); } catch (Throwable ignored) {}
        if (now - last < cd) return;
        lastDashRClick.put(p.getUniqueId(), now);
        doDash(p);
    }

    private void doDash(Player p) {
        org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
        org.bukkit.util.Vector vel = dir.multiply(1.2).setY(0.15);
        p.setVelocity(vel);
        // Trail Partikel
        try {
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 18, 0.35, 0.10, 0.35, 0.02);
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0,1,0), 6, 0.2, 0.2, 0.2, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.75f);
        } catch (Throwable ignored) {}
    }
}
