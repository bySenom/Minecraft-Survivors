package org.bysenom.minecraftSurvivors.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootchestListener implements Listener {

    private final MinecraftSurvivors plugin;
    private final NamespacedKey chestKey;
    private final Random rnd = new Random();

    private static class WeightedReward { final String type; final double value; final String name; final int weight; WeightedReward(String t,double v,String n,int w){type=t;value=v;name=n;weight=w;} }

    public LootchestListener(MinecraftSurvivors plugin) {
        this.plugin = plugin;
        this.chestKey = new NamespacedKey(plugin, "lootchest");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        ItemStack it = e.getItem();
        if (!it.hasItemMeta()) return;
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        Byte flag = pdc.get(chestKey, PersistentDataType.BYTE);
        if (flag == null || flag == 0) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        openSlotmachine(p, it);
    }

    private void openSlotmachine(Player p, ItemStack usedChest) {
        int stepsBase = Math.max(16, plugin.getConfigUtil().getInt("lootchest.roll-steps", 28));
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Lootchest").color(NamedTextColor.GOLD));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        p.openInventory(inv);
        usedChest.setAmount(Math.max(0, usedChest.getAmount() - 1));
        if (usedChest.getAmount() <= 0) {
            // auto remove from hand
            try { p.getInventory().setItemInMainHand(null); } catch (Throwable ignored) {}
        }

        List<WeightedReward> pool = loadWeightedRewards();
        // Build three reels by repeating entries according to weight (length ~ 32)
        List<WeightedReward> reelA = buildReel(pool, 32);
        List<WeightedReward> reelB = buildReel(pool, 36);
        List<WeightedReward> reelC = buildReel(pool, 40);
        WeightedReward hit = pickWeighted(pool);

        java.util.concurrent.atomic.AtomicInteger finished = new java.util.concurrent.atomic.AtomicInteger(0);
        Runnable onAllDone = () -> {
            // Render final triple and apply once
            renderFinal(inv, hit);
            applyReward(p, toMap(hit));
            try { p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f); } catch (Throwable ignored) {}
            Bukkit.getScheduler().runTaskLater(plugin, (Runnable) () -> p.closeInventory(), 20L);
        };

        // Start reels with staggered total steps and easing delay
        spinReel(inv, 11, reelA, rnd.nextInt(reelA.size()), stepsBase + 0, 2, 1, hit, finished, onAllDone);
        spinReel(inv, 13, reelB, rnd.nextInt(reelB.size()), stepsBase + 6, 2, 1, hit, finished, onAllDone);
        spinReel(inv, 15, reelC, rnd.nextInt(reelC.size()), stepsBase + 12, 2, 1, hit, finished, onAllDone);
    }

    private List<WeightedReward> buildReel(List<WeightedReward> pool, int desired) {
        List<WeightedReward> reel = new ArrayList<>(desired);
        // Guarantee at least one of each, then fill weighted
        for (WeightedReward wr : pool) reel.add(wr);
        int total = pool.stream().mapToInt(w -> Math.max(1, w.weight)).sum();
        while (reel.size() < desired) {
            int r = rnd.nextInt(Math.max(1, total)); int cur = 0;
            for (WeightedReward w : pool) { cur += Math.max(1, w.weight); if (r < cur) { reel.add(w); break; } }
        }
        return reel;
    }

    private void spinReel(Inventory inv, int slot, List<WeightedReward> reel, int pos, int steps, int delay, int delayInc, WeightedReward finalSymbol, java.util.concurrent.atomic.AtomicInteger finished, Runnable onAllDone) {
        if (steps <= 0) {
            // force final symbol icon
            inv.setItem(slot, iconFor(finalSymbol));
            if (finished.incrementAndGet() >= 3) onAllDone.run();
            return;
        }
        // display current symbol
        WeightedReward wr = reel.get(Math.floorMod(pos, reel.size()));
        inv.setItem(slot, iconFor(wr));
        int nextPos = pos + 1;
        int nextSteps = steps - 1;
        int nextDelay = delay + (nextSteps < 8 ? delayInc : (nextSteps < 14 ? (delayInc/2+1) : 0)); // ease-out tail
        // if we are near the end, align to final symbol to guarantee ending on it
        if (nextSteps == 0) {
            inv.setItem(slot, iconFor(finalSymbol));
            if (finished.incrementAndGet() >= 3) onAllDone.run();
            return;
        }
        // schedule next tick with dynamic delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> spinReel(inv, slot, reel, nextPos, nextSteps, Math.min(8, nextDelay), delayInc, finalSymbol, finished, onAllDone), Math.max(1L, nextDelay));
    }

    private List<WeightedReward> loadWeightedRewards() {
        List<Map<?, ?>> list = plugin.getConfigUtil().getConfig().getMapList("lootchest.rewards");
        List<WeightedReward> out = new ArrayList<>();
        for (Map<?, ?> m : list) {
            Object oType = m.containsKey("type") ? m.get("type") : "UNKNOWN";
            Object oVal = m.containsKey("value") ? m.get("value") : 0;
            Object oName = m.containsKey("name") ? m.get("name") : null;
            Object oW = m.containsKey("weight") ? m.get("weight") : 1;
            String type = String.valueOf(oType);
            double value = Double.parseDouble(String.valueOf(oVal));
            String name = oName != null ? String.valueOf(oName) : type;
            int weight = Integer.parseInt(String.valueOf(oW));
            if (weight > 0) out.add(new WeightedReward(type, value, name, weight));
        }
        if (out.isEmpty()) out.add(new WeightedReward("DAMAGE_MULT", 0.1, "Attackpower +10%", 1));
        return out;
    }

    private WeightedReward pickWeighted(List<WeightedReward> list) {
        int total = 0; for (WeightedReward w : list) total += Math.max(0, w.weight);
        int r = rnd.nextInt(Math.max(1, total)); int cur = 0;
        for (WeightedReward w : list) { cur += Math.max(0, w.weight); if (r < cur) return w; }
        return list.get(list.size()-1);
    }

    private Map<?, ?> toMap(WeightedReward wr) {
        java.util.Map<String,Object> m = new java.util.HashMap<>();
        m.put("type", wr.type); m.put("value", wr.value); m.put("name", wr.name);
        return m;
    }

    private ItemStack iconFor(WeightedReward wr) {
        Material m;
        switch (wr.type.toUpperCase()) {
            case "DAMAGE_MULT": m = Material.BLAZE_POWDER; break;
            case "RADIUS_MULT": m = Material.HEART_OF_THE_SEA; break;
            case "PALADIN_HEAL": m = Material.GOLDEN_APPLE; break;
            default: m = Material.PAPER; break;
        }
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            NamedTextColor c = wr.weight >= 5 ? NamedTextColor.GOLD : (wr.weight >= 2 ? NamedTextColor.AQUA : NamedTextColor.WHITE);
            im.displayName(Component.text(wr.name).color(c));
            it.setItemMeta(im);
        }
        return it;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView() == null) return;
        // Vermeide getTitle()-Deprecation: Vergleiche Name des Top-Inventars über die beim Erstellen bekannte Kennung
        org.bukkit.inventory.Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        // Wir erkennen unser Lootchest-Inv daran, dass alle drei Mittelslots (11/13/15) nicht null sind und Rand gefüllt ist
        // Zusätzlich: Prüfe, ob der Titel-Component unsere bekannte Zeichenfolge enthält
        String titleStr = String.valueOf(e.getView().title()); // Adventure Component -> String
        boolean looksLikeLoot = titleStr != null && titleStr.toLowerCase().contains("lootchest");
        if (!looksLikeLoot) return;
        e.setCancelled(true);
    }

    private void fill(Inventory inv, Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta im = pane.getItemMeta();
        if (im != null) {
            im.displayName(Component.text(" "));
            pane.setItemMeta(im);
        }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private void tickRoll(Inventory inv) {
        // spin three center slots (11, 13, 15)
        ItemStack[] icons = new ItemStack[] {
                icon(Material.BLAZE_POWDER, "DMG"), icon(Material.BOW, "RNG"), icon(Material.GOLDEN_APPLE, "HEAL")
        };
        inv.setItem(11, icons[rnd.nextInt(icons.length)]);
        inv.setItem(13, icons[rnd.nextInt(icons.length)]);
        inv.setItem(15, icons[rnd.nextInt(icons.length)]);
    }

    private ItemStack icon(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Component.text(name).color(NamedTextColor.AQUA));
            it.setItemMeta(im);
        }
        return it;
    }

    private void applyReward(Player p, Map<?, ?> m) {
        if (m == null) { p.sendMessage("§eDie Kiste war leer…"); return; }
        Object oType = m.containsKey("type") ? m.get("type") : "UNKNOWN";
        Object oValue = m.containsKey("value") ? m.get("value") : 0;
        Object oName = m.containsKey("name") ? m.get("name") : null;
        String type = String.valueOf(oType);
        double val = Double.parseDouble(String.valueOf(oValue));
        String nm = oName != null ? String.valueOf(oName) : type;
        SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        switch (type.toUpperCase()) {
            case "DAMAGE_MULT": sp.addDamageMult(val); break;
            case "RADIUS_MULT": sp.addRadiusMult(val); break;
            case "PALADIN_HEAL": sp.addHealBonus(val); break;
            default: p.sendMessage("§eUnbekannte Belohnung: "+type); return;
        }
        try { p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f); } catch (Throwable ignored) {}
        p.sendMessage("§aGewonnen: "+nm+" §7("+val+")");
    }

    private void renderFinal(Inventory inv, WeightedReward wr) {
        ItemStack it = iconFor(wr);
        try { inv.setItem(11, it); } catch (Throwable ignored) {}
        try { inv.setItem(13, it); } catch (Throwable ignored) {}
        try { inv.setItem(15, it); } catch (Throwable ignored) {}
    }
}
