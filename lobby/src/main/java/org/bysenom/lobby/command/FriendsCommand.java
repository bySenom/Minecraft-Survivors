package org.bysenom.lobby.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.friend.FriendManager;
import org.jetbrains.annotations.NotNull;

public class FriendsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Nur ingame."); return true; }
        if (args.length == 0) { help(p); return true; }
        switch (args[0].toLowerCase()) {
            case "list":
                java.util.List<String> names = LobbySystem.get().getFriendManager().listFriendNames(p.getUniqueId());
                if (names.isEmpty()) p.sendMessage("§7[Friends] Keine Einträge.");
                else p.sendMessage("§a[Friends] " + String.join(", ", names));
                return true;
            case "pending":
                java.util.List<String> pend = LobbySystem.get().getFriendManager().listPendingNames(p.getUniqueId());
                if (pend.isEmpty()) p.sendMessage("§7[Friends] Keine offenen Anfragen.");
                else p.sendMessage("§e[Friends] Offene: " + String.join(", ", pend));
                return true;
            case "invite":
                if (args.length < 2) { p.sendMessage("§c/friends invite <Name>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { p.sendMessage("§cSpieler nicht online."); return true; }
                FriendManager.RequestResult res = LobbySystem.get().getFriendManager().sendRequest(p.getUniqueId(), target.getUniqueId());
                switch (res) {
                    case SUCCESS:
                        p.sendMessage("§aAnfrage an §f" + target.getName() + " §agesendet.");
                        // Clickable Chat für Empfänger
                        Component msg = Component.text("[Friends] ", NamedTextColor.GRAY)
                                .append(Component.text(p.getName(), NamedTextColor.AQUA))
                                .append(Component.text(" möchte mit dir befreundet sein. ", NamedTextColor.GRAY))
                                .append(Component.text("[Annehmen]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/friends accept " + p.getName())))
                                .append(Component.text(" "))
                                .append(Component.text("[Ablehnen]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/friends deny " + p.getName())));
                        target.sendMessage(msg);
                        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
                        return true;
                    case ALREADY_FRIENDS:
                        p.sendMessage("§eIhr seid bereits befreundet."); return true;
                    case BLOCKED:
                        p.sendMessage("§cAnfrage blockiert."); return true;
                    case SELF:
                        p.sendMessage("§cDu kannst dir nicht selbst eine Anfrage senden."); return true;
                    case DUPLICATE:
                        p.sendMessage("§eAnfrage existiert bereits."); return true;
                }
                return true;
            case "accept":
                if (args.length < 2) { p.sendMessage("§c/friends accept <Name>"); return true; }
                OfflinePlayer acc = Bukkit.getOfflinePlayer(args[1]);
                if (acc.getName() == null && !acc.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean okAcc = LobbySystem.get().getFriendManager().acceptRequest(p.getUniqueId(), acc.getUniqueId());
                p.sendMessage(okAcc ? "§aAnfrage angenommen." : "§eKeine passende Anfrage gefunden.");
                if (okAcc && acc.isOnline()) {
                    Player op = acc.getPlayer();
                    if (op != null) op.sendMessage("§a" + p.getName() + " hat deine Freundschaftsanfrage angenommen.");
                }
                return true;
            case "deny":
                if (args.length < 2) { p.sendMessage("§c/friends deny <Name>"); return true; }
                OfflinePlayer dn = Bukkit.getOfflinePlayer(args[1]);
                if (dn.getName() == null && !dn.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean okDn = LobbySystem.get().getFriendManager().denyRequest(p.getUniqueId(), dn.getUniqueId());
                p.sendMessage(okDn ? "§eAnfrage abgelehnt." : "§eKeine passende Anfrage gefunden.");
                return true;
            case "add":
                if (args.length < 2) { p.sendMessage("§c/friends add <Name>"); return true; }
                OfflinePlayer add = Bukkit.getOfflinePlayer(args[1]);
                if (add.getName() == null && !add.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean ok = LobbySystem.get().getFriendManager().addFriend(p.getUniqueId(), add.getUniqueId());
                p.sendMessage(ok ? "§aHinzugefügt: " + (add.getName() != null ? add.getName() : add.getUniqueId()) : "§eBereits in der Liste oder blockiert.");
                return true;
            case "remove":
                if (args.length < 2) { p.sendMessage("§c/friends remove <Name>"); return true; }
                OfflinePlayer rem = Bukkit.getOfflinePlayer(args[1]);
                if (rem.getName() == null && !rem.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean rm = LobbySystem.get().getFriendManager().removeFriend(p.getUniqueId(), rem.getUniqueId());
                p.sendMessage(rm ? "§aEntfernt: " + (rem.getName() != null ? rem.getName() : rem.getUniqueId()) : "§eNicht in der Liste.");
                return true;
            case "block":
                if (args.length < 2) { p.sendMessage("§c/friends block <Name>"); return true; }
                OfflinePlayer bl = Bukkit.getOfflinePlayer(args[1]);
                if (bl.getName() == null && !bl.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean b = LobbySystem.get().getFriendManager().block(p.getUniqueId(), bl.getUniqueId());
                p.sendMessage(b ? "§aBlockiert: " + (bl.getName() != null ? bl.getName() : bl.getUniqueId()) : "§eBereits blockiert.");
                return true;
            case "unblock":
                if (args.length < 2) { p.sendMessage("§c/friends unblock <Name>"); return true; }
                OfflinePlayer ub = Bukkit.getOfflinePlayer(args[1]);
                if (ub.getName() == null && !ub.hasPlayedBefore()) { p.sendMessage("§cUnbekannter Spieler"); return true; }
                boolean ubok = LobbySystem.get().getFriendManager().unblock(p.getUniqueId(), ub.getUniqueId());
                p.sendMessage(ubok ? "§aEntblockt: " + (ub.getName() != null ? ub.getName() : ub.getUniqueId()) : "§eWar nicht blockiert.");
                return true;
            default:
                help(p); return true;
        }
    }

    private void help(Player p) {
        p.sendMessage("§bFriends: /friends list|pending|invite <Name>|accept <Name>|deny <Name>|add <Name>|remove <Name>|block <Name>|unblock <Name>");
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return java.util.Arrays.asList("list", "pending", "invite", "accept", "deny", "add", "remove", "block", "unblock");
        if (args.length == 2 && ("invite".equalsIgnoreCase(args[0]) || "accept".equalsIgnoreCase(args[0]) || "deny".equalsIgnoreCase(args[0]) || "add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]) || "block".equalsIgnoreCase(args[0]) || "unblock".equalsIgnoreCase(args[0]))) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
        }
        return java.util.Collections.emptyList();
    }
}
