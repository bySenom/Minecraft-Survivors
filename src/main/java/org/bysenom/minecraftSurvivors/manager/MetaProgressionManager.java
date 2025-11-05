// File: src/main/java/org/bysenom/minecraftSurvivors/manager/MetaProgressionManager.java
package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.MetaProfile;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt und speichert Meta-Profile und stellt Utility-Methoden zur Verfügung.
 */
public class MetaProgressionManager {

    private final MinecraftSurvivors plugin;
    private final Map<java.util.UUID, MetaProfile> profiles = new ConcurrentHashMap<>();
    private File dir;

    public MetaProgressionManager(MinecraftSurvivors plugin) {
        this.plugin = plugin;
        ensureDir();
    }

    private void ensureDir() {
        this.dir = new File(plugin.getDataFolder(), "meta");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    public MetaProfile get(java.util.UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::load);
    }

    public void saveAll() {
        for (MetaProfile p : profiles.values()) save(p);
    }

    public MetaProfile load(java.util.UUID uuid) {
        if (uuid == null) return null;
        File f = new File(dir, uuid + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        MetaProfile mp = new MetaProfile(uuid);
        mp.setEssence(cfg.getInt("essence", 0));
        mp.setPermDamageMult(cfg.getDouble("perm.damage-mult", 0.0));
        mp.setPermHealthHearts(cfg.getInt("perm.health-hearts", 0));
        mp.setPermMoveSpeed(cfg.getDouble("perm.move-speed", 0.0));
        mp.setPermAttackSpeed(cfg.getDouble("perm.attack-speed", 0.0));
        mp.setPermResist(cfg.getDouble("perm.resist", 0.0));
        mp.setPermLuck(cfg.getDouble("perm.luck", 0.0));
        mp.setPermSkillSlots(cfg.getInt("perm.skill-slots", 0));
        // Wichtig: während computeIfAbsent() nicht erneut in 'profiles' schreiben!
        return mp;
    }

    public void save(MetaProfile mp) {
        if (mp == null) return;
        File f = new File(dir, mp.getUuid() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("essence", mp.getEssence());
        cfg.set("perm.damage-mult", mp.getPermDamageMult());
        cfg.set("perm.health-hearts", mp.getPermHealthHearts());
        cfg.set("perm.move-speed", mp.getPermMoveSpeed());
        cfg.set("perm.attack-speed", mp.getPermAttackSpeed());
        cfg.set("perm.resist", mp.getPermResist());
        cfg.set("perm.luck", mp.getPermLuck());
        cfg.set("perm.skill-slots", mp.getPermSkillSlots());
        try { cfg.save(f); } catch (IOException e) { plugin.getLogger().severe("Failed to save MetaProfile: "+e.getMessage()); }
    }

    /**
     * Award essence on run end (e.g., per minute survived or kills). Config-driven.
     */
    public void awardEndOfRunEssence(org.bukkit.entity.Player bukkitPlayer, SurvivorPlayer sp, int minutesSurvived) {
        if (bukkitPlayer == null) return;
        MetaProfile mp = get(bukkitPlayer.getUniqueId());
        int perMinute = plugin.getConfigUtil().getInt("meta.endrun.essence-per-minute", 2);
        int perKill = plugin.getConfigUtil().getInt("meta.endrun.essence-per-kill", 0);
        int total = Math.max(0, minutesSurvived * perMinute) + Math.max(0, (sp!=null?sp.getKills():0) * perKill);
        if (total > 0) {
            mp.addEssence(total);
            bukkitPlayer.sendMessage("§d+"+total+" Essence (Meta)");
        }
        save(mp);
    }

    /**
     * Apply meta bonuses to survivor at run start.
     */
    public void applyMetaOnRunStart(org.bukkit.entity.Player p, SurvivorPlayer sp) {
        if (p == null || sp == null) return;
        MetaProfile mp = get(p.getUniqueId());
        mp.applyTo(sp, p);
    }

    // Simple pricing helper with caps enforced
    public boolean tryPurchase(org.bukkit.entity.Player p, String key) {
        if (p == null || key == null) return false;
        MetaProfile mp = get(p.getUniqueId());
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        java.util.Map<String, Object> node = null;
        java.util.List<java.util.Map<?, ?>> list = cfg.getMapList("meta.shop");
        for (java.util.Map<?, ?> m : list) {
            Object idObj = m.containsKey("key") ? m.get("key") : "";
            String id = String.valueOf(idObj);
            if (key.equalsIgnoreCase(id)) { try { node = (java.util.Map<String, Object>) m; } catch (Throwable ignored) {} break; }
        }
        if (node == null) { p.sendMessage("§cUnbekannter Meta-Shop-Eintrag: "+key); return false; }
        int price = Integer.parseInt(String.valueOf(node.getOrDefault("price", 10)));
        String type = String.valueOf(node.getOrDefault("type", ""));
        double stepD = Double.parseDouble(String.valueOf(node.getOrDefault("step", 0.01)));
        double capD = Double.parseDouble(String.valueOf(node.getOrDefault("cap", 0.50)));
        if (mp.getEssence() < price) { p.sendMessage("§cNicht genug Essence."); return false; }
        boolean ok = false;
        switch (type.toUpperCase()) {
            case "DAMAGE_MULT":
                if (mp.getPermDamageMult() + stepD <= capD+1e-9) { mp.addPermDamageMult(stepD); ok = true; }
                break;
            case "MOVE_SPEED":
                if (mp.getPermMoveSpeed() + stepD <= capD+1e-9) { mp.addPermMoveSpeed(stepD); ok = true; }
                break;
            case "ATTACK_SPEED":
                if (mp.getPermAttackSpeed() + stepD <= capD+1e-9) { mp.addPermAttackSpeed(stepD); ok = true; }
                break;
            case "RESIST":
                if (mp.getPermResist() + stepD <= capD+1e-9) { mp.addPermResist(stepD); ok = true; }
                break;
            case "LUCK":
                if (mp.getPermLuck() + stepD <= capD+1e-9) { mp.addPermLuck(stepD); ok = true; }
                break;
            case "HEALTH_HEARTS":
                int heartsCap = (int) Math.round(capD);
                int stepHearts = (int) Math.round(stepD);
                if (mp.getPermHealthHearts() + stepHearts <= heartsCap) { mp.addPermHealthHearts(stepHearts); ok = true; }
                break;
            case "SKILL_SLOT":
                int slotStep = (int) Math.round(stepD);
                int slotCap = (int) Math.round(capD);
                if (mp.getPermSkillSlots() + slotStep <= slotCap) { mp.addPermSkillSlots(slotStep); ok = true; }
                break;
            default:
                p.sendMessage("§cUnbekannter Typ: "+type);
                return false;
        }
        if (!ok) { p.sendMessage("§eCap erreicht."); return false; }
        mp.setEssence(mp.getEssence() - price);
        save(mp);
        try { p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f); } catch (Throwable ignored) {}
        p.sendMessage("§dMeta gekauft: "+type+" §7(+"+stepD+")");
        return true;
    }
}
