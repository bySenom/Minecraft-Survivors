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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(center.getWorld(), center.clone().add(0,0.2,0), radius, 24, Particle.PORTAL);
            try { if (source != null) source.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.3f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
