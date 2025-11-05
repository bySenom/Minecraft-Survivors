package org.bysenom.minecraftSurvivors.manager;

import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

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
        // Optional: Scoreboard zurücksetzen
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
        }
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { updateFor(p); } catch (Throwable ignored) {}
        }
    }

    private void updateFor(Player p) {
        if (p == null || !p.isOnline()) return;
        Scoreboard sb;
        try {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
        } catch (Throwable t) {
            // Fallback auf das bestehende Scoreboard
            sb = p.getScoreboard();
        }

        // Titel gestalten
        String title = ChatColor.GOLD + "Minecraft " + ChatColor.YELLOW + "Survivors";

        Objective obj;
        try {
            obj = sb.getObjective("ms_side");
            if (obj != null) obj.unregister();
        } catch (Throwable ignored) {}
        obj = sb.registerNewObjective("ms_side", "dummy", title);
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
        // party + stats mode
        String mode = "-";
        try { mode = plugin.getStatsDisplayManager().getMode().name().toLowerCase(); } catch (Throwable ignored) {}
        PartyManager pm = plugin.getPartyManager();
        PartyManager.Party party = pm != null ? pm.getPartyOf(uuid) : null;
        String partyLine = party == null ? "keine" : (pm.onlineMembers(party).size() + "/" + party.getMembers().size());

        // Nicer status icon
        String statusIcon;
        ChatColor statusColor;
        switch (state) {
            case "RUNNING": statusIcon = "▶"; statusColor = ChatColor.GREEN; break;
            case "PAUSED": statusIcon = "⏸"; statusColor = ChatColor.YELLOW; break;
            case "ENDED": statusIcon = "■"; statusColor = ChatColor.RED; break;
            default: statusIcon = "●"; statusColor = ChatColor.WHITE; break;
        }

        // Einträge (von oben nach unten absteigend) — Wave entfernt
        int line = 15;
        addLine(obj, ChatColor.DARK_GRAY + "──────────────", line--);
        addLine(obj, ChatColor.WHITE + "Status " + statusColor + statusIcon + ChatColor.GRAY + " • " + statusColor + state, line--);
        addLine(obj, ChatColor.DARK_GRAY + "", line--);
        addLine(obj, ChatColor.WHITE + "Klasse " + ChatColor.GREEN + clazzDisplay, line--);
        addLine(obj, ChatColor.WHITE + "Lvl " + ChatColor.AQUA + lvl + ChatColor.GRAY + " • " + ChatColor.WHITE + "XP " + ChatColor.GREEN + xp + ChatColor.GRAY + "/" + ChatColor.GREEN + xpNext, line--);
        addLine(obj, ChatColor.DARK_GRAY + " ", line--);
        addLine(obj, ChatColor.WHITE + "⚔ Kills " + ChatColor.YELLOW + kills, line--);
        addLine(obj, ChatColor.WHITE + "⛃ Coins " + ChatColor.GOLD + coins, line--);
        addLine(obj, ChatColor.DARK_GRAY + "  ", line--);
        addLine(obj, ChatColor.WHITE + "👥 Online " + ChatColor.AQUA + online, line--);
        addLine(obj, ChatColor.WHITE + "Party " + ChatColor.AQUA + partyLine, line--);
        addLine(obj, ChatColor.WHITE + "Stats " + ChatColor.AQUA + mode, line--);
        addLine(obj, ChatColor.DARK_GRAY + "───────────────", line--);

        p.setScoreboard(sb);
    }

    private void startHudTask() {
        if (hudTask != null) hudTask.cancel();
        int hudIntervalTicks = plugin.getConfigUtil().getInt("levelup.hud-interval-ticks", 100);
        hudTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Only render HUD in SCOREBOARD stats mode to avoid ActionBar collisions
            try {
                org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode mode = plugin.getStatsDisplayManager().getMode();
                if (mode != org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD) return;
            } catch (Throwable ignored) {}
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
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

    private ChatColor colorByState(String state) {
        if (state == null) return ChatColor.WHITE;
        switch (state) {
            case "RUNNING": return ChatColor.GREEN;
            case "PAUSED": return ChatColor.YELLOW;
            case "ENDED": return ChatColor.RED;
            default: return ChatColor.WHITE;
        }
    }

    private void addLine(Objective obj, String text, int score) {
        // Scoreboard benötigt einzigartige Zeilen, nutze Anhänge für Leerzeilen
        String safe = ensureUniqueLength(text, score);
        Score s = obj.getScore(safe);
        s.setScore(score);
    }

    private String ensureUniqueLength(String text, int salt) {
        if (text == null) text = "";
        // Scoreboard-Zeilenlänge begrenzen (<= 40 meistens sicher)
        String base = text;
        if (base.length() > 40) base = base.substring(0, 40);
        // Mach Zeile eindeutig, indem wir unsichtbare Farbcodes anhängen
        String suffix = ChatColor.values()[Math.floorMod(salt, ChatColor.values().length)].toString();
        String out = base + suffix;
        if (out.length() > 40) out = out.substring(0, 40);
        return out;
    }
}
