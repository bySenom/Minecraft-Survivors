package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bysenom.lobby.QueueManager;
import org.jetbrains.annotations.NotNull;

public class QueueCommand implements CommandExecutor, TabCompleter {
    private final QueueManager qm;
    public QueueCommand(QueueManager qm) { this.qm = qm; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Nur ingame."); return true; }
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
                double etaSec = pos > 0 ? qm.getRollingEtaSeconds(pos) : -1;
                String fullHint = qm.isFull() && !admitted ? " §c(Volle Zugangs-Slots)" : "";
                p.sendMessage("§bQueue-Status: §7Pos §e" + pos + " §7• Zugelassen: " + (admitted ? "§aJa" : "§cNein") + (etaSec >= 0 ? " §7• ETA ~§e" + String.format(java.util.Locale.ROOT, "%.0fs", etaSec) : "") + fullHint);
                return true;
            }
            case "stats": {
                if (!p.hasPermission("lobby.admin")) { p.sendMessage("§cKeine Berechtigung."); return true; }
                double avg = qm.getAverageWaitSeconds();
                double eta2 = qm.getRollingEtaSeconds(2);
                p.sendMessage("§eQueue Stats: §7Queued=" + qm.size() + " Admitted=" + qm.admittedCount() + " MaxAdmitted=" + qm.getMaxAdmitted() + " AvgWait=" + String.format(java.util.Locale.ROOT, "%.1fs", avg) + " ETA(next)=" + String.format(java.util.Locale.ROOT, "%.0fs", eta2));
                return true;
            }
            case "next":
                if (!p.hasPermission("lobby.admin")) { p.sendMessage("§cKeine Berechtigung."); return true; }
                java.util.UUID id = qm.admitNext();
                p.sendMessage(id == null ? "§eNiemand in der Queue." : "§aNächster Spieler zugelassen.");
                return true;
            default:
                sender.sendMessage("Usage: /queue join|leave|status|stats|next");
                return true;
        }
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("join", "leave", "status", "next");
        }
        return java.util.Collections.emptyList();
    }
}
