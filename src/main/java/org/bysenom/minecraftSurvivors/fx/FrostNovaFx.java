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
            Location c = center.clone();
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c.clone().add(0, 0.2, 0), Math.max(1.0, radius * 0.6), 40, Particle.SNOWFLAKE);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.ITEM_SNOWBALL, 10, 0.6);
            // multiple small shards outward
            for (int i=0;i<12;i++) {
                double ang = 2*Math.PI*i/12.0;
                double x = c.getX()+Math.cos(ang)*(0.6+Math.random()*1.2);
                double z = c.getZ()+Math.sin(ang)*(0.6+Math.random()*1.2);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafe(c.getWorld(), Particle.CRIT, new Location(c.getWorld(), x, c.getY()+0.4+Math.random()*0.4, z), 2, 0.02,0.02,0.02,0.0);
            }
            try { c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.6f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
