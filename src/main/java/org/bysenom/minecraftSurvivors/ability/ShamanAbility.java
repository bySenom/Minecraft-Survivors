// java
// File: src/main/java/org/bysenom/minecraftSurvivors/ability/ShamanAbility.java
package org.bysenom.minecraftSurvivors.ability;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class ShamanAbility implements Ability {

    private final MinecraftSurvivors plugin;
    private final SpawnManager spawnManager;
    private final Random random = new Random();

    public ShamanAbility(MinecraftSurvivors plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public void tick(Player player, SurvivorPlayer sp) {
        // Gültigkeitsprüfungen: player muss existieren, online und valid sein.
        if (player == null || !player.isOnline() || !player.isValid()) return;

        Location playerLoc = player.getLocation();
        if (playerLoc.getWorld() == null) return;

        double radius = plugin.getConfigUtil().getDouble("shaman.radius", 10.0);
        List<LivingEntity> mobs = spawnManager.getNearbyWaveMobs(playerLoc, radius);

        boolean debug = plugin.getConfigUtil().getBoolean("debug.shaman-log", true);
        if (debug) plugin.getLogger().info("[ShamanAbility] tick for " + player.getName() + " — found mobs: " + mobs.size());

        if (mobs.isEmpty()) return;

        int baseStrikes = plugin.getConfigUtil().getInt("shaman.strikes-per-tick", 1);
        int strikes = baseStrikes + (sp != null ? sp.getBonusStrikes() : 0);
        double baseDamage = plugin.getConfigUtil().getDouble("shaman.base-damage", 6.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        // Berechne finalen Schaden: base * level + bonusDamage + flatDamage
        double damage = baseDamage * level;
        if (sp != null) {
            damage += sp.getBonusDamage();
            damage += sp.getFlatDamage();
        }

        for (int s = 0; s < strikes && !mobs.isEmpty(); s++) {
            LivingEntity target = mobs.get(random.nextInt(mobs.size()));
            if (target == null) continue;
            // Defensive: skip players (nur Mobs sollen Schaden erhalten)
            if (target instanceof Player) {
                mobs.remove(target);
                continue;
            }
            Location strikeLoc = target.getLocation();
            if (debug) plugin.getLogger().info("[ShamanAbility] striking at " + strikeLoc.getBlockX() + "," + strikeLoc.getBlockY() + "," + strikeLoc.getBlockZ()
                    + " target=" + target.getType() + " (mainThread=" + Bukkit.isPrimaryThread() + ")");

            // visueller Effekt und gezielter Schaden nur auf das Ziel (SpawnManager sorgt für Thread-Safety)
            spawnManager.strikeLightningAtTarget(target, damage, player);

            // optional: entferne getroffenen Mob aus Liste, damit nicht erneut getroffen wird
            mobs.remove(target);
        }
    }
}
