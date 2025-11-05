// File: src/main/java/org/bysenom/minecraftSurvivors/gui/GuiClickListener.java
package org.bysenom.minecraftSurvivors.gui;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.PlayerClass;
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
            case "start":
                guiManager.getGameManager().startGameWithCountdown(5);
                player.closeInventory();
                player.sendMessage("§aStart in 5 Sekunden...");
                break;
            case "class":
                guiManager.openClassSelection(player);
                break;
            case "party":
                guiManager.openPartyMenu(player);
                break;
            case "stats":
                guiManager.openStatsMenu(player);
                break;
            case "config":
                if (!player.hasPermission("minecraftsurvivors.admin")) { player.sendMessage("§cKeine Berechtigung für Config."); return; }
                guiManager.openConfigMenu(player);
                break;
            case "party_create":
                if (plugin.getPartyManager().createParty(player.getUniqueId())) player.sendMessage("§aParty erstellt."); else player.sendMessage("§cDu bist bereits in einer Party.");
                guiManager.openPartyMenu(player);
                break;
            case "party_join_invite":
                java.util.UUID leader = plugin.getPartyManager().getPendingInviteLeader(player.getUniqueId());
                if (leader != null && plugin.getPartyManager().join(player.getUniqueId(), leader)) player.sendMessage("§aEinladung angenommen."); else player.sendMessage("§cKeine gültige Einladung.");
                guiManager.openPartyMenu(player);
                break;
            case "party_leave":
                if (plugin.getPartyManager().leave(player.getUniqueId())) player.sendMessage("§aParty verlassen/aufgelöst."); else player.sendMessage("§cDu bist in keiner Party.");
                guiManager.openPartyMenu(player);
                break;
            case "stats_mode_actionbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.ACTIONBAR);
                player.sendMessage("§aStats-Modus: ActionBar");
                break;
            case "stats_mode_bossbar":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.BOSSBAR);
                player.sendMessage("§aStats-Modus: BossBar");
                break;
            case "stats_mode_scoreboard":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD);
                player.sendMessage("§aStats-Modus: Scoreboard");
                break;
            case "stats_mode_off":
                plugin.getStatsDisplayManager().setMode(org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.OFF);
                player.sendMessage("§aStats-Modus: Aus");
                break;
            case "config_reload":
                plugin.getGameManager().reloadConfigAndApply();
                player.sendMessage("§aConfig neu geladen.");
                break;
            case "config_preset_flashy":
                guiManager.applyPreset("flashy");
                player.sendMessage("§aPreset 'flashy' angewendet.");
                break;
            case "config_preset_epic":
                guiManager.applyPreset("epic");
                player.sendMessage("§aPreset 'epic' angewendet.");
                break;
            case "back":
                guiManager.openMainMenu(player);
                break;
            case "info":
                guiManager.openInfoMenu(player);
                break;
            case "info_close":
                player.closeInventory();
                break;
            case "status":
                guiManager.openInfoMenu(player);
                break;
            case "select_shaman":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(PlayerClass.SHAMAN);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: " + PlayerClass.SHAMAN.getDisplayName());
                break;
            case "powerup":
                player.closeInventory();
                player.sendMessage("§dPowerup-Auswahl folgt später.");
                break;
            case "select_pyromancer":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Pyromant");
                break;
            case "select_ranger":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Waldläufer");
                break;
            case "select_paladin":
                plugin.getPlayerManager().get(player.getUniqueId()).setSelectedClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN);
                player.closeInventory();
                player.sendMessage("§aKlasse gewählt: Paladin");
                break;
            case "party_invite_list":
                guiManager.openPartyInviteList(player);
                break;
            default:
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
                } else if (action.equals("party_back")) {
                    guiManager.openPartyMenu(player);
                } else {
                    break;
                }
                break;
        }
    }
}
