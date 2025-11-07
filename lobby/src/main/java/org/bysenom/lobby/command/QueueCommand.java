package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.lobby.QueueManager;

public class QueueCommand implements CommandExecutor {
    private final QueueManager qm;
    public QueueCommand(QueueManager qm) { this.qm = qm; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("Usage: /queue join|leave");
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) {
            qm.join(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            qm.leave(p);
            return true;
        }
        sender.sendMessage("Usage: /queue join|leave");
        return true;
    }
}
