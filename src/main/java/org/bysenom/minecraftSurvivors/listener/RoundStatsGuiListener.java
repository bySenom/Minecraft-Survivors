package org.bysenom.minecraftSurvivors.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class RoundStatsGuiListener implements Listener {
    private final MinecraftSurvivors plugin;
    public RoundStatsGuiListener(MinecraftSurvivors plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        try {
            // detect our RoundStats GUI by header item (slot 0 PAPER with display name 'Round Report')
            if (top.getSize() >= 1) {
                var it = top.getItem(0);
                if (it != null && it.getType() == org.bukkit.Material.PAPER && it.hasItemMeta()) {
                    var meta = it.getItemMeta();
                    if (meta != null) {
                        Component comp = meta.displayName();
                        if (comp != null) {
                            String text = PlainTextComponentSerializer.plainText().serialize(comp);
                            if (text != null && text.contains("Round Report")) {
                                e.setCancelled(true);
                                int slot = e.getRawSlot();
                                var human = e.getWhoClicked();
                                if (!(human instanceof org.bukkit.entity.Player)) return;
                                org.bukkit.entity.Player p = (org.bukkit.entity.Player) human;
                                // read header lore to determine current global page and total pages
                                int globalPage = 1;
                                int totalPages = 1;
                                try {
                                    var lore = meta.lore();
                                    if (lore != null) {
                                        for (var c : lore) {
                                            String lt = PlainTextComponentSerializer.plainText().serialize((Component)c);
                                            if (lt != null && lt.startsWith("MS_GLOBAL_PAGE:")) {
                                                try { globalPage = Integer.parseInt(lt.substring("MS_GLOBAL_PAGE:".length())); } catch (Throwable ignored) {}
                                            }
                                            if (lt != null && lt.startsWith("MS_TOTAL_PAGES:")) {
                                                try { totalPages = Integer.parseInt(lt.substring("MS_TOTAL_PAGES:".length())); } catch (Throwable ignored) {}
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                                 if (slot == 26) {
                                     p.closeInventory();
                                     p.performCommand("msroundstats show 2");
                                 } else if (slot == 18) {
                                     p.closeInventory();
                                     p.performCommand("msroundstats show 1");
                                 } else if (slot == 53) {
                                    // next global page
                                    int next = Math.min(totalPages, globalPage + 1);
                                    p.closeInventory();
                                    p.performCommand("msroundstats show " + next);
                                } else if (slot == 45) {
                                    int prevPage = Math.max(1, globalPage - 1);
                                    p.closeInventory();
                                    p.performCommand("msroundstats show " + prevPage);
                                 }
                             }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
