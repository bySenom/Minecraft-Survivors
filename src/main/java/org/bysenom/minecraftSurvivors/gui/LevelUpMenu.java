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

    public LevelUpMenu(int level) {
        this.level = level;
        this.inv = Bukkit.createInventory(null, 9, Component.text("Level Up - Wähle eine Belohnung (Level " + level + ")"));
        setupItems();
    }

    private void setupItems() {
        // mögliche Optionen
        List<ItemStack> options = new ArrayList<>();

        options.add(create(Material.DIAMOND_SWORD, Component.text("§b+Schaden"), List.of(Component.text("Erhöhe deinen Schaden (additiv)."))));
        options.add(create(Material.TRIDENT, Component.text("§3+Treffer"), List.of(Component.text("Erhöhe die Anzahl der Blitze/Ticks (Strikes)."))));
        options.add(create(Material.IRON_INGOT, Component.text("§e+FlatDmg"), List.of(Component.text("Flacher zusätzlicher Schaden pro Treffer."))));
        options.add(create(Material.APPLE, Component.text("§c+Herzen"), List.of(Component.text("Füge zusätzliche Herzen hinzu."))));

        // Anzahl der Choices = min(3, 1 + level/2)
        int choices = Math.min(3, 1 + (level / 2));
        // Zufaellige Auswahl aus Optionen
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

    public Inventory getInventory() {
        return inv;
    }

    public int getLevel() {
        return level;
    }
}
