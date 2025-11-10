package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class ShockwaveFx {
    private ShockwaveFx() {}

    public static void onHit(MinecraftSurvivors plugin, Player source, Location center) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0,1.0,0), 1.2, 32, Particle.SWEEP_ATTACK);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.CLOUD, 10, 0.6);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.SMOKE, c.clone().add(0,0.1,0), 6, 0.25,0.1,0.25, 0.01);
            try { c.getWorld().playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
