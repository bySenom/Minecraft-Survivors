package org.bysenom.minecraftSurvivors.manager;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Tick-Manager für passive/aktive Zusatz-Skills (bis zu 5) pro Spieler.
 * + Passive Weapons (Vampire Survivors-Style): w_lightning, w_fire, w_ranged, w_holy
 */
public class SkillManager {
    private final MinecraftSurvivors plugin;
    private org.bukkit.scheduler.BukkitTask task;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillManager(MinecraftSurvivors plugin) { this.plugin = plugin; }

    public void start() { stop(); task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L); }
    public void stop() { if (task != null) { task.cancel(); task = null; } }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
            if (sp == null) continue;
            if (plugin.getGameManager().isPlayerPaused(p.getUniqueId())) continue;
            // Passive weapons
            for (String w : sp.getWeapons()) {
                int lvl = sp.getWeaponLevel(w);
                switch (w) {
                    case "w_lightning": runWLightning(p, sp, lvl); break;
                    case "w_fire": runWFire(p, sp, lvl); break;
                    case "w_ranged": runWRanged(p, sp, lvl); break;
                    case "w_holy": runWHoly(p, sp, lvl); break;
                    default: break;
                }
            }
            // Passive/active skills (bestehendes)
            for (String key : sp.getSkills()) {
                int lvl = sp.getSkillLevel(key);
                switch (key) {
                    case "shockwave": runShockwave(p, lvl); break;
                    case "heal_totem": runHealTotem(p, lvl); break;
                    case "frost_nova": runFrostNova(p, lvl); break;
                    default: break;
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

    // --- Weapons ---
    private void runWLightning(Player p, SurvivorPlayer sp, int lvl) {
        long cd = Math.max(400, 1600 - lvl * 120L);
        if (onCd(p.getUniqueId(), "w_lightning", cd)) return;
        double damage = 2.0 + lvl * 0.8 + sp.getFlatDamage();
        double radius = 8.0 * (1.0 + sp.getRadiusMult());
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), radius);
        if (mobs.isEmpty()) return;
        org.bukkit.entity.LivingEntity target = mobs.get(new java.util.Random().nextInt(mobs.size()));
        try {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.damage(damage * (1.0 + sp.getDamageMult()), p);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1.0,0), 14, 0.3,0.3,0.3, 0.02);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 2.0f);
        } catch (Throwable ignored) {}
        // synergy: lightning + fire -> ignite
        if (sp.getWeapons().contains("w_fire")) {
            int igniteTicks = 40 + sp.getIgniteBonusTicks();
            try { target.setFireTicks(Math.max(target.getFireTicks(), igniteTicks)); } catch (Throwable ignored) {}
        }
    }

    private void runWFire(Player p, SurvivorPlayer sp, int lvl) {
        long cd = Math.max(400, 1400 - lvl * 100L);
        if (onCd(p.getUniqueId(), "w_fire", cd)) return;
        double radius = 4.0 + lvl * 0.5;
        double damage = 0.8 + lvl * 0.4 + sp.getFlatDamage() * 0.3;
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.setFireTicks(Math.max(le.getFireTicks(), 40 + sp.getIgniteBonusTicks()));
                le.damage(damage * (1.0 + sp.getDamageMult()), p);
                le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0,1.0,0), 6, 0.25,0.25,0.25, 0.01);
            } catch (Throwable ignored) {}
        }
        try { p.playSound(p.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.3f, 1.6f); } catch (Throwable ignored) {}
    }

    private void runWRanged(Player p, SurvivorPlayer sp, int lvl) {
        long cd = Math.max(300, 900 - lvl * 60L);
        if (onCd(p.getUniqueId(), "w_ranged", cd)) return;
        double range = 16.0 + lvl * 2.0;
        double damage = 1.8 + lvl * 0.6 + sp.getFlatDamage() * 0.6;
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), range);
        if (mobs.isEmpty()) return;
        org.bukkit.entity.LivingEntity target = mobs.get(0);
        org.bukkit.Location eye = p.getEyeLocation();
        org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();
        org.bukkit.Location cur = eye.clone();
        double speed = 1.3 + 0.05 * lvl;
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0; @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                cur.add(dir.clone().multiply(speed));
                cur.getWorld().spawnParticle(Particle.CRIT, cur, 2, 0.02,0.02,0.02, 0.0);
                if (cur.distanceSquared(target.getLocation()) < 1.0) {
                    try { target.damage(damage * (1.0 + sp.getDamageMult()), p); } catch (Throwable ignored) {}
                    try { p.playSound(cur, Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.6f); } catch (Throwable ignored) {}
                    cancel(); return;
                }
                if (++t > 40) cancel();
            }}.runTaskTimer(plugin, 0L, 1L);
    }

    private void runWHoly(Player p, SurvivorPlayer sp, int lvl) {
        long cd = Math.max(600, 1800 - lvl * 120L);
        if (onCd(p.getUniqueId(), "w_holy", cd)) return;
        double radius = 5.0 + lvl * 0.6 + sp.getRadiusMult() * 2.0;
        double damage = 1.2 + lvl * 0.5 + sp.getFlatDamage() * 0.4;
        org.bukkit.Location loc = p.getLocation();
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try { le.damage(damage * (1.0 + sp.getDamageMult()), p); } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.END_ROD, loc.add(0,1.0,0), 28, radius/2, 0.3, radius/2, 0.0);
            p.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.8f);
        } catch (Throwable ignored) {}
        // synergy: holy burst on kill handled in EntityDeathListener optional (future)
    }

    // --- Existing skills ---
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
        double cd = 3000 - Math.min(2000, lvl * 200);
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
        double cd = 5000 - Math.min(3000, lvl * 250);
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
