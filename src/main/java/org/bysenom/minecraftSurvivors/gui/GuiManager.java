package org.bysenom.minecraftSurvivors.gui;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.SkullType;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class GuiManager {

    private final MinecraftSurvivors plugin;
    private final GameManager gameManager;
    private final NamespacedKey key;

    // Sichtbarer Titel des Hauptmenüs (gestylt)
    public static final Component MAIN_TITLE = Component.text("MinecraftSurvivors").color(NamedTextColor.AQUA).append(Component.text(" • Menu").color(NamedTextColor.WHITE));

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
        // enlarge to 45 slots (5 rows)
        Inventory inv = Bukkit.createInventory(null, 45, MAIN_TITLE);
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        PlayerClass sel = sp != null ? sp.getSelectedClass() : null;
        Material classMat = materialForClass(sel != null ? sel : PlayerClass.SHAMAN);
        Component classTitle = Component.text(sel != null ? "Klasse: " + sel.getDisplayName() : "Klasse wählen").color(NamedTextColor.AQUA);
        java.util.List<Component> classLore = java.util.List.of(Component.text(sel != null ? sel.getDescription() : "Wähle deine Klasse bevor du startest").color(NamedTextColor.GRAY));

        int coins = sp != null ? sp.getCoins() : 0;
        int lvl = sp != null ? sp.getClassLevel() : 1;

        // Row 2 (slots 9..17): Status, Start, Klasse, Powerups
        inv.setItem(10, createGuiItem(Material.EMERALD, Component.text("Status").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("✦ Coins: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(coins)).color(NamedTextColor.WHITE)),
                        Component.text("⚑ Level: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(lvl)).color(NamedTextColor.WHITE))), "status", false));
        inv.setItem(12, createGuiItem(Material.GREEN_STAINED_GLASS, Component.text("Start Spiel").color(NamedTextColor.GREEN),
                java.util.List.of(Component.text("Klicke um das Spiel zu starten").color(NamedTextColor.GRAY)), "start", false));
        inv.setItem(14, createGuiItem(classMat, classTitle, classLore, "class", sel != null));
        inv.setItem(16, createGuiItem(Material.NETHER_STAR, Component.text("Powerups").color(NamedTextColor.LIGHT_PURPLE),
                java.util.List.of(Component.text("Später verfügbare Powerups & Items").color(NamedTextColor.GRAY)), "powerup", false));

        // Row 3 (slots 18..26): Party, Stats, ggf. Config
        inv.setItem(20, createGuiItem(Material.PLAYER_HEAD, Component.text("Party").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("Erstelle/Verwalte deine Party").color(NamedTextColor.GRAY)), "party", false));
        inv.setItem(22, createGuiItem(Material.CLOCK, Component.text("Stats").color(NamedTextColor.YELLOW),
                java.util.List.of(Component.text("DPS/HPS Anzeige-Modus umschalten").color(NamedTextColor.GRAY)), "stats", false));
        if (p.hasPermission("minecraftsurvivors.admin")) {
            inv.setItem(24, createGuiItem(Material.COMPARATOR, Component.text("Config").color(NamedTextColor.AQUA),
                    java.util.List.of(Component.text("Reload & Presets").color(NamedTextColor.GRAY)), "config", false));
        }

        // Info Button unten links
        inv.setItem(36, createGuiItem(Material.PAPER, Component.text("Info").color(NamedTextColor.YELLOW),
                java.util.List.of(Component.text("V: Vampire Survivors like Mini-Game"), Component.text("bySenom").color(NamedTextColor.GRAY)), "info"));

        // Footer (Status-Zeile) unten rechts: Stats-Modus + Party-Zusammenfassung
        String mode = "-";
        try { mode = plugin.getStatsDisplayManager().getMode().name().toLowerCase(); } catch (Throwable ignored) {}
        java.util.List<Component> footerLore = new java.util.ArrayList<>();
        footerLore.add(Component.text("Stats: ").color(NamedTextColor.GRAY).append(Component.text(mode).color(NamedTextColor.AQUA)));
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) {
            int online = pm.onlineMembers(party).size();
            int total = party.getMembers().size();
            footerLore.add(Component.text("Party: ").color(NamedTextColor.GRAY)
                    .append(Component.text(online + "/" + total).color(NamedTextColor.GREEN)));
        } else {
            footerLore.add(Component.text("Party: keine").color(NamedTextColor.DARK_GRAY));
        }
        inv.setItem(44, createGuiItem(Material.MAP, Component.text("Status").color(NamedTextColor.WHITE), footerLore, "noop", false));

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
                List.of(Component.text("Main: Blitz"), Component.text("Solider Allrounder").color(NamedTextColor.GRAY)), "select_shaman", current == org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN));
        inv.setItem(12, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER), Component.text("Pyromant 🔥").color(NamedTextColor.GOLD),
                List.of(Component.text("Main: Feuer & DoT"), Component.text("Nahbereich-AoE").color(NamedTextColor.GRAY)), "select_pyromancer", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER));
        inv.setItem(14, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER), Component.text("Waldläufer 🏹").color(NamedTextColor.GREEN),
                List.of(Component.text("Main: Fernschuss"), Component.text("Single-Target").color(NamedTextColor.GRAY)), "select_ranger", current == org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER));
        inv.setItem(16, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN), Component.text("Paladin ✨").color(NamedTextColor.AQUA),
                List.of(Component.text("Main: Heilige Nova"), Component.text("AoE + Heal").color(NamedTextColor.GRAY)), "select_paladin", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN));

        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    private void fillBorder(Inventory inv, Material borderMat) {
        ItemStack border = new ItemStack(borderMat);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == (inv.getSize()/9 -1) || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action) {
        return createGuiItem(mat, name, lore, action, false);
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action, boolean glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            if (glow) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (ItemStack-Variante).
     * Schließt das Inventar, vergibt ein kleines Beispiel-Reward und sendet eine Nachricht.
     */
    public void handleLevelChoice(Player player, ItemStack display, int level) {
        if (player == null || display == null) return;
        player.closeInventory();
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
        org.bukkit.Material mat = display.getType();

        double cfgBonusDamage = plugin.getConfigUtil().getDouble("levelup.values.bonus-damage", 1.5);
        int cfgBonusStrikes = plugin.getConfigUtil().getInt("levelup.values.bonus-strikes", 1);
        double cfgFlatDamage = plugin.getConfigUtil().getDouble("levelup.values.flat-damage", 2.0);
        int cfgExtraHearts = plugin.getConfigUtil().getInt("levelup.values.extra-hearts", 2);

        double sizeStep = plugin.getConfigUtil().getDouble("levelup.weapon.size-step", 0.15);
        double atkMultStep = plugin.getConfigUtil().getDouble("levelup.weapon.attackpower-step", 0.20);
        int igniteStep = plugin.getConfigUtil().getInt("levelup.pyromancer.ignite-step", 20);
        double kbStep = plugin.getConfigUtil().getDouble("levelup.ranger.knockback-step", 0.10);
        double healStep = plugin.getConfigUtil().getDouble("levelup.paladin.heal-step", 0.5);

        switch (mat) {
            case DIAMOND_SWORD:
                sp.addBonusDamage(cfgBonusDamage);
                player.sendMessage(Component.text("§a+" + cfgBonusDamage + " Bonus-Schaden"));
                break;
            case TRIDENT:
                sp.addBonusStrikes(cfgBonusStrikes);
                player.sendMessage(Component.text("§a+" + cfgBonusStrikes + " Treffer (Strikes)"));
                break;
            case IRON_INGOT:
                sp.addFlatDamage(cfgFlatDamage);
                player.sendMessage(Component.text("§a+" + cfgFlatDamage + " flacher Schaden"));
                break;
            case APPLE:
                sp.addExtraHearts(cfgExtraHearts);
                player.sendMessage(Component.text("§a+" + (cfgExtraHearts / 2.0) + " Herzen (Modell)"));
                break;
            case BLAZE_POWDER:
                sp.addRadiusMult(sizeStep);
                player.sendMessage(Component.text("§6Radius +" + (int)(sizeStep * 100) + "%"));
                break;
            case GOLDEN_SWORD:
                sp.addDamageMult(atkMultStep);
                player.sendMessage(Component.text("§6Attackpower +" + (int)(atkMultStep * 100) + "%"));
                break;
            case MAGMA_CREAM:
                sp.addIgniteBonusTicks(igniteStep);
                player.sendMessage(Component.text("§6Burn Dauer +" + igniteStep + "t"));
                break;
            case BOW:
                sp.addKnockbackBonus(kbStep);
                player.sendMessage(Component.text("§6Knockback +" + (int)(kbStep * 100) + "%"));
                break;
            case GOLDEN_APPLE:
                sp.addHealBonus(healStep);
                player.sendMessage(Component.text("§6Heilung +" + healStep));
                break;
            default:
                sp.addCoins(5);
                player.sendMessage(Component.text("§a+5 Coins"));
                break;
        }
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (String-Variante).
     */
    public void handleLevelChoice(Player player, String displayName, int level) {
        if (player == null || displayName == null) return;
        player.closeInventory();
        try {
            plugin.getPlayerManager().get(player.getUniqueId()).addCoins(5);
            player.sendMessage(Component.text("§aAusgewählt: " + displayName + " (Level " + level + ") — §e+5 Münzen"));
        } catch (Exception ex) {
            player.sendMessage(Component.text("§aAusgewählt: " + displayName + " (Level " + level + ")"));
        }
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
                lore.add(Component.text("- "+name).color(NamedTextColor.WHITE));
            }
        } else {
            lore.add(Component.text("Keine Party").color(NamedTextColor.GRAY));
        }
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
        switch (pc) {
            case SHAMAN: return Material.ENCHANTED_BOOK;
            case PYROMANCER: return Material.BLAZE_POWDER;
            case RANGER: return Material.BOW;
            case PALADIN: return Material.SHIELD;
            default: return Material.POPPED_CHORUS_FRUIT;
        }
    }
}
