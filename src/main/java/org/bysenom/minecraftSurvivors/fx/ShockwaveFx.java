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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.SWEEP_ATTACK, center.clone().add(0,1.0,0), 18, 1.0, 0.2, 1.0, 0.0);
            try { if (source != null) source.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
