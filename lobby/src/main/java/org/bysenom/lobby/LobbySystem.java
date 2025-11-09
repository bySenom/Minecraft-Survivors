package org.bysenom.lobby;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbySystem extends JavaPlugin {

    private static LobbySystem instance;
    private QueueManager queueManager;
    private UiManager uiManager;
    private org.bukkit.boss.BossBar bossBar;
    private org.bukkit.scheduler.BukkitTask autoStartTask;
    private org.bysenom.lobby.npc.PlayerNpcRegistry npcRegistry;
    private org.bysenom.lobby.net.PacketInterceptor packetInterceptor;
    private NavigatorManager navigatorManager; // Neuer Manager
    private int countdownValue = -1;

    // Neu: Bridges/Manager
    private org.bysenom.lobby.bridge.PartyBridge partyBridge;
    private org.bysenom.lobby.friend.FriendManager friendManager;
    private org.bysenom.lobby.cosmetic.CosmeticManager cosmeticManager;

    // Performance: Cache letzter BossBar-Status
    private String lastBossTitle = null;
    private double lastBossProgress = -1.0;

    private volatile boolean survivorsDispatchDone = false;

    @Override
    public void onLoad() { instance = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.queueManager = new QueueManager(this);
        this.uiManager = new UiManager(this, queueManager);
        this.npcRegistry = new org.bysenom.lobby.npc.PlayerNpcRegistry(this);
        this.packetInterceptor = new org.bysenom.lobby.net.PacketInterceptor(this, npcRegistry);
        this.navigatorManager = new NavigatorManager(this);
        // Neu: Initialisieren
        this.partyBridge = new org.bysenom.lobby.bridge.PartyBridge();
        this.friendManager = new org.bysenom.lobby.friend.FriendManager(this);
        this.cosmeticManager = new org.bysenom.lobby.cosmetic.CosmeticManager(this);
        npcRegistry.loadFromConfig();
        // Verzögerten NPC-Spawn planen, um FancyNpcs zuerst eine Chance zu geben
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try { npcRegistry.spawnAllForOnlineViewers(); } catch (Throwable t) { getLogger().warning("NPC initial spawn error: " + t.getMessage()); }
        }, 40L);
        // Event-Listener für ggf. später aktiv werdendes FancyNpcs
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent e) {
                if (e.getPlugin().getName().equalsIgnoreCase("FancyNpcs")) {
                    getLogger().info("FancyNpcs wurde nachträglich geladen – versuche NPC-Reinitialisierung.");
                    Bukkit.getScheduler().runTaskLater(LobbySystem.this, () -> {
                        try {
                            npcRegistry.hideAll();
                            npcRegistry.loadFromConfig();
                            npcRegistry.spawnAllForOnlineViewers();
                        } catch (Throwable ex) { getLogger().warning("Reinit nach FancyNpcs Enable fehlgeschlagen: " + ex.getMessage()); }
                    }, 20L);
                }
            }
        }, this);

        PluginCommand cmd = getCommand("lobby");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.LobbyCommand(uiManager));
        cmd = getCommand("queue");
        if (cmd != null) {
            org.bysenom.lobby.command.QueueCommand qc = new org.bysenom.lobby.command.QueueCommand(queueManager);
            cmd.setExecutor(qc);
            cmd.setTabCompleter(qc);
        }
        cmd = getCommand("startsurvivors");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.StartSurvivorsCommand(this, queueManager));
        cmd = getCommand("lobbyreload");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.LobbyReloadCommand(this));
        cmd = getCommand("lobbynpc");
        if (cmd != null) cmd.setExecutor(new org.bysenom.lobby.command.LobbyNpcCommand(this, npcRegistry));
        PluginCommand cmdNav = getCommand("navigator");
        if (cmdNav != null) cmdNav.setExecutor(new org.bysenom.lobby.command.NavigatorCommand());
        PluginCommand cmdFriends = getCommand("friends");
        if (cmdFriends != null) { cmdFriends.setExecutor(new org.bysenom.lobby.command.FriendsCommand()); cmdFriends.setTabCompleter(new org.bysenom.lobby.command.FriendsCommand()); }
        PluginCommand cmdCos = getCommand("cosmetics");
        if (cmdCos != null) {
            org.bysenom.lobby.command.CosmeticsCommand cc = new org.bysenom.lobby.command.CosmeticsCommand();
            cmdCos.setExecutor(cc);
            cmdCos.setTabCompleter(cc);
        }

        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.JoinQuitListener(queueManager), this);
        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.UiClickListener(uiManager), this);
        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.NpcClickListener(npcRegistry), this);
        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.CompassListener(), this);
        // Neu: Reaktive Cosmetics (Trails/Emotes)
        Bukkit.getPluginManager().registerEvents(new org.bysenom.lobby.listener.CosmeticListener(), this);

        setupBossBar();
        setupAutoOpenOnJoin();
        setupAutoStartLoop();
        // Entfernt: sofortiger spawnAllForOnlineViewers(); (nun verzögert oben)
        getLogger().info("LobbySystem enabled.");
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) bossBar.removePlayer(p);
        }
        if (autoStartTask != null) autoStartTask.cancel();
        try { npcRegistry.hideAll(); } catch (Throwable ignored) {}
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            try { packetInterceptor.uninject(p); } catch (Throwable ignored) {}
        }
        try { if (cosmeticManager != null) cosmeticManager.shutdown(); } catch (Throwable ignored) {}
        getLogger().info("LobbySystem disabled.");
    }

    private void setupBossBar() {
        if (!getConfig().getBoolean("bossbar-enabled", true)) return;
        org.bukkit.boss.BarColor color = parseColor(getConfig().getString("bossbar-color", "BLUE"));
        org.bukkit.boss.BarStyle style = parseStyle(getConfig().getString("bossbar-overlay", "PROGRESS"));
        bossBar = Bukkit.createBossBar("Lobby • Warten auf Spieler…", color, style);
        bossBar.setProgress(0.0);
        lastBossTitle = bossBar.getTitle();
        lastBossProgress = 0.0;
        // Nur Quit-Handling: aus BossBar entfernen
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                removeFromBossBar(e.getPlayer());
            }
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                // Nur hinzufügen, falls bereits in Queue (z.B. Rejoin)
                try {
                    if (queueManager != null && queueManager.isInQueue(e.getPlayer().getUniqueId())) {
                        addToBossBar(e.getPlayer());
                    }
                } catch (Throwable ignored) {}
            }
        }, this);
        // Initial synchronisieren: alle bereits gequeueten Spieler hinzufügen
        try {
            for (java.util.UUID id : queueManager.snapshot()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) addToBossBar(p);
            }
        } catch (Throwable ignored) {}
    }

    private void updateBossBar(String title, double progress) {
        if (bossBar == null) return;
        String t = title == null ? lastBossTitle : title;
        double p = Math.max(0.0, Math.min(1.0, progress));
        if (t != null && !t.equals(lastBossTitle)) { bossBar.setTitle(t); lastBossTitle = t; }
        if (Math.abs(p - lastBossProgress) > 0.0001) { bossBar.setProgress(p); lastBossProgress = p; }
    }

    public void addToBossBar(Player p) {
        if (bossBar == null || p == null) return;
        try { bossBar.addPlayer(p); } catch (Throwable ignored) {}
    }

    public void removeFromBossBar(Player p) {
        if (bossBar == null || p == null) return;
        try { bossBar.removePlayer(p); } catch (Throwable ignored) {}
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
                Bukkit.getScheduler().runTask(LobbySystem.this, () -> {
                    try { navigatorManager.giveCompass(e.getPlayer()); } catch (Throwable ignored) {}
                    try {
                        if (queueManager != null && queueManager.isInQueue(e.getPlayer().getUniqueId())) addToBossBar(e.getPlayer());
                    } catch (Throwable ignored) {}
                    try { packetInterceptor.inject(e.getPlayer()); } catch (Throwable ignored) {}
                    try { npcRegistry.showAllTo(e.getPlayer()); } catch (Throwable ignored) {}
                });
            }
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                removeFromBossBar(e.getPlayer());
                packetInterceptor.uninject(e.getPlayer());
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
        lastBossTitle = null;
        lastBossProgress = -1.0;
        // Restart features with current config
        setupBossBar();
        setupAutoOpenOnJoin();
        setupAutoStartLoop();
    }

    private boolean canStartWithCurrentAdmission(int minNeeded) {
        int admitted = queueManager.admittedCount();
        if (admitted == 0) return false;
        // Solo: genau 1 Spieler -> erlaubt
        if (admitted == 1) return true;
        // Mehrere -> alle müssen gleiche Party haben und Partygröße >= minNeeded
        java.util.List<java.util.UUID> list = queueManager.admittedSnapshot();
        org.bukkit.entity.Player first = org.bukkit.Bukkit.getPlayer(list.get(0));
        if (first == null) return false;
        org.bysenom.lobby.bridge.PartyBridge bridge = getPartyBridge();
        if (bridge == null || !bridge.hasParty(first)) return false; // erste hat keine Party => kein gemeinsamer Start
        java.util.Set<java.util.UUID> partyMembers = bridge.getMemberUuids(first);
        if (partyMembers == null || partyMembers.isEmpty()) return false;
        // Prüfe ob alle admitted IDs in partyMembers enthalten sind
        for (java.util.UUID id : list) {
            if (!partyMembers.contains(id)) return false;
        }
        // optional: require party size == admitted
        return admitted >= minNeeded;
    }

    private boolean isSurvivorsLobbyState() {
        try {
            org.bukkit.plugin.Plugin pl = getServer().getPluginManager().getPlugin("MinecraftSurvivors");
            if (pl != null) {
                // Prüfe per Reflection ob GameManager#getState == LOBBY ohne harte Compile-Abhängigkeit
                Class<?> mainClazz = pl.getClass();
                java.lang.reflect.Method gmGetter = mainClazz.getMethod("getGameManager");
                Object gm = gmGetter.invoke(pl);
                if (gm != null) {
                    java.lang.reflect.Method stateMethod = gm.getClass().getMethod("getState");
                    Object stateObj = stateMethod.invoke(gm);
                    String stateName = String.valueOf(stateObj);
                    return "LOBBY".equalsIgnoreCase(stateName);
                }
            }
        } catch (Throwable ignored) {}
        // Falls Plugin nicht geladen oder Fehler -> treat as lobby (kein Start erzwingen)
        return true;
    }

    private void stopAutoStartLoop() {
        try {
            if (autoStartTask != null) { autoStartTask.cancel(); autoStartTask = null; }
        } catch (Throwable ignored) {}
    }

    private void setupAutoStartLoop() {
        boolean admission = getConfig().getBoolean("admission.enabled", true);
        if (admission) {
            setupAdmissionLoop();
            return;
        }
        final int min = Math.max(1, getConfig().getInt("min-players", 2));
        final int max = Math.max(min, getConfig().getInt("max-players", 8));
        final int seconds = Math.max(3, getConfig().getInt("autostart-seconds", 15));
        final String startCmd = getConfig().getString("survivors-start-command", "msstart");
        final boolean dispatchEnabled = getConfig().getBoolean("survivors.auto-dispatch-enabled", false); // Default false -> kein AutoStart
        autoStartTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Falls Spiel schon läuft oder bereits dispatcht wurde -> Task beenden
            if (survivorsDispatchDone || !isSurvivorsLobbyState()) { stopAutoStartLoop(); return; }
            int size = queueManager.size();
            if (bossBar != null) {
                double prog = Math.min(1.0, size / (double) min);
                String title = size >= min ? "Lobby • Startbereit" : "Lobby • Spieler: " + size + "/" + min;
                updateBossBar(title, prog);
            }
            if (!dispatchEnabled) return; // Auto-Dispatch global deaktiviert
            if (size >= min) {
                if (countdownValue < 0) countdownValue = seconds;
                if (size >= max) countdownValue = Math.min(countdownValue, 5);
                if (countdownValue <= 0) {
                    // Validierung: admittedSnapshot muss Solo oder gemeinsame Party sein
                    if (!canStartWithCurrentAdmission(min)) { countdownValue = seconds; return; }
                    for (java.util.UUID id : queueManager.snapshot()) {
                        org.bukkit.entity.Player p = Bukkit.getPlayer(id);
                        if (p != null && p.isOnline()) {
                            try { p.performCommand("msmenu"); } catch (Throwable ignored) {}
                        }
                    }
                    // Einmaliger Dispatch
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), startCmd);
                    survivorsDispatchDone = true;
                    try { queueManager.clearQueue(true); } catch (Throwable ignored) {}
                    stopAutoStartLoop();
                } else {
                    countdownValue--;
                }
            } else {
                countdownValue = -1;
            }
        }, 20L, 20L);
    }

    private void setupAdmissionLoop() {
        final int interval = Math.max(1, getConfig().getInt("admission.interval-seconds", 3));
        final String startWhen = getConfig().getString("admission.start-when", "min");
        final boolean backfill = getConfig().getBoolean("admission.backfill-while-running", true);
        final int min = Math.max(1, getConfig().getInt("min-players", 2));
        final String startCmd = getConfig().getString("survivors-start-command", "msstart");
        final boolean dispatchEnabled = getConfig().getBoolean("survivors.auto-dispatch-enabled", false);
        autoStartTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (survivorsDispatchDone || !isSurvivorsLobbyState()) { stopAutoStartLoop(); return; }
            int queued = queueManager.size();
            int admitted = queueManager.admittedCount();
            if (bossBar != null) {
                int etaNext = (countdownValue < 0 ? interval : countdownValue);
                String title = "Lobby • In Queue: " + queued + " • Zugelassen: " + admitted + " • Next in " + etaNext + "s";
                double target = Math.max(1.0, (double) (queued + admitted));
                double prog = Math.min(1.0, admitted / target);
                updateBossBar(title, prog);
            }
            if (countdownValue < 0) countdownValue = interval;
            if (countdownValue <= 0) {
                queueManager.admitNext();
                countdownValue = interval;
            } else {
                countdownValue--;
            }
            boolean shouldStart;
            switch (startWhen.toLowerCase()) {
                case "queue_empty": shouldStart = queued == 0 && admitted > 0; break;
                case "manual": shouldStart = false; break;
                default: shouldStart = admitted >= min; break;
            }
            if (shouldStart && dispatchEnabled) {
                if (!canStartWithCurrentAdmission(min)) return; // Warte weiter bis Bedingungen erfüllt
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), startCmd);
                survivorsDispatchDone = true;
                if (!backfill) { countdownValue = -1; try { queueManager.clearQueue(true); } catch (Throwable ignored) {} }
                stopAutoStartLoop();
            }
        }, 20L, 20L);
    }

    public static LobbySystem get() { return instance; }

    // Getter
    public NavigatorManager getNavigatorManager() { return navigatorManager; }
    public org.bysenom.lobby.bridge.PartyBridge getPartyBridge() { return partyBridge; }
    public org.bysenom.lobby.friend.FriendManager getFriendManager() { return friendManager; }
    public org.bysenom.lobby.cosmetic.CosmeticManager getCosmeticManager() { return cosmeticManager; }
    // Platzhalter-Hooks für UI-Integration
    public void openFriendsGui(org.bukkit.entity.Player p) {
        try { uiManager.openFriendsMenu(p); } catch (Throwable t) { p.sendMessage("§cFriends-GUI aktuell nicht verfügbar."); }
    }
    public void openCosmeticsGui(org.bukkit.entity.Player p) {
        try { uiManager.openCosmeticsMenu(p); } catch (Throwable t) { p.sendMessage("§cCosmetics-GUI aktuell nicht verfügbar."); }
    }
}
