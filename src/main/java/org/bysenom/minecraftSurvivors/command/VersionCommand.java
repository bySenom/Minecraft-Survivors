package org.bysenom.minecraftSurvivors.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class VersionCommand implements CommandExecutor {
    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MinecraftSurvivors plugin = MinecraftSurvivors.getInstance();
        String name = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        sender.sendMessage("§6" + name + " §7version §a" + version);
        return true;
    }
}
