package org.bysenom.lobby.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.QueueManager;

public class StartSurvivorsCommand implements CommandExecutor {
    private final LobbySystem plugin;
    private final QueueManager qm;
    public StartSurvivorsCommand(LobbySystem plugin, QueueManager qm) { this.plugin = plugin; this.qm = qm; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lobby.admin")) { sender.sendMessage("§cKeine Berechtigung."); return true; }
        if (qm.size() == 0) { sender.sendMessage("§eKeine Spieler in der Queue."); return true; }
        sender.sendMessage("§aStarte Survivors-Match für " + qm.size() + " Spieler...");
        // Simple Übergabe: Öffne bei allen Queue-Spielern das MS-Menü und triggere optional Start-Countdown via Command
        for (java.util.UUID id : qm.snapshot()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                // Öffnet MS Hauptmenü (so können Klassen gewählt werden etc.)
                p.performCommand("msmenu");
            }
        }
        // Optional kann Admin direkt Countdown im MS-Plugin triggern
        // Bukkit.dispatchCommand(sender, "msstart 10"); // falls vorhanden
        // Reset any running countdown on manual start
        org.bysenom.lobby.LobbySystem.get().resetCountdown();
        return true;
    }
}
