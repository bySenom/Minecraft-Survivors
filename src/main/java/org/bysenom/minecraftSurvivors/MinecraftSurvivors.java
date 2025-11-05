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
    private org.bysenom.minecraftSurvivors.manager.PartyManager partyManager;
    private org.bysenom.minecraftSurvivors.manager.ShopManager shopManager;
    private org.bysenom.minecraftSurvivors.manager.MetaProgressionManager metaManager;
    private GuiManager guiManager;
    private org.bysenom.minecraftSurvivors.manager.ShopNpcManager shopNpcManager;
    private org.bysenom.minecraftSurvivors.manager.SkillManager skillManager;

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
        this.partyManager = new org.bysenom.minecraftSurvivors.manager.PartyManager(this);
        this.shopManager = new org.bysenom.minecraftSurvivors.manager.ShopManager(this);
        this.metaManager = new org.bysenom.minecraftSurvivors.manager.MetaProgressionManager(this);
        this.skillManager = new org.bysenom.minecraftSurvivors.manager.SkillManager(this);

        // GuiManager als Feld speichern
        this.guiManager = new GuiManager(this, gameManager);
        // Shop-NPC Manager
        this.shopNpcManager = new org.bysenom.minecraftSurvivors.manager.ShopNpcManager(this, guiManager);

        // Registrierungen
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
            if (getCommand("party") != null) {
                getCommand("party").setExecutor(new org.bysenom.minecraftSurvivors.command.PartyCommand(this));
            }

            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.EntityDeathListener(playerManager, guiManager, this.configUtil), this);
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(gameManager, playerManager, this.playerDataManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.PlayerDataListener(this.playerDataManager, playerManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.SpawnFreezeListener(gameManager, gameManager.getSpawnManager()), this);
            getServer().getPluginManager().registerEvents(new GuiClickListener(this, guiManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.gui.LevelUpMenuListener(guiManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.DamageHealListener(this), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.LootchestListener(this), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.SkillListener(this), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.ShopNpcListener(this, shopNpcManager), this);
            getServer().getPluginManager().registerEvents(new org.bysenom.minecraftSurvivors.listener.DashListener(this), this);

            this.scoreboardManager.start();
            try { this.statsDisplayManager.start(); } catch (Throwable ignored) {}
            try { this.skillManager.start(); } catch (Throwable ignored) {}

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
        try { if (this.metaManager != null) this.metaManager.saveAll(); } catch (Throwable ignored) {}
        try { if (this.shopNpcManager != null) this.shopNpcManager.despawnAll(); } catch (Throwable ignored) {}
        try { if (this.skillManager != null) this.skillManager.stop(); } catch (Throwable ignored) {}
        getLogger().info("MinecraftSurvivors disabled.");
    }

    public static MinecraftSurvivors getInstance() { return instance; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public GameManager getGameManager() { return gameManager; }
    public ConfigUtil getConfigUtil() { return configUtil; }
    public org.bysenom.minecraftSurvivors.manager.StatsMeterManager getStatsMeterManager() { return statsMeterManager; }
    public org.bysenom.minecraftSurvivors.manager.StatsDisplayManager getStatsDisplayManager() { return statsDisplayManager; }
    public org.bysenom.minecraftSurvivors.manager.PartyManager getPartyManager() { return partyManager; }
    public org.bysenom.minecraftSurvivors.manager.ShopManager getShopManager() { return shopManager; }
    public org.bysenom.minecraftSurvivors.manager.MetaProgressionManager getMetaManager() { return metaManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public org.bysenom.minecraftSurvivors.manager.ShopNpcManager getShopNpcManager() { return shopNpcManager; }
    public org.bysenom.minecraftSurvivors.manager.SkillManager getSkillManager() { return skillManager; }
}
