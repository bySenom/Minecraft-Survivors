package org.bysenom.lobby;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Verwaltet den Navigator (Kompass) und zugehörige Menüs. */
public class NavigatorManager {
    private final LobbySystem plugin;
    private final org.bukkit.NamespacedKey navKey;

    public NavigatorManager(LobbySystem plugin) {
        this.plugin = plugin;
        this.navKey = new org.bukkit.NamespacedKey(plugin, "nav_action");
    }

    public void giveCompass(Player p) {
        if (!plugin.getConfig().getBoolean("compass.enabled", true)) return;
        int slot = plugin.getConfig().getInt("compass.slot", 0);
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text(plugin.getConfig().getString("compass.name", "§bNavigator")));
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("compass.lore")) lore.add(Component.text(line));
        meta.lore(lore);
        compass.setItemMeta(meta);
        p.getInventory().setItem(slot, compass);
    }

    public void openNavigator(Player p) {
        String title = plugin.getConfig().getString("ui.navigator-title", "Navigator • Auswahl");
        Inventory inv = Bukkit.createInventory(p, 54, Component.text(title));
        inv.setItem(plugin.getConfig().getInt("navigator.slots.survivors", 10),
                actionIcon("navigator.items.survivors", Material.NETHER_STAR, "§aMinecraft Survivors", "§7Betrete den Survivors Spielmodus", "nav_survivors"));
        inv.setItem(plugin.getConfig().getInt("navigator.slots.profile", 12),
                actionIcon("navigator.items.profile", Material.PLAYER_HEAD, "§bProfil", "§7Zeigt deine Statistiken", "nav_profile"));
        inv.setItem(plugin.getConfig().getInt("navigator.slots.party", 14),
                actionIcon("navigator.items.party", Material.ENDER_PEARL, "§dParty", "§7Party verwalten", "nav_party"));
        inv.setItem(plugin.getConfig().getInt("navigator.slots.friends", 16),
                actionIcon("navigator.items.friends", Material.BOOK, "§eFreunde", "§7Freundesliste anzeigen", "nav_friends"));
        inv.setItem(plugin.getConfig().getInt("navigator.slots.cosmetics", 28),
                actionIcon("navigator.items.cosmetics", Material.DIAMOND, "§9Cosmetics", "§7Kosmetische Anpassungen", "nav_cosmetics"));
        java.util.List<String> pend = plugin.getFriendManager().listPendingNames(p.getUniqueId());
        if (!pend.isEmpty()) {
            ItemStack pendingHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta ph = pendingHead.getItemMeta();
            ph.displayName(Component.text("§6Offene Anfragen"));
            List<Component> lore = new java.util.ArrayList<>();
            for (String s : pend) lore.add(Component.text("§e» §7" + s));
            lore.add(Component.text("§8Klicke zum Verwalten"));
            ph.lore(lore);
            ph.getPersistentDataContainer().set(navKey, org.bukkit.persistence.PersistentDataType.STRING, "nav_friends_pending");
            pendingHead.setItemMeta(ph);
            inv.setItem(37, pendingHead);
        }
        inv.setItem(plugin.getConfig().getInt("navigator.slots.close", 49),
                actionIcon("navigator.items.close", Material.BARRIER, "§cSchließen", "§7Fenster schließen", "nav_close"));
        p.openInventory(inv); blip(p);
    }

    public void openProfile(Player p) {
        String title = plugin.getConfig().getString("ui.profile-title", "Profil");
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(title));
        inv.setItem(11, actionIcon(null, Material.EXPERIENCE_BOTTLE, "§aLevel", "§7Dein Level wird hier später angezeigt", "profile_level"));
        inv.setItem(13, actionIcon(null, Material.GOLD_INGOT, "§6Währung", "§7Später: Coins / Essenzen", "profile_currency"));
        inv.setItem(15, actionIcon(null, Material.CLOCK, "§eSpielzeit", "§7Platzhalter", "profile_playtime"));
        inv.setItem(26, actionIcon(null, Material.ARROW, "§fZurück", "§7Zum Navigator", "profile_back"));
        p.openInventory(inv); blip(p);
    }

    public void openParty(Player p) {
        String title = plugin.getConfig().getString("ui.party-title", "Party");
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(title));
        inv.setItem(10, actionIcon(null, Material.LIME_DYE, "§aEinladen (GUI)", "§7Klicke zum Einladen", "party_invite_gui"));
        java.util.List<String> members = plugin.getPartyBridge().listMembers(p);
        ItemStack membersIcon = actionIcon(null, Material.CHEST, "§bMitglieder", "§7Mitglieder anzeigen", "party_members_info");
        ItemMeta im = membersIcon.getItemMeta();
        java.util.List<Component> lore = new java.util.ArrayList<>();
        if (members.isEmpty()) lore.add(Component.text("§7Keine Party")); else for (String s : members) lore.add(Component.text("§7" + s));
        im.lore(lore); membersIcon.setItemMeta(im); inv.setItem(12, membersIcon);
        inv.setItem(14, actionIcon(null, Material.NAME_TAG, "§eRollen", "§7Leader / Member", "party_roles"));
        inv.setItem(16, actionIcon(null, Material.REDSTONE, "§cVerlassen", "§7Party verlassen", "party_leave"));
        inv.setItem(26, actionIcon(null, Material.ARROW, "§fZurück", "§7Zum Navigator", "party_back"));
        p.openInventory(inv); blip(p);
    }

    // Neues Party-Invite-GUI mit Online-Köpfen
    public void openPartyInvite(Player p) {
        String title = plugin.getConfig().getString("ui.party-invite-title", "Party • Spieler einladen");
        Inventory inv = Bukkit.createInventory(p, 54, Component.text(title));
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break; if (target.getUniqueId().equals(p.getUniqueId())) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD); ItemMeta meta = head.getItemMeta();
            meta.displayName(Component.text("§a" + target.getName()));
            java.util.List<Component> lore = new java.util.ArrayList<>(); lore.add(Component.text("§7Klicke zum Einladen"));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(navKey, org.bukkit.persistence.PersistentDataType.STRING, "party_invite_player:"+target.getName());
            head.setItemMeta(meta); inv.setItem(slot++, head);
        }
        inv.setItem(49, actionIcon(null, Material.BARRIER, "§cZurück", "§7Zur Party", "party_invite_back"));
        p.openInventory(inv); blip(p);
    }

    public void openFriends(Player p) {
        String title = plugin.getConfig().getString("ui.friends-title", "Freunde");
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(title));
        inv.setItem(10, actionIcon(null, Material.LIME_DYE, "§aFreund hinzufügen", "§7Nutze /friends invite <Name>", "friends_add"));
        java.util.List<String> list = plugin.getFriendManager().listFriendNames(p.getUniqueId());
        ItemStack head = actionIcon(null, Material.PLAYER_HEAD, "§bFreunde", "§7Freundesliste", "friends_list_info");
        ItemMeta hm = head.getItemMeta(); java.util.List<Component> lore = new java.util.ArrayList<>();
        if (list.isEmpty()) lore.add(Component.text("§7Keine Einträge")); else for (String s : list) lore.add(Component.text("§7" + s));
        hm.lore(lore); head.setItemMeta(hm); inv.setItem(12, head);
        java.util.List<String> pend = plugin.getFriendManager().listPendingNames(p.getUniqueId());
        ItemStack pending = actionIcon(null, Material.PAPER, "§eOffene Anfragen", "§7Klicke zum Verwalten", "friends_pending" );
        ItemMeta pm = pending.getItemMeta(); java.util.List<Component> plore = new java.util.ArrayList<>();
        if (pend.isEmpty()) plore.add(Component.text("§7Keine")); else for (String s : pend) plore.add(Component.text("§7" + s)); plore.add(Component.text("§8Klicke zum Verwalten"));
        pm.lore(plore); pending.setItemMeta(pm); inv.setItem(14, pending);
        inv.setItem(16, actionIcon(null, Material.BARRIER, "§cBlockierte", "§7Blockierte Spieler (später)", "friends_blocked"));
        inv.setItem(26, actionIcon(null, Material.ARROW, "§fZurück", "§7Zum Navigator", "friends_back"));
        p.openInventory(inv); blip(p);
    }

    // Neues GUI: Pending Requests mit Köpfen und Click-Aktionen
    public void openFriendsPending(Player p) {
        String title = plugin.getConfig().getString("ui.friends-pending-title", "Freunde • Anfragen");
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(title));
        java.util.List<String> pend = plugin.getFriendManager().listPendingNames(p.getUniqueId());
        int slot = 0;
        for (String name : pend) {
            if (slot >= 18) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD); ItemMeta meta = head.getItemMeta();
            meta.displayName(Component.text("§e" + name));
            java.util.List<Component> lore = new java.util.ArrayList<>(); lore.add(Component.text("§aLinksklick: Annehmen")); lore.add(Component.text("§cShift+Klick: Ablehnen"));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(navKey, org.bukkit.persistence.PersistentDataType.STRING, "friends_pending_player:"+name);
            head.setItemMeta(meta); inv.setItem(slot++, head);
        }
        inv.setItem(26, actionIcon(null, Material.ARROW, "§fZurück", "§7Zu Freunde", "friends_pending_back"));
        p.openInventory(inv); blip(p);
    }

    public void openCosmetics(Player p) {
        String title = plugin.getConfig().getString("ui.cosmetics-title", "Cosmetics");
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(title));
        inv.setItem(10, actionIcon(null, Material.FIREWORK_ROCKET, "§eTrails", "§7Freigeschaltet: /cosmetics unlock trail_sparkles", "cos_trail"));
        inv.setItem(12, actionIcon(null, Material.SKELETON_SKULL, "§cKill-Effekte", "§7Freigeschaltet: /cosmetics unlock kill_fire", "cos_kill"));
        inv.setItem(14, actionIcon(null, Material.ARMOR_STAND, "§bEmotes", "§7Freigeschaltet: /cosmetics unlock emote_wave", "cos_emote"));
        ItemStack status = actionIcon(null, Material.MAP, "§aStatus", "§7Übersicht", "cos_status" );
        ItemMeta sm = status.getItemMeta(); java.util.List<Component> slore = new java.util.ArrayList<>(); slore.add(lineCos(p, "trail_sparkles", "Trail")); slore.add(lineCos(p, "kill_fire", "Kill-Effekt")); slore.add(lineCos(p, "emote_wave", "Emote")); sm.lore(slore); status.setItemMeta(sm); inv.setItem(16, status);
        inv.setItem(26, actionIcon(null, Material.ARROW, "§fZurück", "§7Zum Navigator", "cos_back"));
        p.openInventory(inv); blip(p);
    }

    private Component lineCos(Player p, String key, String label) {
        boolean unlocked = plugin.getCosmeticManager().isUnlocked(p.getUniqueId(), key);
        return Component.text((unlocked ? "§a✔ " : "§c✘ ") + label + " (§7" + key + "§f)");
    }

    private ItemStack themedIcon(String baseKey, Material defMat, String defName, String defLore) {
        String matName = plugin.getConfig().getString(baseKey + ".material", defMat.name());
        Material mat = Material.matchMaterial(matName); if (mat == null) mat = defMat;
        String name = plugin.getConfig().getString(baseKey + ".name", defName);
        String lore = plugin.getConfig().getString(baseKey + ".lore", defLore);
        return icon(mat, name, lore);
    }

    private ItemStack icon(Material m, String name, String loreLine) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(loreLine));
        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack actionIcon(String baseKey, Material defMat, String defName, String defLore, String action) {
        ItemStack it = (baseKey != null) ? themedIcon(baseKey, defMat, defName, defLore) : icon(defMat, defName, defLore);
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(navKey, org.bukkit.persistence.PersistentDataType.STRING, action);
        it.setItemMeta(meta);
        return it;
    }

    private void blip(Player p) {
        try {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            p.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.2, 0), 5, 0.15, 0.15, 0.15, 0.0);
        } catch (Throwable ignored) {}
    }
}
