package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class RangedFx {
    private RangedFx() {}

    public static void onProjectileTrail(MinecraftSurvivors plugin, Player source, Location loc) {
        if (plugin == null || loc == null) return;
        try { org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRangedTrail(loc.getWorld(), loc, true); } catch (Throwable ignored) {}
    }

    public static void onProjectileHit(MinecraftSurvivors plugin, Player source, LivingEntity target) {
        if (plugin == null || source == null || target == null) return;
        try { org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRangedTrail(target.getWorld(), target.getLocation().add(0,1.0,0), true); } catch (Throwable ignored) {}
    }
}
