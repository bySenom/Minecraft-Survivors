package org.bysenom.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UiManager {
    private final LobbySystem plugin;
    private final QueueManager queueManager;

    public UiManager(LobbySystem plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
    }

    public void openLobbyMenu(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, net.kyori.adventure.text.Component.text("Lobby • Matchmaking"));
        inv.setItem(11, item(Material.LIME_DYE, "Queue beitreten", "Klicke, um der Queue beizutreten"));
        inv.setItem(15, item(Material.BARRIER, "Queue verlassen", "Klicke, um die Queue zu verlassen"));
        inv.setItem(22, item(Material.NETHER_STAR, "Survivors-Menü", "Öffnet das MS Hauptmenü (/msmenu)"));
        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name, String lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(name));
        meta.lore(java.util.List.of(net.kyori.adventure.text.Component.text(lore)));
        it.setItemMeta(meta);
        return it;
    }
}
