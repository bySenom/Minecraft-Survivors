package org.bysenom.minecraftSurvivors.command;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
                if (args.length < 2) { p.sendMessage("§c/party invite <Spieler>"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { p.sendMessage("§cSpieler nicht gefunden."); return true; }
                if (pm.invite(p.getUniqueId(), t.getUniqueId(), 60)) p.sendMessage("§aEinladung gesendet."); else p.sendMessage("§cInvite fehlgeschlagen.");
                return true;
            case "join":
                if (args.length < 2) { p.sendMessage("§c/party join <Leader>"); return true; }
                Player leader = Bukkit.getPlayer(args[1]);
                if (leader == null) { p.sendMessage("§cLeader nicht gefunden."); return true; }
                if (pm.join(p.getUniqueId(), leader.getUniqueId())) p.sendMessage("§aParty beigetreten."); else p.sendMessage("§cJoin fehlgeschlagen (keine Einladung oder abgelaufen).");
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
            default:
                p.sendMessage("§e/party create | invite <Spieler> | join <Leader> | leave | disband | list");
                return true;
        }
    }
}

