package org.bysenom.minecraftSurvivors.listener;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bysenom.minecraftSurvivors.util.PlayerDataManager;

public class PlayerDataListener implements Listener {

    private final PlayerDataManager dataManager;
    private final PlayerManager playerManager;
    private final ConcurrentMap<UUID, Boolean> loading = new ConcurrentHashMap<>();

    public PlayerDataListener(PlayerDataManager dataManager, PlayerManager playerManager) {
        this.dataManager = dataManager;
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final UUID uuid = e.getPlayer().getUniqueId();
        // Debounce: verhindere parallele Loads
        if (loading.putIfAbsent(uuid, Boolean.TRUE) != null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(MinecraftSurvivors.getInstance(), () -> {
            SurvivorPlayer loaded = dataManager.load(uuid);
            Bukkit.getScheduler().runTask(MinecraftSurvivors.getInstance(), () -> {
                try {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline()) return;
                    SurvivorPlayer sp = playerManager.get(uuid);
                    if (loaded != null) {
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
                        sp.setMoveSpeedMult(loaded.getMoveSpeedMult());
                        sp.setAttackSpeedMult(loaded.getAttackSpeedMult());
                        sp.setDamageResist(loaded.getDamageResist());
                        sp.setLuck(loaded.getLuck());
                        p.sendMessage(Component.text("Player data loaded (Level " + sp.getClassLevel() + ")").color(NamedTextColor.GREEN));
                    } else {
                        p.sendMessage(Component.text("No previous player data found. New profile created.").color(NamedTextColor.YELLOW));
                    }
                } finally {
                    loading.remove(uuid);
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        final UUID uuid = e.getPlayer().getUniqueId();
        final SurvivorPlayer sp = playerManager.get(uuid);
        if (sp != null) {
            Bukkit.getScheduler().runTaskAsynchronously(MinecraftSurvivors.getInstance(), () -> dataManager.save(sp));
        }
        playerManager.remove(uuid);
        loading.remove(uuid);
    }
}
