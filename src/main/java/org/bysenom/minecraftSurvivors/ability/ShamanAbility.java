// java
// File: src/main/java/org/bysenom/minecraftSurvivors/ability/ShamanAbility.java
package org.bysenom.minecraftSurvivors.ability;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.SpawnManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

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

        double baseRadius = plugin.getConfigUtil().getDouble("shaman.radius", 10.0);
        double radius = baseRadius * (1.0 + (sp != null ? sp.getRadiusMult() : 0.0));
        // Apply generic SIZE multiplier
        if (sp != null) radius *= (1.0 + sp.getEffectiveSizeMult());
        List<LivingEntity> mobs = spawnManager.getTargetsIncludingBoss(playerLoc, radius);

        boolean debug = plugin.getConfigUtil().getBoolean("debug.shaman-log", true);
        if (debug) plugin.getLogger().info("[ShamanAbility] tick for " + player.getName() + " — found mobs: " + mobs.size());

        if (mobs.isEmpty()) return;

        int baseStrikes = plugin.getConfigUtil().getInt("shaman.strikes-per-tick", 1);
        int strikes = baseStrikes + (sp != null ? sp.getBonusStrikes() : 0);
        // AttackSpeed scaling: ohne Cap
        double as = sp != null ? Math.max(0.0, sp.getAttackSpeedMult()) : 0.0;
        double speedFactor = 1.0 + as;
        strikes = Math.max(1, (int) Math.floor(strikes * speedFactor));

        double baseDamage = plugin.getConfigUtil().getDouble("shaman.base-damage", 6.0);
        int level = Math.max(1, sp != null ? sp.getClassLevel() : 1);

        // Berechne finalen Schaden: base * level + bonusDamage + flatDamage
        double damage = baseDamage * level;
        if (sp != null) {
            damage += sp.getDamageAddTotal();
            damage *= (1.0 + sp.getDamageMult());
        }

        // ambient spark ring around player
        try {
            if (sp == null || sp.isFxEnabled()) {
                int points = 18;
                for (int i = 0; i < points; i++) {
                    double ang = 2 * Math.PI * i / points;
                    double x = playerLoc.getX() + Math.cos(ang) * 0.8;
                    double z = playerLoc.getZ() + Math.sin(ang) * 0.8;
                    playerLoc.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, new Location(playerLoc.getWorld(), x, playerLoc.getY() + 1.2, z), 1, 0.02, 0.02, 0.02, 0.0);
                }
                player.playSound(playerLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.35f, 1.7f);
            }
        } catch (Throwable ignored) {}

        for (int s = 0; s < strikes && !mobs.isEmpty(); s++) {
            LivingEntity target = mobs.get(random.nextInt(mobs.size()));
            if (target == null) continue;
            // Defensive: skip players (nur Mobs sollen Schaden erhalten)
            if (target instanceof Player) {
                mobs.remove(target);
                continue;
            }
            Location strikeLoc = target.getLocation();

            // draw particle arc from player head to target
            try {
                if (sp == null || sp.isFxEnabled()) {
                    Location from = playerLoc.clone().add(0, 1.6, 0);
                    Location to = strikeLoc.clone().add(0, 1.0, 0);
                    org.bukkit.util.Vector dir = to.toVector().subtract(from.toVector());
                    int steps = Math.max(6, (int) Math.min(30, from.distance(to) * 4));
                    for (int i = 0; i <= steps; i++) {
                        double t = i / (double) steps;
                        org.bukkit.util.Vector pt = from.toVector().add(dir.clone().multiply(t));
                        Location pLoc = new Location(from.getWorld(), pt.getX(), pt.getY(), pt.getZ());
                        from.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, pLoc, 1, 0.01, 0.01, 0.01, 0.0);
                    }
                    player.playSound(playerLoc, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 2.0f);
                }
            } catch (Throwable ignored) {}

            // visueller Effekt und gezielter Schaden nur auf das Ziel (SpawnManager sorgt für Thread-Safety)
            try {
                // mark player with ability key for damage attribution
                try { player.setMetadata("ms_ability_key", new org.bukkit.metadata.FixedMetadataValue(plugin, "ab_lightning")); } catch (Throwable ignored) {}
                spawnManager.strikeLightningAtTarget(target, damage, player);
            } finally {
                // remove metadata next tick to avoid leaking
                try { org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { try { player.removeMetadata("ms_ability_key", plugin); } catch (Throwable ignored) {} }); } catch (Throwable ignored) {}
            }

            // optional: entferne getroffenen Mob aus Liste, damit nicht erneut getroffen wird
            mobs.remove(target);
        }
    }
}
