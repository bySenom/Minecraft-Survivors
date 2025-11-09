package org.bysenom.lobby.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bysenom.lobby.QueueManager;

public class AfkActivityListener implements Listener {
    private final QueueManager qm;
    public AfkActivityListener(QueueManager qm){ this.qm = qm; }

    private void touch(Player p){
        if (p == null) return;
        try { if (qm.isInQueue(p.getUniqueId())) qm.markActive(p.getUniqueId()); } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        try {
            if (e.getFrom() == null || e.getTo() == null) return;
            if (e.getFrom().getWorld() != e.getTo().getWorld()) { touch(e.getPlayer()); return; }
            double dx = e.getTo().getX() - e.getFrom().getX();
            double dy = e.getTo().getY() - e.getFrom().getY();
            double dz = e.getTo().getZ() - e.getFrom().getZ();
            if ((dx*dx + dy*dy + dz*dz) > 0.0004) touch(e.getPlayer());
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){ touch(e.getPlayer()); }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){ touch(e.getPlayer()); }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){ if (e.getWhoClicked() instanceof Player p) touch(p); }
}
