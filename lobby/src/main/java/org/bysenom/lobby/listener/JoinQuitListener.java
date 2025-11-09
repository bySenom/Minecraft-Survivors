package org.bysenom.lobby.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bysenom.lobby.QueueManager;

public class JoinQuitListener implements Listener {
    private final QueueManager qm;
    public JoinQuitListener(QueueManager qm) { this.qm = qm; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Option: Auto-open Lobby-Menu on join
        // e.getPlayer().performCommand("lobby");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        qm.leaveSilent(e.getPlayer());
    }
}
