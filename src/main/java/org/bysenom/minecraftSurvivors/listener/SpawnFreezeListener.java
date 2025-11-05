package org.bysenom.minecraftSurvivors.listener;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;

public class SpawnFreezeListener implements Listener {

    private final GameManager gameManager;
    private final SpawnManager spawnManager;

    public SpawnFreezeListener(GameManager gameManager, SpawnManager spawnManager) {
        this.gameManager = gameManager;
        this.spawnManager = spawnManager;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        try {
            // Check nearby players and if any are paused, freeze the mob for those players
            LivingEntity mob = (LivingEntity) e.getEntity();
            Location loc = mob.getLocation();
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                UUID uid = p.getUniqueId();
                if (gameManager.isPlayerPaused(uid)) {
                    double radius = gameManager.getPlugin().getConfigUtil().getDouble("spawn.freeze-radius", 10.0);
                    if (loc.getWorld() == p.getLocation().getWorld() && loc.distanceSquared(p.getLocation()) <= radius * radius) {
                        // schedule a short delayed freeze to ensure mob AI/targeting is initialized
                        org.bukkit.scheduler.BukkitScheduler sched = Bukkit.getScheduler();
                        try {
                            sched.runTaskLater(gameManager.getPlugin(), () -> spawnManager.freezeSingleMobForPlayer(uid, mob), 1L);
                        } catch (Throwable ex) {
                            // fallback to immediate
                            spawnManager.freezeSingleMobForPlayer(uid, mob);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
