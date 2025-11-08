package org.bysenom.lobby.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class NpcClickListener implements Listener {
    private final org.bysenom.lobby.npc.PlayerNpcRegistry registry;
    public NpcClickListener(org.bysenom.lobby.npc.PlayerNpcRegistry registry) { this.registry = registry; }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent e) {
        try {
            if (registry.handleNpcClick(e.getRightClicked(), e.getPlayer())) {
                e.setCancelled(true);
            }
        } catch (Throwable ignored) {}
    }
}
