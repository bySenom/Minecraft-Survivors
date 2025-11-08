// java
// File: src/main/java/org/bysenom/minecraftSurvivors/command/OpenGuiCommand.java
package org.bysenom.minecraftSurvivors.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.gui.GuiManager;

public class OpenGuiCommand implements CommandExecutor {

    private final GuiManager guiManager;

    public OpenGuiCommand(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können das Menü öffnen.");
            return true;
        }
        Player p = (Player) sender;
        // Spieler betritt Survivors-Kontext: HUD/Scoreboard aktivieren
        try { org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getGameManager().enterSurvivorsContext(p.getUniqueId()); } catch (Throwable ignored) {}
        guiManager.openMainMenu(p);
        return true;
    }
}
