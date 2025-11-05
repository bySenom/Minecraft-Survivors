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
        // Loot chest drop chance (mark item with PDC for interaction)
        try {
            org.bukkit.entity.LivingEntity dead = e.getEntity();
            org.bukkit.World w = dead.getWorld();
            int chance = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getConfigUtil().getInt("spawn.loot.chest-drop-chance-percentage", 5);
            if (chance > 0 && new java.util.Random().nextInt(100) < chance) {
                org.bukkit.Location loc = dead.getLocation();
                w.dropItemNaturally(loc, createLootChestItem());
            }
        } catch (Throwable ignored) {}
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

    private org.bukkit.inventory.ItemStack createLootChestItem() {
        org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST);
        org.bukkit.inventory.meta.ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("Lootchest").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("Einarmiger Bandit: Rechtsklick zum Öffnen").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            meta.lore(lore);
            try {
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance(), "lootchest"), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
            } catch (Throwable ignored) {}
            chest.setItemMeta(meta);
        }
        return chest;
    }
}
