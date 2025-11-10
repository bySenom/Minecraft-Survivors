package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class HealTotemFx {
    private HealTotemFx() {}

    public static void onHealPulse(MinecraftSurvivors plugin, Player source, Player target) {
        if (plugin == null || source == null || target == null) return;
        try {
            Location c = target.getLocation().clone().add(0, 1.0, 0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRingThrottled(c.getWorld(), c, 0.6, 18, Particle.HEART);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.HAPPY_VILLAGER, c, 6, 0.2, 0.2, 0.2, 0.01);
            try { c.getWorld().playSound(c, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void onAegisProc(MinecraftSurvivors plugin, Player source, Player target) {
        if (plugin == null || source == null || target == null) return;
        try {
            Location c = target.getLocation().clone().add(0, 1.0, 0);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurstThrottled(c.getWorld(), c, Particle.END_ROD, 8, 0.4);
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafeThrottled(c.getWorld(), Particle.END_ROD, c, 6, 0.3,0.3,0.3, 0.02);
            try { c.getWorld().playSound(c, Sound.ITEM_SHIELD_BLOCK, 0.7f, 1.0f); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
