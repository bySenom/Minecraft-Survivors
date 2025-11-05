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

        double baseRadius = plugin.getConfigUtil().getDouble("paladin.radius", 5.5);
        double radius = baseRadius * (1.0 + (sp != null ? sp.getRadiusMult() : 0.0));
        double baseDamage = plugin.getConfigUtil().getDouble("paladin.base-damage", 2.5);
        double healBase = plugin.getConfigUtil().getDouble("paladin.heal", 1.0);
        double heal = healBase + (sp != null ? sp.getHealBonus() : 0.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        double damage = baseDamage + (sp != null ? sp.getFlatDamage() : 0.0) + (sp != null ? sp.getBonusDamage() : 0.0);
        damage *= Math.max(1.0, 1.0 + 0.08 * (level - 1));
        if (sp != null) damage *= (1.0 + sp.getDamageMult());

        List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(loc, radius);
        if (!mobs.isEmpty()) {
            try {
                int points = 32;
                for (int i = 0; i < points; i++) {
                    double ang = 2 * Math.PI * i / points;
                    double x = loc.getX() + Math.cos(ang) * radius;
                    double z = loc.getZ() + Math.sin(ang) * radius;
                    for (int h = 0; h < 3; h++) {
                        Location p = new Location(loc.getWorld(), x, loc.getY() + 0.2 + h * 0.4, z);
                        loc.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.02, 0.02, 0.02, 0.0);
                    }
                }
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.9f);
                loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.6f);
            } catch (Throwable ignored) {}
        }

        for (LivingEntity target : mobs) {
            if (target == null || !target.isValid()) continue;
            try { target.damage(damage, player); } catch (Throwable ignored) {}
        }

        // heal nearby allied players as well (exclude self here to avoid double heal)
        try {
            double r2 = radius * radius;
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other == null || !other.isOnline()) continue;
                if (other.getWorld() != loc.getWorld()) continue;
                if (other.getUniqueId().equals(player.getUniqueId())) continue; // self handled below
                if (other.getLocation().distanceSquared(loc) > r2) continue;
                double before = other.getHealth();
                // particles on ally
                try { other.getWorld().spawnParticle(Particle.HEART, other.getLocation().add(0, 1.2, 0), 2, 0.15, 0.2, 0.15, 0.01); } catch (Throwable ignored) {}
                // safe heal up to max
                try {
                    AttributeInstance maxAttr = null;
                    try { maxAttr = other.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")); } catch (Throwable ignored) {}
                    double maxH = maxAttr != null ? maxAttr.getBaseValue() : 20.0;
                    double newH = Math.min(maxH, other.getHealth() + heal);
                    other.setHealth(newH);
                    double healed = Math.max(0.0, newH - before);
                    try { MinecraftSurvivors.getInstance().getStatsMeterManager().recordHeal(other.getUniqueId(), healed); } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
                // subtle chime per ally
                try { other.playSound(other.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.8f); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // heal sparkle on player (self)
        try {
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 2, 0.15, 0.2, 0.15, 0.01);
        } catch (Throwable ignored) {}
        // leichte Heilung (versionssicher) - self
        try {
            double before = player.getHealth();
            AttributeInstance max = null;
            try {
                Attribute maxHealthAttr = Attribute.valueOf("GENERIC_MAX_HEALTH");
                max = player.getAttribute(maxHealthAttr);
            } catch (Throwable ignored) {}
            double maxH = max != null ? max.getBaseValue() : 20.0;
            double newH = Math.min(maxH, player.getHealth() + heal);
            player.setHealth(newH);
            double healed = Math.max(0.0, newH - before);
            try { MinecraftSurvivors.getInstance().getStatsMeterManager().recordHeal(player.getUniqueId(), healed); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
