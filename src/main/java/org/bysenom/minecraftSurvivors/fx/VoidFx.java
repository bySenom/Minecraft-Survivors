package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class VoidFx {
    private VoidFx() {}

    public static void onPulse(MinecraftSurvivors plugin, Player source, Location center, double radius) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            // layered portal rings
            for (int i=0;i<3;i++) {
                double r = Math.max(1.0, radius * 0.3) + i * 0.6;
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0, 0.15 + i*0.05, 0), r, 24 + i*8, Particle.PORTAL);
            }
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.REVERSE_PORTAL, 6, 0.4);
            try { c.getWorld().playSound(c, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.6f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void spawnLingeringField(MinecraftSurvivors plugin, Location center, double radius) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0,0.1,0), radius, 20, Particle.REVERSE_PORTAL);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c.clone().add(0,0.2,0), Particle.PORTAL, 6, 0.35);
        } catch (Throwable ignored) {}
    }
}
