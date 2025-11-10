package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class FireFx {
    private FireFx() {}

    public static void onIgnite(MinecraftSurvivors plugin, Player source, LivingEntity target) {
        if (plugin == null || source == null || target == null) return;
        try {
            Location c = target.getLocation().clone().add(0, 1.0, 0);
            // ember spray
            for (int i = 0; i < 8; i++) {
                double ang = 2 * Math.PI * i / 8.0;
                double r = 0.4 + Math.random() * 0.6;
                Location p = c.clone().add(Math.cos(ang) * r, 0.6 + Math.random()*0.3, Math.sin(ang) * r);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.LARGE_SMOKE, p, 2, 0.05, 0.05, 0.05, 0.0);
            }
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnHelixThrottled(c.getWorld(), c.clone().add(0, -0.2, 0), 0.6, 1.0, 20, Particle.FLAME, 2);
            try { source.playSound(c, Sound.ENTITY_BLAZE_HURT, 0.6f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void infernoPulse(MinecraftSurvivors plugin, Player source, Location center) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            // expanding ember rings
            for (int r = 1; r <= 3; r++) {
                double rad = r * 0.7;
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0, 0.2, 0), rad, 28 + r*6, Particle.FLAME);
            }
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.LAVA, 6, 0.6);
            try { c.getWorld().playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.9f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
