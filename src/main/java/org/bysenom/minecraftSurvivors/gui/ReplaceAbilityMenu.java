package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.ability.AbilityCatalog;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

public class ReplaceAbilityMenu {

    private final Inventory inv;
    private final Player player;
    private final SurvivorPlayer sp;
    private final String newAbilityKey;
    private final double rarityMult;
    private final NamespacedKey actionKey;
    private final boolean allowReplaceClass;

    public ReplaceAbilityMenu(Player player, SurvivorPlayer sp, String newAbilityKey, double rarityMult, boolean allowReplaceClass) {
        this.player = player;
        this.sp = sp;
        this.newAbilityKey = newAbilityKey;
        this.rarityMult = rarityMult;
        this.actionKey = new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_replace_pick");
        this.allowReplaceClass = allowReplaceClass;
        this.inv = Bukkit.createInventory(null, 27, Component.text("Ersetze eine Ability").color(NamedTextColor.AQUA));
        setup();
    }

    private void setup() {
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);
        // Info-Karte in der Mitte
        AbilityCatalog.Def def = AbilityCatalog.get(newAbilityKey);
        java.util.List<Component> infoLore = new java.util.ArrayList<>();
        infoLore.add(Component.text("Rarity: "+(rarityMult>=1.5?"EPIC":rarityMult>=1.2?"RARE":"COMMON")).color(rarityMult>=1.5?NamedTextColor.LIGHT_PURPLE:rarityMult>=1.2?NamedTextColor.AQUA:NamedTextColor.WHITE));
        ItemStack info = GuiTheme.createAction(MinecraftSurvivors.getInstance(), def != null ? def.icon : Material.NETHER_STAR, Component.text("Neue Ability: "+(def!=null?def.display:newAbilityKey)).color(NamedTextColor.LIGHT_PURPLE), infoLore, null, true);
        // transport key/rar/flag in PDC manually after creation
        try {
            ItemMeta im = info.getItemMeta();
            if (im != null) {
                im.getPersistentDataContainer().set(new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_new_ability_key"), PersistentDataType.STRING, newAbilityKey);
                im.getPersistentDataContainer().set(new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_new_ability_rar"), PersistentDataType.STRING, String.valueOf(rarityMult));
                im.getPersistentDataContainer().set(new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_allow_replace_class"), PersistentDataType.BYTE, (byte)(this.allowReplaceClass ? 1 : 0));
                info.setItemMeta(im);
            }
        } catch (Throwable ignored) {}
        inv.setItem(13, info);
        // Slots der vorhandenen Abilities 10/12/14/16 + 22 als Layout
        int[] slots = new int[]{10,12,14,16,22};
        for (int i=0;i<sp.getAbilities().size() && i<5;i++) {
            String k = sp.getAbilities().get(i);
            int lvl = sp.getAbilityLevel(k);
            AbilityCatalog.Def d = AbilityCatalog.get(k);
            java.util.List<Component> lore = new java.util.ArrayList<>(); lore.add(Component.text("Klicke zum Ersetzen").color(NamedTextColor.GRAY));
            ItemStack decorated = GuiTheme.createAction(MinecraftSurvivors.getInstance(), d != null ? d.icon : Material.BARRIER, Component.text((d!=null?d.display:k)+" Lv."+lvl).color(NamedTextColor.WHITE), lore, String.valueOf(i), true);
            // ensure our actionKey is also present for older listeners
            try { org.bukkit.inventory.meta.ItemMeta meta = decorated.getItemMeta(); if (meta != null) { meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, String.valueOf(i)); decorated.setItemMeta(meta); } } catch (Throwable ignored) {}
            inv.setItem(slots[i], decorated);
         }
     }

    private void fillBorder(Inventory inv, Material borderMat) {
        ItemStack border = new ItemStack(borderMat);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9; int col = i % 9;
            if (row == 0 || row == (inv.getSize()/9 -1) || col == 0 || col == 8) inv.setItem(i, border);
        }
    }

    public void open() {
        try { player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);} catch (Throwable ignored) {}
        try { player.spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0,1.4,0), 20, 0.3,0.3,0.3, 0.01);} catch (Throwable ignored) {}
        player.openInventory(inv);
    }

    public static class Listener implements org.bukkit.event.Listener {
        private final MinecraftSurvivors plugin;
        public Listener(MinecraftSurvivors plugin) { this.plugin = plugin; }
        @org.bukkit.event.EventHandler
        public void onClick(org.bukkit.event.inventory.InventoryClickEvent e) {
            String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title());
            if (!title.toLowerCase(java.util.Locale.ROOT).contains("ersetze eine ability")) return;
            e.setCancelled(true);
            ItemStack cur = e.getCurrentItem(); if (cur == null) return;
            ItemMeta md = cur.getItemMeta(); if (md == null) return;
            String idxStr = md.getPersistentDataContainer().get(new NamespacedKey(plugin, "ms_replace_pick"), PersistentDataType.STRING);
            if (idxStr == null) return;
            int idx; try { idx = Integer.parseInt(idxStr); } catch (Throwable ex) { return; }
            Player p = (Player) e.getWhoClicked();
            SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
            String newKey = e.getView().getTopInventory().getItem(13) != null ? getKeyFromInfo(e.getView().getTopInventory().getItem(13)) : null;
            double rar = 1.0; try { rar = Double.parseDouble(getRarityFromInfo(e.getView().getTopInventory().getItem(13))); } catch (Throwable ignored) {}
            if (newKey == null) return;
            int newLevelBase = 1;
            if (rar >= 1.5) newLevelBase += 2; else if (rar >= 1.2) newLevelBase += 1; // Rarity-bonus auf Level
            // Check protection: don't allow replacing class abilities if not permitted
            String oldKey = null; try { oldKey = sp.getAbilities().get(idx); } catch (Throwable ignored) {}
            if (oldKey != null) {
                String origin = sp.getAbilityOrigin(oldKey);
                // read allowReplaceClass from the info item in the top inventory
                boolean allow = false;
                ItemStack infoItem = e.getView().getTopInventory().getItem(13);
                if (infoItem != null && infoItem.getItemMeta() != null) {
                    Byte b = infoItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "ms_allow_replace_class"), PersistentDataType.BYTE);
                    allow = (b != null && b == 1);
                }
                if ("class".equals(origin) && !allow) {
                    try { p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.8f); p.sendActionBar(Component.text("Diese Ability ist Klassen-gebunden und kann hier nicht ersetzt werden").color(NamedTextColor.RED)); } catch (Throwable ignored) {}
                    return;
                }
            }
            // Open confirmation UI before replacing
            new ReplaceConfirmMenu(p, sp, idx, newKey, newLevelBase, rar).open();
            return;

         }
        private static String getKeyFromInfo(ItemStack info) {
            if (info == null || info.getItemMeta()==null) return null;
            return info.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_new_ability_key"), PersistentDataType.STRING);
        }
        private static String getRarityFromInfo(ItemStack info) {
            if (info == null || info.getItemMeta()==null) return "1.0";
            String s = info.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(MinecraftSurvivors.getInstance(), "ms_new_ability_rar"), PersistentDataType.STRING);
            return s != null ? s : "1.0";
        }
        @org.bukkit.event.EventHandler
        public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
            String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title());
            if (!title.toLowerCase(java.util.Locale.ROOT).contains("ersetze eine ability")) return;
            try { plugin.getGameManager().resumeForPlayer(e.getPlayer().getUniqueId()); plugin.getGameManager().tryOpenNextQueued(e.getPlayer().getUniqueId()); } catch (Throwable ignored) {}
        }
    }
}
