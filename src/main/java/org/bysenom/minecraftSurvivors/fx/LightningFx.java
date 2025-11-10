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
            Location c = target.getLocation().clone().add(0, 1.0, 0);
            // strong center burst
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.EXPLOSION, 1, 0.6);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.ELECTRIC_SPARK, c, 18, 0.35, 0.35, 0.35, 0.02);
            // small helix around the target
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnHelixThrottled(c.getWorld(), c.clone().add(0, -0.2, 0), 0.9, 1.2, 36, Particle.END_ROD, 2);
            // quick crack lines to nearby points
            for (int i = 0; i < 6; i++) {
                double ang = 2 * Math.PI * i / 6.0;
                double x = c.getX() + Math.cos(ang) * (0.8 + Math.random() * 0.6);
                double z = c.getZ() + Math.sin(ang) * (0.8 + Math.random() * 0.6);
                Location p = new Location(c.getWorld(), x, c.getY() + 0.2, z);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnLine(c.getWorld(), c, p, 10, Particle.ELECTRIC_SPARK);
            }
            // layered sounds for punchiness
            try { source.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f); } catch (Throwable ignored) {}
            try { source.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void genkidamaVisual(MinecraftSurvivors plugin, Player source, Location center, double damage) {
        if (plugin == null || center == null) return;
        try {
            Location c = center.clone();
            // meteor pre-trail: several glowing rods high above
            for (int i = 0; i < 10; i++) {
                Location top = c.clone().add((Math.random() - 0.5) * 4.0, 10 + i * 0.5, (Math.random() - 0.5) * 4.0);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(top.getWorld(), Particle.END_ROD, top, 6, 0.08, 0.08, 0.08, 0.0);
            }
            // fall-in meteors (visual only) - spawn small impact flares at center offsets
            int count = Math.max(4, plugin.getConfigUtil().getInt("skills.genkidama.meteor-count", 6));
            for (int m = 0; m < count; m++) {
                double ox = (Math.random() - 0.5) * 5.0;
                double oz = (Math.random() - 0.5) * 5.0;
                Location impact = c.clone().add(ox, 0, oz);
                // small comet line
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnLine(c.getWorld(), impact.clone().add(0, 8 + m * 0.6, 0), impact.clone().add(0, 0.2, 0), 12, Particle.FLAME);
                // delayed mini impact
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(impact.getWorld(), impact, Particle.EXPLOSION, 1, 0.6);
                        org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(impact.getWorld(), Particle.CLOUD, impact, 28, 1.8, 0.8, 1.8, 0.01);
                        org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(impact.getWorld(), Particle.LAVA, impact, 12, 0.6, 0.4, 0.6, 0.02);
                        try { impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.1f, 0.9f); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                }, 6L + m * 4L);
            }
            // final central supernova
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.EXPLOSION, 1, 1.0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnHelixThrottled(c.getWorld(), c.clone().add(0, -0.1, 0), 1.4, 1.8, 48, Particle.END_ROD, 3);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0, 0.2, 0), Math.max(2.0, 1.2), 48, Particle.CRIT);
            try { c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.85f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
