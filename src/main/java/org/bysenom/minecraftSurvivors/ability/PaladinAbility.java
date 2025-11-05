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

        // Pulsierende Welle (outward und zurück)
        try { spawnPulse(loc, radius); } catch (Throwable ignored) {}

        List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(loc, radius);
        for (LivingEntity target : mobs) {
            if (target == null || !target.isValid()) continue;
            try { target.damage(damage, player); } catch (Throwable ignored) {}
        }

        // Allies im Radius heilen — HPS NUR für den Heiler zählen (nicht Empfänger)
        try {
            double r2 = radius * radius;
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other == null || !other.isOnline()) continue;
                if (other.getWorld() != loc.getWorld()) continue;
                if (other.getLocation().distanceSquared(loc) > r2) continue;
                boolean isSelf = other.getUniqueId().equals(player.getUniqueId());
                double before = other.getHealth();
                // Herz-Partikel
                try { other.getWorld().spawnParticle(Particle.HEART, other.getLocation().add(0, 1.2, 0), 2, 0.15, 0.2, 0.15, 0.01); } catch (Throwable ignored) {}
                // Heal bis max
                try {
                    AttributeInstance maxAttr = null;
                    try { maxAttr = other.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH")); } catch (Throwable ignored) {}
                    double maxH = maxAttr != null ? maxAttr.getBaseValue() : 20.0;
                    double newH = Math.min(maxH, other.getHealth() + heal);
                    other.setHealth(newH);
                    double healed = Math.max(0.0, newH - before);
                    // HPS nur dem Heiler gutschreiben
                    if (healed > 0) {
                        try { MinecraftSurvivors.getInstance().getStatsMeterManager().recordHeal(player.getUniqueId(), healed); } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
                // dezentes Chime je Empfänger
                try { other.playSound(other.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, 1.85f); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void spawnPulse(Location center, double radius) {
        if (center == null || center.getWorld() == null) return;
        final int steps = 6;            // nach außen 6 Frames
        final int total = steps * 2;    // hin + zurück
        final int points = 36;          // Punkte pro Ring
        final org.bukkit.World w = center.getWorld();
        try { w.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 1.9f); } catch (Throwable ignored) {}
        try { w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.25f, 1.7f); } catch (Throwable ignored) {}
        final int[] t = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (t[0] > total) { task.cancel(); return; }
            double phase = (t[0] <= steps) ? (t[0] / (double) steps) : ((total - t[0]) / (double) steps);
            double r = Math.max(0.1, radius * phase);
            for (int i = 0; i < points; i++) {
                double ang = 2 * Math.PI * i / points;
                double x = center.getX() + Math.cos(ang) * r;
                double z = center.getZ() + Math.sin(ang) * r;
                Location p = new Location(w, x, center.getY() + 0.3, z);
                try { w.spawnParticle(Particle.END_ROD, p, 2, 0.02, 0.02, 0.02, 0.0); } catch (Throwable ignored) {}
                if (i % 6 == 0) {
                    try { w.spawnParticle(Particle.NOTE, p, 1, 0.0, 0.0, 0.0, 0.0); } catch (Throwable ignored) {}
                }
            }
            t[0]++;
        }, 0L, 2L);
    }
}
