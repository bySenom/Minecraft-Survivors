package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class HolyFx {
    private HolyFx() {}

    public static void onBurst(MinecraftSurvivors plugin, Player source, Location center, double radius) {
        if (plugin == null || source == null || center == null) return;
        try {
            Location c = center.clone();
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0,0.7,0), Math.max(1.0, radius*0.6), 36, Particle.END_ROD);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0,0.3,0), Math.max(0.8, radius*0.4), 30, Particle.CRIT);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.HEART, 6, 0.6);
            try { c.getWorld().playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.3f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
