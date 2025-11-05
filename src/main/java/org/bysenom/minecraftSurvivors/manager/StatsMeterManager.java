package org.bysenom.minecraftSurvivors.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Erfasst pro Spieler den verursachten Schaden und die Heilung in einem Sliding-Window (sekundengenau).
 */
public class StatsMeterManager {

    private final int windowSeconds;
    private final Map<UUID, Buckets> buckets = new ConcurrentHashMap<>();

    public StatsMeterManager(int windowSeconds) {
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    public void recordDamage(UUID player, double amount) {
        if (player == null || amount <= 0) return;
        buckets.computeIfAbsent(player, k -> new Buckets(windowSeconds)).addDamage(amount);
    }

    public void recordHeal(UUID player, double amount) {
        if (player == null || amount <= 0) return;
        buckets.computeIfAbsent(player, k -> new Buckets(windowSeconds)).addHeal(amount);
    }

    public double getDps(UUID player) {
        Buckets b = buckets.get(player);
        if (b == null) return 0.0;
        return b.sumDamage() / windowSeconds;
    }

    public double getHps(UUID player) {
        Buckets b = buckets.get(player);
        if (b == null) return 0.0;
        return b.sumHeal() / windowSeconds;
    }

    public void reset(UUID player) {
        buckets.remove(player);
    }

    public void resetAll() {
        buckets.clear();
    }

    private static final class Buckets {
        private final double[] dmg;
        private final double[] heal;
        private final int size;
        private long lastSec;
        private int index;

        Buckets(int size) {
            this.size = size;
            this.dmg = new double[size];
            this.heal = new double[size];
            this.lastSec = currentSec();
            this.index = 0;
        }

        void addDamage(double a) { roll(); dmg[index] += a; }
        void addHeal(double a) { roll(); heal[index] += a; }

        double sumDamage() { roll(); double s = 0; for (double v : dmg) s += v; return s; }
        double sumHeal() { roll(); double s = 0; for (double v : heal) s += v; return s; }

        private void roll() {
            long now = currentSec();
            long diff = now - lastSec;
            if (diff <= 0) return;
            if (diff >= size) {
                // reset all buckets
                java.util.Arrays.fill(dmg, 0.0);
                java.util.Arrays.fill(heal, 0.0);
                index = 0;
            } else {
                for (int i = 0; i < diff; i++) {
                    index = (index + 1) % size;
                    dmg[index] = 0.0;
                    heal[index] = 0.0;
                }
            }
            lastSec = now;
        }

        private long currentSec() { return System.currentTimeMillis() / 1000L; }
    }
}

