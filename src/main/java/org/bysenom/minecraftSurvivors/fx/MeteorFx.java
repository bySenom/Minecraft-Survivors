package org.bysenom.minecraftSurvivors.fx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class MeteorFx {
    private MeteorFx() {}

    public static void spawnMeteor(MinecraftSurvivors plugin, Location target, double height, double speed, double radius, double damage, Player source) {
        if (plugin == null || target == null || target.getWorld() == null) return;
        World w = target.getWorld();
        Location start = target.clone().add(0, Math.max(8.0, height), 0);
        // Visual entity: invisible marker armor stand with magma block helmet
        ArmorStand stand = null;
        try {
            stand = w.spawn(start, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false); // we animate manually for precision
                as.setSmall(false);
                try { as.setInvulnerable(true); } catch (Throwable ignored) {}
                try { as.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.MAGMA_BLOCK)); } catch (Throwable ignored) {}
                try { as.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING); } catch (Throwable ignored) {}
            });
        } catch (Throwable t) {
            org.bysenom.minecraftSurvivors.util.LogUtil.logFine("MeteorFx: failed to spawn armor stand: ", t);
        }
        final ArmorStand meteor = stand;
        final Location dest = target.clone();
        final int maxTicks = 200; // safety
        new BukkitRunnable() {
            int t = 0;
            Location cur = start.clone();
            @Override public void run() {
                try {
                    if (t++ > maxTicks || meteor != null && meteor.isDead()) { cleanup(); return; }
                    // Move towards dest
                    double dy = Math.max(0.2, speed);
                    cur.subtract(0, dy, 0);
                    if (meteor != null) meteor.teleport(cur);
                    // trail
                    w.spawnParticle(Particle.LARGE_SMOKE, cur, 4, 0.15, 0.15, 0.15, 0.01);
                    w.spawnParticle(Particle.FLAME, cur, 4, 0.15, 0.15, 0.15, 0.01);
                    // impact check
                    if (cur.getY() <= dest.getY() + 0.5) {
                        impact();
                        cleanup();
                    }
                } catch (Throwable t) {
                    org.bysenom.minecraftSurvivors.util.LogUtil.logFine("MeteorFx tick failed: ", t);
                    cleanup();
                }
            }
            private void impact() {
                try {
                    // Use a widely supported explosion particle
                    try { w.spawnParticle(Particle.valueOf("EXPLOSION_HUGE"), dest, 1); }
                    catch (Throwable ignored) { w.spawnParticle(Particle.EXPLOSION, dest, 1); }
                    w.spawnParticle(Particle.LAVA, dest, 16, 0.6, 0.4, 0.6, 0.02);
                    w.spawnParticle(Particle.CRIT, dest, 24, 0.8, 0.6, 0.8, 0.02);
                    w.playSound(dest, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                } catch (Throwable ignored) {}
                // optional AoE damage
                boolean dmgEnabled = plugin.getConfigUtil().getBoolean("visuals.meteor.damage-enabled", false);
                double rad = Math.max(0.5, radius);
                if (dmgEnabled && damage > 0.0) {
                    for (LivingEntity le : w.getEntitiesByClass(LivingEntity.class)) {
                        if (le.isDead() || le.getWorld() != w) continue;
                        if (le.getLocation().distanceSquared(dest) <= rad*rad) {
                            try { org.bysenom.minecraftSurvivors.util.DamageUtil.damageWithAttributionNullable(plugin, source, le, damage, source == null ? "meteor" : "ab_lightning:genkidama"); } catch (Throwable ignored) {}
                        }
                    }
                }
            }
            private void cleanup() {
                try { if (meteor != null) meteor.remove(); } catch (Throwable ignored) {}
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
