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
        Inventory inv = Bukkit.createInventory(p, 54, net.kyori.adventure.text.Component.text("Lobby • Matchmaking"));
        // Basisdaten
        int pos = queueManager.getPosition(p.getUniqueId());
        int interval = Math.max(1, plugin.getConfig().getInt("admission.interval-seconds", 3));
        int eta = pos > 0 ? (pos - 1) * interval : -1;
        String info = (pos > 0) ? ("§7Deine Position: §e" + pos + (eta >= 0 ? " §7• ETA ~§e" + eta + "s" : "")) : "§7Du bist nicht in der Queue.";
        boolean inQueue = pos > 0;
        // Queue Buttons (größeres Layout, Border optional später)
        inv.setItem(20, item(inQueue ? Material.GRAY_DYE : Material.LIME_DYE, inQueue ? "Bereits in Queue" : "Queue beitreten", new String[]{"§7Klicke zum Beitreten", info}));
        inv.setItem(24, item(inQueue ? Material.BARRIER : Material.GRAY_DYE, "Queue verlassen", new String[]{"§7Klicke zum Verlassen", info}));
        // Survivors Menü Shortcut
        inv.setItem(22, item(Material.NETHER_STAR, "Survivors-Menü", new String[]{"§7Öffnet das MS Hauptmenü (/msmenu)"}));
        // Friends Übersicht
        int friendCount = plugin.getFriendManager().listFriendNames(p.getUniqueId()).size();
        int pendingCount = plugin.getFriendManager().listPendingNames(p.getUniqueId()).size();
        inv.setItem(30, item(Material.PLAYER_HEAD, "Friends", new String[]{"§7Freunde: §e" + friendCount, "§7Offene Anfragen: §e" + pendingCount, "§7Befehl: /friends"}));
        // Cosmetics Übersicht
        int cosmetics = plugin.getCosmeticManager().getUnlockedKeys(p.getUniqueId()).size();
        inv.setItem(32, item(Material.GLOWSTONE_DUST, "Cosmetics", new String[]{"§7Freigeschaltet: §e" + cosmetics, "§7Befehl: /cosmetics", "§7Beispiel: /cosmetics unlock trail_flame"}));
        // Party Status (optional Bridge)
        int partySize = 0;
        try {
            org.bysenom.lobby.bridge.PartyBridge bridge = plugin.getPartyBridge();
            if (bridge != null) partySize = bridge.getMemberUuids(p).size();
        } catch (Throwable ignored) {}
        inv.setItem(34, item(Material.BOOK, "Party", new String[]{partySize > 0 ? "§7Mitglieder: §e" + partySize : "§7Keine Party", "§7Befehl: /party"}));
        // Info / Refresh
        inv.setItem(49, item(Material.CLOCK, "Aktualisieren", new String[]{"§7Klicke zum Refresh"}));
        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name, String[] lores) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(name));
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        if (lores != null) {
            for (String s : lores) lore.add(net.kyori.adventure.text.Component.text(s));
        }
        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    // Neue Wrapper-Methoden für LobbySystem / ClickListener
    public void openFriendsMenu(Player p) {
        try { plugin.getNavigatorManager().openFriends(p); } catch (Throwable t) { p.sendMessage("§cFriends-GUI nicht verfügbar."); }
    }
    public void openCosmeticsMenu(Player p) {
        try { plugin.getNavigatorManager().openCosmetics(p); } catch (Throwable t) { p.sendMessage("§cCosmetics-GUI nicht verfügbar."); }
    }
    public void openPartyMenu(Player p) {
        try { plugin.getNavigatorManager().openParty(p); } catch (Throwable t) { p.sendMessage("§cParty-GUI nicht verfügbar."); }
    }
}
