// File: src/main/java/org/bysenom/minecraftSurvivors/gui/GuiClickListener.java
package org.bysenom.minecraftSurvivors.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class GuiClickListener implements Listener {

    private final GuiManager guiManager;
    private final NamespacedKey key;
    private final MinecraftSurvivors plugin;

    public GuiClickListener(MinecraftSurvivors plugin, GuiManager guiManager) {
        this.guiManager = guiManager;
        this.key = new NamespacedKey(plugin, "ms_gui");
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta im = clicked.getItemMeta();
        if (im == null) return;
        String action = im.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (action == null) return;
        Player player = (Player) e.getWhoClicked();

        // Debug logging: inventory title, slot and display name
        try {
            String invTitle = "";
            try { Object t = e.getView().getTitle(); invTitle = t == null ? "" : t.toString(); } catch (Throwable ignored) {}
            String display = "";
            try { if (im.displayName() != null) display = PlainTextComponentSerializer.plainText().serialize(im.displayName()); } catch (Throwable ignored) {}
            plugin.getLogger().info("GuiClickListener: click action='" + action + "' by=" + player.getName() + " invTitle='" + invTitle + "' slot=" + e.getSlot() + " display='" + display + "'");
        } catch (Throwable ignored) {}

        e.setCancelled(true);

        // Prefix handler for admin config categories (admin_cfg_cat:<category>)
        if (action.startsWith("admin_cfg_cat:")) {
            String rest = action.substring("admin_cfg_cat:".length());
            // support admin_cfg_cat:category:page and admin_cfg_cat:category
            String cat = rest;
            int page = 0;
            if (rest.contains(":")) {
                String[] parts = rest.split(":", 2);
                cat = parts[0];
                try { page = Integer.parseInt(parts[1]); } catch (Throwable ignored) { page = 0; }
            }
            plugin.getLogger().info("GuiClickListener: admin category click detected, opening category='" + cat + "' page="+page+" for " + player.getName());
            try {
                // try 3-arg variant first
                try {
                    java.lang.reflect.Method m = guiManager.getClass().getMethod("openAdminCategoryEditor", org.bukkit.entity.Player.class, String.class, int.class);
                    m.invoke(guiManager, player, cat, page);
                } catch (NoSuchMethodException nsme) {
                    // try 2-arg variant
                    try {
                        java.lang.reflect.Method m2 = guiManager.getClass().getMethod("openAdminCategoryEditor", org.bukkit.entity.Player.class, String.class);
                        m2.invoke(guiManager, player, cat);
                    } catch (NoSuchMethodException nsme2) {
                        plugin.getLogger().warning("openAdminCategoryEditor method not found (3-arg or 2-arg)");
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Reflective openAdminCategoryEditor failed: " + t.getMessage());
            }
            return;
        }

        if (action.startsWith("admin_cfg_cat_page:")) {
            String rest = action.substring("admin_cfg_cat_page:".length());
            String[] parts = rest.split(":");
            if (parts.length >= 2) {
                String cat = parts[0]; int page = 0; try { page = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
                plugin.getLogger().info("GuiClickListener: admin category pagination click detected, category='"+cat+"' page="+page+" by="+player.getName());
                try {
                    try {
                        java.lang.reflect.Method m = guiManager.getClass().getMethod("openAdminCategoryEditor", org.bukkit.entity.Player.class, String.class, int.class);
                        m.invoke(guiManager, player, cat, page);
                    } catch (NoSuchMethodException nsme) {
                        java.lang.reflect.Method m2 = guiManager.getClass().getMethod("openAdminCategoryEditor", org.bukkit.entity.Player.class, String.class);
                        m2.invoke(guiManager, player, cat);
                    }
                } catch (Throwable t) { plugin.getLogger().warning("Reflective openAdminCategoryEditor failed: "+t.getMessage()); }
            }
            return;
        }

        // Standard actions
        switch (action) {
            case "open_profile":
                guiManager.openLevelUpMenu(player, Math.max(1, plugin.getPlayerManager().get(player.getUniqueId()).getClassLevel()));
                return;
            case "start_wizard":
                guiManager.openClassSelection(player);
                return;
            case "start":
                player.sendMessage("§eBitte nutze 'Spiel starten' und wähle eine Klasse.");
                return;
            case "admin":
                guiManager.openAdminPanel(player);
                return;
            case "admin_config":
                guiManager.openAdminConfigEditor(player);
                return;
            case "config":
                if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung für Config."); return; }
                guiManager.openConfigMenu(player);
                return;
            case "party":
                guiManager.openPartyMenu(player);
                return;
            case "party_invite_list":
                guiManager.openPartyInviteList(player);
                return;
            case "powerup":
                try { java.lang.reflect.Method m = guiManager.getClass().getMethod("openShop", org.bukkit.entity.Player.class); m.invoke(guiManager, player); } catch (Throwable t) { player.sendMessage("§cShop derzeit nicht verfügbar."); }
                return;
            case "stats":
                guiManager.openStatsMenu(player);
                return;
            case "back":
                guiManager.openMainMenu(player);
                return;
            case "admin_back":
                guiManager.openAdminPanel(player);
                return;
            case "config_reload":
                plugin.getGameManager().reloadConfigAndApply();
                player.sendMessage("§aConfig neu geladen.");
                return;
            // admin debug shortcuts
            case "adm_coins":
                plugin.getPlayerManager().get(player.getUniqueId()).addCoins(100);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+100 Coins");
                guiManager.openAdminPanel(player);
                return;
            case "adm_essence":
                org.bysenom.minecraftSurvivors.model.MetaProfile mp = plugin.getMetaManager().get(player.getUniqueId());
                mp.addEssence(10); plugin.getMetaManager().save(mp);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+10 Essence");
                guiManager.openAdminPanel(player);
                return;
            default:
                break;
        }

        // Post-switch handlers (prefixes and other actions)
        if (action.startsWith("party_invite:")) {
            try {
                java.util.UUID target = java.util.UUID.fromString(action.substring("party_invite:".length()));
                if (plugin.getPartyManager().invite(player.getUniqueId(), target, 60)) player.sendMessage("§aEinladung gesendet."); else player.sendMessage("§cInvite fehlgeschlagen.");
                guiManager.openPartyInviteList(player);
            } catch (Throwable ignored) {}
            return;
        }

        if (action.equals("party_back")) { guiManager.openPartyMenu(player); return; }

        if (action.startsWith("party_member:")) {
            try {
                java.util.UUID uid = java.util.UUID.fromString(action.substring("party_member:".length()));
                org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
                org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Party Aktion").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                org.bukkit.entity.Player clickedPlayer = org.bukkit.Bukkit.getPlayer(uid);
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text(clickedPlayer != null ? clickedPlayer.getName() : uid.toString()).color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                inv.setItem(3, GuiTheme.createAction(plugin, org.bukkit.Material.PLAYER_HEAD, net.kyori.adventure.text.Component.text("Spieler").color(net.kyori.adventure.text.format.NamedTextColor.WHITE), lore, "noop", false));
                if (party != null && party.getLeader().equals(player.getUniqueId())) {
                    inv.setItem(1, GuiTheme.createAction(plugin, org.bukkit.Material.LIME_DYE, net.kyori.adventure.text.Component.text("Promote").color(net.kyori.adventure.text.format.NamedTextColor.GREEN), java.util.List.of(net.kyori.adventure.text.Component.text("Macht zum Leader")), "party_promote:"+uid, false));
                    if (!uid.equals(player.getUniqueId())) inv.setItem(5, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Kick").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Spieler aus Party")), "party_kick:"+uid, false));
                }
                inv.setItem(7, GuiTheme.createAction(plugin, org.bukkit.Material.ARROW, net.kyori.adventure.text.Component.text("Zurück").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Schließt dieses Menü")), "party_back", false));
                player.openInventory(inv);
            } catch (Throwable ignored) {}
            return;
        }

        // Glyph interactions
        if (action.startsWith("glyph_socket:")) {
            try {
                String[] parts = action.split(":", 3);
                if (parts.length < 3) return;
                String abilityKey = parts[1]; int slot = Integer.parseInt(parts[2]);
                java.util.List<org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def> choices = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.forAbility(abilityKey);
                org.bukkit.inventory.Inventory sel = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Wähle Glyph").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                int si = 0;
                for (org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def gd : choices) {
                    java.util.List<net.kyori.adventure.text.Component> gl = new java.util.ArrayList<>();
                    gl.add(net.kyori.adventure.text.Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                    String act = "glyph_select:" + abilityKey + ":" + slot + ":" + gd.key;
                    sel.setItem(si, GuiTheme.createAction(plugin, gd.icon, net.kyori.adventure.text.Component.text(gd.name).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, act, false));
                    si++; if (si >= sel.getSize()) break;
                }
                sel.setItem(8, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Entfernen").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Glyph aus dem Sockel")), "glyph_remove:"+abilityKey+":"+slot, false));
                try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), true); org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionContext(player.getUniqueId(), abilityKey, slot); } catch (Throwable ignored) {}
                player.openInventory(sel);
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("glyph_select:")) {
            try {
                String[] parts = action.split(":", 4);
                if (parts.length < 4) return;
                String abilityKey = parts[1]; int slot = Integer.parseInt(parts[2]); String glyphKey = parts[3];
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId()); if (sp == null) return;
                String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(player.getUniqueId());
                String pendingNorm = pending != null ? pending.trim().toLowerCase(java.util.Locale.ROOT) : null;
                String clickedNorm = glyphKey != null ? glyphKey.trim().toLowerCase(java.util.Locale.ROOT) : null;
                if (clickedNorm != null && pendingNorm != null && pendingNorm.equals(clickedNorm)) {
                    org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.consumePendingGlyph(player.getUniqueId());
                    boolean ok = sp.replaceGlyph(abilityKey, slot, glyphKey);
                    if (ok) {
                        plugin.getPlayerDataManager().saveAsync(sp);
                        org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + glyphKey);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                        try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false); org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId()); } catch (Throwable ignored) {}
                        try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                        try { player.closeInventory(); } catch (Throwable ignored) {}
                    } else {
                        org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setPendingGlyphWithLog(player.getUniqueId(), pending);
                        new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                    }
                } else {
                    org.bysenom.minecraftSurvivors.util.Msg.err(player, "Ausgewählte Glyphe stimmt nicht mit der aufgesammelten Glyphe überein.");
                    new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                }
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("glyph_remove:")) {
            try {
                String[] parts = action.split(":", 3); String abilityKey = parts[1]; int slot = Integer.parseInt(parts[2]);
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId()); if (sp == null) return;
                boolean ok = sp.replaceGlyph(abilityKey, slot, null);
                if (ok) { plugin.getPlayerDataManager().saveAsync(sp); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph entfernt"); }
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false); org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId()); } catch (Throwable ignored) {}
                try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                try { player.closeInventory(); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
            return;
        }

        // Config editing handlers --------------------------------------------------
        if (action.startsWith("cfg_edit:")) {
            String fullPath = action.substring("cfg_edit:".length());
            plugin.getLogger().info("GuiClickListener: open key editor for='" + fullPath + "' by=" + player.getName());
            try {
                java.lang.reflect.Method m = guiManager.getClass().getMethod("openAdminConfigKeyEditor", org.bukkit.entity.Player.class, String.class);
                m.invoke(guiManager, player, fullPath);
            } catch (NoSuchMethodException nsme) {
                plugin.getLogger().warning("openAdminConfigKeyEditor method not found: " + nsme.getMessage());
            } catch (Throwable t) { plugin.getLogger().warning("Failed to open key editor for '"+fullPath+"' via reflection: "+t.getMessage()); }
            return;
        }

        if (action.startsWith("cfg_chat:")) {
            String fullPath = action.substring("cfg_chat:".length());
            plugin.getLogger().info("GuiClickListener: start chat-edit for='" + fullPath + "' by=" + player.getName());
            try {
                // start a chat session for this admin
                org.bysenom.minecraftSurvivors.manager.ConfigEditSessionManager.Session s = plugin.getConfigEditSessionManager().startChatSession(player.getUniqueId(), fullPath);
                // close inventory and pause player locally so game isn't progressing for them
                try { player.closeInventory(); } catch (Throwable ignored) {}
                try { plugin.getGameManager().pauseForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                player.sendMessage("§aBearbeite Config: " + fullPath + " — Gib nun den neuen Wert im Chat ein (60s). Verwende /msconfig confirm oder /msconfig cancel.");
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to start chat-edit session for '"+fullPath+"': "+t.getMessage());
                player.sendMessage("§cKonnte Chat-Edit-Session nicht starten: " + t.getMessage());
            }
            return;
        }

        if (action.startsWith("cfg_toggle:")) {
            String fullPath = action.substring("cfg_toggle:".length());
            plugin.getLogger().info("GuiClickListener: toggle cfg '"+fullPath+"' by="+player.getName());
            try {
                org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
                Object v = cfg.get(fullPath);
                if (v instanceof Boolean) {
                    boolean cur = (Boolean) v;
                    plugin.getConfigUtil().setValue(fullPath, !cur);
                } else {
                    player.sendMessage("§cDieser Key ist kein Boolean: " + fullPath);
                }
            } catch (Throwable t) { plugin.getLogger().warning("cfg_toggle failed: "+t.getMessage()); player.sendMessage("§cFehler beim Setzen der Config"); }
            try { java.lang.reflect.Method m = guiManager.getClass().getMethod("openAdminConfigKeyEditor", org.bukkit.entity.Player.class, String.class); m.invoke(guiManager, player, fullPath); } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("cfg_inc:") || action.startsWith("cfg_dec:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 3) {
                String op = parts[0]; String fullPath = parts[1]; String stepS = parts[2];
                double step = 0; try { step = Double.parseDouble(stepS); } catch (Throwable ignored) {}
                plugin.getLogger().info("GuiClickListener: cfg op="+op+" path='"+fullPath+"' step='"+step+"' by="+player.getName());
                try {
                    org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
                    Object cur = cfg.get(fullPath);
                    if (cur instanceof Number) {
                        double curd = ((Number) cur).doubleValue();
                        double nw = op.equals("cfg_inc") ? (curd + step) : (curd - step);
                        if (cur instanceof Integer || (Math.floor(curd) == curd && Math.floor(step) == step)) {
                            plugin.getConfigUtil().setValue(fullPath, (int) Math.round(nw));
                        } else {
                            plugin.getConfigUtil().setValue(fullPath, nw);
                        }
                    } else {
                        player.sendMessage("§cDieser Key ist kein numerischer Wert: " + fullPath);
                    }
                } catch (Throwable t) { plugin.getLogger().warning("cfg_inc/dec failed: "+t.getMessage()); player.sendMessage("§cFehler beim Setzen der Config"); }
                try { java.lang.reflect.Method m = guiManager.getClass().getMethod("openAdminConfigKeyEditor", org.bukkit.entity.Player.class, String.class); m.invoke(guiManager, player, parts[1]); } catch (Throwable ignored) {}
            }
            return;
        }
    }
}
