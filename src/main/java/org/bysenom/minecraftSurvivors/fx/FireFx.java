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
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(target.getWorld(), Particle.FLAME, target.getLocation().add(0,1.0,0), 8, 0.25,0.25,0.25, 0.01);
            source.playSound(source.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.35f, 1.5f);
        } catch (Throwable ignored) {}
    }

    public static void infernoPulse(MinecraftSurvivors plugin, Player source, Location center) {
        if (plugin == null || center == null) return;
        try {
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(center.getWorld(), center, Particle.LAVA, 8, 0.6);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.CAMPFIRE_COSY_SMOKE, center, 8, 1.0, 0.6, 1.0, 0.01);
            try { source.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
