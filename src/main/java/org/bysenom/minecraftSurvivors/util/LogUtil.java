package org.bysenom.minecraftSurvivors.util;

public final class LogUtil {
    private LogUtil() {}

    public static void logFine(String message, Throwable t) {
        try {
            org.bysenom.minecraftSurvivors.MinecraftSurvivors inst = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance();
            if (inst != null) {
                inst.getLogger().log(java.util.logging.Level.FINE, message, t);
                return;
            }
        } catch (Throwable t2) { java.util.logging.Logger.getLogger("MinecraftSurvivors").log(java.util.logging.Level.FINE, "LogUtil fallback failed: ", t2); }
        java.util.logging.Logger.getLogger("MinecraftSurvivors").log(java.util.logging.Level.FINE, message, t);
    }

    public static void logWarning(String message, Throwable t) {
        try {
            org.bysenom.minecraftSurvivors.MinecraftSurvivors inst = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance();
            if (inst != null) {
                inst.getLogger().log(java.util.logging.Level.WARNING, message, t);
                return;
            }
        } catch (Throwable t2) { java.util.logging.Logger.getLogger("MinecraftSurvivors").log(java.util.logging.Level.FINE, "LogUtil fallback failed: ", t2); }
        java.util.logging.Logger.getLogger("MinecraftSurvivors").log(java.util.logging.Level.WARNING, message, t);
    }
}
