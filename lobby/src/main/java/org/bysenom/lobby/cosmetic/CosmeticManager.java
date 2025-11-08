package org.bysenom.lobby.cosmetic;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Einfacher Cosmetic-Manager mit YAML-Persistenz (Unlock-Flags).
 */
public class CosmeticManager {

    private final Plugin plugin;
    private final java.util.Map<UUID, Set<String>> unlocked = new java.util.concurrent.ConcurrentHashMap<>();
    private File file;
    private FileConfiguration cfg;
    private volatile boolean dirty = false; // neu: nur periodisch speichern
    private org.bukkit.scheduler.BukkitTask autosaveTask; // neu

    public CosmeticManager(Plugin plugin) {
        this.plugin = plugin;
        init();
        setupAutosave(); // neu
    }

    private void init() {
        file = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!file.exists()) {
            try { plugin.getDataFolder().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void setupAutosave() {
        int interval = Math.max(30, plugin.getConfig().getInt("cosmetics.autosave-seconds", 120));
        autosaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (dirty) save();
        }, interval * 20L, interval * 20L);
    }

    private void load() {
        unlocked.clear();
        if (cfg == null) return;
        if (!cfg.isConfigurationSection("players")) return;
        for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = java.util.UUID.fromString(key);
                java.util.List<String> list = cfg.getStringList("players." + key + ".unlocked");
                unlocked.put(id, new java.util.HashSet<>(list));
            } catch (IllegalArgumentException ignored) {}
        }
        dirty = false; // reset
    }

    private void save() {
        if (cfg == null) cfg = new YamlConfiguration();
        cfg.set("players", null);
        for (java.util.Map.Entry<UUID, Set<String>> e : unlocked.entrySet()) {
            cfg.set("players." + e.getKey() + ".unlocked", new java.util.ArrayList<>(e.getValue()));
        }
        try { cfg.save(file); } catch (IOException ignored) {}
        dirty = false;
    }

    private Set<String> get(UUID player) { return unlocked.computeIfAbsent(player, k -> new HashSet<>()); }

    public boolean isUnlocked(UUID player, String key) { return get(player).contains(key); }

    public void unlock(UUID player, String key) { if (get(player).add(key)) dirty = true; }

    public boolean revoke(UUID player, String key) { boolean r = get(player).remove(key); if (r) dirty = true; return r; }

    public void clear(UUID player) { unlocked.remove(player); dirty = true; }

    // Lesender Zugriff für Statusanzeigen
    public java.util.Set<String> getUnlockedKeys(UUID player) { return new java.util.HashSet<>(get(player)); }

    public void flushNow() { save(); }

    public void shutdown() { if (autosaveTask != null) autosaveTask.cancel(); if (dirty) save(); }
}
