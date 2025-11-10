package org.bysenom.minecraftSurvivors.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public final class CombatEngine {
    private CombatEngine() {}

    public static void handleDamageEvent(MinecraftSurvivors plugin, EntityDamageByEntityEvent e) {
        // Block input damage to paused/protected players
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            try {
                if (plugin.getGameManager() != null) {
                    if (plugin.getGameManager().isPlayerPaused(victim.getUniqueId())) { e.setCancelled(true); return; }
                    if (plugin.getGameManager().isPlayerTemporarilyProtected(victim.getUniqueId())) { e.setCancelled(true); return; }
                }
            } catch (Throwable ignored) {}
        }

        Player damagerPlayer = null;
        if (e.getDamager() instanceof Player) {
            damagerPlayer = (Player) e.getDamager();
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            Object shooter = proj.getShooter();
            if (shooter instanceof Player) damagerPlayer = (Player) shooter;
        }
        // Optional: block outgoing damage from paused players (keine Aktionen während Auswahl)
        if (damagerPlayer != null) {
            try { if (plugin.getGameManager() != null && plugin.getGameManager().isPlayerPaused(damagerPlayer.getUniqueId())) { e.setCancelled(true); return; } } catch (Throwable ignored) {}
        }

        // --- Offensiv: Crit & Lifesteal für Spieler als Angreifer ---
        if (damagerPlayer != null) {
            var sp = plugin.getPlayerManager().get(damagerPlayer.getUniqueId());
            if (sp != null) {
                // Crit roll
                try {
                    double critChance = sp.getCritChance();
                    if (critChance > 0.0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < Math.min(1.0, critChance)) {
                        double mult = 1.0 + Math.max(0.0, sp.getCritDamage());
                        e.setDamage(e.getDamage() * mult);
                        try { damagerPlayer.playSound(damagerPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.2f); } catch (Throwable ignored) {}
                        try { damagerPlayer.spawnParticle(org.bukkit.Particle.CRIT, damagerPlayer.getLocation().add(0,1.2,0), 12, 0.25,0.2,0.25, 0.02); } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
                // Lifesteal: heal nach finaler Schadensberechnung approximieren (merken)
                try { damagerPlayer.setMetadata("ms_last_hit_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, Math.max(0.0, e.getDamage()))); } catch (Throwable ignored) {}
                // Bonus-Damage gg. Elite/Boss nach Crit
                try {
                    if (e.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                        boolean elite = target.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "ms_elite"), org.bukkit.persistence.PersistentDataType.BYTE);
                        boolean boss = target.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "ms_boss"), org.bukkit.persistence.PersistentDataType.BYTE);
                        if (elite || boss) {
                            double bonus = sp.getDamageEliteBoss();
                            if (bonus > 0.0) e.setDamage(e.getDamage() * (1.0 + Math.min(3.0, bonus))); // cap x4 total
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // --- Defensiv: Evasion/Shield/Armor/Thorns für Spieler als Opfer ---
        if (e.getEntity() instanceof Player victimPl && e.getEntity() instanceof LivingEntity) {
            var sp = plugin.getPlayerManager().get(victimPl.getUniqueId());
            if (sp != null) {
                // Evasion roll: full dodge
                try {
                    double eva = sp.getEvasion();
                    if (eva > 0.0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < Math.min(0.95, eva)) {
                        e.setCancelled(true);
                        try { victimPl.playSound(victimPl.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 2.0f);} catch (Throwable ignored) {}
                        try { victimPl.spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, victimPl.getLocation().add(0,1.0,0), 6, 0.2,0.1,0.2, 0.01);} catch (Throwable ignored) {}
                        return;
                    }
                } catch (Throwable ignored) {}

                // mark damaged (shield regen gate)
                sp.markDamagedNow();

                // Shield absorbs first
                double dmg = Math.max(0.0, e.getDamage());
                try {
                    double curShield = sp.getShieldCurrent();
                    double maxShield = sp.getShieldMax();
                    if (maxShield < curShield) sp.setShieldCurrent(Math.max(0.0, maxShield));
                    if (curShield > 0.0) {
                        double remain = curShield - dmg;
                        if (remain >= 0.0) {
                            sp.setShieldCurrent(remain);
                            // fully absorbed
                            e.setCancelled(true);
                            try { victimPl.playSound(victimPl.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BLOCK, 0.7f, 1.2f);} catch (Throwable ignored) {}
                            try { victimPl.spawnParticle(org.bukkit.Particle.END_ROD, victimPl.getLocation().add(0,1.0,0), 8, 0.2,0.2,0.2, 0.0);} catch (Throwable ignored) {}
                            return;
                        } else {
                            // deplete shield, remaining damage hits HP
                            sp.setShieldCurrent(0.0);
                            dmg = -remain; // remain is negative => leftover damage
                        }
                    }
                } catch (Throwable ignored) {}

                // Armor reduction (after shield)
                try {
                    double armor = sp.getArmor();
                    if (armor > 0.0) {
                        double red = Math.max(0.0, Math.min(0.9, armor));
                        double newDmg = Math.max(0.0, Math.max(0.0, e.getDamage()) * (1.0 - red));
                        e.setDamage(newDmg);
                    }
                } catch (Throwable ignored) {}

                // Thorns reflect
                try {
                    double th = sp.getThorns();
                    if (th > 0.0 && e.getDamager() instanceof LivingEntity le && !(e.getDamager() instanceof Player)) {
                        double ref = Math.max(0.0, Math.max(0.0, e.getDamage()) * Math.min(1.5, th));
                        if (ref > 0.0) {
                            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                try { le.damage(ref, victimPl); } catch (Throwable ignored) {}
                                try { le.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, le.getLocation().add(0,1.0,0), 6, 0.2,0.2,0.2, 0.01);} catch (Throwable ignored) {}
                            });
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Stats Meter & Holo HP for victim entity
        if (damagerPlayer != null && e.getEntity() instanceof LivingEntity) {
            double amount = Math.max(0.0, e.getFinalDamage());
            if (amount > 0) {
                try { plugin.getStatsMeterManager().recordDamage(damagerPlayer.getUniqueId(), amount); } catch (Throwable ignored) {}
                try {
                    LivingEntity le = (LivingEntity) e.getEntity();
                    double hp = Math.max(0.0, le.getHealth() - amount);
                    double max = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null ? le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue() : Math.max(1.0, le.getHealth());
                    plugin.getHoloHpManager().updateBar(le, hp, max);
                } catch (Throwable ignored) {}
                // New: Round stats recording
                try {
                    var rsm = plugin.getRoundStatsManager();
                    if (rsm != null) {
                        String src = "unknown";
                        // Prefer explicit metadata set on the projectile if present (e.g., ability projectiles)
                        try {
                            if (e.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.hasMetadata("ms_ability_key")) {
                                var mv = proj.getMetadata("ms_ability_key"); if (!mv.isEmpty()) src = String.valueOf(mv.get(0).value());
                            } else if (damagerPlayer.hasMetadata("ms_ability_key")) {
                                var mv = damagerPlayer.getMetadata("ms_ability_key"); if (!mv.isEmpty()) src = String.valueOf(mv.get(0).value());
                            }
                        } catch (Throwable ignored) {}
                        // fallback: held item or weapon
                        if ("unknown".equals(src)) {
                            try {
                                var it = damagerPlayer.getInventory().getItemInMainHand();
                                if (it != null && it.getType() != org.bukkit.Material.AIR) src = "item:" + it.getType().name();
                                else src = "player:melee";
                            } catch (Throwable ignored) { src = "player:melee"; }
                        }
                        rsm.recordDamage(damagerPlayer.getUniqueId(), src, amount);
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Lifesteal heal application (post-event approx)
        if (damagerPlayer != null) {
            var sp = plugin.getPlayerManager().get(damagerPlayer.getUniqueId());
            if (sp != null) {
                // Enhanced knockback optional
                try {
                    if (e.getEntity() instanceof org.bukkit.entity.LivingEntity victim && !e.isCancelled()) {
                        double kb = sp.getKnockbackEffective();
                        if (kb > 0.0) {
                            org.bukkit.util.Vector dir = victim.getLocation().toVector().subtract(damagerPlayer.getLocation().toVector()).normalize();
                            victim.setVelocity(dir.multiply(Math.min(4.0, 0.4 + kb)).setY(0.35 + Math.min(0.8, kb*0.15)));
                        }
                    }
                } catch (Throwable ignored) {}
                double ls = sp.getLifesteal();
                if (ls > 0.0) {
                    double dealt = 0.0;
                    try {
                        if (damagerPlayer.hasMetadata("ms_last_hit_damage")) {
                            java.util.List<org.bukkit.metadata.MetadataValue> mv = damagerPlayer.getMetadata("ms_last_hit_damage");
                            for (org.bukkit.metadata.MetadataValue v : mv) { if (v.getOwningPlugin() == plugin) { dealt = Math.max(dealt, v.asDouble()); } }
                        }
                    } catch (Throwable ignored) {}
                    if (dealt > 0.0) {
                        final double healAmount = Math.max(0.0, dealt * Math.min(1.0, ls));
                        final org.bukkit.entity.Player dp = damagerPlayer;
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            try {
                                double max = dp.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null ? dp.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue() : 20.0;
                                dp.setHealth(Math.min(max, dp.getHealth() + healAmount));
                                dp.spawnParticle(org.bukkit.Particle.HEART, dp.getLocation().add(0,1.2,0), 2, 0.15,0.15,0.15, 0.01);
                            } catch (Throwable ignored) {}
                        });
                    }
                }
            }
        }
    }
}
