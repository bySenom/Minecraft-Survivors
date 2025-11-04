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

        plugin.getLogger().info("[ShamanAbility] tick for " + player.getName() + " — found mobs: " + mobs.size());

        if (mobs.isEmpty()) return;

        int strikes = plugin.getConfigUtil().getInt("shaman.strikes-per-tick", 1);
        double baseDamage = plugin.getConfigUtil().getDouble("shaman.base-damage", 6.0);
        int level = Math.max(1, sp.getClassLevel());
        double damage = baseDamage * level;

        for (int s = 0; s < strikes && !mobs.isEmpty(); s++) {
            LivingEntity target = mobs.get(random.nextInt(mobs.size()));
            if (target == null) continue;
            Location strikeLoc = target.getLocation();
            plugin.getLogger().info("[ShamanAbility] striking at " + strikeLoc.getBlockX() + "," + strikeLoc.getBlockY() + "," + strikeLoc.getBlockZ()
                    + " target=" + target.getType() + " (mainThread=" + Bukkit.isPrimaryThread() + ")");

            // visueller Effekt und gezielter Schaden am Mob (keine Spieler durch Target-Liste)
            spawnManager.strikeLightningEffectSafe(strikeLoc);
            try {
                target.damage(damage, player);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to apply shaman damage: " + ex.getMessage());
            }
            // optional: entferne getroffenen Mob aus Liste, damit nicht erneut getroffen wird
            mobs.remove(target);
        }
    }
}
