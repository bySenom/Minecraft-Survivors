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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.PORTAL, center.clone().add(0,0.2,0), 28, Math.max(1.0, radius*0.6), 0.4, Math.max(1.0, radius*0.6), 0.02);
            try { if (source != null) source.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.6f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void spawnLingeringField(MinecraftSurvivors plugin, Location center, double radius) {
        if (plugin == null || center == null) return;
        try { org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(center.getWorld(), center.clone().add(0,0.1,0), radius, 16, Particle.REVERSE_PORTAL); } catch (Throwable ignored) {}
    }
}
