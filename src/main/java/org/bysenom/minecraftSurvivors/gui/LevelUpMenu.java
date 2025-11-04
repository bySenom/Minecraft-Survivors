// java
package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LevelUpMenu {

    private final Inventory inv;
    private final int level;

    public LevelUpMenu(int level) {
        this.level = level;
        this.inv = Bukkit.createInventory(null, 9, Component.text("Level Up - Wähle eine Belohnung (Level " + level + ")"));
        setupItems();
    }

    private void setupItems() {
        inv.setItem(2, createItem(Material.DIAMOND, Component.text("§bPowerup: Stärke"), Arrays.asList(Component.text("Verstärkt Schaden."))));
        inv.setItem(4, createItem(Material.IRON_SWORD, Component.text("§eWaffe: Schwert"), Arrays.asList(Component.text("Stärke deine Waffe."))));
        inv.setItem(6, createItem(Material.GOLD_INGOT, Component.text("§6Münzen"), Arrays.asList(Component.text("Erhalte zusätzliche Münzen."))));
    }

    private ItemStack createItem(Material mat, Component name, List<Component> lore) {
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
