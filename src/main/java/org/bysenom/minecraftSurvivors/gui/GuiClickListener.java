// File: src/main/java/org/bysenom/minecraftSurvivors/gui/GuiClickListener.java
package org.bysenom.minecraftSurvivors.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
        String action = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (action == null) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();

        switch (action) {
            case "open_profile":
                // simple profile view -> show levelup menu or info
                guiManager.openLevelUpMenu(player, Math.max(1, plugin.getPlayerManager().get(player.getUniqueId()).getClassLevel()));
                return;
            case "start_wizard":
                guiManager.openClassSelection(player);
                return;
            case "start":
                player.sendMessage("§eBitte nutze 'Spiel starten' und wähle eine Klasse.");
                return;
            // Klassenwahl-Actions (wiederhergestellt)
            case "select_shaman_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN);
                // finalize selection immediately (no extra-skill)
                finalizeClassSelection(player);
                return;
            case "select_pyromancer_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER);
                finalizeClassSelection(player);
                return;
            case "select_ranger_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER);
                finalizeClassSelection(player);
                return;
            case "select_paladin_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN);
                finalizeClassSelection(player);
                return;
            case "party":
                guiManager.openPartyMenu(player);
                return;
            case "open_class_select":
                guiManager.openClassSelection(player);
                return;
            case "powerup":
                // open shop/powerup overview (use reflection to avoid compile-time coupling)
                try {
                    java.lang.reflect.Method m = guiManager.getClass().getMethod("openShop", org.bukkit.entity.Player.class);
                    m.invoke(guiManager, player);
                } catch (Throwable t) {
                    // fallback: send message
                    player.sendMessage("§cShop derzeit nicht verfügbar.");
                }
                return;
            case "party_invite_list":
                guiManager.openPartyInviteList(player);
                return;
            case "stats":
                guiManager.openStatsMenu(player);
                return;
            case "config":
                if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung für Config."); return; }
                guiManager.openConfigMenu(player);
                return;
            case "admin":
                guiManager.openAdminPanel(player);
                return;
            case "meta":
                guiManager.openMetaMenu(player);
                return;
            case "info":
                guiManager.openInfoMenu(player);
                return;
            case "back":
                guiManager.openMainMenu(player);
                return;
            case "stats_mode_actionbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.ACTIONBAR);
                player.sendMessage("§aStats-Modus: ActionBar");
                return;
            case "stats_mode_bossbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.BOSSBAR);
                player.sendMessage("§aStats-Modus: BossBar");
                return;
            case "stats_mode_scoreboard":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD);
                player.sendMessage("§aStats-Modus: Scoreboard");
                return;
            case "stats_mode_off":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.OFF);
                player.sendMessage("§aStats-Modus: Aus");
                return;
            case "config_reload":
                plugin.getGameManager().reloadConfigAndApply();
                player.sendMessage("§aConfig neu geladen.");
                return;
            case "config_preset_flashy":
                guiManager.applyPreset("flashy");
                player.sendMessage("§aPreset 'flashy' angewendet.");
                return;
            case "config_preset_epic":
                guiManager.applyPreset("epic");
                player.sendMessage("§aPreset 'epic' angewendet.");
                return;
            case "party_create":
                if (plugin.getPartyManager().createParty(player.getUniqueId())) player.sendMessage("§aParty erstellt."); else player.sendMessage("§cDu bist bereits in einer Party.");
                guiManager.openPartyMenu(player);
                return;
            case "party_join_invite":
                java.util.UUID leader = plugin.getPartyManager().getPendingInviteLeader(player.getUniqueId());
                if (leader != null && plugin.getPartyManager().join(player.getUniqueId(), leader)) player.sendMessage("§aEinladung angenommen."); else player.sendMessage("§cKeine gültige Einladung.");
                guiManager.openPartyMenu(player);
                return;
            case "party_leave":
                if (plugin.getPartyManager().leave(player.getUniqueId())) player.sendMessage("§aParty verlassen/aufgelöst."); else player.sendMessage("§cDu bist in keiner Party.");
                guiManager.openPartyMenu(player);
                return;
            case "adm_coins":
                plugin.getPlayerManager().get(player.getUniqueId()).addCoins(100);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+100 Coins");
                try { player.showTitle(net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("+100 Coins").color(net.kyori.adventure.text.format.NamedTextColor.GOLD), net.kyori.adventure.text.Component.text("Admin Reward").color(net.kyori.adventure.text.format.NamedTextColor.GRAY))); } catch (Throwable ignored) {}
                guiManager.openAdminPanel(player);
                return;
            case "adm_essence":
                org.bysenom.minecraftSurvivors.model.MetaProfile mp = plugin.getMetaManager().get(player.getUniqueId());
                mp.addEssence(10);
                plugin.getMetaManager().save(mp);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+10 Essence");
                try { player.sendActionBar(net.kyori.adventure.text.Component.text("+10 Essence").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)); } catch (Throwable ignored) {}
                guiManager.openAdminPanel(player);
                return;
            case "adm_spawn":
                org.bukkit.Location loc = player.getLocation();
                for (int i=0; i<5; i++) {
                    try {
                        org.bukkit.Location s = loc.clone().add((i-2)*1.5, 0, 2.0);
                        org.bukkit.entity.LivingEntity le = (org.bukkit.entity.LivingEntity) player.getWorld().spawnEntity(s, org.bukkit.entity.EntityType.ZOMBIE);
                        plugin.getGameManager().getSpawnManager().markAsWave(le);
                    } catch (Throwable ignored) {}
                }
                org.bysenom.minecraftSurvivors.util.Msg.info(player, "5 Testmobs gespawnt");
                try { player.sendActionBar(net.kyori.adventure.text.Component.text("Spawned 5 test mobs").color(net.kyori.adventure.text.format.NamedTextColor.GREEN)); } catch (Throwable ignored) {}
                guiManager.openAdminPanel(player);
                return;
            case "adm_levelup":
                guiManager.openLevelUpMenu(player, 1);
                return;
            case "adm_ready_toggle": {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                sp.setReady(!sp.isReady());
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Ready: "+sp.isReady());
                guiManager.openAdminPanel(player);
                return;
            }
            case "adm_force_start": {
                guiManager.getGameManager().startGameWithCountdown(3);
                try { player.showTitle(net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("Start!"), net.kyori.adventure.text.Component.text("3s Countdown").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW))); } catch (Throwable ignored) {}
                player.closeInventory();
                return;
            }
            case "adm_give_dash": {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp.addSkill("dash")) {
                    org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Dash erhalten");
                    try { player.sendActionBar(net.kyori.adventure.text.Component.text("Skill: Dash").color(net.kyori.adventure.text.format.NamedTextColor.AQUA)); } catch (Throwable ignored) {}
                } else {
                    org.bysenom.minecraftSurvivors.util.Msg.warn(player, "Skill-Slots voll (max "+sp.getMaxSkillSlots()+")");
                }
                guiManager.openAdminPanel(player);
                return;
            }
            case "adm_skillslot": {
                // Grant a permanent skill slot via MetaProgressionManager
                org.bysenom.minecraftSurvivors.manager.MetaProgressionManager mpm = plugin.getMetaManager();
                org.bysenom.minecraftSurvivors.model.MetaProfile mp2 = mpm.get(player.getUniqueId());
                mp2.addPermSkillSlots(1);
                mpm.save(mp2);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Meta: +1 Skill-Slot (permanent)");
                try { player.showTitle(net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("Skill Slot +1").color(net.kyori.adventure.text.format.NamedTextColor.AQUA), net.kyori.adventure.text.Component.text("Meta-Upgrade").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE))); } catch (Throwable ignored) {}
                guiManager.openAdminPanel(player);
                return;
            }
        }
        // Wizard actions
        if (action.startsWith("select_") && action.endsWith("_wizard")) {
            return; // bereits oben behandelt
        }
        if (action.startsWith("party_invite:")) {
            try {
                java.util.UUID target = java.util.UUID.fromString(action.substring("party_invite:".length()));
                if (plugin.getPartyManager().invite(player.getUniqueId(), target, 60)) {
                    player.sendMessage("§aEinladung gesendet.");
                } else {
                    player.sendMessage("§cInvite fehlgeschlagen (bist du Leader?).");
                }
                guiManager.openPartyInviteList(player);
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        if (action.equals("party_back")) {
            guiManager.openPartyMenu(player);
            return;
        }
        // Party member clicked: open context actions
        if (action.startsWith("party_member:")) {
            String uidStr = action.substring("party_member:".length());
            try {
                java.util.UUID uid = java.util.UUID.fromString(uidStr);
                // Open a small context menu: If player is leader show promote/kick
                org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
                org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Party Aktion").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                // Who is clicked
                org.bukkit.entity.Player clickedPlayer = org.bukkit.Bukkit.getPlayer(uid);
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text(clickedPlayer != null ? clickedPlayer.getName() : uid.toString()).color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                inv.setItem(3, GuiTheme.createAction(plugin, org.bukkit.Material.PLAYER_HEAD, net.kyori.adventure.text.Component.text("Spieler").color(net.kyori.adventure.text.format.NamedTextColor.WHITE), lore, "noop", false));
                // If leader, show promote/kick
                if (party != null && party.getLeader().equals(player.getUniqueId())) {
                    inv.setItem(1, GuiTheme.createAction(plugin, org.bukkit.Material.LIME_DYE, net.kyori.adventure.text.Component.text("Promote").color(net.kyori.adventure.text.format.NamedTextColor.GREEN), java.util.List.of(net.kyori.adventure.text.Component.text("Macht zum Leader")), "party_promote:"+uid, false));
                    if (!uid.equals(player.getUniqueId())) inv.setItem(5, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Kick").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Spieler aus Party")), "party_kick:"+uid, false));
                }
                inv.setItem(7, GuiTheme.createAction(plugin, org.bukkit.Material.ARROW, net.kyori.adventure.text.Component.text("Zurück").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Schließt dieses Menü")), "party_back", false));
                player.openInventory(inv);
            } catch (Throwable ignored) {}
            return;
        }

        // Glyph socket interaction: open glyph selection for a given ability and slot
        if (action.startsWith("glyph_socket:")) {
            try {
                String[] parts = action.split(":", 3);
                if (parts.length < 3) { return; }
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                // Build a small selection UI of available glyphs for that ability
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
                // Also add remove option
                sel.setItem(8, GuiTheme.createAction(plugin, org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Entfernen").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Entfernt Glyph aus dem Sockel")), "glyph_remove:"+abilityKey+":"+slot, false));
                // mark selection opened so the game stays paused while player chooses
                try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), true); org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionContext(player.getUniqueId(), abilityKey, slot); } catch (Throwable ignored) {}
                player.openInventory(sel);
            } catch (Throwable ignored) {}
            return;
        }

        // glyph selection clicked in the selection UI
        if (action.startsWith("glyph_select:")) {
            // glyph_select:ability:slot:glyphKey
            try {
                String[] parts = action.split(":", 4);
                if (parts.length < 4) return;
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                String glyphKey = parts[3];
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp == null) return;

                String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(player.getUniqueId());
                try { plugin.getLogger().fine("glyph_select clicked by " + player.getName() + " (pending=" + pending + ", clicked=" + glyphKey + ", ability=" + abilityKey + ", slot=" + slot + ")"); } catch (Throwable ignored) {}

                if (pending == null) {
                    org.bysenom.minecraftSurvivors.util.Msg.err(player, "Keine Glyphen-Pending zum Einsetzen gefunden.");
                    new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                    return;
                }

                String pendingNorm = pending.trim().toLowerCase(java.util.Locale.ROOT);
                String clickedNorm = glyphKey != null ? glyphKey.trim().toLowerCase(java.util.Locale.ROOT) : null;

                // exact match
                if (clickedNorm != null && pendingNorm.equals(clickedNorm)) {
                    org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.consumePendingGlyph(player.getUniqueId());
                    boolean ok = sp.replaceGlyph(abilityKey, slot, glyphKey);
                    if (ok) {
                        plugin.getPlayerDataManager().saveAsync(sp);
                        org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + glyphKey);
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                    } else {
                        org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setPendingGlyphWithLog(player.getUniqueId(), pending);
                    }
                    // selection closed now
                    try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false); org.bysenom.minecraftSurvivors.listener.clearSelectionContext(player.getUniqueId()); } catch (Throwable ignored) {}
                    new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                    return;
                }

                // fallback: if pending glyph belongs to the same ability as the menu, accept pending
                String pendingAbility = null;
                try { pendingAbility = pending.split(":", 2)[0]; } catch (Throwable ignored) { pendingAbility = null; }
                String abilityNorm = abilityKey != null ? abilityKey.trim().toLowerCase(java.util.Locale.ROOT) : null;
                if (pendingAbility != null && abilityNorm != null && pendingAbility.trim().toLowerCase(java.util.Locale.ROOT).equals(abilityNorm)) {
                    try { plugin.getLogger().info("Glyph key mismatch but same ability - applying pending glyph as fallback for player=" + player.getName() + " uuid=" + player.getUniqueId() + " pending=" + pending + " clicked=" + glyphKey + " ability=" + abilityKey + " slot=" + slot); } catch (Throwable ignored) {}
                    org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.consumePendingGlyph(player.getUniqueId());
                    boolean ok = sp.replaceGlyph(abilityKey, slot, pending);
                    if (ok) {
                        plugin.getPlayerDataManager().saveAsync(sp);
                        org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph eingesetzt: " + pending + " (Fallback)");
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.clearPendingFor(player.getUniqueId());
                    } else {
                        org.bysenom.minecraftSurvivors.util.Msg.err(player, "Konnte Glyph nicht einsetzen (Max 3 oder bereits vorhanden)");
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setPendingGlyphWithLog(player.getUniqueId(), pending);
                    }
                    new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                    return;
                }

                // final mismatch -> log and inform
                try { plugin.getLogger().warning("Glyph mismatch for player=" + player.getName() + " uuid=" + player.getUniqueId() + " pending(raw)=" + pending + " clicked(raw)=" + glyphKey + " pendingNorm=" + pendingNorm + " clickedNorm=" + clickedNorm + " ability=" + abilityKey + " slot=" + slot); } catch (Throwable ignored) {}
                org.bysenom.minecraftSurvivors.util.Msg.err(player, "Ausgewählte Glyphe stimmt nicht mit der aufgesammelten Glyphe überein.");
                new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
                return;
            } catch (Throwable ignored) {}
            return;
        }

        if (action.startsWith("glyph_remove:")) {
            try {
                String[] parts = action.split(":", 3);
                String abilityKey = parts[1];
                int slot = Integer.parseInt(parts[2]);
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp == null) return;
                boolean ok = sp.replaceGlyph(abilityKey, slot, null);
                if (ok) { plugin.getPlayerDataManager().saveAsync(sp); org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Glyph entfernt"); }
                // selection closed now
                try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(player.getUniqueId(), false); org.bysenom.minecraftSurvivors.listener.clearSelectionContext(player.getUniqueId()); } catch (Throwable ignored) {}
                new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu(player, sp, abilityKey);
            } catch (Throwable ignored) {}
            return;
        }
    }

    // Helper: finalize class selection directly (no extra-skill)
    private void finalizeClassSelection(org.bukkit.entity.Player player) {
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
        try {
            String classAbility = null;
            org.bysenom.minecraftSurvivors.model.PlayerClass pc = sp.getSelectedClass();
            if (pc != null) {
                classAbility = switch (pc) {
                    case SHAMAN -> "ab_lightning";
                    case PYROMANCER -> "ab_fire";
                    case RANGER -> "ab_ranged";
                    case PALADIN -> "ab_heal_totem";
                };
            }
            if (classAbility != null) {
                int placed = sp.addAbilityAtFirstFreeIndex(classAbility, 1);
                if (placed < 0) {
                    // open replace UI (class selection should allow replacing class items)
                    new org.bysenom.minecraftSurvivors.gui.ReplaceAbilityMenu(player, sp, classAbility, 1.0, true).open();
                    try { player.closeInventory(); } catch (Throwable ignored) {}
                    return;
                } else {
                    sp.setAbilityOrigin(classAbility, "class");
                    // Ensure class ability is permanently available/unlocked
                    try { sp.unlockAbility(classAbility); plugin.getPlayerDataManager().save(sp); } catch (Throwable ignored) {}
                    // visual feedback: update client hotbar and play particles/sound
                    try {
                        player.updateInventory();
                        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0,1.2,0), 40, 0.4, 0.6, 0.4, 0.02);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                        player.sendActionBar(net.kyori.adventure.text.Component.text("Neue Klassen-Ability erhalten!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) { plugin.getLogger().warning("Error finalizing class selection: " + t.getMessage()); }
        // mark ready and check start
        sp.setReady(true);
        boolean allReady = true;
        for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer osp = plugin.getPlayerManager().get(op.getUniqueId());
            if (osp == null || osp.getSelectedClass() == null || !osp.isReady()) { allReady = false; break; }
        }
        try { player.closeInventory(); } catch (Throwable ignored) {}
        if (allReady) guiManager.getGameManager().startGameWithCountdown(5);
        else org.bysenom.minecraftSurvivors.util.Msg.info(player, "Warte auf andere Spieler... (Ready "+(int)org.bukkit.Bukkit.getOnlinePlayers().stream().filter(pp->plugin.getPlayerManager().get(pp.getUniqueId()).isReady()).count()+"/"+org.bukkit.Bukkit.getOnlinePlayers().size()+")");
    }
}
