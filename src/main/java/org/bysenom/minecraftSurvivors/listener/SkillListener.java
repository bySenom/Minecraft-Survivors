package org.bysenom.minecraftSurvivors.listener;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SkillListener implements Listener {

    private final MinecraftSurvivors plugin;
    private final NamespacedKey shockwaveKey;
    private final java.util.Map<java.util.UUID, Long> shockwaveCd = new java.util.concurrent.ConcurrentHashMap<>();

    public SkillListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
        this.shockwaveKey = new NamespacedKey(plugin, "skill_shockwave");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        String mark = it.getItemMeta().getPersistentDataContainer().get(shockwaveKey, PersistentDataType.STRING);
        if (mark == null) {
            // try byte mark
            Byte b = it.getItemMeta().getPersistentDataContainer().get(shockwaveKey, PersistentDataType.BYTE);
            if (b == null || b == 0) return;
        }
        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        long last = shockwaveCd.getOrDefault(p.getUniqueId(), 0L);
        long cdMs = plugin.getConfigUtil().getInt("skills.shockwave.cooldown-ms", 800) ;
        if (now - last < cdMs) {
            long left = (cdMs - (now - last));
            org.bysenom.minecraftSurvivors.util.Msg.warn(p, "Shockwave CD: "+left+"ms");
            return;
        }
        shockwaveCd.put(p.getUniqueId(), now);
        // small AoE knockback/damage pulse
        double radius = plugin.getConfigUtil().getDouble("skills.shockwave.radius", 6.0);
        double damage = plugin.getConfigUtil().getDouble("skills.shockwave.damage", 2.0);
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = MinecraftSurvivors.getInstance().getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                org.bukkit.util.Vector v = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.25);
                le.setVelocity(v);
            } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0,1.0,0), 20, 1.0, 0.2, 1.0, 0.0);
            p.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f);
        } catch (Throwable ignored) {}
    }
}

