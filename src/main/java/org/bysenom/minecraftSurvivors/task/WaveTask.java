// File: src/main/java/org/bysenom/minecraftSurvivors/task/WaveTask.java
package org.bysenom.minecraftSurvivors.task;

import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class WaveTask extends BukkitRunnable {

    private final MinecraftSurvivors plugin;
    private final GameManager gameManager;
    private int wave;

    public WaveTask(MinecraftSurvivors plugin, GameManager gameManager, int startWave) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.wave = startWave;
    }

    @Override
    public void run() {
        if (gameManager.getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING) {
            cancel();
            return;
        }
        gameManager.nextWave(wave);
        // ensure GameManager's currentWaveNumber stays in sync
        gameManager.incrementWaveNumber();
        wave = gameManager.getCurrentWaveNumber();
    }
}
