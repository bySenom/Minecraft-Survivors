package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class VenomSpireFx {
    private VenomSpireFx() {}

    public static void onSpawn(MinecraftSurvivors plugin, Player source, Location center) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.ITEM_SLIME, 8, 0.4);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0,0.2,0), 1.2, 18, Particle.DAMAGE_INDICATOR);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.WITCH, c, 10, 0.6,0.3,0.6, 0.01);
            try { c.getWorld().playSound(c, Sound.BLOCK_BASALT_BREAK, 0.7f, 0.8f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
