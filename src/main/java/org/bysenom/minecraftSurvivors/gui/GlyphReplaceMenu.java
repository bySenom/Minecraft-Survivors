package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.glyph.GlyphCatalog;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * UI to replace an existing glyph of an ability with a newly picked glyph when sockets are full.
 */
public class GlyphReplaceMenu {
    private final Player player;
    private final SurvivorPlayer sp;
    private final String abilityKey;
    private final String newGlyphKey;
    private final Inventory inv;

    public GlyphReplaceMenu(Player player, SurvivorPlayer sp, String abilityKey, String newGlyphKey) {
        this.player = player;
        this.sp = sp;
        this.abilityKey = abilityKey;
        this.newGlyphKey = newGlyphKey;
        String title = "Replace Glyph";
        try { var def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(abilityKey); if (def != null) title = "Ersetze Glyphe: " + def.display; } catch (Throwable ignored) {}
        this.inv = Bukkit.createInventory(null, 27, Component.text(title).color(NamedTextColor.LIGHT_PURPLE));
        setup();
        player.openInventory(inv);
    }

    private void setup() {
        // border
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9; int col = i % 9;
            if (row == 0 || row == (inv.getSize()/9 -1) || col == 0 || col == 8) inv.setItem(i, GuiTheme.borderItem(Material.GRAY_STAINED_GLASS_PANE));
        }
        // center info
        var def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(abilityKey);
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("Neue Glyphe: ").color(NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.text(newGlyphKey).color(NamedTextColor.AQUA));
        inv.setItem(13, GuiTheme.createAction(MinecraftSurvivors.getInstance(), def != null && def.icon != null ? def.icon : Material.PAPER, Component.text(def != null ? def.display : abilityKey).color(NamedTextColor.AQUA), lore, null, true));

        int[] slots = new int[]{10,12,14};
        java.util.List<String> glyphs = sp.getGlyphs(abilityKey);
        for (int i = 0; i < 3; i++) {
            String gk = i < glyphs.size() ? glyphs.get(i) : null;
            if (gk != null && !gk.isEmpty()) {
                var gd = GlyphCatalog.get(gk);
                var item = GuiTheme.createAction(MinecraftSurvivors.getInstance(), gd != null && gd.icon != null ? gd.icon : Material.PAPER, Component.text(gd != null ? gd.name : gk).color(NamedTextColor.GOLD), java.util.List.of(Component.text("Klicke zum Ersetzen").color(NamedTextColor.GRAY)), "glyph_replace_confirm:" + abilityKey + ":" + i + ":" + newGlyphKey, true);
                inv.setItem(slots[i], item);
            } else {
                var item = GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.LIGHT_BLUE_STAINED_GLASS, Component.text("SOCKEL: EMPTY").color(NamedTextColor.DARK_GRAY), java.util.List.of(Component.text("Kein Glyph vorhanden")), "glyph_replace_confirm:" + abilityKey + ":" + i + ":" + newGlyphKey, false);
                inv.setItem(slots[i], item);
            }
        }
        inv.setItem(22, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.BARRIER, Component.text("Abbrechen").color(NamedTextColor.RED), java.util.List.of(Component.text("Schließt dieses Menü").color(NamedTextColor.GRAY)), "back", false));
    }
}
