package org.bysenom.minecraftSurvivors.ability;

import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

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
        if (sp != null) { double size = sp.getEffectiveSizeMult(); minRange *= (1.0 + size*0.5); maxRange *= (1.0 + size); }
        double baseDamage = plugin.getConfigUtil().getDouble("ranger.base-damage", 7.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        double damage = baseDamage + (sp != null ? sp.getDamageAddTotal() : 0.0);
        damage *= Math.max(1.0, 1.0 + 0.12 * (level - 1));
        if (sp != null) {
            damage *= (1.0 + sp.getDamageMult());
        }

        // wind-up sparkles
        try {
            if (sp == null || sp.isFxEnabled()) {
                for (int i = 0; i < 6; i++) {
                    double ang = 2 * Math.PI * i / 6.0;
                    double r = 0.5;
                    double x = loc.getX() + Math.cos(ang) * r;
                    double z = loc.getZ() + Math.sin(ang) * r;
                    loc.getWorld().spawnParticle(Particle.CRIT, new Location(loc.getWorld(), x, loc.getY() + 1.5, z), 1, 0.01, 0.01, 0.01, 0.0);
                }
                loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 0.35f, 1.7f);
            }
        } catch (Throwable ignored) {}

        List<LivingEntity> mobs = spawnManager.getTargetsIncludingBoss(loc, maxRange);
        if (mobs.isEmpty()) return;
        // Boss bevorzugt: falls Boss in Liste -> nach vorne setzen
        mobs.sort(Comparator.comparingDouble((LivingEntity m) -> m.getLocation().distanceSquared(loc)).reversed());
        LivingEntity bossFirst = null;
        for (LivingEntity m : mobs) { if (m.getType() != null && m.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(MinecraftSurvivors.getInstance(), "ms_boss_tag"), org.bukkit.persistence.PersistentDataType.BYTE)) { bossFirst = m; break; } }
        if (bossFirst != null) { mobs.remove(bossFirst); mobs.add(0, bossFirst); }

        int multi = 1 + Math.max(0, (sp != null ? sp.getBonusStrikes() : 0)); // mehrere Pfeile
        int pierce = Math.max(0, (sp != null ? sp.getRangerPierce() : 0));    // wie viele Ziele wird ein Pfeil durchdringen
        // AttackSpeed scaling: ohne Cap; Piercing wächst mit AS leicht mit
        double as = sp != null ? Math.max(0.0, sp.getAttackSpeedMult()) : 0.0;
        double factor = 1.0 + as;
        multi = Math.max(1, (int) Math.floor(multi * factor));
        pierce = Math.max(0, (int) Math.floor(pierce * (1.0 + as * 0.5)));

        int fired = 0;
        for (LivingEntity target : mobs) {
            double d2 = target.getLocation().distanceSquared(loc);
            if (d2 < minRange * minRange) continue; // zu nah
            // Für jeden „Pfeil“ leicht andere Richtung (Fächer)
            for (int j = 0; j < multi; j++) {
                double spread = (j - (multi - 1) / 2.0) * 0.06; // sanfter Fächer
                shootPiercing(loc, target, damage, pierce, spread, player, sp);
                fired++;
                if (fired >= multi) break;
            }
            break; // eine Ausgangs-Ziellinie pro Tick
        }
    }

    private void shootPiercing(Location fromBase, LivingEntity first, double damage, int pierce, double spreadYaw, Player src, SurvivorPlayer sp) {
        Location from = fromBase.clone().add(0, 1.5, 0);
        Location to = first.getLocation().clone().add(0, 1.0, 0);
        Vector dir = to.toVector().subtract(from.toVector()).normalize();
        // yaw spread
        org.bukkit.util.Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(spreadYaw);
        dir.add(side).normalize();
        // Schadenslinie: suche Ziele entlang der Linie und treffe bis pierce+1
        List<LivingEntity> all = spawnManager.getNearbyWaveMobs(from, from.distance(to) + 2.0);
        all.sort(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(from)));
        int hit = 0;
        for (LivingEntity e : all) {
            if (hit > pierce) break;
            // Kollisions-Test: Abstand zur Linie klein?
            Vector pe = e.getLocation().add(0, 1.0, 0).toVector().subtract(from.toVector());
            double proj = pe.dot(dir);
            if (proj < 0) continue; // hinter dem Start
            Vector closest = dir.clone().multiply(proj);
            double dist = pe.clone().subtract(closest).length();
            if (dist <= 1.0) { // Trefferbreite ~1 Block
                drawLine(from, from.clone().add(closest), Particle.CRIT, 14);
                try { e.damage(damage, src); } catch (Throwable ignored) {}
                // knockback
                try { e.setVelocity(e.getVelocity().add(dir.clone().multiply(0.2))); } catch (Throwable ignored) {}
                // sonic ring at hit
                try {
                    if (sp == null || sp.isFxEnabled()) {
                        org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(from.getWorld(), e.getLocation().add(0,0.2,0), 0.6, 18, org.bukkit.Particle.CRIT);
                    }
                } catch (Throwable ignored) {}
                hit++;
            }
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
