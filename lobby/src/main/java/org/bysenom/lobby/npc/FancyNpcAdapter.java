package org.bysenom.lobby.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bysenom.lobby.LobbySystem;

/**
 * Optionale Integration mit FancyNpcs, komplett reflektionsbasiert damit keine
 * Compile-Zeit-Abhängigkeit nötig ist. Falls FancyNpcs installiert ist und die
 * erwarteten Klassen gefunden werden, versucht dieser Adapter einen echten
 * Player-NPC zu erzeugen. Bei Fehlern wird Fallback (ArmorStand oder eigener
 * Packet-NPC) genutzt.
 */
public class FancyNpcAdapter {
    private final LobbySystem plugin;
    private boolean checked = false;
    private boolean available = false;
    private Class<?> npcManagerClass;
    private Class<?> fancyPluginClass;
    private Object npcManagerInstance;

    public FancyNpcAdapter(LobbySystem plugin) { this.plugin = plugin; }

    private void detect() {
        if (checked) return;
        checked = true;
        try {
            org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("FancyNpcs");
            if (p == null) { plugin.getLogger().info("FancyNpcAdapter: FancyNpcs Plugin nicht gefunden – Fallback aktiv."); return; }
            Object pluginInst = p; fancyPluginClass = pluginInst.getClass();
            // 1) Direkte, häufige Methoden
            for (String mn : new String[]{"getNpcManager","getManager","getApi","api","getRegistry","getNpcs"}) {
                try {
                    java.lang.reflect.Method m = pluginInst.getClass().getMethod(mn);
                    Object mgr = m.invoke(pluginInst);
                    if (mgr != null) { npcManagerInstance = mgr; npcManagerClass = mgr.getClass(); break; }
                } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
            }
            // 2) Fallback: alle no-arg public Methoden aufrufen und nach Klassen mit "npc" suchen
            if (npcManagerInstance == null) {
                for (java.lang.reflect.Method m : pluginInst.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    try {
                        Object r = m.invoke(pluginInst);
                        if (r == null) continue;
                        String cn = r.getClass().getName().toLowerCase();
                        if (cn.contains("npc")) { npcManagerInstance = r; npcManagerClass = r.getClass(); break; }
                        // Auch ohne "npc": prüfe ob create/spawn Methoden vorhanden
                        if (hasCreateOrBuilder(r.getClass())) { npcManagerInstance = r; npcManagerClass = r.getClass(); break; }
                    } catch (Throwable ignored) {}
                }
            }
            if (npcManagerInstance == null) { plugin.getLogger().warning("FancyNpcAdapter: Kein Manager über Reflection zugreifbar – Fallback."); return; }
            available = true;
            plugin.getLogger().info("FancyNpcAdapter: FancyNpcs erkannt – Manager=" + npcManagerClass.getName());
        } catch (Throwable t) { plugin.getLogger().warning("FancyNpcAdapter detect() Fehler: " + t.getMessage()); }
    }

    private boolean hasCreateOrBuilder(Class<?> c) {
        for (java.lang.reflect.Method m : c.getMethods()) {
            String n = m.getName().toLowerCase();
            if (n.contains("create") || n.contains("spawn") || n.contains("builder") || n.contains("build")) return true;
        }
        return false;
    }

    public boolean isAvailable() {
        detect();
        return available;
    }

    /**
     * Versucht einen FancyNpcs Player-NPC zu spawnen.
     * Erwartet, dass Manager eine create* Methode besitzt. Da API unbekannt ist,
     * werden mehrere Signaturen heuristisch getestet.
     * Gibt true zurück, wenn irgendein Aufruf erfolgreich war.
     */
    public boolean spawnFancyNpc(String name, String skin, Location loc) {
        detect();
        if (!available) return false;
        try {
            // A) Direkte create*/spawn* Methoden auf Manager
            for (java.lang.reflect.Method m : npcManagerClass.getMethods()) {
                String n = m.getName().toLowerCase();
                if (!(n.contains("npc") || n.contains("player")) && !(n.contains("create") || n.contains("spawn") || n.contains("add"))) continue;
                if (m.getParameterCount() == 0) continue;
                Object[] args = tryBuildArgs(m.getParameterTypes(), name, skin, loc);
                if (args == null) continue;
                try {
                    Object npc = m.invoke(npcManagerInstance, args);
                    if (npc != null) { plugin.getLogger().info("FancyNpcAdapter: NPC via Manager." + m.getName()); attemptSpawn(npc); return true; }
                } catch (Throwable ignored) {}
            }
            // B) Builder: manager.builder()/newBuilder()/createBuilder()
            for (String bn : new String[]{"builder","newBuilder","createBuilder","npcBuilder","playerBuilder"}) {
                try {
                    java.lang.reflect.Method bm = npcManagerClass.getMethod(bn);
                    Object builder = bm.invoke(npcManagerInstance);
                    if (builder != null && configureAndBuild(builder, name, skin, loc)) return true;
                } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
            }
            // C) Static Fancy API Klassen: FancyNpcs.get().getNpcManager() usw.
            for (String cn : new String[]{
                    "de.oliver.fancynpcs.FancyNpcs",
                    "de.oliver.fancynpcs.FancyNpcsPlugin",
                    "de.oliver.fancynpcs.api.FancyNpcs",
                    "de.oliver.fancynpcs.api.FancyNpcsPlugin"}) {
                try {
                    Class<?> cl = Class.forName(cn);
                    for (String sn : new String[]{"get","getInstance","instance","plugin"}) {
                        try {
                            java.lang.reflect.Method gm = cl.getMethod(sn);
                            Object api = gm.invoke(null);
                            if (api == null) continue;
                            // erneut Manager finden
                            for (String mn : new String[]{"getNpcManager","getManager","getApi","getRegistry","getNpcs"}) {
                                try {
                                    java.lang.reflect.Method mm = api.getClass().getMethod(mn);
                                    Object mgr = mm.invoke(api);
                                    if (mgr != null) {
                                        npcManagerInstance = mgr; npcManagerClass = mgr.getClass();
                                        // Wiederhole A/B
                                        if (spawnFancyNpc(name, skin, loc)) return true;
                                    }
                                } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
                            }
                        } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
                    }
                } catch (ClassNotFoundException ignored) {}
            }
            plugin.getLogger().warning("FancyNpcAdapter: Keine passende create*/builder Signatur gefunden – Fallback.");
        } catch (Throwable t) {
            plugin.getLogger().warning("FancyNpcAdapter spawnFancyNpc Fehler: " + t.getMessage());
        }
        return false;
    }

