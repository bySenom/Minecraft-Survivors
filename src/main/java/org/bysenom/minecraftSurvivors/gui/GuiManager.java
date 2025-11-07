package org.bysenom.minecraftSurvivors.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.GameManager;

public class GuiManager {

    private final MinecraftSurvivors plugin;
    private final GameManager gameManager;
    private final NamespacedKey key;


    public GuiManager(MinecraftSurvivors plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.key = new NamespacedKey(plugin, "ms_gui");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Hauptmenü mit neuem Eintrag "Klasse wählen".
     */
    public void openMainMenu(Player p) {
        if (p == null) return;
        // use a larger inventory for nicer layout
        Inventory inv = Bukkit.createInventory(null, 54, GuiTheme.styledTitle("MinecraftSurvivors", "Hauptmenü"));
        // border
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        // player card (center-left)
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        int coins = sp != null ? sp.getCoins() : 0;
        int lvl = sp != null ? sp.getClassLevel() : 1;
        java.util.List<Component> cardLore = new java.util.ArrayList<>();
        cardLore.add(Component.text("Spieler: ").color(NamedTextColor.GRAY).append(Component.text(p.getName()).color(NamedTextColor.AQUA)));
        cardLore.add(Component.text("Coins: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(coins)).color(NamedTextColor.WHITE)));
        cardLore.add(Component.text("Klassen-Level: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(lvl)).color(NamedTextColor.WHITE)));
        cardLore.add(Component.text("").color(NamedTextColor.DARK_GRAY));
        cardLore.add(Component.text("Klicke hier, um Profil zu öffnen").color(NamedTextColor.GRAY));
        inv.setItem(13, GuiTheme.createAction(plugin, Material.PLAYER_HEAD, Component.text("Profil").color(NamedTextColor.GOLD), cardLore, "open_profile", true));

        // Big Start Button
        inv.setItem(22, GuiTheme.createAction(plugin, Material.GREEN_STAINED_GLASS, Component.text("Spiel starten").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Wähle eine Klasse & starte die Session").color(NamedTextColor.GRAY)), "start_wizard", true));

        // Quick actions fixed slots (classes / party / shop / stats / meta / info)
        inv.setItem(20, GuiTheme.createAction(plugin, Material.ENCHANTED_BOOK, Component.text("Klassen wählen").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Wähle deine Klasse").color(NamedTextColor.GRAY)), "open_class_select", false));
        inv.setItem(24, GuiTheme.createAction(plugin, Material.PLAYER_HEAD, Component.text("Party").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Erstelle/Verwalte deine Party").color(NamedTextColor.GRAY)), "party", false));
        inv.setItem(30, GuiTheme.createAction(plugin, Material.NETHER_STAR, Component.text("Powerups / Items").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Shop & Powerups").color(NamedTextColor.GRAY)), "powerup", false));
        inv.setItem(32, GuiTheme.createAction(plugin, Material.CLOCK, Component.text("Stats Anzeige").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("DPS/HPS Anzeige-Modus").color(NamedTextColor.GRAY)), "stats", false));
        inv.setItem(34, GuiTheme.createAction(plugin, Material.NETHERITE_INGOT, Component.text("Meta Shop").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Permanente Upgrades").color(NamedTextColor.GRAY)), "meta", false));
        inv.setItem(41, GuiTheme.createAction(plugin, Material.PAPER, Component.text("Info").color(NamedTextColor.WHITE), java.util.List.of(Component.text("Über das Spiel & Hilfe").color(NamedTextColor.GRAY)), "info", false));

        // Admin button (if permitted)
        if (p.hasPermission("minecraftsurvivors.admin")) {
            inv.setItem(4, GuiTheme.createAction(plugin, Material.COMPARATOR, Component.text("Admin Panel").color(NamedTextColor.RED), java.util.List.of(Component.text("Debug & Test-Tools").color(NamedTextColor.GRAY)), "admin", false));
        }

        // Footer status block (right-bottom)
        java.util.List<Component> footerLore = new java.util.ArrayList<>();
        String mode = "-"; try { mode = plugin.getStatsDisplayManager().getMode().name().toLowerCase(); } catch (Throwable ignored) {}
        footerLore.add(Component.text("Stats: ").color(NamedTextColor.GRAY).append(Component.text(mode).color(NamedTextColor.AQUA)));
        String eta = plugin.getShopManager().formatRemainingHHMMSS();
        footerLore.add(Component.text("Shop Reset: ").color(NamedTextColor.GRAY).append(Component.text(eta).color(NamedTextColor.YELLOW)));
        int dailyN = plugin.getConfigUtil().getInt("shop.daily.max-weapons", 6);
        footerLore.add(Component.text("Daily Offers: ").color(NamedTextColor.GRAY).append(Component.text(dailyN).color(NamedTextColor.GOLD)));
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) {
            int online = pm.onlineMembers(party).size();
            int total = party.getMembers().size();
            footerLore.add(Component.text("Party: ").color(NamedTextColor.GRAY).append(Component.text(online + "/" + total).color(NamedTextColor.GREEN)));
        } else {
            footerLore.add(Component.text("Party: keine").color(NamedTextColor.DARK_GRAY));
        }
        int essence = plugin.getMetaManager().get(p.getUniqueId()).getEssence();
        footerLore.add(Component.text("Essence: ").color(NamedTextColor.LIGHT_PURPLE).append(Component.text(String.valueOf(essence)).color(NamedTextColor.WHITE)));
        int totalPlayers = org.bukkit.Bukkit.getOnlinePlayers().size();
        int ready = (int) org.bukkit.Bukkit.getOnlinePlayers().stream().filter(op -> { try { org.bysenom.minecraftSurvivors.model.SurvivorPlayer osp = plugin.getPlayerManager().get(op.getUniqueId()); return osp != null && osp.isReady(); } catch (Throwable t) { return false; } }).count();
        footerLore.add(Component.text("Ready: ").color(NamedTextColor.GRAY).append(Component.text(ready + "/" + totalPlayers).color(NamedTextColor.GREEN)));
        inv.setItem(52, GuiTheme.createAction(plugin, Material.MAP, Component.text("Status").color(NamedTextColor.WHITE), footerLore, "noop", false));

        p.openInventory(inv);
    }

    public void openMetaMenu(Player p) {
        if (p == null) return;
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, Component.text("Meta-Progression").color(NamedTextColor.LIGHT_PURPLE));
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        java.util.List<java.util.Map<?, ?>> list = cfg.getMapList("meta.shop");
        int slot = 10;
        for (java.util.Map<?, ?> m : list) {
            Object keyObj = m.containsKey("key") ? m.get("key") : "";
            String keyId = String.valueOf(keyObj);
            Object nameObj = m.containsKey("name") ? m.get("name") : keyId;
            String name = String.valueOf(nameObj);
            Object typeObj = m.containsKey("type") ? m.get("type") : "";
            String type = String.valueOf(typeObj);
            Object priceObj = m.containsKey("price") ? m.get("price") : 10;
            int price = Integer.parseInt(String.valueOf(priceObj));
            Object stepObj = m.containsKey("step") ? m.get("step") : 0.01;
            double step = Double.parseDouble(String.valueOf(stepObj));
            Object capObj = m.containsKey("cap") ? m.get("cap") : 0.5;
            double cap = Double.parseDouble(String.valueOf(capObj));
            Material mat = Material.AMETHYST_SHARD;
            if (type.equalsIgnoreCase("DAMAGE_MULT")) mat = Material.DIAMOND_SWORD;
            else if (type.equalsIgnoreCase("MOVE_SPEED")) mat = Material.SUGAR;
            else if (type.equalsIgnoreCase("ATTACK_SPEED")) mat = Material.FEATHER;
            else if (type.equalsIgnoreCase("RESIST")) mat = Material.SHIELD;
            else if (type.equalsIgnoreCase("LUCK")) mat = Material.RABBIT_FOOT;
            else if (type.equalsIgnoreCase("HEALTH_HEARTS")) mat = Material.GOLDEN_APPLE;
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text(name).color(NamedTextColor.WHITE));
            lore.add(Component.text("Preis: "+price+" Essence").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("Schritt: +"+step).color(NamedTextColor.GRAY));
            lore.add(Component.text("Cap: "+cap).color(NamedTextColor.DARK_GRAY));
            inv.setItem(slot, createGuiItem(mat, Component.text(keyId).color(NamedTextColor.AQUA), lore, "meta_buy:"+keyId, false));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    /**
     * Öffnet die Klassenwahl; aktuell ein Beispiel: Shamanen.
     */
    public void openClassSelection(Player p) {
        if (p == null) return;
        Component title = Component.text("Klassenwahl").color(NamedTextColor.AQUA);
        Inventory inv = Bukkit.createInventory(null, 27, title);
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        org.bysenom.minecraftSurvivors.model.PlayerClass current = sp != null ? sp.getSelectedClass() : null;
        inv.setItem(10, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN), Component.text("Shamanen ⚡").color(NamedTextColor.LIGHT_PURPLE),
                java.util.List.of(Component.text("Main: Blitz"), Component.text("Solider Allrounder").color(NamedTextColor.GRAY)), "select_shaman_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN));
        inv.setItem(12, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER), Component.text("Pyromant 🔥").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("Main: Feuer & DoT"), Component.text("Nahbereich-AoE").color(NamedTextColor.GRAY)), "select_pyromancer_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER));
        inv.setItem(14, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER), Component.text("Waldläufer 🏹").color(NamedTextColor.GREEN),
                java.util.List.of(Component.text("Main: Fernschuss"), Component.text("Single-Target").color(NamedTextColor.GRAY)), "select_ranger_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER));
        inv.setItem(16, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN), Component.text("Paladin ✨").color(NamedTextColor.AQUA),
                java.util.List.of(Component.text("Main: Heilige Nova"), Component.text("AoE + Heal").color(NamedTextColor.GRAY)), "select_paladin_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Abbrechen").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openInfoMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("MinecraftSurvivors - Info").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, createGuiItem(Material.PAPER, Component.text("Über das Spiel").color(NamedTextColor.GOLD),
                List.of(Component.text("V: Vampire Survivors like Mini-Game"), Component.text("bySenom").color(NamedTextColor.GRAY)), "info_close"));
        p.openInventory(inv);
    }

    /**
     * Öffnet das Level-Up-Menü für einen Spieler mit einer Auswahl entsprechend `level`.
     */
    public void openLevelUpMenu(Player p, int level) {
        if (p == null) return;
        // Pause only this player while they choose a level reward (local pause)
        try {
            if (gameManager != null) gameManager.pauseForPlayer(p.getUniqueId());
        } catch (Throwable ignored) {}
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        LevelUpMenu menu = new LevelUpMenu(p, sp, level);
        p.openInventory(menu.getInventory());
    }

    public void openPartyMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Party").color(NamedTextColor.GOLD));
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        java.util.UUID pendingLeader = pm != null ? pm.getPendingInviteLeader(p.getUniqueId()) : null;
        inv.setItem(10, createGuiItem(Material.SLIME_BALL, Component.text("Erstellen").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Neue Party erstellen")), "party_create"));
        // Einladen per Liste
        inv.setItem(12, createGuiItem(Material.PLAYER_HEAD, Component.text("Einladen (Liste)").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Klicke, um Online-Spieler zu sehen").color(NamedTextColor.GRAY)), "party_invite_list"));
        if (pendingLeader != null) {
            org.bukkit.entity.Player lp = org.bukkit.Bukkit.getPlayer(pendingLeader);
            String name = lp != null ? lp.getName() : "Leader";
            inv.setItem(14, createGuiItem(Material.LIME_DYE, Component.text("Einladung annehmen").color(NamedTextColor.AQUA), java.util.List.of(Component.text("von "+name)), "party_join_invite"));
        } else {
            inv.setItem(14, createGuiItem(Material.GRAY_DYE, Component.text("Keine Einladung").color(NamedTextColor.GRAY), java.util.List.of(Component.text("Du hast derzeit keine offene Einladung")), "noop"));
        }
        // Mitglieder-Info
        java.util.List<Component> lore = new java.util.ArrayList<>();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) {
            lore.add(Component.text("Mitglieder:").color(NamedTextColor.GOLD));
            for (java.util.UUID u : party.getMembers()) {
                org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                String name = pl != null ? pl.getName() : u.toString();
                // create clickable item per member instead of plain lore line
                java.util.List<Component> memberLore = new java.util.ArrayList<>();
                memberLore.add(Component.text("Klicken für Aktionen").color(NamedTextColor.GRAY));
                inv.addItem(createGuiItem(Material.PLAYER_HEAD, Component.text(name).color(NamedTextColor.AQUA), memberLore, "party_member:" + u, false));
            }
        } else {
            lore.add(Component.text("Keine Party").color(NamedTextColor.GRAY));
        }
        // keep a summary book at slot 11 as well
        inv.setItem(11, createGuiItem(Material.BOOK, Component.text("Mitglieder").color(NamedTextColor.AQUA), lore, "noop"));

        inv.setItem(16, createGuiItem(Material.BARRIER, Component.text("Verlassen/Auflösen").color(NamedTextColor.RED), java.util.List.of(Component.text("Party verlassen oder als Leader auflösen")), "party_leave"));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openPartyInviteList(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Spieler einladen").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        java.util.Set<java.util.UUID> skip = new java.util.HashSet<>();
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) skip.addAll(party.getMembers());
        int slot = 10;
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(p.getUniqueId())) continue;
            if (skip.contains(online.getUniqueId())) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            try {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                if (sm != null) {
                    sm.setOwningPlayer(online);
                    sm.displayName(Component.text(online.getName(), NamedTextColor.AQUA));
                    sm.lore(java.util.List.of(Component.text("Klicken zum Einladen").color(NamedTextColor.GRAY)));
                    sm.getPersistentDataContainer().set(key, PersistentDataType.STRING, "party_invite:" + online.getUniqueId());
                    head.setItemMeta(sm);
                }
            } catch (Throwable ignored) {}
            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2; // skip border
            if (slot >= inv.getSize() - 9) break;
        }
        inv.setItem(49, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Party-Menü")), "party_back"));
        p.openInventory(inv);
    }

    public void openStatsMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Stats Anzeige").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode mode = plugin.getStatsDisplayManager().getMode();
        inv.setItem(10, createGuiItem(Material.PAPER, Component.text("ActionBar").color(NamedTextColor.WHITE), java.util.List.of(Component.text("XP/Lvl + DPS/HPS in der ActionBar")), "stats_mode_actionbar", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.ACTIONBAR));
        inv.setItem(12, createGuiItem(Material.DRAGON_BREATH, Component.text("BossBar").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Zwei dezente BossBars (DPS/HPS)")), "stats_mode_bossbar", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.BOSSBAR));
        inv.setItem(14, createGuiItem(Material.OAK_SIGN, Component.text("Scoreboard").color(NamedTextColor.AQUA), java.util.List.of(Component.text("HUD nur im Scoreboard-Modus")), "stats_mode_scoreboard", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD));
        inv.setItem(16, createGuiItem(Material.BARRIER, Component.text("Aus").color(NamedTextColor.RED), java.util.List.of(Component.text("Anzeige deaktivieren")), "stats_mode_off", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.OFF));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openConfigMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Konfiguration").color(NamedTextColor.AQUA));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(11, createGuiItem(Material.WRITABLE_BOOK, Component.text("Reload Config").color(NamedTextColor.GOLD), java.util.List.of(Component.text("/msconfig reload ausführen")), "config_reload"));
        inv.setItem(13, createGuiItem(Material.GLOWSTONE_DUST, Component.text("Preset: flashy").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Spawn-Visual-Preset anwenden")), "config_preset_flashy"));
        inv.setItem(15, createGuiItem(Material.ENDER_EYE, Component.text("Preset: epic").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Spawn-Visual-Preset anwenden")), "config_preset_epic"));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void applyPreset(String name) {
        if (name == null) return;
        switch (name.toLowerCase()) {
            case "flashy":
                plugin.getConfigUtil().setValue("spawn.particle", "FLAME");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 14);
                plugin.getConfigUtil().setValue("spawn.particle-points", 20);
                plugin.getConfigUtil().setValue("spawn.particle-count", 3);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.12);
                break;
            case "epic":
                plugin.getConfigUtil().setValue("spawn.particle", "REDSTONE");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 20);
                plugin.getConfigUtil().setValue("spawn.particle-points", 28);
                plugin.getConfigUtil().setValue("spawn.particle-count", 4);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.18);
                break;
            default:
                // subtle as base
                plugin.getConfigUtil().setValue("spawn.particle", "END_ROD");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 8);
                plugin.getConfigUtil().setValue("spawn.particle-points", 12);
                plugin.getConfigUtil().setValue("spawn.particle-count", 1);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.06);
                break;
        }
    }

    private Material materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass pc) {
        if (pc == null) return Material.POPPED_CHORUS_FRUIT;
        return switch (pc) {
            case SHAMAN -> Material.ENCHANTED_BOOK;
            case PYROMANCER -> Material.BLAZE_POWDER;
            case RANGER -> Material.BOW;
            case PALADIN -> Material.SHIELD;
        };
    }

    public void applyShopPurchase(Player p, String key) {
         if (p == null || key == null) return;
         org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
         org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        // key patterns (handled):
        // cosmetic:<key>
        // meta:<key>
        // legacy/disabled: gear:<index>, ability:<key>, glyph:<key> are rejected
         String[] parts = key.split(":", 2);
         if (parts.length != 2) return;
         String cat = parts[0];
         String payload = parts[1];
         // Block any gear/ability/glyph purchases — these are intentionally disabled
         if (cat.equalsIgnoreCase("gear") || cat.equalsIgnoreCase("ability") || cat.equalsIgnoreCase("glyph")) {
             org.bysenom.minecraftSurvivors.util.Msg.err(p, "Dieser Kauf-Typ ist derzeit deaktiviert.");
             try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f); } catch (Throwable ignored) {}
             return;
         }
         String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
         if (sp.getShopLastDay() == null || !today.equals(sp.getShopLastDay())) { sp.setShopLastDay(today); sp.setShopPurchasesToday(0); }
         int maxPerRun = cfg.getInt("shop.limits.max-per-run", 999);
         int maxPerDay = cfg.getInt("shop.limits.max-per-day", 999);
         if (sp.getShopPurchasesRun() >= maxPerRun) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Lauflimit erreicht (max "+maxPerRun+")."); return; }
         if (sp.getShopPurchasesToday() >= maxPerDay) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Tageslimit erreicht (max "+maxPerDay+")."); return; }
        if (cat.equalsIgnoreCase("cosmetic")) {
            // Cosmetics are placeholder for now — just notify (no persistent handling yet)
            org.bysenom.minecraftSurvivors.util.Msg.info(p, "Kosmetik-Käufe sind experimentell — noch nicht implementiert.");
            try { p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_IN, 0.8f, 1.2f); } catch (Throwable ignored) {}
            return;
        }

        // Meta purchases handled by existing code path using 'meta_buy:' actions (handled elsewhere)
     }

    public void openAdminPanel(Player p) {
        if (p == null) return;
        if (!p.hasPermission("minecraftsurvivors.admin")) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Keine Rechte."); return; }
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Admin Panel").color(NamedTextColor.RED));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(10, createGuiItem(Material.GOLD_BLOCK, Component.text("+100 Coins").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Test Coins")), "adm_coins"));
        inv.setItem(12, createGuiItem(Material.AMETHYST_SHARD, Component.text("+10 Essence").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Meta testen")), "adm_essence"));
        inv.setItem(14, createGuiItem(Material.ZOMBIE_HEAD, Component.text("Spawn Testmobs").color(NamedTextColor.GREEN), java.util.List.of(Component.text("5 Gegner um dich")), "adm_spawn"));
        inv.setItem(16, createGuiItem(Material.CHEST, Component.text("LevelUp öffnen").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Debug LevelUp UI")), "adm_levelup"));
        // Config Editor entry
        inv.setItem(8, createGuiItem(Material.CLOCK, Component.text("Config Editor").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Ingame Konfiguration editieren (Admin)")), "admin_config"));
        inv.setItem(20, createGuiItem(Material.LIME_DYE, Component.text("Toggle Ready").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Ready-Flag toggeln")), "adm_ready_toggle"));
        inv.setItem(22, createGuiItem(Material.REDSTONE_BLOCK, Component.text("Force Start").color(NamedTextColor.RED), java.util.List.of(Component.text("Countdown 3s & Start")), "adm_force_start"));
        inv.setItem(18, createGuiItem(Material.EXPERIENCE_BOTTLE, Component.text("Give Skill Slot +1").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Erhöhe die Anzahl der Skill-Slots um 1")), "adm_skillslot"));
        inv.setItem(26, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück")), "back"));
        p.openInventory(inv);
    }

    public void openAdminConfigEditor(Player p) {
         if (p == null) return;
         if (!p.hasPermission("minecraftsurvivors.admin")) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Keine Rechte."); return; }
         org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
         java.util.Set<String> keys = cfg.getKeys(false);
         plugin.getLogger().info("openAdminConfigEditor invoked by " + p.getName() + " - runtime config top keys count=" + (keys==null?0:keys.size()));
         // Fallback: wenn keine keys gefunden werden, versuche die eingebettete resource config.yml zu parsen
         String cfgSource = "runtime";
         if (keys == null || keys.isEmpty()) {
            try {
                java.io.InputStream is = plugin.getResource("config.yml");
                plugin.getLogger().info("AdminConfigEditor: runtime keys empty, plugin.getResource(config.yml)=" + (is != null));
                if (is != null) {
                    java.io.InputStreamReader r = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
                    org.bukkit.configuration.file.FileConfiguration rc = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(r);
                    java.util.Set<String> rkeys = rc.getKeys(false);
                    if (rkeys != null && !rkeys.isEmpty()) {
                        keys = rkeys;
                        cfg = rc; // use resource-backed config for display
                        cfgSource = "embedded-resource";
                        plugin.getLogger().info("AdminConfigEditor: using embedded resource config (fallback), firstKeys=" + firstN(rkeys,5));
                    } else {
                        plugin.getLogger().warning("AdminConfigEditor: embedded config.yml parsed but no top-level keys found");
                    }
                } else {
                    plugin.getLogger().warning("AdminConfigEditor: runtime config has no keys and embedded resource config.yml not found");
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("AdminConfigEditor: failed to load embedded config.yml: " + t.getMessage());
            }
        }
        if (keys == null) keys = java.util.Collections.emptySet();
        // Compute rows so interior capacity ((rows-2)*7) can hold all keys.
        // interiorPerRow = 7 (cols 1..7). rowsNeeded = ceil(keys/7) + 2 (for border rows). Clamp to [3..6].
        int keyCount = keys.size();
        int neededInnerRows = (int) Math.ceil((double) keyCount / 7.0);
        int rowsNeeded = Math.max(3, Math.min(6, neededInnerRows + 2));
        int size = rowsNeeded * 9;
        // Debug: send a quick chat message to the admin showing first keys
        try {
            String ks = firstN(keys, 12);
            p.sendMessage("§6AdminConfig: found top-level keys: §f" + ks);
        } catch (Throwable ignored) {}
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Admin Config - Kategorien").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        int rows = size / 9;
        // compute interior slots: rows 1..rows-2, cols 1..7
        java.util.List<Integer> interiorSlots = new java.util.ArrayList<>();
        for (int r = 1; r <= rows - 2; r++) {
            for (int c = 1; c <= 7; c++) {
                interiorSlots.add(r * 9 + c);
            }
        }
        plugin.getLogger().info("AdminConfigEditor: inventory size=" + size + " rows=" + rows + " interiorSlots=" + interiorSlots.size());
        // place items into interiorSlots
        int placed = 0;
        if (keys == null || keys.isEmpty()) {
             // show placeholder so admin sees there's no categories and a hint in the UI
             java.util.List<Component> lore = new java.util.ArrayList<>();
             lore.add(Component.text("Keine Kategorien gefunden").color(NamedTextColor.GRAY));
             lore.add(Component.text("Siehe Server-Log (plugin) für Details").color(NamedTextColor.DARK_GRAY));
             lore.add(Component.text("Konfig geladen: " + (cfg == null ? "null" : (cfg.getName()==null?"resource/config":cfg.getName()))).color(NamedTextColor.DARK_GRAY));
             inv.setItem(13, createGuiItem(Material.PAPER, Component.text("Keine Kategorien").color(NamedTextColor.RED), lore, "noop"));
        } else {
            java.util.Iterator<String> it = keys.iterator();
            for (int slotIndex = 0; slotIndex < interiorSlots.size() && it.hasNext(); slotIndex++) {
                String k = it.next();
                java.util.List<Component> lore = new java.util.ArrayList<>();
                Object val = cfg.get(k);
                lore.add(Component.text("Typ: "+(val==null?"null":val.getClass().getSimpleName())).color(NamedTextColor.GRAY));
                lore.add(Component.text("Pfad: "+k).color(NamedTextColor.DARK_GRAY));
                Component name = Component.text(k).color(NamedTextColor.AQUA);
                ItemStack itStack = GuiTheme.createAction(plugin, Material.PAPER, name, lore, "admin_cfg_cat:"+k, false);
                int placeSlot = interiorSlots.get(slotIndex);
                inv.setItem(placeSlot, itStack);
                try { plugin.getLogger().info("AdminConfigEditor: placed category='"+k+"' slot="+placeSlot+" action=admin_cfg_cat:"+k); } catch (Throwable ignored) {}
                placed++;
            }
            if (placed < keys.size()) {
                plugin.getLogger().info("AdminConfigEditor: displayed " + placed + " of " + keys.size() + " keys (inventory too small)");
            } else {
                plugin.getLogger().info("AdminConfigEditor: displayed all " + placed + " keys");
            }
            try { p.sendMessage("§6AdminConfig: showing " + placed + " of " + keys.size() + " categories"); } catch (Throwable ignored) {}
         }
         inv.setItem(inv.getSize()-5, createGuiItem(Material.GLOWSTONE_DUST, Component.text("Reload All").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Reload config & apply")), "config_reload"));
         inv.setItem(inv.getSize()-4, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Admin-Panel")), "admin"));
         p.openInventory(inv);
     }

    public void openAdminConfigKeyEditor(Player p, String fullPath) {
        if (p == null || fullPath == null) return;
        if (!p.hasPermission("minecraftsurvivors.admin")) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Keine Rechte."); return; }
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        Object v = cfg.get(fullPath);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Edit: " + fullPath).color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        java.util.List<Component> info = new java.util.ArrayList<>();
        info.add(Component.text("Aktueller Wert: " + String.valueOf(v)).color(NamedTextColor.WHITE));
        info.add(Component.text("Pfad: " + fullPath).color(NamedTextColor.DARK_GRAY));
        inv.setItem(13, createGuiItem(Material.PAPER, Component.text("Key").color(NamedTextColor.AQUA), info, "noop"));

        if (v instanceof Boolean) {
            boolean cur = (Boolean) v;
            inv.setItem(11, createGuiItem(cur ? Material.LIME_DYE : Material.REDSTONE_BLOCK,
                    Component.text("Toggle").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Aktuell: " + cur)), "cfg_toggle:" + fullPath));
        } else if (v instanceof Number) {
            double val = ((Number) v).doubleValue();
            double mag = Math.max(1.0, Math.abs(val));
            double step1 = (mag >= 10 ? 10 : mag >= 1 ? 1 : 0.1);
            double step2 = step1 * 0.1;
            inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS, Component.text("- " + step1).color(NamedTextColor.RED), java.util.List.of(Component.text("Decrease by " + step1)), "cfg_dec:" + fullPath + ":" + step1));
            inv.setItem(12, createGuiItem(Material.RED_STAINED_GLASS, Component.text("- " + step2).color(NamedTextColor.RED), java.util.List.of(Component.text("Decrease by " + step2)), "cfg_dec:" + fullPath + ":" + step2));
            inv.setItem(14, createGuiItem(Material.LIME_STAINED_GLASS, Component.text("+ " + step2).color(NamedTextColor.GREEN), java.util.List.of(Component.text("Increase by " + step2)), "cfg_inc:" + fullPath + ":" + step2));
            inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS, Component.text("+ " + step1).color(NamedTextColor.GREEN), java.util.List.of(Component.text("Increase by " + step1)), "cfg_inc:" + fullPath + ":" + step1));
        } else if (v instanceof String) {
            inv.setItem(11, createGuiItem(Material.PAPER, Component.text("Set via Chat").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Gebe neuen Wert im Chat ein:"), Component.text("/msconfig set " + fullPath + " <value>").color(NamedTextColor.GRAY)), "cfg_chat:" + fullPath));
        } else {
            inv.setItem(11, createGuiItem(Material.BOOK, Component.text("Nicht editierbar").color(NamedTextColor.GRAY), java.util.List.of(Component.text("Dieser Typ wird von der UI nicht direkt unterstützt.")), "noop"));
        }

        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zur Kategorie")), "admin_config"));
        p.openInventory(inv);
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (ItemStack-Variante).
     * Schließt das Inventar, vergibt ein kleines Beispiel-Reward und sendet eine Nachricht.
     */
    public void handleLevelChoice(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack display, int level) {
        if (player == null || display == null) return;
        player.closeInventory();
        try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);} catch (Throwable ignored) {}
        try { player.spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0,1.2,0), 12, 0.3,0.3,0.3, 0.02);} catch (Throwable ignored) {}
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
        org.bukkit.NamespacedKey rarKey = new org.bukkit.NamespacedKey(plugin, "ms_rarmult");
        org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(plugin, "ms_ability_pick");
        double rarMult = 1.0; String abilityPick = null;
        try {
            org.bukkit.inventory.meta.ItemMeta md = display.getItemMeta();
            if (md != null) {
                String s = md.getPersistentDataContainer().get(rarKey, org.bukkit.persistence.PersistentDataType.STRING);
                if (s != null) rarMult = Double.parseDouble(s);
                abilityPick = md.getPersistentDataContainer().get(abilityKey, org.bukkit.persistence.PersistentDataType.STRING);
            }
        } catch (Throwable ignored) {}

        // reference level for diagnostics (avoids 'unused parameter' warning)
        try { plugin.getLogger().fine("Level choice selected: " + level); } catch (Throwable ignored) {}

        if (abilityPick != null) {
            // Rarity-basierte Upgrade-Stärke: COMMON +1 Level, RARE +2, EPIC +3 (beim ersten Erhalt: Startlevel entsprechend)
            int boost = (rarMult >= 1.5 ? 3 : (rarMult >= 1.2 ? 2 : 1));
            if (!sp.hasAbility(abilityPick)) {
                if (sp.getAbilities().size() < sp.getMaxAbilitySlots()) {
                    int placedIdx = sp.addAbilityAtFirstFreeIndex(abilityPick, boost);
                    if (placedIdx < 0) {
                        // fallback conservative: try non-indexed add
                        boolean added = sp.addAbilityAtFirstFree(abilityPick, boost);
                        placedIdx = added ? sp.getAbilities().size()-1 : -1;
                    }
                    if (placedIdx >= 0) sp.setAbilityOrigin(abilityPick, "levelup");
                    org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Def def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(abilityPick);
                    String disp = def != null ? def.display : abilityPick;
                    player.sendMessage("§bNeue Ability: §f" + disp + " §7(Start Lv+"+boost+")");
                    try { player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f); } catch (Throwable ignored) {}
                    // Visual feedback: update client hotbar and play particles/sound
                    try {
                        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0,1.0,0), 36, 0.4, 0.6, 0.4, 0.02);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.1f);
                        // hotbar glow animation: enchant the item briefly
                        if (placedIdx >= 0) {
                            org.bukkit.inventory.ItemStack it = player.getInventory().getItem(placedIdx);
                            if (it != null && it.hasItemMeta()) {
                                org.bukkit.inventory.meta.ItemMeta m = it.getItemMeta();
                                try { m.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true); m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); it.setItemMeta(m); player.updateInventory();
                                // schedule to remove glow after 40 ticks (~2s)
                                final int idxToClear = placedIdx;
                                org.bukkit.Bukkit.getScheduler().runTaskLater(MinecraftSurvivors.getInstance(), () -> {
                                     try {
                                         org.bukkit.inventory.ItemStack it2 = player.getInventory().getItem(idxToClear);
                                         if (it2!=null && it2.hasItemMeta()) {
                                             org.bukkit.inventory.meta.ItemMeta m2 = it2.getItemMeta();
                                             try { m2.removeEnchant(org.bukkit.enchantments.Enchantment.LURE); m2.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); it2.setItemMeta(m2); player.updateInventory(); } catch (Throwable ignored) {}
                                         }
                                     } catch (Throwable ignored) {}
                                }, 40L);
                                } catch (Throwable ignored) {}
                            }
                        }
                        player.sendActionBar(net.kyori.adventure.text.Component.text("Neue Ability erhalten: "+disp).color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                     } catch (Throwable ignored) {}
                 } else {
                     // Slots voll: Replace-UI öffnen
                     try {
                         new ReplaceAbilityMenu(player, sp, abilityPick, rarMult, false).open();
                         return;
                     } catch (Throwable t) {
                         player.sendMessage("§7Ability-Slots voll (max " + sp.getMaxAbilitySlots() + ")");
                     }
                 }
             } else {
                 sp.incrementAbilityLevel(abilityPick, boost);
                 org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Def def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(abilityPick);
                 String disp = def != null ? def.display : abilityPick;
                 player.sendMessage("§a"+disp+" gelevelt: +"+boost);
                 try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.6f); } catch (Throwable ignored) {}
                 // when leveling existing ability, small feedback
                 try {
                     player.updateInventory();
                     player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
                     player.sendActionBar(net.kyori.adventure.text.Component.text(disp+" gelevelt +"+boost).color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                 } catch (Throwable ignored) {}
             }
            // Resume + evtl. nächste Queue
            try { plugin.getGameManager().resumeForPlayer(player.getUniqueId()); plugin.getGameManager().tryOpenNextQueuedDelayed(player.getUniqueId()); } catch (Throwable ignored) {}
            return;
        }

        // Fallback: alte Logik (Coins etc.)
        org.bukkit.Material mat = display.getType();
        double cfgBonusDamage = plugin.getConfigUtil().getDouble("levelup.values.bonus-damage", 1.5) * rarMult;
        int cfgBonusStrikes = (int) Math.round(plugin.getConfigUtil().getInt("levelup.values.bonus-strikes", 1) * rarMult);
        double cfgFlatDamage = plugin.getConfigUtil().getDouble("levelup.values.flat-damage", 2.0) * rarMult;
        int cfgExtraHearts = plugin.getConfigUtil().getInt("levelup.values.extra-hearts", 2); // hearts not scaled by rarity by default

        double sizeStep = plugin.getConfigUtil().getDouble("levelup.weapon.size-step", 0.15);
        double atkMultStep = plugin.getConfigUtil().getDouble("levelup.weapon.attackpower-step", 0.20);
        int igniteStep = (int) Math.round(plugin.getConfigUtil().getInt("levelup.pyromancer.ignite-step", 20) * rarMult);
        double kbStep = plugin.getConfigUtil().getDouble("levelup.ranger.knockback-step", 0.10) * rarMult;
        double healStep = plugin.getConfigUtil().getDouble("levelup.paladin.heal-step", 0.5) * rarMult;

        double moveStep = plugin.getConfigUtil().getDouble("levelup.values.move-speed", 0.05) * rarMult;
        double asStep = plugin.getConfigUtil().getDouble("levelup.values.attack-speed", 0.07) * rarMult;
        double resistStep = plugin.getConfigUtil().getDouble("levelup.values.resist", 0.05) * rarMult;
        double luckStep = plugin.getConfigUtil().getDouble("levelup.values.luck", 0.05) * rarMult;

        switch (mat) {
            case DIAMOND_SWORD:
                sp.addBonusDamage(cfgBonusDamage);
                player.sendMessage("§a+" + cfgBonusDamage + " Bonus-Schaden");
                break;
            case TRIDENT:
                sp.addBonusStrikes(cfgBonusStrikes);
                player.sendMessage("§a+" + cfgBonusStrikes + " Treffer (Strikes)");
                break;
            case IRON_INGOT:
                sp.addFlatDamage(cfgFlatDamage);
                player.sendMessage("§a+" + cfgFlatDamage + " flacher Schaden");
                break;
            case APPLE:
                sp.addExtraHearts(cfgExtraHearts);
                try {
                    org.bukkit.attribute.AttributeInstance max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (max != null) {
                        double addHearts = cfgExtraHearts / 2.0;
                        double newBase = Math.max(1.0, max.getBaseValue() + (addHearts * 2.0));
                        max.setBaseValue(newBase);
                        player.setHealth(Math.min(newBase, player.getHealth() + (addHearts * 2.0)));
                    }
                } catch (Throwable ignored) {}
                player.sendMessage("§a+" + (cfgExtraHearts / 2.0) + " Herzen");
                break;
            case BLAZE_POWDER:
                sp.addRadiusMult(sizeStep);
                player.sendMessage("§6Radius +" + (int)(sizeStep * 100) + "%");
                break;
            case GOLDEN_SWORD:
                sp.addDamageMult(atkMultStep);
                player.sendMessage("§6Attackpower +" + (int)(atkMultStep * 100) + "%");
                break;
            case MAGMA_CREAM:
                sp.addIgniteBonusTicks(igniteStep);
                player.sendMessage("§6Burn Dauer +" + igniteStep + "t");
                break;
            case BOW:
                 // Ambig: BOW kann auch Waffe: Fernschuss sein – Material-Konflikt vermeiden wir, indem wir den Display-Namen prüfen
                String dn = null; try { org.bukkit.inventory.meta.ItemMeta md = display.getItemMeta(); if (md != null) { var comp = md.displayName(); if (comp != null) dn = PlainTextComponentSerializer.plainText().serialize(comp); } } catch (Throwable ignored) {}
                if (dn != null && dn.toLowerCase(java.util.Locale.ROOT).contains("waffe: fern")) {
                    if (sp.addWeapon("w_ranged")) player.sendMessage("§bNeue Waffe: Fernschuss"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                } else {
                    sp.addKnockbackBonus(kbStep);
                    player.sendMessage("§6Knockback +" + (int)(kbStep * 100) + "%");
                }
                break;
            case GOLDEN_APPLE:
                sp.addHealBonus(healStep);
                player.sendMessage("§6Heilung +" + healStep);
                break;
            case SUGAR:
                sp.addMoveSpeedMult(moveStep);
                try {
                    org.bukkit.attribute.AttributeInstance mv = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                    if (mv != null) mv.setBaseValue(mv.getBaseValue() * (1.0 + moveStep));
                } catch (Throwable ignored) {}
                player.sendMessage("§bGeschwindigkeit +" + (int)(moveStep*100) + "%");
                break;
            case FEATHER:
                sp.addAttackSpeedMult(asStep);
                try {
                    org.bukkit.potion.PotionEffectType haste = org.bukkit.potion.PotionEffectType.HASTE;
                    if (haste == null) haste = org.bukkit.potion.PotionEffectType.SPEED;
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(haste, 20*60, 0, false, false, false));
                } catch (Throwable ignored) {}
                player.sendMessage("§bAngriffstempo +" + (int)(asStep*100) + "%");
                break;
            case SHIELD:
                sp.addDamageResist(resistStep);
                player.sendMessage("§bResistenz +" + (int)(resistStep*100) + "%");
                break;
            case RABBIT_FOOT:
                sp.addLuck(luckStep);
                player.sendMessage("§bGlück +" + (int)(luckStep*100) + "%");
                break;
            case LIGHTNING_ROD:
                if (sp.addWeapon("w_lightning")) player.sendMessage("§bNeue Waffe: Blitz"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            case FIRE_CHARGE:
                if (sp.addWeapon("w_fire")) player.sendMessage("§bNeue Waffe: Feuer"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            case HEART_OF_THE_SEA:
                if (sp.addWeapon("w_holy")) player.sendMessage("§bNeue Waffe: Heilige Nova"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            default:
                sp.addCoins(5);
                player.sendMessage("§a+5 Coins");
                break;
        }
        // nach Reward: Spieler fortsetzen und evtl. nächstes GUI öffnen
        try {
            plugin.getGameManager().resumeForPlayer(player.getUniqueId());
            plugin.getGameManager().tryOpenNextQueuedDelayed(player.getUniqueId());
        } catch (Throwable ignored) {}
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (String-Variante).
     */
    public void handleLevelChoice(Player player, String displayName, int level) {
         if (player == null || displayName == null) return;
         player.closeInventory();
         try {
             plugin.getPlayerManager().get(player.getUniqueId()).addCoins(5);
             player.sendMessage(net.kyori.adventure.text.Component.text("§aAusgewählt: " + displayName + " (Level " + level + ") — §e+5 Münzen"));
         } catch (Exception ex) {
             player.sendMessage(net.kyori.adventure.text.Component.text("§aAusgewählt: " + displayName + " (Level " + level + ")"));
         }
         try {
             plugin.getGameManager().resumeForPlayer(player.getUniqueId());
             plugin.getGameManager().tryOpenNextQueuedDelayed(player.getUniqueId());
         } catch (Throwable ignored) {}
    }

    // Helper wrappers (restore if missing) -------------------------------------------------
    private void fillBorder(Inventory inv, Material borderMat) {
        ItemStack border = org.bysenom.minecraftSurvivors.gui.GuiTheme.borderItem(borderMat);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9; int col = i % 9;
            if (row == 0 || row == (inv.getSize()/9 -1) || col == 0 || col == 8) inv.setItem(i, border);
        }
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action) {
        return createGuiItem(mat, name, lore, action, false);
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action, boolean glow) {
        return GuiTheme.createAction(plugin, mat, name, lore, action, glow);
    }

    public void openShop(Player p) {
        if (p == null) return;
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("MinecraftSurvivors - Shop").color(NamedTextColor.LIGHT_PURPLE));
        // decorative border
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        // Header card
        java.util.List<Component> hdr = new java.util.ArrayList<>();
        hdr.add(Component.text("Willkommen im Shop").color(NamedTextColor.GOLD));
        hdr.add(Component.text("",
                NamedTextColor.DARK_GRAY));
        hdr.add(Component.text("Abilities & Glyphen sind derzeit standardmäßig verfügbar.").color(NamedTextColor.GRAY));
        hdr.add(Component.text("Rüstungen / Gear sind vorübergehend entfernt.").color(NamedTextColor.GRAY));
        hdr.add(Component.text("",
                NamedTextColor.DARK_GRAY));
        hdr.add(Component.text("Essence: ").color(NamedTextColor.LIGHT_PURPLE).append(Component.text(String.valueOf(plugin.getMetaManager().get(p.getUniqueId()).getEssence())).color(NamedTextColor.WHITE)));
        inv.setItem(13, createGuiItem(Material.PLAYER_HEAD, Component.text("Shop Übersicht").color(NamedTextColor.AQUA), hdr, "noop", false));

        // Quick category buttons
        inv.setItem(20, createGuiItem(Material.BOOK, Component.text("Cosmetics").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Kosmetische Items & Skins (coming soon)").color(NamedTextColor.GRAY)), "shop_cat:cosmetics"));
        inv.setItem(22, createGuiItem(Material.CHEST, Component.text("Meta Shop").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Permanente Upgrades & Essence Shop").color(NamedTextColor.GRAY)), "meta"));
        inv.setItem(24, createGuiItem(Material.ENCHANTED_BOOK, Component.text("Abilities (Info)").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Abilities sind standardmäßig verfügbar — kein Kauf nötig").color(NamedTextColor.GRAY)), "shop_cat:abilities_info"));
        inv.setItem(29, createGuiItem(Material.AMETHYST_SHARD, Component.text("Glyphen (Info)").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Glyphen spawnen wie Lootchests — nicht kaufbar im Shop").color(NamedTextColor.GRAY)), "shop_cat:glyphs_info"));

        // Placeholder cosmetics area (reads config if present)
        java.util.List<java.util.Map<?, ?>> cosmetics = cfg.getMapList("shop.categories.cosmetics");
        int slot = 30;
        if (cosmetics != null && !cosmetics.isEmpty()) {
            for (java.util.Map<?, ?> node : cosmetics) {
                Object nameObj = node.get("name");
                String name = nameObj != null ? String.valueOf(nameObj) : "Cosmetic";
                Object keyObj = node.get("key");
                String key = keyObj != null ? String.valueOf(keyObj) : ("cosmetic_" + slot);
                Object priceObj = node.get("price");
                int price = priceObj != null ? Integer.parseInt(String.valueOf(priceObj)) : 10;
                Material mat = Material.ARMOR_STAND;
                try {
                    Object iconObj = node.get("icon");
                    String matS = iconObj != null ? String.valueOf(iconObj) : "AMETHYST_SHARD";
                    mat = Material.valueOf(matS.toUpperCase(java.util.Locale.ROOT));
                } catch (Throwable ignored) {}
                java.util.List<Component> cl = new java.util.ArrayList<>();
                cl.add(Component.text(name).color(NamedTextColor.WHITE));
                cl.add(Component.text("Preis: " + price + " Coins").color(NamedTextColor.GOLD));
                inv.setItem(slot, createGuiItem(mat, Component.text(name).color(NamedTextColor.AQUA), cl, "shop_buy:cosmetic:" + key, false));
                slot++; if (slot % 9 == 8) slot += 2; if (slot >= 53) break;
            }
        } else {
            inv.setItem(31, createGuiItem(Material.AMETHYST_SHARD, Component.text("Keine Cosmetics").color(NamedTextColor.GRAY), java.util.List.of(Component.text("Derzeit gibt es keine kosmetischen Items im Shop.").color(NamedTextColor.DARK_GRAY)), "noop"));
        }

        // Footer
        inv.setItem(49, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Schließt das Menü")), "back"));
        p.openInventory(inv);
    }

    // small helper to return first N elements of a set as string
    private static String firstN(java.util.Set<String> s, int n) {
        if (s == null || s.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String v : s) {
            if (i++ >= n) break;
            if (sb.length() > 1) sb.append(", ");
            sb.append(v);
        }
        if (s.size() > n) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }

    private String lastPathSegment(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx+1) : path;
    }
}
