package org.bysenom.minecraftSurvivors.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

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
        cfg.set("moveSpeedMult", sp.getMoveSpeedMult());
        cfg.set("attackSpeedMult", sp.getAttackSpeedMult());
        cfg.set("damageResist", sp.getDamageResist());
        cfg.set("luck", sp.getLuck());
        // unlocked abilities and glyphs
        cfg.set("unlockedAbilities", new java.util.ArrayList<>(sp.getUnlockedAbilities()));
        cfg.set("unlockedGlyphs", new java.util.ArrayList<>(sp.getUnlockedGlyphs()));
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
            // Klasse NICHT automatisch laden (verhindert erzwungene Auswahl). Optional via Config aktivierbar.
            boolean loadClass = plugin.getConfigUtil().getBoolean("profile.load-class", false);
            if (loadClass) {
                String cls = cfg.getString("selectedClass", null);
                if (cls != null) {
                    try { sp.setSelectedClass(PlayerClass.valueOf(cls)); } catch (IllegalArgumentException ignored) {}
                }
            }
            sp.setClassLevel(cfg.getInt("classLevel", 1));
            sp.setXp(cfg.getInt("xp", 0));
            sp.setXpToNext(cfg.getInt("xpToNext", Math.max(1, 5 * sp.getClassLevel())));
            sp.setBonusDamage(cfg.getDouble("bonusDamage", 0.0));
            sp.setBonusStrikes(cfg.getInt("bonusStrikes", 0));
            sp.setFlatDamage(cfg.getDouble("flatDamage", 0.0));
            sp.setExtraHearts(cfg.getInt("extraHearts", 0));
            sp.setMoveSpeedMult(cfg.getDouble("moveSpeedMult", 0.0));
            sp.setAttackSpeedMult(cfg.getDouble("attackSpeedMult", 0.0));
            sp.setDamageResist(cfg.getDouble("damageResist", 0.0));
            sp.setLuck(cfg.getDouble("luck", 0.0));
            java.util.List<String> uas = cfg.getStringList("unlockedAbilities");
            for (String a : uas) { try { if (a != null && !a.isEmpty()) sp.unlockAbility(a); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "Failed to unlock ability for player " + uuid + ": ", t); } }
            java.util.List<String> ugs = cfg.getStringList("unlockedGlyphs");
            for (String g : ugs) { try { if (g != null && !g.isEmpty()) sp.unlockGlyph(g); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "Failed to unlock glyph for player " + uuid + ": ", t); } }
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

    /**
     * Persist only coins for a player (used on death to preserve coins while other fields may reset).
     */
    public void saveCoins(UUID uuid, int coins) {
        if (uuid == null) return;
        File f = new File(playersDir, uuid.toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("uuid", uuid.toString());
        cfg.set("coins", coins);
        try {
            cfg.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save coins for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Persist a single player's full data asynchronously to avoid blocking the main thread.
     */
    public void saveAsync(SurvivorPlayer sp) {
        if (sp == null) return;
        // schedule asynchronous save using Bukkit scheduler
        try {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try { save(sp); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.WARNING, "Async save failed for " + sp.getUuid() + ": ", t); }
            });
        } catch (Throwable t) {
            // Fallback: do synchronous save if scheduling not possible
            try { save(sp); } catch (Throwable t2) { plugin.getLogger().log(java.util.logging.Level.WARNING, "Fallback sync save failed for " + sp.getUuid() + ": ", t2); }
        }
    }
}
