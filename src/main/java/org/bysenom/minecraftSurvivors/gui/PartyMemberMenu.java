package org.bysenom.minecraftSurvivors.gui;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Small context menu for party member actions (Promote/Transfer/Kick/Message)
 */
public class PartyMemberMenu {

    private final Player viewer;
    private final UUID target;
    private final Inventory inv;

    public PartyMemberMenu(Player viewer, UUID target) {
        this.viewer = viewer;
        this.target = target;
        String name = Bukkit.getOfflinePlayer(target).getName();
        this.inv = Bukkit.createInventory(null, 9, GuiTheme.styledTitle("Party: "+(name==null?target.toString():name), "Mitglied Aktionen"));
        setup();
    }

    private void setup() {
        // border
        GuiTheme.safeLore(null);
        for (int i=0;i<inv.getSize();i++) inv.setItem(i, GuiTheme.borderItem(Material.BLACK_STAINED_GLASS_PANE));
        String name = Bukkit.getOfflinePlayer(target).getName();
        // Info center
        ItemStack info = GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.PLAYER_HEAD, Component.text(name==null?target.toString():name).color(NamedTextColor.AQUA), java.util.List.of(Component.text("Klicke die Aktion an").color(NamedTextColor.GRAY)), null, false);
        inv.setItem(4, info);
        // Promote / Transfer / Kick / Message
        inv.setItem(1, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.DIAMOND, Component.text("Promote to Leader").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Mache diesen Spieler zum Leader (nur aktueller Leader)")), "party_promote:"+target, true));
        inv.setItem(3, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.BEACON, Component.text("Transfer Leadership").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Transferiere Leadership an diesen Spieler")), "party_transfer:"+target, true));
        inv.setItem(5, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.BARRIER, Component.text("Kick Member").color(NamedTextColor.RED), java.util.List.of(Component.text("Entferne diesen Spieler aus der Party")), "party_kick:"+target, true));
        inv.setItem(7, GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.PAPER, Component.text("Nachricht senden").color(NamedTextColor.WHITE), java.util.List.of(Component.text("Öffne privaten Chat mit dem Spieler")), "party_msg:"+target, false));
    }

    public void open() {
        try { viewer.playSound(viewer.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f); } catch (Throwable ignored) {}
        viewer.openInventory(inv);
    }
}
