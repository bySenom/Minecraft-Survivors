// File: src/main/java/org/bysenom/minecraftSurvivors/command/StartCommand.java
package org.bysenom.minecraftSurvivors.command;

import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartCommand implements CommandExecutor {

    private final GameManager gameManager;

    public StartCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        gameManager.startGame();
        sender.sendMessage("§aSpiel gestartet.");
        return true;
    }
}
