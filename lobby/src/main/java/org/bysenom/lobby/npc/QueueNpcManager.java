package org.bysenom.lobby.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bysenom.lobby.LobbySystem;

public class QueueNpcManager {
    private final LobbySystem plugin;
    private ArmorStand npc;
    private PlayerNpc playerNpc;

    public QueueNpcManager(LobbySystem plugin) { this.plugin = plugin; }

    public void spawnFromConfig() {
        if (!plugin.getConfig().getBoolean("npc.enabled", true)) return;
        String type = plugin.getConfig().getString("npc.type", "armorstand");
        String worldName = plugin.getConfig().getString("npc.world", "");
        org.bukkit.World w = (worldName == null || worldName.isEmpty()) ? org.bukkit.Bukkit.getWorlds().get(0) : org.bukkit.Bukkit.getWorld(worldName);
        if (w == null) return;
        double x = plugin.getConfig().getDouble("npc.x", 0.0);
        double y = plugin.getConfig().getDouble("npc.y", 0.0);
        double z = plugin.getConfig().getDouble("npc.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("npc.yaw", 0.0);
        org.bukkit.Location loc = new org.bukkit.Location(w, x, y, z, yaw, 0.0f);
        if ("player".equalsIgnoreCase(type)) {
            // Player-NPC Stub (Benötigt Packet-Library für echte Fake-Spieler). Fallback auf ArmorStand.
            plugin.getLogger().warning("npc.type=player erfordert externe NPC-Implementation. Fallback auf ArmorStand.");
            spawnArmorStand(loc);
        } else {
            spawnArmorStand(loc);
        }
    }

    public void spawnAt(org.bukkit.Location loc) { spawnArmorStand(loc); }

    private void spawnArmorStand(org.bukkit.Location loc) {
        remove();
        npc = (org.bukkit.entity.ArmorStand) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ARMOR_STAND);
        npc.setInvisible(false);
        npc.setMarker(false);
        npc.setGravity(false);
        npc.setInvulnerable(true);
        npc.setSmall(false);
        npc.setArms(true);
        npc.setBasePlate(false);
        npc.customName(net.kyori.adventure.text.Component.text(plugin.getConfig().getString("npc.mode-name", "Minecraft Survivors")));
        npc.setCustomNameVisible(true);
        applyStoredSkin();
        applyArmor();
        plugin.getLogger().info("Queue NPC (ArmorStand) spawned at " + loc);
    }

    private void applyArmor() {
        try {
            if (!plugin.getConfig().getBoolean("npc.armor-enabled", true)) return;
            org.bukkit.inventory.EntityEquipment eq = npc.getEquipment();
            if (eq == null) return;
            eq.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE));
            eq.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS));
            eq.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS));
        } catch (Throwable ignored) {}
    }

    private void applyStoredSkin() {
        try {
            String skinPlayer = plugin.getConfig().getString("npc.skin-player", "");
            if (skinPlayer == null || skinPlayer.isBlank()) return;
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(skinPlayer);
            if (off == null) return;
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(off);
                head.setItemMeta(meta);
                npc.getEquipment().setHelmet(head);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Skin konnte nicht gesetzt werden: " + t.getMessage());
        }
    }

    public void setSkin(String playerName) {
        plugin.getConfig().set("npc.skin-player", playerName);
        plugin.saveConfig();
        applyStoredSkin();
    }

    public void remove() {
        broadcastPlayerNpcHide();
        if (npc != null && !npc.isDead()) npc.remove();
        npc = null;
    }

    public void tickLookAt() {
        if (npc == null) return;
        if (!plugin.getConfig().getBoolean("npc.look-at-player", true)) return;
        Player nearest = null; double best = 999999.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(npc.getWorld())) continue;
            double d = p.getLocation().distanceSquared(npc.getLocation());
            if (d < best) { best = d; nearest = p; }
        }
        if (nearest == null) return;
        Location eye = npc.getLocation();
        Vector dir = nearest.getLocation().toVector().subtract(eye.toVector());
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        eye.setYaw(yaw);
        npc.teleport(eye);
    }

    public boolean isNpc(org.bukkit.entity.Entity e) {
        return npc != null && e.getUniqueId().equals(npc.getUniqueId());
    }

    public ArmorStand getNpc() { return npc; }

    public boolean placePlayerNpc(org.bukkit.entity.Player admin) {
        try {
            org.bukkit.Location loc = admin.getLocation();
            String name = plugin.getConfig().getString("npc.mode-name", "Minecraft Survivors");
            String skin = plugin.getConfig().getString("npc.skin-player", "");
            this.playerNpc = PlayerNpc.create(loc, name, skin);
            // Zeige NPC allen aktuellen Spielern
            for (org.bukkit.entity.Player viewer : org.bukkit.Bukkit.getOnlinePlayers()) {
                playerNpc.showTo(viewer, true);
            }
            plugin.getLogger().info("Packet-basierter PlayerNpc erstellt (UUID=" + playerNpc.getUuid() + ")");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("PlayerNpc konnte nicht erstellt werden: " + t.getMessage());
            return false;
        }
    }

    public void broadcastPlayerNpcHide() {
        if (playerNpc == null) return;
        for (org.bukkit.entity.Player viewer : org.bukkit.Bukkit.getOnlinePlayers()) {
            playerNpc.hideFrom(viewer);
        }
        playerNpc = null;
    }
}
