// File: src/main/java/org/bysenom/minecraftSurvivors/listener/EntityDeathListener.java
package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.gui.GuiManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import net.kyori.adventure.text.Component;
import org.bysenom.minecraftSurvivors.util.ConfigUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final PlayerManager playerManager;
    private final GuiManager guiManager;
    private final ConfigUtil config;

    public EntityDeathListener(PlayerManager playerManager, GuiManager guiManager, ConfigUtil config) {
        this.playerManager = playerManager;
        this.guiManager = guiManager;
        this.config = config;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() instanceof Player) {
            Player killer = e.getEntity().getKiller();
            SurvivorPlayer sp = playerManager.get(killer.getUniqueId());
            sp.addKill();
            sp.addCoins(1); // einfache Belohnung
            killer.sendActionBar(Component.text("Kills: " + sp.getKills() + "  Coins: " + sp.getCoins()));

            // XP-Verteilung bei Kill: aus der Config lesen (default 1)
            int xpGain = 1;
            try {
                if (config != null) {
                    xpGain = config.getInt("levelup.xp-per-kill", 1);
                }
            } catch (Throwable ignored) {}

            int beforeLevel = sp.getClassLevel();
            boolean leveled = sp.addXp(xpGain);
            int afterLevel = sp.getClassLevel();

            if (leveled) {
                // Wenn mehrere Level auf einmal erreicht wurden, beschreibe kurz die Änderung
                if (guiManager != null) {
                    guiManager.openLevelUpMenu(killer, afterLevel);
                }
                if (afterLevel > beforeLevel) {
                    killer.sendMessage(Component.text("§aLevel up! Du bist jetzt Level " + afterLevel));
                } else {
                    killer.sendMessage(Component.text("§aLevel up!"));
                }
            }
        }
    }
}
