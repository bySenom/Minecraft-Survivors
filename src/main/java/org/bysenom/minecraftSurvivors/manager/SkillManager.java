package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tick-Manager für passive/aktive Zusatz-Skills (bis zu 5) pro Spieler.
 * - shockwave: periodische Stoßwelle
 * - dash: kurzer Dash auf Sneak (Cooldown)
 * - heal_totem: periodischer kleiner Heal
 * - frost_nova: AoE-Slow+Damage
 */
public class SkillManager {
    private final MinecraftSurvivors plugin;
    private org.bukkit.scheduler.BukkitTask task;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillManager(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L); // 1s Takt
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
            if (sp == null) continue;
            if (plugin.getGameManager().isPlayerPaused(p.getUniqueId())) continue;
            List<String> skills = sp.getSkills();
            for (String key : skills) {
                int lvl = sp.getSkillLevel(key);
                switch (key) {
                    case "shockwave":
                        runShockwave(p, lvl);
                        break;
                    case "heal_totem":
                        runHealTotem(p, lvl);
                        break;
                    case "frost_nova":
                        runFrostNova(p, lvl);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private boolean onCd(UUID id, String key, long cdMs) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(id, k -> new HashMap<>());
        Long last = map.getOrDefault(key, 0L);
        if (now - last < cdMs) return true;
        map.put(key, now);
        return false;
    }

    private void runShockwave(Player p, int lvl) {
        double cd = plugin.getConfigUtil().getDouble("skills.shockwave.cooldown-ms", 800);
        if (onCd(p.getUniqueId(), "shockwave", (long)cd)) return;
        double radius = 4.0 + lvl * 0.6;
        double damage = 1.5 + lvl * 0.5;
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                org.bukkit.util.Vector v = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.6).setY(0.25);
                le.setVelocity(v);
            } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0,1.0,0), 18, 1.0, 0.2, 1.0, 0.0);
            p.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private void runHealTotem(Player p, int lvl) {
        double cd = 3000 - Math.min(2000, lvl * 200); // bis 1s
        if (onCd(p.getUniqueId(), "heal_totem", (long)cd)) return;
        double heal = 0.6 + lvl * 0.2;
        double radius = 6.0 + lvl * 0.5;
        for (Player other : p.getWorld().getPlayers()) {
            if (!other.getWorld().equals(p.getWorld())) continue;
            if (other.getLocation().distanceSquared(p.getLocation()) <= radius*radius) {
                try {
                    double max = other.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    other.setHealth(Math.min(max, other.getHealth() + heal));
                    other.spawnParticle(org.bukkit.Particle.HEART, other.getLocation().add(0,1.0,0), 4, 0.2, 0.2, 0.2, 0.0);
                } catch (Throwable ignored) {}
            }
        }
        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f); } catch (Throwable ignored) {}
    }

    private void runFrostNova(Player p, int lvl) {
        double cd = 5000 - Math.min(3000, lvl * 250); // bis 2s
        if (onCd(p.getUniqueId(), "frost_nova", (long)cd)) return;
        double radius = 5.0 + lvl * 0.5;
        double damage = 1.0 + lvl * 0.3;
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40 + lvl*10, 1, false, false, true));
                le.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, le.getLocation().add(0,1.0,0), 8, 0.2, 0.4, 0.2, 0.01);
            } catch (Throwable ignored) {}
        }
        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.5f, 1.6f); } catch (Throwable ignored) {}
    }
}
