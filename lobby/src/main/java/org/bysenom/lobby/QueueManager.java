package org.bysenom.lobby;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class QueueManager {
    private final LobbySystem plugin;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> admitted = new LinkedHashSet<>(); // bereits zugelassene Spieler

    public QueueManager(LobbySystem plugin) { this.plugin = plugin; }

    private void setSurvivorsContext(UUID playerId, boolean enter) {
        if (playerId == null) return;
        try {
            org.bukkit.plugin.Plugin surv = Bukkit.getPluginManager().getPlugin("MinecraftSurvivors");
            if (surv == null || !surv.isEnabled()) return;
            Object gm = surv.getClass().getMethod("getGameManager").invoke(surv);
            String m = enter ? "enterSurvivorsContext" : "leaveSurvivorsContext";
            gm.getClass().getMethod(m, java.util.UUID.class).invoke(gm, playerId);
        } catch (Throwable ignored) {}
    }

    public boolean join(Player p) {
        // Party-Bridge Autojoin: Wenn Leader joint, alle Mitglieder mit in Queue
        if (plugin.getPartyBridge().isLeader(p)) {
            Set<UUID> members = plugin.getPartyBridge().getMemberUuids(p);
            // Leader zuerst
            boolean changed = queue.add(p.getUniqueId());
            for (UUID m : members) {
                if (!m.equals(p.getUniqueId())) {
                    queue.add(m);
                    Player mp = plugin.getServer().getPlayer(m);
                    if (mp != null && mp.isOnline()) mp.sendMessage("§aDein Party-Leader hat dich gequeued. Position: §e" + getPosition(m));
                }
            }
            p.sendMessage("§aQueue beigetreten (Party). Deine Position: §e" + getPosition(p.getUniqueId()));
            try { LobbySystem.get().addToBossBar(p); } catch (Throwable ignored) {}
            return changed;
        }
        // Falls Mitglied aber nicht Leader -> kein Teilbeitritt zulassen
        if (plugin.getPartyBridge().hasParty(p) && !plugin.getPartyBridge().isLeader(p)) {
            p.sendMessage("§eDu bist in einer Party. Nur der Leader kann die Queue betreten.");
            return false;
        }
        if (queue.add(p.getUniqueId())) {
            p.sendMessage("§aQueue beigetreten. Position: §e" + getPosition(p.getUniqueId()));
            try { LobbySystem.get().addToBossBar(p); } catch (Throwable ignored) {}
            return true;
        } else {
            p.sendMessage("§eBereits in der Queue. Position: §e" + getPosition(p.getUniqueId()));
            return false;
        }
    }

    public void joinParty(UUID leaderId) {
        Player leader = plugin.getServer().getPlayer(leaderId);
        if (leader == null) return;
        if (!plugin.getPartyBridge().isLeader(leader)) return; // Sicherheitscheck
        Set<UUID> members = plugin.getPartyBridge().getMemberUuids(leader);
        for (UUID m : members) {
            queue.add(m);
            Player mp = plugin.getServer().getPlayer(m);
            if (mp != null && mp.isOnline()) {
                mp.sendMessage("§aDeine Party wurde gequeued. Position: §e" + getPosition(m));
                try { LobbySystem.get().addToBossBar(mp); } catch (Throwable ignored) {}
            }
        }
    }

    public boolean leave(Player p) {
        boolean removed = queue.remove(p.getUniqueId());
        admitted.remove(p.getUniqueId());
        try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
        if (removed) {
            p.sendMessage("§cQueue verlassen.");
            // Wenn Leader verlässt, gesamte Party entfernen
            if (plugin.getPartyBridge().isLeader(p)) {
                Set<UUID> members = plugin.getPartyBridge().getMemberUuids(p);
                for (UUID m : members) {
                    if (!m.equals(p.getUniqueId())) {
                        queue.remove(m);
                        admitted.remove(m);
                        Player mp = plugin.getServer().getPlayer(m);
                        if (mp != null && mp.isOnline()) {
                            mp.sendMessage("§cQueue verlassen (Leader hat verlassen).");
                            try { LobbySystem.get().removeFromBossBar(mp); } catch (Throwable ignored) {}
                        }
                    }
                }
            }
            // Survivors-Kontext explizit entfernen, falls gesetzt
            setSurvivorsContext(p.getUniqueId(), false);
            return true;
        } else {
            p.sendMessage("§eNicht in der Queue.");
            return false;
        }
    }

    public int size() { return queue.size(); }
    public boolean isInQueue(UUID id) { return queue.contains(id); }
    public List<UUID> snapshot() { return new ArrayList<>(queue); }
    public int admittedCount() { return admitted.size(); }
    public boolean isAdmitted(UUID id) { return admitted.contains(id); }

    public int getPosition(UUID id) {
        int pos = 1;
        for (UUID q : queue) {
            if (q.equals(id)) return pos;
            pos++;
        }
        return -1;
    }

    /**
     * Nimmt den ersten wartenden Spieler und markiert ihn als admitted. Gibt dessen UUID zurück oder null.
     */
    public UUID admitNext() {
        Iterator<UUID> it = queue.iterator();
        while (it.hasNext()) {
            UUID next = it.next();
            if (!admitted.contains(next)) {
                admitted.add(next);
                Player p = plugin.getServer().getPlayer(next);
                if (p != null && p.isOnline()) {
                    p.sendMessage("§aDu bist jetzt dran! Öffne Survivors-Menü...");
                    try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
                    // Survivors-Kontext setzen, damit HUD/Scoreboard erscheinen
                    setSurvivorsContext(p.getUniqueId(), true);
                    // Survivors-Menü für Klassenwahl öffnen
                    p.performCommand("msmenu");
                }
                return next;
            }
        }
        return null;
    }

    /** Leert admission-Status (z.B. nach Rundenende) */
    public void resetAdmission() { admitted.clear(); }
}
