package org.bysenom.lobby.npc;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Minimaler, reflection-basierter Fake-Player NPC für Paper 1.21.x (mojmap-Names).
 * - Erzeugt einen NMS ServerPlayer (nicht registriert) nur für Packet-Daten.
 * - Sendet PlayerInfoUpdate(ADD), AddPlayer, optional RemoveFromTab.
 * - Klick-Erkennung erfolgt extern über eine unsichtbare ArmorStand-Hitbox.
 */
public class PlayerNpc {
    private final UUID uuid;
    private final String name16;
    private final Object nmsServerPlayer; // net.minecraft.server.level.ServerPlayer
    private final int entityId;
    private final Location loc;

    // Cached classes/ctors
    private static Class<?> CRAFT_SERVER, CRAFT_WORLD, CRAFT_PLAYER;
    private static Class<?> MINECRAFT_SERVER, SERVER_LEVEL, SERVER_PLAYER;
    private static Class<?> GAMEPROFILE;
    private static Class<?> CBPIUP; // ClientboundPlayerInfoUpdatePacket
    private static Class<?> CBAP; // ClientboundAddPlayerPacket (optional)
    private static Class<?> CBADDENT; // ClientboundAddEntityPacket (fallback)
    private static Class<?> CBPIR; // ClientboundPlayerInfoRemovePacket
    private static Class<?> CBREM; // ClientboundRemoveEntitiesPacket
    private static Class<?> CONNECTION; // ServerGamePacketListenerImpl
    private static Class<?> CBROT; // ClientboundRotateHeadPacket
    private static Class<?> CBTEL; // ClientboundTeleportEntityPacket

    private static Method CRAFT_SERVER_getServer;
    private static Method CRAFT_WORLD_getHandle;
    private static Method CRAFT_PLAYER_getHandle;
    private static Field SERVER_PLAYER_connection;
    private static Method CONNECTION_send;
    private final java.util.Set<java.util.UUID> viewers = new java.util.HashSet<>();

    private static boolean inited = false;
    private static boolean disableProtocolLibNpc = false;

