package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final org.bysenom.minecraftSurvivors.util.PlayerDataManager dataManager;

    public PlayerDeathListener(GameManager gameManager, PlayerManager playerManager, org.bysenom.minecraftSurvivors.util.PlayerDataManager dataManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        if (gameManager.getState() == GameState.RUNNING) {
            // Spiel stoppen, Spieler-Daten aufräumen und Broadcast
            // Reset XP but preserve coins: save coins to disk first
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(player.getUniqueId());
            if (sp != null) {
                // reset XP
                sp.setXp(0);
                sp.setXpToNext(Math.max(1, 5 * sp.getClassLevel()));
                // persist coins (and other permanent fields) via dataManager if available
                try {
                    if (dataManager != null) dataManager.saveCoins(sp.getUuid(), sp.getCoins());
                } catch (Throwable ignored) {}
            }
            gameManager.stopGame();
            if (playerManager != null) {
                playerManager.remove(player.getUniqueId());
            }
            Bukkit.getServer().sendMessage(Component.text("§cDie Wave endet — Spieler " + player.getName() + " ist gestorben."));
        }
    }
}
