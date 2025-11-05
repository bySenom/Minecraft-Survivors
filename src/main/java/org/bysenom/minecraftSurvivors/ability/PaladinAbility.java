package org.bysenom.minecraftSurvivors.ability;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class PaladinAbility implements Ability {

    private final MinecraftSurvivors plugin;
    private final SpawnManager spawnManager;

    public PaladinAbility(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public void tick(Player player, SurvivorPlayer sp) {
        if (player == null || !player.isOnline() || !player.isValid()) return;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        double baseRadius = plugin.getConfigUtil().getDouble("paladin.radius", 7.0);
        double radius = baseRadius * (1.0 + (sp != null ? sp.getRadiusMult() : 0.0));
        double baseDamage = plugin.getConfigUtil().getDouble("paladin.base-damage", 2.5);
        double healBase = plugin.getConfigUtil().getDouble("paladin.heal", 1.0);
        double heal = healBase + (sp != null ? sp.getHealBonus() : 0.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        double damage = baseDamage + (sp != null ? sp.getFlatDamage() : 0.0) + (sp != null ? sp.getBonusDamage() : 0.0);
        damage *= Math.max(1.0, 1.0 + 0.08 * (level - 1));
        if (sp != null) damage *= (1.0 + sp.getDamageMult());

        // attack speed factor: repeat light-weight damage/heal loop more times (cap x2.5) without re-spawning heavy pulse each time
        double as = sp != null ? Math.max(0.0, sp.getAttackSpeedMult()) : 0.0;
        int repeats = Math.max(1, (int) Math.floor(Math.min(2.5, 1.0 + as)));

        // spawn one pulse visual only once per tick
        try { spawnPulse(player, radius); } catch (Throwable ignored) {}

        for (int rep = 0; rep < repeats; rep++) {
            List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(loc, radius);
            for (LivingEntity target : mobs) {
                if (target == null || !target.isValid()) continue;
                try { target.damage(damage, player); } catch (Throwable ignored) {}
            }
            // Allies heal per repeat
            try {
                double r2 = radius * radius;
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other == null || !other.isOnline()) continue;
                    if (other.getWorld() != loc.getWorld()) continue;
                    if (other.getLocation().distanceSquared(loc) > r2) continue;
                    double before = other.getHealth();
                    try { other.getWorld().spawnParticle(Particle.HEART, other.getLocation().add(0, 1.2, 0), 1, 0.1, 0.1, 0.1, 0.0); } catch (Throwable ignored) {}
                    try {
                        AttributeInstance maxAttr = null;
                        try { maxAttr = other.getAttribute(Attribute.MAX_HEALTH); } catch (Throwable ignored) {}
                        double maxH = maxAttr != null ? maxAttr.getBaseValue() : 20.0;
                        double newH = Math.min(maxH, other.getHealth() + heal);
                        other.setHealth(newH);
                        double healed = Math.max(0.0, newH - before);
                        if (healed > 0) {
                            try { MinecraftSurvivors.getInstance().getStatsMeterManager().recordHeal(player.getUniqueId(), healed); } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }

    private void spawnPulse(Player player, double radius) {
        if (player == null || !player.isOnline() || !player.isValid()) return;
        final int steps = 6;            // quicker
        final int total = steps + 4;    // expand then quick fade
        final int points = 36;          // fewer points for less continuous disc
        try { player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.28f, 2.0f); } catch (Throwable ignored) {}
        final int[] t = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (player == null || !player.isOnline() || !player.isValid()) { task.cancel(); return; }
            Location base = player.getLocation();
            if (base == null || base.getWorld() == null) { task.cancel(); return; }
            if (t[0] > total) {
                task.cancel();
                if (plugin.getConfigUtil().getBoolean("paladin.implosion-enabled", true)) {
                    implosion(player, radius * 0.9);
                }
                return;
            }
            double phase = t[0] / (double) steps;       // 0..1
            double r = Math.max(0.1, radius * Math.min(1.0, phase));
            double fade = Math.max(0.0, 1.0 - phase);   // fade out quickly
            org.bukkit.World w = base.getWorld();
            // outer sparse ring
            for (int i = 0; i < points; i++) {
                double ang = 2 * Math.PI * i / points;
                double x = base.getX() + Math.cos(ang) * r;
                double z = base.getZ() + Math.sin(ang) * r;
                Location p = new Location(w, x, base.getY() + 0.15, z);
                try { w.spawnParticle(Particle.END_ROD, p, (int)Math.max(1, 2*fade), 0.01, 0.01, 0.01, 0.0); } catch (Throwable ignored) {}
            }
            // tiny inner sparks that disappear even quicker
            double r2 = Math.max(0.05, r * 0.55);
            for (int i = 0; i < points/3; i++) {
                double ang = 2 * Math.PI * i / (points/3);
                double x = base.getX() + Math.cos(ang) * r2;
                double z = base.getZ() + Math.sin(ang) * r2;
                Location p = new Location(w, x, base.getY() + 0.22, z);
                try { w.spawnParticle(Particle.CRIT, p, (int)Math.max(0, 1*fade), 0.005, 0.005, 0.005, 0.0); } catch (Throwable ignored) {}
            }
            t[0]++;
        }, 0L, 1L);
    }

    private void implosion(Player player, double radius) {
        if (player == null || !player.isOnline() || !player.isValid()) return;
        final int ticks = 3; // 2–3 ticks
        final int points = 24;
        final double startR = Math.max(0.3, radius);
        for (int i = 0; i < ticks; i++) {
            final int ti = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player == null || !player.isOnline() || !player.isValid()) return;
                Location base = player.getLocation();
                if (base == null || base.getWorld() == null) return;
                double r = Math.max(0.1, startR * (1.0 - ti / (double) ticks));
                org.bukkit.World w = base.getWorld();
                for (int j = 0; j < points; j++) {
                    double ang = 2 * Math.PI * j / points;
                    double x = base.getX() + Math.cos(ang) * r;
                    double z = base.getZ() + Math.sin(ang) * r;
                    Location p = new Location(w, x, base.getY() + 0.1, z);
                    try { w.spawnParticle(Particle.INSTANT_EFFECT, p, 1, 0.01, 0.01, 0.01, 0.0); } catch (Throwable ignored) {}
                }
                try { player.playSound(base, Sound.BLOCK_BEACON_DEACTIVATE, 0.18f, 1.9f); } catch (Throwable ignored) {}
            }, i);
        }
    }
}
