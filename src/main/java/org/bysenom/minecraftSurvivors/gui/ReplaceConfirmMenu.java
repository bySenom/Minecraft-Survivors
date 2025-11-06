package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

public class ReplaceConfirmMenu {
    private final Player player;
    private final SurvivorPlayer sp;
    private final int idx;
    private final String newKey;
    private final int newLevel;
    private final double rarity;
    private final Inventory inv;
    private final NamespacedKey key;
    private static final java.util.concurrent.ConcurrentMap<java.util.UUID, Intent> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private static class Intent { final int idx; final String newKey; final int newLevel; final double rarity; Intent(int idx,String newKey,int newLevel,double rarity){this.idx=idx;this.newKey=newKey;this.newLevel=newLevel;this.rarity=rarity;} }

    public ReplaceConfirmMenu(Player player, SurvivorPlayer sp, int idx, String newKey, int newLevel, double rarity) {
        this.player = player;
        this.sp = sp;
        this.idx = idx;
        this.newKey = newKey;
        this.newLevel = newLevel;
        this.rarity = rarity;
        this.key = new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_replace_confirm");
        this.inv = Bukkit.createInventory(null, 9, GuiTheme.styledTitle("Bestätigen", "Ability ersetzen"));
        setup();
        PENDING.put(player.getUniqueId(), new Intent(idx, newKey, newLevel, rarity));
    }

    private void setup() {
        // Cancel (left) - Confirm (right)
        ItemStack cancel = GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.RED_STAINED_GLASS_PANE, Component.text("Abbrechen").color(NamedTextColor.RED), java.util.List.of(Component.text("Klick zum Abbrechen").color(NamedTextColor.GRAY)), "cancel", true);
        ItemStack info = GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.PAPER, Component.text("Replace?").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Ersetze Slot #"+idx).color(NamedTextColor.GRAY), Component.text("Mit: "+newKey).color(NamedTextColor.AQUA)), null, false);
        ItemStack confirm = GuiTheme.createAction(MinecraftSurvivors.getInstance(), Material.GREEN_STAINED_GLASS_PANE, Component.text("Bestätigen").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Klick zum Bestätigen").color(NamedTextColor.GRAY)), "confirm", true);
        try { org.bukkit.inventory.meta.ItemMeta m0 = cancel.getItemMeta(); if (m0!=null) { m0.getPersistentDataContainer().set(key, PersistentDataType.STRING, "cancel"); cancel.setItemMeta(m0);} } catch (Throwable ignored) {}
        try { org.bukkit.inventory.meta.ItemMeta m1 = confirm.getItemMeta(); if (m1!=null) { m1.getPersistentDataContainer().set(key, PersistentDataType.STRING, "confirm"); confirm.setItemMeta(m1);} } catch (Throwable ignored) {}
        inv.setItem(2, cancel);
        inv.setItem(4, info);
        inv.setItem(6, confirm);
    }

    public void open() {
        try { player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);} catch (Throwable ignored) {}
        player.openInventory(inv);
    }

    public static class Listener implements org.bukkit.event.Listener {
        private final MinecraftSurvivors plugin;
        public Listener(MinecraftSurvivors plugin) { this.plugin = plugin; }
        @org.bukkit.event.EventHandler
        public void onClick(InventoryClickEvent e) {
            String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title());
            if (!title.toLowerCase(java.util.Locale.ROOT).contains("confirm replace")) return;
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem(); if (it == null || !it.hasItemMeta()) return;
            ItemMeta m = it.getItemMeta(); if (m == null) return;
            String action = m.getPersistentDataContainer().get(new NamespacedKey(plugin, "ms_replace_confirm"), PersistentDataType.STRING);
            if (action == null) return;
            Player p = (Player) e.getWhoClicked();
            // We need to retrieve original context: we stored it not in PDC; instead, we encode info in the inventory title? Simpler: scan player's open top inventory to find original ReplaceAbilityMenu info.
            Inventory top = e.getView().getTopInventory();
            // Find the ReplaceAbilityMenu info item (slot 4 in replace menu was item 13 earlier) - but we don't have direct mapping. Instead: we will attempt to infer newKey/newLevel by scanning top inv for a PAPER with ms_new_ability_key; the ReplaceConfirmMenu was opened directly from ReplaceAbilityMenu, so the ReplaceAbilityMenu still may be open in background. We'll just re-use the title to locate a pending replace stored in plugin's ephemeral map (but we didn't store). To keep it simple, perform action via player's metadata: the ReplaceConfirmMenu should be ephemeral; we'll use the clicked inventory to find confirm/cancel and simply close and trigger a delayed replacement via the ReplaceAbilityMenu's static methods.
            // For simplicity: if confirm -> run a silent replacement using data encoded in the info item of ReplaceConfirmMenu (slot 4 lore). We'll parse the lore line.
            String titlePlain = title.toLowerCase(java.util.Locale.ROOT);
            if (action.equals("cancel")) {
                try { p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.8f); } catch (Throwable ignored) {}
                p.closeInventory();
                return;
            }
            if (action.equals("confirm")) {
                // retrieve intent
                Intent intent = PENDING.remove(p.getUniqueId());
                p.closeInventory();
                if (intent == null) return;
                SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
                if (sp == null) return;
                // Perform replacement
                boolean ok = sp.replaceAbilityAt(intent.idx, intent.newKey, intent.newLevel);
                if (ok) {
                    sp.setAbilityOrigin(intent.newKey, "levelup");
                    try { p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f); p.sendActionBar(Component.text("Ability ersetzt!").color(NamedTextColor.GREEN)); } catch (Throwable ignored) {}
                    try { p.updateInventory(); } catch (Throwable ignored) {}
                }
            }
        }
    }
}
