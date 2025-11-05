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

            int xpGain = 1;
            try { if (config != null) xpGain = config.getInt("levelup.xp-per-kill", 1); } catch (Throwable ignored) {}

            // Party-Share
            org.bysenom.minecraftSurvivors.manager.PartyManager pm = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getPartyManager();
            java.util.List<java.util.UUID> targets = new java.util.ArrayList<>();
            if (pm != null) {
                org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm.getPartyOf(killer.getUniqueId());
                if (party != null) targets = pm.onlineMembers(party);
            }
            if (targets.isEmpty()) {
                // no party: solo xp
                handleXpGain(killer, sp, xpGain);
            } else {
                // even split among online party members
                int members = Math.max(1, targets.size());
                int per = Math.max(1, xpGain / members);
                int remainder = Math.max(0, xpGain - per * members);
                for (java.util.UUID u : targets) {
                    Player pl = org.bukkit.Bukkit.getPlayer(u);
                    if (pl == null) continue;
                    SurvivorPlayer memberSp = playerManager.get(u);
                    int give = per + (remainder > 0 ? 1 : 0);
                    if (remainder > 0) remainder--;
                    handleXpGain(pl, memberSp, give);
                }
            }
        }
    }

    private void handleXpGain(Player player, SurvivorPlayer sp, int xpGain) {
        int beforeLevel = sp.getClassLevel();
        boolean leveled = sp.addXp(xpGain);
        int afterLevel = sp.getClassLevel();
        if (leveled) {
            if (guiManager != null) guiManager.openLevelUpMenu(player, afterLevel);
            if (afterLevel > beforeLevel) player.sendMessage(Component.text("§aLevel up! Du bist jetzt Level " + afterLevel));
            else player.sendMessage(Component.text("§aLevel up!"));
        }
    }
}
