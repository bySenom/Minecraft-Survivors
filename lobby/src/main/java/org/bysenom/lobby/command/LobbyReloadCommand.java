package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bysenom.lobby.LobbySystem;

public class LobbyReloadCommand implements CommandExecutor {
    private final LobbySystem plugin;
    public LobbyReloadCommand(LobbySystem plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lobby.admin")) { sender.sendMessage("§cKeine Berechtigung."); return true; }
        plugin.reloadConfig();
        plugin.reinitRuntime();
        sender.sendMessage("§aLobby-Konfiguration neu geladen.");
        return true;
    }
}
