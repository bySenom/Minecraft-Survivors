package org.bysenom.lobby;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbySystem extends JavaPlugin {

    private static LobbySystem instance;
    private QueueManager queueManager;
    private UiManager uiManager;
    private org.bukkit.boss.BossBar bossBar;
    private org.bukkit.scheduler.BukkitTask autoStartTask;
    private int countdownValue = -1;

    @Override
    public void onLoad() { instance = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.queueManager = new QueueManager(this);
        this.uiManager = new UiManager(this, queueManager);

        PluginCommand cmd = getCommand("lobby");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.LobbyCommand(uiManager));
        cmd = getCommand("queue");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.QueueCommand(queueManager));
        cmd = getCommand("startsurvivors");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.StartSurvivorsCommand(this, queueManager));
        cmd = getCommand("lobbyreload");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.LobbyReloadCommand(this));

        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.JoinQuitListener(queueManager), this);
        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.UiClickListener(uiManager), this);

        setupBossBar();
        setupAutoOpenOnJoin();
        setupAutoStartLoop();
        getLogger().info("LobbySystem enabled.");
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) bossBar.removePlayer(p);
        }
        if (autoStartTask != null) autoStartTask.cancel();
        getLogger().info("LobbySystem disabled.");
    }

    private void setupBossBar() {
        if (!getConfig().getBoolean("bossbar-enabled", true)) return;
        org.bukkit.boss.BarColor color = parseColor(getConfig().getString("bossbar-color", "BLUE"));
        org.bukkit.boss.BarStyle style = parseStyle(getConfig().getString("bossbar-overlay", "PROGRESS"));
        bossBar = Bukkit.createBossBar("Lobby • Warten auf Spieler…", color, style);
        bossBar.setProgress(0.0);
        // beim Join allen anzeigen
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                if (bossBar != null) bossBar.addPlayer(e.getPlayer());
            }
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                if (bossBar != null) bossBar.removePlayer(e.getPlayer());
            }
        }, this);
        // initial hinzufügen
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) bossBar.addPlayer(p);
    }

    private org.bukkit.boss.BarColor parseColor(String s) {
        if (s == null) return org.bukkit.boss.BarColor.BLUE;
        try {
            return org.bukkit.boss.BarColor.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return org.bukkit.boss.BarColor.BLUE;
        }
    }

    private org.bukkit.boss.BarStyle parseStyle(String s) {
        String v = s == null ? "SOLID" : s.trim().toUpperCase();
        // Akzeptiere alte/bequeme Aliase aus anderen Projekten
        if (v.equals("PROGRESS") || v.equals("SOLID")) return org.bukkit.boss.BarStyle.SOLID;
        if (v.equals("NOTCHED_6")) return org.bukkit.boss.BarStyle.SEGMENTED_6;
        if (v.equals("NOTCHED_10")) return org.bukkit.boss.BarStyle.SEGMENTED_10;
        if (v.equals("NOTCHED_12")) return org.bukkit.boss.BarStyle.SEGMENTED_12;
        if (v.equals("NOTCHED_20")) return org.bukkit.boss.BarStyle.SEGMENTED_20;
        // Direkter Versuch (falls bereits SEGMENTED_* angegeben)
        try {
            return org.bukkit.boss.BarStyle.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return org.bukkit.boss.BarStyle.SOLID;
        }
    }

    private void setupAutoOpenOnJoin() {
        if (!getConfig().getBoolean("open-menu-on-join", true)) return;
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                Bukkit.getScheduler().runTask(LobbySystem.this, () -> uiManager.openLobbyMenu(e.getPlayer()));
            }
        }, this);
    }

    public void resetCountdown() { this.countdownValue = -1; }

    public void reinitRuntime() {
        // Stop loops & remove bossbar
        if (autoStartTask != null) autoStartTask.cancel();
        autoStartTask = null;
        if (bossBar != null) {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) bossBar.removePlayer(p);
            bossBar = null;
        }
        countdownValue = -1;
        // Restart features with current config
        setupBossBar();
        setupAutoOpenOnJoin();
        setupAutoStartLoop();
    }

    private void setupAutoStartLoop() {
        final int min = Math.max(1, getConfig().getInt("min-players", 2));
        final int max = Math.max(min, getConfig().getInt("max-players", 8));
        final int seconds = Math.max(3, getConfig().getInt("autostart-seconds", 15));
        final String startCmd = getConfig().getString("survivors-start-command", "msstart");
        autoStartTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int size = queueManager.size();
            // Bossbar aktualisieren
            if (bossBar != null) {
                double prog = Math.min(1.0, size / (double) min);
                bossBar.setProgress(prog);
                String title = size >= min ? "Lobby • Start in " + (countdownValue > 0 ? countdownValue + "s" : seconds + "s") : "Lobby • Spieler: " + size + "/" + min;
                bossBar.setTitle(title);
            }
            if (size >= min) {
                if (countdownValue < 0) countdownValue = seconds; // countdown starten
                if (size >= max) countdownValue = Math.min(countdownValue, 5); // schneller starten bei voller Lobby
                if (countdownValue <= 0) {
                    // Start: öffne bei allen Queue-Spielern das Survivors-Menü und sende Startkommando
                    for (java.util.UUID id : queueManager.snapshot()) {
                        org.bukkit.entity.Player p = Bukkit.getPlayer(id);
                        if (p != null && p.isOnline()) p.performCommand("msmenu");
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), startCmd);
                    countdownValue = -1; // zurücksetzen
                } else {
                    countdownValue--;
                }
            } else {
                countdownValue = -1; // warten
            }
        }, 20L, 20L);
    }

    public static LobbySystem get() { return instance; }
}
