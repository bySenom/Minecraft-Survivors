package org.bysenom.minecraftSurvivors.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class LevelUpMenuListener implements Listener {

    private final GuiManager guiManager;

    public LevelUpMenuListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;

        e.setCancelled(true);
        ItemStack display = e.getCurrentItem();
        if (display == null) return;

        Player player = (Player) e.getWhoClicked();
        // Früh Knockback (leicht reduziert Radius/Stärke) + temporärer Schutz
        try { guiManager.getGameManager().getSpawnManager().repelMobsAround(player, 6.0, 1.0, true); try { int t = Math.max(1, guiManager.getGameManager().getPlugin().getConfigUtil().getInt("spawn.repel-protect-ticks", 12)); guiManager.getGameManager().protectPlayer(player.getUniqueId(), t); } catch(Throwable ignored){} } catch (Throwable ignored) {}

        // Versuche die Level-Nummer aus dem Titel zu parsen: "... (Level X)"
        int level = 1;
        try {
            int idx = title.toLowerCase(java.util.Locale.ROOT).lastIndexOf("level ");
            if (idx >= 0) {
                String sub = title.substring(idx + "level ".length()).trim();
                String num = sub.replaceAll("[^0-9]", "");
                if (!num.isEmpty()) {
                    level = Integer.parseInt(num);
                }
            }
        } catch (Exception ignored) {
        }

        if (display.getItemMeta() != null) {
            var meta = display.getItemMeta();
            var pdc = meta.getPersistentDataContainer();
            if (pdc.has(new org.bukkit.NamespacedKey(org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance(), "ms_stat_pick"), org.bukkit.persistence.PersistentDataType.STRING)) {
                String raw = pdc.get(new org.bukkit.NamespacedKey(org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance(), "ms_stat_pick"), org.bukkit.persistence.PersistentDataType.STRING);
                try {
                    String[] parts = raw.split(":" );
                    org.bysenom.minecraftSurvivors.model.StatType st = org.bysenom.minecraftSurvivors.model.StatType.valueOf(parts[0]);
                    double val = Double.parseDouble(parts[1]);
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer spObj = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getPlayerManager().get(player.getUniqueId());
                    if (spObj != null) {
                        org.bysenom.minecraftSurvivors.model.StatModifier mod = new org.bysenom.minecraftSurvivors.model.StatModifier(st, val, "levelup:"+st.name());
                        spObj.addStatModifier(mod);
                        player.sendMessage("§a+"+val+" " + st.name());
                    }
                } catch (Throwable ignored) {}
                guiManager.getGameManager().resumeForPlayer(player.getUniqueId());
                player.closeInventory();
                guiManager.getGameManager().tryOpenNextQueued(player.getUniqueId());
                return;
            }
        }

        if (display.getItemMeta() != null && display.getItemMeta().hasDisplayName()) {
            guiManager.handleLevelChoice(player, display, level);
        } else {
            guiManager.handleLevelChoice(player, display.getType().name(), level);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;
        try {
            if (guiManager != null && guiManager.getGameManager() != null) {
                java.util.UUID uuid = e.getPlayer().getUniqueId();
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
                // Späterer Knockback abgeschwächt oder ganz entfernt; hier entfernt um Doppel-Effekt zu vermeiden
                // guiManager.getGameManager().getSpawnManager().repelMobsAround(p, 8.0, 1.2, true);
                guiManager.getGameManager().resumeForPlayer(uuid);
                guiManager.getGameManager().tryOpenNextQueued(uuid);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.toLowerCase(java.util.Locale.ROOT).contains("level up")) return;
        try {
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            p.spawnParticle(org.bukkit.Particle.ENCHANT, p.getLocation().add(0,1.4,0), 24, 0.4,0.4,0.4, 0.02);
        } catch (Throwable ignored) {}
    }
}
