package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class SkillListener implements Listener {

    private final MinecraftSurvivors plugin;
    private final NamespacedKey shockwaveKey;
    private final java.util.Map<java.util.UUID, Long> shockwaveCd = new java.util.concurrent.ConcurrentHashMap<>();

    // Genkidama
    private final NamespacedKey genkidamaKey;
    private final java.util.Map<java.util.UUID, Long> genkiCd = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Charge> genkiCharge = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class Charge {
        final long startMs; org.bukkit.scheduler.BukkitTask task; Charge(long s){ this.startMs = s; }
    }

    public SkillListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
        this.shockwaveKey = new NamespacedKey(plugin, "skill_shockwave");
        this.genkidamaKey = new NamespacedKey(plugin, "skill_genkidama");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        org.bukkit.persistence.PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        boolean isShock = false, isGenki = false;
        try {
            if (pdc.has(shockwaveKey, PersistentDataType.BYTE)) {
                Byte b = pdc.get(shockwaveKey, PersistentDataType.BYTE);
                isShock = b != null && b != 0;
            } else if (pdc.has(shockwaveKey, PersistentDataType.STRING)) {
                String s = pdc.get(shockwaveKey, PersistentDataType.STRING);
                isShock = s != null && !s.isEmpty();
            }
        } catch (Throwable ignored) {}
        try {
            if (pdc.has(genkidamaKey, PersistentDataType.BYTE)) {
                Byte b = pdc.get(genkidamaKey, PersistentDataType.BYTE);
                isGenki = b != null && b != 0;
            } else if (pdc.has(genkidamaKey, PersistentDataType.STRING)) {
                String s = pdc.get(genkidamaKey, PersistentDataType.STRING);
                isGenki = s != null && !s.isEmpty();
            }
        } catch (Throwable ignored) {}
        if (!isShock && !isGenki) return;
        Player p = e.getPlayer();
        if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(p.getUniqueId())) return; // block during pause

        if (isShock) {
            handleShockwave(p);
            return;
        }
        if (isGenki) {
            handleGenkidama(p);
        }
    }

    private void handleShockwave(Player p) {
        long now = System.currentTimeMillis();
        long last = shockwaveCd.getOrDefault(p.getUniqueId(), 0L);
        long cdMs = plugin.getConfigUtil().getInt("skills.shockwave.cooldown-ms", 800) ;
        if (now - last < cdMs) {
            long left = (cdMs - (now - last));
            org.bysenom.minecraftSurvivors.util.Msg.warn(p, "Shockwave CD: "+left+"ms");
            return;
        }
        shockwaveCd.put(p.getUniqueId(), now);
        double radius = plugin.getConfigUtil().getDouble("skills.shockwave.radius", 6.0);
        double damage = plugin.getConfigUtil().getDouble("skills.shockwave.damage", 2.0);
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = MinecraftSurvivors.getInstance().getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                org.bukkit.util.Vector v = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.25);
                le.setVelocity(v);
            } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0,1.0,0), 20, 1.0, 0.2, 1.0, 0.0);
            p.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private void handleGenkidama(Player p) {
        java.util.UUID uid = p.getUniqueId();
        long now = System.currentTimeMillis();
        long last = genkiCd.getOrDefault(uid, 0L);
        long cdMs = (long) plugin.getConfigUtil().getInt("skills.genkidama.cooldown-ms", 12000);
        Charge ch = genkiCharge.get(uid);
        if (ch == null) {
            // start charging
            if (now - last < cdMs) {
                long left = (cdMs - (now - last));
                org.bysenom.minecraftSurvivors.util.Msg.warn(p, "Genkidama CD: "+left+"ms");
                return;
            }
            long maxMs = (long) plugin.getConfigUtil().getInt("skills.genkidama.charge-max-ms", 4000);
            Charge c = new Charge(now);
            genkiCharge.put(uid, c);
            // charging visual task
            c.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!p.isOnline()) { stopCharge(uid); return; }
                long elapsed = System.currentTimeMillis() - c.startMs;
                if (elapsed >= maxMs) {
                    stopCharge(uid);
                    launchGenkidama(p, maxMs);
                    return;
                }
                // draw aura above head
                org.bukkit.Location head = p.getLocation().add(0, 1.7, 0);
                double pct = Math.min(1.0, elapsed / (double) maxMs);
                int points = 20;
                double r = 0.6 + pct * 0.8; // radius grows
                for (int i=0;i<points;i++) {
                    double th = (i/(double)points) * Math.PI*2;
                    double x = Math.cos(th) * r;
                    double z = Math.sin(th) * r;
                    p.getWorld().spawnParticle(Particle.END_ROD, head.clone().add(x, 0, z), 0, 0,0,0, 0);
                }
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, head, 2, 0.15, 0.15, 0.15, 0.0);
                try { p.playSound(head, Sound.BLOCK_BEACON_AMBIENT, 0.15f, 1.8f); } catch (Throwable ignored) {}
                p.sendActionBar(net.kyori.adventure.text.Component.text("Genkidama lädt: "+(int)(pct*100)+"%"));
            }, 0L, 2L);
            try { p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 1.8f); } catch (Throwable ignored) {}
        } else {
            // release
            long elapsed = now - ch.startMs;
            stopCharge(uid);
            launchGenkidama(p, elapsed);
        }
    }

    private void stopCharge(java.util.UUID uid) {
        Charge c = genkiCharge.remove(uid);
        if (c != null && c.task != null) { try { c.task.cancel(); } catch (Throwable ignored) {} }
    }

    private void launchGenkidama(Player p, long chargeMs) {
        long maxMs = (long) plugin.getConfigUtil().getInt("skills.genkidama.charge-max-ms", 4000);
        double pct = Math.min(1.0, chargeMs / (double) maxMs);
        double baseDmg = plugin.getConfigUtil().getDouble("skills.genkidama.damage-base", 6.0);
        double dmgPerSec = plugin.getConfigUtil().getDouble("skills.genkidama.damage-per-second", 4.0);
        double radiusBase = plugin.getConfigUtil().getDouble("skills.genkidama.radius-base", 3.0);
        double radiusPerSec = plugin.getConfigUtil().getDouble("skills.genkidama.radius-per-second", 1.0);
        double speed = plugin.getConfigUtil().getDouble("skills.genkidama.projectile-speed", 1.1);
        int maxTicks = (int) plugin.getConfigUtil().getInt("skills.genkidama.max-travel-ticks", 80);
        double radius = radiusBase + radiusPerSec * (pct * (maxMs/1000.0));
        double damage = baseDmg + dmgPerSec * (pct * (maxMs/1000.0));
        org.bukkit.Location loc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(1.2));
        org.bukkit.util.Vector vel = p.getLocation().getDirection().normalize().multiply(speed);
        try { p.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f); } catch (Throwable ignored) {}
        final double hitRadius = 1.0;
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            org.bukkit.Location cur = loc.clone();
            @Override public void run() {
                try {
                    if (!p.isOnline()) { explode(cur, radius, damage, p); cancel(); return; }
                    if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(p.getUniqueId())) { explode(cur, radius, damage, p); cancel(); return; }
                    // move stepwise with sub-steps for smoother trail
                    int sub = 2;
                    for (int s=0; s<sub; s++) {
                        cur.add(vel.clone().multiply(1.0/sub));
                        // trail ring
                        for (int i=0;i<8;i++) {
                            double th = (i/8.0) * Math.PI*2;
                            double rr = Math.max(0.2, radius*0.25);
                            double x = Math.cos(th)*rr;
                            double z = Math.sin(th)*rr;
                            cur.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, cur.clone().add(x, 0, z), 0, 0,0,0, 0);
                        }
                        cur.getWorld().spawnParticle(Particle.END_ROD, cur, 4, 0.05,0.05,0.05, 0.0);
                        if (cur.getBlock().getType().isSolid()) { explode(cur, radius, damage, p); cancel(); return; }
                        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(cur, hitRadius);
                        if (!mobs.isEmpty()) { explode(cur, radius, damage, p); cancel(); return; }
                    }
                    t++;
                    if (t >= maxTicks) { explode(cur, radius, damage, p); cancel(); }
                } catch (Throwable ignored) { cancel(); }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        genkiCd.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private void explode(org.bukkit.Location at, double radius, double damage, Player owner) {
        try {
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1, 0,0,0, 0.0);
            at.getWorld().spawnParticle(Particle.CLOUD, at, 20, 1.2, 0.6, 1.2, 0.01);
            at.getWorld().spawnParticle(Particle.SONIC_BOOM, at, 1, 0,0,0, 0.0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        } catch (Throwable ignored) {}
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(at, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try { le.damage(damage, owner); } catch (Throwable ignored) {}
        }
    }
}
