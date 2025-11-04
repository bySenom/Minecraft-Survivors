// File: src/main/java/org/bysenom/minecraftSurvivors/gui/GuiClickListener.java
package org.bysenom.minecraftSurvivors.gui;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

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

        // Akzeptiere GUI-Aktionen auch in speziellen Inventar-Titeln (Klassenwahl etc.)
        String action = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (action == null) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();

        switch (action) {
            case "start":
                guiManager.getGameManager().startGame();
                player.closeInventory();
                player.sendMessage("§aSpiel gestartet.");
                break;
            case "class":
                // Öffne Klassenwahl
                guiManager.openClassSelection(player);
                break;
            case "back":
                guiManager.openMainMenu(player);
                break;
            case "info":
                guiManager.openInfoMenu(player);
                break;
            case "info_close":
                player.closeInventory();
                break;
            case "status":
                // einfach Status-Item: öffne Info
                guiManager.openInfoMenu(player);
                break;
            case "select_shaman":
                // Spielerklasse setzen
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(PlayerClass.SHAMAN);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: " + PlayerClass.SHAMAN.getDisplayName());
                break;
            case "powerup":
                player.closeInventory();
                player.sendMessage("§dPowerup-Auswahl folgt später.");
                break;
            default:
                break;
        }
    }
}
