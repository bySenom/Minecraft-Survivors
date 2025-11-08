package org.bysenom.minecraftSurvivors.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.jetbrains.annotations.NotNull;

public class MsConfigTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) return Collections.emptyList();
        MinecraftSurvivors pl = MinecraftSurvivors.getInstance();
        if (pl == null) return Collections.emptyList();
        if (args.length == 1) {
            List<String> base = List.of("reload", "preset", "set", "confirm", "cancel", "show");
            return StringUtil.copyPartialMatches(args[0], base, new ArrayList<>());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("preset")) {
                org.bukkit.configuration.ConfigurationSection sec = pl.getConfig().getConfigurationSection("presets");
                Set<String> keys = sec != null ? sec.getKeys(false) : Collections.emptySet();
                List<String> out = new ArrayList<>(keys);
                out.add("list");
                out.add("show");
                return StringUtil.copyPartialMatches(args[1], out, new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("set")) {
                // biete Top-Level Keys an
                Set<String> keys = pl.getConfig().getKeys(false);
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(keys), new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("show")) {
                Set<String> keys = pl.getConfig().getKeys(false);
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(keys), new ArrayList<>());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("preset") && args[1].equalsIgnoreCase("show")) {
                org.bukkit.configuration.ConfigurationSection sec = pl.getConfig().getConfigurationSection("presets");
                Set<String> keys = sec != null ? sec.getKeys(false) : Collections.emptySet();
                return StringUtil.copyPartialMatches(args[2], new ArrayList<>(keys), new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("set")) {
                // Vorschläge für Werte: true/false wenn boolean, ansonsten leer
                Object cur = pl.getConfig().get(args[1]);
                if (cur instanceof Boolean) {
                    return StringUtil.copyPartialMatches(args[2], List.of("true", "false"), new ArrayList<>());
                }
            }
        }
        return Collections.emptyList();
    }
}
