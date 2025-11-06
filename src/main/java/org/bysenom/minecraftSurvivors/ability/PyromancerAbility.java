package org.bysenom.minecraftSurvivors.ability;

import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

public class PyromancerAbility implements Ability {

    private final MinecraftSurvivors plugin;
    private final SpawnManager spawnManager;

    public PyromancerAbility(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public void tick(Player player, SurvivorPlayer sp) {
        if (player == null || !player.isOnline() || !player.isValid()) return;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        double baseRadius = plugin.getConfigUtil().getDouble("pyromancer.radius", 8.0);
        double radius = baseRadius * (1.0 + (sp != null ? sp.getRadiusMult() : 0.0));
        int targets = Math.max(1, plugin.getConfigUtil().getInt("pyromancer.targets-per-tick", 2) + (sp != null ? sp.getBonusStrikes() : 0));
        // AttackSpeed scaling: ohne Cap
        double as = sp != null ? Math.max(0.0, sp.getAttackSpeedMult()) : 0.0;
        double speedFactor = 1.0 + as;
        targets = Math.max(1, (int) Math.floor(targets * speedFactor));

        double baseDamage = plugin.getConfigUtil().getDouble("pyromancer.base-damage", 4.0);
        int igniteTicksBase = plugin.getConfigUtil().getInt("pyromancer.ignite-ticks", 60);
        int igniteTicks = igniteTicksBase + (sp != null ? sp.getIgniteBonusTicks() : 0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        double damage = baseDamage + (sp != null ? sp.getDamageAddTotal() : 0.0);
        damage *= Math.max(1.0, 1.0 + 0.1 * (level - 1));
        if (sp != null) {
            damage *= (1.0 + sp.getDamageMult());
        }

        // Evolution Nova Partikel mit realem Radius
        try {
            if (sp != null && !sp.isEvoPyroNova()) {
                int ignReq = plugin.getConfigUtil().getInt("evo.pyromancer.ignite-ticks-min", 60);
                double dmgMultReq = plugin.getConfigUtil().getDouble("evo.pyromancer.damage-mult-min", 0.40);
                if (sp.getIgniteBonusTicks() + igniteTicksBase >= ignReq && sp.getDamageMult() >= dmgMultReq) {
                    sp.setEvoPyroNova(true);
                    player.sendMessage("§6Evolution freigeschaltet: Flammennova!");
                }
            }
            if (sp != null && sp.isEvoPyroNova()) {
                // kleine Nova alle Ticks (dezent): AoE um den Spieler
                double novaR = Math.min(6.0, radius * 0.8);
                for (LivingEntity m : spawnManager.getNearbyWaveMobs(loc, novaR)) {
                    try { m.damage(Math.max(1.0, damage * 0.25), player); } catch (Throwable ignored) {}
                    try { m.setFireTicks(Math.max(m.getFireTicks(), igniteTicks/2)); } catch (Throwable ignored) {}
                }
                // Partikelkreis anhand novaR (Hitbox)
                for (int i = 0; i < 24; i++) {
                    double ang = 2 * Math.PI * i / 24.0;
                    double x = loc.getX() + Math.cos(ang) * novaR;
                    double z = loc.getZ() + Math.sin(ang) * novaR;
                    loc.getWorld().spawnParticle(Particle.FLAME, new Location(loc.getWorld(), x, loc.getY() + 0.8, z), 1, 0.01, 0.01, 0.01, 0.0);
                }
            }
        } catch (Throwable ignored) {}

        // swirling flame ring around the player
        try {
            int points = 20;
            for (int i = 0; i < points; i++) {
                double ang = 2 * Math.PI * i / points + (System.currentTimeMillis() % 1000) / 1000.0 * 2 * Math.PI;
                double r = 0.8;
                double x = loc.getX() + Math.cos(ang) * r;
                double z = loc.getZ() + Math.sin(ang) * r;
                loc.getWorld().spawnParticle(Particle.SMALL_FLAME, new Location(loc.getWorld(), x, loc.getY() + 1.0, z), 1, 0.01, 0.01, 0.01, 0.0);
            }
            loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.25f, 0.8f);
        } catch (Throwable ignored) {}

        List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(loc, radius);
        if (mobs.isEmpty()) return;
        mobs.sort(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(loc)));

        int hit = 0;
        for (LivingEntity target : mobs) {
            if (hit >= targets) break;
            if (target == null || !target.isValid()) continue;
            // fiery beam from player to target
            try {
                Location from = loc.clone().add(0, 1.5, 0);
                Location to = target.getLocation().clone().add(0, 1.0, 0);
                org.bukkit.util.Vector dir = to.toVector().subtract(from.toVector());
                int steps = Math.max(6, (int) Math.min(28, from.distance(to) * 4));
                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    org.bukkit.util.Vector pt = from.toVector().add(dir.clone().multiply(t));
                    Location pLoc = new Location(from.getWorld(), pt.getX(), pt.getY(), pt.getZ());
                    from.getWorld().spawnParticle(Particle.FLAME, pLoc, 2, 0.02, 0.02, 0.02, 0.0);
                    if (i % 3 == 0) from.getWorld().spawnParticle(Particle.LAVA, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
                }
                loc.getWorld().playSound(loc, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 0.5f, 0.6f);
            } catch (Throwable ignored) {}

            try { target.damage(damage, player); } catch (Throwable ignored) {}
            try { target.setFireTicks(Math.max(target.getFireTicks(), igniteTicks)); } catch (Throwable ignored) {}
            hit++;
        }
    }
}
