package org.bysenom.minecraftSurvivors.fx;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class RangedFx {
    private RangedFx() {}

    public static void onProjectileLaunch(MinecraftSurvivors plugin, Player source, Location origin) {
        if (plugin == null || source == null || origin == null) return;
        try {
            // Sonic wall: quick expanding ring and low-frequency sound at origin
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(origin.getWorld(), origin.clone().add(0, -0.2, 0), 0.8, 28, Particle.CRIT);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(origin.getWorld(), origin.clone().add(0, 0.1, 0), Particle.SONIC_BOOM, 1, 0.45);
            // a faint cloud + rods to sell the shock
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(origin.getWorld(), Particle.CLOUD, origin.clone().add(0,0.05,0), 6, 0.4,0.12,0.4, 0.01);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(origin.getWorld(), Particle.END_ROD, origin.clone().add(0,0.05,0), 6, 0.06,0.06,0.06, 0.0);
            try { origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.2f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void onProjectileTrail(MinecraftSurvivors plugin, Player source, Location loc) {
        if (plugin == null || loc == null) return;
        try {
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafe(loc.getWorld(), Particle.CRIT, loc, 1, 0.01,0.01,0.01, 0.0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafe(loc.getWorld(), Particle.END_ROD, loc.clone().add(0, -0.05, 0), 1, 0.02,0.02,0.02, 0.0);
        } catch (Throwable ignored) {}
    }

    public static void onProjectileHit(MinecraftSurvivors plugin, Player source, LivingEntity target) {
        if (plugin == null || source == null || target == null) return;
        try {
            Location c = target.getLocation().clone().add(0, 0.2, 0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c, 0.6, 18, Particle.CRIT);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.SONIC_BOOM, 1, 0.25);
            try { c.getWorld().playSound(c, Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.2f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
