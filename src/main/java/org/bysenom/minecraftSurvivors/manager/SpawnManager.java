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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpawnManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final Random random = new Random();
    private final NamespacedKey waveKey;

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
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < perPlayer; i++) {
                Location spawnLoc = randomNearby(player.getLocation(), 3, 6);
                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                // Setze glowing und modernen Component-Namen
                mob.setGlowing(shouldGlow);
                mob.setCustomName("WaveMob");
                mob.setCustomNameVisible(false);
                // Spielt eine kurze Partikel-Spawn-Animation an der Spawn-Location
                playSpawnAnimation(spawnLoc);
                // Markiere mit PersistentDataContainer statt veralteter Metadata-API
                mob.getPersistentDataContainer().set(waveKey, PersistentDataType.BYTE, (byte) 1);
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
     * Sicherer Blitz mit Schaden (World#strikeLightning). Immer auf Haupt-Thread ausführen.
     */
    public void strikeLightningSafe(Location loc) {
        if (loc == null) return;
        World w = loc.getWorld();
        if (w == null) return;

        // Sicherstellen, dass der Aufruf auf dem Haupt-Thread läuft
        if (Bukkit.isPrimaryThread()) {
            w.strikeLightning(loc); // schadet Entities
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> w.strikeLightning(loc));
        }
    }

    /**
     * Sicherer Blitz-Effekt ohne Schaden (World#strikeLightningEffect).
     * Nützlich, wenn nur visuelles Feedback erwünscht ist.
     */
    public void strikeLightningEffectSafe(Location loc) {
        if (loc == null) return;
        World w = loc.getWorld();
        if (w == null) return;

        Runnable effect = () -> {
            w.strikeLightningEffect(loc); // nur Effekt
            // zusätzlich: Sound und Partikel, damit der Effekt sicher sichtbar ist
            try {
                w.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            try {
                w.spawnParticle(org.bukkit.Particle.CRIT, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5);
            } catch (Exception ignored) {}
        };

        if (Bukkit.isPrimaryThread()) {
            effect.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, effect);
        }
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
}
