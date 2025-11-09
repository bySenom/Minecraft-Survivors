package org.bysenom.minecraftSurvivors.listener;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.ability.AbilityCatalog;

/**
 * Glyphen-Pickup wie Lootchests: schwebende Items mit Hologramm, bei Berührung öffnet sich ein GlyphSocket-UI für eine Ability.
 */
public class GlyphPickupListener implements Listener {

    private final MinecraftSurvivors plugin;
    public GlyphPickupListener(MinecraftSurvivors plugin) { this.plugin = plugin; }

    private static final Map<java.util.UUID, ActiveGlyph> GLYPHS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<java.util.UUID, java.util.UUID> DISPLAY_TO_GLYPH = new java.util.concurrent.ConcurrentHashMap<>();
    private static org.bukkit.scheduler.BukkitTask anim;

    private static final class ActiveGlyph {
        final java.util.UUID id; final World world; final Location base; final java.util.UUID itemId; final java.util.UUID textId; final long expireAt; final String abilityKey; final java.util.List<String> glyphChoices;
        ActiveGlyph(java.util.UUID id, World w, Location base, java.util.UUID item, java.util.UUID text, long expireAt, String abilityKey, java.util.List<String> glyphChoices) {
            this.id=id; this.world=w; this.base=base; this.itemId=item; this.textId=text; this.expireAt=expireAt; this.abilityKey=abilityKey; this.glyphChoices = glyphChoices == null ? new java.util.ArrayList<>() : glyphChoices;
        }
    }

    // pending glyphs per player (one pickup = one pending socket action)
    private static final java.util.Map<java.util.UUID, String> PENDING_BY_PLAYER = new java.util.concurrent.ConcurrentHashMap<>();
    // track when the player currently has a glyph-selection UI open (so we don't resume the game)
    private static final java.util.Set<java.util.UUID> SELECTION_OPEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // store selection context so we can reopen socket menu after selection close
    private static final java.util.Map<java.util.UUID, java.util.Map<String, Object>> SELECTION_CTX = new java.util.concurrent.ConcurrentHashMap<>();
    // mark that selection was handled (clicked) to avoid the close-listener reopening UI twice
    private static final java.util.Set<java.util.UUID> SELECTION_HANDLED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    // prevent reopen window for a short period after selection (timestamp expiry)
    private static final java.util.Map<java.util.UUID, Long> NO_REOPEN_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setPendingGlyph(java.util.UUID playerUuid, String glyphKey) { if (playerUuid==null) return; if (glyphKey==null) { PENDING_BY_PLAYER.remove(playerUuid); } else PENDING_BY_PLAYER.put(playerUuid, glyphKey); }
    public static String getPendingGlyph(java.util.UUID playerUuid) { if (playerUuid==null) return null; return PENDING_BY_PLAYER.get(playerUuid); }
    public static String consumePendingGlyph(java.util.UUID playerUuid) { if (playerUuid==null) return null; return PENDING_BY_PLAYER.remove(playerUuid); }
    public static void clearPendingFor(java.util.UUID playerUuid) { if (playerUuid==null) return; PENDING_BY_PLAYER.remove(playerUuid); }
    // improved logging wrapper
    public static void setPendingGlyphWithLog(java.util.UUID playerUuid, String glyphKey) {
        setPendingGlyph(playerUuid, glyphKey);
        try {
            MinecraftSurvivors pl = MinecraftSurvivors.getInstance();
            if (pl != null) {
                String name = "<unknown>";
                try { org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(playerUuid); if (p != null) name = p.getName(); } catch (Throwable ignored) {}
                if (glyphKey == null) pl.getLogger().info("Pending glyph cleared for " + name + " (" + playerUuid + ")");
                else pl.getLogger().info("Pending glyph set for " + name + " (" + playerUuid + ") -> " + glyphKey);
            }
        } catch (Throwable ignored) {}
    }

