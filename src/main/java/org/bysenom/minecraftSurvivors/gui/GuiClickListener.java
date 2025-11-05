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
                guiManager.getGameManager().startGameWithCountdown(5);
                player.closeInventory();
                player.sendMessage("§aStart in 5 Sekunden...");
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
            case "select_pyromancer":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Pyromant");
                break;
            case "select_ranger":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Waldläufer");
                break;
            case "select_paladin":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Paladin");
                break;
            default:
                break;
        }
    }
}
