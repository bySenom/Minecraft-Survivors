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

    public static void spawnBurst(World w, Location center, Particle p, int count, double spread) {
        if (w == null || center == null) return;
        try {
            spawnSafe(w, p, center, count, spread, spread, spread, 0.02);
        } catch (Throwable t) {
            LogUtil.logFine("spawnBurst failed: " + p + " center=" + center, t);
        }
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

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
