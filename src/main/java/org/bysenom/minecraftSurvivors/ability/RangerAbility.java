package org.bysenom.minecraftSurvivors.ability;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;

public class RangerAbility implements Ability {

    private final MinecraftSurvivors plugin;
    private final SpawnManager spawnManager;

    public RangerAbility(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public void tick(Player player, SurvivorPlayer sp) {
        if (player == null || !player.isOnline() || !player.isValid()) return;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        double minRangeBase = plugin.getConfigUtil().getDouble("ranger.min-range", 8.0);
        double maxRangeBase = plugin.getConfigUtil().getDouble("ranger.max-range", 20.0);
        double radiusMult = sp != null ? sp.getRadiusMult() : 0.0;
        double minRange = minRangeBase * (1.0 + radiusMult * 0.5); // min leicht mitwachsen
        double maxRange = maxRangeBase * (1.0 + radiusMult);
        double baseDamage = plugin.getConfigUtil().getDouble("ranger.base-damage", 7.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        double damage = baseDamage + (sp != null ? sp.getFlatDamage() : 0.0) + (sp != null ? sp.getBonusDamage() : 0.0);
        damage *= Math.max(1.0, 1.0 + 0.12 * (level - 1));
        if (sp != null) {
            damage *= (1.0 + sp.getDamageMult());
        }

        // wind-up sparkles
        try {
            for (int i = 0; i < 6; i++) {
                double ang = 2 * Math.PI * i / 6.0;
                double r = 0.5;
                double x = loc.getX() + Math.cos(ang) * r;
                double z = loc.getZ() + Math.sin(ang) * r;
                loc.getWorld().spawnParticle(Particle.CRIT, new Location(loc.getWorld(), x, loc.getY() + 1.5, z), 1, 0.01, 0.01, 0.01, 0.0);
            }
            loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 0.35f, 1.7f);
        } catch (Throwable ignored) {}

        List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(loc, maxRange);
        if (mobs.isEmpty()) return;
        // bevorzugt weit entfernte Ziele
        mobs.sort(Comparator.comparingDouble((LivingEntity m) -> m.getLocation().distanceSquared(loc)).reversed());

        for (LivingEntity target : mobs) {
            double d2 = target.getLocation().distanceSquared(loc);
            if (d2 < minRange * minRange) continue; // zu nah
            // Partikel-Linie
            drawLine(loc.clone().add(0, 1.5, 0), target.getLocation().clone().add(0, 1.0, 0), Particle.CRIT, 16);
            try { loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1.1, 0), 1, 0.0, 0.0, 0.0, 0.0); } catch (Throwable ignored) {}
            try { loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.8f); } catch (Throwable ignored) {}
            // Schaden + leichter Rückstoß
            try { target.damage(damage, player); } catch (Throwable ignored) {}
            try {
                double kbMult = 1.0 + (sp != null ? sp.getKnockbackBonus() : 0.0);
                Vector kb = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.35 * kbMult);
                target.setVelocity(target.getVelocity().add(kb));
            } catch (Throwable ignored) {}
            break; // nur ein Schuss pro Tick
        }
    }

    private void drawLine(Location from, Location to, Particle particle, int points) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return;
        Vector dir = to.toVector().subtract(from.toVector());
        double step = 1.0 / Math.max(1, points);
        for (int i = 0; i <= points; i++) {
            Vector off = dir.clone().multiply(step * i);
            Location p = from.clone().add(off);
            try { from.getWorld().spawnParticle(particle, p, 2, 0.02, 0.02, 0.02, 0.0); } catch (Throwable ignored) {}
        }
    }
}