    private static void init() throws Exception {
        if (inited) return;
        CRAFT_SERVER = Class.forName("org.bukkit.craftbukkit.CraftServer");
        CRAFT_WORLD = Class.forName("org.bukkit.craftbukkit.CraftWorld");
        CRAFT_PLAYER = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
        MINECRAFT_SERVER = Class.forName("net.minecraft.server.MinecraftServer");
        SERVER_LEVEL = Class.forName("net.minecraft.server.level.ServerLevel");
        SERVER_PLAYER = Class.forName("net.minecraft.server.level.ServerPlayer");
        GAMEPROFILE = Class.forName("com.mojang.authlib.GameProfile");
        CBPIUP = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        try { CBAP = Class.forName("net.minecraft.network.protocol.game.ClientboundAddPlayerPacket"); }
        catch (ClassNotFoundException e) { CBAP = null; }
        try { CBADDENT = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket"); }
        catch (ClassNotFoundException e) { CBADDENT = null; }
        // Remove from tab list packet (1.19+)
        try { CBPIR = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket"); } catch (ClassNotFoundException e) { CBPIR = null; }
        CBREM = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
        CONNECTION = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
        CBROT = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
        CBTEL = Class.forName("net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket");

        CRAFT_SERVER_getServer = CRAFT_SERVER.getMethod("getServer");
        CRAFT_WORLD_getHandle = CRAFT_WORLD.getMethod("getHandle");
        CRAFT_PLAYER_getHandle = CRAFT_PLAYER.getMethod("getHandle");
        SERVER_PLAYER_connection = SERVER_PLAYER.getField("connection");
        CONNECTION_send = CONNECTION.getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
        inited = true;
    }

    public static PlayerNpc create(Location loc, String displayName16, String skinOwnerOrNull) throws Exception {
        init();
        Object craftServer = CRAFT_SERVER.cast(Bukkit.getServer());
        Object nmsServer = CRAFT_SERVER_getServer.invoke(craftServer);
        Object craftWorld = CRAFT_WORLD.cast(loc.getWorld());
        Object nmsLevel = CRAFT_WORLD_getHandle.invoke(craftWorld);
        UUID id = UUID.randomUUID();
        String name = (displayName16 == null || displayName16.isEmpty()) ? "NPC" : displayName16;
        if (name.length() > 16) name = name.substring(0, 16);
        Constructor<?> gpCtor = GAMEPROFILE.getConstructor(UUID.class, String.class);
        Object profile = gpCtor.newInstance(id, name);
        try {
            if (skinOwnerOrNull != null && !skinOwnerOrNull.isBlank()) {
                org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(skinOwnerOrNull);
                if (off.isOnline()) {
                    Object cp = CRAFT_PLAYER.cast(off.getPlayer());
                    Method getProfile = cp.getClass().getMethod("getProfile");
                    Object their = getProfile.invoke(cp);
                    Method getProps = their.getClass().getMethod("getProperties");
                    Object theirProps = getProps.invoke(their);
                    Object myProps = profile.getClass().getMethod("getProperties").invoke(profile);
                    Method putAll = myProps.getClass().getMethod("putAll", theirProps.getClass());
                    putAll.invoke(myProps, theirProps);
                }
            }
        } catch (Throwable ignored) {}
        Object serverPlayer = null;
        List<String> attemptLog = new ArrayList<>();
        for (Constructor<?> ctor : SERVER_PLAYER.getDeclaredConstructors()) {
            Class<?>[] pts = ctor.getParameterTypes();
            if (pts.length < 3) continue;
            if (!MINECRAFT_SERVER.isAssignableFrom(pts[0]) || !SERVER_LEVEL.isAssignableFrom(pts[1]) || !GAMEPROFILE.isAssignableFrom(pts[2])) continue;
            Object[] args = new Object[pts.length];
            args[0] = nmsServer; args[1] = nmsLevel; args[2] = profile;
            boolean skip = false;
            for (int i = 3; i < pts.length; i++) {
                Class<?> t = pts[i]; String n = t.getName().toLowerCase();
                try {
                    if (n.contains("clientinformation")) {
                        args[i] = buildDummyClientInformation(t);
                    } else if (n.contains("profilepublickey") || n.contains("publickey")) {
                        args[i] = null; // akzeptiere null für PublicKey
                    } else if (t.isPrimitive()) {
                        if (t == boolean.class) args[i] = false; else if (t == int.class) args[i] = 0; else if (t == long.class) args[i] = 0L; else if (t == float.class) args[i] = 0f; else if (t == double.class) args[i] = 0d; else args[i] = 0;
                    } else if (t.isEnum()) {
                        Object[] c = t.getEnumConstants(); args[i] = (c != null && c.length > 0) ? c[0] : null;
                    } else {
                        args[i] = null;
                    }
                } catch (Throwable ex) { attemptLog.add("Param build fail for " + t.getName() + ": " + ex.getMessage()); args[i] = null; }
            }
            attemptLog.add("Trying ctor " + ctor);
            try {
                ctor.setAccessible(true);
                serverPlayer = ctor.newInstance(args);
                attemptLog.add("Success ctor " + ctor);
                break;
            } catch (Throwable ex) {
                attemptLog.add("Failed ctor " + ctor + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
        if (serverPlayer == null) {
            Bukkit.getLogger().warning("PlayerNpc CREATE failed – attempts=" + attemptLog);
            throw new IllegalStateException("NO_VALID_SERVERPLAYER_CTOR");
        }
        int eId;
        try { Method getId = SERVER_PLAYER.getMethod("getId"); eId = (int) getId.invoke(serverPlayer); } catch (Throwable t) { eId = new Random().nextInt(Integer.MAX_VALUE); }
        return new PlayerNpc(id, name, serverPlayer, eId, loc.clone());
    }

    private PlayerNpc(UUID uuid, String name16, Object nmsServerPlayer, int entityId, Location loc) {
        this.uuid = uuid;
        this.name16 = name16;
        this.nmsServerPlayer = nmsServerPlayer;
        this.entityId = entityId;
        this.loc = loc;
    }

    public UUID getUuid() { return uuid; }
    public int getEntityId() { return entityId; }
    public Location getLocation() { return loc.clone(); }

    private static ProtocolNpcSender protocolSender = null;
    private static boolean protocolChecked = false;

    private static boolean hasProtocolLib() {
        if (!protocolChecked) {
            protocolChecked = true;
            try { Class.forName("com.comphenix.protocol.ProtocolLibrary"); protocolSender = new ProtocolNpcSender(); } catch (Throwable ignored) {}
        }
        return protocolSender != null;
    }

    public void showTo(Player viewer, boolean hideFromTabLater) {
        try {
            if (!disableProtocolLibNpc && hasProtocolLib()) {
                boolean ok = protocolSender.showPlayerNpcSafe(viewer, this.uuid, this.name16, this.loc);
                if (ok) {
                    viewers.add(viewer.getUniqueId());
                    return;
                } else {
                    Bukkit.getLogger().warning("PlayerNpc ProtocolLib safe spawn nicht vollständig – versuche einmalige Deaktivierung für diese Server-Version.");
                    disableProtocolLibNpc = true; // einmal abschalten, bis Version unterstützt
                }
            }
        } catch (Throwable protoFail) {
            Bukkit.getLogger().warning("PlayerNpc ProtocolLib spawn failed: " + protoFail.getMessage());
            disableProtocolLibNpc = true;
        }
        // Aktuell fehlschlägt der Reflection PlayerInfo Packet Build in 1.21.10 (Mapping geändert) → nicht weiter spammen
        if (CBPIUP != null) {
            if (!viewer.hasPermission("lobby.admin")) return; // nur Admin sieht Hinweis
            viewer.sendMessage("§cNPC Spawn für diese MC-Version (1.21.10) noch nicht implementiert – Update folgt.");
        }
    }

    public void hideFrom(Player viewer) {
        try {
            if (hasProtocolLib()) {
                protocolSender.hidePlayerNpc(viewer, this.uuid);
                viewers.remove(viewer.getUniqueId());
                return;
            }
        } catch (Throwable protoFail) {
            Bukkit.getLogger().warning("PlayerNpc ProtocolLib hide failed, fallback Reflection: " + protoFail.getMessage());
        }
        try {
            Object handle = CRAFT_PLAYER_getHandle.invoke(viewer);
            Object conn = SERVER_PLAYER_connection.get(handle);
            Constructor<?> remC = CBREM.getConstructor(int[].class);
            Object rem = remC.newInstance((Object) new int[]{ entityId });
            CONNECTION_send.invoke(conn, rem);
            if (CBPIR != null) {
                Constructor<?> rc = CBPIR.getConstructor(java.util.List.class);
                Object remTab = rc.newInstance(java.util.Collections.singletonList(uuid));
                CONNECTION_send.invoke(conn, remTab);
            }
            viewers.remove(viewer.getUniqueId());
        } catch (Throwable t) {
            Bukkit.getLogger().warning("PlayerNpc.hideFrom failed: " + t.getMessage());
        }
    }

    public void setRotation(float yaw, float pitch) {
        try {
            // Update server-side state (not attached, but helps teleport constructor)
            SERVER_PLAYER.getMethod("moveTo", double.class, double.class, double.class, float.class, float.class)
                    .invoke(nmsServerPlayer, loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);
            this.loc.setYaw(yaw);
            this.loc.setPitch(pitch);
        } catch (Throwable ignored) {}
        broadcastRotation();
    }

    public void broadcastRotation() {
        // send RotateHead + Teleport packets to all viewers
        java.util.List<Player> online = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
        for (Player v : online) {
            if (!viewers.contains(v.getUniqueId())) continue;
            try {
                Object handle = CRAFT_PLAYER_getHandle.invoke(v);
                Object conn = SERVER_PLAYER_connection.get(handle);
                // RotateHead
                byte yawByte = (byte) (int) Math.floor((this.loc.getYaw() * 256.0f) / 360.0f);
                Object rotPkt = CBROT.getConstructor(Class.forName("net.minecraft.world.entity.Entity"), byte.class)
                        .newInstance(nmsServerPlayer, yawByte);
                CONNECTION_send.invoke(conn, rotPkt);
                // Teleport (updates yaw/pitch clientside reliably)
                Object telPkt = CBTEL.getConstructor(Class.forName("net.minecraft.world.entity.Entity")).newInstance(nmsServerPlayer);
                CONNECTION_send.invoke(conn, telPkt);
            } catch (Throwable ignored) {}
        }
    }

    // Generic Helper für typisierte EnumSet-Erzeugung
    private static <E extends Enum<E>> EnumSet<E> buildEnumSet(Class<E> enumClass, String[] desired) {
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        outer: for (E constant : enumClass.getEnumConstants()) {
            for (String want : desired) {
                if (constant.name().equalsIgnoreCase(want)) { set.add(constant); continue outer; }
            }
        }
        if (set.isEmpty() && enumClass.getEnumConstants().length > 0) {
            set.add(enumClass.getEnumConstants()[0]);
        }
        return set;
    }

    // Cache für reflektierte Konstruktoren (Overhead reduzieren)
    private static volatile Constructor<?> CACHED_PIUP_CTOR = null; // (EnumSet, Collection)
    private static volatile boolean CACHED_PIUP_USES_ENTRY = true;   // true => Collection<Entry>, false => Collection<ServerPlayer>
    private static volatile Constructor<?> CACHED_ADDPLAYER_CTOR = null; // 1-param bevorzugt

    // Hilfsmethode: baut ein Set der gewünschten Action-Enum-Konstanten typsicher ohne unchecked Adds
    private static Set<?> buildActionSet(Class<?> actionEnumClass, String[] desired) {
        Object[] constants = actionEnumClass.getEnumConstants();
        java.util.LinkedHashSet<Object> set = new java.util.LinkedHashSet<>();
        for (Object c : constants) {
            String n = c.toString();
            for (String want : desired) {
                if (n.equalsIgnoreCase(want)) { set.add(c); break; }
            }
        }
        if (set.isEmpty() && constants.length > 0) set.add(constants[0]);
        return set;
    }

    private Object buildPlayerInfoPacket(Object serverPlayer) throws Exception {
        // Locate Action enum and Entry inner class
        Class<?> actionEnum = null;
        Class<?> entryClass = null;
        for (Class<?> inner : CBPIUP.getDeclaredClasses()) {
            if (inner.isEnum() && inner.getSimpleName().equalsIgnoreCase("Action")) actionEnum = inner;
            else if (inner.getSimpleName().equalsIgnoreCase("Entry")) entryClass = inner;
        }
        if (actionEnum == null) throw new IllegalStateException("PlayerInfoUpdate Action enum not found");
        if (entryClass == null) throw new IllegalStateException("PlayerInfoUpdate Entry class not found");
        // Build Actions Set ohne unchecked Warnungen
        String[] desired = {"ADD_PLAYER","UPDATE_GAME_MODE","UPDATE_LATENCY","UPDATE_DISPLAY_NAME","UPDATE_LISTED"};
        Set<?> actions = buildActionSet(actionEnum, desired);
        // Unterstützende Typen
        UUID uuidLocal = this.uuid;
        Class<?> gameTypeClass = Class.forName("net.minecraft.world.level.GameType");
        Object survival = gameTypeClass.getField("SURVIVAL").get(null);
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Object displayName = componentClass.getMethod("literal", String.class).invoke(null, name16);
        Object gameProfile = null;
        try { gameProfile = SERVER_PLAYER.getMethod("getGameProfile").invoke(serverPlayer); } catch (Throwable ignored) {}
        // Heuristisch Entry-Konstruktor wählen: größter zuerst
        Constructor<?> best = null;
        for (Constructor<?> ec : entryClass.getDeclaredConstructors()) {
            if (best == null || ec.getParameterCount() > best.getParameterCount()) best = ec;
        }
        Object entryInstance = null;
        if (best != null) {
            Class<?>[] pts = best.getParameterTypes();
            Object[] args = new Object[pts.length];
            for (int i=0;i<pts.length;i++) {
                Class<?> t = pts[i]; String tn = t.getName();
                if (t == UUID.class) args[i] = uuidLocal;
                else if (GAMEPROFILE.isAssignableFrom(t)) args[i] = gameProfile;
                else if (t == int.class || t == Integer.class) args[i] = 0;
                else if (t == boolean.class || t == Boolean.class) args[i] = Boolean.TRUE;
                else if (gameTypeClass.isAssignableFrom(t)) args[i] = survival;
                else if (componentClass.isAssignableFrom(t)) args[i] = displayName;
                else if (t.isEnum()) { Object[] ec = t.getEnumConstants(); args[i] = (ec!=null&&ec.length>0)?ec[0]:null; }
                else if (tn.toLowerCase().contains("optional")) args[i] = java.util.Optional.empty();
                else args[i] = null;
            }
            try { best.setAccessible(true); entryInstance = best.newInstance(args); } catch (Throwable ignored) {}
        }
        if (entryInstance == null) {
            for (Constructor<?> ec : entryClass.getDeclaredConstructors()) {
                Class<?>[] pts = ec.getParameterTypes();
                Object[] args = new Object[pts.length];
                for (int i=0;i<pts.length;i++) {
                    Class<?> t = pts[i]; String tn = t.getName();
                    if (t == UUID.class) args[i] = uuidLocal;
                    else if (GAMEPROFILE.isAssignableFrom(t)) args[i] = gameProfile;
                    else if (t == int.class || t == Integer.class) args[i] = 0;
                    else if (t == boolean.class || t == Boolean.class) args[i] = Boolean.TRUE;
                    else if (gameTypeClass.isAssignableFrom(t)) args[i] = survival;
                    else if (componentClass.isAssignableFrom(t)) args[i] = displayName;
                    else if (t.isEnum()) { Object[] ecv = t.getEnumConstants(); args[i] = (ecv!=null&&ecv.length>0)?ecv[0]:null; }
                    else if (tn.toLowerCase().contains("optional")) args[i] = java.util.Optional.empty();
                    else args[i] = null;
                }
                try { ec.setAccessible(true); entryInstance = ec.newInstance(args); break; } catch (Throwable ignored) {}
            }
        }
        if (entryInstance == null) throw new IllegalStateException("Could not construct PlayerInfo Entry");
        List<Object> entryList = java.util.Collections.singletonList(entryInstance);
        List<Object> serverPlayers = java.util.Collections.singletonList(serverPlayer);
        if (CACHED_PIUP_CTOR != null) {
            try { return CACHED_PIUP_CTOR.newInstance(actions, CACHED_PIUP_USES_ENTRY ? entryList : serverPlayers); } catch (Throwable fail) { CACHED_PIUP_CTOR = null; }
        }
        for (Constructor<?> ctor : CBPIUP.getDeclaredConstructors()) {
            Class<?>[] p = ctor.getParameterTypes();
            if (p.length == 2 && Set.class.isAssignableFrom(p[0]) && Collection.class.isAssignableFrom(p[1])) {
                try { ctor.setAccessible(true); Object pkt = ctor.newInstance(actions, entryList); CACHED_PIUP_CTOR = ctor; CACHED_PIUP_USES_ENTRY = true; return pkt; } catch (Throwable ignored) {}
                try { ctor.setAccessible(true); Object pkt = ctor.newInstance(actions, serverPlayers); CACHED_PIUP_CTOR = ctor; CACHED_PIUP_USES_ENTRY = false; return pkt; } catch (Throwable ignored2) {}
            }
        }
        throw new IllegalStateException("No suitable PlayerInfoUpdatePacket constructor (Entry or ServerPlayer) worked");
    }


    private Object buildAddPlayerPacket(Object serverPlayer) throws Exception {
        if (CBAP == null) throw new IllegalStateException("ClientboundAddPlayerPacket class not found");
        if (CACHED_ADDPLAYER_CTOR != null) {
            try { return CACHED_ADDPLAYER_CTOR.newInstance(serverPlayer); } catch (Throwable ignored) { CACHED_ADDPLAYER_CTOR = null; }
        }
        // Bevorzugt: einparametriger ctor
        for (Constructor<?> ctor : CBAP.getDeclaredConstructors()) {
            Class<?>[] p = ctor.getParameterTypes();
            if (p.length == 1) {
                if (p[0].isInstance(serverPlayer) || p[0].isAssignableFrom(serverPlayer.getClass())) {
                    try { ctor.setAccessible(true); Object pkt = ctor.newInstance(serverPlayer); CACHED_ADDPLAYER_CTOR = ctor; return pkt; } catch (Throwable ignored) {}
                }
            }
        }
        // Fallback: weitere ctors probieren
        for (Constructor<?> ctor : CBAP.getDeclaredConstructors()) {
            try {
                Object candidate = tryBuildAddPlayerByParams(ctor);
                if (candidate != null) { CACHED_ADDPLAYER_CTOR = ctor; return candidate; }
            } catch (Throwable ignored) {}
        }
        // Bekannte Signatur: Player (net.minecraft.world.entity.player.Player)
        try {
            Class<?> PLAYER = Class.forName("net.minecraft.world.entity.player.Player");
            Constructor<?> c = CBAP.getDeclaredConstructor(PLAYER);
            c.setAccessible(true);
            Object pkt = c.newInstance(serverPlayer);
            CACHED_ADDPLAYER_CTOR = c;
            return pkt;
        } catch (Throwable ignored) {}
        throw new IllegalStateException("No suitable ClientboundAddPlayerPacket constructor found");
    }

    private Object buildAddEntityPacket(Object serverPlayer) throws Exception {
        // Bevorzugt: ctor(Entity)
        try {
            Constructor<?> c = CBADDENT.getDeclaredConstructor(Class.forName("net.minecraft.world.entity.Entity"));
            c.setAccessible(true);
            return c.newInstance(serverPlayer);
        } catch (NoSuchMethodException ignored) {}
        // Sonst: finde ctor mit (int entityId, UUID, double x,y,z, float yaw, float pitch, int type, int data)
        for (Constructor<?> ctor : CBADDENT.getDeclaredConstructors()) {
            try {
                Class<?>[] p = ctor.getParameterTypes();
                Object[] args = new Object[p.length];
                int doubleCount = 0; int floatCount = 0; boolean usedId = false;
                for (int i = 0; i < p.length; i++) {
                    Class<?> t = p[i];
                    if (t == int.class || t == Integer.class) { args[i] = usedId ? 0 : this.entityId; usedId = true; }
                    else if (t == long.class || t == Long.class) { args[i] = 0L; }
                    else if (t == UUID.class) { args[i] = this.uuid; }
                    else if (t == double.class || t == Double.class) {
                        if (doubleCount == 0) args[i] = this.loc.getX();
                        else if (doubleCount == 1) args[i] = this.loc.getY();
                        else if (doubleCount == 2) args[i] = this.loc.getZ();
                        else args[i] = 0.0;
                        doubleCount++;
                    } else if (t == float.class || t == Float.class) {
                        if (floatCount == 0) args[i] = this.loc.getYaw();
                        else if (floatCount == 1) args[i] = this.loc.getPitch();
                        else args[i] = 0.0f;
                        floatCount++;
                    } else if (t.isEnum()) {
                        Object[] consts = t.getEnumConstants();
                        args[i] = consts != null && consts.length > 0 ? consts[0] : null;
                    } else if (t.getName().startsWith("net.minecraft")) {
                        args[i] = null;
                    } else { args[i] = null; }
                }
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            } catch (Throwable ignored) {}
        }
        throw new IllegalStateException("No suitable ClientboundAddEntityPacket constructor found");
    }

    private Object tryBuildAddPlayerByParams(Constructor<?> ctor) throws Exception {
        Class<?>[] types = ctor.getParameterTypes();
        Object[] args = new Object[types.length];
        boolean usedEntityId = false; int doubleCount = 0; int floatCount = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == int.class || t == Integer.class) {
                args[i] = usedEntityId ? 0 : this.entityId; usedEntityId = true;
            } else if (t == long.class || t == Long.class) {
                args[i] = 0L;
            } else if (t == java.util.UUID.class) {
                args[i] = this.uuid;
            } else if (t == double.class || t == Double.class) {
                // Erste drei doubles: x,y,z
                if (doubleCount == 0) args[i] = this.loc.getX();
                else if (doubleCount == 1) args[i] = this.loc.getY();
                else if (doubleCount == 2) args[i] = this.loc.getZ();
                else args[i] = 0.0;
                doubleCount++;
            } else if (t == float.class || t == Float.class) {
                // Erste zwei floats: yaw, pitch
                if (floatCount == 0) args[i] = this.loc.getYaw();
                else if (floatCount == 1) args[i] = this.loc.getPitch();
                else args[i] = 0.0f;
                floatCount++;
            } else if (t == boolean.class || t == Boolean.class) {
                args[i] = Boolean.FALSE;
            } else if (t == String.class) {
                args[i] = this.name16;
            } else if (t == java.util.Optional.class) {
                args[i] = java.util.Optional.empty();
            } else if (java.util.Collection.class.isAssignableFrom(t) || java.util.List.class.isAssignableFrom(t)) {
                args[i] = java.util.Collections.emptyList();
            } else if (t.isEnum()) {
                Object[] consts = t.getEnumConstants();
                args[i] = consts != null && consts.length > 0 ? consts[0] : null;
            } else if (t.getName().startsWith("net.minecraft")) {
                // Für komplexe Mojang-Typen: versuche null oder Standardwerte
                args[i] = null;
            } else {
                // Unbekannt: null
                args[i] = null;
            }
        }
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    private static Field findFieldBySuffix(Class<?> clazz, String suffix) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().endsWith(suffix)) return f;
        }
        Class<?> sup = clazz.getSuperclass();
        if (sup != null && sup != Object.class) return findFieldBySuffix(sup, suffix);
        return null;
    }
    private static Object buildDummyClientInformation(Class<?> ciClass) {
        try {
            // Suche static factory Methoden
            for (Method m : ciClass.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0 && m.getReturnType() == ciClass) {
                    m.setAccessible(true); return m.invoke(null);
                }
            }
            // Versuch: erster Konstruktor
            for (Constructor<?> ctor : ciClass.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                Object[] args = new Object[p.length];
                for (int i = 0; i < p.length; i++) {
                    Class<?> t = p[i]; String ln = t.getName().toLowerCase();
                    if (t == String.class) args[i] = "en_US";
                    else if (t == int.class || t == Integer.class) args[i] = 8;
                    else if (t == boolean.class || t == Boolean.class) args[i] = true;
                    else if (t.isEnum()) { Object[] ec = t.getEnumConstants(); args[i] = (ec != null && ec.length > 0) ? ec[0] : null; }
                    else args[i] = null;
                }
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            }
        } catch (Throwable ignored) {}
        return null; // fallback null
    }
}
