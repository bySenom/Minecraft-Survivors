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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(center.getWorld(), center, Particle.PORTAL, 6, 0.3);
            try { if (source != null) source.playSound(center, Sound.BLOCK_BASALT_BREAK, 0.6f, 0.8f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
