package org.bysenom.minecraftSurvivors.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bysenom.minecraftSurvivors.manager.GameManager;

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
            sender.sendMessage("Usage: /msconfig reload | /msconfig preset <name> | /msconfig set <path> <value> | /msconfig show");
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
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /msconfig set <path> <value>");
                return true;
            }
            String path = args[1];
            // join remaining args as value
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(args[i]);
            }
            String raw = sb.toString();
            org.bukkit.configuration.file.FileConfiguration cfg = gameManager.getPlugin().getConfig();
            // try to infer type from existing value
            Object cur = cfg.get(path);
            try {
                if (cur instanceof Boolean) {
                    boolean v = Boolean.parseBoolean(raw);
                    gameManager.getPlugin().getConfigUtil().setValue(path, v);
                } else if (cur instanceof Integer) {
                    int v = Integer.parseInt(raw);
                    gameManager.getPlugin().getConfigUtil().setValue(path, v);
                } else if (cur instanceof Double || cur instanceof Float) {
                    double v = Double.parseDouble(raw);
                    gameManager.getPlugin().getConfigUtil().setValue(path, v);
                } else {
                    // fallback: if raw looks like boolean or number, cast accordingly
                    if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
                        gameManager.getPlugin().getConfigUtil().setValue(path, Boolean.parseBoolean(raw));
                    } else {
                        try {
                            if (raw.contains(".")) {
                                double dv = Double.parseDouble(raw);
                                gameManager.getPlugin().getConfigUtil().setValue(path, dv);
                            } else {
                                int iv = Integer.parseInt(raw);
                                gameManager.getPlugin().getConfigUtil().setValue(path, iv);
                            }
                        } catch (NumberFormatException nfe) {
                            // treat as string
                            gameManager.getPlugin().getConfigUtil().setValue(path, raw);
                        }
                    }
                }
                // apply immediately
                gameManager.reloadConfigAndApply();
                sender.sendMessage("§aSet: " + path + " = " + raw);
            } catch (Throwable t) {
                sender.sendMessage("§cFailed to set config: " + t.getMessage());
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
