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

        // get plain title and short-circuit for confirm dialog so ReplaceConfirmMenu.Listener can handle it
        String title = "";
        try { title = PlainTextComponentSerializer.plainText().serialize(e.getView().title()); } catch (Throwable ignored) {}
        String titleLower = title.toLowerCase(java.util.Locale.ROOT);
        // Only bypass when it's the Replace-Confirm dialog, not every "bestätigen" titled inventory
        boolean isReplaceConfirm = false;
        try {
            isReplaceConfirm = im.getPersistentDataContainer().has(new NamespacedKey(plugin, "ms_replace_confirm"), PersistentDataType.STRING)
                    || titleLower.contains("ability ersetzen") || titleLower.contains("confirm replace");
        } catch (Throwable ignored) {}
        if (isReplaceConfirm) {
            // Let the specialised ReplaceConfirmMenu.Listener handle this inventory entirely
            return;
        }

        // Debug logging
        try {
            net.kyori.adventure.text.Component comp = e.getView().title();
            String invTitle = PlainTextComponentSerializer.plainText().serialize(comp);
            String display = "";
            try {
                net.kyori.adventure.text.Component dn = im.displayName();
                if (dn != null) display = PlainTextComponentSerializer.plainText().serialize(dn);
            } catch (Throwable ignored) {}
            plugin.getLogger().info("GuiClickListener: action='" + action + "' player=" + player.getName() + " inv='" + invTitle + "' slot=" + e.getSlot() + " item='" + display + "'");
        } catch (Throwable ignored) {}

        e.setCancelled(true);

        // Admin Category open (direkte Aufrufe statt Reflection)
        if (action.startsWith("admin_cfg_cat:")) {
            String rest = action.substring("admin_cfg_cat:".length());
            String cat = rest; int page = 0;
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
                String cat = parts[0];
                int page = 0; try { page = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
                try { guiManager.openAdminCategoryEditor(player, cat, page); } catch (Throwable t) { plugin.getLogger().warning("openAdminCategoryEditor page failed: " + t.getMessage()); }
            }
            return;
        }

        switch (action) {
            case "open_profile" -> {
                try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                guiManager.openLevelUpMenu(player, Math.max(1, plugin.getPlayerManager().get(player.getUniqueId()).getClassLevel()));
                return;
            }
            case "open_class_select", "start_wizard" -> {
                try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                // Neu: Party-Flow -> Ready-Vote statt sofort Klassenwahl
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
                        // Solo: sofort Klassenwahl
                        guiManager.openClassSelection(player);
                    }
                } catch (Throwable t) {
                    player.sendMessage("§cFehler beim Start: " + t.getMessage());
                }
                return;
            }
            case "start" -> {
                try { plugin.getGameManager().enterSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                // "start" bleibt Hinweis, eigentliche Wahl passiert über start_wizard
                player.sendMessage("§eBitte nutze 'Spiel starten' und wähle eine Klasse.");
                return;
            }
            case "admin" -> { guiManager.openAdminPanel(player); return; }
            case "admin_config" -> { guiManager.openAdminConfigEditor(player); return; }
            case "config" -> {
                if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung."); return; }
                guiManager.openConfigMenu(player); return;
            }
            case "party" -> { guiManager.openPartyMenu(player); return; }
            case "party_invite_list" -> { guiManager.openPartyInviteList(player); return; }
            case "powerup" -> {
                try { guiManager.getClass().getMethod("openShop", Player.class).invoke(guiManager, player); } catch (Throwable t) { player.sendMessage("§cShop derzeit nicht verfügbar."); }
                return;
            }
            case "stats" -> { guiManager.openStatsMenu(player); return; }
            case "back" -> { guiManager.openMainMenu(player); return; }
            case "admin_back" -> { guiManager.openAdminPanel(player); return; }
            case "config_reload" -> { plugin.getGameManager().reloadConfigAndApply(); player.sendMessage("§aConfig neu geladen."); return; }
            case "leave_context" -> {
                try { plugin.getGameManager().leaveSurvivorsContext(player.getUniqueId()); } catch (Throwable ignored) {}
                try { player.closeInventory(); } catch (Throwable ignored) {}
                player.sendMessage("§7Survivors-Menü verlassen.");
                return;
            }
            case "adm_coins" -> {
                plugin.getPlayerManager().get(player.getUniqueId()).addCoins(100);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+100 Coins");
                guiManager.openAdminPanel(player); return;
            }
            case "adm_essence" -> {
                var mp = plugin.getMetaManager().get(player.getUniqueId());
                mp.addEssence(10); plugin.getMetaManager().save(mp);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+10 Essence");
                guiManager.openAdminPanel(player); return;
            }
            case "toggle_ready" -> {
                var sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp == null) return;
                boolean nr = !sp.isReady();
                sp.setReady(nr);
                player.sendMessage(nr ? "§aBereit gesetzt." : "§eBereitschaft aufgehoben.");
                try { plugin.getScoreboardManager().forceUpdate(player); } catch (Throwable ignored) {}
                if (nr) {
                    // Nur Party-Flow: Leader startet Ready-Check
                    try {
                        var pm = plugin.getPartyManager();
                        var party = pm != null ? pm.getPartyOf(player.getUniqueId()) : null;
                        if (party == null) {
                            player.sendMessage("§7Solo: Wähle deine Klasse im Menü – Startet automatisch sobald nur du im Modus bist.");
                        } else if (party.getLeader().equals(player.getUniqueId())) {
                            int voteSec = Math.max(5, plugin.getConfigUtil().getInt("lobby.party-vote.seconds", 15));
                            plugin.getGameManager().beginPartyStartVote(party, voteSec);
                        } else {
                            player.sendMessage("§7Warte auf Party-Leader für Start-Abstimmung.");
                        }
                    } catch (Throwable ignored) {}
                }
                else {
                    // Ready entfernt -> Countdown ggf. abbrechen
                    try { plugin.getGameManager().abortStartCountdown("Player unready"); } catch (Throwable ignored) {}
                }
                return;
            }
            default -> {}
        }

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
                // Kein Auto-Start mehr hier; Party-Flow startet nach gemeinsamer Klassenwahl über GameManager
                player.sendMessage("§aKlasse gewählt: §f" + chosen.name());
                try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f); } catch (Throwable ignored) {}
                try { plugin.getScoreboardManager().forceUpdate(player); } catch (Throwable ignored) {}
                // Solo-Start: wenn allein im Survivors-Kontext und Klasse gewählt, startet GameManager.trySoloAutoStart
                try { plugin.getGameManager().trySoloAutoStart(player); } catch (Throwable ignored) {}
            }
            guiManager.openMainMenu(player);
            return;
        }

        if (action.startsWith("glyph_pickup_select:")) {
            try {
                // Extract full glyph key after the prefix. This may contain ':' so use substring.
                String full = action.substring("glyph_pickup_select:".length());
                if (full == null || full.isEmpty()) return;
                String glyphKeyFull = full;
                // Derive abilityKey from glyphKey (format ability:glyphId)
                String abilityKey = glyphKeyFull.contains(":") ? glyphKeyFull.split(":", 2)[0] : glyphKeyFull;
                // Directly apply the glyph to the player's SurvivorPlayer
                try {
                    var sp = plugin.getPlayerManager().get(player.getUniqueId());
                    if (sp == null) {
                        player.sendMessage("§cFehler: Spielerprofil nicht geladen.");
                    } else {
                        boolean ok = sp.addGlyph(abilityKey, glyphKeyFull);
                        if (ok) {
                            plugin.getPlayerDataManager().saveAsync(sp);
                            try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph erhalten: " + glyphKeyFull); } catch (Throwable ignored) {}
                            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f); } catch (Throwable ignored) {}
                            // If ability now has 3 filled glyphs, level up the ability
                            try {
                                java.util.List<String> gls = sp.getGlyphs(abilityKey);
                                long filled = gls.stream().filter(s -> s != null && !s.isEmpty()).count();
                                if (filled >= 3) {
                                    boolean inc = sp.incrementAbilityLevel(abilityKey, 1);
                                    if (inc) {
                                        plugin.getPlayerDataManager().saveAsync(sp);
                                        try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {}
                                        try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f); } catch (Throwable ignored) {}
                                    }
                                }
                            } catch (Throwable ignored) {}
                         } else {
                             // Could be duplicate or full
                             try { org.bysenom.minecraftSurvivors.util.Msg.err(player, "Glyph konnte nicht hinzugefügt werden (bereits vorhanden oder volle Slots)"); } catch (Throwable ignored) {}
                         }
                     }
                 } catch (Throwable ex) {
                     try { player.sendMessage("§cFehler beim Hinzufügen der Glyphe: " + ex.getMessage()); } catch (Throwable ignored) {}
                 }
                 // mark handled and resume
                 try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId()); } catch (Throwable ignored) {}
                 try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false); } catch (Throwable ignored) {}
                 try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                 try { player.closeInventory(); } catch (Throwable ignored) {}
             } catch (Throwable ignored) {}
             return;
         }

        if (action.startsWith("glyph_socket:")) {
            try {
                String[] parts = action.split(":", 3);
                if (parts.length < 3) return;
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                // If player has a pending glyph from pickup for this ability, apply it directly
                try {
                    String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(player.getUniqueId());
                    if (pending != null && pending.startsWith(abilityKey + ":")) {
                        var sp2 = plugin.getPlayerManager().get(player.getUniqueId());
                        if (sp2 != null) {
                            boolean ok = sp2.replaceGlyph(abilityKey, slot, pending);
                            if (ok) {
                                plugin.getPlayerDataManager().saveAsync(sp2);
                                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + pending);
                                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.markSelectionHandled(player.getUniqueId());
                                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false);
                                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearSelectionContext(player.getUniqueId());
                                try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                                try { player.closeInventory(); } catch (Throwable ignored) {}

                                // check fullness and level up if needed
                                try {
                                    java.util.List<String> gls = sp2.getGlyphs(abilityKey);
                                    long filled = gls.stream().filter(s -> s != null && !s.isEmpty()).count();
                                    if (filled >= 3) {
                                        boolean inc = sp2.incrementAbilityLevel(abilityKey, 1);
                                        if (inc) {
                                            plugin.getPlayerDataManager().saveAsync(sp2);
                                            try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {}
                                            try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f); } catch (Throwable ignored) {}
                                        }
                                    }
                                } catch (Throwable ignored) {}

                                return;
                            } else {
                                org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                                // fallthrough to open selection UI as before
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                try { plugin.getGameManager().pauseForPlayer(player.getUniqueId()); } catch (Throwable ignored) {}
                var choices = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.forAbility(abilityKey);
                var sel = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Wähle Glyph").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                int si = 0;
                for (var gd : choices) {
                    var gl = new java.util.ArrayList<net.kyori.adventure.text.Component>();
                    gl.add(net.kyori.adventure.text.Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                    String act = "glyph_select:" + abilityKey + ":" + slot + ":" + gd.key;
                    sel.setItem(si, GuiTheme.createAction(plugin, gd.icon, net.kyori.adventure.text.Component.text(gd.name).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, act, false));
                    if (++si >= sel.getSize()) break;
                }
                sel.setItem(8, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Entfernen").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Glyph aus dem Sockel")), "glyph_remove:"+abilityKey+":"+slot, false));
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), true);
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionContext(player.getUniqueId(), abilityKey, slot);
                player.openInventory(sel);
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("glyph_select:")) {
            try {
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
                        // check fullness and level up if needed
                        try {
                            java.util.List<String> gls = sp.getGlyphs(abilityKey);
                            long filled = gls.stream().filter(s -> s != null && !s.isEmpty()).count();
                            if (filled >= 3) {
                                boolean inc = sp.incrementAbilityLevel(abilityKey, 1);
                                if (inc) {
                                    plugin.getPlayerDataManager().saveAsync(sp);
                                    try { org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Deine Fähigkeit wurde verbessert: " + abilityKey); } catch (Throwable ignored) {}
                                    try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f); } catch (Throwable ignored) {}
                                }
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
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("glyph_remove:")) {
            try {
                String[] parts = action.split(":", 3);
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
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("cfg_edit:")) {
            String fullPath = action.substring("cfg_edit:".length());
            try {
                guiManager.openAdminConfigKeyEditor(player, fullPath);
            } catch (Throwable t) { plugin.getLogger().warning("openAdminConfigKeyEditor failed: " + t.getMessage()); }
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

        if (action.startsWith("cfg_toggle:")) {
            String fullPath = action.substring("cfg_toggle:".length());
            plugin.getLogger().info("GuiClickListener: toggle cfg '" + fullPath + "' by=" + player.getName());
            try {
                var cfg = plugin.getConfigUtil().getConfig();
                Object v = cfg.get(fullPath);
                if (v instanceof Boolean cur) {
                    plugin.getConfigUtil().setValue(fullPath, !cur);
                } else {
                    player.sendMessage("§cNicht Boolean: " + fullPath);
                }
            } catch (Throwable t) { plugin.getLogger().warning("cfg_toggle failed: " + t.getMessage()); player.sendMessage("§cFehler beim Setzen"); }
            try { guiManager.openAdminConfigKeyEditor(player, fullPath); } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("cfg_inc:") || action.startsWith("cfg_dec:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 3) {
                String op = parts[0];
                String fullPath = parts[1];
                double step = 0; try { step = Double.parseDouble(parts[2]); } catch (Throwable ignored) {}
                try {
                    var cfg = plugin.getConfigUtil().getConfig();
                    Object cur = cfg.get(fullPath);
                    if (cur instanceof Number n) {
                        double curd = n.doubleValue();
                        double nw = op.equals("cfg_inc") ? curd + step : curd - step;
                        if (cur instanceof Integer || (Math.floor(curd) == curd && Math.floor(step) == step)) {
                            plugin.getConfigUtil().setValue(fullPath, (int) Math.round(nw));
                        } else {
                            plugin.getConfigUtil().setValue(fullPath, nw);
                        }
                    } else {
                        player.sendMessage("§cNicht numerisch: " + fullPath);
                    }
                } catch (Throwable t) { plugin.getLogger().warning("cfg_inc/dec failed: " + t.getMessage()); player.sendMessage("§cFehler beim Setzen"); }
                try { guiManager.openAdminConfigKeyEditor(player, fullPath); } catch (Throwable ignored) {}
            }
        }

        if (action.startsWith("party_vote_yes:")) {
            try {
                String leaderStr = action.substring("party_vote_yes:".length());
                java.util.UUID leader = java.util.UUID.fromString(leaderStr);
                plugin.getGameManager().handlePartyVote(leader, player.getUniqueId(), true);
            } catch (Throwable ignored) {}
            return;
        }
        if (action.startsWith("party_vote_no:")) {
            try {
                String leaderStr = action.substring("party_vote_no:".length());
                java.util.UUID leader = java.util.UUID.fromString(leaderStr);
                plugin.getGameManager().handlePartyVote(leader, player.getUniqueId(), false);
            } catch (Throwable ignored) {}
            return;
        }
    }
}
