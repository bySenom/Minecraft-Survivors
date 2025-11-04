package org.bysenom.minecraftSurvivors;

import org.bukkit.Bukkit;
import org.bysenom.minecraftSurvivors.command.OpenGuiCommand;
import org.bysenom.minecraftSurvivors.command.StartCommand;
import org.bysenom.minecraftSurvivors.gui.GuiClickListener;
import org.bysenom.minecraftSurvivors.gui.GuiManager;
import org.bysenom.minecraftSurvivors.listener.EntityDeathListener;
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

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Initiale Objekte erzeugen
        this.configUtil = new ConfigUtil(this);
        this.playerManager = new PlayerManager();
        this.gameManager = new GameManager(this, playerManager);

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

            getServer().getPluginManager().registerEvents(new EntityDeathListener(playerManager), this);
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(gameManager, playerManager), this);
            getServer().getPluginManager().registerEvents(new GuiClickListener(this, guiManager), this);

            getLogger().info("MinecraftSurvivors enabled (registrations done on main thread).");
        } catch (Throwable t) {
            getLogger().severe("Error during plugin registration: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
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
}
