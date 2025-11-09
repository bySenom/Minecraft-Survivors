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
                try { if (plugin.getGuiManager() != null) plugin.getGuiManager().openLevelUpMenu(p, Math.max(1, req.level)); } catch (Throwable ignored) {}
            }
            case LOOT_CHEST -> {
                try { LootchestListener.openQueued(p); } catch (Throwable ignored) {}
            }
        }
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
            try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable ignored) {}
            // Klassenfähigkeit nur erzwingen, falls Spiel bereits läuft (RUNNING) und Klasse gewählt ist
            if (state == GameState.RUNNING) {
                try { ensureClassAbility(uuid); } catch (Throwable ignored) {}
            }
            try { giveInitialKit(p); } catch (Throwable ignored) {}
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
                    int currentXp = sp.getXp();
                    int xpToNext = sp.getXpToNext();
                    p.sendActionBar(Component.text("XP: " + currentXp + "/" + xpToNext + " • Lvl " + sp.getClassLevel()));
                } catch (Throwable ignored) {}
            }
            // BossManager tick
            try { bossManager.tick(); } catch (Throwable ignored) {}
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
                try { plugin.getPlayerDataManager().saveAsync(sp); } catch (Throwable ignored) {}
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
        } catch (Throwable ignored) {}
        try { p.updateInventory(); } catch (Throwable ignored) {}
    }

    public synchronized void startGame() {
        if (state == GameState.RUNNING) return;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        starting = false;
        state = GameState.RUNNING;
        playerManager.resetAllPreserveSkills();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            try {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
                plugin.getMetaManager().applyMetaOnRunStart(p, sp);
                // Beim Start sicherstellen, dass alle aktiven Spieler im Kontext sind
                enterSurvivorsContext(p.getUniqueId());
            } catch (Throwable ignored) {}
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
        try { plugin.getShopNpcManager().spawnConfigured(); } catch (Throwable ignored) {}
        // Survivors-HUD einschalten: Scoreboard-Update triggern, Bossbars werden im StatsDisplayManager-Tick nur bei RUNNING gezeigt
        try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable ignored) {}
        plugin.getLogger().info("Game started");
    }

    public synchronized void stopGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;
        if (currentWaveTask != null) currentWaveTask.cancel();
        spawnManager.stopContinuous();
        spawnManager.clearWaveMobs();
        abilityManager.stop();
        try { plugin.getSkillManager().clearLingeringEffects(); } catch (Throwable ignored) {}
        if (xpHudTask != null) xpHudTask.cancel();
        try { plugin.getShopNpcManager().despawnAll(); } catch (Throwable ignored) {}
        // Scoreboard soll nach Tod bestehen bleiben -> Tasks beenden aber nicht resetten
        try { plugin.getScoreboardManager().forceUpdateAll(); } catch (Throwable ignored) {}
        try { plugin.getStatsDisplayManager().clearAllBossbarsNow(); } catch (Throwable ignored) {}
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

    /**
     * Öffnet das nächste wartende GUI (LevelUp / LootChest) mit 1 Tick Verzögerung.
     * Öffentlich gemacht für GuiManager, um nach Resume/Schließen weitere Warteschlangen-Elemente anzuzeigen.
     * Falls der Spieler offline ist oder die Queue leer bleibt ein No-Op.
     */
    public void tryOpenNextQueuedDelayed(java.util.UUID uuid) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> tryOpenNextQueued(uuid), 1L);
    }

    private boolean isCountdownRunning() { return countdownTask != null; }

    public synchronized void requestAutoStartIfAllReady() {
        if (state == GameState.RUNNING || starting) return;
        // Sammle Online Spieler im Survivors Kontext
        java.util.List<org.bukkit.entity.Player> online = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;
        int totalRelevant = 0;
        int readyCount = 0;
        for (org.bukkit.entity.Player p : online) {
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            if (sp == null) continue;
            // Berücksichtige nur Spieler, die den Survivors-Kontext betreten haben (enterSurvivorsContext), optional könnte man scoreboard checken
            if (!isInSurvivorsContext(p.getUniqueId())) continue;
            totalRelevant++;
            if (sp.getSelectedClass() != null && sp.isReady()) readyCount++;
        }
        if (totalRelevant == 0) return; // niemand im Kontext
        if (readyCount < totalRelevant) return; // nicht alle bereit
        // alle bereit -> Countdown starten falls nicht aktiv
        if (!isCountdownRunning()) {
            int cd = Math.max(3, plugin.getConfigUtil().getInt("lobby.autostart-countdown-seconds", 5));
            startGameWithCountdown(cd);
        }
    }

    // Party-Start-Abstimmung
    private static final class PartyVoteState {
        final java.util.Set<java.util.UUID> members;
        final java.util.Set<java.util.UUID> yes = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        final java.util.Set<java.util.UUID> no = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        org.bukkit.scheduler.BukkitTask timeoutTask;
        PartyVoteState(java.util.Set<java.util.UUID> members) { this.members = members; }
    }
    private final java.util.Map<java.util.UUID, PartyVoteState> partyVotes = new java.util.concurrent.ConcurrentHashMap<>(); // key: leader UUID

    public synchronized void beginPartyStartVote(org.bysenom.minecraftSurvivors.manager.PartyManager.Party party, int seconds) {
        if (party == null) return;
        java.util.UUID leader = party.getLeader();
        if (leader == null) return;
        if (partyVotes.containsKey(leader)) return; // bereits laufend
        java.util.Set<java.util.UUID> online = new java.util.HashSet<>(plugin.getPartyManager().onlineMembers(party));
        if (online.isEmpty()) return;
        PartyVoteState vs = new PartyVoteState(online);
        partyVotes.put(leader, vs);
        int sec = Math.max(5, seconds);
        // GUI öffnen für alle Mitglieder
        for (java.util.UUID u : online) {
            org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
            if (pl == null || !pl.isOnline()) continue;
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
        // Timeout
        vs.timeoutTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PartyVoteState cur = partyVotes.remove(leader);
            if (cur != null) {
                for (java.util.UUID u : cur.members) {
                    org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                    if (pl != null && pl.isOnline()) pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgebrochen (Zeitüberschreitung)").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                }
            }
        }, 20L * sec);
    }

    public synchronized void handlePartyVote(java.util.UUID leader, java.util.UUID member, boolean accept) {
        PartyVoteState vs = partyVotes.get(leader);
        if (vs == null) return;
        if (!vs.members.contains(member)) return;
        if (accept) vs.yes.add(member); else vs.no.add(member);
        // Schließe GUI
        try { org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(member); if (pl != null) pl.closeInventory(); } catch (Throwable ignored) {}
        if (!vs.no.isEmpty()) {
            // Abbruch
            partyVotes.remove(leader);
            if (vs.timeoutTask != null) try { vs.timeoutTask.cancel(); } catch (Throwable ignored) {}
            for (java.util.UUID u : vs.members) {
                org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                if (pl != null && pl.isOnline()) pl.sendMessage(net.kyori.adventure.text.Component.text("Party-Start abgelehnt").color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
            return;
        }
        if (vs.yes.containsAll(vs.members)) {
            // Alle bestätigt -> Start erzwingen
            partyVotes.remove(leader);
            if (vs.timeoutTask != null) try { vs.timeoutTask.cancel(); } catch (Throwable ignored) {}
            int cd = Math.max(1, plugin.getConfigUtil().getInt("lobby.party-countdown-seconds", 5));
            startGameWithCountdownForce(cd);
        }
    }

    public synchronized void startGameWithCountdownForce(int seconds) {
        if (starting || state == GameState.RUNNING) return;
        starting = true;
        final int total = Math.max(1, seconds);
        final int[] remaining = { total };
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                java.util.List<org.bukkit.entity.Player> online = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
                for (org.bukkit.entity.Player p : online) {
                    try {
                        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(80), java.time.Duration.ofMillis(900), java.time.Duration.ofMillis(20));
                        net.kyori.adventure.title.Title t = net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text(String.valueOf(remaining[0]), net.kyori.adventure.text.format.NamedTextColor.GOLD),
                                net.kyori.adventure.text.Component.text("Spiel startet...", net.kyori.adventure.text.format.NamedTextColor.GRAY), times);
                        p.showTitle(t);
                        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.9f); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                }
                if (remaining[0] <= 0) {
                    try { for (org.bukkit.entity.Player p : online) { try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f); } catch (Throwable ignored) {} } } catch (Throwable ignored) {}
                    org.bukkit.scheduler.BukkitTask t = countdownTask; if (t != null) t.cancel();
                    countdownTask = null; starting = false; startGame(); return;
                }
                remaining[0]--;
            } catch (Throwable ignored) {}
        }, 0L, 20L);
    }

    // Helfer: Solo Auto-Start
    public void trySoloAutoStart(org.bukkit.entity.Player starter) {
        if (starter == null) return;
        try {
            org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
            org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(starter.getUniqueId()) : null;
            boolean enabled = plugin.getConfigUtil().getBoolean("lobby.solo-autostart.enabled", true);
            if (party == null && enabled) {
                int cd = Math.max(1, plugin.getConfigUtil().getInt("lobby.solo-countdown-seconds", 3));
                startGameWithCountdownForce(cd);
            }
        } catch (Throwable ignored) {}
    }
}
