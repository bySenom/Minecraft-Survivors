package org.bysenom.minecraftSurvivors.command;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.PartyManager;

public class PartyCommand implements CommandExecutor {

    private final MinecraftSurvivors plugin;

    public PartyCommand(MinecraftSurvivors plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame verfügbar."); return true; }
        Player p = (Player) sender;
        PartyManager pm = plugin.getPartyManager();
        if (pm == null) { p.sendMessage("§cParty-Manager nicht verfügbar."); return true; }
        if (args.length == 0) {
            p.sendMessage("§e/party create | invite <Spieler> | join <Leader> | leave | disband | list");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                if (pm.createParty(p.getUniqueId())) p.sendMessage("§aParty erstellt."); else p.sendMessage("§cDu bist bereits in einer Party.");
                return true;
            case "disband":
                if (pm.disband(p.getUniqueId())) p.sendMessage("§aParty aufgelöst."); else p.sendMessage("§cDu bist kein Party-Leader.");
                return true;
            case "leave":
                if (pm.leave(p.getUniqueId())) p.sendMessage("§aDu hast die Party verlassen."); else p.sendMessage("§cDu bist in keiner Party.");
                return true;
            case "invite":
                if (args.length < 2) { p.sendMessage("§c/party invite <Spieler> [Sekunden]"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { p.sendMessage("§cSpieler nicht gefunden."); return true; }
                int sec = 60;
                if (args.length >= 3) {
                    try { sec = Math.max(5, Integer.parseInt(args[2])); } catch (Throwable ignored) {}
                }
                if (pm.invite(p.getUniqueId(), t.getUniqueId(), sec)) p.sendMessage("§aEinladung gesendet ("+sec+"s)."); else p.sendMessage("§cInvite fehlgeschlagen.");
                return true;
            case "join":
                if (args.length < 2) { p.sendMessage("§c/party join <Leader>"); return true; }
                Player leader = Bukkit.getPlayer(args[1]);
                if (leader == null) { p.sendMessage("§cLeader nicht gefunden."); return true; }
                if (pm.join(p.getUniqueId(), leader.getUniqueId())) p.sendMessage("§aParty beigetreten."); else p.sendMessage("§cJoin fehlgeschlagen (keine Einladung oder abgelaufen).");
                return true;
            case "accept":
                if (args.length < 2) { p.sendMessage("§c/party accept <LeaderUUID>"); return true; }
                try {
                    java.util.UUID leaderId = java.util.UUID.fromString(args[1]);
                    if (pm.join(p.getUniqueId(), leaderId)) {
                        p.sendMessage("§aEinladung angenommen.");
                    } else {
                        p.sendMessage("§cAnnahme fehlgeschlagen (keine gültige Einladung).");
                    }
                } catch (IllegalArgumentException ex) {
                    p.sendMessage("§cUngültige Leader-UUID.");
                }
                return true;
            case "decline":
                if (args.length < 2) { p.sendMessage("§c/party decline <LeaderUUID>"); return true; }
                try {
                    java.util.UUID leaderId = java.util.UUID.fromString(args[1]);
                    // Einladung ablehnen -> pending invite für diesen Spieler entfernen
                    boolean ok = pm.cancelInvite(p.getUniqueId());
                    p.sendMessage(ok ? "§7Einladung abgelehnt." : "§cKeine gültige Einladung gefunden.");
                } catch (IllegalArgumentException ex) {
                    p.sendMessage("§cUngültige Leader-UUID.");
                }
                return true;
            case "kick":
                if (args.length < 2) { p.sendMessage("§c/party kick <Spieler>"); return true; }
                Player k = Bukkit.getPlayer(args[1]);
                if (k == null) { p.sendMessage("§cSpieler nicht gefunden."); return true; }
                if (pm.getByLeader(p.getUniqueId()) == null) { p.sendMessage("§cNur der Leader kann kicken."); return true; }
                if (pm.kickMember(p.getUniqueId(), k.getUniqueId())) p.sendMessage("§aSpieler entfernt."); else p.sendMessage("§cKick fehlgeschlagen.");
                return true;
            case "promote":
                if (args.length < 2) { p.sendMessage("§c/party promote <Spieler>"); return true; }
                Player np = Bukkit.getPlayer(args[1]);
                if (np == null) { p.sendMessage("§cSpieler nicht gefunden."); return true; }
                if (pm.transferLeadership(p.getUniqueId(), np.getUniqueId())) p.sendMessage("§aLeader übertragen."); else p.sendMessage("§cPromotion fehlgeschlagen.");
                return true;
            case "list":
                PartyManager.Party party = pm.getPartyOf(p.getUniqueId());
                if (party == null) { p.sendMessage("§7Keine Party."); return true; }
                StringBuilder sb = new StringBuilder();
                for (UUID u : party.getMembers()) {
                    Player pl = Bukkit.getPlayer(u);
                    sb.append(pl != null ? pl.getName() : u.toString()).append(" ");
                }
                p.sendMessage("§eParty: §a" + sb.toString().trim());
                return true;
            case "ready":
                plugin.getGameManager().enterSurvivorsContext(p.getUniqueId());
                var spReady = plugin.getPlayerManager().get(p.getUniqueId());
                if (spReady != null) { spReady.setReady(true); p.sendMessage("§aBereit gesetzt."); }
                var pm2 = plugin.getPartyManager(); var party2 = pm2 != null ? pm2.getPartyOf(p.getUniqueId()) : null;
                if (party2 != null && party2.getLeader().equals(p.getUniqueId())) {
                    int voteSec = Math.max(5, plugin.getConfigUtil().getInt("lobby.party-vote.seconds", 15));
                    plugin.getGameManager().beginPartyStartVote(party2, voteSec);
                } else {
                    p.sendMessage("§7Warte auf Party-Leader.");
                }
                return true;
            case "yes":
                if (args.length < 2) { p.sendMessage("§c/party yes <LeaderUUID>"); return true; }
                try { java.util.UUID l = java.util.UUID.fromString(args[1]); plugin.getGameManager().handlePartyVote(l, p.getUniqueId(), true); p.sendMessage("§aZugestimmt."); } catch (IllegalArgumentException ex) { p.sendMessage("§cUngültige UUID."); }
                return true;
            case "no":
                if (args.length < 2) { p.sendMessage("§c/party no <LeaderUUID>"); return true; }
                try { java.util.UUID l = java.util.UUID.fromString(args[1]); plugin.getGameManager().handlePartyVote(l, p.getUniqueId(), false); p.sendMessage("§eAbgelehnt."); } catch (IllegalArgumentException ex) { p.sendMessage("§cUngültige UUID."); }
                return true;
            default:
                p.sendMessage("§e/party create | invite <Spieler> | join <Leader> | leave | disband | list");
                return true;
        }
    }
}
