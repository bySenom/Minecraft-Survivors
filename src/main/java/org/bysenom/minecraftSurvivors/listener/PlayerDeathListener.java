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

    public PlayerDeathListener(GameManager gameManager, PlayerManager playerManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        if (gameManager.getState() == GameState.RUNNING) {
            // Spiel stoppen, Spieler-Daten aufräumen und Broadcast
            gameManager.stopGame();
            if (playerManager != null) {
                playerManager.remove(player.getUniqueId());
            }
            Bukkit.getServer().sendMessage(Component.text("§cDie Wave endet — Spieler " + player.getName() + " ist gestorben."));
        }
    }
}
