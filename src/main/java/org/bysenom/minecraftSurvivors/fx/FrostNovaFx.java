package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class FrostNovaFx {
    private FrostNovaFx() {}

    public static void onExplode(MinecraftSurvivors plugin, Player source, Location center, double radius) {
        if (plugin == null || source == null || center == null) return;
        try {
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(center.getWorld(), center.clone().add(0, 0.25, 0), Math.max(1.0, radius), 36, Particle.SNOWFLAKE);
            try { source.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.6f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
