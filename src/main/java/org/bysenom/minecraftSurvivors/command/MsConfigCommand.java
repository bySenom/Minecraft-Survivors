package org.bysenom.minecraftSurvivors.command;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MsConfigCommand implements CommandExecutor {

    private final GameManager gameManager;

    public MsConfigCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /msconfig reload | /msconfig preset <name> | /msconfig show");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            gameManager.reloadConfigAndApply();
            sender.sendMessage("§aMinecraftSurvivors config reloaded and applied.");
            return true;
        }
        if (args[0].equalsIgnoreCase("preset")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /msconfig preset <name>");
                return true;
            }
            String sub = args[1];
            if (sub.equalsIgnoreCase("list")) {
                org.bukkit.configuration.ConfigurationSection presets = gameManager.getPlugin().getConfig().getConfigurationSection("presets");
                if (presets == null) {
                    sender.sendMessage("§cNo presets defined.");
                    return true;
                }
                java.util.Set<String> keys = presets.getKeys(false);
                if (keys.isEmpty()) {
                    sender.sendMessage("§cNo presets available.");
                    return true;
                }
                sender.sendMessage("§6Available presets:");
                for (String k : keys) {
                    sender.sendMessage(" - §a" + k);
                }
                return true;
            }

            if (sub.equalsIgnoreCase("show")) {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /msconfig preset show <name>");
                    return true;
                }
                String name = args[2];
                org.bukkit.configuration.ConfigurationSection presets = gameManager.getPlugin().getConfig().getConfigurationSection("presets");
                if (presets == null || !presets.isConfigurationSection(name)) {
                    sender.sendMessage("§cPreset not found: " + name);
                    return true;
                }
                org.bukkit.configuration.ConfigurationSection p = presets.getConfigurationSection(name);
                sender.sendMessage("§6Preset: §a" + name);
                for (String key : p.getKeys(false)) {
                    sender.sendMessage(" - §e" + key + ": §f" + p.get(key));
                }
                return true;
            }

            String preset = sub;
            boolean ok = applyPreset(preset);
            if (ok) {
                // Auto-reload and apply
                gameManager.reloadConfigAndApply();
                sender.sendMessage("§aPreset '" + preset + "' applied and config reloaded.");
            } else {
                sender.sendMessage("§cPreset not found: " + preset);
            }
            return true;
        }
        sender.sendMessage("Unknown subcommand. Usage: /msconfig reload");
        return true;
    }

    private boolean applyPreset(String name) {
        org.bukkit.configuration.ConfigurationSection presets = gameManager.getPlugin().getConfig().getConfigurationSection("presets");
        if (presets == null || !presets.isConfigurationSection(name)) return false;
        org.bukkit.configuration.ConfigurationSection preset = presets.getConfigurationSection(name);
        if (preset == null) return false;
        // Kopiere Keys in root 'spawn' Section
        org.bukkit.configuration.ConfigurationSection spawn = gameManager.getPlugin().getConfig().getConfigurationSection("spawn");
        if (spawn == null) {
            spawn = gameManager.getPlugin().getConfig().createSection("spawn");
        }
        for (String key : preset.getKeys(false)) {
            spawn.set(key, preset.get(key));
        }
        gameManager.getPlugin().saveConfig();
        return true;
    }
}
