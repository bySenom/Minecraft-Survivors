package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.ability.ShamanAbility;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private BukkitRunnable task;
    private final ShamanAbility shamanAbility;

    public AbilityManager(MinecraftSurvivors plugin, PlayerManager playerManager, SpawnManager spawnManager, GameManager gameManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.gameManager = gameManager;
        this.shamanAbility = new ShamanAbility(plugin, spawnManager);
    }

    public synchronized void start() {
        if (task != null) return;
        plugin.getLogger().info("AbilityManager starting task");
        // synchrone Task auf dem Haupt-Thread: Intervall aus config (default 30 ticks)
        int intervalTicks = plugin.getConfigUtil().getInt("ability.interval-ticks", 30);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        };
        task.runTaskTimer(plugin, 0L, intervalTicks);
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("AbilityManager stopped task");
        }
    }

    private void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            if (sp == null) continue;
            // skip players who are paused (choosing a level/powerup)
            try {
                if (gameManager != null && gameManager.isPlayerPaused(p.getUniqueId())) continue;
            } catch (Throwable ignored) {}
            if (sp.getSelectedClass() == PlayerClass.SHAMAN) {
                shamanAbility.tick(p, sp);
            }
            // weitere Klassen hier ergänzen
        }
    }
}
