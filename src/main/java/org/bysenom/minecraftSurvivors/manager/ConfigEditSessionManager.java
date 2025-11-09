package org.bysenom.minecraftSurvivors.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class ConfigEditSessionManager {
    public static class Session {
        public final UUID player;
        public final String path;
        public final Object oldValue;
        public Object newValue;
        public boolean awaitingChatInput = false;
        public long expiresAt = 0L;
        public Session(UUID player, String path, Object oldValue) {
            this.player = player; this.path = path; this.oldValue = oldValue;
        }
    }

    private final MinecraftSurvivors plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ConfigEditSessionManager(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public Session startChatSession(UUID player, String path) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        Object cur = cfg.get(path);
        Session s = new Session(player, path, cur);
        s.awaitingChatInput = true;
        s.expiresAt = System.currentTimeMillis() + 60000L; // 60s
        sessions.put(player, s);
        // schedule clear
        try {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Session ss = sessions.get(player);
                if (ss != null && ss.awaitingChatInput) sessions.remove(player);
            }, 20L * 60L);
        } catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.FINE, "Failed to schedule config edit session cleanup: ", t); }
        return s;
    }

    public Session getSession(UUID player) { return sessions.get(player); }
    public void setNewValue(UUID player, Object newValue) {
        Session s = sessions.get(player); if (s == null) return; s.newValue = newValue; s.awaitingChatInput = false; s.expiresAt = System.currentTimeMillis() + 120000L; // 2min to confirm
    }
    public void clearSession(UUID player) { sessions.remove(player); }

    public boolean applySession(UUID player) {
        Session s = sessions.get(player); if (s == null) return false;
        try {
            plugin.getConfigUtil().setValue(s.path, s.newValue);
            plugin.getGameManager().reloadConfigAndApply();
            sessions.remove(player);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("ConfigEditSession apply failed: " + t.getMessage());
            return false;
        }
    }
}
