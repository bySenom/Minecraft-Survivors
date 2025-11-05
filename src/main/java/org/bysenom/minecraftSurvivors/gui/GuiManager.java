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
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);

        // Border + background
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        // Hauptbuttons (zentriert auf der mittleren Reihe)
        inv.setItem(11, createGuiItem(Material.GREEN_STAINED_GLASS, Component.text("Start Spiel").color(NamedTextColor.GREEN),
                List.of(Component.text("Klicke um das Spiel zu starten").color(NamedTextColor.GRAY)), "start", false));

        // Klasse-Button: wenn Spieler bereits eine Klasse gewählt hat, zeige deren Icon + Glow
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        PlayerClass sel = sp != null ? sp.getSelectedClass() : null;
        Material classMat = materialForClass(sel != null ? sel : PlayerClass.SHAMAN);
        Component classTitle = Component.text(sel != null ? "Klasse: " + sel.getDisplayName() : "Klasse wählen").color(NamedTextColor.AQUA);
        List<Component> classLore = List.of(Component.text(sel != null ? sel.getDescription() : "Wähle deine Klasse bevor du startest").color(NamedTextColor.GRAY));
        inv.setItem(13, createGuiItem(classMat, classTitle, classLore, "class", sel != null));

        inv.setItem(15, createGuiItem(Material.NETHER_STAR, Component.text("Powerups").color(NamedTextColor.LIGHT_PURPLE),
                List.of(Component.text("Später verfügbare Powerups & Items").color(NamedTextColor.GRAY)), "powerup", false));

        // Status-Item (Coins / Level)
        int coins = sp != null ? sp.getCoins() : 0;
        int lvl = sp != null ? sp.getClassLevel() : 1;
        inv.setItem(10, createGuiItem(Material.EMERALD, Component.text("Status").color(NamedTextColor.GOLD),
                List.of(Component.text("✦ Coins: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(coins)).color(NamedTextColor.WHITE)),
                        Component.text("⚑ Level: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(lvl)).color(NamedTextColor.WHITE))), "status", false));

        // Info / Close
        inv.setItem(22, createGuiItem(Material.PAPER, Component.text("Info").color(NamedTextColor.YELLOW),
                List.of(Component.text("V: Vampire Survivors like Mini-Game"), Component.text("bySenom").color(NamedTextColor.GRAY)), "info"));

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
