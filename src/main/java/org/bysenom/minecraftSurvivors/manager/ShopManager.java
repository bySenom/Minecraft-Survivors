package org.bysenom.minecraftSurvivors.manager;

import org.bukkit.configuration.file.FileConfiguration;
import java.time.*;
import java.util.*;

public class ShopManager {
    private final org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin;

    public ShopManager(org.bysenom.minecraftSurvivors.MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public List<Map<?, ?>> getDailyOffers(String path, int maxItems) {
        FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        List<Map<?, ?>> all = cfg.getMapList(path);
        if (all == null || all.isEmpty()) return Collections.emptyList();
        // Deterministic RNG per day
        String key = LocalDate.now(ZoneId.systemDefault()).toString() + "|" + path;
        long seed = key.hashCode();
        Random r = new Random(seed);
        List<Map<?, ?>> copy = new ArrayList<>(all);
        Collections.shuffle(copy, r);
        if (maxItems <= 0 || maxItems >= copy.size()) return copy;
        return new ArrayList<>(copy.subList(0, Math.max(0, maxItems)));
    }

    public long millisUntilMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).toMillis();
    }

    public String formatRemainingHHMMSS() {
        long ms = millisUntilMidnight();
        long s = ms / 1000L;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}

