package org.bysenom.lobby;

import java.util.*;
import org.bukkit.entity.Player;

public class QueueManager {
    private final LobbySystem plugin;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();

    public QueueManager(LobbySystem plugin) { this.plugin = plugin; }

    public boolean join(Player p) {
        if (queue.add(p.getUniqueId())) {
            p.sendMessage("§aQueue beigetreten. Aktuelle Spieler: " + queue.size());
            return true;
        } else {
            p.sendMessage("§eBereits in der Queue.");
            return false;
        }
    }

    public boolean leave(Player p) {
        if (queue.remove(p.getUniqueId())) {
            p.sendMessage("§cQueue verlassen. Spieler jetzt: " + queue.size());
            return true;
        } else {
            p.sendMessage("§eNicht in der Queue.");
            return false;
        }
    }

    public List<UUID> snapshot() { return new ArrayList<>(queue); }
    public int size() { return queue.size(); }
    public boolean isInQueue(UUID id) { return queue.contains(id); }
}
