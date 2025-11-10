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
        cfg.set("damageMult", sp.getDamageMult());
        cfg.set("radiusMult", sp.getRadiusMult());
        cfg.set("bonusStrikes", sp.getBonusStrikes());
        cfg.set("extraHearts", sp.getExtraHearts());
        cfg.set("igniteBonusTicks", sp.getIgniteBonusTicks());
        cfg.set("knockbackBonus", sp.getKnockbackBonus());
        cfg.set("healBonus", sp.getHealBonus());
        cfg.set("moveSpeedMult", sp.getMoveSpeedMult());
        cfg.set("attackSpeedMult", sp.getAttackSpeedMult());
        // persist base HP regen so players keep their level-up choices
        try { cfg.set("hpRegenBase", sp.getBaseHpRegen()); } catch (Throwable ignored) {}
        cfg.set("damageResist", sp.getDamageResist());
        cfg.set("luck", sp.getLuck());
        // unlocked abilities and glyphs
        cfg.set("unlockedAbilities", new java.util.ArrayList<>(sp.getUnlockedAbilities()));
        cfg.set("unlockedGlyphs", new java.util.ArrayList<>(sp.getUnlockedGlyphs()));
        // Preferences & counters
        cfg.set("fxEnabled", sp.isFxEnabled());
        cfg.set("shopPurchasesRun", sp.getShopPurchasesRun());
        cfg.set("shopPurchasesToday", sp.getShopPurchasesToday());
        cfg.set("shopLastDay", sp.getShopLastDay());
        cfg.set("perRunCounts", sp.getPerRunCounts());
        cfg.set("perDayCounts", sp.getPerDayCounts());
        // Abilities/Skills/Weapons
        cfg.set("abilities", new java.util.ArrayList<>(sp.getAbilities()));
        cfg.set("abilityLevels", sp.getAbilityLevelsMap());
        cfg.set("skills", new java.util.ArrayList<>(sp.getSkills()));
        cfg.set("skillLevels", sp.getSkillLevelsMap());
        cfg.set("weapons", new java.util.ArrayList<>(sp.getWeapons()));
        cfg.set("weaponLevels", sp.getWeaponLevelsMap());
        // Runtime shields
        cfg.set("shieldCurrent", sp.getShieldCurrent());
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
            try { sp.setDamageMult(cfg.getDouble("damageMult", sp.getDamageMult())); } catch (Throwable ignored) {}
            try { sp.setRadiusMult(cfg.getDouble("radiusMult", sp.getRadiusMult())); } catch (Throwable ignored) {}
            try { sp.setBonusStrikes(cfg.getInt("bonusStrikes", sp.getBonusStrikes())); } catch (Throwable ignored) {}
            try { sp.setExtraHearts(cfg.getInt("extraHearts", sp.getExtraHearts())); } catch (Throwable ignored) {}
            try { sp.setIgniteBonusTicks(cfg.getInt("igniteBonusTicks", sp.getIgniteBonusTicks())); } catch (Throwable ignored) {}
            try { sp.setKnockbackBonus(cfg.getDouble("knockbackBonus", sp.getKnockbackBonus())); } catch (Throwable ignored) {}
            try { sp.setHealBonus(cfg.getDouble("healBonus", sp.getHealBonus())); } catch (Throwable ignored) {}
            sp.setMoveSpeedMult(cfg.getDouble("moveSpeedMult", 0.0));
            sp.setAttackSpeedMult(cfg.getDouble("attackSpeedMult", 0.0));
            try { sp.setBaseHpRegen(cfg.getDouble("hpRegenBase", sp.getBaseHpRegen())); } catch (Throwable ignored) {}
            sp.setDamageResist(cfg.getDouble("damageResist", 0.0));
            sp.setLuck(cfg.getDouble("luck", 0.0));
            java.util.List<String> uas = cfg.getStringList("unlockedAbilities");
            for (String a : uas) { try { if (a != null && !a.isEmpty()) sp.unlockAbility(a); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "Failed to unlock ability for player " + uuid + ": ", t); } }
            java.util.List<String> ugs = cfg.getStringList("unlockedGlyphs");
            for (String g : ugs) { try { if (g != null && !g.isEmpty()) sp.unlockGlyph(g); } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "Failed to unlock glyph for player " + uuid + ": ", t); } }
            // Preferences & counters
            try { sp.setFxEnabled(cfg.getBoolean("fxEnabled", true)); } catch (Throwable ignored) {}
            try { sp.setShopPurchasesRun(cfg.getInt("shopPurchasesRun", sp.getShopPurchasesRun())); } catch (Throwable ignored) {}
            try { sp.setShopPurchasesToday(cfg.getInt("shopPurchasesToday", sp.getShopPurchasesToday())); } catch (Throwable ignored) {}
            try { sp.setShopLastDay(cfg.getString("shopLastDay", sp.getShopLastDay())); } catch (Throwable ignored) {}
            try {
                java.util.Map<String,Object> pr = cfg.getConfigurationSection("perRunCounts") != null ? cfg.getConfigurationSection("perRunCounts").getValues(false) : null;
                if (pr != null) {
                    for (var e : pr.entrySet()) {
                        try { sp.setPerRunCount(e.getKey(), Integer.parseInt(String.valueOf(e.getValue()))); } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
            try {
                java.util.Map<String,Object> pd = cfg.getConfigurationSection("perDayCounts") != null ? cfg.getConfigurationSection("perDayCounts").getValues(false) : null;
                if (pd != null) {
                    for (var e : pd.entrySet()) {
                        try { sp.setPerDayCount(e.getKey(), Integer.parseInt(String.valueOf(e.getValue()))); } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
            // Abilities/levels
            try { java.util.List<String> ab = cfg.getStringList("abilities"); if (ab != null) for (String k : ab) if (k != null && !k.isEmpty()) sp.addAbilityAtFirstFree(k, cfg.getInt("abilityLevels." + k, 1)); } catch (Throwable ignored) {}
            try { java.util.List<String> sk = cfg.getStringList("skills"); if (sk != null) for (String k : sk) if (k != null && !k.isEmpty()) { sp.addSkill(k); sp.setSkillLevel(k, cfg.getInt("skillLevels." + k, sp.getSkillLevel(k))); } } catch (Throwable ignored) {}
            try { java.util.List<String> wp = cfg.getStringList("weapons"); if (wp != null) for (String k : wp) if (k != null && !k.isEmpty()) { sp.addWeapon(k); sp.setWeaponLevel(k, cfg.getInt("weaponLevels." + k, sp.getWeaponLevel(k))); } } catch (Throwable ignored) {}
            try { sp.setShieldCurrent(cfg.getDouble("shieldCurrent", sp.getShieldCurrent())); } catch (Throwable ignored) {}
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
     * Persist only durable/persistent player data (base stats, unlocks, abilityLevels).
     * Use this at Run-End to avoid saving transient run-only fields like current coins/kills/weapons.
     */
    public void savePersistent(SurvivorPlayer sp) {
        if (sp == null) return;
        File f = new File(playersDir, sp.getUuid().toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("uuid", sp.getUuid().toString());
        // Persist base upgrade stats only
        cfg.set("flatDamage", sp.getFlatDamage());
        cfg.set("damageMult", sp.getDamageMult());
        cfg.set("radiusMult", sp.getRadiusMult());
        cfg.set("extraHearts", sp.getExtraHearts());
        cfg.set("igniteBonusTicks", sp.getIgniteBonusTicks());
        cfg.set("knockbackBonus", sp.getKnockbackBonus());
        cfg.set("healBonus", sp.getHealBonus());
        cfg.set("moveSpeedMult", sp.getMoveSpeedMult());
        cfg.set("attackSpeedMult", sp.getAttackSpeedMult());
        try { cfg.set("hpRegenBase", sp.getBaseHpRegen()); } catch (Throwable ignored) {}
        cfg.set("damageResist", sp.getDamageResist());
        cfg.set("luck", sp.getLuck());
        // Persist unlocked progression elements
        cfg.set("unlockedAbilities", new java.util.ArrayList<>(sp.getUnlockedAbilities()));
        cfg.set("unlockedGlyphs", new java.util.ArrayList<>(sp.getUnlockedGlyphs()));
        // Persist ability levels map (progression)
        cfg.set("abilityLevels", sp.getAbilityLevelsMap());
        // Note: do NOT persist run-temporary lists such as "abilities" (current run loadout), weapons, skills, coins, kills
        try { cfg.save(f); } catch (IOException e) { plugin.getLogger().severe("Failed to persist player data for " + sp.getUuid() + ": " + e.getMessage()); }
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
     * QA helper: load the player's YAML and log a compact snapshot of persisted keys.
     */
    public void logPlayerData(java.util.UUID uuid) {
        if (uuid == null) return;
        try {
            File f = new File(playersDir, uuid.toString() + ".yml");
            if (!f.exists()) { plugin.getLogger().info("Player data not found for " + uuid); return; }
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            StringBuilder sb = new StringBuilder();
            sb.append("PlayerData[").append(uuid).append("]: ");
            sb.append("hpRegenBase=").append(cfg.getDouble("hpRegenBase", Double.NaN)).append(", ");
            sb.append("damageMult=").append(cfg.getDouble("damageMult", Double.NaN)).append(", ");
            sb.append("flatDamage=").append(cfg.getDouble("flatDamage", Double.NaN)).append(", ");
            sb.append("extraHearts=").append(cfg.getInt("extraHearts", -1)).append(", ");
            sb.append("igniteBonusTicks=").append(cfg.getInt("igniteBonusTicks", -1)).append(", ");
            sb.append("knockbackBonus=").append(cfg.getDouble("knockbackBonus", Double.NaN)).append(", ");
            sb.append("moveSpeedMult=").append(cfg.getDouble("moveSpeedMult", Double.NaN)).append(", ");
            java.util.List<String> abilities = cfg.getStringList("abilities");
            sb.append("abilities=").append(abilities == null ? "[]" : abilities.toString()).append(", ");
            java.util.List<String> uabs = cfg.getStringList("unlockedAbilities");
            sb.append("unlockedAbilities=").append(uabs == null ? "[]" : uabs.toString()).append(", ");
            sb.append("shieldCurrent=").append(cfg.getDouble("shieldCurrent", Double.NaN));
            plugin.getLogger().info(sb.toString());
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to log player data for " + uuid + ": ", t);
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
