package org.bysenom.minecraftSurvivors.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Central GUI styling helpers — consistent titles, item creation and rarities.
 */
public final class GuiTheme {
    private GuiTheme() {}

    public static Component styledTitle(String main, String sub) {
        return Component.text(main).color(NamedTextColor.AQUA).append(Component.text(" • ")).append(Component.text(sub).color(NamedTextColor.WHITE));
    }

    public static ItemStack borderItem(Material mat) {
        ItemStack it = new ItemStack(mat);
        try {
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                m.displayName(Component.text(""));
                it.setItemMeta(m);
            }
        } catch (Throwable ignored) {}
        return it;
    }

    public static ItemStack createAction(MinecraftSurvivors plugin, Material mat, Component name, List<Component> lore, String actionKey, boolean glow) {
        ItemStack item = new ItemStack(mat);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(name);
                meta.lore(safeLore(lore));
                if (actionKey != null) meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "ms_gui"), PersistentDataType.STRING, actionKey);
                if (glow) {
                    meta.addEnchant(Enchantment.LURE, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                // compact visual polish
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}
        return item;
    }

    public static List<Component> safeLore(List<Component> lore) {
        if (lore == null) return new ArrayList<>();
        return lore;
    }

    public static Component rarityLabel(double mult) {
        if (mult >= 1.5) return Component.text("EPIC").color(NamedTextColor.LIGHT_PURPLE);
        if (mult >= 1.2) return Component.text("RARE").color(NamedTextColor.AQUA);
        return Component.text("COMMON").color(NamedTextColor.WHITE);
    }
}