    public static void spawnGlyph(Location loc, String abilityKey) {
        MinecraftSurvivors pl = MinecraftSurvivors.getInstance(); if (pl == null || loc == null || loc.getWorld() == null) return;
        World w = loc.getWorld();
        // choose up to 3 glyph choices for this ability so player can pick
        java.util.List<org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def> choices = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.forAbility(abilityKey);
        java.util.List<String> glyphKeys = new java.util.ArrayList<>();
        if (choices != null && !choices.isEmpty()) {
            java.util.Random rnd = new java.util.Random();
            // select up to 3 distinct choices (allow duplicates only if insufficient)
            int want = Math.min(3, Math.max(1, choices.size()));
            java.util.Set<Integer> idxs = new java.util.LinkedHashSet<>();
            while (idxs.size() < want) idxs.add(rnd.nextInt(choices.size()));
            for (int i : idxs) glyphKeys.add(choices.get(i).key);
            // if less than 3, fill with repeated randoms
            while (glyphKeys.size() < 3) glyphKeys.add(choices.get(rnd.nextInt(choices.size())).key);
        }
        // Wähle Icon von Ability or glyph if möglich
        AbilityCatalog.Def def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(abilityKey);
        org.bukkit.Material mat = null;
        String cfgMat = null;
        try { cfgMat = pl.getConfigUtil().getString("glyph.display-material", "ARMOR_TRIM_SMITHING_TEMPLATE"); } catch (Throwable ignored) {}
        if (cfgMat != null) {
            try { mat = org.bukkit.Material.valueOf(cfgMat.toUpperCase(Locale.ROOT)); } catch (Throwable ignored) { mat = null; }
        }
        if (mat == null) {
            // prefer glyph icon if available
            if (glyphKeys.get(0) != null) {
                org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def gdef = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.get(glyphKeys.get(0));
                if (gdef != null && gdef.icon != null) mat = gdef.icon;
            }
            if (mat == null) mat = (def != null && def.icon != null) ? def.icon : org.bukkit.Material.AMETHYST_SHARD;
        }

        final org.bukkit.Material displayMat = mat;
        Location itemLoc = loc.clone().add(0, 0.6, 0);
        Location textLoc = loc.clone().add(0, 1.6, 0);
        ItemDisplay item = w.spawn(itemLoc, ItemDisplay.class, d -> {
            try { d.setItemStack(new ItemStack(displayMat)); } catch (Throwable ignored) { d.setItemStack(new ItemStack(org.bukkit.Material.AMETHYST_SHARD)); }
            try { d.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}
            try { d.setInterpolationDuration(0); } catch (Throwable ignored) {}
            try { d.setRotation(0f, 0f); } catch (Throwable ignored) {}
            d.setPersistent(true); d.setInvulnerable(true);
        });
        TextDisplay text = w.spawn(textLoc, TextDisplay.class, t -> {
            String name = abilityKey;
            try { if (def != null && def.display != null) name = def.display; } catch (Throwable ignored) {}
            t.text(Component.text("Glyph: ").color(NamedTextColor.LIGHT_PURPLE).append(Component.text(name).color(NamedTextColor.WHITE)));
            try { t.setAlignment(TextDisplay.TextAlignment.CENTER);} catch (Throwable ignored) {}
            try { t.setBillboard(Display.Billboard.CENTER);} catch (Throwable ignored) {}
            t.setShadowed(true); t.setSeeThrough(true); t.setPersistent(true); t.setInvulnerable(true);
        });
        int lifeSec = pl.getConfigUtil().getInt("glyph.lifetime-seconds", 45);
        long expires = System.currentTimeMillis() + Math.max(5, lifeSec) * 1000L;
        java.util.UUID id = java.util.UUID.randomUUID();
        GLYPHS.put(id, new ActiveGlyph(id, w, loc, item.getUniqueId(), text.getUniqueId(), expires, abilityKey, glyphKeys));
        DISPLAY_TO_GLYPH.put(item.getUniqueId(), id);
        DISPLAY_TO_GLYPH.put(text.getUniqueId(), id);
        try { pl.getLogger().info("Glyph spawned: choices=" + glyphKeys + " (ability=" + abilityKey + ") @ " + loc + " (display=" + item.getUniqueId() + ")"); } catch (Throwable ignored) {}
        try { pl.getLogger().fine("Glyph internal choices for spawn id=" + id + " -> " + glyphKeys); } catch (Throwable ignored) {}
        ensureAnim(pl);
    }

