// java
package org.bysenom.minecraftSurvivors.gui;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public class LevelUpMenu {

    private final Inventory inv;
    private final int level;
    private final Random random = new Random();

    private final org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp;
    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance();
    private final org.bukkit.NamespacedKey rarKey = new org.bukkit.NamespacedKey(plugin, "ms_rarmult");
    private final org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(plugin, "ms_ability_pick");

    public LevelUpMenu(org.bukkit.entity.Player player, org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp, int level) {
        this.sp = sp;
        this.level = level;
        this.inv = Bukkit.createInventory(null, 27, org.bysenom.minecraftSurvivors.gui.GuiTheme.styledTitle("Level Up", "Wähle Belohnung (Lvl "+level+")"));
        setupItems();
    }

    private enum Rarity { COMMON, RARE, EPIC }
    private static Rarity rollRarity(java.util.Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 10) return Rarity.EPIC; // 10%
        if (r < 35) return Rarity.RARE; // 25%
        return Rarity.COMMON;           // 65%
    }

    private double multByRarity(Rarity rar) {
        switch (rar) {
            case EPIC: return 1.5;
            case RARE: return 1.2;
            default: return 1.0;
        }
    }

    private void setupItems() {
        java.util.List<org.bukkit.inventory.ItemStack> options = new java.util.ArrayList<>();

        // Rarities
        Rarity rar1 = rollRarity(random); double mult1 = multByRarity(rar1);
        Rarity rar2 = rollRarity(random); double mult2 = multByRarity(rar2);
        Rarity rar3 = rollRarity(random); double mult3 = multByRarity(rar3);

        // Abilities-Pool: include all abilities by default (no unlock required)
        java.util.List<String> pool = new java.util.ArrayList<>();
        for (org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Def d : org.bysenom.minecraftSurvivors.ability.AbilityCatalog.all()) {
            if (d != null && d.key != null) pool.add(d.key);
        }
        java.util.Collections.shuffle(pool, random);
        java.util.List<String> picks = pool.subList(0, Math.min(3, pool.size()));
        double[] rms = new double[]{mult1, mult2, mult3};
        Rarity[] rrs = new Rarity[]{rar1, rar2, rar3};
        for (int i=0;i<picks.size();i++) {
            String key = picks.get(i);
            org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Def def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(key);
            if (def == null) continue;
            int currentLvl = sp != null ? Math.max(1, sp.getAbilityLevel(key)) : 1;
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("✦ "+rarLabel(rms[i])+" ✦").color(rarColor(rms[i])));
            if (def.desc != null && !def.desc.isEmpty()) lore.add(net.kyori.adventure.text.Component.text(def.desc).color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
            lore.add(net.kyori.adventure.text.Component.text("§8———"));
            lore.addAll(def.buildDeltaLore(sp, currentLvl, rms[i]));
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(def.icon);
            java.util.List<net.kyori.adventure.text.Component> fullLore = new java.util.ArrayList<>();
            fullLore.add(net.kyori.adventure.text.Component.text("✦ ").append(GuiTheme.rarityLabel(rms[i])).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            fullLore.addAll(lore);
            fullLore.add(net.kyori.adventure.text.Component.text("§8———"));
            fullLore.add(net.kyori.adventure.text.Component.text("LEVEL: "+currentLvl, net.kyori.adventure.text.format.NamedTextColor.AQUA));
            // use GuiTheme createAction through a temporary NamespacedKey action - LevelUp picks don't need a persistent action key here
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(def.display).color(rarColor(rms[i])));
                meta.lore(fullLore);
                meta.getPersistentDataContainer().set(abilityKey, org.bukkit.persistence.PersistentDataType.STRING, key);
                meta.getPersistentDataContainer().set(rarKey, org.bukkit.persistence.PersistentDataType.STRING, String.valueOf(rms[i]));
                try { if (rrs[i] != Rarity.COMMON) { meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true); meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);} } catch (Throwable ignored) {}
                item.setItemMeta(meta);
            }
            options.add(item);
        }

        int choices = Math.min(3, 1 + (level / 2));
        java.util.List<org.bukkit.inventory.ItemStack> chosen = new java.util.ArrayList<>();
        java.util.List<Integer> idxs = java.util.List.of(11, 13, 15); // center row nicer layout
        while (chosen.size() < Math.min(choices, options.size()) && !options.isEmpty()) {
            int pick = random.nextInt(options.size());
            chosen.add(options.remove(pick));
        }
        // fill decorative center with a headline
        inv.setItem(4, GuiTheme.borderItem(Material.PURPLE_STAINED_GLASS));
        inv.setItem(22, GuiTheme.borderItem(Material.PURPLE_STAINED_GLASS));
        for (int i = 0; i < chosen.size(); i++) {
            int slot = idxs.get(i);
            inv.setItem(slot, chosen.get(i));
        }
        // Fill border and some flourish
        // Use GuiTheme border via GuiManager.fillBorder when opened
    }

    private net.kyori.adventure.text.format.NamedTextColor rarColor(double m) { return m >= 1.5 ? net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE : (m >= 1.2 ? net.kyori.adventure.text.format.NamedTextColor.AQUA : net.kyori.adventure.text.format.NamedTextColor.WHITE); }
    private String rarLabel(double m) { return m >= 1.5 ? "EPIC" : (m >= 1.2 ? "RARE" : "COMMON"); }

    public Inventory getInventory() { return inv; }
    public int getLevel() { return level; }
}
