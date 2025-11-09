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

    // Individueller Beitritt: unabhängig von Party/Leader
    public boolean join(Player p) {
        if (queue.add(p.getUniqueId())) {
            p.sendMessage("§aQueue beigetreten. Position: §e" + getPosition(p.getUniqueId()));
            try { LobbySystem.get().addToBossBar(p); } catch (Throwable ignored) {}
            return true;
        } else {
            p.sendMessage("§eBereits in der Queue. Position: §e" + getPosition(p.getUniqueId()));
            return false;
        }
    }

    // Alt: joinParty(UUID) bleibt bestehen für etwaige Altnutzer, bewirkt jetzt nur Leader-Join
    public void joinParty(UUID leaderId) {
        Player leader = plugin.getServer().getPlayer(leaderId);
        if (leader == null) return;
        queue.add(leaderId);
        try { LobbySystem.get().addToBossBar(leader); } catch (Throwable ignored) {}
        leader.sendMessage("§aQueue beigetreten. Position: §e" + getPosition(leaderId));
    }

    public boolean leave(Player p) { return leaveInternal(p, false); }
    public boolean leaveSilent(Player p) { return leaveInternal(p, true); }

    private boolean leaveInternal(Player p, boolean silent) {
        boolean removed = queue.remove(p.getUniqueId());
        admitted.remove(p.getUniqueId());
        try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
        if (removed) {
            if (!silent) p.sendMessage("§cQueue verlassen.");
            // Survivors-Kontext explizit entfernen, falls gesetzt
            setSurvivorsContext(p.getUniqueId(), false);
            return true;
        } else {
            if (!silent) p.sendMessage("§eNicht in der Queue.");
            return false;
        }
    }

    public int size() { return queue.size(); }
    public boolean isInQueue(UUID id) { return queue.contains(id); }
    public List<UUID> snapshot() { return new ArrayList<>(queue); }
    public int admittedCount() { return admitted.size(); }
    public boolean isAdmitted(UUID id) { return admitted.contains(id); }
    public java.util.List<java.util.UUID> admittedSnapshot() { return new java.util.ArrayList<>(admitted); }

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
            if (admitted.contains(next)) continue;
            Player p = plugin.getServer().getPlayer(next);
            if (p == null || !p.isOnline()) {
                // Offline/abwesend -> überspringen, nicht als admitted zählen
                continue;
            }
            admitted.add(next);
            p.sendMessage("§aDu bist jetzt dran! Öffne Survivors-Menü...");
            try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
            // Survivors-Kontext setzen, damit HUD/Scoreboard erscheinen
            setSurvivorsContext(p.getUniqueId(), true);
            // Survivors-Menü für Klassenwahl öffnen
            p.performCommand("msmenu");
            return next;
        }
        return null;
    }

    /** Leert admission-Status (z.B. nach Rundenende) */
    public void resetAdmission() { admitted.clear(); }

    /** Leert die Queue (und optional admitted) und entfernt BossBar Einträge. */
    public void clearQueue(boolean includeAdmitted) {
        for (UUID id : new java.util.ArrayList<>(queue)) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null && p.isOnline()) {
                try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
            }
        }
        queue.clear();
        if (includeAdmitted) admitted.clear();
    }
}
