package org.bysenom.lobby;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class QueueManager {
    private final LobbySystem plugin;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> admitted = new LinkedHashSet<>(); // bereits zugelassene Spieler
    // Join Timestamps (für Wartezeit / Timeout / Cooldown)
    private final Map<UUID, Long> joinAt = new HashMap<>();
    private final Map<UUID, Long> lastJoinAttempt = new HashMap<>();
    private final Map<UUID, Long> admittedAt = new HashMap<>();
    // Metrics
    private long totalWaitMillis = 0L;
    private long totalAdmittedCount = 0L;

    // Rolling ETA samples (timestamps of admissions)
    private final Deque<Long> admitTimestamps = new ArrayDeque<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private long now(){ return System.currentTimeMillis(); }
    private int afkTimeoutSeconds(){ return Math.max(0, plugin.getConfig().getInt("queue.afk-timeout-seconds", 0)); }
    public void markActive(UUID id){ if(id!=null) lastActivity.put(id, now()); }
    public void pruneAfk(){ int to = afkTimeoutSeconds(); if(to<=0) return; long cutoff = now() - to*1000L; java.util.List<UUID> removed = new java.util.ArrayList<>();
        for(UUID id : new java.util.ArrayList<>(queue)){
            Long act = lastActivity.get(id);
            if(act!=null && act < cutoff && !admitted.contains(id)) { queue.remove(id); joinAt.remove(id); removed.add(id); }
        }
        if(!removed.isEmpty()){
            for(UUID id: removed){ Player p = plugin.getServer().getPlayer(id); if(p!=null&&p.isOnline()) p.sendMessage("§cAus Queue entfernt (AFK)"); }
        }
    }

    public QueueManager(LobbySystem plugin) { this.plugin = plugin; }

    private void debug(String msg) { if (plugin.getConfig().getBoolean("queue.debug", false)) plugin.getLogger().info("[Queue] " + msg); }

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

    private int maxAdmitted() { return Math.max(0, plugin.getConfig().getInt("admission.max-admitted", 0)); }
    private int rejoinCooldownSec() { return Math.max(0, plugin.getConfig().getInt("queue.rejoin-cooldown-seconds", 0)); }
    private int timeoutSeconds() { return Math.max(0, plugin.getConfig().getInt("admission.timeout-seconds", 0)); }
    private String timeoutAction() { return plugin.getConfig().getString("admission.timeout-action", "remove").toLowerCase(Locale.ROOT); }
    private int etaSampleSize() { return Math.max(3, plugin.getConfig().getInt("admission.eta-sample-size", 6)); }

    // Individueller Beitritt: unabhängig von Party/Leader
    public boolean join(Player p) {
        long now = System.currentTimeMillis();
        long cd = rejoinCooldownSec() * 1000L;
        Long last = lastJoinAttempt.get(p.getUniqueId());
        lastJoinAttempt.put(p.getUniqueId(), now);
        markActive(p.getUniqueId());
        if (cd > 0 && last != null && (now - last) < cd) {
            long remain = (cd - (now - last)) / 1000L + 1;
            p.sendMessage("§cBitte warte " + remain + "s vor erneutem /queue join.");
            return false;
        }
        if (queue.add(p.getUniqueId())) {
            joinAt.put(p.getUniqueId(), now);
            markActive(p.getUniqueId());
            p.sendMessage("§aQueue beigetreten. Position: §e" + getPosition(p.getUniqueId()));
            try { LobbySystem.get().addToBossBar(p); } catch (Throwable ignored) {}
            debug("join " + p.getName() + " pos=" + getPosition(p.getUniqueId()));
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
        if (queue.add(leaderId)) joinAt.put(leaderId, System.currentTimeMillis());
        try { LobbySystem.get().addToBossBar(leader); } catch (Throwable ignored) {}
        leader.sendMessage("§aQueue beigetreten. Position: §e" + getPosition(leaderId));
    }

    public boolean leave(Player p) { return leaveInternal(p, false); }
    public boolean leaveSilent(Player p) { return leaveInternal(p, true); }

    private boolean leaveInternal(Player p, boolean silent) {
        boolean removed = queue.remove(p.getUniqueId());
        boolean wasAdmitted = admitted.remove(p.getUniqueId());
        lastActivity.remove(p.getUniqueId());
        if (wasAdmitted) admittedAt.remove(p.getUniqueId());
        joinAt.remove(p.getUniqueId());
        try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
        if (removed || wasAdmitted) {
            if (!silent) p.sendMessage("§cQueue verlassen.");
            // Survivors-Kontext explizit entfernen, falls gesetzt
            setSurvivorsContext(p.getUniqueId(), false);
            debug("leave " + p.getName());
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
        // Kapazität prüfen
        int max = maxAdmitted();
        if (max > 0 && admitted.size() >= max) return null;
        Iterator<UUID> it = queue.iterator();
        while (it.hasNext()) {
            UUID next = it.next();
            if (admitted.contains(next)) continue; // sollte nicht vorkommen
            Player p = plugin.getServer().getPlayer(next);
            if (p == null || !p.isOnline()) {
                continue; // offline -> überspringen
            }
            admitted.add(next);
            admittedAt.put(next, System.currentTimeMillis());
            markActive(next);
            // Rolling ETA tracking
            admitTimestamps.addLast(System.currentTimeMillis());
            while (admitTimestamps.size() > etaSampleSize()) admitTimestamps.removeFirst();
            // Wartezeit registrieren
            Long st = joinAt.get(next);
            if (st != null) {
                totalWaitMillis += (System.currentTimeMillis() - st);
                totalAdmittedCount++;
            }
            p.sendMessage("§aDu bist jetzt dran! Öffne Survivors-Menü...");
            try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
            setSurvivorsContext(p.getUniqueId(), true);
            p.performCommand("msmenu");
            debug("admit " + p.getName());
            return next;
        }
        return null;
    }

    /** Admission Timeout Prüfung */
    public void enforceAdmissionTimeout() {
        int to = timeoutSeconds();
        if (to <= 0) return;
        long now = System.currentTimeMillis();
        java.util.List<UUID> expired = new ArrayList<>();
        for (UUID id : admitted) {
            Long at = admittedAt.get(id);
            if (at != null && (now - at) > to * 1000L) expired.add(id);
        }
        if (expired.isEmpty()) return;
        for (UUID id : expired) {
            admitted.remove(id);
            admittedAt.remove(id);
            Player p = plugin.getServer().getPlayer(id);
            if (p != null && p.isOnline()) {
                if ("recycle".equals(timeoutAction())) {
                    // Zurück ans Ende (erneut join)
                    queue.remove(id);
                    queue.add(id);
                    joinAt.put(id, System.currentTimeMillis());
                    p.sendMessage("§eTimeout – du wurdest ans Queue-Ende gesetzt.");
                } else {
                    queue.remove(id);
                    joinAt.remove(id);
                    p.sendMessage("§cTimeout – du wurdest aus der Queue entfernt.");
                }
                try { LobbySystem.get().addToBossBar(p); } catch (Throwable ignored) {}
                setSurvivorsContext(id, false);
            }
            debug("timeout " + id);
        }
    }

    public double getAverageWaitSeconds() {
        if (totalAdmittedCount <= 0) return 0.0;
        return (totalWaitMillis / (double) totalAdmittedCount) / 1000.0;
    }

    /** Leert admission-Status (z.B. nach Rundenende) */
    public void resetAdmission() { admitted.clear(); admittedAt.clear(); }

    /** Leert die Queue (und optional admitted) und entfernt BossBar Einträge. */
    public void clearQueue(boolean includeAdmitted) {
        for (UUID id : new java.util.ArrayList<>(queue)) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null && p.isOnline()) {
                try { LobbySystem.get().removeFromBossBar(p); } catch (Throwable ignored) {}
            }
        }
        queue.clear();
        joinAt.clear();
        if (includeAdmitted) { admitted.clear(); admittedAt.clear(); }
    }

    // Persistenz (einfache JSON-ähnliche Listen) --------------------------
    public void savePersistedQueue() {
        if (!plugin.getConfig().getBoolean("queue.persist-enabled", false)) return;
        File f = new File(plugin.getDataFolder(), "queue-data.txt");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8))) {
            w.write("#QUEUE\n");
            for (UUID id : queue) w.write(id.toString() + "\n");
            w.write("#ADMITTED\n");
            for (UUID id : admitted) w.write(id.toString() + "\n");
        } catch (Throwable t) { plugin.getLogger().warning("Queue persist save failed: " + t.getMessage()); }
    }

    public void loadPersistedQueue() {
        if (!plugin.getConfig().getBoolean("queue.persist-enabled", false)) return;
        File f = new File(plugin.getDataFolder(), "queue-data.txt");
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String mode = "";
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    if ("#QUEUE".equalsIgnoreCase(line)) mode = "Q"; else if ("#ADMITTED".equalsIgnoreCase(line)) mode = "A"; continue;
                }
                try {
                    UUID id = UUID.fromString(line);
                    if ("Q".equals(mode)) { queue.add(id); joinAt.put(id, System.currentTimeMillis()); }
                    else if ("A".equals(mode)) { admitted.add(id); admittedAt.put(id, System.currentTimeMillis()); }
                } catch (IllegalArgumentException ignored) {}
            }
            plugin.getLogger().info("Queue persist loaded: " + queue.size() + " queued, " + admitted.size() + " admitted");
        } catch (Throwable t) { plugin.getLogger().warning("Queue persist load failed: " + t.getMessage()); }
    }

    public int getMaxAdmitted() { return maxAdmitted(); }
    public boolean isFull() { int m = maxAdmitted(); return m > 0 && admitted.size() >= m; }
    public double getRollingEtaSeconds(int positionIndex) {
        // positionIndex: 1-based position in queue (excluding already admitted)
        int intervalCfg = Math.max(1, plugin.getConfig().getInt("admission.interval-seconds", 3));
        if (admitTimestamps.size() < 2) {
            return (positionIndex - 1) * intervalCfg; // fallback
        }
        // Compute average actual interval between last samples
        long prev = -1; int count = 0; long sum = 0;
        for (Long ts : admitTimestamps) {
            if (prev >= 0) { sum += (ts - prev); count++; }
            prev = ts;
        }
        double avgMillis = count > 0 ? (double) sum / count : (double) intervalCfg * 1000.0;
        double base = avgMillis / 1000.0;
        return Math.max(0.0, (positionIndex - 1) * base);
    }
}
