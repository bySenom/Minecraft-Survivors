package org.bysenom.minecraftSurvivors.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Zeigt eine ruckelfreie, angeheftete HP-Bar: unsichtbarer ArmorStand als Passenger auf dem Mob.
 * - Ein Overlay pro Mob
 * - Text wird aktualisiert und Timeout wird verlängert
 * - Periodischer Cleanup entfernt abgelaufene/ungültige Overlays
 */
public class HoloHpManager {

    private static final int DEFAULT_BAR_LEN = 10;
    private static final int DEFAULT_TTL_TICKS = 40; // ~2s

    private final MinecraftSurvivors plugin;
    private final Map<UUID, Overlay> byMob = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;
    private BukkitTask visibilityTask;
    private final double viewDistance;

    private static final class Overlay {
        final UUID mobId;
        ArmorStand stand; // kann null sein bis Spawn OK
        int ttl; // ticks bis Auto-Remove
        Overlay(UUID mobId, ArmorStand stand, int ttl) { this.mobId = mobId; this.stand = stand; this.ttl = ttl; }
    }

    public HoloHpManager(MinecraftSurvivors plugin) {
        this.plugin = plugin;
        this.viewDistance = Math.max(8.0, plugin.getConfigUtil().getDouble("holohp.view-distance", 20.0));
        ensureCleanup();
        ensureVisibilityTask();
    }

    public void updateBar(LivingEntity mob, double hp, double max) {
        if (mob == null || !mob.isValid() || mob.getWorld() == null) return;
        String bar = makeBar(Math.max(0.0, hp) / Math.max(1.0, max), DEFAULT_BAR_LEN);
        Component text = Component.text(bar, NamedTextColor.RED)
                .append(Component.text(" " + (int) Math.round(max), NamedTextColor.GRAY));
        Overlay o = byMob.get(mob.getUniqueId());
        if (o == null || o.stand == null || !o.stand.isValid()) {
            ArmorStand as = spawnStand(mob.getLocation());
            if (as == null) return;
            // Name setzen but don't make it globally visible; visibilityTask will show per-player
            try { as.customName(text); as.setCustomNameVisible(false); } catch (Throwable ignored) {}
            // Attach as passenger if possible; do it next tick to ensure spawn flags are applied
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (as == null || !as.isValid()) return;
                    boolean mounted = false;
                    try { mounted = mob.addPassenger(as); } catch (Throwable ignored) {}
                    if (!mounted) startFollowTask(as, mob);
                } catch (Throwable ignored) {}
            });
            o = new Overlay(mob.getUniqueId(), as, DEFAULT_TTL_TICKS);
            byMob.put(mob.getUniqueId(), o);
        } else {
            try { o.stand.customName(text); } catch (Throwable ignored) {}
            o.ttl = DEFAULT_TTL_TICKS;
        }
    }

    private ArmorStand spawnStand(Location base) {
        if (base == null || base.getWorld() == null) return null;
        try {
            Location loc = base.clone().add(0, 0.2, 0); // leichte Anhebung; Passenger-Offset sorgt für Rest
            ArmorStand as = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            try { as.setInvisible(true); } catch (Throwable ignored) {}
            try { as.setMarker(true); } catch (Throwable ignored) {}
            try { as.setSmall(true); } catch (Throwable ignored) {}
            try { as.setGravity(false); } catch (Throwable ignored) {}
            try { as.setBasePlate(false); } catch (Throwable ignored) {}
            try { as.setCollidable(false); } catch (Throwable ignored) {}
            // Hide from all players immediately to prevent any short-lived client-side flash
            try {
                for (org.bukkit.entity.Player pl : org.bukkit.Bukkit.getOnlinePlayers()) {
                    try { pl.hideEntity(plugin, as); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            return as;
        } catch (Throwable t) {
            plugin.getLogger().warning("HoloHpManager: ArmorStand spawn failed: " + t.getMessage());
            return null;
        }
    }

    private void startFollowTask(ArmorStand as, LivingEntity mob) {
        // Minimaler Fallback, falls Passenger nicht klappt; folgt per Teleport
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            try {
                if (as == null || !as.isValid() || mob == null || !mob.isValid()) { task.cancel(); return; }
                as.teleport(mob.getLocation().clone().add(0, 1.8, 0));
            } catch (Throwable ex) {
                task.cancel();
            }
        }, 1L, 1L);
    }

    private void ensureVisibilityTask() {
        if (visibilityTask != null && !visibilityTask.isCancelled()) return;
        int period = Math.max(4, plugin.getConfigUtil().getInt("holohp.visibility-tick-interval", 5));
        visibilityTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                double viewDistSq = viewDistance * viewDistance;
                for (Map.Entry<UUID, Overlay> en : byMob.entrySet()) {
                    Overlay o = en.getValue();
                    if (o == null || o.stand == null) continue;
                    org.bukkit.entity.LivingEntity mob = (org.bukkit.entity.LivingEntity) Bukkit.getEntity(o.mobId);
                    if (mob == null || !mob.isValid()) continue;
                    for (org.bukkit.entity.Player pl : org.bukkit.Bukkit.getOnlinePlayers()) {
                        try {
                            boolean shouldShow = pl.getWorld().equals(mob.getWorld()) && pl.getLocation().distanceSquared(mob.getLocation()) <= viewDistSq;
                            if (shouldShow) {
                                try { pl.showEntity(plugin, o.stand); } catch (Throwable ignored) {}
                                try { if (!o.stand.isCustomNameVisible()) o.stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                            } else {
                                try { pl.hideEntity(plugin, o.stand); } catch (Throwable ignored) {}
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }, 0L, period);
    }

    private void ensureCleanup() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) return;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID mobId : byMob.keySet()) {
                Overlay o = byMob.get(mobId);
                if (o == null) continue;
                boolean remove = false;
                try {
                    if (o.ttl-- <= 0) remove = true;
                    if (o.stand == null || !o.stand.isValid()) remove = true;
                    LivingEntity mob = (LivingEntity) Bukkit.getEntity(mobId);
                    if (mob == null || !mob.isValid()) remove = true;
                } catch (Throwable ignored) { remove = true; }
                if (remove) {
                    try {
                        if (o.stand != null) {
                            // Make sure it's hidden from all players before removal
                            try { for (org.bukkit.entity.Player pl : org.bukkit.Bukkit.getOnlinePlayers()) pl.hideEntity(plugin, o.stand); } catch (Throwable ignored) {}
                            o.stand.remove();
                        }
                     } catch (Throwable ignored) {}
                     byMob.remove(mobId);
                 }
             }
         }, 10L, 10L);
     }

    private String makeBar(double ratio, int len) {
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int fill = (int) Math.round(ratio * len);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) sb.append(i < fill ? '█' : '░');
        sb.append("]");
        return sb.toString();
    }
}
