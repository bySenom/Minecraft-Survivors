package org.bysenom.minecraftSurvivors.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.BossManager;

public class BossDebugCommand implements CommandExecutor {
    private final MinecraftSurvivors plugin;
    public BossDebugCommand(MinecraftSurvivors plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur ingame nutzbar.");
            return true;
        }
        if (!p.hasPermission("minecraftsurvivors.admin")) {
            p.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        BossManager bm = plugin.getGameManager().getBossManager();
        if (bm == null) { p.sendMessage("§cBossManager fehlt."); return true; }
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (sub) {
            case "spawn" -> {
                if (bm.isBossActive()) { p.sendMessage("§eBoss bereits aktiv."); }
                else {
                    try { // erzwungen: interne tick() prüft enrage, wir wollen direkten Spawn -> reflektierter Aufruf
                        java.lang.reflect.Method m = BossManager.class.getDeclaredMethod("trySpawnBoss");
                        m.setAccessible(true);
                        m.invoke(bm);
                        p.sendMessage("§aBoss gespawnt.");
                    } catch (Throwable t) { p.sendMessage("§cSpawn fehlgeschlagen: " + t.getMessage()); }
                }
            }
            case "kill" -> {
                if (!bm.isBossActive()) p.sendMessage("§7Kein Boss aktiv.");
                else {
                    try {
                        java.lang.reflect.Field f = BossManager.class.getDeclaredField("boss");
                        f.setAccessible(true);
                        Object obj = f.get(bm);
                        if (obj instanceof org.bukkit.entity.LivingEntity le) le.damage(99999.0);
                        p.sendMessage("§aKill-Trigger gesendet.");
                    } catch (Throwable t) { p.sendMessage("§cKill fehlgeschlagen: " + t.getMessage()); }
                }
            }
            case "meteor" -> {
                if (!bm.isBossActive()) { p.sendMessage("§7Kein Boss aktiv."); }
                else {
                    try {
                        java.lang.reflect.Method m = BossManager.class.getDeclaredMethod("abilityMeteor", double.class);
                        m.setAccessible(true);
                        m.invoke(bm, plugin.getGameManager().getSpawnManager().getEnemyPowerIndex());
                        p.sendMessage("§aMeteor-Test ausgelöst.");
                    } catch (Throwable t) { p.sendMessage("§cMeteor fehlgeschlagen: " + t.getMessage()); }
                }
            }
            case "barrage" -> {
                if (!bm.isBossActive()) { p.sendMessage("§7Kein Boss aktiv."); }
                else {
                    try {
                        java.lang.reflect.Method m = BossManager.class.getDeclaredMethod("abilityMeteorBarrage", double.class);
                        m.setAccessible(true);
                        m.invoke(bm, plugin.getGameManager().getSpawnManager().getEnemyPowerIndex());
                        p.sendMessage("§aMeteor-Barrage ausgelöst.");
                    } catch (Throwable t) { p.sendMessage("§cBarrage fehlgeschlagen: " + t.getMessage()); }
                }
            }
            case "shockwave" -> {
                if (!bm.isBossActive()) { p.sendMessage("§7Kein Boss aktiv."); }
                else {
                    try {
                        java.lang.reflect.Method m = BossManager.class.getDeclaredMethod("abilityShockwave", double.class);
                        m.setAccessible(true);
                        m.invoke(bm, plugin.getGameManager().getSpawnManager().getEnemyPowerIndex());
                        p.sendMessage("§aShockwave ausgelöst.");
                    } catch (Throwable t) { p.sendMessage("§cShockwave fehlgeschlagen: " + t.getMessage()); }
                }
            }
            case "lightning" -> {
                if (!bm.isBossActive()) { p.sendMessage("§7Kein Boss aktiv."); }
                else {
                    try {
                        java.lang.reflect.Method m = BossManager.class.getDeclaredMethod("abilityLightningChain", double.class);
                        m.setAccessible(true);
                        m.invoke(bm, plugin.getGameManager().getSpawnManager().getEnemyPowerIndex());
                        p.sendMessage("§aLightning Chain ausgelöst.");
                    } catch (Throwable t) { p.sendMessage("§cLightning fehlgeschlagen: " + t.getMessage()); }
                }
            }
            default -> {
                p.sendMessage("§eVerwendung: /msboss <spawn|kill|meteor|barrage|shockwave|lightning>");
            }
        }
        return true;
    }
}
