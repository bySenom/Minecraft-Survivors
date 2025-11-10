package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class TimeRiftFx {
    private TimeRiftFx() {}

    public static void onActivate(MinecraftSurvivors plugin, Player source, Location center, double radius) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            // rotating rings
            for (int i=0;i<3;i++) {
                double r = radius * (0.5 + i*0.25);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0, 0.2 + i*0.05, 0), r, 28 + i*6, Particle.PORTAL);
            }
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSpiralThrottled(c.getWorld(), c.clone().add(0, -0.2, 0), 0.5, radius*0.8, 30, Particle.END_ROD, 2);
            try { c.getWorld().playSound(c, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.3f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
