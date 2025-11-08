package org.bysenom.lobby.npc;

import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PlayerNpcRegistry {
    public static class Entry {
        public String name;
        public Location loc;
        public boolean lookAt = true;
        public String command = "/lobby";
        public org.bukkit.entity.Villager villager; // Villager als NPC
    }

    private final org.bysenom.lobby.LobbySystem plugin;
    private final List<Entry> entries = new ArrayList<>();
    private org.bukkit.scheduler.BukkitTask rotationTask;

    public PlayerNpcRegistry(org.bysenom.lobby.LobbySystem plugin) { this.plugin = plugin; }

    public void loadFromConfig() {
        entries.clear();
        if (plugin.getConfig().getBoolean("npc.enabled", false)) {
            String world = plugin.getConfig().getString("npc.world", "");
            World w = (world == null || world.isEmpty()) ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(world);
            if (w != null) {
                Entry e = new Entry();
                e.name = plugin.getConfig().getString("npc.mode-name", "Minecraft Survivors");
                e.lookAt = plugin.getConfig().getBoolean("npc.look-at-player", true);
                double x = plugin.getConfig().getDouble("npc.x", 0.0);
                double y = plugin.getConfig().getDouble("npc.y", 0.0);
                double z = plugin.getConfig().getDouble("npc.z", 0.0);
                float yaw = (float) plugin.getConfig().getDouble("npc.yaw", 0.0);
                e.loc = new Location(w, x, y, z, yaw, 0.0f);
                entries.add(e);
            }
        }
        ConfigurationSection listSec = plugin.getConfig().getConfigurationSection("npcs");
        if (listSec != null) {
            for (String key : listSec.getKeys(false)) {
                ConfigurationSection s = listSec.getConfigurationSection(key);
                if (s == null) continue;
                String world = s.getString("world", "");
                World w = (world == null || world.isEmpty()) ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(world);
                if (w == null) continue;
                Entry e = new Entry();
                e.name = s.getString("name", "Minecraft Survivors");
                e.lookAt = s.getBoolean("lookAt", true);
                e.command = s.getString("command", "/lobby");
                double x = s.getDouble("x", 0.0);
                double y = s.getDouble("y", 0.0);
                double z = s.getDouble("z", 0.0);
                float yaw = (float) s.getDouble("yaw", 0.0);
                e.loc = new Location(w, x, y, z, yaw, 0.0f);
                entries.add(e);
            }
        }
    }

    public void spawnAllForOnlineViewers() {
        for (Entry e : entries) {
            try {
                org.bukkit.World w = e.loc.getWorld();
                e.villager = (org.bukkit.entity.Villager) w.spawnEntity(e.loc, org.bukkit.entity.EntityType.VILLAGER);
                e.villager.setAI(false);
                e.villager.setInvulnerable(true);
                e.villager.setSilent(true);
                e.villager.setPersistent(true);
                try { e.villager.setCollidable(false); } catch (Throwable ignored) {}
                e.villager.customName(net.kyori.adventure.text.Component.text(e.name));
                e.villager.setCustomNameVisible(true);
                try { e.villager.setProfession(org.bukkit.entity.Villager.Profession.NITWIT); } catch (Throwable ignored) {}
            } catch (Throwable t) {
                plugin.getLogger().warning("Villager-NPC spawn failed: " + t.getMessage());
            }
        }
        startRotationLoop();
    }

    public void showAllTo(Player viewer) { /* Villager sind global sichtbar – nichts nötig */ }
    public void hideAllFrom(Player viewer) { /* Einzelverstecken nicht nötig */ }

    private void startRotationLoop() {
        if (rotationTask != null) rotationTask.cancel();
        rotationTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                for (Entry e : entries) {
                    if (!e.lookAt) continue;
                    org.bukkit.Location baseLoc = e.loc;
                    org.bukkit.entity.Player target = nearestPlayer(baseLoc);
                    if (target == null || e.villager == null || e.villager.isDead()) continue;
                    double dx = target.getLocation().getX() - baseLoc.getX();
                    double dz = target.getLocation().getZ() - baseLoc.getZ();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    org.bukkit.Location newLoc = baseLoc.clone();
                    newLoc.setYaw(yaw);
                    e.villager.teleport(newLoc);
                }
            } catch (Throwable ignored) {}
        }, 20L, 20L);
    }

    private Player nearestPlayer(org.bukkit.Location loc) {
        Player nearest = null; double best = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    public void hideAll() {
        for (Entry e : entries) {
            if (e.villager != null && !e.villager.isDead()) {
                e.villager.remove();
                e.villager = null;
            }
        }
        if (rotationTask != null) { rotationTask.cancel(); rotationTask = null; }
    }

    public boolean handleNpcClick(org.bukkit.entity.Entity entity, org.bukkit.entity.Player p) {
        for (Entry e : entries) {
            if (e.villager != null && e.villager.getUniqueId().equals(entity.getUniqueId())) {
                try {
                    if (e.command != null && !e.command.isEmpty()) {
                        if (e.command.startsWith("/")) p.performCommand(e.command.substring(1)); else p.performCommand(e.command);
                    }
                } catch (Throwable ignored) {}
                return true;
            }
        }
        return false;
    }

    // Neu: Unterstützung für PacketInterceptor – Klick anhand EntityId verarbeiten
    public boolean handleClick(org.bukkit.entity.Player p, int entityId) {
        for (Entry e : entries) {
            if (e.villager != null && e.villager.getEntityId() == entityId) {
                try {
                    if (e.command != null && !e.command.isEmpty()) {
                        if (e.command.startsWith("/")) p.performCommand(e.command.substring(1)); else p.performCommand(e.command);
                    }
                } catch (Throwable ignored) {}
                return true;
            }
        }
        return false;
    }

    // Liefert mutable Entry (nur intern verwenden) oder null
    public Entry getEntry(int index) {
        if (index < 0 || index >= entries.size()) return null;
        return entries.get(index);
    }

    public int count() { return entries.size(); }

    // Speichert eine Entry zurück in die Config unter gegebener Index-Position
    public void saveNpcConfig(int index, Entry e) {
        String base = "npcs." + index;
        plugin.getConfig().set(base + ".name", e.name);
        plugin.getConfig().set(base + ".lookAt", e.lookAt);
        plugin.getConfig().set(base + ".command", e.command);
        plugin.getConfig().set(base + ".world", e.loc.getWorld().getName());
        plugin.getConfig().set(base + ".x", e.loc.getX());
        plugin.getConfig().set(base + ".y", e.loc.getY());
        plugin.getConfig().set(base + ".z", e.loc.getZ());
        plugin.getConfig().set(base + ".yaw", e.loc.getYaw());
    }

    public void respawnAll() {
        hideAll();
        loadFromConfig();
        spawnAllForOnlineViewers();
    }

    public List<Entry> entries() { return Collections.unmodifiableList(entries); }
}
