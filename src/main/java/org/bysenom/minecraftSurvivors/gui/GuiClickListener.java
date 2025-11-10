package org.bysenom.minecraftSurvivors.gui;

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

        // Protect specialized confirm dialog
        String title = "";
        try { title = PlainTextComponentSerializer.plainText().serialize(e.getView().title()); } catch (Throwable ignored) {}
        String titleLower = title.toLowerCase(java.util.Locale.ROOT);
        boolean isReplaceConfirm = false;
        try {
            isReplaceConfirm = im.getPersistentDataContainer().has(new NamespacedKey(plugin, "ms_replace_confirm"), PersistentDataType.STRING)
                    || titleLower.contains("ability ersetzen") || titleLower.contains("confirm replace");
        } catch (Throwable ignored) {}
        if (isReplaceConfirm) return;

        e.setCancelled(true);

        try {
            // --- simple prefix handlers ---
            if (action.startsWith("admin_cfg_cat:")) {
                String rest = action.substring("admin_cfg_cat:".length());
                String cat = rest;
                int page = 0;
                if (rest.contains(":")) {
                    String[] parts = rest.split(":", 2);
                    cat = parts[0];
                    try { page = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
                }
                try { guiManager.openAdminCategoryEditor(player, cat, page); } catch (Throwable t) { plugin.getLogger().warning("openAdminCategoryEditor failed: " + t.getMessage()); }
                return;
            }
            if (action.startsWith("admin_cfg_cat_page:")) {
                String rest = action.substring("admin_cfg_cat_page:".length());
                String[] parts = rest.split(":");
                if (parts.length >= 2) {
                    try { guiManager.openAdminCategoryEditor(player, parts[0], Integer.parseInt(parts[1])); } catch (Throwable t) { plugin.getLogger().warning("openAdminCategoryEditor page failed: " + t.getMessage()); }
                }
                return;
            }

            // Compact switch for common actions
            switch (action) {
                case "open_profile" -> {
                    try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                    guiManager.openLevelUpMenu(player, Math.max(1, plugin.getPlayerManager().get(player.getUniqueId()).getClassLevel()));
                    return;
                }
                case "open_class_select", "start_wizard" -> {
                    try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                    try {
                        var pm = plugin.getPartyManager();
                        var party = pm != null ? pm.getPartyOf(player.getUniqueId()) : null;
                        if (party != null) {
                            if (party.getLeader().equals(player.getUniqueId())) {
                                int voteSec = Math.max(5, plugin.getConfigUtil().getInt("lobby.party-vote.seconds", 15));
                                plugin.getGameManager().beginPartyStartVote(party, voteSec);
                                player.sendMessage("§eReady-Abstimmung gestartet. Bitte bestätigen.");
                            } else {
                                player.sendMessage("§eWarte bis der Party-Leader die Ready-Abstimmung startet.");
                            }
                        } else {
                            guiManager.openClassSelection(player);
                        }
                    } catch (Throwable t) { player.sendMessage("§cFehler beim Start: " + t.getMessage()); }
                    return;
                }
                case "start" -> { try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {} player.sendMessage("§eBitte nutze 'Spiel starten' und wähle eine Klasse."); return; }
                case "admin" -> { guiManager.openAdminPanel(player); return; }
                case "admin_config" -> { guiManager.openAdminConfigEditor(player); return; }
                case "config" -> { if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung."); return; } guiManager.openConfigMenu(player); return; }
                case "party" -> { guiManager.openPartyMenu(player); return; }
                case "party_invite_list" -> { guiManager.openPartyInviteList(player); return; }
                case "powerup" -> { try { guiManager.getClass().getMethod("openShop", Player.class).invoke(guiManager, player); } catch (Throwable t) { player.sendMessage("§cShop derzeit nicht verfügbar."); } return; }
                case "stats" -> { guiManager.openStatsMenu(player); return; }
                case "back" -> { guiManager.openMainMenu(player); return; }
                case "admin_back" -> { guiManager.openAdminPanel(player); return; }
                case "config_reload" -> { plugin.getGameManager().reloadConfigAndApply(); player.sendMessage("§aConfig neu geladen."); return; }
                case "leave_context" -> { try { plugin.getGameManager().leaveSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {} try { player.closeInventory(); } catch (Throwable ignored) {} player.sendMessage("§7Survivors-Menü verlassen."); return; }
                case "adm_coins" -> { plugin.getPlayerManager().get(player.getUniqueId()).addCoins(100); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+100 Coins"); guiManager.openAdminPanel(player); return; }
                case "adm_essence" -> { var mp = plugin.getMetaManager().get(player.getUniqueId()); mp.addEssence(10); plugin.getMetaManager().save(mp); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+10 Essence"); guiManager.openAdminPanel(player); return; }
                case "toggle_ready" -> {
                    var sp = plugin.getPlayerManager().get(player.getUniqueId());
                    if (sp == null) return;
                    boolean nr = !sp.isReady();
                    sp.setReady(nr);
                    player.sendMessage(nr ? "§aBereit gesetzt." : "§eBereitschaft aufgehoben.");
                    try { plugin.getScoreboardManager().forceUpdate(player); } catch (Throwable ignored) {}
                    if (nr) {
                        try {
                            var pm = plugin.getPartyManager();
                            var party = pm != null ? pm.getPartyOf(player.getUniqueId()) : null;
                            if (party == null) player.sendMessage("§7Solo: Wähle deine Klasse im Menü – Startet automatisch sobald nur du im Modus bist.");
                            else if (party.getLeader().equals(player.getUniqueId())) {
                                int voteSec = Math.max(5, plugin.getConfigUtil().getInt("lobby.party-vote.seconds", 15));
                                plugin.getGameManager().beginPartyStartVote(party, voteSec);
                            }
                        } catch (Throwable ignored) {}
                    } else {
                        try { plugin.getGameManager().abortStartCountdown("Player unready"); } catch (Throwable ignored) {}
                    }
                    return;
                }
                default -> {}
            }

            // select_<class>_wizard flow
            if (action.startsWith("select_") && action.endsWith("_wizard")) {
                String mid = action.substring(7, action.length() - 7).toLowerCase(java.util.Locale.ROOT);
                org.bysenom.minecraftSurvivors.model.PlayerClass chosen = switch (mid) {
                    case "shaman" -> org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN;
                    case "pyromancer" -> org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER;
                    case "ranger" -> org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER;
                    case "paladin" -> org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN;
                    default -> null;
                };
                if (chosen == null) { player.sendMessage("§cUnbekannte Klasse: " + mid); return; }
                var sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp != null) {
                    sp.setSelectedClass(chosen);
                    try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                    player.sendMessage("§aKlasse gewählt: §f" + chosen.name());
                    try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f); } catch (Throwable ignored) {}
                    try { plugin.getScoreboardManager().forceUpdate(player); } catch (Throwable ignored) {}
                    try { plugin.getGameManager().trySoloAutoStart(player); } catch (Throwable ignored) {}
                }
                guiManager.openMainMenu(player);
                return;
            }

            // --- GLYPH: pickup select ---
            if (action.startsWith("glyph_pickup_select:")) {
                String glyphKeyFull = action.substring("glyph_pickup_select:".length());
                if (glyphKeyFull.isEmpty()) return;
                String abilityKey = glyphKeyFull.contains(":") ? glyphKeyFull.split(":",2)[0] : glyphKeyFull;
                var sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp == null) { player.sendMessage("§cFehler: Spielerprofil nicht geladen."); return; }

                boolean added = false;
                try { added = sp.addGlyph(abilityKey, glyphKeyFull); } catch (Throwable ignored) { added = false; }

                if (added) {
                    plugin.getPlayerDataManager().saveAsync(sp);
                    try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph erhalten: " + glyphKeyFull); } catch (Throwable ignored) {}
                    try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f); } catch (Throwable ignored) {}
                    try {
                        long filled = sp.getGlyphs(abilityKey).stream().filter(s -> s != null && !s.isEmpty()).count();
                        if (filled >= 3) {
                            if (sp.incrementAbilityLevel(abilityKey, 1)) {
                                plugin.getPlayerDataManager().saveAsync(sp);
                                try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                } else {
                    try {
                        var gls = sp.getGlyphs(abilityKey);
                        long filled = gls.stream().filter(s -> s != null && !s.isEmpty()).count();
                        boolean has = gls.stream().anyMatch(s -> s != null && s.equalsIgnoreCase(glyphKeyFull));
                        if (filled >= 3 && !has) {
                            if (sp.incrementAbilityLevel(abilityKey, 1)) {
                                plugin.getPlayerDataManager().saveAsync(sp);
                                try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Keine freien Sockel — Fähigkeit verbessert: " + abilityKey); } catch (Throwable ignored) {}
                            } else {
                                try { org.bysenom.minecraftSurvivors.util.Msg.err(player, "Glyph konnte nicht hinzugefügt werden"); } catch (Throwable ignored) {}
                            }
                        } else {
                            try { org.bysenom.minecraftSurvivors.util.Msg.err(player, "Glyph konnte nicht hinzugefügt werden (bereits vorhanden oder volle Slots). Öffne Ersetzen-UI..."); } catch (Throwable ignored) {}
                            try { new org.bysenom.minecraftSurvivors.gui.GlyphReplaceMenu(player, sp, abilityKey, glyphKeyFull); } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {
                        try { org.bysenom.minecraftSurvivors.util.Msg.err(player, "Glyph konnte nicht hinzugefügt werden (Fehler)"); } catch (Throwable ignored2) {}
                    }
                }

                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                try { player.closeInventory(); } catch (Throwable ignored) {}
                return;
            }

            // --- GLYPH: socket into slot ---
            if (action.startsWith("glyph_socket:")) {
                String[] pparts = action.split(":", 3);
                if (pparts.length < 3) return;
                String abilityKey = pparts[1];
                int slot = Integer.parseInt(pparts[2]);
                var sp2 = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp2 == null) return;

                String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(player.getUniqueId());
                if (pending != null && pending.startsWith(abilityKey + ":")) {
                    boolean ok = sp2.replaceGlyph(abilityKey, slot, pending);
                    if (ok) {
                        plugin.getPlayerDataManager().saveAsync(sp2);
                        org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + pending);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId());
                        try { long filled = sp2.getGlyphs(abilityKey).stream().filter(s -> s != null && !s.isEmpty()).count(); if (filled >= 3 && sp2.incrementAbilityLevel(abilityKey, 1)) { plugin.getPlayerDataManager().saveAsync(sp2); try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {} } } catch (Throwable ignored) {}
                        try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                        try { player.closeInventory(); } catch (Throwable ignored) {}
                        return;
                    } else {
                        org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                    }
                }

                // show selection UI (filter out already socketed glyphs)
                try { plugin.getGameManager().pauseForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                var choices = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.forAbility(abilityKey);
                var sel = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Wähle Glyph").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                int si = 0;
                try {
                    var spLocal = plugin.getPlayerManager().get(player.getUniqueId());
                    java.util.Set<String> existing = spLocal != null ? new java.util.HashSet<>(spLocal.getGlyphs(abilityKey)) : java.util.Collections.emptySet();
                    if (choices != null) {
                        for (var gd : choices) {
                            if (gd == null) continue;
                            if (existing.contains(gd.key)) continue;
                            var gl = new java.util.ArrayList<net.kyori.adventure.text.Component>();
                            gl.add(net.kyori.adventure.text.Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                            String act = "glyph_select:" + abilityKey + ":" + slot + ":" + gd.key;
                            sel.setItem(si, GuiTheme.createAction(plugin, gd.icon, net.kyori.adventure.text.Component.text(gd.name).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, act, false));
                            si++; if (si >= sel.getSize()) break;
                        }
                    }
                    if (si == 0) {
                        String fallback = (choices != null && !choices.isEmpty()) ? choices.get(0).key : null;
                        if (fallback != null) {
                            try { new org.bysenom.minecraftSurvivors.gui.GlyphReplaceMenu(player, sp2, abilityKey, fallback); } catch (Throwable ignored) {}
                            return;
                        }
                    }
                } catch (Throwable t) {
                    // fallback: show all choices
                    int ssi = 0;
                    if (choices != null) for (var gd : choices) {
                        try {
                            if (gd == null) continue;
                            var gl = new java.util.ArrayList<net.kyori.adventure.text.Component>();
                            gl.add(net.kyori.adventure.text.Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                            String act = "glyph_select:" + abilityKey + ":" + slot + ":" + gd.key;
                            sel.setItem(ssi, GuiTheme.createAction(plugin, gd.icon, net.kyori.adventure.text.Component.text(gd.name).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, act, false));
                        } catch (Throwable ignored) {}
                        ssi++; if (ssi >= sel.getSize()) break;
                    }
                }
                sel.setItem(8, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Entfernen").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Glyph aus dem Sockel")), "glyph_remove:"+abilityKey+":"+slot, false));
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), true);
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionContext(player.getUniqueId(), abilityKey, slot);
                player.openInventory(sel);
                return;
            }

            // --- GLYPH: direct selection from socket menu ---
            if (action.startsWith("glyph_select:")) {
                String[] parts = action.split(":", 4);
                if (parts.length < 4) return;
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                String glyphKey = parts[3];
                var sp = plugin.getPlayerManager().get(player.getUniqueId()); if (sp == null) return;

                String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(player.getUniqueId());
                String pendingNorm = pending == null ? null : pending.trim().toLowerCase(java.util.Locale.ROOT);
                String clickedNorm = glyphKey == null ? null : glyphKey.trim().toLowerCase(java.util.Locale.ROOT);

                if (pendingNorm != null && pendingNorm.equals(clickedNorm)) {
                    org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.consumePendingGlyph(player.getUniqueId());
                    boolean ok = sp.replaceGlyph(abilityKey, slot, glyphKey);
                    if (ok) {
                        plugin.getPlayerDataManager().saveAsync(sp);
                        org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + glyphKey);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId());
                        try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                        try { player.closeInventory(); } catch (Throwable ignored) {}
                        try {
                            long filled = sp.getGlyphs(abilityKey).stream().filter(s -> s != null && !s.isEmpty()).count();
                            if (filled >= 3 && sp.incrementAbilityLevel(abilityKey, 1)) {
                                plugin.getPlayerDataManager().saveAsync(sp);
                                try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {}
                            }
                        } catch (Throwable ignored) {}
                    } else {
                        org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setPendingGlyphWithLog(player.getUniqueId(), pending);
                        new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                    }
                } else {
                    org.bysenom.minecraftSurvivors.util.Msg.err(player, "Ausgewählte Glyphe stimmt nicht mit der aufgesammelten Glyphe überein.");
                    new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                }
                return;
            }

            // --- GLYPH: replace confirm ---
            if (action.startsWith("glyph_replace_confirm:")) {
                String[] parts = action.split(":", 4);
                if (parts.length < 4) return;
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                String newGlyph = parts[3];
                var sp = plugin.getPlayerManager().get(player.getUniqueId()); if (sp == null) return;
                boolean ok = sp.replaceGlyph(abilityKey, slot, newGlyph);
                if (ok) { plugin.getPlayerDataManager().saveAsync(sp); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph ersetzt: " + newGlyph); }
                else { org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht ersetzen"); }
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId());
                try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                try { player.closeInventory(); } catch (Throwable ignored) {}
                return;
            }

            // --- GLYPH: remove ---
            if (action.startsWith("glyph_remove:")) {
                String[] parts = action.split(":", 3);
                if (parts.length < 3) return;
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                var sp = plugin.getPlayerManager().get(player.getUniqueId()); if (sp == null) return;
                boolean ok = sp.replaceGlyph(abilityKey, slot, null);
                if (ok) { plugin.getPlayerDataManager().saveAsync(sp); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph entfernt"); }
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId());
                try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                try { player.closeInventory(); } catch (Throwable ignored) {}
                return;
            }

            // --- Config editors ---
            if (action.startsWith("cfg_edit:")) {
                String fullPath = action.substring("cfg_edit:".length());
                try { guiManager.openAdminConfigKeyEditor(player, fullPath); } catch (Throwable t) { plugin.getLogger().warning("openAdminConfigKeyEditor failed: " + t.getMessage()); }
                return;
            }

            if (action.startsWith("cfg_chat:")) {
                String fullPath = action.substring("cfg_chat:".length());
                try {
                    plugin.getConfigEditSessionManager().startChatSession(player.getUniqueId(), fullPath);
                    try { player.closeInventory(); } catch (Throwable ignored) {}
                    try { plugin.getGameManager().pauseForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                    player.sendMessage("§aBearbeite Config: " + fullPath + " — Eingabe im Chat (60s). /msconfig confirm|cancel");
                } catch (Throwable t) {
                    plugin.getLogger().warning("cfg_chat failed: " + t.getMessage());
                    player.sendMessage("§cFehler: " + t.getMessage());
                }
                return;
            }

        } catch (Throwable top) {
            // top-level guard: don't let unexpected exceptions break click handling
            plugin.getLogger().log(java.util.logging.Level.FINE, "GuiClickListener top-level handler error: ", top);
        }
    }
}
