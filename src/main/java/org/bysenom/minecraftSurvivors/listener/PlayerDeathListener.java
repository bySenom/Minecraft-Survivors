package org.bysenom.minecraftSurvivors.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.GameState;

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
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(player.getUniqueId());
            if (sp != null) {
                sp.setXp(0);
                sp.setXpToNext(Math.max(1, 5 * sp.getClassLevel()));
            }
            try {
                int minutes = Math.max(1, sp != null ? sp.getClassLevel() : 1);
                org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getMetaManager().awardEndOfRunEssence(player, sp, minutes);
            } catch (Throwable ignored) {}
            // Coins reset per Run
            if (sp != null) sp.setCoins(0);
            // Multiplayer: Run-Ende für alle, und Skills nicht behalten
            try {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer spx = playerManager.get(p.getUniqueId());
                    if (spx != null) spx.softReset(); // Skills & Waffen resetten
                }
            } catch (Throwable ignored) {}
            gameManager.stopGame();
            org.bukkit.Bukkit.getServer().sendMessage(Component.text("§cRun beendet — " + player.getName() + " ist gestorben."));
        }
    }
}
