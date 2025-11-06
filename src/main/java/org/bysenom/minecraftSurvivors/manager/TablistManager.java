package org.bysenom.minecraftSurvivors.manager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

public class TablistManager {

    private final MinecraftSurvivors plugin;
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private BukkitTask task;

    public TablistManager(MinecraftSurvivors plugin, PlayerManager playerManager, GameManager gameManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.gameManager = gameManager;
    }

    public void start() {
        stop();
        boolean enabled = plugin.getConfigUtil().getBoolean("tablist.enabled", true);
        if (!enabled) return;
        int period = Math.max(10, plugin.getConfigUtil().getInt("tablist.update-interval-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, period);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        // clear header/footer
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty()); } catch (Throwable ignored) {}
        }
    }

    private void tick() {
        boolean showPower = plugin.getConfigUtil().getBoolean("tablist.show-enemy-power", true);
        boolean showParty = plugin.getConfigUtil().getBoolean("tablist.show-party-hp", true);
        String title = plugin.getConfigUtil().getString("tablist.header-title", "Minecraft Survivors");
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Component header = buildHeader(title, showPower);
                Component footer = showParty ? buildPartyFooter(p) : Component.empty();
                p.sendPlayerListHeaderAndFooter(header, footer);
                // Optional: List Name – Klasse & Level
                setPlayerListName(p);
            } catch (Throwable ignored) {}
        }
    }

    private Component buildHeader(String title, boolean showPower) {
        Component top = Component.text(title, NamedTextColor.GOLD);
        Component stateLine;
        try {
            org.bysenom.minecraftSurvivors.model.GameState st = gameManager.getState();
            NamedTextColor col = NamedTextColor.WHITE;
            String icon = "●";
            switch (String.valueOf(st)) {
                case "RUNNING": col = NamedTextColor.GREEN; icon = "▶"; break;
                case "PAUSED": col = NamedTextColor.YELLOW; icon = "⏸"; break;
                case "ENDED": col = NamedTextColor.RED; icon = "■"; break;
            }
            // Time
            double minutes = 0.0;
            try { minutes = gameManager.getSpawnManager().getElapsedMinutes(); } catch (Throwable ignored) {}
            stateLine = Component.text(String.format("%s  %dm %02ds", icon, (int) minutes, (int) ((minutes*60)%60)), col);
        } catch (Throwable t) {
            stateLine = Component.text("●", NamedTextColor.WHITE);
        }
        if (!showPower) return Component.join(JoinConfiguration.newlines(), top, stateLine);
        try {
            SpawnManager sm = gameManager.getSpawnManager();
            double power = sm.getEnemyPowerIndex();
            double enrage = sm.getEnrageProgress();
            Component pow = Component.text(String.format("Enemy %.2fx", power), NamedTextColor.LIGHT_PURPLE);
            Component enr = Component.text(String.format("Enrage %d%%", (int)Math.round(enrage*100)), enrage >= 1.0 ? NamedTextColor.RED : (enrage > 0 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY));
            return Component.join(JoinConfiguration.newlines(), top, stateLine, pow.append(Component.text("  ")).append(enr));
        } catch (Throwable ignored) {
            return Component.join(JoinConfiguration.newlines(), top, stateLine);
        }
    }

    private Component buildPartyFooter(Player viewer) {
        PartyManager pm = plugin.getPartyManager();
        if (pm == null) return Component.empty();
        PartyManager.Party party = pm.getPartyOf(viewer.getUniqueId());
        if (party == null) return Component.text("Keine Party", NamedTextColor.DARK_GRAY);
        List<UUID> online = pm.onlineMembers(party);
        if (online.isEmpty()) return Component.text("Party: (niemand online)", NamedTextColor.DARK_GRAY);
        List<Component> lines = online.stream().map(u -> buildPartyLine(u)).collect(Collectors.toList());
        Component title = Component.text("Party:", NamedTextColor.AQUA);
        return Component.join(JoinConfiguration.newlines(), title, Component.join(JoinConfiguration.newlines(), lines));
    }

    private Component buildPartyLine(UUID memberId) {
        Player pl = Bukkit.getPlayer(memberId);
        String name = pl != null ? pl.getName() : Bukkit.getOfflinePlayer(memberId).getName();
        if (name == null) name = memberId.toString().substring(0, 8);
        double hp = 0.0, max = 20.0;
        if (pl != null && pl.isOnline()) {
            try { max = pl.getAttribute(Attribute.MAX_HEALTH) != null ? pl.getAttribute(Attribute.MAX_HEALTH).getBaseValue() : 20.0; } catch (Throwable ignored) {}
            try { hp = pl.getHealth(); } catch (Throwable ignored) {}
        }
        String bar = makeBar(hp / Math.max(1.0, max), 12);
        Component barC = Component.text(bar, NamedTextColor.GREEN);
        Component hpC = Component.text(String.format(" %d%%", (int)Math.round((hp/Math.max(1.0,max))*100.0)), NamedTextColor.GRAY);
        // Klasse/Icon
        String icon = "";
        try {
            SurvivorPlayer sp = playerManager.get(memberId);
            if (sp != null && sp.getSelectedClass() != null) {
                switch (sp.getSelectedClass().name()) {
                    case "SHAMAN": icon = "⚡"; break;
                    case "PYROMANCER": icon = "🔥"; break;
                    case "RANGER": icon = "🏹"; break;
                    case "PALADIN": icon = "✚"; break;
                }
            }
        } catch (Throwable ignored) {}
        return Component.text(icon + " " + name + " ", NamedTextColor.WHITE).append(barC).append(hpC);
    }

    private void setPlayerListName(Player p) {
        try {
            SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            String icon = ""; int lvl = 1;
            if (sp != null) {
                lvl = sp.getClassLevel();
                if (sp.getSelectedClass() != null) {
                    switch (sp.getSelectedClass().name()) {
                        case "SHAMAN": icon = "⚡"; break;
                        case "PYROMANCER": icon = "🔥"; break;
                        case "RANGER": icon = "🏹"; break;
                        case "PALADIN": icon = "✚"; break;
                    }
                }
            }
            p.playerListName(Component.text(String.format("%s %s §7(Lv %d)", icon, p.getName(), lvl)));
        } catch (Throwable ignored) {}
    }

    private String makeBar(double ratio, int len) {
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int fill = (int) Math.round(ratio * len);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) sb.append(i < fill ? '█' : '░');
        sb.append("]");
        return sb.toString();
    }
}
