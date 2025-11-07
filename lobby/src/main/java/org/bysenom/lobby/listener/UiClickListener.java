package org.bysenom.lobby.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bysenom.lobby.UiManager;

public class UiClickListener implements Listener {
    private final UiManager ui;
    public UiClickListener(UiManager ui) { this.ui = ui; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().title() == null) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.startsWith("Lobby")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) return;
        String name = e.getCurrentItem().getItemMeta().hasDisplayName() ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getCurrentItem().getItemMeta().displayName()) : "";
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
        if (name.contains("Queue beitreten")) {
            p.performCommand("queue join");
        } else if (name.contains("Queue verlassen")) {
            p.performCommand("queue leave");
        } else if (name.contains("Survivors-Menü")) {
            p.performCommand("msmenu");
        }
    }
}
