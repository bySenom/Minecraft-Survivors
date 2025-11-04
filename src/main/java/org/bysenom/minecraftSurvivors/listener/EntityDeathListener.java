// File: src/main/java/org/bysenom/minecraftSurvivors/listener/EntityDeathListener.java
package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final PlayerManager playerManager;

    public EntityDeathListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() instanceof Player) {
            Player killer = e.getEntity().getKiller();
            SurvivorPlayer sp = playerManager.get(killer.getUniqueId());
            sp.addKill();
            sp.addCoins(1); // einfache Belohnung
            killer.sendActionBar(Component.text("Kills: " + sp.getKills() + "  Coins: " + sp.getCoins()));
        }
    }
}
