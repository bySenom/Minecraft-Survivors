package org.bysenom.minecraftSurvivors.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Msg {
    private static final String PREFIX = "§8[§bMS§8]§r ";

    private Msg() {}

    public static void send(CommandSender to, String msg) {
        if (to != null && msg != null) to.sendMessage(PREFIX + msg);
    }

    public static void ok(CommandSender to, String msg) { send(to, "§a" + msg); }
    public static void warn(CommandSender to, String msg) { send(to, "§e" + msg); }
    public static void err(CommandSender to, String msg) { send(to, "§c" + msg); }
    public static void info(CommandSender to, String msg) { send(to, "§7" + msg); }

    public static void action(Player p, String msg) {
        if (p != null) p.sendActionBar(net.kyori.adventure.text.Component.text(msg));
    }
}
