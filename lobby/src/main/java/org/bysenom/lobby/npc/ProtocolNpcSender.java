package org.bysenom.lobby.npc;

import static com.comphenix.protocol.PacketType.Play.Server.*;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * NPC-Pakete via ProtocolLib (robuster gegenüber Mojang/Paper-Änderungen).
 */
public class ProtocolNpcSender {
    private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

    private PacketType resolveInfoPacketType() {
        try {
            Class<?> cls = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            try { return (PacketType) cls.getField("PLAYER_INFO_UPDATE").get(null); } catch (Throwable ignored) {}
            try { return (PacketType) cls.getField("PLAYER_INFO").get(null); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    private void writePlayerInfoActions(PacketContainer info) {
        try {
            // Neuer Weg (EnumSet)
            java.util.EnumSet<EnumWrappers.PlayerInfoAction> actions = java.util.EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            info.getPlayerInfoActions().write(0, actions);
            return;
        } catch (Throwable ignored) {}
        try {
            // Alter Weg (einzelne Action)
            info.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        } catch (Throwable ignored) {}
    }

    private void writePlayerInfoData(PacketContainer info, List<PlayerInfoData> list) {
        try {
            info.getPlayerInfoDataLists().write(0, list);
            return;
        } catch (Throwable ignored) {}
        try {
            // generischer Fallback auf List-Modifier falls verfügbar
            info.getSpecificModifier(List.class).write(0, list);
        } catch (Throwable ignored) {}
    }

    /**
     * Sichere Variante: sendet ADD_PLAYER + NAMED_ENTITY_SPAWN + HEAD_ROTATION + REMOVE_PLAYER (Tab) mit Delay.
     */
    public boolean showPlayerNpcSafe(Player viewer, UUID npcId, String name16, Location loc) {
        // Fail-safe für problematische Server-Builds
        String bv = Bukkit.getBukkitVersion();
        if (bv.startsWith("1.21.4")) {
            org.bysenom.lobby.LobbySystem.get().getLogger().warning("[LobbySystem] NPC spawn disabled on " + bv + " (ProtocolLib PlayerInfo mapping unstable). Using placeholder mode.");
            return false;
        }
        boolean okInfo = false;
        try {
            PacketType infoType = resolveInfoPacketType();
            if (infoType == null) throw new IllegalStateException("No PlayerInfo packet type available");
            PacketContainer info = pm.createPacket(infoType);
            writePlayerInfoActions(info);
            PlayerInfoData data = new PlayerInfoData(new WrappedGameProfile(npcId, name16), 0,
                    EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name16));
            writePlayerInfoData(info, java.util.Collections.singletonList(data));
            pm.sendServerPacket(viewer, info);
            okInfo = true;
        } catch (Throwable t) {
            try { viewer.sendMessage("§cNPC ADD_PLAYER fehlgeschlagen: " + t.getClass().getSimpleName()); } catch (Throwable ignored) {}
        }
        if (!okInfo) return false;
        // Verzögert spawnen, damit Client das Tab-Entry kennt
        Bukkit.getScheduler().runTaskLater(ProtocolLibrary.getPlugin(), () -> {
            try {
                boolean spawned = false;
                try { spawned = tryNamedEntitySpawn(viewer, npcId, loc); } catch (Throwable ignored) {}
                if (!spawned) {
                    tryGenericSpawnEntity(viewer, npcId, loc);
                }
                // Kopfrotation
                try {
                    PacketContainer head = pm.createPacket(ENTITY_HEAD_ROTATION);
                    if (!head.getIntegers().getValues().isEmpty()) head.getIntegers().write(0, getEntityIdFor(npcId));
                    if (!head.getBytes().getValues().isEmpty()) head.getBytes().write(0, yawToByte(loc.getYaw()));
                    pm.sendServerPacket(viewer, head);
                } catch (Throwable ignored) {}
                // Tab-Remove leicht verzögert
                Bukkit.getScheduler().runTaskLater(ProtocolLibrary.getPlugin(), () -> {
                    try {
                        PacketContainer remove = pm.createPacket(PLAYER_INFO_REMOVE);
                        if (!remove.getUUIDLists().getValues().isEmpty()) remove.getUUIDLists().write(0, java.util.Collections.singletonList(npcId));
                        pm.sendServerPacket(viewer, remove);
                    } catch (Throwable ignored) {}
                }, 10L);
            } catch (Throwable ignoredOuter) {}
        }, 2L);
        return true;
    }

    public void hidePlayerNpc(Player viewer, UUID npcId) {
        PacketContainer destroy = pm.createPacket(ENTITY_DESTROY);
        if (!destroy.getIntLists().getValues().isEmpty()) {
            destroy.getIntLists().write(0, Collections.singletonList(getEntityIdFor(npcId)));
        }
        pm.sendServerPacket(viewer, destroy);

        PacketContainer remove = pm.createPacket(PLAYER_INFO_REMOVE);
        if (!remove.getUUIDLists().getValues().isEmpty()) {
            remove.getUUIDLists().write(0, Collections.singletonList(npcId));
        }
        pm.sendServerPacket(viewer, remove);
    }

    @SuppressWarnings("deprecation")
    private boolean tryNamedEntitySpawn(Player viewer, UUID npcId, Location loc) {
        // Verwende NAMED_ENTITY_SPAWN (ProtocolLib-API), auch wenn als deprecated markiert – neuere Konstante fehlt teils
        PacketContainer spawn = pm.createPacket(NAMED_ENTITY_SPAWN);
        // UUID
        if (!spawn.getUUIDs().getValues().isEmpty()) {
            spawn.getUUIDs().write(0, npcId);
        } else {
            return false;
        }
        // EntityId (falls vorhanden)
        if (!spawn.getIntegers().getValues().isEmpty()) {
            spawn.getIntegers().write(0, getEntityIdFor(npcId));
        }
        // Position
        if (spawn.getDoubles().size() >= 3) {
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());
        }
        // Rotationen
        if (spawn.getBytes().size() >= 2) {
            spawn.getBytes().write(0, yawToByte(loc.getYaw()));
            spawn.getBytes().write(1, pitchToByte(loc.getPitch()));
        }
        pm.sendServerPacket(viewer, spawn);
        return true;
    }

    private void tryGenericSpawnEntity(Player viewer, UUID npcId, Location loc) {
        PacketContainer spawn = pm.createPacket(SPAWN_ENTITY);
        // EntityId
        if (!spawn.getIntegers().getValues().isEmpty()) {
            spawn.getIntegers().write(0, getEntityIdFor(npcId));
        }
        // UUID
        if (!spawn.getUUIDs().getValues().isEmpty()) {
            spawn.getUUIDs().write(0, npcId);
        }
        // EntityType: Spieler
        if (!spawn.getEntityTypeModifier().getValues().isEmpty()) {
            spawn.getEntityTypeModifier().write(0, EntityType.PLAYER);
        }
        // Position
        if (spawn.getDoubles().size() >= 3) {
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());
        }
        // Rotationen (falls vorhanden)
        if (spawn.getBytes().size() >= 3) {
            spawn.getBytes().write(0, pitchToByte(loc.getPitch()));
            spawn.getBytes().write(1, yawToByte(loc.getYaw()));
            spawn.getBytes().write(2, yawToByte(loc.getYaw())); // headYaw
        }
        pm.sendServerPacket(viewer, spawn);
    }

    private int getEntityIdFor(UUID uuid) {
        // Eine einfache konsistente Ableitung: lower 31 bits (positiv)
        return (int) (uuid.getLeastSignificantBits() & 0x7FFFFFFF);
    }

    private byte yawToByte(float yaw) {
        return (byte) (int) Math.floor((yaw * 256.0f) / 360.0f);
    }

    private byte pitchToByte(float pitch) {
        return (byte) (int) Math.floor((pitch * 256.0f) / 360.0f);
    }
}
