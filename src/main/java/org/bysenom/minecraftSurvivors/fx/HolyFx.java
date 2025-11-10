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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.END_ROD, center.clone().add(0,1.0,0), 24, radius/2, 0.3, radius/2, 0.0);
            try { source.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.8f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
