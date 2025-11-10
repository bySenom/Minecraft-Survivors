package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class RangedFx {
    private RangedFx() {}

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
            Location c = target.getLocation().clone().add(0, 1.0, 0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c, 0.6, 18, Particle.CRIT);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.SONIC_BOOM, 1, 0.35);
            try { c.getWorld().playSound(c, Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.2f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
