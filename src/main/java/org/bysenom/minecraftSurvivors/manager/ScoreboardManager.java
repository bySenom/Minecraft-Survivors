package org.bysenom.minecraftSurvivors.manager;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Erstellt und aktualisiert ein Sidebar-Scoreboard mit Spielinfos je Spieler.
 */
public class ScoreboardManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private org.bukkit.scheduler.BukkitTask updateTask;
    private org.bukkit.scheduler.BukkitTask hudTask;

    public ScoreboardManager(MinecraftSurvivors plugin, PlayerManager playerManager, GameManager gameManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.gameManager = gameManager;
    }

    public void start() {
        stop();
        int period = Math.max(20, plugin.getConfigUtil().getInt("scoreboard.update-interval-ticks", 40));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, period);
        startHudTask();
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
        }
    }

    public void forceUpdateAll() {
        try { updateAll(); } catch (Throwable ignored) {}
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { updateFor(p); } catch (Throwable ignored) {}
        }
    }

    private void updateFor(Player p) {
        if (p == null || !p.isOnline()) return;
        boolean running = gameManager.getState() == org.bysenom.minecraftSurvivors.model.GameState.RUNNING;
        boolean inCtx = gameManager.isInSurvivorsContext(p.getUniqueId());
        // Zeige Scoreboard wenn Spiel läuft ODER Spieler im Survivors-Kontext ist (z. B. Klassenwahl)
        if (!running && !inCtx) {
            try { p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
            return;
        }
        Scoreboard sb;
        try {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
        } catch (Throwable t) {
            sb = p.getScoreboard();
        }
        boolean fancy = false;
        try { fancy = plugin.getConfigUtil().getBoolean("scoreboard.fancy", false); } catch (Throwable ignored) {}
        String titleStr = fancy ? "§6❖ §eSurvivors" : "§6Minecraft §eSurvivors";
        Objective obj;
        try {
            obj = sb.getObjective("ms_side");
            if (obj != null) obj.unregister();
        } catch (Throwable ignored) {}
        obj = sb.registerNewObjective("ms_side", org.bukkit.scoreboard.Criteria.DUMMY, net.kyori.adventure.text.Component.text(titleStr));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Daten sammeln
        UUID uuid = p.getUniqueId();
        SurvivorPlayer sp = playerManager.get(uuid);
        String state = String.valueOf(gameManager.getState());
        String clazzDisplay;
        try {
            clazzDisplay = sp.getSelectedClass() == null ? "-" : sp.getSelectedClass().getDisplayName();
        } catch (Throwable t) {
            clazzDisplay = sp.getSelectedClass() == null ? "-" : sp.getSelectedClass().name();
        }
        int lvl = sp.getClassLevel();
        int xp = sp.getXp();
        int xpNext = sp.getXpToNext();
        int kills = sp.getKills();
        int coins = sp.getCoins();
        int online = Bukkit.getOnlinePlayers().size();
        int essence = 0;
        try { essence = plugin.getMetaManager().get(uuid).getEssence(); } catch (Throwable ignored) {}
        String mode = "-";
        try { mode = plugin.getStatsDisplayManager().getMode().name().toLowerCase(); } catch (Throwable ignored) {}
        PartyManager pm = plugin.getPartyManager();
        PartyManager.Party party = pm != null ? pm.getPartyOf(uuid) : null;
        String partyLine = party == null ? "keine" : (pm.onlineMembers(party).size() + "/" + party.getMembers().size());

        // Status-Icon + Farbe
        String statusIcon; String statusColor;
        switch (state) {
            case "RUNNING": statusIcon = "▶"; statusColor = "§a"; break;
            case "PAUSED": statusIcon = "⏸"; statusColor = "§e"; break;
            case "ENDED": statusIcon = "■"; statusColor = "§c"; break;
            default: statusIcon = "●"; statusColor = "§f"; break;
        }

        // Einträge (von oben nach unten absteigend)
        int line = 15;
        addLine(obj, "§8" + (fancy?"──────────────────────":""), line--);
        addLine(obj, "§fStatus " + statusColor + statusIcon + " §7• " + statusColor + state, line--);
        addLine(obj, "§8", line--);
        addLine(obj, "§fKlasse §a" + clazzDisplay, line--);
        addLine(obj, "§fLvl §b" + lvl + " §7• §fXP §a" + xp + "§7/§a" + xpNext, line--);
        addLine(obj, "§8 ", line--);
        addLine(obj, "§f⚔ Kills §e" + kills, line--);
        addLine(obj, "§f⛃ Coins §6" + coins, line--);
        addLine(obj, "§d✦ Essence §f" + essence, line--);
        addLine(obj, "§8  ", line--);
        addLine(obj, "§f👥 Online §b" + online, line--);
        addLine(obj, "§fParty §b" + partyLine, line--);
        addLine(obj, "§fStats §b" + mode, line--);
        addLine(obj, "§8" + (fancy?"──────────────────────":""), line--);

        p.setScoreboard(sb);
    }

    private void startHudTask() {
        if (hudTask != null) hudTask.cancel();
        int hudIntervalTicks = plugin.getConfigUtil().getInt("levelup.hud-interval-ticks", 100);
        hudTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode mode = plugin.getStatsDisplayManager().getMode();
                if (mode != org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD) return;
            } catch (Throwable ignored) {}
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    boolean inCtx = gameManager.isInSurvivorsContext(p.getUniqueId());
                    if (!inCtx && gameManager.getState() != org.bysenom.minecraftSurvivors.model.GameState.RUNNING) continue;
                    if (gameManager != null && gameManager.isPlayerPaused(p.getUniqueId())) continue;
                    org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = playerManager.get(p.getUniqueId());
                    if (sp == null) continue;
                    int currentXp = sp.getXp();
                    int xpToNext = sp.getXpToNext();
                    double dps = 0.0, hps = 0.0;
                    try {
                        org.bysenom.minecraftSurvivors.manager.StatsMeterManager sm = plugin.getStatsMeterManager();
                        dps = sm.getDps(p.getUniqueId());
                        hps = sm.getHps(p.getUniqueId());
                    } catch (Throwable ignored) {}
                    p.sendActionBar(net.kyori.adventure.text.Component.text(String.format("XP %d/%d • Lvl %d • DPS %.1f • HPS %.1f", currentXp, xpToNext, sp.getClassLevel(), dps, hps)));
                } catch (Throwable ignored) {}
            }
        }, 0L, hudIntervalTicks);
    }

    private void addLine(Objective obj, String text, int score) {
        String safe = ensureUnique(text, score);
        Score s = obj.getScore(safe);
        s.setScore(score);
    }

    private String ensureUnique(String text, int salt) {
        if (text == null) text = "";
        String base = text.length() > 40 ? text.substring(0, 40) : text;
        String hex = "0123456789abcdef";
        String suffix = "§" + hex.charAt(Math.floorMod(salt, hex.length()));
        String out = base + suffix;
        if (out.length() > 40) out = out.substring(0, 40);
        return out;
    }
}
