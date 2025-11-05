package org.bysenom.minecraftSurvivors;

import org.bysenom.minecraftSurvivors.command.OpenGuiCommand;
import org.bysenom.minecraftSurvivors.command.StartCommand;
import org.bysenom.minecraftSurvivors.gui.GuiClickListener;
import org.bysenom.minecraftSurvivors.gui.GuiManager;
import org.bysenom.minecraftSurvivors.listener.PlayerDeathListener;
import org.bysenom.minecraftSurvivors.manager.GameManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.util.ConfigUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftSurvivors extends JavaPlugin {

    private static MinecraftSurvivors instance;
    private PlayerManager playerManager;
    private GameManager gameManager;
    private ConfigUtil configUtil;
    private org.bysenom.minecraftSurvivors.util.PlayerDataManager playerDataManager;
    private org.bysenom.minecraftSurvivors.manager.ScoreboardManager scoreboardManager;
    private org.bysenom.minecraftSurvivors.manager.StatsMeterManager statsMeterManager;
    private org.bysenom.minecraftSurvivors.manager.StatsDisplayManager statsDisplayManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Initiale Objekte erzeugen
        this.configUtil = new ConfigUtil(this);
        this.playerManager = new PlayerManager();
        this.playerDataManager = new org.bysenom.minecraftSurvivors.util.PlayerDataManager(this, playerManager);
        this.gameManager = new GameManager(this, playerManager);
        this.scoreboardManager = new org.bysenom.minecraftSurvivors.manager.ScoreboardManager(this, playerManager, gameManager);
        this.statsMeterManager = new org.bysenom.minecraftSurvivors.manager.StatsMeterManager(this.configUtil.getInt("stats.window-seconds", 10));
        this.statsDisplayManager = new org.bysenom.minecraftSurvivors.manager.StatsDisplayManager(this, this.statsMeterManager);

        // GuiManager einmal erstellen
        final GuiManager guiManager = new GuiManager(this, gameManager);

        // Registrierungen direkt und synchron (onEnable sollte auf dem Hauptthread laufen)
        getLogger().info("onEnable thread: " + Thread.currentThread().getName());
        try {
            if (getCommand("msstart") != null) {
                getCommand("msstart").setExecutor(new StartCommand(gameManager));
            }
            if (getCommand("msmenu") != null) {
                getCommand("msmenu").setExecutor(new OpenGuiCommand(guiManager));
            }
            if (getCommand("msconfig") != null) {
                getCommand("msconfig").setExecutor(new org.bysenom.minecraftSurvivors.command.MsConfigCommand(gameManager));
            }
            if (getCommand("msstats") != null) {
                getCommand("msstats").setExecutor(new org.bysenom.minecraftSurvivors.command.MsStatsCommand(this));
            }

            // EntityDeathListener benötigt nun ConfigUtil zur Bestimmung von XP pro Kill
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.EntityDeathListener(playerManager, guiManager, this.configUtil), this);
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(gameManager, playerManager, this.playerDataManager), this);
            // Also register PlayerDataListener for join/quit
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.PlayerDataListener(this.playerDataManager, playerManager), this);
            // SpawnFreezeListener: freeze mobs that spawn while players are selecting upgrades
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.SpawnFreezeListener(gameManager, gameManager.getSpawnManager()), this);
            getServer().getPluginManager().registerEvents(new GuiClickListener(this, guiManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.gui.LevelUpMenuListener(guiManager), this);
            // Damage tracker for DPS
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.DamageHealListener(this), this);

            // Scoreboard starten
            this.scoreboardManager.start();
            // start stats display
            try { this.statsDisplayManager.start(); } catch (Throwable ignored) {}

            getLogger().info("MinecraftSurvivors enabled (registrations done on main thread).");
        } catch (Throwable t) {
            getLogger().severe("Error during plugin registration: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try { if (this.scoreboardManager != null) this.scoreboardManager.stop(); } catch (Throwable ignored) {}
        try { if (this.statsDisplayManager != null) this.statsDisplayManager.stop(); } catch (Throwable ignored) {}
        getLogger().info("MinecraftSurvivors disabled.");
    }

    public static MinecraftSurvivors getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public org.bysenom.minecraftSurvivors.manager.StatsMeterManager getStatsMeterManager() {
        return statsMeterManager;
    }

    public org.bysenom.minecraftSurvivors.manager.StatsDisplayManager getStatsDisplayManager() {
        return statsDisplayManager;
    }
}
