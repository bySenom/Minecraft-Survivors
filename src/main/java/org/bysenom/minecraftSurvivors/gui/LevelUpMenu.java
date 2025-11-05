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

    public LevelUpMenu(org.bukkit.entity.Player player, org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp, int level) {
        this.player = player;
        this.sp = sp;
        this.level = level;
        this.inv = Bukkit.createInventory(null, 9, Component.text("Level Up - Wähle eine Belohnung (Level " + level + ")"));
        setupItems();
    }

    private void setupItems() {
        List<ItemStack> options = new ArrayList<>();

        double dmgAdd = plugin.getConfigUtil().getDouble("levelup.values.bonus-damage", 1.5);
        int strikesAdd = plugin.getConfigUtil().getInt("levelup.values.bonus-strikes", 1);
        double flatAdd = plugin.getConfigUtil().getDouble("levelup.values.flat-damage", 2.0);
        int heartsAdd = plugin.getConfigUtil().getInt("levelup.values.extra-hearts", 2);

        double sizeStep = plugin.getConfigUtil().getDouble("levelup.weapon.size-step", 0.15);
        double atkMultStep = plugin.getConfigUtil().getDouble("levelup.weapon.attackpower-step", 0.20);
        int igniteStep = plugin.getConfigUtil().getInt("levelup.pyromancer.ignite-step", 20);
        double kbStep = plugin.getConfigUtil().getDouble("levelup.ranger.knockback-step", 0.10);
        double healStep = plugin.getConfigUtil().getDouble("levelup.paladin.heal-step", 0.5);

        options.add(create(Material.DIAMOND_SWORD, Component.text("§b+Schaden"), List.of(Component.text("Erhöhe deinen Schaden (additiv): +" + dmgAdd))));
        options.add(create(Material.TRIDENT, Component.text("§3+Treffer"), List.of(Component.text("Mehr Ziele pro Tick: +" + strikesAdd))));
        options.add(create(Material.IRON_INGOT, Component.text("§e+FlatDmg"), List.of(Component.text("Flacher Schaden: +" + flatAdd))));
        options.add(create(Material.APPLE, Component.text("§c+Herzen"), List.of(Component.text("Halbe Herzen: +" + heartsAdd))));

        options.add(create(Material.BLAZE_POWDER, Component.text("§6+Size"), List.of(Component.text("Radius-Multiplikator: +" + (int)(sizeStep*100) + "%"))));
        options.add(create(Material.GOLDEN_SWORD, Component.text("§6+Attackpower"), List.of(Component.text("Schaden-Multiplikator: +" + (int)(atkMultStep*100) + "%"))));

        // Klassen-spezifische Option ergänzen
        org.bysenom.minecraftSurvivors.model.PlayerClass pc = sp != null ? sp.getSelectedClass() : null;
        if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER) {
            options.add(create(Material.MAGMA_CREAM, Component.text("§6+Burn Dauer"), List.of(Component.text("Zusätzliche Brennzeit: +" + igniteStep + "t"))));
        } else if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER) {
            options.add(create(Material.BOW, Component.text("§6+Knockback"), List.of(Component.text("Knockback-Multiplikator: +" + (int)(kbStep*100) + "%"))));
        } else if (pc == org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN) {
            options.add(create(Material.GOLDEN_APPLE, Component.text("§6+Heilung"), List.of(Component.text("Zusätzliche Heilung: +" + healStep))));
        }

        // Anzahl der Choices = min(3, 1 + level/2)
        int choices = Math.min(3, 1 + (level / 2));
        List<ItemStack> chosen = new ArrayList<>();
        List<Integer> idxs = List.of(2, 4, 6);
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

    public Inventory getInventory() { return inv; }
    public int getLevel() { return level; }
}
