// java
// File: src/main/java/org/bysenom/minecraftSurvivors/util/ConfigUtil.java
package org.bysenom.minecraftSurvivors.util;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStream;

public class ConfigUtil {

    private final MinecraftSurvivors plugin;
    private FileConfiguration cfg;

    public ConfigUtil(MinecraftSurvivors plugin) {
        this.plugin = plugin;

        // Prüfen, ob eine eingebettete config.yml vorhanden ist, bevor saveDefaultConfig() aufgerufen wird.
        InputStream defaultConfig = plugin.getResource("config.yml");
        if (defaultConfig != null) {
            plugin.saveDefaultConfig();
        } else {
            plugin.getLogger().warning("Default config.yml not found inside the plugin jar — using server config (no default saved).");
        }

        this.cfg = plugin.getConfig();
    }

    public synchronized void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public int getInt(String path, int def) {
        if (!cfg.contains(path)) {
            cfg.set(path, def);
            plugin.saveConfig();
            return def;
        }
        return cfg.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        if (!cfg.contains(path)) {
            cfg.set(path, def);
            plugin.saveConfig();
            return def;
        }
        return cfg.getDouble(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        if (!cfg.contains(path)) {
            cfg.set(path, def);
            plugin.saveConfig();
            return def;
        }
        return cfg.getBoolean(path, def);
    }

    public String getString(String path, String def) {
        if (!cfg.contains(path)) {
            cfg.set(path, def);
            plugin.saveConfig();
            return def;
        }
        return cfg.getString(path, def);
    }

    public void setValue(String path, Object value) {
        try {
            cfg.set(path, value);
            plugin.saveConfig();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to set config value for " + path + ": " + t.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return cfg;
    }
}
