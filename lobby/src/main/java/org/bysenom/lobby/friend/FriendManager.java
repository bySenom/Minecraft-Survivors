package org.bysenom.lobby.friend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Sehr einfache Friend-Persistenz (YAML). Später durch Datenbank ersetzen.
 * Erweiterung: Pending Friend Requests + optionaler Autosave.
 */
public class FriendManager {

    private final Plugin plugin;
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> blocked = new ConcurrentHashMap<>();
    // Eingehende Anfragen: Spieler -> Set von Requester UUIDs
    private final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();
    private File file;
    private FileConfiguration cfg;
    private volatile boolean dirty = false;

    public FriendManager(Plugin plugin) {
        this.plugin = plugin;
        init();
        setupAutosave();
    }

    private void init() {
        file = new File(plugin.getDataFolder(), "friends.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void setupAutosave() {
        int interval = plugin.getConfig().getInt("friends.autosave-seconds", 120);
        interval = Math.max(30, interval); // Mindestintervall
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (dirty) save();
        }, interval * 20L, interval * 20L);
    }

    private void load() {
        friends.clear();
        blocked.clear();
        pending.clear();
        if (cfg == null) return;
        if (!cfg.isConfigurationSection("players")) return;
        for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                List<String> fr = cfg.getStringList("players." + key + ".friends");
                List<String> bl = cfg.getStringList("players." + key + ".blocked");
                List<String> req = cfg.getStringList("players." + key + ".pending");
                Set<UUID> frSet = ConcurrentHashMap.newKeySet();
                for (String f : fr) try { frSet.add(UUID.fromString(f)); } catch (IllegalArgumentException ignored) {}
                Set<UUID> blSet = ConcurrentHashMap.newKeySet();
                for (String b : bl) try { blSet.add(UUID.fromString(b)); } catch (IllegalArgumentException ignored) {}
                Set<UUID> reqSet = ConcurrentHashMap.newKeySet();
                for (String r : req) try { reqSet.add(UUID.fromString(r)); } catch (IllegalArgumentException ignored) {}
                friends.put(id, frSet);
                blocked.put(id, blSet);
                if (!reqSet.isEmpty()) pending.put(id, reqSet);
            } catch (IllegalArgumentException ignored) {}
        }
        dirty = false;
    }

    private void save() {
        if (cfg == null) cfg = new YamlConfiguration();
        cfg.set("players", null);
        for (Map.Entry<UUID, Set<UUID>> e : friends.entrySet()) {
            cfg.set("players." + e.getKey() + ".friends", serialize(e.getValue()));
        }
        for (Map.Entry<UUID, Set<UUID>> e : blocked.entrySet()) {
            cfg.set("players." + e.getKey() + ".blocked", serialize(e.getValue()));
        }
        for (Map.Entry<UUID, Set<UUID>> e : pending.entrySet()) {
            cfg.set("players." + e.getKey() + ".pending", serialize(e.getValue()));
        }
        try { cfg.save(file); } catch (IOException ignored) {}
        dirty = false;
    }

    private List<String> serialize(Set<UUID> set) {
        List<String> out = new ArrayList<>();
        for (UUID u : set) out.add(u.toString());
        return out;
    }

    public Set<UUID> getFriends(UUID owner) { return friends.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()); }
    public Set<UUID> getBlocked(UUID owner) { return blocked.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()); }
    private Set<UUID> getPending(UUID owner) { return pending.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()); }

    public boolean isFriend(UUID a, UUID b) { return getFriends(a).contains(b); }

    public boolean addFriend(UUID owner, UUID other) {
        if (owner.equals(other)) return false;
        if (getBlocked(owner).contains(other)) return false;
        boolean added = getFriends(owner).add(other);
        if (added) {
            dirty = true;
            // Option: Freundschaft beidseitig? Standard: Ja
            getFriends(other).add(owner);
        }
        return added;
    }

    public boolean removeFriend(UUID owner, UUID other) {
        boolean removed = getFriends(owner).remove(other);
        if (removed) {
            getFriends(other).remove(owner);
            dirty = true;
        }
        return removed;
    }

    public boolean block(UUID owner, UUID other) {
        removeFriend(owner, other);
        boolean ok = getBlocked(owner).add(other);
        if (ok) {
            // Block entfernte auch pending requests
            getPending(owner).remove(other);
            dirty = true;
        }
        return ok;
    }

    public boolean unblock(UUID owner, UUID other) {
        boolean ok = getBlocked(owner).remove(other);
        if (ok) dirty = true;
        return ok;
    }

    // Friend Request Handling
    public enum RequestResult { SUCCESS, ALREADY_FRIENDS, BLOCKED, SELF, DUPLICATE }

    public RequestResult sendRequest(UUID from, UUID to) {
        if (from.equals(to)) return RequestResult.SELF;
        if (isFriend(from, to)) return RequestResult.ALREADY_FRIENDS;
        if (getBlocked(to).contains(from) || getBlocked(from).contains(to)) return RequestResult.BLOCKED;
        Set<UUID> inbound = getPending(to);
        if (!inbound.add(from)) return RequestResult.DUPLICATE;
        dirty = true;
        return RequestResult.SUCCESS;
    }

    public boolean hasPending(UUID target, UUID requester) { return getPending(target).contains(requester); }

    public boolean acceptRequest(UUID target, UUID requester) {
        if (!hasPending(target, requester)) return false;
        getPending(target).remove(requester);
        addFriend(target, requester); // beidseitig
        dirty = true;
        return true;
    }

    public boolean denyRequest(UUID target, UUID requester) {
        boolean removed = getPending(target).remove(requester);
        if (removed) dirty = true;
        return removed;
    }

    public List<String> listFriendNames(UUID owner) {
        List<String> out = new ArrayList<>();
        for (UUID id : getFriends(owner)) {
            OfflinePlayer op = plugin.getServer().getOfflinePlayer(id);
            out.add(op.getName() != null ? op.getName() : id.toString());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public List<String> listPendingNames(UUID owner) {
        List<String> out = new ArrayList<>();
        for (UUID id : getPending(owner)) {
            OfflinePlayer op = plugin.getServer().getOfflinePlayer(id);
            out.add(op.getName() != null ? op.getName() : id.toString());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    // Manuelles Speichern (falls nötig)
    public void flushNow() { save(); }
}
