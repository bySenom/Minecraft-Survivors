package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShopNpcManager {
    private final MinecraftSurvivors plugin;
    private final org.bysenom.minecraftSurvivors.gui.GuiManager gui;
    private final NamespacedKey key;
    private final Set<java.util.UUID> npcs = new HashSet<>();
    private final Map<java.util.UUID, java.util.UUID> holoByNpc = new HashMap<>();

    public ShopNpcManager(MinecraftSurvivors plugin, org.bysenom.minecraftSurvivors.gui.GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
        this.key = new NamespacedKey(plugin, "ms_shop_npc");
    }

    public void spawnConfigured() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        String mode = cfg.getString("shop.npc.spawn-mode", "worldspawn").toLowerCase();
        Location loc = null;
        switch (mode) {
            case "first-player":
                org.bukkit.entity.Player first = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                if (first != null) loc = first.getLocation().clone().add(1, 0, 0);
                break;
            case "config":
                String worldName = cfg.getString("shop.npc.world", "world");
                org.bukkit.World w = Bukkit.getWorld(worldName);
                double x = cfg.getDouble("shop.npc.x", 0.0);
                double y = cfg.getDouble("shop.npc.y", 64.0);
                double z = cfg.getDouble("shop.npc.z", 0.0);
                float yaw = (float) cfg.getDouble("shop.npc.yaw", 0.0);
                float pitch = (float) cfg.getDouble("shop.npc.pitch", 0.0);
                if (w != null) loc = new Location(w, x, y, z, yaw, pitch);
                break;
            default:
                org.bukkit.World w0 = Bukkit.getWorlds().isEmpty()?null:Bukkit.getWorlds().get(0);
                if (w0 != null) loc = w0.getSpawnLocation().clone().add(1, 0, 0);
        }
        if (loc == null) return;
        spawnAt(loc);
    }

    public void spawnAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        org.bukkit.entity.Villager v = (org.bukkit.entity.Villager) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.VILLAGER);
        v.setAI(false);
        v.setInvulnerable(true);
        try {
            // Name via Adventure
            String display = plugin.getConfigUtil().getString("shop.npc.name", "Händler");
            v.customName(net.kyori.adventure.text.Component.text(display).color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            v.setCustomNameVisible(true);
        } catch (Throwable ignored) {}
        try {
            String type = plugin.getConfigUtil().getString("shop.npc.type", "plains").toLowerCase();
            String prof = plugin.getConfigUtil().getString("shop.npc.profession", "armorer").toLowerCase();
            org.bukkit.NamespacedKey tKey = org.bukkit.NamespacedKey.minecraft(type);
            org.bukkit.NamespacedKey pKey = org.bukkit.NamespacedKey.minecraft(prof);
            org.bukkit.entity.Villager.Type vType = org.bukkit.Registry.VILLAGER_TYPE.get(tKey);
            org.bukkit.entity.Villager.Profession vProf = org.bukkit.Registry.VILLAGER_PROFESSION.get(pKey);
            if (vType != null) v.setVillagerType(vType);
            if (vProf != null) v.setProfession(vProf);
        } catch (Throwable ignored) {}
        v.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
        npcs.add(v.getUniqueId());
        // Hologramm/ArmorStand Label
        try {
            org.bukkit.entity.ArmorStand as = (org.bukkit.entity.ArmorStand) v.getWorld().spawnEntity(v.getLocation().add(0, 1.8, 0), org.bukkit.entity.EntityType.ARMOR_STAND);
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.customName(net.kyori.adventure.text.Component.text("Klicke zum Shop").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            as.setCustomNameVisible(true);
            holoByNpc.put(v.getUniqueId(), as.getUniqueId());
        } catch (Throwable ignored) {}
    }

    public boolean isShopNpc(org.bukkit.entity.Entity e) {
        if (e == null) return false;
        try { return e.getPersistentDataContainer().has(key, PersistentDataType.BYTE); } catch (Throwable ignored) {}
        return false;
    }

    public void openShop(org.bukkit.entity.Player p) {
        if (p == null) return;
        gui.openShop(p);
    }

    public void despawnAll() {
        for (java.util.UUID id : new java.util.HashSet<>(npcs)) {
            try {
                org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
                java.util.UUID h = holoByNpc.remove(id);
                if (h != null) { org.bukkit.entity.Entity he = Bukkit.getEntity(h); if (he != null) he.remove(); }
            } catch (Throwable ignored) {}
        }
        npcs.clear();
    }
}
