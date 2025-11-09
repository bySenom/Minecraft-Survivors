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

    private int abilityTick = 0; // counts ticks for ability cadence
    private java.util.Random rnd = new java.util.Random();

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
        Player p = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (p == null) return;
        Location loc = p.getLocation();
        World w = loc.getWorld(); if (w == null) return;
        EntityType type = resolveBossType();
        LivingEntity le = (LivingEntity) w.spawnEntity(loc.clone().add(0, 0, 8), type);
        double minutes = spawnManager.getElapsedMinutes();
        double power = spawnManager.getEnemyPowerIndex();
        double hpBaseCfg = plugin.getConfigUtil().getDouble("endgame.boss.base-hp", 200.0);
        double hpScaleMinMult = plugin.getConfigUtil().getDouble("endgame.boss.hp-minutes-mult", 1.0);
        double hpPowerLogBase = plugin.getConfigUtil().getDouble("endgame.boss.hp-power-log-base", 50.0);
        double hpBase = hpBaseCfg * Math.max(1.0, minutes * hpScaleMinMult) * Math.max(1.0, Math.log10(Math.max(10.0, power * hpPowerLogBase)));
        try {
            AttributeInstance maxHp = le.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null) {
                maxHp.setBaseValue(hpBase);
                le.setHealth(hpBase);
            }
        } catch (Throwable ignored) {}
        try {
            AttributeInstance spd = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(spd.getBaseValue() * plugin.getConfigUtil().getDouble("endgame.boss.speed-mult", 1.15));
            AttributeInstance dmg = le.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dmg != null) {
                double dmgBaseCfg = plugin.getConfigUtil().getDouble("endgame.boss.damage-base", 6.0);
                double dmgScale = plugin.getConfigUtil().getDouble("endgame.boss.damage-scale-per-minute", 1.2);
                double newDmg = Math.max(dmgBaseCfg, dmgBaseCfg + minutes * dmgScale);
                dmg.setBaseValue(newDmg);
            }
        } catch (Throwable ignored) {}
        try {
            String name = plugin.getConfigUtil().getString("endgame.boss.display-name", "APOKALYPTISCHER ENDBOSS");
            le.customName(Component.text(name).color(NamedTextColor.RED));
            le.setCustomNameVisible(true);
        } catch (Throwable ignored) {}
        this.boss = le;
        abilityTick = 0;
        broadcastSpawn();
        ensureTask();
    }

    private EntityType resolveBossType() {
        String t = plugin.getConfigUtil().getString("endgame.boss.type", "WARDEN");
        if (t == null) t = "WARDEN";
        String upper = t.toUpperCase();
        // Benutzer wünscht ausdrücklich KEIN WITHER
        if ("WITHER".equals(upper)) upper = "WARDEN"; // forcieren
        EntityType et = null;
        try { et = EntityType.valueOf(upper); } catch (Throwable ignored) {}
        if (et == null || !et.isAlive()) {
            // Fallback Kette
            for (String fallback : new String[]{"WARDEN","ENDER_DRAGON","IRON_GOLEM","GIANT","ZOMBIE"}) {
                try { EntityType f = EntityType.valueOf(fallback); if (f.isAlive()) { et = f; break; } } catch (Throwable ignored) {}
            }
        }
        if (et == null) et = EntityType.ZOMBIE;
        return et;
    }

    private void ensureTask() {
        if (task != null && !task.isCancelled()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBoss, 0L, 10L);
    }

    private void tickBoss() {
        if (!isBossActive()) { clearUi(); return; }
        updateUi();
        double power = spawnManager.getEnemyPowerIndex();
        double baseRadius = plugin.getConfigUtil().getDouble("endgame.boss.aura.base-radius", 4.0);
        double auraScale = plugin.getConfigUtil().getDouble("endgame.boss.aura.power-scale", 1.5);
        double radius = Math.min(plugin.getConfigUtil().getDouble("endgame.boss.aura.max-radius", 12.0), baseRadius + Math.log1p(power) * auraScale);
        if (boss.getWorld() == null) return;
        double auraDmgBase = plugin.getConfigUtil().getDouble("endgame.boss.aura.damage-base", 2.0);
        double auraDmgScale = plugin.getConfigUtil().getDouble("endgame.boss.aura.damage-log-scale", 1.0);
        double auraDmg = Math.max(auraDmgBase, auraDmgBase + Math.log1p(power) * auraDmgScale);
        for (Player pl : boss.getWorld().getPlayers()) {
            if (!pl.isOnline()) continue;
            if (pl.getLocation().distanceSquared(boss.getLocation()) <= radius*radius) {
                try { pl.damage(auraDmg); } catch (Throwable ignored) {}
                try { pl.playSound(pl.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.9f);} catch (Throwable ignored) {}
            }
        }
        // Partikel-Ring (neutraler Rauch/Endrod Mix)
        try {
            int points = Math.max(24, (int) Math.round(2*Math.PI*radius));
            for (int i=0;i<points;i++) {
                double a = 2*Math.PI*i/points;
                double x = boss.getLocation().getX() + Math.cos(a) * radius;
                double z = boss.getLocation().getZ() + Math.sin(a) * radius;
                boss.getWorld().spawnParticle(Particle.LARGE_SMOKE, new Location(boss.getWorld(), x, boss.getLocation().getY()+0.2, z), 1, 0.02,0.02,0.02,0.0);
                if (i % 3 == 0) boss.getWorld().spawnParticle(Particle.END_ROD, new Location(boss.getWorld(), x, boss.getLocation().getY()+0.25, z), 1, 0.01,0.01,0.01,0.0);
            }
        } catch (Throwable ignored) {}
        // Fähigkeitencadence
        abilityTick += 10; // tickBoss läuft alle 10 Ticks
        int abilityInterval = Math.max(40, plugin.getConfigUtil().getInt("endgame.boss.ability.interval-ticks", 120));
        if (abilityTick >= abilityInterval) {
            abilityTick = 0;
            performRandomAbility(power);
        }
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

    private void performRandomAbility(double power) {
        if (!isBossActive()) return;
        java.util.List<String> enabled = new java.util.ArrayList<>();
        for (String k : new String[]{"meteor","lightning_chain","summon_minions","shockwave"}) {
            if (plugin.getConfigUtil().getBoolean("endgame.boss.ability."+k, true)) enabled.add(k);
        }
        if (enabled.isEmpty()) return;
        String pick = enabled.get(rnd.nextInt(enabled.size()));
        switch (pick) {
            case "meteor" -> abilityMeteor(power);
            case "lightning_chain" -> abilityLightningChain(power);
            case "summon_minions" -> abilitySummonMinions(power);
            case "shockwave" -> abilityShockwave(power);
        }
    }

    private void abilityMeteor(double power) {
        // Ziel: weit entfernter Spieler -> verzögerter Einschlag
        Player target = boss.getWorld().getPlayers().stream().filter(Player::isOnline)
                .max(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(boss.getLocation()))).orElse(null);
        if (target == null) return;
        Location strike = target.getLocation().clone();
        strike.getWorld().playSound(strike, org.bukkit.Sound.ENTITY_PHANTOM_SWOOP, 0.8f, 0.5f);
        // Vorwarn-Ring
        try {
            int pts = 32;
            double warnR = 2.5;
            for (int i=0;i<pts;i++) {
                double a = 2*Math.PI*i/pts;
                double x = strike.getX()+Math.cos(a)*warnR;
                double z = strike.getZ()+Math.sin(a)*warnR;
                strike.getWorld().spawnParticle(Particle.END_ROD, new Location(strike.getWorld(), x, strike.getY()+0.1, z), 1,0,0,0,0);
            }
        } catch (Throwable ignored) {}
        // Einschlag nach Delay
        double dmg = Math.max(8.0, 8.0 + Math.log1p(power)*4.0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { strike.getWorld().playSound(strike, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);} catch (Throwable ignored) {}
            try { strike.getWorld().spawnParticle(Particle.EXPLOSION, strike.clone().add(0,0.6,0), 1); } catch (Throwable ignored) {}
            for (Player pl : strike.getWorld().getPlayers()) {
                if (pl.getLocation().distanceSquared(strike) <= 6.25) {
                    try { pl.damage(dmg); } catch (Throwable ignored) {}
                    try { pl.setVelocity(pl.getLocation().toVector().subtract(strike.toVector()).normalize().multiply(1.2).setY(0.6)); } catch (Throwable ignored) {}
                }
            }
        }, 40L); // 2s Warnung
    }

    private void abilityLightningChain(double power) {
        java.util.List<Player> plist = boss.getWorld().getPlayers().stream().filter(Player::isOnline).toList();
        if (plist.isEmpty()) return;
        Player start = plist.get(rnd.nextInt(plist.size()));
        double chainRange = plugin.getConfigUtil().getDouble("endgame.boss.lightning.chain-range", 12.0);
        int maxChains = plugin.getConfigUtil().getInt("endgame.boss.lightning.max-chains", 4);
        double dmg = Math.max(6.0, 6.0 + Math.log1p(power)*3.0);
        java.util.Set<Player> hit = new java.util.HashSet<>();
        Player current = start;
        for (int c=0;c<maxChains && current!=null;c++) {
            strikeLightningSafe(current, dmg);
            hit.add(current);
            Player next = null;
            double best = Double.MAX_VALUE;
            for (Player pl : plist) {
                if (hit.contains(pl)) continue;
                double d2 = pl.getLocation().distanceSquared(current.getLocation());
                if (d2 <= chainRange*chainRange && d2 < best) { best=d2; next=pl; }
            }
            current = next;
        }
        try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.6f);} catch (Throwable ignored) {}
    }

    private void strikeLightningSafe(Player pl, double dmg) {
        try {
            Location loc = pl.getLocation();
            loc.getWorld().strikeLightningEffect(loc);
            pl.damage(dmg);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0,1.0,0), 14,0.3,0.3,0.3,0.02);
        } catch (Throwable ignored) {}
    }

    private void abilitySummonMinions(double power) {
        int count = plugin.getConfigUtil().getInt("endgame.boss.minions.count", 4);
        double spread = plugin.getConfigUtil().getDouble("endgame.boss.minions.spread", 6.0);
        for (int i=0;i<count;i++) {
            Location base = boss.getLocation().clone().add((rnd.nextDouble()-0.5)*spread, 0, (rnd.nextDouble()-0.5)*spread);
            try { base.setY(boss.getWorld().getHighestBlockYAt(base)+1); } catch (Throwable ignored) {}
            EntityType type = pickMinionType();
            LivingEntity mob;
            try { mob = (LivingEntity) boss.getWorld().spawnEntity(base, type); } catch (Throwable ex) { mob = (LivingEntity) boss.getWorld().spawnEntity(base, EntityType.ZOMBIE);}
            // Mark & slight buffs
            try { mob.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ms_wave"), org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);} catch (Throwable ignored) {}
            try { mob.customName(Component.text("§6Minion").color(NamedTextColor.GOLD)); mob.setCustomNameVisible(true);} catch (Throwable ignored) {}
            try {
                AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH); if (hp!=null) { double newHp = Math.max(1.0, hp.getBaseValue()* (1.2+ Math.log1p(power)*0.3)); hp.setBaseValue(newHp); mob.setHealth(newHp);} } catch (Throwable ignored) {}
            try {
                AttributeInstance dmg = mob.getAttribute(Attribute.ATTACK_DAMAGE); if (dmg!=null) dmg.setBaseValue(Math.max(3.0, dmg.getBaseValue()* (1.0+ Math.log1p(power)*0.4))); } catch (Throwable ignored) {}
            try { boss.getWorld().spawnParticle(Particle.PORTAL, base, 24,0.4,0.4,0.4,0.1);} catch (Throwable ignored) {}
        }
        try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.6f);} catch (Throwable ignored) {}
    }

    private EntityType pickMinionType() {
        String cfg = plugin.getConfigUtil().getString("endgame.boss.minions.type", "ZOMBIE");
        try { EntityType et = EntityType.valueOf(cfg.toUpperCase()); if (et.isAlive()) return et; } catch (Throwable ignored) {}
        // Fallback rotation
        for (String t : new String[]{"HUSK","DROWNED","ZOMBIE","SKELETON"}) {
            try { EntityType et = EntityType.valueOf(t); if (et.isAlive()) return et; } catch (Throwable ignored) {}
        }
        return EntityType.ZOMBIE;
    }

    private void abilityShockwave(double power) {
        double radius = plugin.getConfigUtil().getDouble("endgame.boss.shockwave.radius", 8.0);
        double dmg = Math.max(5.0, 5.0 + Math.log1p(power)*2.5);
        Location c = boss.getLocation();
        try { boss.getWorld().playSound(c, org.bukkit.Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f);} catch (Throwable ignored) {}
        try {
            int pts = 48;
            for (int i=0;i<pts;i++) {
                double a = 2*Math.PI*i/pts;
                double x = c.getX()+Math.cos(a)*radius;
                double z = c.getZ()+Math.sin(a)*radius;
                boss.getWorld().spawnParticle(Particle.CRIT, new Location(boss.getWorld(), x, c.getY()+0.2, z), 1,0.02,0.02,0.02,0.0);
            }
        } catch (Throwable ignored) {}
        for (Player pl : boss.getWorld().getPlayers()) {
            double d2 = pl.getLocation().distanceSquared(c);
            if (d2 <= radius*radius) {
                try { pl.damage(dmg); } catch (Throwable ignored) {}
                try { org.bukkit.util.Vector knock = pl.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.0).setY(0.6); pl.setVelocity(knock);} catch (Throwable ignored) {}
            }
        }
    }

    private void broadcastSpawn() {
        Bukkit.broadcast(Component.text("§cEin apokalyptischer Endboss ist erschienen!"));
        for (Player p : Bukkit.getOnlinePlayers()) try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.8f);} catch (Throwable ignored) {}
    }

    private void broadcastDefeat() {
        Bukkit.broadcast(Component.text("§aDer Endboss wurde besiegt!"));
    }
}
