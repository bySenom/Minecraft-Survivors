// File: src/main/java/org/bysenom/minecraftSurvivors/manager/GameManager.java
package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.GameState;
import org.bysenom.minecraftSurvivors.task.WaveTask;
import org.bukkit.Bukkit;

public class GameManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final SpawnManager spawnManager;
    private final AbilityManager abilityManager;
    private GameState state = GameState.LOBBY;
    private WaveTask currentWaveTask;

    public GameManager(MinecraftSurvivors plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.spawnManager = new SpawnManager(plugin, playerManager);
        this.abilityManager = new AbilityManager(plugin, playerManager, spawnManager);
    }

    public synchronized void startGame() {
        if (state != GameState.LOBBY) return;
        state = GameState.RUNNING;
        playerManager.resetAll();
        // Debug: falls ein Spieler noch keine Klasse gewählt hat, setze Shaman als Default, damit Abilities sichtbar werden.
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            if (sp.getSelectedClass() == null) {
                sp.setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN);
            }
        }
        currentWaveTask = new WaveTask(plugin, this, 1);
        currentWaveTask.runTaskTimer(plugin, 0L, 20L * 10); // jede 10s eine "Wave"-Iteration
        // Ability-Task starten (z.\u00a0B. Shaman-Blitz alle 1.5s)
        abilityManager.start();
        Bukkit.getLogger().info("Game started");
    }

    public synchronized void stopGame() {
        if (state == GameState.ENDED) return;
        state = GameState.ENDED;
        if (currentWaveTask != null) currentWaveTask.cancel();
        spawnManager.clearWaveMobs();
        // Ability-Task stoppen
        abilityManager.stop();
        Bukkit.getLogger().info("Game stopped");
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
}