    private static void ensureAnim(MinecraftSurvivors pl) {
        if (anim != null && !anim.isCancelled()) return;
        anim = new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<java.util.UUID, ActiveGlyph> en : GLYPHS.entrySet()) {
                    ActiveGlyph ag = en.getValue();
                    if (ag == null) continue;
                    org.bukkit.entity.Entity ei = org.bukkit.Bukkit.getEntity(ag.itemId);
                    org.bukkit.entity.Entity et = org.bukkit.Bukkit.getEntity(ag.textId);
                    if (ei == null || et == null) continue;
                    double t = (now % 4000L) / 4000.0; double bob = Math.sin(t * Math.PI * 2) * 0.15;
                    try {
                        ei.teleport(ag.base.clone().add(0, 0.6 + bob, 0));
                        et.teleport(ag.base.clone().add(0, 1.6 + bob, 0));
                        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(6) == 0) ag.world.spawnParticle(Particle.ENCHANT, ag.base.clone().add(0,1.0+bob,0), 2, 0.2,0.2,0.2, 0.01);
                    } catch (Throwable ignored) {}
                    if (now > ag.expireAt) {
                        try {
                            // If a game is running, keep glyphs alive until round end. Only despawn on expiry when not running.
                            MinecraftSurvivors pluginInstance = MinecraftSurvivors.getInstance();
                            if (pluginInstance != null && pluginInstance.getGameManager() != null) {
                                var state = pluginInstance.getGameManager().getState();
                                if (state == org.bysenom.minecraftSurvivors.model.GameState.RUNNING) {
                                    // skip despawn while running
                                    continue;
                                }
                            }
                        } catch (Throwable ignored) {}
                        despawn(en.getKey(), ag);
                    }
                }
            }
        }.runTaskTimer(pl, 0L, 2L);
    }

    private static void despawn(java.util.UUID id, ActiveGlyph ag) {
        try { org.bukkit.entity.Entity ei = org.bukkit.Bukkit.getEntity(ag.itemId); if (ei != null) ei.remove(); DISPLAY_TO_GLYPH.remove(ag.itemId);} catch (Throwable ignored) {}
        try { org.bukkit.entity.Entity et = org.bukkit.Bukkit.getEntity(ag.textId); if (et != null) et.remove(); DISPLAY_TO_GLYPH.remove(ag.textId);} catch (Throwable ignored) {}
        GLYPHS.remove(id);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (GLYPHS.isEmpty()) return;
        Player p = e.getPlayer();
        Location to = e.getTo(); if (to == null) return;
        double trigger = plugin.getConfigUtil().getDouble("glyph.trigger-radius", 1.4);
        double r2 = trigger * trigger;
        java.util.UUID hitId = null; ActiveGlyph hit = null; long now = System.currentTimeMillis();
        for (Map.Entry<java.util.UUID, ActiveGlyph> en : GLYPHS.entrySet()) {
            ActiveGlyph ag = en.getValue(); if (ag == null) continue; if (!ag.world.equals(to.getWorld())) continue; if (now > ag.expireAt) { despawn(en.getKey(), ag); continue; }
            double dx = to.getX() - ag.base.getX(); double dz = to.getZ() - ag.base.getZ(); if (dx*dx + dz*dz <= r2) { hitId = en.getKey(); hit = ag; break; }
        }
        if (hitId != null && hit != null) {
            despawn(hitId, hit);
            // Open a selection UI with the prechosen glyph choices for this pickup
            try {
                plugin.getGameManager().pauseForPlayer(p.getUniqueId());
            } catch (Throwable ignored) {}
            try {
                var sel = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("Wähle Glyph").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                int si = 0;
                for (String gk : hit.glyphChoices) {
                    try {
                        org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def gd = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.get(gk);
                        var gl = new java.util.ArrayList<net.kyori.adventure.text.Component>();
                        if (gd != null) gl.add(net.kyori.adventure.text.Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                        String act = "glyph_pickup_select:" + gk;
                        sel.setItem(si, org.bysenom.minecraftSurvivors.gui.GuiTheme.createAction(MinecraftSurvivors.getInstance(), gd != null ? gd.icon : org.bukkit.Material.PAPER, net.kyori.adventure.text.Component.text(gd != null ? gd.name : gk).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, act, false));
                        try { MinecraftSurvivors.getInstance().getLogger().fine("Glyph pickup option shown: " + gk + " for player=" + p.getName()); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                    si++; if (si >= sel.getSize()) break;
                }
                sel.setItem(8, org.bysenom.minecraftSurvivors.gui.GuiTheme.createAction(MinecraftSurvivors.getInstance(), org.bukkit.Material.BARRIER, net.kyori.adventure.text.Component.text("Abbrechen").color(net.kyori.adventure.text.format.NamedTextColor.RED), java.util.List.of(net.kyori.adventure.text.Component.text("Schließe dieses Menü")), "back", false));
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(p.getUniqueId(), true);
                org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionContext(p.getUniqueId(), hit.abilityKey, 0);
                p.openInventory(sel);
                try { MinecraftSurvivors.getInstance().getLogger().fine("Opened glyph pickup selection for player=" + p.getName() + " choices=" + hit.glyphChoices); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
    }

    // Hilfs-API zum Spawnen via Config oder Events
    public static void spawnRandomGlyphNear(Location around, String abilityKey) {
        if (around == null) return; World w = around.getWorld(); if (w == null) return;
        java.util.Random r = new java.util.Random();
        Location loc = around.clone().add(r.nextDouble()*4-2, 0, r.nextDouble()*4-2);
        spawnGlyph(loc, abilityKey);
    }

    // Selection open helpers
    public static void setSelectionOpen(java.util.UUID playerUuid, boolean open) {
        if (playerUuid == null) return;
        if (open) SELECTION_OPEN.add(playerUuid); else SELECTION_OPEN.remove(playerUuid);
        try { MinecraftSurvivors pl = MinecraftSurvivors.getInstance(); if (pl != null) pl.getLogger().fine("SelectionOpen set for " + playerUuid + " -> " + open); } catch (Throwable ignored) {}
    }
    public static boolean isSelectionOpen(java.util.UUID playerUuid) { if (playerUuid == null) return false; return SELECTION_OPEN.contains(playerUuid); }

    public static void setSelectionContext(java.util.UUID playerUuid, String abilityKey, int slot) {
        if (playerUuid == null) return;
        java.util.Map<String,Object> m = new java.util.HashMap<>();
        m.put("ability", abilityKey);
        m.put("slot", slot);
        SELECTION_CTX.put(playerUuid, m);
    }
    public static java.util.Map<String,Object> consumeSelectionContext(java.util.UUID playerUuid) { if (playerUuid == null) return null; return SELECTION_CTX.remove(playerUuid); }
    public static void clearSelectionContext(java.util.UUID playerUuid) { if (playerUuid == null) return; SELECTION_CTX.remove(playerUuid); }

    public static void markSelectionHandled(java.util.UUID playerUuid) { if (playerUuid == null) return; SELECTION_HANDLED.add(playerUuid); }
    public static boolean consumeSelectionHandled(java.util.UUID playerUuid) { if (playerUuid == null) return false; return SELECTION_HANDLED.remove(playerUuid); }

    // Prevent reopen window for a short period after selection (timestamp expiry)
    public static void setNoReopenFor(java.util.UUID playerUuid, int seconds) {
        if (playerUuid == null) return;
        long until = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        NO_REOPEN_UNTIL.put(playerUuid, until);
    }
    public static boolean isNoReopen(java.util.UUID playerUuid) {
        if (playerUuid == null) return false;
        Long t = NO_REOPEN_UNTIL.get(playerUuid);
        if (t == null) return false;
        if (System.currentTimeMillis() > t) { NO_REOPEN_UNTIL.remove(playerUuid); return false; }
        return true;
    }
    public static void clearNoReopen(java.util.UUID playerUuid) { if (playerUuid == null) return; NO_REOPEN_UNTIL.remove(playerUuid); }
}
