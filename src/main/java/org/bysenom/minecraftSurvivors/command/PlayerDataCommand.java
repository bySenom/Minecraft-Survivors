package org.bysenom.minecraftSurvivors.command;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class PlayerDataCommand implements CommandExecutor {
    private final MinecraftSurvivors plugin;
    public PlayerDataCommand(MinecraftSurvivors plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /msplayerdata log <player|uuid>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("log")) {
            if (args.length < 2) { sender.sendMessage("§cUsage: /msplayerdata log <player|uuid>"); return true; }
            String target = args[1];
            UUID uuid = null;
            try {
                uuid = UUID.fromString(target);
            } catch (Throwable ignored) {
                Player p = Bukkit.getPlayerExact(target);
                if (p != null) uuid = p.getUniqueId();
            }
            if (uuid == null) { sender.sendMessage("§cSpieler nicht gefunden: " + target); return true; }
            plugin.getPlayerDataManager().logPlayerData(uuid);
            sender.sendMessage("§aPlayerData logged for " + uuid);
            return true;
        }
        sender.sendMessage("§cUnbekannter Subcommand: " + sub);
        return true;
    }
}

