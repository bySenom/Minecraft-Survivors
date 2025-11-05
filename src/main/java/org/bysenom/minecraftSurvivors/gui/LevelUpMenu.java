// java
package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelUpMenu {

    private final Inventory inv;
    private final int level;
    private final Random random = new Random();

    private final org.bukkit.entity.Player player;
    private final org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp;
    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance();
    private final org.bukkit.NamespacedKey rarKey = new org.bukkit.NamespacedKey(plugin, "ms_rarmult");

    public LevelUpMenu(org.bukkit.entity.Player player, org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp, int level) {
        this.player = player;
        this.sp = sp;
        this.level = level;
        this.inv = Bukkit.createInventory(null, 9, Component.text("Level Up - Wähle eine Belohnung (Level " + level + ")"));
        setupItems();
    }

    private enum Rarity { COMMON, RARE, EPIC }
    private static Rarity rollRarity(java.util.Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 10) return Rarity.EPIC; // 10%
        if (r < 35) return Rarity.RARE; // 25%
        return Rarity.COMMON;           // 65%
    }

    private net.kyori.adventure.text.Component colorize(Rarity rar, String text) {
        switch (rar) {
            case EPIC: return net.kyori.adventure.text.Component.text(text).color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE);
            case RARE: return net.kyori.adventure.text.Component.text(text).color(net.kyori.adventure.text.format.NamedTextColor.AQUA);
            default: return net.kyori.adventure.text.Component.text(text).color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
        }
    }

    private double multByRarity(Rarity rar) {
        switch (rar) {
            case EPIC: return 1.5;
            case RARE: return 1.2;
            default: return 1.0;
        }
    }

    private void setupItems() {
        java.util.List<org.bukkit.inventory.ItemStack> options = new java.util.ArrayList<>();

        double dmgAdd = plugin.getConfigUtil().getDouble("levelup.values.bonus-damage", 1.5);
        int strikesAdd = plugin.getConfigUtil().getInt("levelup.values.bonus-strikes", 1);
        double flatAdd = plugin.getConfigUtil().getDouble("levelup.values.flat-damage", 2.0);
        int heartsAdd = plugin.getConfigUtil().getInt("levelup.values.extra-hearts", 2);

        double sizeStep = plugin.getConfigUtil().getDouble("levelup.weapon.size-step", 0.15);
        double atkMultStep = plugin.getConfigUtil().getDouble("levelup.weapon.attackpower-step", 0.20);
        int igniteStep = plugin.getConfigUtil().getInt("levelup.pyromancer.ignite-step", 20);
        double kbStep = plugin.getConfigUtil().getDouble("levelup.ranger.knockback-step", 0.10);
        double healStep = plugin.getConfigUtil().getDouble("levelup.paladin.heal-step", 0.5);

        // New stats base steps
        double moveStep = plugin.getConfigUtil().getDouble("levelup.values.move-speed", 0.05);
        double asStep = plugin.getConfigUtil().getDouble("levelup.values.attack-speed", 0.07);
        double resistStep = plugin.getConfigUtil().getDouble("levelup.values.resist", 0.05);
        double luckStep = plugin.getConfigUtil().getDouble("levelup.values.luck", 0.05);

        // Pick rarities and apply multipliers to values in display
        Rarity rar1 = rollRarity(random); double mult1 = multByRarity(rar1);
        Rarity rar2 = rollRarity(random); double mult2 = multByRarity(rar2);
        Rarity rar3 = rollRarity(random); double mult3 = multByRarity(rar3);
        Rarity rar4 = rollRarity(random); double mult4 = multByRarity(rar4);

        // Use createRarity for items with rarity
        options.add(createRarity(org.bukkit.Material.DIAMOND_SWORD, colorize(rar1, "+Schaden"), java.util.List.of(net.kyori.adventure.text.Component.text("Additiver Schaden: +" + (dmgAdd*mult1)), net.kyori.adventure.text.Component.text("Rarity: "+rar1)), mult1));
        options.add(createRarity(org.bukkit.Material.TRIDENT, colorize(rar2, "+Treffer"), java.util.List.of(net.kyori.adventure.text.Component.text("Mehr Ziele +" + strikesAdd), net.kyori.adventure.text.Component.text("Rarity: "+rar2)), mult2));
        options.add(createRarity(org.bukkit.Material.IRON_INGOT, colorize(rar3, "+FlatDmg"), java.util.List.of(net.kyori.adventure.text.Component.text("Flacher Schaden +" + (flatAdd*mult3)), net.kyori.adventure.text.Component.text("Rarity: "+rar3)), mult3));
        options.add(createRarity(org.bukkit.Material.APPLE, colorize(rar4, "+Herzen"), java.util.List.of(net.kyori.adventure.text.Component.text("Halbe Herzen +" + heartsAdd), net.kyori.adventure.text.Component.text("Rarity: "+rar4)), mult4));

        options.add(create(org.bukkit.Material.BLAZE_POWDER, net.kyori.adventure.text.Component.text("§6+Size"), java.util.List.of(net.kyori.adventure.text.Component.text("Radius +" + (int)(sizeStep*100) + "%"))));
        options.add(create(org.bukkit.Material.GOLDEN_SWORD, net.kyori.adventure.text.Component.text("§6+Attackpower"), java.util.List.of(net.kyori.adventure.text.Component.text("Schaden-Multiplikator +" + (int)(atkMultStep*100) + "%"))));

        // New: movement speed, attack speed, resist, luck (with rarity)
        Rarity rar5 = rollRarity(random); double mult5 = multByRarity(rar5);
        Rarity rar6 = rollRarity(random); double mult6 = multByRarity(rar6);
        Rarity rar7 = rollRarity(random); double mult7 = multByRarity(rar7);
        Rarity rar8 = rollRarity(random); double mult8 = multByRarity(rar8);
        options.add(createRarity(org.bukkit.Material.SUGAR, colorize(rar5, "+Bewegung"), java.util.List.of(net.kyori.adventure.text.Component.text("Geschwindigkeit +" + (int)((moveStep*mult5)*100) + "%"), net.kyori.adventure.text.Component.text("Rarity: "+rar5)), mult5));
        options.add(createRarity(org.bukkit.Material.FEATHER, colorize(rar6, "+Angriffstempo"), java.util.List.of(net.kyori.adventure.text.Component.text("Attack Speed +" + (int)((asStep*mult6)*100) + "%"), net.kyori.adventure.text.Component.text("Rarity: "+rar6)), mult6));
        options.add(createRarity(org.bukkit.Material.SHIELD, colorize(rar7, "+Resistenz"), java.util.List.of(net.kyori.adventure.text.Component.text("Schadensreduktion +" + (int)((resistStep*mult7)*100) + "%"), net.kyori.adventure.text.Component.text("Rarity: "+rar7)), mult7));
        options.add(createRarity(org.bukkit.Material.RABBIT_FOOT, colorize(rar8, "+Glück"), java.util.List.of(net.kyori.adventure.text.Component.text("Luck +" + (int)((luckStep*mult8)*100) + "%"), net.kyori.adventure.text.Component.text("Rarity: "+rar8)), mult8));

        // Klassen-spezifische Option ergänzen
        org.bysenom.minecraftSurvivors.model.PlayerClass pc = sp != null ? sp.getSelectedClass() : null;
        if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER) {
            options.add(create(org.bukkit.Material.MAGMA_CREAM, net.kyori.adventure.text.Component.text("§6+Burn Dauer"), java.util.List.of(net.kyori.adventure.text.Component.text("Zusätzliche Brennzeit: +" + igniteStep + "t"))));
        } else if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER) {
            options.add(create(org.bukkit.Material.BOW, net.kyori.adventure.text.Component.text("§6+Knockback"), java.util.List.of(net.kyori.adventure.text.Component.text("Knockback: +" + (int)(kbStep*100) + "%"))));
        } else if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN) {
            options.add(create(org.bukkit.Material.GOLDEN_APPLE, net.kyori.adventure.text.Component.text("§6+Heilung"), java.util.List.of(net.kyori.adventure.text.Component.text("Zusätzliche Heilung: +" + healStep))));
        }

        int choices = Math.min(3, 1 + (level / 2));
        java.util.List<org.bukkit.inventory.ItemStack> chosen = new java.util.ArrayList<>();
        java.util.List<Integer> idxs = java.util.List.of(2, 4, 6);
        while (chosen.size() < choices && !options.isEmpty()) {
            int pick = random.nextInt(options.size());
            chosen.add(options.remove(pick));
        }

        for (int i = 0; i < chosen.size(); i++) {
            inv.setItem(idxs.get(i), chosen.get(i));
        }
    }

    private ItemStack create(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRarity(Material mat, Component name, List<Component> lore, double mult) {
        ItemStack item = create(mat, name, lore);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(rarKey, org.bukkit.persistence.PersistentDataType.STRING, String.valueOf(mult));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}
        return item;
    }

    public Inventory getInventory() { return inv; }
    public int getLevel() { return level; }
}
