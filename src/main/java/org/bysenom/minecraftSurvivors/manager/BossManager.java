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
import org.bukkit.entity.Creature;
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
    private final java.util.Set<java.util.UUID> meteorEntities = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private java.util.UUID nameStandId = null; // ArmorStand für sichtbaren Namen über dem Boss

    private int abilityTick = 0; // counts ticks for ability cadence
    private long nameStandLastUpdateTick = 0L;
    private double nameStandLastHp = -1.0;
    private String nameStandLastText = null;

    // Phasensteuerung (P1: >66%, P2: >33%, P3: <=33%)
    private enum Phase {P1, P2, P3}

    private Phase phase = Phase.P1;

    private BukkitTask nameStandFollowTask = null; // 1-Tick-Follow für glattes Nachführen

    public BossManager(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    public synchronized boolean isBossActive() {
        return boss != null && boss.isValid() && !boss.isDead();
    }

    public synchronized void tick() {
        if (plugin.getGameManager() == null || plugin.getGameManager().getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING)
            return;
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
        World w = loc.getWorld();
        if (w == null) return;
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
        } catch (Throwable ignored) {
        }
        try {
            AttributeInstance spd = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (spd != null)
                spd.setBaseValue(spd.getBaseValue() * plugin.getConfigUtil().getDouble("endgame.boss.speed-mult", 1.15));
            AttributeInstance dmg = le.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dmg != null) {
                double dmgBaseCfg = plugin.getConfigUtil().getDouble("endgame.boss.damage-base", 6.0);
                double dmgScale = plugin.getConfigUtil().getDouble("endgame.boss.damage-scale-per-minute", 1.2);
                double newDmg = Math.max(dmgBaseCfg, dmgBaseCfg + minutes * dmgScale);
                dmg.setBaseValue(newDmg);
            }
        } catch (Throwable ignored) {
        }
        try {
            String name = plugin.getConfigUtil().getString("endgame.boss.display-name", "APOKALYPTISCHER ENDBOSS");
            le.customName(Component.text(name).color(NamedTextColor.RED));
            // Hide the mob's default name label; we show visible name+phase on the ArmorStand passenger
            try { le.setCustomNameVisible(false); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
        }
        this.boss = le;
        // ensure boss immediately has a target to avoid AI idle
        try { ensureBossHasTarget(); } catch (Throwable ignored) {}
        try {
            le.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ms_boss_tag"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable ignored) {
        }
        // Optional: separater Namens-ArmorStand über dem Boss
        try {
            if (plugin.getConfigUtil().getBoolean("endgame.boss.name-armorstand.enabled", true))
                spawnOrUpdateNameStand();
        } catch (Throwable ignored) {
        }
        abilityTick = 0;
        phase = Phase.P1;
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
        try {
            et = EntityType.valueOf(upper);
        } catch (Throwable ignored) {
        }
        if (et == null || !et.isAlive()) {
            // Fallback Kette
            for (String fallback : new String[]{"WARDEN", "ENDER_DRAGON", "IRON_GOLEM", "GIANT", "ZOMBIE"}) {
                try {
                    EntityType f = EntityType.valueOf(fallback);
                    if (f.isAlive()) {
                        et = f;
                        break;
                    }
                } catch (Throwable ignored) {
                }
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
        if (!isBossActive()) {
            clearUi();
            return;
        }
        updateUi();
        try {
            updatePhaseFromHp();
        } catch (Throwable ignored) {
        }
        // NameStand-Content kann seltener aktualisiert werden; Position übernimmt der 1-Tick-Follow-Task
        try {
            updateNameStandContent();
        } catch (Throwable ignored) {
        }
        // Ensure boss targets a player (avoid AI ignoring players)
        try {
            ensureBossHasTarget();
        } catch (Throwable ignored) {}
        double power = spawnManager.getEnemyPowerIndex();
        double baseRadius = plugin.getConfigUtil().getDouble("endgame.boss.aura.base-radius", 4.0);
        double auraScale = plugin.getConfigUtil().getDouble("endgame.boss.aura.power-scale", 1.5);
        double phaseAuraMult;
        switch (phase) {
            case P1: phaseAuraMult = 1.0; break;
            case P2: phaseAuraMult = 1.15; break;
            case P3: phaseAuraMult = 1.3; break;
            default: phaseAuraMult = 1.0; break;
        }
        double radius = Math.min(plugin.getConfigUtil().getDouble("endgame.boss.aura.max-radius", 12.0), (baseRadius + Math.log1p(power) * auraScale) * phaseAuraMult);
        if (boss.getWorld() == null) return;
        double auraDmgBase = plugin.getConfigUtil().getDouble("endgame.boss.aura.damage-base", 2.0);
        double auraDmgScale = plugin.getConfigUtil().getDouble("endgame.boss.aura.damage-log-scale", 1.0);
        double auraDmg = Math.max(auraDmgBase, auraDmgBase + Math.log1p(power) * auraDmgScale) * (phase == Phase.P3 ? 1.25 : (phase == Phase.P2 ? 1.1 : 1.0));
        for (Player pl : boss.getWorld().getPlayers()) {
            if (!pl.isOnline()) continue;
            if (pl.getLocation().distanceSquared(boss.getLocation()) <= radius * radius) {
                try {
                    pl.damage(auraDmg);
                } catch (Throwable ignored) {
                }
                try {
                    pl.playSound(pl.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.9f);
                } catch (Throwable ignored) {
                }
            }
        }
        // Partikel-Ring (neutraler Rauch/Endrod Mix)
        try {
            int points = Math.max(24, (int) Math.round(2 * Math.PI * radius));
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                double x = boss.getLocation().getX() + Math.cos(a) * radius;
                double z = boss.getLocation().getZ() + Math.sin(a) * radius;
                boss.getWorld().spawnParticle(Particle.LARGE_SMOKE, new Location(boss.getWorld(), x, boss.getLocation().getY() + 0.2, z), 1, 0.02, 0.02, 0.02, 0.0);
                if (i % 3 == 0)
                    boss.getWorld().spawnParticle(Particle.END_ROD, new Location(boss.getWorld(), x, boss.getLocation().getY() + 0.25, z), 1, 0.01, 0.01, 0.01, 0.0);
            }
        } catch (Throwable ignored) {
        }
        // Fähigkeitencadence
        abilityTick += 10; // tickBoss läuft alle 10 Ticks
        int baseInterval = Math.max(40, plugin.getConfigUtil().getInt("endgame.boss.ability.interval-ticks", 120));
        double phaseMul;
        switch (phase) {
            case P1: phaseMul = 1.0; break;
            case P2: phaseMul = 0.8; break;
            case P3: phaseMul = 0.6; break;
            default: phaseMul = 1.0; break;
        }
        int abilityInterval = Math.max(30, (int) Math.round(baseInterval * phaseMul));
        if (abilityTick >= abilityInterval) {
            abilityTick = 0;
            performRandomAbility(power);
        }
        if (boss.isDead() || boss.getHealth() <= 0.0) {
            onBossDeath();
        }
    }

    private void updatePhaseFromHp() {
        if (!isBossActive()) return;
        double hp = Math.max(0.0, boss.getHealth());
        double max = boss.getAttribute(Attribute.MAX_HEALTH) != null ? Math.max(1.0, boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue()) : Math.max(1.0, hp);
        double ratio = hp / max;
        Phase newPhase = ratio > 0.66 ? Phase.P1 : (ratio > 0.33 ? Phase.P2 : Phase.P3);
        if (newPhase != phase) {
            phase = newPhase;
            onPhaseEnter(newPhase);
        }
    }

    private void onPhaseEnter(Phase ph) {
        if (!isBossActive()) return;
        switch (ph) {
            case P2:
                broadcastPhase("§6Phase II: Der Boss wird wütender – schnellere Fähigkeiten, grössere Aura!");
                // read phase multipliers from config for easier balancing
                double p2Speed = plugin.getConfigUtil().getDouble("endgame.boss.phase.p2.speed-mult", 1.08);
                double p2Damage = plugin.getConfigUtil().getDouble("endgame.boss.phase.p2.damage-mult", 1.10);
                tryPhaseBuffs(p2Speed, p2Damage);
                // Kleiner Minion-Impuls
                abilitySummonMinions(spawnManager.getEnemyPowerIndex());
                ringEffect(org.bukkit.Particle.SOUL_FIRE_FLAME, 10, 7.0, 0.3f);
                break;
            case P3:
                broadcastPhase("§cPhase III: Tödliche Raserei – Vorsicht vor Meteorschauern!");
                double p3Speed = plugin.getConfigUtil().getDouble("endgame.boss.phase.p3.speed-mult", 1.15);
                double p3Damage = plugin.getConfigUtil().getDouble("endgame.boss.phase.p3.damage-mult", 1.25);
                tryPhaseBuffs(p3Speed, p3Damage);
                // Sofortige Schockwelle als Phase-Start
                abilityShockwave(spawnManager.getEnemyPowerIndex());
                ringEffect(org.bukkit.Particle.FLAME, 14, 8.5, 0.45f);
                break;
            case P1:
            default:
                broadcastPhase("§ePhase I: Der Kampf beginnt!");
                break;
        }
        try {
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.8f);
        } catch (Throwable ignored) {
        }
    }

    private void tryPhaseBuffs(double speedMult, double damageMult) {
        try {
            AttributeInstance spd = boss.getAttribute(Attribute.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(spd.getBaseValue() * speedMult);
        } catch (Throwable ignored) {
        }
        try {
            AttributeInstance dmg = boss.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * damageMult);
        } catch (Throwable ignored) {
        }
    }

    // === fehlende Fähigkeits- & Hilfs-Methoden (wiederhergestellt) ===
    private void performRandomAbility(double power) {
        if (!isBossActive()) return;
        java.util.List<String> enabled = new java.util.ArrayList<>();
        for (String k : new String[]{"meteor", "lightning_chain", "summon_minions", "shockwave"}) {
            if (plugin.getConfigUtil().getBoolean("endgame.boss.ability." + k, true)) enabled.add(k);
        }
        // In Phase 3: Meteor-Schauer bevorzugen (falls aktiviert)
        if (phase == Phase.P3 && plugin.getConfigUtil().getBoolean("endgame.boss.ability.meteor_barrage", true)) {
            enabled.add("meteor_barrage");
            enabled.add("meteor_barrage"); // doppeltes Gewicht
        }
        if (enabled.isEmpty()) return;
        String pick = enabled.get(new java.util.Random().nextInt(enabled.size()));
        switch (pick) {
            case "meteor": abilityMeteor(power); break;
            case "lightning_chain": abilityLightningChain(power); break;
            case "summon_minions": abilitySummonMinions(power); break;
            case "shockwave": abilityShockwave(power); break;
            case "meteor_barrage": abilityMeteorBarrage(power); break;
        }
    }

    private void abilityMeteorBarrage(double power) {
        int count = Math.max(3, plugin.getConfigUtil().getInt("endgame.boss.meteor_barrage.count", 5));
        double spread = plugin.getConfigUtil().getDouble("endgame.boss.meteor_barrage.spread", 6.0);
        java.util.List<Location> targets = new java.util.ArrayList<>();
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (Player pl : boss.getWorld().getPlayers()) if (pl.isOnline()) players.add(pl);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < count; i++) {
            Location base;
            if (!players.isEmpty() && r.nextBoolean()) {
                Player p = players.get(r.nextInt(players.size()));
                base = p.getLocation().clone();
            } else {
                base = boss.getLocation().clone();
            }
            Location strike = base.add((r.nextDouble() - 0.5) * spread, 0, (r.nextDouble() - 0.5) * spread);
            targets.add(strike);
        }
        double dmg = Math.max(7.0, 7.0 + Math.log1p(power) * 3.5);
        int delayStep = 10; // 0.5s zwischen Einschlägen
        for (int i = 0; i < targets.size(); i++) {
            final Location strike = targets.get(i).clone();
            try {
                int pts = 28;
                double warnR = 2.2;
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(strike.getWorld(), strike.clone().add(0, 0.1, 0), warnR, pts, Particle.ASH);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(strike.getWorld(), strike.clone().add(0, 0.1, 0), warnR, pts, Particle.CRIT);
                }, Math.max(1L, delayStep / 2L));
            } catch (Throwable ignored) {}
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    org.bukkit.entity.FallingBlock fb = strike.getWorld().spawn(strike.clone().add(0, 12, 0), org.bukkit.entity.FallingBlock.class, f -> {
                        f.setBlockData(org.bukkit.Material.MAGMA_BLOCK.createBlockData());
                        f.setDropItem(false);
                        f.setHurtEntities(false);
                        f.setGravity(true);
                    });
                    try { fb.setMetadata("ms_boss_meteor", new org.bukkit.metadata.FixedMetadataValue(plugin, true)); } catch (Throwable ignored) {}
                    meteorEntities.add(fb.getUniqueId());
                    fb.setVelocity(new org.bukkit.util.Vector(0, -1.2, 0));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try { fb.remove(); } catch (Throwable ignored) {}
                        meteorEntities.remove(fb.getUniqueId());
                    }, 30L);
                } catch (Throwable ignored) {}
                try { strike.getWorld().playSound(strike, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f); } catch (Throwable ignored) {}
                try { strike.getWorld().spawnParticle(Particle.EXPLOSION, strike.clone().add(0, 0.6, 0), 1); } catch (Throwable ignored) {}
                for (Player pl : strike.getWorld().getPlayers()) {
                    if (pl.getLocation().distanceSquared(strike) <= 5.5) {
                        try { pl.damage(dmg); } catch (Throwable ignored) {}
                        try { pl.setVelocity(pl.getLocation().toVector().subtract(strike.toVector()).normalize().multiply(1.0).setY(0.5)); } catch (Throwable ignored) {}
                    }
                }
            }, (long) (i * delayStep));
        }
        try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.6f); } catch (Throwable ignored) {}
    }

    private void abilityMeteor(double power) {
        Player target = boss.getWorld().getPlayers().stream().filter(Player::isOnline).max(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(boss.getLocation()))).orElse(null);
        if (target == null) return;
        Location strike = target.getLocation().clone();
        try { strike.getWorld().playSound(strike, org.bukkit.Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.6f); } catch (Throwable ignored) {}
        try {
            int pts = 32;
            double warnR = 2.5;
            org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(strike.getWorld(), strike.clone().add(0, 0.1, 0), warnR, pts, Particle.END_ROD);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(strike.getWorld(), strike.clone().add(0, 0.1, 0), warnR, pts, Particle.CRIT);
                try { strike.getWorld().playSound(strike, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.9f); } catch (Throwable ignored) {}
            }, 20L);
        } catch (Throwable ignored) {}
        double dmg = Math.max(8.0, 8.0 + Math.log1p(power) * 4.0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                org.bukkit.entity.FallingBlock fb = strike.getWorld().spawn(strike.clone().add(0, 14, 0), org.bukkit.entity.FallingBlock.class, f -> {
                    f.setBlockData(org.bukkit.Material.MAGMA_BLOCK.createBlockData());
                    f.setDropItem(false);
                    f.setHurtEntities(false);
                    f.setGravity(true);
                });
                try { fb.setMetadata("ms_boss_meteor", new org.bukkit.metadata.FixedMetadataValue(plugin, true)); } catch (Throwable ignored) {}
                meteorEntities.add(fb.getUniqueId());
                fb.setVelocity(new org.bukkit.util.Vector(0, -1.25, 0));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try { fb.remove(); } catch (Throwable ignored) {}
                    meteorEntities.remove(fb.getUniqueId());
                }, 40L);
            } catch (Throwable ignored) {}
            try { strike.getWorld().playSound(strike, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f); } catch (Throwable ignored) {}
            try { strike.getWorld().spawnParticle(Particle.EXPLOSION, strike.clone().add(0, 0.6, 0), 1); } catch (Throwable ignored) {}
            for (Player pl : strike.getWorld().getPlayers()) {
                if (pl.getLocation().distanceSquared(strike) <= 6.25) {
                    try { pl.damage(dmg); } catch (Throwable ignored) {}
                    try { pl.setVelocity(pl.getLocation().toVector().subtract(strike.toVector()).normalize().multiply(1.2).setY(0.6)); } catch (Throwable ignored) {}
                }
            }
        }, 40L);
    }

    private void abilityLightningChain(double power) {
        java.util.List<Player> plist = new java.util.ArrayList<>();
        for (Player p : boss.getWorld().getPlayers()) if (p.isOnline()) plist.add(p);
        if (plist.isEmpty()) return;
        Player start = plist.get(new java.util.Random().nextInt(plist.size()));
        double chainRange = plugin.getConfigUtil().getDouble("endgame.boss.lightning.chain-range", 12.0);
        int maxChains = plugin.getConfigUtil().getInt("endgame.boss.lightning.max-chains", 4);
        double dmg = Math.max(6.0, 6.0 + Math.log1p(power) * 3.0);
        java.util.Set<Player> hit = new java.util.HashSet<>();
        Player current = start;
        for (int c = 0; c < maxChains && current != null; c++) {
            strikeLightningSafe(current, dmg);
            hit.add(current);
            Player next = null;
            double best = Double.MAX_VALUE;
            for (Player pl : plist) {
                if (hit.contains(pl)) continue;
                double d2 = pl.getLocation().distanceSquared(current.getLocation());
                if (d2 <= chainRange * chainRange && d2 < best) { best = d2; next = pl; }
            }
            if (next != null) try { drawBeam(current.getLocation().add(0, 1.0, 0), next.getLocation().add(0, 1.0, 0), Particle.ELECTRIC_SPARK, 16); } catch (Throwable ignored) {}
            current = next;
        }
        try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.6f); } catch (Throwable ignored) {}
    }

    private void strikeLightningSafe(Player pl, double dmg) {
        try {
            Location loc = pl.getLocation();
            loc.getWorld().strikeLightningEffect(loc);
            pl.damage(dmg);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0, 1.0, 0), 14, 0.3, 0.3, 0.3, 0.02);
        } catch (Throwable ignored) {}
    }

    private void abilitySummonMinions(double power) {
        int count = plugin.getConfigUtil().getInt("endgame.boss.minions.count", 4);
        double spread = plugin.getConfigUtil().getDouble("endgame.boss.minions.spread", 6.0);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < count; i++) {
            Location base = boss.getLocation().clone().add((r.nextDouble() - 0.5) * spread, 0, (r.nextDouble() - 0.5) * spread);
            try { base.setY(boss.getWorld().getHighestBlockYAt(base) + 1); } catch (Throwable ignored) {}
            EntityType type = pickMinionType();
            LivingEntity mob;
            try { mob = (LivingEntity) boss.getWorld().spawnEntity(base, type); } catch (Throwable ex) { mob = (LivingEntity) boss.getWorld().spawnEntity(base, EntityType.ZOMBIE); }
            try { mob.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ms_wave"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1); } catch (Throwable ignored) {}
            try { mob.customName(Component.text("§6Minion").color(NamedTextColor.GOLD)); mob.setCustomNameVisible(true); } catch (Throwable ignored) {}
            try { AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH); if (hp != null) { double newHp = Math.max(1.0, hp.getBaseValue() * (1.2 + Math.log1p(power) * 0.3)); hp.setBaseValue(newHp); mob.setHealth(newHp); } } catch (Throwable ignored) {}
            try { AttributeInstance dmg = mob.getAttribute(Attribute.ATTACK_DAMAGE); if (dmg != null) dmg.setBaseValue(Math.max(3.0, dmg.getBaseValue() * (1.0 + Math.log1p(power) * 0.4))); } catch (Throwable ignored) {}
            try { boss.getWorld().spawnParticle(Particle.PORTAL, base, 24, 0.4, 0.4, 0.4, 0.1); } catch (Throwable ignored) {}
        }
        try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.6f); } catch (Throwable ignored) {}
    }

    private EntityType pickMinionType() {
        String cfg = plugin.getConfigUtil().getString("endgame.boss.minions.type", "ZOMBIE");
        try { EntityType et = EntityType.valueOf(cfg.toUpperCase()); if (et.isAlive()) return et; } catch (Throwable ignored) {}
        for (String t : new String[]{"HUSK", "DROWNED", "ZOMBIE", "SKELETON"}) {
            try { EntityType et = EntityType.valueOf(t); if (et.isAlive()) return et; } catch (Throwable ignored) {}
        }
        return EntityType.ZOMBIE;
    }

    private void abilityShockwave(double power) {
        double radius = plugin.getConfigUtil().getDouble("endgame.boss.shockwave.radius", 8.0);
        double dmg = Math.max(5.0, 5.0 + Math.log1p(power) * 2.5);
        Location c = boss.getLocation();
        try { boss.getWorld().playSound(c, org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.6f); } catch (Throwable ignored) {}
        for (int t = 0; t < 20; t += 5) {
            final int tt = t;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(c.getWorld(), c.clone().add(0, 0.2, 0), radius, 48, org.bukkit.Particle.CRIT); c.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, c.clone().add(0, 0.3, 0), 6, 0.2, 0.2, 0.2, 0.01); } catch (Throwable ignored2) {}
            }, tt);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { boss.getWorld().playSound(c, org.bukkit.Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f); } catch (Throwable ignored) {}
            try {
                int pts = 48;
                for (int i = 0; i < pts; i++) {
                    double a = 2 * Math.PI * i / pts;
                    double x = c.getX() + Math.cos(a) * radius;
                    double z = c.getZ() + Math.sin(a) * radius;
                    boss.getWorld().spawnParticle(Particle.CRIT, new Location(boss.getWorld(), x, c.getY() + 0.2, z), 1, 0.02, 0.02, 0.02, 0.0);
                }
            } catch (Throwable ignored) {}
            for (Player pl : boss.getWorld().getPlayers()) {
                double d2 = pl.getLocation().distanceSquared(c);
                if (d2 <= radius * radius) {
                    try { pl.damage(dmg); } catch (Throwable ignored) {}
                    try { org.bukkit.util.Vector knock = pl.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.0).setY(0.6); pl.setVelocity(knock); } catch (Throwable ignored) {}
                }
            }
        }, 20L);
    }

    private void broadcastSpawn() {
        Bukkit.broadcast(Component.text("§cEin apokalyptischer Endboss ist erschienen!"));
        for (Player p : Bukkit.getOnlinePlayers()) try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.8f); } catch (Throwable ignored) {}
    }

    private void broadcastDefeat() {
        Bukkit.broadcast(Component.text("§aDer Endboss wurde besiegt!"));
    }

    private void broadcastPhase(String msg) {
        try { Bukkit.broadcast(Component.text(msg)); } catch (Throwable ignored) {}
    }

    private void drawBeam(Location a, Location b, Particle type, int steps) {
        try {
            if (a.getWorld() != b.getWorld()) return;
            steps = Math.max(4, steps);
            org.bukkit.util.Vector v = b.toVector().subtract(a.toVector());
            for (int i = 0; i <= steps; i++) {
                double f = i / (double) steps;
                org.bukkit.util.Vector p = a.toVector().add(v.clone().multiply(f));
                a.getWorld().spawnParticle(type, new Location(a.getWorld(), p.getX(), p.getY(), p.getZ()), 1, 0.01, 0.01, 0.01, 0.0);
            }
        } catch (Throwable ignored) {}
    }

    private void updateUi() {
        if (!isBossActive()) { clearUi(); return; }
        // Always hide any bossbar previously created by this manager; do not create new Adventure BossBars here.
        if (bossbar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try { p.hideBossBar(bossbar); } catch (Throwable ignored) {}
            }
            bossbar = null;
        }

        // Ensure the name ArmorStand exists and shows only name+phase
        try { if (plugin.getConfigUtil().getBoolean("endgame.boss.name-armorstand.enabled", true)) spawnOrUpdateNameStand(); } catch (Throwable ignored) {}
        try { updateNameStandContent(); } catch (Throwable ignored) {}

        boolean holoEnabled = false;
        try { holoEnabled = plugin.getConfigUtil().getBoolean("endgame.boss.hologram.enabled", false); } catch (Throwable ignored) {}
        double hp = boss.getHealth();
        double max = boss.getAttribute(Attribute.MAX_HEALTH) != null ? boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : hp;
        if (holoEnabled) {
            updateHpHologram(hp, max);
        } else {
            // Remove any lingering holograms if holograms are disabled
            updateHpHologramCleanup();
        }
    }

    // cleanup helper when holograms disabled
    private void updateHpHologramCleanup() {
        for (UUID id : new java.util.ArrayList<>(holograms)) {
            try { org.bukkit.entity.Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); } catch (Throwable ignored) {}
            holograms.remove(id);
        }
    }

    // New: per-boss HP progress TextDisplay
    private void updateHpHologram(double hp, double max) {
        if (!isBossActive() || boss.getWorld() == null) return;
        try {
            // Compose a short progress bar string (e.g. ███░░ style)
            int len = Math.max(10, plugin.getConfigUtil().getInt("endgame.boss.hud.bar-length", 14));
            double ratio = Math.max(0.0, Math.min(1.0, hp / Math.max(1.0, max)));
            StringBuilder bar = new StringBuilder();
            int filled = (int) Math.round(ratio * len);
            for (int i = 0; i < len; i++) {
                if (i < filled) bar.append('█');
                else bar.append('░');
            }
            String text = "§c" + bar.toString() + " §7" + (int) Math.round(ratio * 100) + "%";
            // find existing TextDisplay in holograms, reuse first one
            org.bukkit.entity.TextDisplay td = null;
            for (UUID id : holograms) {
                org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                if (e instanceof org.bukkit.entity.TextDisplay) {
                    td = (org.bukkit.entity.TextDisplay) e;
                    break;
                }
            }
            // position: slightly below name stand so name stays top; compute offsets
            double nameY = computeNameStandYOffset();
            double hpY = nameY - 0.45; // HP bar sits a bit below the name
            org.bukkit.Location at = boss.getLocation().clone().add(0, hpY, 0);
            if (td == null) {
                td = boss.getWorld().spawn(at, org.bukkit.entity.TextDisplay.class, t -> {
                    try {
                        t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                    } catch (Throwable ignored) {
                    }
                    try {
                        t.setShadowed(false);
                    } catch (Throwable ignored) {
                    }
                    t.text(net.kyori.adventure.text.Component.text(text));
                    t.setSeeThrough(true);
                    t.setPersistent(true);
                    // brightness API differs between server versions; keep default
                });
                holograms.add(td.getUniqueId());
            } else {
                td.text(net.kyori.adventure.text.Component.text(text));
                try {
                    td.teleport(at);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void clearUi() {
        if (bossbar != null) {
            for (Player p : Bukkit.getOnlinePlayers())
                try {
                    p.hideBossBar(bossbar);
                } catch (Throwable ignored) {
                }
            bossbar = null;
        }
        // Holograms entfernen
        for (UUID id : holograms) {
            try {
                org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            } catch (Throwable ignored) {
            }
        }
        holograms.clear();
        // Namens-ArmorStand entfernen
        if (nameStandId != null) {
            try {
                org.bukkit.entity.Entity e = Bukkit.getEntity(nameStandId);
                if (e != null) e.remove();
            } catch (Throwable ignored) {
            }
            nameStandId = null;
        }
        // Follow-Task stoppen
        if (nameStandFollowTask != null) {
            try {
                nameStandFollowTask.cancel();
            } catch (Throwable ignored) {
            }
            nameStandFollowTask = null;
        }
        // Stoppe den Boss-Management-Task, falls aktiv
        try {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            task = null;
        } catch (Throwable ignored) {}
        cleanupProjectiles();
    }

    private void cleanupProjectiles() {
        // iterate over a snapshot to avoid ConcurrentModification
        for (java.util.UUID id : new java.util.HashSet<>(meteorEntities)) {
            try {
                org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(id);
                if (e != null) {
                    // If it's a FallingBlock, try to ensure no block remains at its last location
                    try {
                        if (e instanceof org.bukkit.entity.FallingBlock) {
                            org.bukkit.entity.FallingBlock fb = (org.bukkit.entity.FallingBlock) e;
                            org.bukkit.Location loc = fb.getLocation();
                            e.remove();
                            // Defensive: set block at integer coordinates to AIR to avoid leftover blocks
                            try {
                                org.bukkit.block.Block b = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                                if (b.getType() != org.bukkit.Material.AIR) {
                                    b.setType(org.bukkit.Material.AIR, false);
                                }
                            } catch (Throwable ignored2) {
                            }
                        } else {
                            e.remove();
                        }
                    } catch (Throwable ignored2) {
                    }
                }
            } catch (Throwable ignored) {
            }
            meteorEntities.remove(id);
        }
    }

    // ===== Namens-ArmorStand (sichtbarer Name über dem Boss) =====
    private double computeNameStandYOffset() {
        double baseExtra = 0.9; // etwas höherer Standard-Abstand (erhöht auf 0.9)
        try {
            baseExtra = plugin.getConfigUtil().getDouble("endgame.boss.name-armorstand.extra-offset", 0.9);
        } catch (Throwable ignored) {
        }
        double dyn = 0.0;
        boolean dynEnabled = true;
        try {
            dynEnabled = plugin.getConfigUtil().getBoolean("endgame.boss.name-armorstand.dynamic-height.enabled", true);
        } catch (Throwable ignored) {
        }
        if (dynEnabled && boss != null) {
            try {
                // Versuche echte Höhe; fallback auf BoundingBox; sonst Standard 2.2
                double h = boss.getHeight();
                if (!Double.isFinite(h) || h <= 0.5) {
                    try {
                        org.bukkit.util.BoundingBox bb = boss.getBoundingBox();
                        h = bb.getHeight();
                    } catch (Throwable ignored2) {
                    }
                }
                if (Double.isFinite(h) && h > 0.5) dyn = h;
            } catch (Throwable ignored) {
            }
        }
        double manualOffset = 2.2; // legacy fallback
        try {
            manualOffset = plugin.getConfigUtil().getDouble("endgame.boss.name-armorstand.y-offset", 2.2);
        } catch (Throwable ignored) {
        }
        // Wenn dynamisch aktiv und dyn sinnvoll: dyn + baseExtra; sonst manueller Offset
        return (dyn > 0.5 ? dyn + baseExtra : manualOffset);
    }

    private void spawnOrUpdateNameStand() {
        if (!isBossActive() || boss.getWorld() == null) return;
        org.bukkit.entity.ArmorStand as = null;
        if (nameStandId != null) {
            org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(nameStandId);
            if (e instanceof org.bukkit.entity.ArmorStand) as = (org.bukkit.entity.ArmorStand) e;
        }
        if (as == null || !as.isValid() || as.getWorld() != boss.getWorld()) {
            double yOff = computeNameStandYOffset();
            org.bukkit.Location base = boss.getLocation().clone().add(0, yOff, 0);
            as = boss.getWorld().spawn(base, org.bukkit.entity.ArmorStand.class, a -> {
                try { a.setMarker(true); } catch (Throwable ignored) {}
                try { a.setInvisible(true); } catch (Throwable ignored) {}
                try { a.setGravity(false); } catch (Throwable ignored) {}
                try { a.setSmall(true); } catch (Throwable ignored) {}
                try { a.setSilent(true); } catch (Throwable ignored) {}
                try { a.setInvulnerable(true); } catch (Throwable ignored) {}
                try { a.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ms_boss_name"), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);} catch (Throwable ignored) {}
            });
            nameStandId = as.getUniqueId();
        }
        // Do NOT mount the ArmorStand as passenger (passenger offsets cause the label to shift during attack animations).
        // We instead keep it separate and teleport it every tick to the desired offset for smooth, deterministic positioning.
        try { if (as != null && as.isValid()) {
                double yOff = computeNameStandYOffset();
                try { as.teleport(boss.getLocation().clone().add(0, yOff, 0)); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        ensureNameStandFollowTask();
        nameStandLastText = null; // force first build
        updateNameStandContent();
    }

    private void ensureNameStandFollowTask() {
        try {
            if (nameStandFollowTask != null && !nameStandFollowTask.isCancelled()) return;
            nameStandFollowTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                try { updateNameStandPosition(); } catch (Throwable ignored) {}
                if (!isBossActive()) {
                    try { nameStandFollowTask.cancel(); } catch (Throwable ignored) {}
                    nameStandFollowTask = null;
                }
            }, 0L, 1L);
        } catch (Throwable ignored) {}
    }

    private void updateNameStandPosition() {
        if (!isBossActive() || boss.getWorld() == null || nameStandId == null) return;
        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(nameStandId);
        if (!(e instanceof org.bukkit.entity.ArmorStand as)) return;
        // Always teleport the ArmorStand to the desired offset (1-tick update -> smooth and no passenger jitter)
        double yOff = computeNameStandYOffset();
        org.bukkit.Location target = boss.getLocation().clone().add(0, yOff, 0);
        try {
            // teleport every tick for deterministic positioning; server-side teleport is cheap for a single entity
            as.teleport(target);
        } catch (Throwable ignored) {}
    }

    private void updateNameStandContent() {
        if (!isBossActive() || nameStandId == null) return;
        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(nameStandId);
        if (!(e instanceof org.bukkit.entity.ArmorStand)) return;
        org.bukkit.entity.ArmorStand as = (org.bukkit.entity.ArmorStand) e;
        boolean showHp = false, showPhase = true;
        int intervalTicks = 20;
        try { showHp = plugin.getConfigUtil().getBoolean("endgame.boss.name-armorstand.show-hp", false); } catch (Throwable ignored) {}
        try { showPhase = plugin.getConfigUtil().getBoolean("endgame.boss.name-armorstand.show-phase", true); } catch (Throwable ignored) {}
        try { intervalTicks = Math.max(1, plugin.getConfigUtil().getInt("endgame.boss.name-armorstand.update-interval-ticks", 20)); } catch (Throwable ignored) {}
        long logicalTick = abilityTick;
        if (logicalTick - nameStandLastUpdateTick < intervalTicks) {
            double hpNow = boss.getHealth();
            if (Math.abs(hpNow - nameStandLastHp) < 0.01) return;
        }
        String baseName = null;
        try { String cfgName = plugin.getConfigUtil().getString("endgame.boss.display-name", null); if (cfgName != null && !cfgName.isBlank()) baseName = cfgName; } catch (Throwable ignored) {}
        if (baseName == null && boss.customName() != null) {
            try { baseName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(boss.customName()); } catch (Throwable ignored) {
            }
        }
        if (baseName == null || baseName.isBlank()) baseName = "BOSS";
        double hp = boss.getHealth();
        double max = hp;
        try { AttributeInstance m = boss.getAttribute(Attribute.MAX_HEALTH); if (m != null) max = Math.max(1.0, m.getBaseValue()); } catch (Throwable ignored) {
        }
        StringBuilder sb = new StringBuilder(baseName);
        if (showPhase) {
            String ph;
            switch (phase) {
                case P1: ph = "I"; break;
                case P2: ph = "II"; break;
                case P3: ph = "III"; break;
                default: ph = "I"; break;
            }
            sb.append(" §7[").append(ph).append("]");
        }
        if (showHp) {
            double pct = Math.max(0.0, Math.min(1.0, hp / Math.max(1.0, max)));
            sb.append(" §c").append((int) Math.round(hp)).append("§7/").append((int) Math.round(max)).append(" (§c").append((int) Math.round(pct * 100)).append("%§7)");
        }
        String text = sb.toString();
        if (text.equals(nameStandLastText)) return;
        nameStandLastText = text;
        nameStandLastHp = hp;
        nameStandLastUpdateTick = logicalTick;
        try {
            as.customName(net.kyori.adventure.text.Component.text(text).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            as.setCustomNameVisible(true);
        } catch (Throwable ignored) {
        }
    }

    private void onBossDeath() {
        try {
            if (boss != null && boss.getWorld() != null) {
                try { boss.getWorld().dropItemNaturally(boss.getLocation(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR, 1)); } catch (Throwable ignored) {}
                try { boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        broadcastDefeat();
        clearUi();
        boss = null;
    }

    public void forceEnd() {
        try {
            if (boss != null) {
                try { boss.remove(); } catch (Throwable ignored) {}
                boss = null;
            }
        } catch (Throwable ignored) {}
        try { clearUi(); } catch (Throwable ignored) {}
    }

    // Public debug API: spawn the boss immediately for testing (no enrage check)
    public synchronized void debugSpawnBoss() {
        try {
            if (isBossActive()) return;
            trySpawnBoss();
        } catch (Throwable t) {
            try { plugin.getLogger().warning("debugSpawnBoss failed: " + t.getMessage()); } catch (Throwable ignored) {}
        }
    }

    public synchronized org.bukkit.entity.LivingEntity getBossEntity() {
        return isBossActive() ? boss : null;
    }

    // Ensure the boss (if a Creature) has a valid target; pick nearest player within configured range
    private void ensureBossHasTarget() {
        if (!isBossActive() || boss == null || boss.getWorld() == null) return;
        if (!(boss instanceof Creature)) return;
        Creature c = (Creature) boss;
        org.bukkit.entity.LivingEntity cur = c.getTarget();
        if (cur != null && cur.isValid()) return; // already has a valid target
        double range = 40.0;
        try { range = plugin.getConfigUtil().getDouble("endgame.boss.target-range", 40.0); } catch (Throwable ignored) {}
        double best = Double.MAX_VALUE;
        Player bestP = null;
        for (Player p : boss.getWorld().getPlayers()) {
            if (!p.isOnline()) continue;
            double d2 = p.getLocation().distanceSquared(boss.getLocation());
            if (d2 <= range * range && d2 < best) { best = d2; bestP = p; }
        }
        if (bestP != null) {
            try { c.setTarget(bestP); } catch (Throwable ignored) {}
        }
    }

    // Public debug API: force boss to aggro nearest player now
    public synchronized void debugAggroNearest() {
        try { ensureBossHasTarget(); } catch (Throwable t) { try { plugin.getLogger().warning("debugAggroNearest failed: " + t.getMessage()); } catch (Throwable ignored) {} }
    }

    private void ringEffect(Particle type, int points, double radius, float pitch) {
        try {
            Location c = boss.getLocation();
            for (int i = 0; i < Math.max(8, points); i++) {
                double a = 2 * Math.PI * i / points;
                double x = c.getX() + Math.cos(a) * radius;
                double z = c.getZ() + Math.sin(a) * radius;
                boss.getWorld().spawnParticle(type, new Location(boss.getWorld(), x, c.getY() + 0.3, z), 1, 0.02, 0.02, 0.02, 0.0);
            }
            try { boss.getWorld().playSound(c, org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, pitch); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

}
