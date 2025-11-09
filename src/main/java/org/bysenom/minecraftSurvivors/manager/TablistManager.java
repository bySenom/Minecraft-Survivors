package org.bysenom.minecraftSurvivors.manager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    // Caches to avoid sending unchanged tab updates
    private final java.util.Map<UUID, String> lastHeader = new ConcurrentHashMap<>();
    private final java.util.Map<UUID, String> lastFooter = new ConcurrentHashMap<>();
    private final java.util.Map<UUID, String> lastPlayerListName = new ConcurrentHashMap<>();

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
        lastHeader.clear(); lastFooter.clear(); lastPlayerListName.clear();
    }

    private void tick() {
        boolean showPower = plugin.getConfigUtil().getBoolean("tablist.show-enemy-power", true);
        boolean showParty = plugin.getConfigUtil().getBoolean("tablist.show-party-hp", true);
        String title = plugin.getConfigUtil().getString("tablist.header-title", "Minecraft Survivors");
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Component headerComp = buildHeader(title, showPower);
                Component footerComp = showParty ? buildPartyFooter(p) : Component.empty();

                // serialize to plain text for cheap equality checks
                String headerPlain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(headerComp);
                String footerPlain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(footerComp);

                String prevHeader = lastHeader.get(p.getUniqueId());
                String prevFooter = lastFooter.get(p.getUniqueId());

                if (!headerPlain.equals(prevHeader) || !footerPlain.equals(prevFooter)) {
                    try {
                        p.sendPlayerListHeaderAndFooter(headerComp, footerComp);
                        lastHeader.put(p.getUniqueId(), headerPlain);
                        lastFooter.put(p.getUniqueId(), footerPlain);
                    } catch (Throwable ignored) {}
                }

                // Optional: List Name – Klasse & Level (only update when changed)
                updatePlayerListNameIfNeeded(p);
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
                case "RUNNING" -> { col = NamedTextColor.GREEN; icon = "▶"; }
                case "PAUSED" -> { col = NamedTextColor.YELLOW; icon = "⏸"; }
                case "ENDED" -> { col = NamedTextColor.RED; icon = "■"; }
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
        List<Component> lines = online.stream().map(this::buildPartyLine).collect(Collectors.toList());
        Component title = Component.text("Party:", NamedTextColor.AQUA);
        return Component.join(JoinConfiguration.newlines(), title, Component.join(JoinConfiguration.newlines(), lines));
    }

    private Component buildPartyLine(UUID memberId) {
        Player pl = Bukkit.getPlayer(memberId);
        String name = pl != null ? pl.getName() : Bukkit.getOfflinePlayer(memberId).getName();
        if (name == null) name = memberId.toString().substring(0, 8);
        double hp = 0.0, max = 20.0;
        if (pl != null && pl.isOnline()) {
            try { org.bukkit.attribute.AttributeInstance ai = pl.getAttribute(Attribute.MAX_HEALTH); if (ai != null) max = ai.getBaseValue(); } catch (Throwable ignored) {}
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
                    case "SHAMAN" -> icon = "⚡";
                    case "PYROMANCER" -> icon = "🔥";
                    case "RANGER" -> icon = "🏹";
                    case "PALADIN" -> icon = "✚";
                }
            }
        } catch (Throwable ignored) {}
        return Component.text(icon + " " + name + " ", NamedTextColor.WHITE).append(barC).append(hpC);
    }

    private void updatePlayerListNameIfNeeded(Player p) {
        try {
            SurvivorPlayer sp = playerManager.get(p.getUniqueId());
            String icon = ""; int lvl = 1;
            if (sp != null) {
                lvl = sp.getClassLevel();
                if (sp.getSelectedClass() != null) {
                    switch (sp.getSelectedClass().name()) {
                        case "SHAMAN" -> icon = "⚡";
                        case "PYROMANCER" -> icon = "🔥";
                        case "RANGER" -> icon = "🏹";
                        case "PALADIN" -> icon = "✚";
                    }
                }
            }
            String plain = (icon.isEmpty() ? "" : icon + " ") + p.getName() + " (Lv " + lvl + ")";
            String prev = lastPlayerListName.get(p.getUniqueId());
            if (plain.equals(prev)) return;
            Component left = Component.text((icon.isEmpty() ? "" : icon + " ") + p.getName() + " ", NamedTextColor.WHITE);
            Component level = Component.text("(Lv " + lvl + ")", NamedTextColor.GRAY);
            p.playerListName(left.append(level));
            lastPlayerListName.put(p.getUniqueId(), plain);
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

    // Convenience overload using default length 12 (used by tablist footer)
    private String makeBar(double ratio) { return makeBar(ratio, 12); }
}
