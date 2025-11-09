package org.bysenom.minecraftSurvivors.manager;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * SpawnManager: Spawnt Wellen und verwaltet das "Einfrieren" von Mobs ausschließlich über AI-Deaktivierung.
 * Zusätzlich: Continuous-Spawn (Vampire Survivors Style) mit Gewichtungen & Scaling.
 */
public class SpawnManager {

    private final MinecraftSurvivors plugin;
    private final NamespacedKey waveKey;
    private final org.bukkit.NamespacedKey eliteKey; // mark as final
    private final Random random = new Random();

    // Map: playerUuid -> Set gefrorener Entity-UUIDs (für diesen Spieler eingefroren)
    private final Map<UUID, Set<UUID>> frozenByPlayer = new ConcurrentHashMap<>();
    private BukkitTask freezeEnforcerTask;

    // Continuous spawn state
    private BukkitTask continuousTask;
    private long continuousStartMillis = 0L;
    private final Map<UUID, Double> spawnAccumulator = new ConcurrentHashMap<>();
    private long restPhaseUntilMillis = 0L;

    private static final org.bukkit.NamespacedKey MAX_HEALTH = org.bukkit.NamespacedKey.minecraft("generic.max_health");
    private static final org.bukkit.NamespacedKey MOVEMENT_SPEED = org.bukkit.NamespacedKey.minecraft("generic.movement_speed");
    private static final org.bukkit.NamespacedKey ATTACK_DAMAGE = org.bukkit.NamespacedKey.minecraft("generic.attack_damage");
    private static final org.bukkit.NamespacedKey FOLLOW_RANGE = org.bukkit.NamespacedKey.minecraft("generic.follow_range");

    // PDC Keys für Baseline-Attribute
    private static final org.bukkit.NamespacedKey BASE_MAX_HP = new org.bukkit.NamespacedKey("minecraftsurvivors","base_max_hp");
    private static final org.bukkit.NamespacedKey BASE_SPEED = new org.bukkit.NamespacedKey("minecraftsurvivors","base_speed");
    private static final org.bukkit.NamespacedKey BASE_DAMAGE = new org.bukkit.NamespacedKey("minecraftsurvivors","base_damage");

    public SpawnManager(MinecraftSurvivors plugin, @SuppressWarnings("unused") PlayerManager playerManager) {
        this.plugin = plugin;
        this.waveKey = new NamespacedKey(plugin, "ms_wave");
        this.eliteKey = new org.bukkit.NamespacedKey(plugin, "ms_elite");
    }

    private BukkitTask scalingTask;
    private BukkitTask aggroTask;

