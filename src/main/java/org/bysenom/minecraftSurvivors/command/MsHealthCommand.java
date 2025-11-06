package org.bysenom.minecraftSurvivors.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;

public class MsHealthCommand implements CommandExecutor {
    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        MinecraftSurvivors plugin = MinecraftSurvivors.getInstance();
        PlayerManager pm = plugin.getPlayerManager();
        GameManager gm = plugin.getGameManager();

        String version = plugin.getDescription().getVersion();
        int autosaveSec = plugin.getConfigUtil().getInt("data.autosave-interval-seconds", 120);
        int loaded = pm != null ? pm.getAll().size() : 0;
        int online = Bukkit.getOnlinePlayers().size();
        String state = gm != null ? String.valueOf(gm.getState()) : "UNKNOWN";
        int wave = gm != null ? gm.getCurrentWaveNumber() : -1;
        boolean continuous = plugin.getConfigUtil().getBoolean("spawn.continuous.enabled", true);

        sender.sendMessage("§6MinecraftSurvivors §7Status");
        sender.sendMessage("§7Version: §a" + version);
        sender.sendMessage("§7Autosave: §a" + (autosaveSec > 0 ? autosaveSec + "s" : "aus") );
        sender.sendMessage("§7Spieler: §a" + loaded + " §7geladen / §a" + online + " §7online");
        sender.sendMessage("§7GameState: §a" + state + " §7| Wave: §a" + (wave >= 0 ? wave : "-") + " §7| Continuous: §a" + continuous);
        return true;
    }
}
