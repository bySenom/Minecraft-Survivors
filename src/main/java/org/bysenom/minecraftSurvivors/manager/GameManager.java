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
    private final SpawnManager spawnManager;
    private final AbilityManager abilityManager;
    private GameState state = GameState.LOBBY;
    private WaveTask currentWaveTask;
    private BukkitTask xpHudTask;
    private int currentWaveNumber = 1;
    private int pauseCounter = 0; // counts GUI pauses (e.g., multiple players)
    private final java.util.Set<java.util.UUID> pausedPlayers = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> pauseTimeoutTasks = new java.util.concurrent.ConcurrentHashMap<>();

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
        state = GameState.RUNNING;
        playerManager.resetAll();
        this.currentWaveNumber = 1;
        // Debug: falls ein Spieler noch keine Klasse gewählt hat, setze Shaman als Default, damit Abilities sichtbar werden.
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            if (sp.getSelectedClass() == null) {
                sp.setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN);
            }
        }
        startWaveTask();
        // Ability-Task starten (z.\u00a0B. Shaman-Blitz alle 1.5s)
        abilityManager.start();
        // Start XP-HUD task (shows XP progress on ActionBar periodically)
        startHudTask();
        Bukkit.getLogger().info("Game started");
    }

    public synchronized void stopGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;
        if (currentWaveTask != null) currentWaveTask.cancel();
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
            abilityManager.stop();
            if (xpHudTask != null) xpHudTask.cancel();
            plugin.getLogger().info("Game paused for GUI (pauseCount=" + pauseCounter + ")");
        }
    }

    public synchronized void resumeFromGui() {
        if (pauseCounter > 0) pauseCounter--;
        if (pauseCounter == 0 && state == GameState.PAUSED) {
            state = GameState.RUNNING;
            // restart ability & HUD & waves
            abilityManager.start();
            startHudTask();
            startWaveTask();
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
                // show Title + Subtitle (fadeIn, stay, fadeOut)
                p.sendTitle("§eAuswahl", "§7Wähle dein Powerup... (Spiel pausiert für dich)", 5, 300, 5);
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
                p.sendTitle("§aFortgesetzt", "§7Viel Erfolg!", 5, 60, 5);
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
        spawnManager.spawnWave(waveNumber);
        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text("Wave " + waveNumber + " started!"));
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
}
