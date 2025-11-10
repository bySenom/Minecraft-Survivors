package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class LightningFx {
    private LightningFx() {}

    public static void onPrimaryHit(MinecraftSurvivors plugin, Player source, LivingEntity target) {
        if (plugin == null || source == null || target == null) return;
        try {
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(target.getWorld(), Particle.ELECTRIC_SPARK, target.getLocation().add(0,1.0,0), 12, 0.3,0.3,0.3, 0.02);
            source.playSound(source.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 1.8f);
        } catch (Throwable ignored) {}
    }

    public static void genkidamaVisual(MinecraftSurvivors plugin, Player source, Location center, double damage) {
        if (plugin == null || center == null) return;
        try {
            // small preeffects
            for (int i=0;i<8;i++) {
                Location l = center.clone().add((Math.random()-0.5)*3, 6 + i*0.6, (Math.random()-0.5)*3);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.END_ROD, l, 6, 0.08,0.08,0.08, 0.0);
            }
            // big cloud + sonic
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(center.getWorld(), center, Particle.EXPLOSION, 1, 0.6);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(center.getWorld(), Particle.CLOUD, center, 36, 2.0, 1.0, 2.0, 0.01);
            try { source.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
