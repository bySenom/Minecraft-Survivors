// File: src/main/java/org/bysenom/minecraftSurvivors/manager/GameManager.java
package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.GameState;
import org.bysenom.minecraftSurvivors.task.WaveTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

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

    public GameManager(MinecraftSurvivors plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.spawnManager = new SpawnManager(plugin, playerManager);
        this.abilityManager = new AbilityManager(plugin, playerManager, spawnManager, this);
    }

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
        }, 0L, hudIntervalTicks);
    }

    public synchronized void startGame() {
        if (state == GameState.RUNNING) return;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        starting = false;
        state = GameState.RUNNING;
        playerManager.resetAll();
        this.currentWaveNumber = 1;
        boolean continuous = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);
        if (continuous) {
            spawnManager.startContinuousIfEnabled();
        } else {
            startWaveTask();
        }
        abilityManager.start();
        startHudTask();
        Bukkit.getLogger().info("Game started");
    }

    public synchronized void stopGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;
        if (currentWaveTask != null) currentWaveTask.cancel();
        spawnManager.stopContinuous();
        spawnManager.clearWaveMobs();
        // Ability-Task stoppen
        abilityManager.stop();
        if (xpHudTask != null) xpHudTask.cancel();
        Bukkit.getLogger().info("Game stopped");
    }

    /**
     * Pause triggered when a GUI (e.g., LevelUpMenu) needs player choice time.
     * Multiple pauses stack and only when all are resumed the game continues.
     */
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

    public synchronized void resumeFromGui() {
        if (pauseCounter > 0) pauseCounter--;
        if (pauseCounter == 0 && state == GameState.PAUSED) {
            state = GameState.RUNNING;
            // restart ability & HUD & spawns
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
    @SuppressWarnings("deprecation")
    public synchronized void pauseForPlayer(java.util.UUID playerUuid) {
        if (playerUuid == null) return;
        pausedPlayers.add(playerUuid);
        // send player a notice/UI if online
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline()) {
            try {
                // show Title + Subtitle (Adventure Title API)
                net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.of(java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(15), java.time.Duration.ofMillis(250));
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
                            resumeForPlayer(playerUuid);
                        }
                        remaining[0]--;
                    } catch (Throwable ignored) {}
                }, 0L, 20L);
                pauseTimeoutTasks.put(playerUuid, task);
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("deprecation")
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
                net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.of(java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(250));
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

    public synchronized void setCurrentWaveNumber(int n) {
        this.currentWaveNumber = n;
    }

    public synchronized void startGameWithCountdown(int seconds) {
        if (starting || state == GameState.RUNNING) {
            plugin.getLogger().info("Start requested but game already starting/running");
            return;
        }
        starting = true;
        final int total = Math.max(1, seconds);
        final int[] remaining = { total };
        // Broadcast countdown via Title/ActionBar and tick sound
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    try {
                        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.of(java.time.Duration.ofMillis(80), java.time.Duration.ofMillis(900), java.time.Duration.ofMillis(20));
                        net.kyori.adventure.title.Title t = net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text(String.valueOf(remaining[0]), net.kyori.adventure.text.format.NamedTextColor.GOLD),
                                net.kyori.adventure.text.Component.text("Spiel startet...", net.kyori.adventure.text.format.NamedTextColor.GRAY), times);
                        p.showTitle(t);
                        p.sendActionBar(net.kyori.adventure.text.Component.text("Start in " + remaining[0] + "s"));
                        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.9f); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                }
                if (remaining[0] <= 0) {
                    try {
                        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
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
}
