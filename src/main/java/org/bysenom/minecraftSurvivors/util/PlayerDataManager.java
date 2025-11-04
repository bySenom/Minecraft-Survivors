package org.bysenom.minecraftSurvivors.util;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class PlayerDataManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final File playersDir;

    public PlayerDataManager(MinecraftSurvivors plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) playersDir.mkdirs();
    }

    public void save(SurvivorPlayer sp) {
        if (sp == null) return;
        File f = new File(playersDir, sp.getUuid().toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("uuid", sp.getUuid().toString());
        cfg.set("kills", sp.getKills());
        cfg.set("coins", sp.getCoins());
        cfg.set("selectedClass", sp.getSelectedClass() != null ? sp.getSelectedClass().name() : null);
        cfg.set("classLevel", sp.getClassLevel());
        cfg.set("xp", sp.getXp());
        cfg.set("xpToNext", sp.getXpToNext());
        cfg.set("bonusDamage", sp.getBonusDamage());
        cfg.set("bonusStrikes", sp.getBonusStrikes());
        cfg.set("flatDamage", sp.getFlatDamage());
        cfg.set("extraHearts", sp.getExtraHearts());
        try {
            cfg.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + sp.getUuid() + ": " + e.getMessage());
        }
    }

    public SurvivorPlayer load(UUID uuid) {
        if (uuid == null) return null;
        File f = new File(playersDir, uuid.toString() + ".yml");
        if (!f.exists()) return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        try {
            SurvivorPlayer sp = new SurvivorPlayer(uuid);
            sp.setKills(cfg.getInt("kills", 0));
            sp.setCoins(cfg.getInt("coins", 0));
            String cls = cfg.getString("selectedClass", null);
            if (cls != null) {
                try {
                    sp.setSelectedClass(PlayerClass.valueOf(cls));
                } catch (IllegalArgumentException ignored) {}
            }
            sp.setClassLevel(cfg.getInt("classLevel", 1));
            sp.setXp(cfg.getInt("xp", 0));
            sp.setXpToNext(cfg.getInt("xpToNext", Math.max(1, 5 * sp.getClassLevel())));
            sp.setBonusDamage(cfg.getDouble("bonusDamage", 0.0));
            sp.setBonusStrikes(cfg.getInt("bonusStrikes", 0));
            sp.setFlatDamage(cfg.getDouble("flatDamage", 0.0));
            sp.setExtraHearts(cfg.getInt("extraHearts", 0));
            return sp;
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + t.getMessage());
            return null;
        }
    }

    public void saveAll() {
        Collection<SurvivorPlayer> all = playerManager.getAll();
        for (SurvivorPlayer sp : all) {
            save(sp);
        }
    }
}

