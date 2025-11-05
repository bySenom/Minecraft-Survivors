package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Creature;

public class SpawnManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final Random random = new Random();
    private final NamespacedKey waveKey;
    private final Map<UUID, Set<UUID>> frozenByPlayer = new ConcurrentHashMap<>();

    public SpawnManager(MinecraftSurvivors plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.waveKey = new NamespacedKey(plugin, "ms_wave");
    }

    @SuppressWarnings("deprecation")
    public void spawnWave(int waveNumber) {
        int base = plugin.getConfigUtil().getInt("wave.spawn-per-player-base", 1);
        double scale = plugin.getConfigUtil().getDouble("wave.scale-per-wave", 0.5);
        int perPlayer = Math.max(1, base + (int) Math.floor(waveNumber * scale)); // skalierung
        boolean shouldGlow = plugin.getConfigUtil().getBoolean("spawn.glowing", true);
        int glowDurationTicks = plugin.getConfigUtil().getInt("spawn.glowing-duration-ticks", 20 * 60 * 5); // default 5min
        // read spawn distances from config; default to further distances to avoid spawns too close
        double minDist = plugin.getConfigUtil().getDouble("spawn.min-distance", 8.0);
        double maxDist = plugin.getConfigUtil().getDouble("spawn.max-distance", 16.0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < perPlayer; i++) {
                Location spawnLoc = randomNearby(player.getLocation(), minDist, maxDist);
                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                // Setze glowing (vanilla) und modernen Component-Namen
                mob.setGlowing(shouldGlow);
                mob.setCustomName("WaveMob");
                mob.setCustomNameVisible(false);
                // Spielt eine kurze Partikel-Spawn-Animation an der Spawn-Location
                playSpawnAnimation(spawnLoc);
                // Markiere mit PersistentDataContainer statt veralteter Metadata-API
                mob.getPersistentDataContainer().set(waveKey, PersistentDataType.BYTE, (byte) 1);
                // Zusätzlicher PotionEffect damit Spieler eindeutig die Wave-Mobs sehen (falls gewünscht)
                if (shouldGlow) {
                    try {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Math.max(20, glowDurationTicks), 0, true, false, false));
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public void clearWaveMobs() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) entity;
                if (!le.getPersistentDataContainer().has(waveKey, PersistentDataType.BYTE)) continue;
                le.remove();
            }
        }
    }

    /**
     * Liefert alle als Wave-Mobs markierten LivingEntities in einem Radius um 'center'.
     */
    public List<LivingEntity> getNearbyWaveMobs(Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        double radiusSq = radius * radius;
        World world = center.getWorld();
        if (world == null) return out;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) entity;
            if (!le.getPersistentDataContainer().has(waveKey, PersistentDataType.BYTE)) continue;
            if (le.getLocation().getWorld() != center.getWorld()) continue;
            if (le.getLocation().distanceSquared(center) > radiusSq) continue;
            out.add(le);
        }
        return out;
    }

    /**
     * Visuellen Blitz-Effekt auslösen und gezielt Schaden an einem Ziel anwenden.
     * Wichtig: verwendet kein World#strikeLightning (das Schaden an allen Entities in der Nähe verursacht),
     * sondern spielt nur den Effekt und wendet dann gezielt Damage auf das angegebene Ziel an.
     */
    public void strikeLightningAtTarget(LivingEntity target, double damage, Player source) {
        if (target == null) return;
        Location loc = target.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        // Play visual effect on main thread and then apply damage to the targeted entity only
        Runnable r = () -> {
            try {
                loc.getWorld().strikeLightningEffect(loc); // nur visueller Blitz
            } catch (Throwable ignored) {}
            try {
                // Apply damage directly to target; use source as damager so kills are attributed
                target.damage(damage, source);
            } catch (Throwable ex) {
                plugin.getLogger().warning("strikeLightningAtTarget: failed to damage target: " + ex.getMessage());
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    /**
     * Kurze Partikel-Animation beim Spawnen eines Mobs.
     * Läuft für wenige Ticks und erzeugt einen aufsteigenden Ring aus Partikeln.
     */
    private void playSpawnAnimation(Location center) {
        if (center == null || center.getWorld() == null) return;
        final org.bukkit.World world = center.getWorld();
        // Lese Partikel-Einstellungen aus der Config
        String particleName = plugin.getConfigUtil().getString("spawn.particle", "END_ROD").toUpperCase();
        org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName);
        final int maxTicks = Math.max(1, plugin.getConfigUtil().getInt("spawn.particle-duration", 10));
        final int points = Math.max(4, plugin.getConfigUtil().getInt("spawn.particle-points", 16));
        final int count = Math.max(0, plugin.getConfigUtil().getInt("spawn.particle-count", 2));
        final double spread = Math.max(0.0, plugin.getConfigUtil().getDouble("spawn.particle-spread", 0.1));

        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > maxTicks) {
                    cancel();
                    return;
                }

                double radius = 0.4 + tick * 0.12;
                double height = 0.3 + tick * 0.08;

                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    Location p = new Location(world, x, center.getY() + height, z);
                    try {
                        if (particle.name().equals("REDSTONE")) {
                            int r = plugin.getConfigUtil().getInt("spawn.redstone-r", 255);
                            int g = plugin.getConfigUtil().getInt("spawn.redstone-g", 255);
                            int b = plugin.getConfigUtil().getInt("spawn.redstone-b", 255);
                            org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(r, g, b), 1.0f);
                            // spawn with DustOptions as data; use runtime particle variable to avoid compile-time enum reference
                            world.spawnParticle(particle, p, Math.max(1, count), 0.0, 0.0, 0.0, dust);
                        } else {
                            world.spawnParticle(particle, p, count, spread, spread, spread, 0.0);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Location randomNearby(Location base, double minRadius, double maxRadius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double dx = Math.cos(angle) * distance;
        double dz = Math.sin(angle) * distance;
        Location loc = base.clone().add(dx, 0, dz);
        loc.setY(base.getWorld().getHighestBlockYAt(loc) + 1);
        return loc;
    }

    /**
     * Freeze nearby wave mobs for a specific player by applying strong Slowness and
     * optionally disabling AI when supported. The freeze is tracked so it can be undone.
     */
    public void freezeMobsForPlayer(UUID playerUuid, Location center, double radius) {
        if (playerUuid == null || center == null) return;
        boolean onlyTargeting = plugin.getConfigUtil().getBoolean("spawn.freeze-only-targeting", true);
        List<LivingEntity> mobs = getNearbyWaveMobs(center, radius);
        if (mobs.isEmpty()) return;
        Set<UUID> set = frozenByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
        for (LivingEntity le : mobs) {
            try {
                boolean shouldFreeze = false;
                if (onlyTargeting) {
                    if (le instanceof Creature) {
                        Creature c = (Creature) le;
                        org.bukkit.entity.LivingEntity target = c.getTarget();
                        if (target != null && target.getUniqueId().equals(playerUuid)) {
                            shouldFreeze = true;
                        }
                    }
                } else {
                    shouldFreeze = true;
                }
                if (!shouldFreeze) continue;
                // mark as frozen for this player
                set.add(le.getUniqueId());
                // strong slowness to effectively freeze
                org.bukkit.potion.PotionEffectType slowType = org.bukkit.potion.PotionEffectType.getByName("SLOW");
                if (slowType == null) slowType = org.bukkit.potion.PotionEffectType.getByName("SLOWNESS");
                if (slowType != null) {
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(slowType, 20 * 60 * 60, 10, true, false, false));
                }
                // try to disable AI if available (Paper/Spigot 1.14+)
                try {
                    le.getClass().getMethod("setAI", boolean.class).invoke(le, false);
                } catch (NoSuchMethodException nsme) {
                    // ignore if setAI not available
                }
            } catch (Throwable ignored) {}
        }
    }

    public void unfreezeMobsForPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        Set<UUID> set = frozenByPlayer.remove(playerUuid);
        if (set == null || set.isEmpty()) return;
        for (UUID eu : set) {
            try {
                Entity ent = Bukkit.getEntity(eu);
                if (ent instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) ent;
                    // remove slowness
                    org.bukkit.potion.PotionEffectType slowType = org.bukkit.potion.PotionEffectType.getByName("SLOW");
                    if (slowType == null) slowType = org.bukkit.potion.PotionEffectType.getByName("SLOWNESS");
                    if (slowType != null) le.removePotionEffect(slowType);
                    // try to re-enable AI
                    try {
                        le.getClass().getMethod("setAI", boolean.class).invoke(le, true);
                    } catch (NoSuchMethodException nsme) {
                        // ignore
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}
