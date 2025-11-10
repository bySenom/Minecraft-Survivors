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
            case "spawn", "test" -> {
                if (bm.isBossActive()) { p.sendMessage("§eBoss bereits aktiv."); }
                else {
                    try {
                        bm.debugSpawnBoss();
                        p.sendMessage("§aBoss gespawnt (debug).");
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
            case "smoketest", "testcleanup" -> {
                // Optional: /msboss smoketest [N] -> run N iterations (default 1, max 20)
                int iterations = 1;
                if (args.length > 1) {
                    try { iterations = Integer.parseInt(args[1]); } catch (Throwable ignored) {}
                }
                iterations = Math.max(1, Math.min(20, iterations));
                final int iters = iterations;
                p.sendMessage("§aSmoketest: starte " + iters + " Durchläufe (jeweils ~10s + Cleanup)");

                java.util.List<String> reports = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                final java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger(0);

                // Runner: executes one iteration and schedules the next until done
                final Runnable[] runner = new Runnable[1];
                runner[0] = () -> {
                    int i = idx.getAndIncrement();
                    if (i >= iters) {
                        // finished: send aggregated reports
                        p.sendMessage("§6[Smoketest-Ergebnis] Gesamtreport:");
                        for (String r : reports) {
                            try { p.sendMessage(r); } catch (Throwable ignored) {}
                        }
                        p.sendMessage("§aSmoketest abgeschlossen.");
                        return;
                    }
                    final int runNo = i + 1;
                    try {
                        if (!bm.isBossActive()) {
                            try { bm.debugSpawnBoss(); } catch (Throwable ignoreSpawn) { p.sendMessage("§cSpawn fehlgeschlagen: " + ignoreSpawn.getMessage()); }
                        }
                        p.sendMessage("§e[Smoketest] Run " + runNo + "/" + iters + " gestartet...");
                    } catch (Throwable t) { p.sendMessage("§cSmoketest Start-Fehler: " + t.getMessage()); }

                    long delay = 200L; // 10s
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            int meteorCount = -1;
                            int scheduledCount = -1;
                            try {
                                java.lang.reflect.Field fm = BossManager.class.getDeclaredField("meteorEntities");
                                fm.setAccessible(true);
                                Object obj = fm.get(bm);
                                if (obj instanceof java.util.Collection<?> c) meteorCount = c.size();
                            } catch (Throwable ignored) {}
                            try {
                                java.lang.reflect.Field fs = BossManager.class.getDeclaredField("scheduledTasks");
                                fs.setAccessible(true);
                                Object obj = fs.get(bm);
                                if (obj instanceof java.util.Collection<?> c) scheduledCount = c.size();
                            } catch (Throwable ignored) {}
                            boolean active = bm.isBossActive();
                            reports.add("[Run " + runNo + "] Vor Cleanup: BossActive=" + active + ", Meteors=" + meteorCount + ", ScheduledTasks=" + scheduledCount);
                            // Force end
                            try { bm.forceEnd(); } catch (Throwable ignored) {}
                            // Re-check after short delay
                            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                try {
                                    int meteorCount2 = -1;
                                    int scheduledCount2 = -1;
                                    try {
                                        java.lang.reflect.Field fm2 = BossManager.class.getDeclaredField("meteorEntities");
                                        fm2.setAccessible(true);
                                        Object obj2 = fm2.get(bm);
                                        if (obj2 instanceof java.util.Collection<?> c2) meteorCount2 = c2.size();
                                    } catch (Throwable ignored) {}
                                    try {
                                        java.lang.reflect.Field fs2 = BossManager.class.getDeclaredField("scheduledTasks");
                                        fs2.setAccessible(true);
                                        Object obj2 = fs2.get(bm);
                                        if (obj2 instanceof java.util.Collection<?> c2) scheduledCount2 = c2.size();
                                    } catch (Throwable ignored) {}
                                    boolean active2 = bm.isBossActive();
                                    reports.add("[Run " + runNo + "] Nach Cleanup: BossActive=" + active2 + ", Meteors=" + meteorCount2 + ", ScheduledTasks=" + scheduledCount2);
                                } catch (Throwable t2) { reports.add("[Run " + runNo + "] ReCheck Fehler: " + t2.getMessage()); }
                                // schedule next run (1 tick delay to yield)
                                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { try { runner[0].run(); } catch (Throwable ignored) {} });
                            }, 40L);
                        } catch (Throwable t) { p.sendMessage("§cSmoketest Fehler: " + t.getMessage()); org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { try { runner[0].run(); } catch (Throwable ignored) {} }); }
                    }, delay);
                };

                // start the chain
                org.bukkit.Bukkit.getScheduler().runTask(plugin, runner[0]);
            }
            case "aggro" -> {
                if (!bm.isBossActive()) { p.sendMessage("§7Kein Boss aktiv."); }
                else {
                    try {
                        bm.debugAggroNearest();
                        p.sendMessage("§aBoss Aggro (nearest) ausgelöst.");
                    } catch (Throwable t) { p.sendMessage("§cAggro fehlgeschlagen: " + t.getMessage()); }
                }
            }
            default -> {
                p.sendMessage("§eVerwendung: /msboss <spawn|test|kill|meteor|barrage|shockwave|lightning|aggro>");
            }
        }
        return true;
    }
}
