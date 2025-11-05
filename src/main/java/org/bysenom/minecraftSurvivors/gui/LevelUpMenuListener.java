package org.bysenom.minecraftSurvivors.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class LevelUpMenuListener implements Listener {

    private final GuiManager guiManager;

    public LevelUpMenuListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        String title = e.getView().title().toString();
        if (!title.contains("Level Up")) return;

        e.setCancelled(true);
        ItemStack display = e.getCurrentItem();
        if (display == null) return;

        // Versuche die Level-Nummer aus dem Titel zu parsen: "... (Level X)"
        int level = 1;
        try {
            int idx = title.lastIndexOf("Level ");
            if (idx >= 0) {
                String sub = title.substring(idx + "Level ".length()).trim();
                // sub enthält z.B. "3)" oder "3)" – entferne nicht-digit-zeichen
                String num = sub.replaceAll("[^0-9]", "");
                if (!num.isEmpty()) {
                    level = Integer.parseInt(num);
                }
            }
        } catch (Exception ignored) {
        }

        Player player = (Player) e.getWhoClicked();

        if (display.getItemMeta() != null && display.getItemMeta().hasDisplayName()) {
            guiManager.handleLevelChoice(player, display, level); // ItemStack-Overload
        } else {
            guiManager.handleLevelChoice(player, display.getType().name(), level); // String-Overload
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = e.getView().title().toString();
        if (!title.contains("Level Up")) return;
        try {
            if (guiManager != null && guiManager.getGameManager() != null) {
                java.util.UUID uuid = e.getPlayer().getUniqueId();
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
                // kräftiger Knockback im Umkreis, nur unsere Wave-Mobs
                try { guiManager.getGameManager().getSpawnManager().repelMobsAround(p, 8.0, 1.2, true); } catch (Throwable ignored) {}
                guiManager.getGameManager().resumeForPlayer(uuid);
                guiManager.getGameManager().tryOpenNextQueued(uuid);
            }
        } catch (Throwable ignored) {}
    }
}