    private Object[] tryBuildArgs(Class<?>[] pts, String name, String skin, Location loc) {
        Object[] args = new Object[pts.length];
        for (int i = 0; i < pts.length; i++) {
            Class<?> t = pts[i]; String ln = t.getName().toLowerCase();
            if (t == String.class) {
                // heuristik: erster String = name, zweiter = skin
                args[i] = (i == 0) ? name : (skin != null ? skin : name);
            } else if (t == java.util.UUID.class) {
                args[i] = java.util.UUID.randomUUID();
            } else if (t == org.bukkit.Location.class) {
                args[i] = loc;
            } else if (t == org.bukkit.World.class) {
                args[i] = loc.getWorld();
            } else if (t == double.class || t == Double.class) {
                // erste drei double: x,y,z
                args[i] = i % 3 == 0 ? loc.getX() : (i % 3 == 1 ? loc.getY() : loc.getZ());
            } else if (t == float.class || t == Float.class) {
                args[i] = (float) loc.getYaw();
            } else if (t == int.class || t == Integer.class) {
                args[i] = 0;
            } else if (t == boolean.class || t == Boolean.class) {
                args[i] = Boolean.TRUE;
            } else if (ln.contains("skin")) {
                args[i] = skin != null ? skin : name;
            } else {
                // Unbekannt: abbrechen
                return null;
            }
        }
        return args;
    }

    private boolean configureAndBuild(Object builder, String name, String skin, Location loc) {
        try {
            Class<?> bc = builder.getClass();
            // setName / name
            for (String mn : new String[]{"name","setName","displayName","title"}) {
                try { java.lang.reflect.Method m = bc.getMethod(mn, String.class); m.invoke(builder, name); break; } catch (NoSuchMethodException ignored) {} }
            // setSkin
            if (skin != null) {
                for (String mn : new String[]{"skin","setSkin","setPlayerSkin","playerSkin"}) {
                    try { java.lang.reflect.Method m = bc.getMethod(mn, String.class); m.invoke(builder, skin); break; } catch (NoSuchMethodException ignored) {} }
            }
            // setLocation / position
            for (String mn : new String[]{"location","setLocation","position","setPosition"}) {
                try { java.lang.reflect.Method m = bc.getMethod(mn, org.bukkit.Location.class); m.invoke(builder, loc); break; } catch (NoSuchMethodException ignored) {} }
            // world/x/y/z einzeln, wenn nötig
            try { java.lang.reflect.Method m = bc.getMethod("world", org.bukkit.World.class); m.invoke(builder, loc.getWorld()); } catch (NoSuchMethodException ignored) {}
            try { java.lang.reflect.Method m = bc.getMethod("x", double.class); m.invoke(builder, loc.getX()); } catch (NoSuchMethodException ignored) {}
            try { java.lang.reflect.Method m = bc.getMethod("y", double.class); m.invoke(builder, loc.getY()); } catch (NoSuchMethodException ignored) {}
            try { java.lang.reflect.Method m = bc.getMethod("z", double.class); m.invoke(builder, loc.getZ()); } catch (NoSuchMethodException ignored) {}
            // build / create / spawn
            for (String fn : new String[]{"build","create","spawn","register"}) {
                try {
                    java.lang.reflect.Method fm = bc.getMethod(fn);
                    Object npc = fm.invoke(builder);
                    attemptSpawn(npc);
                    return npc != null;
                } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private void attemptSpawn(Object npc) {
        if (npc == null) return;
        try {
            for (String mn : new String[]{"spawn","register","show","display"}) {
                try {
                    java.lang.reflect.Method m = npc.getClass().getMethod(mn);
                    m.setAccessible(true); m.invoke(npc); return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
