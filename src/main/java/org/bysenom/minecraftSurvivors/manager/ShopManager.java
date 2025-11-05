package org.bysenom.minecraftSurvivors.manager;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import java.time.*;
import java.util.*;

public class ShopManager {
    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin;

    public ShopManager(org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public List<Map<?, ?>> getDailyOffersDet(String path, int maxItems) {
        FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        List<Map<?, ?>> all = cfg.getMapList(path);
        if (all == null || all.isEmpty()) return Collections.emptyList();
        // Deterministic RNG per day
        String key = LocalDate.now(ZoneId.systemDefault()) + "|" + path;
        long seed = key.hashCode();
        Random r = new Random(seed);
        List<Map<?, ?>> copy = new ArrayList<>(all);
        Collections.shuffle(copy, r);
        if (maxItems <= 0 || maxItems >= copy.size()) return copy;
        return new ArrayList<>(copy.subList(0, maxItems));
    }

    public long millisUntilMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).toMillis();
    }

    public String formatRemainingHHMMSS() {
        long ms = millisUntilMidnight();
        long s = ms / 1000L;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public boolean applyPurchaseEffect(org.bukkit.entity.Player p, org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp, String type, double value) {
        switch (type.toUpperCase()) {
            case "DAMAGE_MULT": sp.addDamageMult(value); return true;
            case "RADIUS_MULT": sp.addRadiusMult(value); return true;
            case "PALADIN_HEAL": sp.addHealBonus(value); return true;
            default: return false;
        }
    }

    public boolean applyPurchaseGear(org.bukkit.entity.Player p, org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp, String type, java.util.Map<?, ?> node) {
        org.bukkit.Material mat;
        org.bukkit.inventory.EquipmentSlot slot;
        switch (type.toUpperCase()) {
            case "ARMOR_HELMET": mat = org.bukkit.Material.IRON_HELMET; slot = org.bukkit.inventory.EquipmentSlot.HEAD; break;
            case "ARMOR_CHEST": mat = org.bukkit.Material.IRON_CHESTPLATE; slot = org.bukkit.inventory.EquipmentSlot.CHEST; break;
            case "ARMOR_LEGS": mat = org.bukkit.Material.IRON_LEGGINGS; slot = org.bukkit.inventory.EquipmentSlot.LEGS; break;
            case "ARMOR_BOOTS": mat = org.bukkit.Material.IRON_BOOTS; slot = org.bukkit.inventory.EquipmentSlot.FEET; break;
            default: return false;
        }
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String rarity = node.containsKey("rarity") ? String.valueOf(node.get("rarity")) : "common";
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        Object statsObj = node.get("stats");
        if (statsObj instanceof java.util.Map<?, ?>) {
            for (Map.Entry<?, ?> e : ((java.util.Map<?, ?>)statsObj).entrySet()) {
                stats.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        // Lore & Name
        net.kyori.adventure.text.format.NamedTextColor color;
        switch (rarity.toLowerCase()) {
            case "rare": color = net.kyori.adventure.text.format.NamedTextColor.AQUA; break;
            case "epic": color = net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE; break;
            case "legendary": color = net.kyori.adventure.text.format.NamedTextColor.GOLD; break;
            default: color = net.kyori.adventure.text.format.NamedTextColor.WHITE; break;
        }
        String display = node.containsKey("name") ? String.valueOf(node.get("name")) : type;
        meta.displayName(net.kyori.adventure.text.Component.text(display).color(color));
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("Seltenheit: "+rarity).color(color));
        // Apply enchants and attributes based on stats
        int prot = parseInt(stats.get("prot"), 0);
        if (prot > 0) {
            try { meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, Math.min(4, prot), true); } catch (Throwable ignored) {}
            lore.add(net.kyori.adventure.text.Component.text("Schutz "+prot).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
        double hearts = parseDouble(stats.get("health"), 0.0); // hearts
        if (hearts != 0.0) {
            try {
                org.bukkit.attribute.AttributeModifier hpMod = new org.bukkit.attribute.AttributeModifier(java.util.UUID.randomUUID(), "ms:hp", hearts * 2.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, slot);
                meta.addAttributeModifier(org.bukkit.attribute.Attribute.MAX_HEALTH, hpMod);
            } catch (Throwable ignored) {}
            lore.add(net.kyori.adventure.text.Component.text("+"+hearts+"❤").color(net.kyori.adventure.text.format.NamedTextColor.RED));
        }
        double speed = parseDouble(stats.get("speed"), 0.0); // scalar add
        if (speed != 0.0) {
            try {
                org.bukkit.attribute.AttributeModifier spMod = new org.bukkit.attribute.AttributeModifier(java.util.UUID.randomUUID(), "ms:speed", speed, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR, slot);
                meta.addAttributeModifier(org.bukkit.attribute.Attribute.MOVEMENT_SPEED, spMod);
            } catch (Throwable ignored) {}
            lore.add(net.kyori.adventure.text.Component.text("+"+(int)(speed*100)+"% Speed").color(net.kyori.adventure.text.format.NamedTextColor.BLUE));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        switch (type.toUpperCase()) {
            case "ARMOR_HELMET": p.getInventory().setHelmet(item); break;
            case "ARMOR_CHEST": p.getInventory().setChestplate(item); break;
            case "ARMOR_LEGS": p.getInventory().setLeggings(item); break;
            case "ARMOR_BOOTS": p.getInventory().setBoots(item); break;
        }
        return true;
    }

    private int parseInt(Object o, int def) { try { return o==null?def:Integer.parseInt(String.valueOf(o)); } catch (Throwable t) { return def; } }
    private double parseDouble(Object o, double def) { try { return o==null?def:Double.parseDouble(String.valueOf(o)); } catch (Throwable t) { return def; } }
}
