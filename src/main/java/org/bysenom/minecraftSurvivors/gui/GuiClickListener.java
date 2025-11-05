// File: src/main/java/org/bysenom/minecraftSurvivors/gui/GuiClickListener.java
package org.bysenom.minecraftSurvivors.gui;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class GuiClickListener implements Listener {

    private final GuiManager guiManager;
    private final NamespacedKey key;
    private final MinecraftSurvivors plugin;

    public GuiClickListener(MinecraftSurvivors plugin, GuiManager guiManager) {
        this.guiManager = guiManager;
        this.key = new NamespacedKey(plugin, "ms_gui");
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String action = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (action == null) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();

        switch (action) {
            case "start_wizard":
                guiManager.openClassSelection(player);
                return;
            case "start":
                player.sendMessage("§eBitte nutze 'Spiel starten' und wähle eine Klasse.");
                return;
            // Klassenwahl-Actions (wiederhergestellt)
            case "select_shaman_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN);
                guiManager.openSkillSelection(player);
                return;
            case "select_pyromancer_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER);
                guiManager.openSkillSelection(player);
                return;
            case "select_ranger_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER);
                guiManager.openSkillSelection(player);
                return;
            case "select_paladin_wizard":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN);
                guiManager.openSkillSelection(player);
                return;
            case "party":
                guiManager.openPartyMenu(player);
                return;
            case "stats":
                guiManager.openStatsMenu(player);
                return;
            case "config":
                if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung für Config."); return; }
                guiManager.openConfigMenu(player);
                return;
            case "admin":
                guiManager.openAdminPanel(player);
                return;
            case "meta":
                guiManager.openMetaMenu(player);
                return;
            case "info":
                guiManager.openInfoMenu(player);
                return;
            case "back":
                guiManager.openMainMenu(player);
                return;
            case "stats_mode_actionbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.ACTIONBAR);
                player.sendMessage("§aStats-Modus: ActionBar");
                return;
            case "stats_mode_bossbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.BOSSBAR);
                player.sendMessage("§aStats-Modus: BossBar");
                return;
            case "stats_mode_scoreboard":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD);
                player.sendMessage("§aStats-Modus: Scoreboard");
                return;
            case "stats_mode_off":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.OFF);
                player.sendMessage("§aStats-Modus: Aus");
                return;
            case "config_reload":
                plugin.getGameManager().reloadConfigAndApply();
                player.sendMessage("§aConfig neu geladen.");
                return;
            case "config_preset_flashy":
                guiManager.applyPreset("flashy");
                player.sendMessage("§aPreset 'flashy' angewendet.");
                return;
            case "config_preset_epic":
                guiManager.applyPreset("epic");
                player.sendMessage("§aPreset 'epic' angewendet.");
                return;
            case "party_create":
                if (plugin.getPartyManager().createParty(player.getUniqueId())) player.sendMessage("§aParty erstellt."); else player.sendMessage("§cDu bist bereits in einer Party.");
                guiManager.openPartyMenu(player);
                return;
            case "party_join_invite":
                java.util.UUID leader = plugin.getPartyManager().getPendingInviteLeader(player.getUniqueId());
                if (leader != null && plugin.getPartyManager().join(player.getUniqueId(), leader)) player.sendMessage("§aEinladung angenommen."); else player.sendMessage("§cKeine gültige Einladung.");
                guiManager.openPartyMenu(player);
                return;
            case "party_leave":
                if (plugin.getPartyManager().leave(player.getUniqueId())) player.sendMessage("§aParty verlassen/aufgelöst."); else player.sendMessage("§cDu bist in keiner Party.");
                guiManager.openPartyMenu(player);
                return;
            case "adm_coins":
                plugin.getPlayerManager().get(player.getUniqueId()).addCoins(100);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+100 Coins");
                guiManager.openAdminPanel(player);
                return;
            case "adm_essence":
                org.bysenom.minecraftSurvivors.model.MetaProfile mp = plugin.getMetaManager().get(player.getUniqueId());
                mp.addEssence(10);
                plugin.getMetaManager().save(mp);
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "+10 Essence");
                guiManager.openAdminPanel(player);
                return;
            case "adm_spawn":
                org.bukkit.Location loc = player.getLocation();
                for (int i=0; i<5; i++) {
                    try {
                        org.bukkit.Location s = loc.clone().add((i-2)*1.5, 0, 2.0);
                        org.bukkit.entity.LivingEntity le = (org.bukkit.entity.LivingEntity) player.getWorld().spawnEntity(s, org.bukkit.entity.EntityType.ZOMBIE);
                        plugin.getGameManager().getSpawnManager().markAsWave(le);
                    } catch (Throwable ignored) {}
                }
                org.bysenom.minecraftSurvivors.util.Msg.info(player, "5 Testmobs gespawnt");
                guiManager.openAdminPanel(player);
                return;
            case "adm_levelup":
                guiManager.openLevelUpMenu(player, 1);
                return;
            case "adm_ready_toggle": {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                sp.setReady(!sp.isReady());
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Ready: "+sp.isReady());
                guiManager.openAdminPanel(player);
                return;
            }
            case "adm_force_start": {
                guiManager.getGameManager().startGameWithCountdown(3);
                player.closeInventory();
                return;
            }
            case "adm_give_dash": {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
                if (sp.addSkill("dash")) {
                    org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Dash erhalten");
                } else {
                    org.bysenom.minecraftSurvivors.util.Msg.warn(player, "Skill-Slots voll (max "+sp.getMaxSkillSlots()+")");
                }
                guiManager.openAdminPanel(player);
                return;
            }
        }
        // Wizard actions
        if (action.startsWith("select_") && action.endsWith("_wizard")) {
            return; // bereits oben behandelt
        }
        if (action.startsWith("party_invite:")) {
            try {
                java.util.UUID target = java.util.UUID.fromString(action.substring("party_invite:".length()));
                if (plugin.getPartyManager().invite(player.getUniqueId(), target, 60)) {
                    player.sendMessage("§aEinladung gesendet.");
                } else {
                    player.sendMessage("§cInvite fehlgeschlagen (bist du Leader?).");
                }
                guiManager.openPartyInviteList(player);
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        if (action.equals("party_back")) {
            guiManager.openPartyMenu(player);
            return;
        }
        if (action.startsWith("shop_buy:")) {
            String key = action.substring("shop_buy:".length());
            guiManager.applyShopPurchase(player, key);
            guiManager.openShop(player);
            return;
        }
        if (action.startsWith("meta_buy:")) {
            String key = action.substring("meta_buy:".length());
            plugin.getMetaManager().tryPurchase(player, key);
            guiManager.openMetaMenu(player);
            return;
        }
        if (action.startsWith("skill_pick:")) {
            String pick = action.substring("skill_pick:".length());
            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
            if (!pick.equals("none")) {
                if (!sp.addSkill(pick)) {
                    org.bysenom.minecraftSurvivors.util.Msg.warn(player, "Skill-Slots voll (max "+sp.getMaxSkillSlots()+")");
                    return;
                }
                if (pick.equals("shockwave")) {
                    org.bukkit.inventory.ItemStack wand = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK);
                    org.bukkit.inventory.meta.ItemMeta meta = wand.getItemMeta();
                    if (meta != null) {
                        meta.displayName(net.kyori.adventure.text.Component.text("Shockwave Lv."+sp.getSkillLevel("shockwave")).color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "skill_shockwave"), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
                        wand.setItemMeta(meta);
                    }
                    player.getInventory().addItem(wand);
                }
                if (pick.equals("dash")) {
                    try { player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 8, 0.3, 0.1, 0.3, 0.01); } catch (Throwable ignored) {}
                    try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.6f); } catch (Throwable ignored) {}
                }
                org.bysenom.minecraftSurvivors.util.Msg.ok(player, "Skill gewählt: "+pick);
            }
            sp.setReady(true);
            boolean allReady = true;
            for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer osp = plugin.getPlayerManager().get(op.getUniqueId());
                if (osp == null || osp.getSelectedClass() == null || !osp.isReady()) { allReady = false; break; }
            }
            player.closeInventory();
            if (allReady) {
                guiManager.getGameManager().startGameWithCountdown(5);
            } else {
                org.bysenom.minecraftSurvivors.util.Msg.info(player, "Warte auf andere Spieler... (Ready "+(int)org.bukkit.Bukkit.getOnlinePlayers().stream().filter(pp->plugin.getPlayerManager().get(pp.getUniqueId()).isReady()).count()+"/"+org.bukkit.Bukkit.getOnlinePlayers().size()+")");
            }
            return;
        }
    }
}
