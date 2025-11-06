package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class LevelUpMenuListener implements Listener {

    private final GuiManager guiManager;

    public LevelUpMenuListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;

        e.setCancelled(true);
        ItemStack display = e.getCurrentItem();
        if (display == null) return;

        // Versuche die Level-Nummer aus dem Titel zu parsen: "... (Level X)"
        int level = 1;
        try {
            int idx = title.toLowerCase(java.util.Locale.ROOT).lastIndexOf("level ");
            if (idx >= 0) {
                String sub = title.substring(idx + "level ".length()).trim();
                String num = sub.replaceAll("[^0-9]", "");
                if (!num.isEmpty()) {
                    level = Integer.parseInt(num);
                }
            }
        } catch (Exception ignored) {
        }

        Player player = (Player) e.getWhoClicked();

        if (display.getItemMeta() != null && display.getItemMeta().hasDisplayName()) {
            guiManager.handleLevelChoice(player, display, level);
        } else {
            guiManager.handleLevelChoice(player, display.getType().name(), level);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;
        try {
            if (guiManager != null && guiManager.getGameManager() != null) {
                java.util.UUID uuid = e.getPlayer().getUniqueId();
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
                try { guiManager.getGameManager().getSpawnManager().repelMobsAround(p, 8.0, 1.2, true); } catch (Throwable ignored) {}
                guiManager.getGameManager().resumeForPlayer(uuid);
                guiManager.getGameManager().tryOpenNextQueued(uuid);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;
        try {
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            p.spawnParticle(org.bukkit.Particle.ENCHANT, p.getLocation().add(0,1.4,0), 24, 0.4,0.4,0.4, 0.02);
        } catch (Throwable ignored) {}
    }
}
