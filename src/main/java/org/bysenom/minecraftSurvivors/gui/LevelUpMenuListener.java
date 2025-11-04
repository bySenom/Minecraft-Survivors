package org.bysenom.minecraftSurvivors.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LevelUpMenuListener implements Listener {

    private final GuiManager guiManager;

    public LevelUpMenuListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        // Prismatische Titelprüfung: nutzt Component->string
        String title = e.getView().title().toString();
        if (!title.contains("Level Up")) return;

        e.setCancelled(true);
        ItemStack display = e.getCurrentItem();
        if (display == null) return;

        Player player = (Player) e.getWhoClicked();
        int level = 1; // Level aus Kontext ziehen oder berechnen

        if (display.getItemMeta() != null && display.getItemMeta().hasDisplayName()) {
            guiManager.handleLevelChoice(player, display, level); // ItemStack-Overload
        } else {
            guiManager.handleLevelChoice(player, display.getType().name(), level); // String-Overload
        }
    }
}
