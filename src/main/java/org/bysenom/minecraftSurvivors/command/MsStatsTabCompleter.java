package org.bysenom.minecraftSurvivors.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class MsStatsTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = List.of("mode", "toggle", "top", "show", "reset");
            return StringUtil.copyPartialMatches(args[0], base, new ArrayList<>());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("mode")) {
                List<String> modes = List.of("actionbar", "bossbar", "scoreboard", "off");
                return StringUtil.copyPartialMatches(args[1], modes, new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("top")) {
                List<String> kinds = List.of("dps", "hps");
                return StringUtil.copyPartialMatches(args[1], kinds, new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("toggle")) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList();
                return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            List<String> sizes = List.of("5", "10", "20", "50");
            return StringUtil.copyPartialMatches(args[2], sizes, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
