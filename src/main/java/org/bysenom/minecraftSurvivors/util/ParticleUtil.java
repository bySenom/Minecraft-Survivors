package org.bysenom.minecraftSurvivors.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public final class ParticleUtil {
    private ParticleUtil() {}

    public static void spawnSafe(World w, Particle p, Location loc, int count, double offX, double offY, double offZ, double extra) {
        if (w == null || loc == null) return;
        try {
            w.spawnParticle(p, loc, count, offX, offY, offZ, extra);
        } catch (Throwable t) {
            LogUtil.logFine("Particle spawn failed: " + p + " at " + loc, t);
        }
    }

    // --- Throttle helpers ---
    private static boolean shouldThrottle(Location loc) {
        try {
            org.bysenom.minecraftSurvivors.MinecraftSurvivors pl = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance();
            if (pl == null) return false;
            boolean enabled = pl.getConfigUtil().getBoolean("fx.throttle.enabled", false);
            if (!enabled) return false;
            double view = pl.getConfigUtil().getDouble("fx.throttle.view-distance", 32.0);
            double skipChance = pl.getConfigUtil().getDouble("fx.throttle.skip-chance", 0.0);
            if (skipChance > 0.0 && Math.random() < Math.max(0.0, Math.min(1.0, skipChance))) return true;
            // Skip if no nearby player in radius
            try {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (!p.isOnline()) continue;
                    if (p.getWorld() != loc.getWorld()) continue;
                    // Respect per-player FX toggle: if player disabled FX, don't count them as a viewer
                    try {
                        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = pl.getPlayerManager().get(p.getUniqueId());
                        if (sp != null && !sp.isFxEnabled()) continue;
                    } catch (Throwable ignoredInner) { /* ignore and continue */ }
                    if (p.getLocation().distanceSquared(loc) <= view*view) return false; // found viewer nearby
                }
                return true; // no viewers nearby
            } catch (Throwable ignored) { return false; }
        } catch (Throwable ignored) { return false; }
    }

    public static void spawnSafeThrottled(World w, Particle p, Location loc, int count, double offX, double offY, double offZ, double extra) {
        if (w == null || loc == null) return;
        if (shouldThrottle(loc)) return;
        spawnSafe(w, p, loc, count, offX, offY, offZ, extra);
    }

    public static void spawnRing(World w, Location center, double radius, int points, Particle p) {
        if (w == null || center == null) return;
        try {
            for (int i = 0; i < points; i++) {
                double ang = 2.0 * Math.PI * i / Math.max(1, points);
                double x = center.getX() + Math.cos(ang) * radius;
                double z = center.getZ() + Math.sin(ang) * radius;
                Location l = new Location(center.getWorld(), x, center.getY(), z);
                spawnSafe(w, p, l, 1, 0.02, 0.02, 0.02, 0.0);
            }
        } catch (Throwable t) {
            LogUtil.logFine("spawnRing failed: " + p + " center=" + center, t);
        }
    }

    public static void spawnRingThrottled(World w, Location center, double radius, int points, Particle p) {
        if (w == null || center == null) return;
        if (shouldThrottle(center)) return;
        spawnRing(w, center, radius, points, p);
    }

    public static void spawnBurst(World w, Location center, Particle p, int count, double spread) {
        if (w == null || center == null) return;
        try {
            spawnSafe(w, p, center, count, spread, spread, spread, 0.02);
        } catch (Throwable t) {
            LogUtil.logFine("spawnBurst failed: " + p + " center=" + center, t);
        }
    }

    public static void spawnBurstThrottled(World w, Location center, Particle p, int count, double spread) {
        if (w == null || center == null) return;
        if (shouldThrottle(center)) return;
        spawnBurst(w, center, p, count, spread);
    }

    public static void spawnDust(World w, Location loc, int count, double r, double g, double b, float size) {
        if (w == null || loc == null) return;
        try {
            // Try to use REDSTONE dust if available, but fall back to END_ROD if not supported
            try {
                // Use reflection to avoid compile-time dependency on DustOptions in older APIs
                Class<?> dustClass = Class.forName("org.bukkit.Particle$DustOptions");
                java.lang.reflect.Constructor<?> ctor = dustClass.getConstructor(org.bukkit.Color.class, float.class);
                Object dust = ctor.newInstance(org.bukkit.Color.fromRGB((int)(clamp(r)*255), (int)(clamp(g)*255), (int)(clamp(b)*255)), size);
                java.lang.reflect.Method m = World.class.getMethod("spawnParticle", org.bukkit.Particle.class, Location.class, int.class, double.class, double.class, double.class, Object.class);
                m.invoke(w, org.bukkit.Particle.valueOf("REDSTONE"), loc, count, 0.2, 0.2, 0.2, dust);
                return;
            } catch (Throwable t) {
                // fallback to END_ROD
                spawnSafe(w, Particle.END_ROD, loc, Math.max(4, count/2), 0.2, 0.2, 0.2, 0.01);
                return;
            }
        } catch (Throwable t) {
            LogUtil.logFine("spawnDust failed at " + loc + ": ", t);
            // fallback to plain effect
            spawnSafe(w, Particle.END_ROD, loc, Math.max(4, count/2), 0.2, 0.2, 0.2, 0.01);
        }
    }

    public static void spawnLine(World w, Location from, Location to, int steps, Particle p) {
        if (w == null || from == null || to == null) return;
        try {
            int s = Math.max(1, steps);
            double dx = (to.getX() - from.getX()) / s;
            double dy = (to.getY() - from.getY()) / s;
            double dz = (to.getZ() - from.getZ()) / s;
            Location cur = from.clone();
            for (int i = 0; i <= s; i++) {
                spawnSafe(w, p, cur, 1, 0.02, 0.02, 0.02, 0.0);
                cur.add(dx, dy, dz);
            }
        } catch (Throwable t) {
            LogUtil.logFine("spawnLine failed from " + from + " to " + to + ": ", t);
        }
    }

    public static void spawnLineThrottled(World w, Location from, Location to, int steps, Particle p) {
        if (w == null || from == null || to == null) return;
        if (shouldThrottle(from)) return;
        spawnLine(w, from, to, steps, p);
    }

    public static void spawnSpiral(World w, Location center, double radius, double height, int points, Particle p, double turns) {
        if (w == null || center == null) return;
        try {
            int pts = Math.max(1, points);
            double total = pts * turns;
            for (int i=0;i<pts;i++) {
                double t = i / (double) pts; // 0..1
                double ang = t * turns * 2 * Math.PI;
                double x = center.getX() + Math.cos(ang) * radius;
                double z = center.getZ() + Math.sin(ang) * radius;
                double y = center.getY() + t * height;
                spawnSafe(w, p, new Location(w, x, y, z), 1, 0.02,0.02,0.02,0.0);
            }
        } catch (Throwable t) { LogUtil.logFine("spawnSpiral failed: ", t); }
    }

    public static void spawnSpiralThrottled(World w, Location center, double radius, double height, int points, Particle p, double turns) {
        if (w == null || center == null) return;
        if (shouldThrottle(center)) return;
        spawnSpiral(w, center, radius, height, points, p, turns);
    }

    public static void spawnHelix(World w, Location base, double radius, double height, int points, Particle p, int strands) {
        if (w == null || base == null) return;
        try {
            int pts = Math.max(1, points);
            int s = Math.max(1, strands);
            for (int i=0;i<pts;i++) {
                double t = i/(double)pts; // 0..1
                double y = base.getY() + t * height;
                double angBase = t * 2 * Math.PI * s;
                for (int k=0;k<s;k++) {
                    double ang = angBase + (2*Math.PI*k/s);
                    double x = base.getX() + Math.cos(ang) * radius;
                    double z = base.getZ() + Math.sin(ang) * radius;
                    spawnSafe(w, p, new Location(w, x, y, z), 1, 0.02,0.02,0.02,0.0);
                }
            }
        } catch (Throwable t) { LogUtil.logFine("spawnHelix failed: ", t); }
    }

    public static void spawnHelixThrottled(World w, Location base, double radius, double height, int points, Particle p, int strands) {
        if (w == null || base == null) return;
        if (shouldThrottle(base)) return;
        spawnHelix(w, base, radius, height, points, p, strands);
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
