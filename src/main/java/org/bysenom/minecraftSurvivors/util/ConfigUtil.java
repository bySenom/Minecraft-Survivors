// java
// File: src/main/java/org/bysenom/minecraftSurvivors/util/ConfigUtil.java
package org.bysenom.minecraftSurvivors.util;

import java.io.InputStream;
import org.bukkit.configuration.file.FileConfiguration;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class ConfigUtil {

    public static final class Keys {
        // Stats
        public static final String STATS_MODE = "stats.mode";
        public static final String STATS_UPDATE_INTERVAL_TICKS = "stats.update-interval-ticks";
        public static final String STATS_BROADCAST_TOP_ENABLED = "stats.broadcast-top.enabled";
        public static final String STATS_BROADCAST_TOP_INTERVAL_SECONDS = "stats.broadcast-top.interval-seconds";
        public static final String STATS_BROADCAST_TOP_N = "stats.broadcast-top.n";
        public static final String STATS_DYNAMIC_CAP_ENABLED = "stats.dynamic-cap-enabled";
        public static final String STATS_AUTO_CAP_DPS = "stats.auto-cap.dps";
        public static final String STATS_AUTO_CAP_HPS = "stats.auto-cap.hps";
        public static final String STATS_DYNAMIC_CAP_SMOOTHING = "stats.dynamic-cap-smoothing";
        public static final String STATS_WINDOW_SECONDS = "stats.window-seconds";
        // Levelup / HUD
        public static final String LEVELUP_HUD_INTERVAL_TICKS = "levelup.hud-interval-ticks";
        public static final String LEVELUP_CHOICE_MAX_SECONDS = "levelup.choice-max-seconds";
        // Spawn
        public static final String SPAWN_CONTINUOUS_ENABLED = "spawn.continuous.enabled";
        public static final String SPAWN_FREEZE_RADIUS = "spawn.freeze-radius";
        // Data
        public static final String DATA_AUTOSAVE_INTERVAL_SECONDS = "data.autosave-interval-seconds";
        // Tablist
        public static final String TABLIST_ENABLED = "tablist.enabled";
        public static final String TABLIST_UPDATE_INTERVAL_TICKS = "tablist.update-interval-ticks";
        public static final String TABLIST_SHOW_ENEMY_POWER = "tablist.show-enemy-power";
        public static final String TABLIST_SHOW_PARTY_HP = "tablist.show-party-hp";
        public static final String TABLIST_HEADER_TITLE = "tablist.header-title";
    }

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
