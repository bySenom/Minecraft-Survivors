package org.bysenom.minecraftSurvivors.util;

public final class NumberUtil {
    private NumberUtil() {}

    public static int safeParseInt(Object o, int def) {
        if (o == null) return def;
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    public static double safeParseDouble(Object o, double def) {
        if (o == null) return def;
        try {
            return Double.parseDouble(String.valueOf(o).trim());
        } catch (Exception ignored) {
            return def;
        }
    }
}