    private void ensureScalingTask() {
        if (scalingTask != null && !scalingTask.isCancelled()) return;
        int periodTicks = Math.max(20, plugin.getConfigUtil().getInt("scaling.update-interval-ticks", 40));
        scalingTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                if (plugin.getGameManager() == null || plugin.getGameManager().getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING) return;
                double minutes = getElapsedMinutes();
                for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                    for (org.bukkit.entity.Entity e : w.getEntities()) {
                        if (!(e instanceof org.bukkit.entity.LivingEntity)) continue;
                        if (!e.getPersistentDataContainer().has(waveKey, org.bukkit.persistence.PersistentDataType.BYTE)) continue;
                        applyScaling((org.bukkit.entity.LivingEntity) e, minutes);
                    }
                }
            } catch (Throwable ignored) {}
        }, periodTicks, periodTicks);
    }

    private void ensureAggroTask() {
        try {
            if (aggroTask != null && !aggroTask.isCancelled()) return;
            int every = Math.max(10, plugin.getConfigUtil().getInt("ai.aggro-tick-interval", 20));
            double followRangeCfg = Math.max(16.0, plugin.getConfigUtil().getDouble("ai.follow-range", 64.0));
            aggroTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                try {
                    if (plugin.getGameManager() == null || plugin.getGameManager().getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING) return;
                    for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                        java.util.List<org.bukkit.entity.Player> players = new java.util.ArrayList<>(w.getPlayers());
                        if (players.isEmpty()) continue;
                        for (org.bukkit.entity.Entity e : w.getEntities()) {
                            if (!(e instanceof org.bukkit.entity.Mob mob)) continue;
                            if (!e.getPersistentDataContainer().has(waveKey, org.bukkit.persistence.PersistentDataType.BYTE)) continue;
                            try {
                                org.bukkit.attribute.Attribute frAttr = org.bukkit.Registry.ATTRIBUTE.get(FOLLOW_RANGE);
                                if (frAttr != null) {
                                    org.bukkit.attribute.AttributeInstance fr = mob.getAttribute(frAttr);
                                    if (fr != null && fr.getBaseValue() < followRangeCfg) fr.setBaseValue(followRangeCfg);
                                }
                            } catch (Throwable ignored) {}
                            org.bukkit.entity.Player nearest = null;
                            double best = Double.MAX_VALUE;
                            for (org.bukkit.entity.Player p : players) {
                                double d2 = p.getLocation().distanceSquared(mob.getLocation());
                                if (d2 < best) { best = d2; nearest = p; }
                            }
                            if (nearest != null) {
                                try { mob.setTarget(nearest); } catch (Throwable ignored) {}
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }, every, every);
        } catch (Throwable ignored) {}
    }

    private void captureBaselineIfMissing(LivingEntity mob) {
        try {
            org.bukkit.persistence.PersistentDataContainer pdc = mob.getPersistentDataContainer();
            if (!pdc.has(BASE_MAX_HP, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                double v = 20.0;
                try { org.bukkit.attribute.AttributeInstance a = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH); if (a != null) v = a.getBaseValue(); } catch (Throwable ignored) {}
                pdc.set(BASE_MAX_HP, org.bukkit.persistence.PersistentDataType.DOUBLE, v);
            }
            if (!pdc.has(BASE_SPEED, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                double v = 0.25;
                try { org.bukkit.attribute.AttributeInstance a = mob.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED); if (a != null) v = a.getBaseValue(); } catch (Throwable ignored) {}
                pdc.set(BASE_SPEED, org.bukkit.persistence.PersistentDataType.DOUBLE, v);
            }
            if (!pdc.has(BASE_DAMAGE, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                double v = 3.0;
                try { org.bukkit.attribute.AttributeInstance a = mob.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE); if (a != null) v = a.getBaseValue(); } catch (Throwable ignored) {}
                pdc.set(BASE_DAMAGE, org.bukkit.persistence.PersistentDataType.DOUBLE, v);
            }
        } catch (Throwable ignored) {}
    }

    public void spawnWave(int waveNumber) {
        int base = plugin.getConfigUtil().getInt("wave.spawn-per-player-base", 1);
        double scale = plugin.getConfigUtil().getDouble("wave.scale-per-wave", 0.5);
        int perPlayer = Math.max(1, base + (int) Math.floor(waveNumber * scale));

        double minDist = plugin.getConfigUtil().getDouble("spawn.min-distance", 8.0);
        double maxDist = plugin.getConfigUtil().getDouble("spawn.max-distance", 16.0);
        boolean shouldGlow = plugin.getConfigUtil().getBoolean("spawn.glowing", true);
        int glowTicks = plugin.getConfigUtil().getInt("spawn.glowing-duration-ticks", 20 * 60 * 5);

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < perPlayer; i++) {
                Location spawnLoc = randomNearby(player.getLocation(), minDist, maxDist);
                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);

                // mark as wave mob
                mob.getPersistentDataContainer().set(waveKey, PersistentDataType.BYTE, (byte) 1);
                captureBaselineIfMissing(mob);

                if (shouldGlow) {
                    try {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Math.max(20, glowTicks), 0, false, false, false));
                    } catch (Throwable ignored) {}
                }

                // Wenn der Spieler lokal pausiert ist, friere diesen Mob für ihn ein
                try {
                    if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(player.getUniqueId())) {
                        freezeSingleMobForPlayer(player.getUniqueId(), mob);
                    }
                } catch (Throwable ignored) {}

                // Partikel-Animation beim Spawn
                playSpawnAnimation(spawnLoc);
            }
        }
        ensureScalingTask();
        ensureAggroTask();
    }

    public void clearWaveMobs() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!(e instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) e;
                if (!le.getPersistentDataContainer().has(waveKey, PersistentDataType.BYTE)) continue;
                try { le.remove(); } catch (Throwable ignored) {}
            }
        }
    }

    private Location randomNearby(Location base, double minRadius, double maxRadius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double dx = Math.cos(angle) * distance;
        double dz = Math.sin(angle) * distance;
        Location loc = base.clone().add(dx, 0, dz);
        try { loc.setY(base.getWorld().getHighestBlockYAt(loc) + 1); } catch (Throwable ignored) {}
        return loc;
    }

    private void playSpawnAnimation(Location center) {
        if (center == null || center.getWorld() == null) return;
        final org.bukkit.World world = center.getWorld();
        String particleName = plugin.getConfigUtil().getString("spawn.particle", "END_ROD").toUpperCase();
        final org.bukkit.Particle particle = resolveParticle(particleName);
        final int maxTicks = Math.max(1, plugin.getConfigUtil().getInt("spawn.particle-duration", 10));
        final int points = Math.max(4, plugin.getConfigUtil().getInt("spawn.particle-points", 16));
        final int count = Math.max(0, plugin.getConfigUtil().getInt("spawn.particle-count", 2));
        final double spread = Math.max(0.0, plugin.getConfigUtil().getDouble("spawn.particle-spread", 0.1));

        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > maxTicks) { cancel(); return; }
                double radius = 0.4 + tick * 0.12;
                double height = 0.3 + tick * 0.08;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    Location p = new Location(world, x, center.getY() + height, z);
                    try {
                        if (particle.name().equals("REDSTONE")) {
                            int r = plugin.getConfigUtil().getInt("spawn.redstone-r", 255);
                            int g = plugin.getConfigUtil().getInt("spawn.redstone-g", 255);
                            int b = plugin.getConfigUtil().getInt("spawn.redstone-b", 255);
                            org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(r, g, b), 1.0f);
                            world.spawnParticle(particle, p, Math.max(1, count), 0.0, 0.0, 0.0, dust);
                        } else {
                            world.spawnParticle(particle, p, count, spread, spread, spread, 0.0);
                        }
                    } catch (Throwable ignored) {}
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Resolve particle enum safely for current server version
    private org.bukkit.Particle resolveParticle(String name) {
        if (name == null) return org.bukkit.Particle.END_ROD;
        try {
            return org.bukkit.Particle.valueOf(name);
        } catch (Throwable ex) {
            return org.bukkit.Particle.END_ROD;
        }
    }

    /**
     * Return wave mobs in radius around center
     */
    public List<LivingEntity> getNearbyWaveMobs(Location center, double radius) {
        java.util.ArrayList<LivingEntity> out = new java.util.ArrayList<>();
        if (center == null || center.getWorld() == null) return out;
        double rsq = radius * radius;
        for (Entity e : center.getWorld().getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) e;
            if (!le.getPersistentDataContainer().has(waveKey, PersistentDataType.BYTE)) continue;
            if (le.getLocation().distanceSquared(center) > rsq) continue;
            out.add(le);
        }
        return out;
    }

    // Freeze einen einzelnen Mob für einen Spieler (nur AI aus) und starte Enforcer
    public void freezeSingleMobForPlayer(UUID playerUuid, LivingEntity mob) {
        if (playerUuid == null || mob == null) return;
        try {
            Set<UUID> set = frozenByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
            set.add(mob.getUniqueId());
            // Nur AI deaktivieren
            try { mob.getClass().getMethod("setAI", boolean.class).invoke(mob, false); } catch (Throwable ignored) {}
            ensureEnforcerRunning();
        } catch (Throwable ignored) {}
    }

    // Freeze alle nahegelegenen Wave-Mobs für einen Spieler
    public void freezeMobsForPlayer(UUID playerUuid, Location center, double radius) {
        if (playerUuid == null || center == null) return;
        List<LivingEntity> mobs = getNearbyWaveMobs(center, radius);
        if (mobs.isEmpty()) return;
        for (LivingEntity le : mobs) {
            freezeSingleMobForPlayer(playerUuid, le);
        }
        ensureEnforcerRunning();
    }

    // Unfreeze Mobs, die für diesen Spieler eingefroren sind. Nur wenn kein anderer Spieler sie noch einfriert.
    public void unfreezeMobsForPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        Set<UUID> set = frozenByPlayer.remove(playerUuid);
        if (set == null || set.isEmpty()) return;
        for (UUID eu : set) {
            try {
                boolean stillReferenced = false;
                for (Map.Entry<UUID, Set<UUID>> en : frozenByPlayer.entrySet()) {
                    if (en.getValue().contains(eu)) { stillReferenced = true; break; }
                }
                if (stillReferenced) continue;
                Entity ent = Bukkit.getEntity(eu);
                if (!(ent instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) ent;
                // AI wieder aktivieren
                try { le.getClass().getMethod("setAI", boolean.class).invoke(le, true); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
    }

    private void removeEntityFromAllSets(UUID eu) {
        for (UUID key : frozenByPlayer.keySet()) {
            Set<UUID> s = frozenByPlayer.get(key);
            if (s != null) s.remove(eu);
            if (s != null && s.isEmpty()) frozenByPlayer.remove(key);
        }
    }

    // Enforcer-Task: Deaktiviert regelmäßig die AI für alle eingefrorenen Entities, falls sie reaktiviert wurde.
    private void ensureEnforcerRunning() {
        try {
            if (freezeEnforcerTask != null && !freezeEnforcerTask.isCancelled()) return;
            freezeEnforcerTask = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (frozenByPlayer.isEmpty()) {
                            cancel();
                            freezeEnforcerTask = null;
                            return;
                        }

                        // Snapshot aller referenzierten Entity-UUIDs
                        Set<UUID> all = ConcurrentHashMap.newKeySet();
                        for (Set<UUID> s : frozenByPlayer.values()) {
                            if (s != null) all.addAll(s);
                        }

                        for (UUID eu : all) {
                            try {
                                Entity ent = Bukkit.getEntity(eu);
                                if (!(ent instanceof LivingEntity)) {
                                    removeEntityFromAllSets(eu);
                                    continue;
                                }
                                LivingEntity le = (LivingEntity) ent;
                                // AI deaktiviert halten (Reflection für Kompatibilität)
                                try { le.getClass().getMethod("setAI", boolean.class).invoke(le, false); } catch (Throwable ignored) {}
                            } catch (Throwable ignored) {}
                        }

                        // Aufräumen leerer Player-Einträge
                        for (UUID key : frozenByPlayer.keySet()) {
                            Set<UUID> s = frozenByPlayer.get(key);
                            if (s == null || s.isEmpty()) frozenByPlayer.remove(key);
                        }
                    } catch (Throwable ignored) {}
                }
            }.runTaskTimer(plugin, 0L, 20L);
        } catch (Throwable ignored) {}
    }

    /**
     * Blitzeffekt + Schaden sicher auf dem Main-Thread
     */
    public void strikeLightningAtTarget(LivingEntity target, double damage, Player source) {
        if (target == null) return;
        Location loc = target.getLocation();
        if (loc == null || loc.getWorld() == null) return;
        Runnable r = () -> {
            try { loc.getWorld().strikeLightningEffect(loc); } catch (Throwable ignored) {}
            try { target.damage(damage, source); } catch (Throwable ex) { plugin.getLogger().warning("strikeLightningAtTarget: " + ex.getMessage()); }
        };
        if (Bukkit.isPrimaryThread()) r.run(); else Bukkit.getScheduler().runTask(plugin, r);
    }

    // ===== Continuous spawn API =====
    public synchronized void startContinuousIfEnabled() {
        boolean enabled = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);
        if (!enabled) return;
        startContinuous();
    }

    public synchronized void startContinuous() {
        stopContinuous();
        continuousStartMillis = System.currentTimeMillis();
        int ticksPerCycle = plugin.getConfigUtil().getInt("spawn.continuous.ticks-per-cycle", 20);
        continuousTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try { continuousTick(); } catch (Throwable ignored) {}
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, ticksPerCycle));
        plugin.getLogger().info("Continuous spawn started (ticks-per-cycle=" + ticksPerCycle + ")");
        ensureScalingTask();
        ensureAggroTask();
    }

    public synchronized void pauseContinuous() {
        if (continuousTask != null) {
            continuousTask.cancel();
            continuousTask = null;
            plugin.getLogger().info("Continuous spawn paused");
        }
    }

    public synchronized void resumeContinuous() {
        if (continuousTask == null) {
            startContinuous();
            plugin.getLogger().info("Continuous spawn resumed");
        }
    }

    public synchronized void stopContinuous() {
        if (continuousTask != null) {
            continuousTask.cancel();
            continuousTask = null;
        }
        spawnAccumulator.clear();
        if (scalingTask != null) { scalingTask.cancel(); scalingTask = null; }
    }

    private void continuousTick() {
        // Global game paused? Avoid spawns when state is PAUSED
        try {
            org.bysenom.minecraftSurvivors.model.GameState state = plugin.getGameManager() != null ? plugin.getGameManager().getState() : null;
            if (state == org.bysenom.minecraftSurvivors.model.GameState.PAUSED) return;
        } catch (Throwable ignored) {}

        // Optional Ruhephase
        int restEvery = plugin.getConfigUtil().getInt("spawn.continuous.rest-every-seconds", 0);
        int restDur = plugin.getConfigUtil().getInt("spawn.continuous.rest-duration-seconds", 0);
        long now = System.currentTimeMillis();
        if (restEvery > 0 && restDur > 0) {
            long elapsedSec = (long) Math.floor(getElapsedMinutes() * 60.0);
            if (elapsedSec > 0 && elapsedSec % Math.max(1, restEvery) == 0) {
                // enter rest phase window (idempotent)
                restPhaseUntilMillis = Math.max(restPhaseUntilMillis, now + restDur * 1000L);
            }
            if (now < restPhaseUntilMillis) {
                // Could add subtle calm particles around players
                return; // skip spawning during rest
            }
        }

        double minutes = getElapsedMinutes();
        double seconds = minutes * 60.0;
        boolean useSteps = plugin.getConfigUtil().getBoolean("spawn.continuous.use-steps", true);
        int stepSeconds = plugin.getConfigUtil().getInt("spawn.continuous.step-seconds", 30);
        double base = plugin.getConfigUtil().getDouble("spawn.continuous.base-per-player", 0.4);
        double perPlayer;
        if (useSteps) {
            int steps = Math.max(0, (int) Math.floor(seconds / Math.max(1, stepSeconds)));
            double growthPerStep = plugin.getConfigUtil().getDouble("spawn.continuous.growth-per-step", 0.2);
            perPlayer = base + steps * growthPerStep;
        } else {
            double growthPerMin = plugin.getConfigUtil().getDouble("spawn.continuous.growth-per-minute", 0.25);
            perPlayer = base + growthPerMin * minutes;
        }
        // Warmup ramp
        int warmupSec = plugin.getConfigUtil().getInt("spawn.continuous.warmup-seconds", 90);
        double warmStart = plugin.getConfigUtil().getDouble("spawn.continuous.warmup-mult-start", 0.25);
        if (warmupSec > 0 && seconds < warmupSec) {
            double t = Math.min(1.0, Math.max(0.0, seconds / warmupSec));
            double mult = warmStart + (1.0 - warmStart) * t; // linear ramp from warmStart -> 1.0
            perPlayer *= mult;
        }
        double maxPerPlayer = plugin.getConfigUtil().getDouble("spawn.continuous.max-per-player", 8.0);
        perPlayer = Math.max(0.0, Math.min(perPlayer, maxPerPlayer));

        // dynamic cap
        boolean dynCap = plugin.getConfigUtil().getBoolean("spawn.continuous.cap.dynamic", true);
        int capNearby;
        double capRadius = plugin.getConfigUtil().getDouble("spawn.continuous.radius-per-player", 40.0);
        if (dynCap) {
            int online = Bukkit.getOnlinePlayers().size();
            int baseCap = plugin.getConfigUtil().getInt("spawn.continuous.cap.base-per-player", 60);
            int addPer = plugin.getConfigUtil().getInt("spawn.continuous.cap.add-per-player", 30);
            int maxCap = plugin.getConfigUtil().getInt("spawn.continuous.cap.max-per-player", 180);
            capNearby = Math.min(maxCap, baseCap + Math.max(0, online - 1) * addPer);
        } else {
            capNearby = plugin.getConfigUtil().getInt("spawn.continuous.cap-total-nearby-per-player", 60);
        }

        double minDist = plugin.getConfigUtil().getDouble("spawn.continuous.min-distance", plugin.getConfigUtil().getDouble("spawn.min-distance", 8.0));
        double maxDist = plugin.getConfigUtil().getDouble("spawn.continuous.max-distance", plugin.getConfigUtil().getDouble("spawn.max-distance", 16.0));
        boolean shouldGlow = plugin.getConfigUtil().getBoolean("spawn.glowing", true);
        int glowTicks = plugin.getConfigUtil().getInt("spawn.glowing-duration-ticks", 20 * 60 * 5);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline() || !p.isValid() || p.getWorld() == null) continue;
            Location playerLoc = p.getLocation();
            // cap density per player
            int nearby = countOurMobsAround(playerLoc, capRadius);
            if (nearby >= capNearby) continue;

            double acc = spawnAccumulator.getOrDefault(p.getUniqueId(), 0.0);
            double expected = acc + perPlayer;
            int toSpawn = (int) Math.floor(expected);
            acc = expected - toSpawn; // remainder keeps fractional budget
            spawnAccumulator.put(p.getUniqueId(), acc);

            // also ensure not to exceed density cap in this cycle
            int allowed = Math.max(0, capNearby - nearby);
            toSpawn = Math.min(toSpawn, allowed);
            for (int i = 0; i < toSpawn; i++) {
                EntityType type = pickEntityType(minutes);
                if (type == null) type = EntityType.ZOMBIE;
                Location spawnLoc = randomNearby(playerLoc, minDist, maxDist);
                LivingEntity mob;
                try {
                    mob = (LivingEntity) p.getWorld().spawnEntity(spawnLoc, type);
                } catch (Throwable t) {
                    mob = (LivingEntity) p.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                }
                mob.getPersistentDataContainer().set(waveKey, PersistentDataType.BYTE, (byte) 1);
                captureBaselineIfMissing(mob);
                // Elite roll
                maybeMakeElite(mob);
                if (shouldGlow) {
                    try { mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Math.max(20, glowTicks), 0, false, false, false)); } catch (Throwable ignored) {}
                }
                applyScaling(mob, minutes);
                // freeze if player is paused locally
                try {
                    if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(p.getUniqueId())) {
                        freezeSingleMobForPlayer(p.getUniqueId(), mob);
                    }
                } catch (Throwable ignored) {}
                // animation
                playSpawnAnimation(spawnLoc);
            }
        }
    }

    private int countOurMobsAround(Location center, double radius) {
        if (center == null || center.getWorld() == null) return 0;
        double r2 = radius * radius;
        int count = 0;
        for (Entity e : center.getWorld().getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            if (!e.getPersistentDataContainer().has(waveKey, PersistentDataType.BYTE)) continue;
            if (e.getLocation().distanceSquared(center) <= r2) count++;
        }
        return count;
    }

    private void applyScaling(LivingEntity mob, double minutes) {
        try {
            captureBaselineIfMissing(mob);
            org.bukkit.persistence.PersistentDataContainer pdc = mob.getPersistentDataContainer();
            double baseHp = pdc.getOrDefault(BASE_MAX_HP, org.bukkit.persistence.PersistentDataType.DOUBLE, 20.0);
            double baseSpd = pdc.getOrDefault(BASE_SPEED, org.bukkit.persistence.PersistentDataType.DOUBLE, 0.25);
            double baseDmg = pdc.getOrDefault(BASE_DAMAGE, org.bukkit.persistence.PersistentDataType.DOUBLE, 3.0);
            // Skeleton-Schaden leicht reduzieren als Balance
            if (mob.getType() == org.bukkit.entity.EntityType.SKELETON) {
                baseDmg = Math.max(1.0, baseDmg * 0.75);
            }

            double baseHpm = plugin.getConfigUtil().getDouble("scaling.health-mult-per-minute", 0.10);
            double midMin = plugin.getConfigUtil().getDouble("scaling.health-mid-minute", 4.0);
            double lateMin = plugin.getConfigUtil().getDouble("scaling.health-late-minute", 10.0);
            double midMul = plugin.getConfigUtil().getDouble("scaling.health-mid-multiplier", 1.35);
            double lateMul = plugin.getConfigUtil().getDouble("scaling.health-late-multiplier", 1.80);
            double speedPerMin = plugin.getConfigUtil().getDouble("scaling.speed-mult-per-minute", 0.05);
            double dmgAddPerMin = plugin.getConfigUtil().getDouble("scaling.damage-add-per-minute", 0.5);
            double dmgMidMul = plugin.getConfigUtil().getDouble("scaling.damage-mid-multiplier", 1.10);
            double dmgLateMul = plugin.getConfigUtil().getDouble("scaling.damage-late-multiplier", 1.25);

            double healthMultPerMin = baseHpm;
            if (minutes >= lateMin) healthMultPerMin *= lateMul; else if (minutes >= midMin) healthMultPerMin *= midMul;
            double healthMult = 1.0 + Math.max(0, minutes) * healthMultPerMin;

            double speedMult = 1.0 + Math.max(0, minutes) * (speedPerMin * 0.9);
            double dmgMul = 1.0;
            if (minutes >= lateMin) dmgMul = dmgLateMul; else if (minutes >= midMin) dmgMul = dmgMidMul;
            double dmgAdd = Math.max(0, minutes) * (dmgAddPerMin * dmgMul);

            EnrageFactors ef = computeEnrage(minutes);

            // Zielwerte relativ zu Baselines
            double targetMaxHp = Math.max(1.0, baseHp * healthMult * ef.hMul);
            double targetSpeed = Math.max(0.01, baseSpd * speedMult * ef.sMul);
            double targetDamage = Math.max(0.0, (baseDmg + dmgAdd) * ef.dMul);

            // Setzen mit prozentualer Gesundheitserhaltung
            try {
                org.bukkit.attribute.AttributeInstance maxHp = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    double oldMax = maxHp.getBaseValue();
                    double oldHealth = mob.getHealth();
                    double ratio = oldMax > 0.0 ? Math.min(1.0, oldHealth / oldMax) : 1.0;
                    maxHp.setBaseValue(targetMaxHp);
                    mob.setHealth(Math.max(1.0, Math.min(targetMaxHp, ratio * targetMaxHp)));
                }
            } catch (Throwable ignored) {}
            try {
                org.bukkit.attribute.AttributeInstance move = mob.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                if (move != null) move.setBaseValue(targetSpeed);
            } catch (Throwable ignored) {}
            try {
                org.bukkit.attribute.AttributeInstance dmg = mob.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(targetDamage);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    // Enrage-Konfiguration wird aus der Config gelesen
    private boolean enrageEnabled() { return plugin.getConfigUtil().getBoolean("enrage.enabled", true); }
    private double enrageStartMinute() { return plugin.getConfigUtil().getDouble("enrage.start-minute", 12.0); }
    private double enrageRampMinutes() { return Math.max(0.1, plugin.getConfigUtil().getDouble("enrage.ramp-minutes", 3.0)); }
    private double enrageHealthMax() { return Math.max(1.0, plugin.getConfigUtil().getDouble("enrage.health-mult-max", 3.0)); }
    private double enrageSpeedMax() { return Math.max(1.0, plugin.getConfigUtil().getDouble("enrage.speed-mult-max", 1.6)); }
    private double enrageDamageMax() { return Math.max(1.0, plugin.getConfigUtil().getDouble("enrage.damage-mult-max", 2.0)); }

    private EnrageFactors computeEnrage(double minutes) {
        if (!enrageEnabled()) return new EnrageFactors(0.0, 1.0, 1.0, 1.0);
        double start = enrageStartMinute();
        double ramp = enrageRampMinutes();
        if (minutes <= start) return new EnrageFactors(0.0, 1.0, 1.0, 1.0);
        // Kein Max-Cap: nach der Ramp geht es linear weiter
        double prog = Math.max(0.0, (minutes - start) / ramp);
        double h = 1.0 + (enrageHealthMax() - 1.0) * prog;
        double s = 1.0 + (enrageSpeedMax() - 1.0) * prog;
        double d = 1.0 + (enrageDamageMax() - 1.0) * prog;
        return new EnrageFactors(Math.min(1.0, prog), h, s, d);
    }

    // Öffentliche Metriken für Anzeige
    public double getElapsedMinutes() {
        if (continuousStartMillis <= 0L) return 0.0;
        long dt = System.currentTimeMillis() - continuousStartMillis;
        return dt / 60000.0;
    }

    public double getEnrageProgress() {
        double m = getElapsedMinutes();
        return computeEnrage(m).prog;
    }

    public double getEnemyPowerIndex() {
        double minutes = getElapsedMinutes();
        double baseHpm = plugin.getConfigUtil().getDouble("scaling.health-mult-per-minute", 0.10);
        double midMin = plugin.getConfigUtil().getDouble("scaling.health-mid-minute", 4.0);
        double lateMin = plugin.getConfigUtil().getDouble("scaling.health-late-minute", 10.0);
        double midMul = plugin.getConfigUtil().getDouble("scaling.health-mid-multiplier", 1.35);
        double lateMul = plugin.getConfigUtil().getDouble("scaling.health-late-multiplier", 1.80);
        double speedPerMin = plugin.getConfigUtil().getDouble("scaling.speed-mult-per-minute", 0.05);
        double dmgAddPerMin = plugin.getConfigUtil().getDouble("scaling.damage-add-per-minute", 0.5);
        double dmgMidMul = plugin.getConfigUtil().getDouble("scaling.damage-mid-multiplier", 1.10);
        double dmgLateMul = plugin.getConfigUtil().getDouble("scaling.damage-late-multiplier", 1.25);

        double healthMultPerMin = baseHpm;
        if (minutes >= lateMin) healthMultPerMin *= lateMul; else if (minutes >= midMin) healthMultPerMin *= midMul;
        double h = 1.0 + Math.max(0, minutes) * healthMultPerMin;
        double s = 1.0 + Math.max(0, minutes) * (speedPerMin * 0.9);
        double dAdd = Math.max(0, minutes) * (dmgAddPerMin * (minutes >= lateMin ? dmgLateMul : (minutes >= midMin ? dmgMidMul : 1.0)));

        EnrageFactors ef = computeEnrage(minutes);
        double dMul = ef.dMul;
        double power = h * ef.hMul * s * ef.sMul * (1.0 + (dAdd / 5.0)) * dMul; // grobe Gesamtstärke
        if (!Double.isFinite(power)) power = 1.0;
        return Math.max(0.1, power);
    }

    // Re-add missing methods
    private void maybeMakeElite(LivingEntity mob) {
        try {
            int baseChance = plugin.getConfigUtil().getInt("spawn.elite.chance-percentage", 8);
            double extraPerMin = plugin.getConfigUtil().getDouble("spawn.elite.extra-chance-per-minute", 0.0);
            double minutes = getElapsedMinutes();
            double chanceF = Math.min(100.0, Math.max(0.0, baseChance + Math.max(0.0, minutes) * extraPerMin));
            int chance = (int) Math.round(chanceF);
            if (chance <= 0) return;
            if (random.nextInt(100) >= chance) return;
            // mark elite
            mob.getPersistentDataContainer().set(eliteKey, PersistentDataType.BYTE, (byte)1);
            double baseMult = plugin.getConfigUtil().getDouble("spawn.elite.base-health-mult", 1.5);
            double perMin = plugin.getConfigUtil().getDouble("spawn.elite.extra-health-mult-per-minute", 0.03);
            double eliteMult = Math.max(1.0, baseMult + Math.max(0.0, minutes) * perMin);
            try {
                org.bukkit.attribute.Attribute maxHealthAttr = org.bukkit.Registry.ATTRIBUTE.get(MAX_HEALTH);
                org.bukkit.attribute.AttributeInstance maxHp = mob.getAttribute(maxHealthAttr);
                if (maxHp != null) {
                    double newBase = Math.max(1.0, maxHp.getBaseValue() * eliteMult);
                    maxHp.setBaseValue(newBase);
                    mob.setHealth(Math.min(newBase, mob.getHealth()));
                }
            } catch (Throwable ignored) {}
            try { mob.customName(net.kyori.adventure.text.Component.text("Elite "+mob.getType().name()).color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)); mob.setCustomNameVisible(true);} catch (Throwable ignored) {}
            try { java.lang.reflect.Method m = mob.getClass().getMethod("setScale", float.class); m.invoke(mob, (float) plugin.getConfigUtil().getDouble("spawn.elite.size-scale", 1.25)); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static class Weighted {
        final EntityType type; final int weight;
        Weighted(EntityType t, int w) { this.type = t; this.weight = w; }
    }

    private EntityType pickEntityType(double minutes) {
        List<Map<?, ?>> entries = plugin.getConfigUtil().getConfig().getMapList("spawnMobTypes");
        if (entries == null || entries.isEmpty()) return EntityType.ZOMBIE;
        java.util.ArrayList<Weighted> pool = new java.util.ArrayList<>();
        for (Map<?, ?> m : entries) {
            try {
                Object oType = m.get("type");
                Object oWeight = m.get("weight");
                Object oMin = m.get("minMinute");
                String typeName = oType != null ? String.valueOf(oType) : "ZOMBIE";
                int weight = oWeight != null ? Integer.parseInt(String.valueOf(oWeight)) : 1;
                int minMinute = oMin != null ? Integer.parseInt(String.valueOf(oMin)) : 0;
                if (minutes < minMinute) continue;
                EntityType et = null;
                try {
                    String lower = typeName.toLowerCase();
                    org.bukkit.NamespacedKey ns = org.bukkit.NamespacedKey.minecraft(lower);
                    et = org.bukkit.Registry.ENTITY_TYPE.get(ns);
                    if (et == null) {
                        plugin.getLogger().warning("SpawnManager: Unknown mob type in config spawnMobTypes: '" + typeName + "' (registry=null) — defaulting to ZOMBIE");
                        et = EntityType.ZOMBIE; // strict fallback
                    }
                } catch (Throwable registryEx) {
                    plugin.getLogger().warning("SpawnManager: Registry lookup failed for '" + typeName + "': " + registryEx.getMessage() + ". Falling back to ZOMBIE");
                    et = EntityType.ZOMBIE;
                }
                if (!et.isAlive()) continue;
                if (weight <= 0) continue;
                pool.add(new Weighted(et, weight));
            } catch (Throwable ignored) {}
        }
        if (pool.isEmpty()) return EntityType.ZOMBIE;
        int total = 0; for (Weighted w : pool) total += w.weight;
        int r = random.nextInt(Math.max(1, total)); int cur = 0;
        for (Weighted w : pool) { cur += w.weight; if (r < cur) return w.type; }
        return pool.get(pool.size()-1).type;
    }

    public void markAsWave(org.bukkit.entity.LivingEntity mob) {
        if (mob == null) return;
        try { mob.getPersistentDataContainer().set(waveKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1); } catch (Throwable ignored) {}
    }

    public void repelMobsAround(org.bukkit.entity.Player p, double radius, double strength, boolean onlyWave) {
        if (p == null || p.getWorld() == null) return;
        try {
            java.util.List<org.bukkit.entity.LivingEntity> list;
            if (onlyWave) list = getNearbyWaveMobs(p.getLocation(), radius);
            else {
                list = new java.util.ArrayList<>();
                double r2 = radius * radius;
                for (org.bukkit.entity.Entity e : p.getWorld().getEntities()) {
                    if (!(e instanceof org.bukkit.entity.LivingEntity)) continue;
                    if (e.getUniqueId().equals(p.getUniqueId())) continue;
                    if (e.getLocation().distanceSquared(p.getLocation()) > r2) continue;
                    list.add((org.bukkit.entity.LivingEntity) e);
                }
            }
            org.bukkit.util.Vector pc = p.getLocation().toVector();
            for (org.bukkit.entity.LivingEntity le : list) {
                try {
                    org.bukkit.util.Vector dir = le.getLocation().toVector().subtract(pc).normalize();
                    if (!Double.isFinite(dir.getX()) || !Double.isFinite(dir.getZ())) continue;
                    double y = 0.35 + 0.15 * random.nextDouble();
                    org.bukkit.util.Vector v = new org.bukkit.util.Vector(dir.getX() * strength, y, dir.getZ() * strength);
                    le.setVelocity(v);
                    try { le.getWorld().playSound(le.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.0f); } catch (Throwable ignored) {}
                    try { le.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, le.getLocation().add(0,0.2,0), 6, 0.25, 0.1, 0.25, 0.01); } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static final class EnrageFactors { final double prog, hMul, sMul, dMul; EnrageFactors(double p,double h,double s,double d){prog=p;hMul=h;sMul=s;dMul=d;} }
}
