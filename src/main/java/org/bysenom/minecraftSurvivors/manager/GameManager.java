// File: src/main/java/org/bysenom/minecraftSurvivors/manager/GameManager.java
package org.bysenom.minecraftSurvivors.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.listener.LootchestListener;
import org.bysenom.minecraftSurvivors.model.GameState;
import org.bysenom.minecraftSurvivors.task.WaveTask;

public class GameManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final SpawnManager spawnManager; // replaced Backup class with new SpawnManager
    private final AbilityManager abilityManager;
    private GameState state = GameState.LOBBY;
    private WaveTask currentWaveTask;
    private BukkitTask xpHudTask;
    private int currentWaveNumber = 1;
    private int pauseCounter = 0; // counts GUI pauses (e.g., multiple players)
    private final java.util.Set<java.util.UUID> pausedPlayers = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> pauseTimeoutTasks = new java.util.concurrent.ConcurrentHashMap<>();
    // Temporary protection after repel/loot/actions to prevent instant death due to AI/race conditions
    private final java.util.Map<java.util.UUID, Long> protectedUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> protectTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean starting = false;
    private org.bukkit.scheduler.BukkitTask countdownTask;

    private final java.util.Map<java.util.UUID, java.util.Deque<GuiRequest>> guiQueues = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class GuiRequest {
        enum Type { LEVEL_UP, LOOT_CHEST }
        final Type type; final int level;
        GuiRequest(Type t, int level) { this.type=t; this.level=level; }
        static GuiRequest levelUp(int lvl) { return new GuiRequest(Type.LEVEL_UP, lvl); }
        static GuiRequest loot() { return new GuiRequest(Type.LOOT_CHEST, 0); }
    }

    public void enqueueLevelUp(java.util.UUID uuid, int level) {
        if (uuid == null) return;
        guiQueues.computeIfAbsent(uuid, k -> new java.util.ArrayDeque<>()).add(GuiRequest.levelUp(level));
    }
    public void enqueueLoot(java.util.UUID uuid) {
        if (uuid == null) return;
        guiQueues.computeIfAbsent(uuid, k -> new java.util.ArrayDeque<>()).add(GuiRequest.loot());
    }

    public void tryOpenNextQueued(java.util.UUID uuid) {
        if (uuid == null) return;
        java.util.Deque<GuiRequest> q = guiQueues.get(uuid);
        if (q == null || q.isEmpty()) return;
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p == null || !p.isOnline()) return;
        GuiRequest req = q.pollFirst();
        if (req == null) return;
        switch (req.type) {
            case LEVEL_UP -> {
                try { if (plugin.getGuiManager() != null) plugin.getGuiManager().openLevelUpMenu(p, Math.max(1, req.level)); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "openLevelUpMenu failed for " + uuid + ": ", t); }
            }
            case LOOT_CHEST -> {
                try { LootchestListener.openQueued(p); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "openQueued lootchest failed for " + uuid + ": ", t); }
            }
        }
    }

    /** Öffnet das nächste wartende GUI (LevelUp / LootChest) mit 1 Tick Verzögerung. */
    public void tryOpenNextQueuedDelayed(java.util.UUID uuid) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> tryOpenNextQueued(uuid), 1L);
    }

    private final BossManager bossManager; // neu

    // Neu: Spieler, die sich im Survivors-Kontext befinden (Klassenwahl/Startphase od. Spiel)
    private final java.util.Set<java.util.UUID> survivorsContext = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public GameManager(MinecraftSurvivors plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.spawnManager = new SpawnManager(plugin, playerManager);
        this.abilityManager = new AbilityManager(plugin, playerManager, spawnManager, this);
        this.bossManager = new BossManager(plugin, spawnManager);
    }

    public BossManager getBossManager() { return bossManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }

    // Survivors-Kontext API
    public void enterSurvivorsContext(java.util.UUID uuid) {
        if (uuid == null) return;
        survivorsContext.add(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            // Scoreboard sichtbar halten
            try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "forceUpdateAll failed on enterSurvivorsContext for " + uuid + ": ", t); }
            // Klassenfähigkeit nur erzwingen, falls Spiel bereits läuft (RUNNING) und Klasse gewählt ist
            if (state == GameState.RUNNING) {
                try { ensureClassAbility(uuid); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "ensureClassAbility failed for " + uuid + ": ", t); }
            }
            try { giveInitialKit(p); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "giveInitialKit failed for " + uuid + ": ", t); }
        }
    }
    public void exitSurvivorsContext(java.util.UUID uuid) {
        if (uuid == null) return;
        survivorsContext.remove(uuid);
    }
    public boolean isInSurvivorsContext(java.util.UUID uuid) { return uuid != null && survivorsContext.contains(uuid); }
    // Backwards-Compat: alter Methodenname
    public void leaveSurvivorsContext(java.util.UUID uuid) { exitSurvivorsContext(uuid); }

    private void startWaveTask() {
        if (currentWaveTask != null) currentWaveTask.cancel();
        currentWaveTask = new WaveTask(plugin, this, currentWaveNumber);
        currentWaveTask.runTaskTimer(plugin, 0L, 20L * 10);
    }

    private void startHudTask() {
        if (xpHudTask != null) xpHudTask.cancel();
        int hudIntervalTicks = plugin.getConfigUtil().getInt("levelup.hud-interval-ticks", 100);
        xpHudTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    // Skip HUD for players who are currently paused (choose reward)
                    if (isPlayerPaused(p.getUniqueId())) continue;
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
                    if (sp == null) continue;

                    // Apply MAX_HEALTH bonus dynamically
                    try {
                        var attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        int heartsExtra = sp.getEffectiveExtraHearts();
                        double maxHealthBonusHearts = sp.getMaxHealthBonusHearts();
                        double target = Math.max(1.0, 20.0 + heartsExtra * 1.0 + maxHealthBonusHearts * 2.0);
                        if (attr != null && Math.abs(attr.getBaseValue() - target) > 0.1) {
                            attr.setBaseValue(target);
                            if (p.getHealth() > target) p.setHealth(target);
                        }
                    } catch (Throwable ignored) {}

                    // Periodic regeneration (HP + Shield)
                    try {
                        double hpRegenPerSec = sp.getHpRegen();
                        if (hpRegenPerSec > 0.0) {
                            double perTick = hpRegenPerSec / (plugin.getConfigUtil().getInt("combat.ticks-per-second", 20));
                            if (perTick > 0.0) {
                                var maxAttr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                                double max = maxAttr != null ? maxAttr.getBaseValue() : 20.0;
                                p.setHealth(Math.min(max, p.getHealth() + perTick));
                            }
                        }
                    } catch (Throwable ignored) {}
                    try {
                        double shieldMax = sp.getShieldMax();
                        if (shieldMax > 0.0) {
                            long delayMs = (long) (plugin.getConfigUtil().getDouble("combat.shield.regen-delay-seconds", 3.0) * 1000.0);
                            double perSec = plugin.getConfigUtil().getDouble("combat.shield.regen-percentage-per-second", 0.15) * shieldMax;
                            long dt = System.currentTimeMillis() - sp.getLastDamagedAtMillis();
                            if (dt >= Math.max(0L, delayMs) && perSec > 0.0) {
                                double add = perSec / (plugin.getConfigUtil().getInt("combat.ticks-per-second", 20));
                                sp.setShieldCurrent(Math.min(shieldMax, sp.getShieldCurrent() + add));
                            }
                        }
                    } catch (Throwable ignored) {}

                    int currentXp = sp.getXp();
                    int xpToNext = sp.getXpToNext();
                    p.sendActionBar(Component.text("XP: " + currentXp + "/" + xpToNext + " • Lvl " + sp.getClassLevel()));
                } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "HUD update failed for player: " + (p == null ? "null" : p.getUniqueId()) + ": ", t); }
            }
            // BossManager tick
            try { bossManager.tick(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "bossManager.tick failed: ", t); }
        }, 0L, hudIntervalTicks);
    }

    /**
     * Sorgt dafür, dass die Klassen-Ability im Ability-Slot vorhanden ist (Slot 0..4 wird von SkillManager gerendert).
     */
    public void ensureClassAbility(java.util.UUID uuid) {
        if (uuid == null) return;
        var sp = playerManager.get(uuid); if (sp == null) return;
        org.bysenom.minecraftSurvivors.model.PlayerClass pc = sp.getSelectedClass(); if (pc == null) return;
        String correct = switch (pc) {
            case SHAMAN -> "ab_lightning";
            case PYROMANCER -> "ab_fire";
            case RANGER -> "ab_ranged";
            case PALADIN -> "ab_holy";
        };
        // Entferne andere Klassen-Abilities falls vorhanden (nur die, die nicht zur aktuellen gehören)
        java.util.List<String> classAbilities = java.util.Arrays.asList("ab_lightning","ab_fire","ab_ranged","ab_holy");
        for (String k : new java.util.ArrayList<>(sp.getAbilities())) {
            if (classAbilities.contains(k) && !k.equals(correct)) {
                sp.removeAbility(k);
            }
        }
        if (!sp.hasAbility(correct)) {
            int idx = sp.addAbilityAtFirstFreeIndex(correct, 1);
            if (idx >= 0) {
                sp.setAbilityOrigin(correct, "class");
                try { plugin.getPlayerDataManager().saveAsync(sp); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "saveAsync failed for " + uuid + ": ", t); }
            }
        }
    }

    /**
     * Räumt die Hotbar-Slots 0..4 leer, damit der SkillManager dort die Abilities rendert (keine Klassenwaffen mehr).
     */
    public void giveInitialKit(org.bukkit.entity.Player p) {
        if (p == null) return;
        try {
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            for (int i = 0; i < 5; i++) { inv.setItem(i, null); }
        } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "giveInitialKit inventory clear failed for player " + p.getUniqueId() + ": ", t); }
        try { p.updateInventory(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "giveInitialKit updateInventory failed for player " + p.getUniqueId() + ": ", t); }
    }

    public synchronized void startGame() {
        if (state == GameState.RUNNING) return;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        starting = false;
        state = GameState.RUNNING;
        // RunStart = wirklich neu: keine Skills/Abilities aus vorheriger Runde beibehalten
        playerManager.resetAll();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            try {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
                plugin.getMetaManager().applyMetaOnRunStart(p, sp);
                // Beim Start sicherstellen, dass alle aktiven Spieler im Kontext sind
                enterSurvivorsContext(p.getUniqueId());
            } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "applyMetaOnRunStart/enterContext failed for player: " + p.getUniqueId() + ": ", t); }
        }
        this.currentWaveNumber = 1;
        boolean continuous = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);
        if (continuous) {
            spawnManager.startContinuousIfEnabled();
        } else {
            startWaveTask();
        }
        abilityManager.start();
        startHudTask();
        try { plugin.getShopNpcManager().spawnConfigured(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "shopNpcManager.spawnConfigured failed: ", t); }
        // Survivors-HUD einschalten: Scoreboard-Update triggern, Bossbars werden im StatsDisplayManager-Tick nur bei RUNNING gezeigt
        try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "scoreboardManager.forceUpdateAll failed: ", t); }
        plugin.getLogger().info("Game started");
    }

    public synchronized void stopGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;
        if (currentWaveTask != null) currentWaveTask.cancel();
        spawnManager.stopContinuous();
        spawnManager.clearWaveMobs();
        abilityManager.stop();
        try { plugin.getSkillManager().clearLingeringEffects(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "skillManager.clearLingeringEffects failed: ", t); }
        if (xpHudTask != null) xpHudTask.cancel();
        try { plugin.getShopNpcManager().despawnAll(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "shopNpcManager.despawnAll failed: ", t); }
        // Scoreboard soll nach Tod bestehen bleiben -> Tasks beenden aber nicht resetten
        try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "scoreboardManager.forceUpdateAll failed: ", t); }
        try { plugin.getStatsDisplayManager().clearAllBossbarsNow(); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "statsDisplayManager.clearAllBossbarsNow failed: ", t); }
        // survivorsContext NICHT löschen, damit Scoreboard sichtbar bleibt bis Spieler bewusst zurückkehrt
        plugin.getLogger().info("Game stopped");
    }

    /**
     * Pause triggered when a GUI (e.g., LevelUpMenu) needs player choice time.
     * Multiple pauses stack and only when all are resumed the game continues.
     */
    @SuppressWarnings("unused")
    public synchronized void pauseForGui() {
        pauseCounter++;
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
            if (currentWaveTask != null) currentWaveTask.cancel();
            spawnManager.pauseContinuous();
            abilityManager.stop();
            if (xpHudTask != null) xpHudTask.cancel();
            plugin.getLogger().info("Game paused for GUI (pauseCount=" + pauseCounter + ")");
        }
    }

    @SuppressWarnings("unused")
    public synchronized void resumeFromGui() {
        if (pauseCounter > 0) pauseCounter--;
        if (pauseCounter == 0 && state == GameState.PAUSED) {
            state = GameState.RUNNING;
            abilityManager.start();
            startHudTask();
            boolean continuous = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);
            if (continuous) {
                spawnManager.resumeContinuous();
            } else {
                startWaveTask();
            }
            plugin.getLogger().info("Game resumed from GUI");
        } else {
            plugin.getLogger().info("Resume requested but pauseCount=" + pauseCounter + ", state=" + state);
        }
    }

    // --- per-player pause (local pause) ---
    public synchronized void pauseForPlayer(java.util.UUID playerUuid) {
        if (playerUuid == null) return;
        pausedPlayers.add(playerUuid);
        // send player a notice/UI if online
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline()) {
            try {
                // show Title + Subtitle (Adventure Title API)
                net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(15), java.time.Duration.ofMillis(250));
                net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("Auswahl", net.kyori.adventure.text.format.NamedTextColor.YELLOW), net.kyori.adventure.text.Component.text("Wähle dein Powerup... (Spiel pausiert für dich)", net.kyori.adventure.text.format.NamedTextColor.GRAY), times);
                p.showTitle(title);
            } catch (Throwable ignored) {}
            // freeze nearby wave mobs relative to this player so they don't move toward them
            try {
                spawnManager.freezeMobsForPlayer(playerUuid, p.getLocation(), plugin.getConfigUtil().getDouble("spawn.freeze-radius", 10.0));
            } catch (Throwable ignored) {}
            // schedule countdown + auto-resume
            try {
                int maxSeconds = plugin.getConfigUtil().getInt("levelup.choice-max-seconds", 20);
                // cancel existing
                org.bukkit.scheduler.BukkitTask prev = pauseTimeoutTasks.remove(playerUuid);
                if (prev != null) prev.cancel();
                final int[] remaining = new int[]{Math.max(1, maxSeconds)};
                org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    try {
                        if (!p.isOnline() || !pausedPlayers.contains(playerUuid)) {
                            // cancel
                            org.bukkit.scheduler.BukkitTask t = pauseTimeoutTasks.remove(playerUuid);
                            if (t != null) t.cancel();
                            return;
                        }
                        // show ActionBar countdown
                        p.sendActionBar(Component.text("Wähle deine Belohnung — Zeit übrig: " + remaining[0] + "s"));
                        if (remaining[0] <= 0) {
                            org.bukkit.scheduler.BukkitTask t = pauseTimeoutTasks.remove(playerUuid);
                            if (t != null) t.cancel();
                            // auto-resume this player and try to open next queued GUI (if any)
                            resumeForPlayer(playerUuid);
                            tryOpenNextQueuedDelayed(playerUuid);
                        }
                        remaining[0]--;
                    } catch (Throwable ignored) {}
                }, 0L, 20L);
                pauseTimeoutTasks.put(playerUuid, task);
            } catch (Throwable ignored) {}
        }
    }

    public synchronized void resumeForPlayer(java.util.UUID playerUuid) {
        if (playerUuid == null) return;
        pausedPlayers.remove(playerUuid);
        // cancel timeout task if present
        try {
            org.bukkit.scheduler.BukkitTask t = pauseTimeoutTasks.remove(playerUuid);
            if (t != null) t.cancel();
        } catch (Throwable ignored) {}
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline()) {
            try {
                net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(250));
                net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("Fortgesetzt", net.kyori.adventure.text.format.NamedTextColor.GREEN), net.kyori.adventure.text.Component.text("Viel Erfolg!", net.kyori.adventure.text.format.NamedTextColor.GRAY), times);
                p.showTitle(title);
            } catch (Throwable ignored) {}
            try {
                spawnManager.unfreezeMobsForPlayer(playerUuid);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Temporarily protect a player from incoming damage. Duration is in ticks (20 ticks = 1s).
     */
    public synchronized void protectPlayer(java.util.UUID playerUuid, int ticks) {
        if (playerUuid == null || ticks <= 0) return;
        try {
            long until = System.currentTimeMillis() + (long)ticks * 50L;
            protectedUntil.put(playerUuid, until);
            // cancel any existing task
            try {
                org.bukkit.scheduler.BukkitTask old = protectTasks.remove(playerUuid);
                if (old != null) old.cancel();
            } catch (Throwable ignored) {}
            org.bukkit.scheduler.BukkitTask t = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { Long cur = protectedUntil.get(playerUuid); if (cur != null && cur <= System.currentTimeMillis()) protectedUntil.remove(playerUuid); } catch (Throwable ignored) {}
                try { protectTasks.remove(playerUuid); } catch (Throwable ignored) {}
            }, ticks);
            protectTasks.put(playerUuid, t);
        } catch (Throwable ignored) {}
    }

    public boolean isPlayerTemporarilyProtected(java.util.UUID playerUuid) {
        if (playerUuid == null) return false;
        Long until = protectedUntil.get(playerUuid);
        return until != null && until > System.currentTimeMillis();
    }

    public synchronized boolean isPlayerPaused(java.util.UUID playerUuid) {
        return playerUuid != null && pausedPlayers.contains(playerUuid);
    }

    public GameState getState() {
        return state;
    }

    public void nextWave(int waveNumber) {
        boolean continuous = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);
        if (continuous) {
            // In continuous mode, waves are not used; keep for compatibility/logging only.
            Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text("Time " + getCurrentWaveNumber() + "m running..."));
        } else {
            spawnManager.spawnWave(waveNumber);
            Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text("Wave " + waveNumber + " started!"));
        }
    }

    // Zusätzlich Exporte, falls andere Klassen Zugriff benötigen
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public MinecraftSurvivors getPlugin() {
        return plugin;
    }

    /**
     * Lädt die Config neu und wendet relevante Einstellungen an (z. B. AbilityManager neu starten).
     */
    public void reloadConfigAndApply() {
        plugin.getConfigUtil().reload();
        plugin.getLogger().info("Config reloaded via command");
        // Wenn das Spiel läuft, restartet die Ability-Task, damit neue Intervalle greifen
        if (state == GameState.RUNNING) {
            abilityManager.stop();
            abilityManager.start();
            plugin.getLogger().info("AbilityManager restarted to apply new config");
        }
    }

    public synchronized void incrementWaveNumber() {
        currentWaveNumber++;
    }

    public synchronized int getCurrentWaveNumber() {
        return currentWaveNumber;
    }

    public synchronized void setCurrentWaveNumber(int n) { this.currentWaveNumber = n; }

    /**
     * Abort a running start countdown (if any). Used when players leave or become unready.
     */
    public synchronized void abortStartCountdown(String reason) {
        if (countdownTask != null) {
            try {
                countdownTask.cancel();
            } catch (Throwable ignored) {}
            countdownTask = null;
        }
        starting = false;
        plugin.getLogger().info("Start countdown aborted: " + (reason == null ? "unknown" : reason));
        try {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                try { p.sendActionBar(net.kyori.adventure.text.Component.text("Start abgebrochen: " + (reason == null ? "" : reason)).color(net.kyori.adventure.text.format.NamedTextColor.RED)); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unused")
    public synchronized void startGameWithCountdown(int seconds) {
        if (starting || state == GameState.RUNNING) {
            plugin.getLogger().info("Start requested but game already starting/running");
            return;
        }
        starting = true;
        final int total = Math.max(1, seconds);
        final int[] remaining = { total };
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                // compute ready players: must have selected class, set ready, and not be locally paused
                java.util.Set<java.util.UUID> ready = new java.util.HashSet<>();
                java.util.List<org.bukkit.entity.Player> online = new java.util.ArrayList<>();
                for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                    online.add(op);
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer osp = plugin.getPlayerManager().get(op.getUniqueId());
                    if (osp != null && osp.getSelectedClass() != null && osp.isReady() && !isPlayerPaused(op.getUniqueId())) ready.add(op.getUniqueId());
                }

                boolean allReady = online.isEmpty() || ready.size() == online.size();

                // Visual feedback per-player
                for (org.bukkit.entity.Player p : online) {
                    try {
                        if (allReady) {
                            net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(80), java.time.Duration.ofMillis(900), java.time.Duration.ofMillis(20));
                            net.kyori.adventure.title.Title t = net.kyori.adventure.title.Title.title(
                                    net.kyori.adventure.text.Component.text(String.valueOf(remaining[0]), net.kyori.adventure.text.format.NamedTextColor.GOLD),
                                    net.kyori.adventure.text.Component.text("Spiel startet...", net.kyori.adventure.text.format.NamedTextColor.GRAY), times);
                            p.showTitle(t);
                        } else {
                            p.sendActionBar(net.kyori.adventure.text.Component.text("Warte auf Ready... " + remaining[0] + "s", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                        }
                        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.9f); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                }

                if (!allReady) {
                    // Abort countdown if any player became unready/paused
                    try {
                        for (org.bukkit.entity.Player p : online) {
                            try { p.sendActionBar(net.kyori.adventure.text.Component.text("Countdown abgebrochen: Spieler nicht bereit").color(net.kyori.adventure.text.format.NamedTextColor.RED)); } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                    org.bukkit.scheduler.BukkitTask t = countdownTask; if (t != null) t.cancel();
                    countdownTask = null;
                    starting = false;
                    plugin.getLogger().info("Start countdown aborted because not all players are ready/unpaused");
                    return;
                }

                if (remaining[0] <= 0) {
                    try {
                        for (org.bukkit.entity.Player p : online) {
                            try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f); } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                    org.bukkit.scheduler.BukkitTask t = countdownTask; if (t != null) t.cancel();
                    countdownTask = null;
                    starting = false;
                    startGame();
                    return;
                }
                remaining[0]--;
            } catch (Throwable ignored) {}
        }, 0L, 20L);
    }

    // Party-Start-Abstimmung State
    private static final class PartyVoteState {
        final java.util.Set<java.util.UUID> members;
        final java.util.Set<java.util.UUID> yes = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        final java.util.Set<java.util.UUID> no = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        org.bukkit.scheduler.BukkitTask timeoutTask;
        PartyVoteState(java.util.Set<java.util.UUID> members) { this.members = members; }
    }
    private final java.util.Map<java.util.UUID, PartyVoteState> partyVotes = new java.util.concurrent.ConcurrentHashMap<>();

    // Neue Phase nach erfolgreichem Ready-Check: Klassenwahl überwachen
    private static final class PartyStartState {
        final java.util.UUID leader;
        final java.util.Set<java.util.UUID> members; // alle beteiligten Spieler
        org.bukkit.scheduler.BukkitTask pollTask;
        PartyStartState(java.util.UUID leader, java.util.Set<java.util.UUID> members) { this.leader = leader; this.members = members; }
    }
    private final java.util.Map<java.util.UUID, PartyStartState> partyClassSelection = new java.util.concurrent.ConcurrentHashMap<>();

    private void beginClassSelectionForParty(java.util.UUID leader, java.util.Set<java.util.UUID> members) {
        if (leader == null || members == null || members.isEmpty()) return;
        // Offene frühere Zustände bereinigen
        PartyStartState old = partyClassSelection.remove(leader);
        if (old != null && old.pollTask != null) try { old.pollTask.cancel(); } catch (Throwable ignored) {}
        PartyStartState st = new PartyStartState(leader, new java.util.HashSet<>(members));
        partyClassSelection.put(leader, st);
        // Öffne Klassenwahl für alle
        for (java.util.UUID u : st.members) {
            org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
            if (pl != null && pl.isOnline()) {
                try { enterSurvivorsContext(u); } catch (Throwable ignored) {}
                try { plugin.getGuiManager().openClassSelection(pl); } catch (Throwable ignored) {}
                try { pl.sendMessage(net.kyori.adventure.text.Component.text("Wähle deine Klasse – Start sobald alle gewählt haben").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)); } catch (Throwable ignored) {}
            }
        }
        // Poll alle 10 Ticks
        st.pollTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                // Wenn Leader-Map nicht mehr existiert, abbrechen
                PartyStartState cur = partyClassSelection.get(leader);
                if (cur == null) { if (st.pollTask != null) st.pollTask.cancel(); return; }
                // Prüfe Online/Abbruch
                for (java.util.UUID u : new java.util.HashSet<>(cur.members)) {
                    org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                    if (pl == null || !pl.isOnline()) {
                        // Abbrechen und informieren
                        partyClassSelection.remove(leader);
                        if (st.pollTask != null) st.pollTask.cancel();
                        for (java.util.UUID m : cur.members) {
                            org.bukkit.entity.Player op = org.bukkit.Bukkit.getPlayer(m);
                            if (op != null && op.isOnline()) op.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgebrochen: Spieler offline").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                        }
                        return;
                    }
                }
                // Zähle Klassenauswahlen
                int total = cur.members.size();
                int selected = 0;
                for (java.util.UUID u : cur.members) {
                    var sp = playerManager.get(u);
                    if (sp != null && sp.getSelectedClass() != null) selected++;
                }
                // ActionBar Status an alle Mitglieder senden
                for (java.util.UUID u : cur.members) {
                    org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                    if (pl != null && pl.isOnline()) {
                        try { pl.sendActionBar(net.kyori.adventure.text.Component.text("Klassen gewählt: " + selected + "/" + total, net.kyori.adventure.text.format.NamedTextColor.GOLD)); } catch (Throwable ignored) {}
                    }
                }
                // Prüfe vollständige Klassenauswahl
                if (selected >= total) {
                    partyClassSelection.remove(leader);
                    if (st.pollTask != null) st.pollTask.cancel();
                    // Runde direkt starten (kein Countdown)
                    startGame();
                }
            } catch (Throwable ignored) {}
        }, 0L, 10L);
    }

    public synchronized void beginPartyStartVote(org.bysenom.minecraftSurvivors.manager.PartyManager.Party party, int seconds) {
        if (party == null) return; java.util.UUID leader = party.getLeader(); if (leader == null) return; if (partyVotes.containsKey(leader)) return;
        java.util.Set<java.util.UUID> online = new java.util.HashSet<>(plugin.getPartyManager().onlineMembers(party)); if (online.isEmpty()) return;
        PartyVoteState vs = new PartyVoteState(online); partyVotes.put(leader, vs); int sec = Math.max(5, seconds);
        for (java.util.UUID u : online) {
            org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u); if (pl == null || !pl.isOnline()) continue;
            try {
                org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Party Start bestätigen"));
                java.util.List<net.kyori.adventure.text.Component> loreY = java.util.List.of(net.kyori.adventure.text.Component.text("Starte jetzt die Runde").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                java.util.List<net.kyori.adventure.text.Component> loreN = java.util.List.of(net.kyori.adventure.text.Component.text("Abbrechen").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                inv.setItem(3, org.bysenom.minecraftSurvivors.gui.GuiTheme.createAction(plugin, org.bukkit.Material.LIME_WOOL, net.kyori.adventure.text.Component.text("Bestätigen").color(net.kyori.adventure.text.format.NamedTextColor.GREEN), loreY, "party_vote_yes:"+leader, false));
                inv.setItem(5, org.bysenom.minecraftSurvivors.gui.GuiTheme.createAction(plugin, org.bukkit.Material.RED_WOOL, net.kyori.adventure.text.Component.text("Ablehnen").color(net.kyori.adventure.text.format.NamedTextColor.RED), loreN, "party_vote_no:"+leader, false));
                pl.openInventory(inv);
                try { pl.sendMessage(net.kyori.adventure.text.Component.text("Bestätige den Start ("+sec+"s)").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
        vs.timeoutTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PartyVoteState cur = partyVotes.remove(leader); if (cur != null) {
                for (java.util.UUID u : cur.members) { org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u); if (pl != null && pl.isOnline()) pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgebrochen (Zeitüberschreitung)").color(net.kyori.adventure.text.format.NamedTextColor.RED)); }
            }
        }, 20L * sec);
        // initial bar update
        updatePartyVoteBar(leader, sec);
        // schedule per-second updates
        new org.bukkit.scheduler.BukkitRunnable(){ int remain = sec; @Override public void run(){
            if (!partyVotes.containsKey(leader)) { clearPartyVoteBar(leader); cancel(); return; }
            if (remain <= 0) { clearPartyVoteBar(leader); cancel(); return; }
            updatePartyVoteBar(leader, remain--);
        }}.runTaskTimer(plugin, 0L, 20L);
    }

    public synchronized void handlePartyVote(java.util.UUID leader, java.util.UUID member, boolean accept) {
        PartyVoteState vs = partyVotes.get(leader); if (vs == null) return; if (!vs.members.contains(member)) return;
        if (accept) vs.yes.add(member); else vs.no.add(member);
        // Schließe GUI beim Klickenden
        try { org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(member); if (pl != null) pl.closeInventory(); } catch (Throwable ignored) {}
        updatePartyVoteBar(leader, 0);
        if (!vs.no.isEmpty()) { clearPartyVoteBar(leader); /* Abbruch: gezielte Info an Leader, wer abgelehnt hat */ try {
            org.bukkit.entity.Player leaderPl = org.bukkit.Bukkit.getPlayer(leader);
            org.bukkit.entity.Player memberPl = org.bukkit.Bukkit.getPlayer(member);
            String name = memberPl != null ? memberPl.getName() : member.toString();
            if (leaderPl != null && leaderPl.isOnline()) {
                leaderPl.sendMessage(net.kyori.adventure.text.Component.text("Abgelehnt von " + name).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        } catch (Throwable ignored) {}
            // Allgemeine Abbruch-Meldung
            partyVotes.remove(leader); if (vs.timeoutTask != null) try { vs.timeoutTask.cancel(); } catch (Throwable ignored) {}
            for (java.util.UUID u : vs.members) { org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u); if (pl != null && pl.isOnline()) pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgelehnt").color(net.kyori.adventure.text.format.NamedTextColor.RED)); }
            return;
        }
        if (vs.yes.containsAll(vs.members)) { clearPartyVoteBar(leader); /* Ready-Check bestanden -> Wechsel in Klassenwahl-Phase */ partyVotes.remove(leader); if (vs.timeoutTask != null) try { vs.timeoutTask.cancel(); } catch (Throwable ignored) {}
            beginClassSelectionForParty(leader, vs.members);
        }
    }

    public synchronized void handlePlayerQuit(java.util.UUID quitting) {
        if (quitting == null) return; java.util.List<java.util.UUID> affectedLeaders = new java.util.ArrayList<>();
        for (var en : partyVotes.entrySet()) { java.util.UUID leader = en.getKey(); PartyVoteState vs = en.getValue(); if (vs == null) continue; if (leader.equals(quitting) || vs.members.contains(quitting)) affectedLeaders.add(leader); }
        for (java.util.UUID leader : affectedLeaders) {
            PartyVoteState vs = partyVotes.remove(leader); if (vs == null) continue; if (vs.timeoutTask != null) try { vs.timeoutTask.cancel(); } catch (Throwable ignored) {}
            for (java.util.UUID u : vs.members) {
                org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                if (pl != null && pl.isOnline()) {
                    pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgebrochen (Mitglied offline)").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    try { pl.closeInventory(); } catch (Throwable ignored) {}
                }
            }
        }
        // Falls eine Klassenwahl-Phase lief, ebenso abbrechen
        java.util.List<java.util.UUID> affectedClassLeaders = new java.util.ArrayList<>();
        for (var en : partyClassSelection.entrySet()) {
            var st = en.getValue(); if (st == null) continue;
            if (st.leader.equals(quitting) || st.members.contains(quitting)) affectedClassLeaders.add(en.getKey());
        }
        for (java.util.UUID leader : affectedClassLeaders) {
            PartyStartState st = partyClassSelection.remove(leader);
            if (st != null && st.pollTask != null) try { st.pollTask.cancel(); } catch (Throwable ignored) {}
            for (java.util.UUID u : st.members) {
                org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                if (pl != null && pl.isOnline()) pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgebrochen (Mitglied offline)").color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }

    public void trySoloAutoStart(org.bukkit.entity.Player starter) {
        if (starter == null) return;
        try {
            org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
            org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(starter.getUniqueId()) : null;
            if (party == null && state != GameState.RUNNING && !starting) {
                // Solo: wenn nur 1 Spieler im Survivors-Kontext und Klasse gewählt, starte sofort
                int relevant = 0;
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (isInSurvivorsContext(p.getUniqueId())) relevant++;
                }
                var sp = playerManager.get(starter.getUniqueId());
                if (sp != null && sp.getSelectedClass() != null && relevant <= 1) {
                    startGame();
                }
            }
        } catch (Throwable ignored) {}
    }

    private final java.util.Map<java.util.UUID, org.bukkit.boss.BossBar> partyVoteBars = new java.util.concurrent.ConcurrentHashMap<>();
    private void updatePartyVoteBar(java.util.UUID leader, int secondsLeft) {
        PartyVoteState vs = partyVotes.get(leader); if (vs == null) return;
        int total = vs.members.size(); int yesC = vs.yes.size(); int noC = vs.no.size();
        double prog = total <= 0 ? 0.0 : (double) yesC / (double) total;
        org.bukkit.boss.BossBar bar = partyVoteBars.computeIfAbsent(leader, l -> org.bukkit.Bukkit.createBossBar(org.bukkit.NamespacedKey.minecraft("ms_party_vote_"+l.toString().substring(0,8)), "Party Ready", org.bukkit.boss.BarColor.BLUE, org.bukkit.boss.BarStyle.SEGMENTED_10));
        bar.setProgress(Math.max(0.0, Math.min(1.0, prog)));
        bar.setTitle("Ready: " + yesC + "/" + total + " | Nein:" + noC + " | " + secondsLeft + "s");
        for (java.util.UUID u : vs.members) { org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(u); if (p!=null && p.isOnline()) bar.addPlayer(p); }
    }
    private void clearPartyVoteBar(java.util.UUID leader) {
        org.bukkit.boss.BossBar bar = partyVoteBars.remove(leader); if (bar != null) bar.removeAll();
    }
}
