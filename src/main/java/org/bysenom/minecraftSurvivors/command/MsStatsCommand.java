package org.bysenom.minecraftSurvivors.command;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.StatsDisplayManager;
import org.bysenom.minecraftSurvivors.manager.StatsMeterManager;

public class MsStatsCommand implements CommandExecutor {

    private final MinecraftSurvivors plugin;

    public MsStatsCommand(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/msstats mode <actionbar|bossbar|scoreboard|off> | toggle | top [dps|hps] [n] | show");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "mode":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /msstats mode <actionbar|bossbar|scoreboard|off>");
                    return true;
                }
                StatsDisplayManager.Mode m = parseMode(args[1]);
                plugin.getStatsDisplayManager().setMode(m);
                sender.sendMessage("§aStats Mode gesetzt: " + m.name().toLowerCase());
                return true;
            case "toggle":
                plugin.getStatsDisplayManager().toggle();
                sender.sendMessage("§aStats Mode gewechselt zu: " + plugin.getStatsDisplayManager().getMode().name().toLowerCase());
                return true;
            case "show":
                sender.sendMessage("§eAktueller Mode: §a" + plugin.getStatsDisplayManager().getMode().name().toLowerCase());
                return true;
            case "top":
                String which = args.length >= 2 ? args[1].toLowerCase() : "dps";
                int n = 5;
                if (args.length >= 3) {
                    try { n = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
                }
                showTop(sender, which, n);
                return true;
            case "detail":
                if (sender instanceof Player self && args.length == 1) {
                    showPlayerStats(self, self);
                } else if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { sender.sendMessage("§cSpieler nicht gefunden: "+args[1]); return true; }
                    showPlayerStats(sender, target);
                } else {
                    sender.sendMessage("§e/msstats detail [Spieler]");
                }
                return true;
            default:
                sender.sendMessage("§cUnbekannt: " + sub);
                return true;
        }
    }

    private StatsDisplayManager.Mode parseMode(String s) {
        if (s == null) return StatsDisplayManager.Mode.ACTIONBAR;
        switch (s.toLowerCase()) {
            case "bossbar": return StatsDisplayManager.Mode.BOSSBAR;
            case "scoreboard": return StatsDisplayManager.Mode.SCOREBOARD;
            case "off": return StatsDisplayManager.Mode.OFF;
            default: return StatsDisplayManager.Mode.ACTIONBAR;
        }
    }

    private void showTop(CommandSender sender, String which, int n) {
        StatsMeterManager meter = plugin.getStatsMeterManager();
        List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        if (players.isEmpty()) { sender.sendMessage("§cKeine Spieler online."); return; }
        class Pair { final String name; final double val; Pair(String n, double v){name=n;val=v;} }
        List<Pair> vals = players.stream().map(p -> {
            double v = which.equals("hps") ? meter.getHps(p.getUniqueId()) : meter.getDps(p.getUniqueId());
            return new Pair(p.getName(), v);
        }).sorted(Comparator.comparingDouble((Pair p) -> p.val).reversed()).limit(n).collect(Collectors.toList());
        sender.sendMessage("§6Top " + n + " " + which.toUpperCase() + ":");
        int i = 1;
        for (Pair p : vals) {
            sender.sendMessage(" §e" + (i++) + ". §a" + p.name + " §7- §f" + String.format("%.1f", p.val));
        }
    }

    private void showPlayerStats(CommandSender viewer, Player target) {
        var sp = plugin.getPlayerManager().get(target.getUniqueId());
        if (sp == null) { viewer.sendMessage("§cKeine Survivors-Daten für "+target.getName()); return; }
        viewer.sendMessage("§6Stats von §e"+target.getName()+"§6:");
        // Offensiv
        viewer.sendMessage(String.format(" §fDamage +: §a%.2f  §7(mult: +%.0f%%)", sp.getEffectiveDamageAdd(), sp.getEffectiveDamageMult()*100.0));
        viewer.sendMessage(String.format(" §fElite/Boss-DMG: §a+%.0f%%", sp.getDamageEliteBoss()*100.0));
        viewer.sendMessage(String.format(" §fCrit: §aChance %.0f%%%% §7| §aDamage +%.0f%%%%", sp.getCritChance()*100.0, sp.getCritDamage()*100.0));
        // Projektil
        double pc = Math.max(0.0, sp.getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_COUNT));
        double pb = Math.max(0.0, sp.getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_BOUNCE));
        viewer.sendMessage(String.format(" §fProjectiles: §a+%.0f §7| Bounces: §a+%.0f", pc, pb));
        // Kontrolle/Größe
        viewer.sendMessage(String.format(" §fRadius mult: §a+%.0f%%%%  §7| Duration: §a+%.0f%%%%", sp.getEffectiveRadiusMult()*100.0, Math.max(0.0, sp.getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.DURATION))*100.0));
        // Defensiv
        viewer.sendMessage(String.format(" §fArmor: §a%.0f%%%%  §7| Evasion: §a%.0f%%%%  §7| Thorns: §a+%.0f%%%%", sp.getArmor()*100.0, sp.getEvasion()*100.0, sp.getThorns()*100.0));
        viewer.sendMessage(String.format(" §fShield: §a%.1f  §7| HP-Regen: §a%.2f/s  §7| Lifesteal: §a%.0f%%%%", sp.getShieldMax(), sp.getHpRegen(), sp.getLifesteal()*100.0));
        // Beweglichkeit
        viewer.sendMessage(String.format(" §fSpeed: §a+%.0f%%%%  §7| AttackSpeed: §a+%.0f%%%%  §7| Knockback: §a+%.0f", sp.getEffectiveMoveSpeedMult()*100.0, sp.getEffectiveAttackSpeedMult()*100.0, sp.getKnockbackEffective()));
        // Leben
        viewer.sendMessage(String.format(" §fExtra Hearts: §a+%d  §7| MaxHealth(Hearts): §a+%.1f", sp.getEffectiveExtraHearts(), sp.getMaxHealthBonusHearts()));
        // Progression
        viewer.sendMessage(String.format(" §fXP Gain: §a+%.0f%%%%  §7| Powerup Mult: §a+%.0f%%%%", sp.getXpGain()*100.0, sp.getPowerupMult()*100.0));
    }
}
