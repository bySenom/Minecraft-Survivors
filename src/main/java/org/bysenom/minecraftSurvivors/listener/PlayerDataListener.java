package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bysenom.minecraftSurvivors.util.PlayerDataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerDataListener implements Listener {

    private final PlayerDataManager dataManager;
    private final PlayerManager playerManager;

    public PlayerDataListener(PlayerDataManager dataManager, PlayerManager playerManager) {
        this.dataManager = dataManager;
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        SurvivorPlayer loaded = dataManager.load(uuid);
        if (loaded != null) {
            SurvivorPlayer sp = playerManager.get(uuid);
            sp.setKills(loaded.getKills());
            sp.setCoins(loaded.getCoins());
            sp.setSelectedClass(loaded.getSelectedClass());
            sp.setClassLevel(loaded.getClassLevel());
            sp.setXp(loaded.getXp());
            sp.setXpToNext(loaded.getXpToNext());
            sp.setBonusDamage(loaded.getBonusDamage());
            sp.setBonusStrikes(loaded.getBonusStrikes());
            sp.setFlatDamage(loaded.getFlatDamage());
            sp.setExtraHearts(loaded.getExtraHearts());
            e.getPlayer().sendMessage(Component.text("§aPlayer data loaded (Level " + sp.getClassLevel() + ")"));
        } else {
            e.getPlayer().sendMessage(Component.text("§eNo previous player data found. New profile created."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        SurvivorPlayer sp = playerManager.get(uuid);
        if (sp != null) {
            dataManager.save(sp);
        }
    }
}
