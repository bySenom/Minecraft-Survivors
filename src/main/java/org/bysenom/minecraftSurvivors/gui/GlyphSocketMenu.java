package org.bysenom.minecraftSurvivors.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.ability.AbilityCatalog;
import org.bysenom.minecraftSurvivors.glyph.GlyphCatalog;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Simple Glyph Socket UI: shows ability info and 3 socket slots (click to open glyph selection).
 * Actions are encoded via PDC using GuiTheme.createAction (ms_gui key) and handled in GuiClickListener.
 */
public class GlyphSocketMenu {
    private final SurvivorPlayer sp;
    private final String abilityKey;
    private final java.util.UUID playerUuid;

    public GlyphSocketMenu(Player player, SurvivorPlayer sp, String abilityKey) {
        this.playerUuid = player != null ? player.getUniqueId() : null;
        this.sp = sp;
        this.abilityKey = abilityKey;
        String title = "Glyphen-Sockel";
        AbilityCatalog.Def def = AbilityCatalog.get(abilityKey);
        if (def != null) title = "Glyphen: " + def.display;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(title).color(NamedTextColor.LIGHT_PURPLE));
        setup(inv);
        player.openInventory(inv);
    }

    private void setup(Inventory inv) {
        // border
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == (inv.getSize() / 9 - 1) || col == 0 || col == 8) {
                inv.setItem(i, GuiTheme.borderItem(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        // center info
        AbilityCatalog.Def def = AbilityCatalog.get(abilityKey);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Klicke auf einen Sockel, um eine Glyph auszuwählen").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        // show pending glyph if present
        try {
            if (playerUuid != null) {
                String pending = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.getPendingGlyph(playerUuid);
                if (pending != null) lore.add(Component.text("Pending Glyph: "+pending).color(NamedTextColor.AQUA));
            }
        } catch (Throwable ignored) {}
        if (def != null) lore.addAll(def.buildLore(sp, sp.getAbilityLevel(abilityKey)));
        inv.setItem(13, GuiTheme.createAction(MinecraftSurvivors.getInstance(), def != null ? def.icon : Material.PAPER,
                Component.text(def != null ? def.display : abilityKey).color(NamedTextColor.AQUA), lore, null, true));

        // socket slots: 10,12,14
        int[] slots = new int[]{10, 12, 14};
        List<String> glyphs = sp.getGlyphs(abilityKey);
        for (int i = 0; i < 3; i++) {
            String gk = i < glyphs.size() ? glyphs.get(i) : null;
            if (gk != null && !gk.isEmpty()) {
                GlyphCatalog.Def gd = GlyphCatalog.get(gk);
                List<Component> gl = new ArrayList<>();
                if (gd != null) gl.add(Component.text(gd.desc).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                String action = "glyph_socket:" + abilityKey + ":" + i; // clicking opens selection/replace menu
                inv.setItem(slots[i], GuiTheme.createAction(MinecraftSurvivors.getInstance(), gd != null ? gd.icon : Material.PAPER,
                        Component.text(gd != null ? gd.name : gk).color(net.kyori.adventure.text.format.NamedTextColor.GOLD), gl, action, true));
            } else {
                List<Component> gl = new ArrayList<>();
                gl.add(Component.text("Klick zum Einsetzen einer Glyph").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                String action = "glyph_socket:" + abilityKey + ":" + i;
                inv.setItem(slots[i], GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.LIGHT_BLUE_STAINED_GLASS,
                        Component.text("SOCKEL: EMPTY").color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY), gl, action, false));
            }
        }

        // footer: close
        inv.setItem(22, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.ARROW,
                Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Schließt das Menü").color(NamedTextColor.GRAY)), "back", false));
    }

    // Listener inner class so the main plugin can register it: GlyphSocketMenu.Listener
    public static final class Listener implements org.bukkit.event.Listener {
        private final MinecraftSurvivors plugin;
        private final NamespacedKey key;

        public Listener(MinecraftSurvivors plugin) {
            this.plugin = plugin;
            this.key = new NamespacedKey(plugin, "ms_gui");
        }

        @org.bukkit.event.EventHandler
        public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
            try {
                org.bukkit.inventory.Inventory top = e.getView().getTopInventory();
                if (top == null) return;
                // If the closed inventory is the glyph selection UI, reopen the socket menu and keep paused
                String title = "";
                try { java.lang.Object t = e.getView().getTitle(); if (t != null) title = t.toString(); } catch (Throwable ignored) {}
                if (title.contains("Wähle Glyph") || title.contains("Wähle Glyphe")) {
                    try {
                        // mark selection as closed
                        org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.setSelectionOpen(e.getPlayer().getUniqueId(), false);
                        java.util.Map<String,Object> ctx = org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.consumeSelectionContext(e.getPlayer().getUniqueId());
                        if (ctx != null) {
                            String ability = (String) ctx.get("ability");
                            // reopen socket menu for this ability
                            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(e.getPlayer().getUniqueId());
                            try { new org.bysenom.minecraftSurvivors.gui.GlyphSocketMenu((org.bukkit.entity.Player)e.getPlayer(), sp, ability); } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                    return;
                }

                // detect if this inventory is a GlyphSocket menu by scanning for glyph_socket actions in PDC
                boolean isSocket = false;
                for (int i = 0; i < top.getSize(); i++) {
                    org.bukkit.inventory.ItemStack it = top.getItem(i);
                    if (it == null || !it.hasItemMeta()) continue;
                    org.bukkit.inventory.meta.ItemMeta md = it.getItemMeta();
                    if (md == null) continue;
                    String action = md.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    if (action != null && action.startsWith("glyph_socket:")) { isSocket = true; break; }
                }
                if (isSocket) {
                    // Only resume if the player does NOT currently have selection UI open
                    if (!org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.isSelectionOpen(e.getPlayer().getUniqueId())) {
                        try { plugin.getGameManager().resumeForPlayer(e.getPlayer().getUniqueId()); } catch (Throwable ignored) {}
                    } else {
                        // If selection still open, keep paused and do nothing
                    }
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
