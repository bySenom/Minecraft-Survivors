package org.bysenom.lobby.listener;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.UiManager;

public class UiClickListener implements Listener {
    private final UiManager ui;
    public UiClickListener(org.bysenom.lobby.UiManager ui) { this.ui = ui; }

    private String plain(net.kyori.adventure.text.Component c) {
        return c == null ? "" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
    }

    private void fx(org.bukkit.entity.Player p) {
        try {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            p.spawnParticle(Particle.CRIT, p.getLocation().add(0, 1.1, 0), 4, 0.1, 0.1, 0.1, 0.0);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = plain(e.getView().title());
        org.bukkit.inventory.ItemStack it = e.getCurrentItem();
        if (it == null || it.getItemMeta() == null) {
            if (title.startsWith("Navigator") || title.startsWith("Profil") || title.startsWith("Party") || title.startsWith("Freunde") || title.startsWith("Cosmetics") || title.startsWith("Lobby")) {
                e.setCancelled(true);
            }
            return;
        }
        e.setCancelled(true);
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
        String name = plain(it.getItemMeta().displayName());
        // PDC Action Dispatch
        String action = null;
        try {
            action = it.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(LobbySystem.get(), "nav_action"), org.bukkit.persistence.PersistentDataType.STRING);
        } catch (Throwable ignored) {}

        if (title.startsWith("Navigator")) {
            fx(p);
            if (action == null) action = "";
            switch (action) {
                case "nav_survivors": p.performCommand("queue join"); return;
                case "nav_profile": LobbySystem.get().getNavigatorManager().openProfile(p); return;
                case "nav_party": LobbySystem.get().getNavigatorManager().openParty(p); return;
                case "nav_friends": LobbySystem.get().getNavigatorManager().openFriends(p); return;
                case "nav_friends_pending": LobbySystem.get().getNavigatorManager().openFriendsPending(p); return;
                case "nav_cosmetics": LobbySystem.get().getNavigatorManager().openCosmetics(p); return;
                case "nav_close": p.closeInventory(); return;
                default:
                    // Legacy Fallback
                    if (name.contains("Minecraft Survivors")) p.performCommand("queue join");
                    else if (name.contains("Profil")) LobbySystem.get().getNavigatorManager().openProfile(p);
                    else if (name.contains("Party")) LobbySystem.get().getNavigatorManager().openParty(p);
                    else if (name.contains("Freunde")) LobbySystem.get().getNavigatorManager().openFriends(p);
                    else if (name.contains("Cosmetics")) LobbySystem.get().getNavigatorManager().openCosmetics(p);
                    else if (name.contains("Offene Anfragen")) LobbySystem.get().getNavigatorManager().openFriendsPending(p);
                    else if (name.contains("Schließen")) p.closeInventory();
                    return;
            }
        }
        if (title.startsWith("Party • Spieler einladen") || title.contains("einladen")) {
            fx(p);
            if (action != null && action.startsWith("party_invite_player:")) {
                String targetName = action.substring("party_invite_player:".length());
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    try { LobbySystem.get().getPartyBridge().invite(p, target, 60); p.sendMessage("§aInvite an §e" + target.getName() + " §agesendet."); } catch (Throwable ignored) {}
                } else p.sendMessage("§eSpieler nicht online: " + targetName);
                return;
            }
            if ("party_invite_back".equals(action)) { LobbySystem.get().getNavigatorManager().openParty(p); return; }
            return;
        }
        if (title.startsWith("Party")) {
            fx(p);
            if ("party_back".equals(action)) { LobbySystem.get().getNavigatorManager().openNavigator(p); return; }
            if ("party_leave".equals(action)) { try { LobbySystem.get().getPartyBridge().leave(p); } catch (Throwable ignored) {} p.closeInventory(); return; }
            if ("party_invite_gui".equals(action)) { LobbySystem.get().getNavigatorManager().openPartyInvite(p); return; }
            return;
        }
        if (title.startsWith("Freunde • Anfragen")) {
            fx(p);
            if (action != null && action.startsWith("friends_pending_player:")) {
                String target = action.substring("friends_pending_player:".length());
                java.util.UUID targetId = null; try { org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(target); targetId = op.getUniqueId(); } catch (Throwable ignored) {}
                if (targetId != null) {
                    boolean shift = e.isShiftClick();
                    if (shift) {
                        if (LobbySystem.get().getFriendManager().denyRequest(p.getUniqueId(), targetId)) p.sendMessage("§cAnfrage abgelehnt: §e" + target);
                    } else {
                        if (LobbySystem.get().getFriendManager().acceptRequest(p.getUniqueId(), targetId)) p.sendMessage("§aJetzt befreundet mit §e" + target);
                    }
                    LobbySystem.get().getNavigatorManager().openFriendsPending(p);
                }
            }
            if ("friends_pending_back".equals(action)) { LobbySystem.get().getNavigatorManager().openFriends(p); return; }
            return;
        }
        if (title.startsWith("Freunde")) {
            fx(p);
            if ("friends_back".equals(action)) { LobbySystem.get().getNavigatorManager().openNavigator(p); return; }
            if ("friends_pending".equals(action)) { LobbySystem.get().getNavigatorManager().openFriendsPending(p); return; }
            return;
        }
        if (title.startsWith("Cosmetics")) {
            fx(p);
            if ("cos_back".equals(action)) { LobbySystem.get().getNavigatorManager().openNavigator(p); return; }
            int slot = e.getRawSlot();
            if (slot == 10) { toggleCos(p, "trail_sparkles", "Trail"); LobbySystem.get().getNavigatorManager().openCosmetics(p); }
            else if (slot == 12) { toggleCos(p, "kill_fire", "Kill-Effekt"); LobbySystem.get().getNavigatorManager().openCosmetics(p); }
            else if (slot == 14) { toggleCos(p, "emote_wave", "Emote"); LobbySystem.get().getNavigatorManager().openCosmetics(p); }
            return;
        }
        if (title.startsWith("Lobby")) {
            fx(p);
            if (name.contains("Queue beitreten")) p.performCommand("queue join");
            else if (name.contains("Queue verlassen")) p.performCommand("queue leave");
            else if (name.contains("Survivors-Men")) p.performCommand("msmenu");
            else if (name.contains("Friends")) ui.openFriendsMenu(p);
            else if (name.contains("Cosmetics")) ui.openCosmeticsMenu(p);
            else if (name.contains("Party")) ui.openPartyMenu(p);
            else if (name.contains("Aktualisieren")) ui.openLobbyMenu(p);
        }
    }

    private void toggleCos(org.bukkit.entity.Player p, String key, String label) {
        try {
            boolean has = LobbySystem.get().getCosmeticManager().isUnlocked(p.getUniqueId(), key);
            if (has) {
                LobbySystem.get().getCosmeticManager().revoke(p.getUniqueId(), key);
                p.sendMessage("§cDeaktiviert: §f" + label + " §7(" + key + ")");
            } else {
                LobbySystem.get().getCosmeticManager().unlock(p.getUniqueId(), key);
                p.sendMessage("§aAktiviert: §f" + label + " §7(" + key + ")");
            }
        } catch (Throwable ignored) {}
    }
}
