package org.bysenom.minecraftSurvivors.manager;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StatsDisplayManager {

    public enum Mode { ACTIONBAR, BOSSBAR, SCOREBOARD, OFF }

    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin;
    private Mode mode;
    private final StatsMeterManager meter;
    private final Map<UUID, BossBar> bossbarsDps = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossbarsHps = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossbarsEnemy = new java.util.concurrent.ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask task;
    private org.bukkit.scheduler.BukkitTask broadcastTask;
    private final java.util.Map<java.util.UUID, Double> stickyDpsCap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Double> stickyHpsCap = new java.util.concurrent.ConcurrentHashMap<>();

    public StatsDisplayManager(org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin, StatsMeterManager meter) {
        this.plugin = plugin;
        this.meter = meter;
        this.mode = parseMode(plugin.getConfigUtil().getString(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_MODE, "actionbar"));
    }

    public synchronized void setMode(Mode m) {
        this.mode = m;
        restart();
        plugin.getConfig().set("stats.mode", m.name().toLowerCase());
        plugin.saveConfig();
    }

    public synchronized Mode getMode() { return mode; }

    public synchronized void toggle() {
        switch (mode) {
            case ACTIONBAR: setMode(Mode.BOSSBAR); break;
            case BOSSBAR: setMode(Mode.SCOREBOARD); break;
            case SCOREBOARD: setMode(Mode.OFF); break;
            case OFF: setMode(Mode.ACTIONBAR); break;
        }
    }

    public synchronized void start() { restart(); }

    public synchronized void stop() {
        if (task != null) { task.cancel(); task = null; }
        if (broadcastTask != null) { broadcastTask.cancel(); broadcastTask = null; }
        clearBossbars();
    }

    private void restart() {
        if (task != null) { task.cancel(); task = null; }
        if (broadcastTask != null) { broadcastTask.cancel(); broadcastTask = null; }
        clearBossbars();
        if (mode == Mode.OFF) return;
        int period = Math.max(10, plugin.getConfigUtil().getInt(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_UPDATE_INTERVAL_TICKS, 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, period);
        // periodic broadcast of top
        boolean enableBroadcast = plugin.getConfigUtil().getBoolean(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_BROADCAST_TOP_ENABLED, false);
        if (enableBroadcast) {
            int everySec = Math.max(5, plugin.getConfigUtil().getInt(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_BROADCAST_TOP_INTERVAL_SECONDS, 30));
            broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastTop, 20L * everySec, 20L * everySec);
        }
    }

    private void tick() {
        org.bysenom.minecraftSurvivors.model.GameState gState = plugin.getGameManager().getState();
        boolean running = gState == org.bysenom.minecraftSurvivors.model.GameState.RUNNING;
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inCtx = plugin.getGameManager().isInSurvivorsContext(p.getUniqueId());
            double dps = meter.getDps(p.getUniqueId());
            double hps = meter.getHps(p.getUniqueId());
            switch (mode) {
                case ACTIONBAR:
                    // Zeige ActionBar auch vor Spielstart, wenn Spieler im Survivors-Kontext ist (z. B. Klassenwahl)
                    if (!running && !inCtx) continue;
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer spAct = plugin.getPlayerManager().get(p.getUniqueId());
                    int currentXp = spAct != null ? spAct.getXp() : 0;
                    int xpToNext = spAct != null ? spAct.getXpToNext() : 1;
                    int lvl = spAct != null ? spAct.getClassLevel() : 1;
                    p.sendActionBar(Component.text(String.format("XP %d/%d • Lvl %d • DPS %.1f • HPS %.1f", currentXp, xpToNext, lvl, dps, hps)));
                    break;
                case BOSSBAR:
                    // Bossbars bei laufendem Spiel oder im Survivors-Lobby-Kontext anzeigen
                    if (!running && inCtx) {
                        // Lobby-Kontext: einfache Hinweis-BossBar falls noch keine Klasse gewählt
                        try {
                            org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
                            if (sp != null && sp.getSelectedClass() == null) {
                                BossBar hint = bossbarsEnemy.computeIfAbsent(p.getUniqueId(), id -> BossBar.bossBar(Component.text("Wähle eine Klasse im Menü"), 0.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS));
                                hint.name(Component.text("Klassenwahl offen"));
                                hint.progress(0f);
                                p.showBossBar(hint);
                            } else {
                                clearBossbarsFor(p); // keine generischen Bossbars wenn Klasse schon gewählt aber Spiel noch nicht läuft
                            }
                        } catch (Throwable t) {
                            org.bysenom.minecraftSurvivors.util.LogUtil.logFine("Bossbar hint setup failed for " + p.getUniqueId() + ": ", t);
                        }
                        break;
                    }
                    if (!running || !inCtx) { clearBossbarsFor(p); continue; }
                    updateBossbars(p, dps, hps);
                    updateEnemyBossbar(p);
                    break;
                case SCOREBOARD:
                    // handled by ScoreboardManager HUD
                    break;
            }
        }
    }

    private void broadcastTop() {
        java.util.List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        if (players.isEmpty()) return;
        class Pair { final String n; final double v; Pair(String n,double v){this.n=n;this.v=v;} }
        int n = Math.max(1, plugin.getConfigUtil().getInt(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_BROADCAST_TOP_N, 3));
        java.util.List<Pair> topDps = players.stream()
                .map(pl -> new Pair(pl.getName(), meter.getDps(pl.getUniqueId())))
                .sorted(java.util.Comparator.comparingDouble((Pair a) -> a.v).reversed())
                .limit(n).collect(Collectors.toList());
        java.util.List<Pair> topHps = players.stream()
                .map(pl -> new Pair(pl.getName(), meter.getHps(pl.getUniqueId())))
                .sorted(java.util.Comparator.comparingDouble((Pair a) -> a.v).reversed())
                .limit(n).collect(Collectors.toList());
        Bukkit.broadcast(Component.text("Top DPS:", NamedTextColor.GOLD));
        int i = 1;
        for (Pair p : topDps) Bukkit.broadcast(Component.text(String.format(" %d. %s - %.1f", i++, p.n, p.v), NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("Top HPS:", NamedTextColor.AQUA));
        i = 1;
        for (Pair p : topHps) Bukkit.broadcast(Component.text(String.format(" %d. %s - %.1f", i++, p.n, p.v), NamedTextColor.GREEN));
    }

    private void updateBossbars(Player p, double dps, double hps) {
        // If a global Endboss is active, do not show per-player DPS/HPS bossbars (avoid duplicate top bars)
        try {
            var gm = plugin.getGameManager();
            if (gm != null && gm.getBossManager() != null && gm.getBossManager().isBossActive()) {
                clearBossbarsFor(p);
                return;
            }
        } catch (Throwable ignored) {}

        boolean dyn = plugin.getConfigUtil().getBoolean(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_DYNAMIC_CAP_ENABLED, false);
        double dCap = Math.max(1.0, plugin.getConfigUtil().getDouble(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_AUTO_CAP_DPS, 50.0));
        double hCap = Math.max(1.0, plugin.getConfigUtil().getDouble(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_AUTO_CAP_HPS, 30.0));
        if (dyn) {
            try {
                double dMax = meter.getDpsMax(p.getUniqueId());
                double hMax = meter.getHpsMax(p.getUniqueId());
                double alpha = Math.min(1.0, Math.max(0.0, plugin.getConfigUtil().getDouble(org.bysenom.minecraftSurvivors.util.ConfigUtil.Keys.STATS_DYNAMIC_CAP_SMOOTHING, 0.2)));
                dCap = smoothSticky(stickyDpsCap, p.getUniqueId(), Math.max(dCap, dMax), alpha);
                hCap = smoothSticky(stickyHpsCap, p.getUniqueId(), Math.max(hCap, hMax), alpha);
            } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("StatsDisplayManager dynamic cap calc failed for " + p.getUniqueId() + ": ", t); }
        }
        double dProg = Math.min(1.0, dps / dCap);
        double hProg = Math.min(1.0, hps / hCap);
        BossBar d = bossbarsDps.computeIfAbsent(p.getUniqueId(), id -> BossBar.bossBar(Component.text("DPS"), 0.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS));
        BossBar h = bossbarsHps.computeIfAbsent(p.getUniqueId(), id -> BossBar.bossBar(Component.text("HPS"), 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS));
        d.name(Component.text(String.format("DPS %.1f (cap %.0f)", dps, dCap), NamedTextColor.GOLD));
        h.name(Component.text(String.format("HPS %.1f (cap %.0f)", hps, hCap), NamedTextColor.AQUA));
        d.progress((float) dProg);
        h.progress((float) hProg);
        p.showBossBar(d);
        p.showBossBar(h);
    }

    private void updateEnemyBossbar(Player p) {
        try {
            // Wenn der Endboss aktiv ist, keine zusätzliche Enemy-Bossbar anzeigen (sonst doppelte HP-Balken)
            try {
                var gm = plugin.getGameManager();
                if (gm != null && gm.getBossManager() != null && gm.getBossManager().isBossActive()) {
                    BossBar eOld = bossbarsEnemy.remove(p.getUniqueId());
                    if (eOld != null) try { p.hideBossBar(eOld); } catch (Throwable ignored) {}
                    return;
                }
            } catch (Throwable ignored) {}
            SpawnManager sm = plugin.getGameManager().getSpawnManager();
            double minutes = sm.getElapsedMinutes();
            double power = sm.getEnemyPowerIndex();
            double enrage = sm.getEnrageProgress();
            BossBar e = bossbarsEnemy.computeIfAbsent(p.getUniqueId(), id -> BossBar.bossBar(Component.text("Enemy Power"), 0.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS));
            e.name(Component.text(String.format("Enemy %.2fx • %dm %02ds • Enrage %d%%", power, (int)minutes, (int)((minutes*60)%60), (int)Math.round(enrage*100)), NamedTextColor.LIGHT_PURPLE));
            double prog = Math.tanh(Math.log10(Math.max(1.0, power)));
            e.progress((float) Math.max(0.0, Math.min(1.0, prog)));
            BossBar.Color col = enrage >= 1.0 ? BossBar.Color.RED : (enrage > 0.0 ? BossBar.Color.PINK : BossBar.Color.PURPLE);
            try { e.color(col); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("Bossbar color update failed: ", t); }
            p.showBossBar(e);
        } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("updateEnemyBossbar failed for " + p.getUniqueId() + ": ", t); }
    }

    private double smoothSticky(java.util.Map<java.util.UUID, Double> map, java.util.UUID id, double target, double alpha) {
        Double prev = map.get(id);
        double out = (prev == null) ? target : (prev * (1.0 - alpha) + target * alpha);
        if (prev != null && out < prev * 0.85) out = prev * 0.85; // 15% max drop per tick
        map.put(id, out);
        return Math.max(1.0, out);
    }

    private void clearBossbars() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            BossBar d = bossbarsDps.remove(p.getUniqueId());
            BossBar h = bossbarsHps.remove(p.getUniqueId());
            BossBar e = bossbarsEnemy.remove(p.getUniqueId());
            try { if (d != null) p.hideBossBar(d); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
            try { if (h != null) p.hideBossBar(h); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
            try { if (e != null) p.hideBossBar(e); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
        }
    }

    private void clearBossbarsFor(Player p) {
        BossBar d = bossbarsDps.remove(p.getUniqueId());
        BossBar h = bossbarsHps.remove(p.getUniqueId());
        BossBar e = bossbarsEnemy.remove(p.getUniqueId());
        try { if (d != null) p.hideBossBar(d); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
        try { if (h != null) p.hideBossBar(h); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
        try { if (e != null) p.hideBossBar(e); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("hideBossBar failed: ", t); }
    }

    // Neu: sofortiges Leeren auf Anforderung (z.B. beim Moduswechsel)
    public void clearAllBossbarsNow() { clearBossbars(); }

    private Mode parseMode(String s) {
        if (s == null) return Mode.ACTIONBAR;
        switch (s.toLowerCase()) {
            case "bossbar": return Mode.BOSSBAR;
            case "scoreboard": return Mode.SCOREBOARD;
            case "off": return Mode.OFF;
            default: return Mode.ACTIONBAR;
        }
    }
}
