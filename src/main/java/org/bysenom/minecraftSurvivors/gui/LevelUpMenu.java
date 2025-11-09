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
    private final org.bukkit.NamespacedKey statKey = new org.bukkit.NamespacedKey(plugin, "ms_stat_pick");

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
        // After ability options prepared, also prepare stat picks
        java.util.List<org.bysenom.minecraftSurvivors.model.StatType> statPool = java.util.Arrays.asList(
                org.bysenom.minecraftSurvivors.model.StatType.MAX_HEALTH,
                org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN,
                org.bysenom.minecraftSurvivors.model.StatType.SHIELD,
                org.bysenom.minecraftSurvivors.model.StatType.ARMOR,
                org.bysenom.minecraftSurvivors.model.StatType.EVASION,
                org.bysenom.minecraftSurvivors.model.StatType.LIFESTEAL,
                org.bysenom.minecraftSurvivors.model.StatType.THORNS,
                org.bysenom.minecraftSurvivors.model.StatType.CRIT_CHANCE,
                org.bysenom.minecraftSurvivors.model.StatType.CRIT_DAMAGE,
                org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_COUNT,
                org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_BOUNCE,
                org.bysenom.minecraftSurvivors.model.StatType.ATTACK_SPEED,
                org.bysenom.minecraftSurvivors.model.StatType.SIZE,
                org.bysenom.minecraftSurvivors.model.StatType.DURATION,
                org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ELITE_BOSS,
                org.bysenom.minecraftSurvivors.model.StatType.KNOCKBACK,
                org.bysenom.minecraftSurvivors.model.StatType.JUMP_HEIGHT,
                org.bysenom.minecraftSurvivors.model.StatType.XP_GAIN,
                org.bysenom.minecraftSurvivors.model.StatType.ELITE_SPAWN_INCREASE,
                org.bysenom.minecraftSurvivors.model.StatType.POWERUP_MULT
        );
        java.util.Collections.shuffle(statPool, random);
        int statChoices = Math.min(3, 1 + (level / 3));
        java.util.List<org.bysenom.minecraftSurvivors.model.StatType> statPicks = statPool.subList(0, Math.min(statChoices, statPool.size()));
        java.util.List<Integer> statSlots = java.util.List.of(2, 6, 18); // flank positions
        for (int i=0;i<statPicks.size();i++) {
            org.bysenom.minecraftSurvivors.model.StatType st = statPicks.get(i);
            double baseVal = baseValueFor(st);
            Rarity rar = rollRarity(random);
            double mult = multByRarity(rar);
            double value = round2(baseVal * mult);
            org.bukkit.Material icon = iconFor(st);
            String display = displayFor(st);
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("✦ "+rar.name()+" ✦").color(rarColor(mult)));
            lore.add(net.kyori.adventure.text.Component.text(shortDescFor(st, value)).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            lore.add(net.kyori.adventure.text.Component.text("§8———"));
            lore.add(net.kyori.adventure.text.Component.text("Wert: "+value, net.kyori.adventure.text.format.NamedTextColor.AQUA));
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(icon);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(display).color(rarColor(mult)));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(statKey, org.bukkit.persistence.PersistentDataType.STRING, st.name()+":"+value);
                item.setItemMeta(meta);
            }
            int slot = statSlots.get(i);
            if (inv.getItem(slot) == null) inv.setItem(slot, item); // don't overwrite ability border if present
        }
        // Fill border and some flourish
        // Use GuiTheme border via GuiManager.fillBorder when opened
    }

    private net.kyori.adventure.text.format.NamedTextColor rarColor(double m) { return m >= 1.5 ? net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE : (m >= 1.2 ? net.kyori.adventure.text.format.NamedTextColor.AQUA : net.kyori.adventure.text.format.NamedTextColor.WHITE); }
    private String rarLabel(double m) { return m >= 1.5 ? "EPIC" : (m >= 1.2 ? "RARE" : "COMMON"); }

    public Inventory getInventory() { return inv; }
    public int getLevel() { return level; }

    private double baseValueFor(org.bysenom.minecraftSurvivors.model.StatType st) {
        return switch (st) {
            case MAX_HEALTH -> 1.0; // hearts bonus (converted later)
            case HP_REGEN -> 0.6; // hp/sec
            case SHIELD -> 4.0;
            case ARMOR -> 0.05;
            case EVASION -> 0.05;
            case LIFESTEAL -> 0.04;
            case THORNS -> 0.08;
            case CRIT_CHANCE -> 0.06;
            case CRIT_DAMAGE -> 0.25;
            case PROJECTILE_COUNT -> 1.0;
            case PROJECTILE_BOUNCE -> 1.0;
            case ATTACK_SPEED -> 0.07;
            case SIZE -> 0.08;
            case DURATION -> 0.10;
            case DAMAGE_ELITE_BOSS -> 0.12;
            case KNOCKBACK -> 0.25;
            case JUMP_HEIGHT -> 0.15;
            case XP_GAIN -> 0.12;
            case ELITE_SPAWN_INCREASE -> 0.10;
            case POWERUP_MULT -> 0.05;
            default -> 0.05;
        };
    }
    private String displayFor(org.bysenom.minecraftSurvivors.model.StatType st) { return switch (st) {
        case MAX_HEALTH -> "Max Health";
        case HP_REGEN -> "HP Regen";
        case SHIELD -> "Shield";
        case ARMOR -> "Armor";
        case EVASION -> "Evasion";
        case LIFESTEAL -> "Lifesteal";
        case THORNS -> "Thorns";
        case CRIT_CHANCE -> "Crit Chance";
        case CRIT_DAMAGE -> "Crit Damage";
        case PROJECTILE_COUNT -> "Projectile Count";
        case PROJECTILE_BOUNCE -> "Projectile Bounce";
        case ATTACK_SPEED -> "Attack Speed";
        case SIZE -> "Size";
        case DURATION -> "Duration";
        case DAMAGE_ELITE_BOSS -> "Vs Elite/Boss";
        case KNOCKBACK -> "Knockback";
        case JUMP_HEIGHT -> "Jump Height";
        case XP_GAIN -> "XP Gain";
        case ELITE_SPAWN_INCREASE -> "Elite Spawn";
        case POWERUP_MULT -> "Powerup Mult";
        default -> st.name();
    }; }
    private org.bukkit.Material iconFor(org.bysenom.minecraftSurvivors.model.StatType st) { return switch (st) {
        case MAX_HEALTH -> org.bukkit.Material.APPLE;
        case HP_REGEN -> org.bukkit.Material.GHAST_TEAR;
        case SHIELD -> org.bukkit.Material.SHIELD;
        case ARMOR -> org.bukkit.Material.IRON_CHESTPLATE;
        case EVASION -> org.bukkit.Material.FEATHER;
        case LIFESTEAL -> org.bukkit.Material.RED_DYE;
        case THORNS -> org.bukkit.Material.CACTUS;
        case CRIT_CHANCE -> org.bukkit.Material.ARROW;
        case CRIT_DAMAGE -> org.bukkit.Material.SPECTRAL_ARROW;
        case PROJECTILE_COUNT -> org.bukkit.Material.FIREWORK_ROCKET;
        case PROJECTILE_BOUNCE -> org.bukkit.Material.SLIME_BALL;
        case ATTACK_SPEED -> org.bukkit.Material.GOLDEN_SWORD;
        case SIZE -> org.bukkit.Material.SLIME_BLOCK;
        case DURATION -> org.bukkit.Material.CLOCK;
        case DAMAGE_ELITE_BOSS -> org.bukkit.Material.NETHERITE_SCRAP;
        case KNOCKBACK -> org.bukkit.Material.PISTON;
        case JUMP_HEIGHT -> org.bukkit.Material.RABBIT_FOOT;
        case XP_GAIN -> org.bukkit.Material.EXPERIENCE_BOTTLE;
        case ELITE_SPAWN_INCREASE -> org.bukkit.Material.WITHER_ROSE;
        case POWERUP_MULT -> org.bukkit.Material.ENCHANTED_BOOK;
        default -> org.bukkit.Material.BOOK;
    }; }
    private String shortDescFor(org.bysenom.minecraftSurvivors.model.StatType st, double v) { return switch (st) {
        case MAX_HEALTH -> "+"+v+" Herz(e)";
        case HP_REGEN -> "+"+v+" HP/s";
        case SHIELD -> "+"+v+" Schild";
        case ARMOR -> "+"+percentOne(v)+" weniger Schaden";
        case EVASION -> "+"+percentOne(v)+" Ausweichchance";
        case LIFESTEAL -> "+"+percentOne(v)+" Lifesteal";
        case THORNS -> "+"+percentOne(v)+" Rückschaden";
        case CRIT_CHANCE -> "+"+percentOne(v)+" Crit";
        case CRIT_DAMAGE -> "+"+percentOne(v)+" Crit Dmg Mult";
        case PROJECTILE_COUNT -> "+"+((int)Math.round(v))+" Projektil(e)";
        case PROJECTILE_BOUNCE -> "+"+((int)Math.round(v))+" Bounce";
        case ATTACK_SPEED -> "+"+percentOne(v)+" Angriffsrate";
        case SIZE -> "+"+percentOne(v)+" AoE Größe";
        case DURATION -> "+"+percentOne(v)+" Effektdauer";
        case DAMAGE_ELITE_BOSS -> "+"+percentOne(v)+" Elite/Boss Schaden";
        case KNOCKBACK -> "+"+percentOne(v)+" Knockback";
        case JUMP_HEIGHT -> "+"+percentOne(v)+" Sprunghöhe";
        case XP_GAIN -> "+"+percentOne(v)+" XP";
        case ELITE_SPAWN_INCREASE -> "+"+percentOne(v)+" Elite Spawn";
        case POWERUP_MULT -> "+"+percentOne(v)+" Powerup Mult";
        default -> "+"+v;
    }; }
    private String percent(double v){ return (int)Math.round(v*100.0)+"%"; }
    // Show percent with one decimal place for clearer UI
    private String percentOne(double v){ return String.format(java.util.Locale.ROOT, "%.1f%%", v * 100.0); }
    private double round2(double v){ return Math.round(v*100.0)/100.0; }
}
