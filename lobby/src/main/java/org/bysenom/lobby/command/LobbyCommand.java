package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.lobby.UiManager;

public class LobbyCommand implements CommandExecutor {
    private final UiManager ui;
    public LobbyCommand(UiManager ui) { this.ui = ui; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        ui.openLobbyMenu((Player) sender);
        return true;
    }
}
