package org.bysenom.lobby;

import java.util.*;
import org.bukkit.entity.Player;

public class QueueManager {
    private final LobbySystem plugin;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> admitted = new LinkedHashSet<>(); // bereits zugelassene Spieler

    public QueueManager(LobbySystem plugin) { this.plugin = plugin; }

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

    public boolean leave(Player p) {
        boolean removed = queue.remove(p.getUniqueId());
        admitted.remove(p.getUniqueId());
        try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
        if (removed) {
            p.sendMessage("§cQueue verlassen.");
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
