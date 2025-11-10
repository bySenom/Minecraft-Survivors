package org.bysenom.minecraftSurvivors.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TextUtil {
    private TextUtil() {}
    public static Component clickableComponent(String label, String command) {
        return Component.text(label).color(NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand(command));
    }
    // Backwards-compatible method used earlier
    public static String clickable(String label, String command) {
        // If Adventure components aren't handled via sendMessage(String), we send plain text with instruction
        // But earlier callers used TextUtil.clickable(template...) expecting a String. We return a plain string instruction.
        return label + " -> " + command;
    }
}
