package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.QueueManager;

public class QueueCommand implements CommandExecutor {
    private final QueueManager qm;
    public QueueCommand(QueueManager qm) { this.qm = qm; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("Usage: /queue join|leave|status|next");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "join":
                qm.join(p);
                return true;
            case "leave":
                qm.leave(p);
                return true;
            case "status": {
                int pos = qm.getPosition(p.getUniqueId());
                boolean admitted = qm.isAdmitted(p.getUniqueId());
                int interval = Math.max(1, LobbySystem.get().getConfig().getInt("admission.interval-seconds", 3));
                int eta = pos > 0 ? (pos - 1) * interval : -1;
                p.sendMessage("§bQueue-Status: §7Pos §e" + pos + " §7• Zugelassen: " + (admitted ? "§aJa" : "§cNein") + (eta >= 0 ? " §7• ETA ~§e" + eta + "s" : ""));
                return true;
            }
            case "next":
                if (!p.hasPermission("lobby.admin")) { p.sendMessage("§cKeine Berechtigung."); return true; }
                java.util.UUID id = qm.admitNext();
                p.sendMessage(id == null ? "§eNiemand in der Queue." : "§aNächster Spieler zugelassen.");
                return true;
            default:
                sender.sendMessage("Usage: /queue join|leave|status|next");
                return true;
        }
    }
}
