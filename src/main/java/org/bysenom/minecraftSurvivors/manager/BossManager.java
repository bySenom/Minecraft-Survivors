package org.bysenom.minecraftSurvivors.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Simpler Endgame-Boss-Controller: spawnt bei Enrage >= 1.0 einen Boss und verwaltet eine globale Bossbar & Hologram-HP.
 */
public class BossManager {

    private final MinecraftSurvivors plugin;
    private final SpawnManager spawnManager;
    private LivingEntity boss;
    private BossBar bossbar;
    private BukkitTask task;
    private final Set<UUID> holograms = new HashSet<>();

    public BossManager(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    public synchronized boolean isBossActive() { return boss != null && boss.isValid() && !boss.isDead(); }

    public synchronized void tick() {
        if (plugin.getGameManager() == null || plugin.getGameManager().getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING) return;
        double enrage = spawnManager.getEnrageProgress();
        if (!isBossActive() && enrage >= 1.0 && plugin.getConfigUtil().getBoolean("endgame.enabled", true)) {
            trySpawnBoss();
        }
        if (isBossActive()) updateUi();
    }

    private void trySpawnBoss() {
        // Spawn Boss beim erstbesten Online-Spieler
        Player p = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (p == null) return;
        Location loc = p.getLocation();
        World w = loc.getWorld(); if (w == null) return;
        EntityType type = resolveBossType();
        LivingEntity le = (LivingEntity) w.spawnEntity(loc.clone().add(0, 0, 8), type);
        // Stats hochsetzen relativ zur Spielzeit und EnemyPower
        double minutes = spawnManager.getElapsedMinutes();
        double power = spawnManager.getEnemyPowerIndex();
        double hpBase = 200.0 * Math.max(1.0, minutes) * Math.max(1.0, Math.log10(Math.max(10.0, power*50)));
        AttributeInstance maxHp = le.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(hpBase);
            le.setHealth(hpBase);
        }
        // Leichten Speed- und Damage-Buff
        try {
            AttributeInstance spd = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(spd.getBaseValue() * 1.15);
            AttributeInstance dmg = le.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(Math.max(6.0, dmg.getBaseValue() * 1.5));
        } catch (Throwable ignored) {}
        // Name & Markierung
        try { le.customName(Component.text("ENRAGE BOSS").color(NamedTextColor.RED)); le.setCustomNameVisible(true);} catch (Throwable ignored) {}
        this.boss = le;
        broadcastSpawn();
        ensureTask();
    }

    private EntityType resolveBossType() {
        String t = plugin.getConfigUtil().getString("endgame.boss.type", "WITHER");
        try { return EntityType.valueOf(t.toUpperCase()); } catch (Throwable ignored) {}
        return EntityType.WITHER;
    }

    private void ensureTask() {
        if (task != null && !task.isCancelled()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBoss, 0L, 10L);
    }

    private void tickBoss() {
        if (!isBossActive()) { clearUi(); return; }
        updateUi();
        // simple Mechanik: Pulsierende AoE um den Boss, skaliert mit EnemyPower
        double power = spawnManager.getEnemyPowerIndex();
        double radius = Math.min(12.0, 4.0 + Math.log1p(power) * 1.5);
        if (boss.getWorld() == null) return;
        for (Player pl : boss.getWorld().getPlayers()) {
            if (!pl.isOnline()) continue;
            if (pl.getLocation().distanceSquared(boss.getLocation()) <= radius*radius) {
                try { pl.damage(Math.max(2.0, Math.log1p(power))); } catch (Throwable ignored) {}
                try { pl.playSound(pl.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.2f, 0.6f);} catch (Throwable ignored) {}
            }
        }
        // Partikel-Ring
        try {
            int points = Math.max(24, (int) Math.round(2*Math.PI*radius));
            for (int i=0;i<points;i++) {
                double a = 2*Math.PI*i/points;
                double x = boss.getLocation().getX() + Math.cos(a) * radius;
                double z = boss.getLocation().getZ() + Math.sin(a) * radius;
                boss.getWorld().spawnParticle(Particle.LARGE_SMOKE, new Location(boss.getWorld(), x, boss.getLocation().getY()+0.2, z), 1, 0.02,0.02,0.02,0.0);
            }
        } catch (Throwable ignored) {}
        // Death check
        if (boss.isDead() || boss.getHealth() <= 0.0) {
            onBossDeath();
        }
    }

    private void updateUi() {
        if (!isBossActive()) { clearUi(); return; }
        double hp = boss.getHealth();
        double max = boss.getAttribute(Attribute.MAX_HEALTH) != null ? boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : hp;
        float prog = (float) Math.max(0.0, Math.min(1.0, hp / Math.max(1.0, max)));
        BossBar bar = bossbar;
        if (bar == null) {
            bar = BossBar.bossBar(Component.text("ENRAGE BOSS"), prog, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            bossbar = bar;
        } else {
            bar.progress(prog);
        }
        try { bar.name(Component.text(String.format("Boss HP %.0f/%.0f", hp, max), NamedTextColor.RED)); } catch (Throwable ignored) {}
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bar);
        }
        updateHologram();
    }

    private void clearUi() {
        if (bossbar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) try { p.hideBossBar(bossbar); } catch (Throwable ignored) {}
            bossbar = null;
        }
        // Holograms entfernen
        for (UUID id : holograms) {
            try { org.bukkit.entity.Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); } catch (Throwable ignored) {}
        }
        holograms.clear();
    }

    private void updateHologram() {
        // Ein einfaches TextDisplay über dem Boss als HP-Leiste
        if (!isBossActive()) return;
        try {
            double hp = boss.getHealth();
            double max = boss.getAttribute(Attribute.MAX_HEALTH) != null ? boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : hp;
            String bar = makeBar(hp / Math.max(1.0, max), 24);
            Component c = Component.text(bar).color(NamedTextColor.RED);
            // Erstelle ein TextDisplay einmalig und bewege es mit
            org.bukkit.entity.TextDisplay td = null;
            for (UUID id : holograms) {
                org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                if (e instanceof org.bukkit.entity.TextDisplay) { td = (org.bukkit.entity.TextDisplay) e; break; }
            }
            if (td == null) {
                td = boss.getWorld().spawn(boss.getLocation().clone().add(0, 2.4, 0), org.bukkit.entity.TextDisplay.class, t -> {
                    t.text(c);
                    try { t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER); } catch (Throwable ignored) {}
                    t.setShadowed(true);
                    t.setSeeThrough(true);
                    t.setPersistent(true);
                });
                holograms.add(td.getUniqueId());
            } else {
                td.text(c);
            }
            try { td.teleport(boss.getLocation().clone().add(0, 2.4, 0)); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private String makeBar(double ratio, int len) {
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int fill = (int) Math.round(ratio * len);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) sb.append(i < fill ? '§' + "c█" : '§' + "7░");
        sb.append("]");
        return sb.toString();
    }

    private void onBossDeath() {
        // Drop belohnen
        try {
            boss.getWorld().dropItemNaturally(boss.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR, 1));
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
        broadcastDefeat();
        clearUi();
        boss = null;
    }

    private void broadcastSpawn() {
        Bukkit.broadcast(Component.text("§cDer ENRAGE BOSS ist erschienen!"));
        for (Player p : Bukkit.getOnlinePlayers()) try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);} catch (Throwable ignored) {}
    }

    private void broadcastDefeat() {
        Bukkit.broadcast(Component.text("§aDer ENRAGE BOSS wurde besiegt!"));
    }
}
