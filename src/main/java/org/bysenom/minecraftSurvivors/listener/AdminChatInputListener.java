package org.bysenom.minecraftSurvivors.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.ConfigEditSessionManager;

public class AdminChatInputListener implements Listener {
    private final MinecraftSurvivors plugin;
    private final ConfigEditSessionManager sessManager;

    public AdminChatInputListener(MinecraftSurvivors plugin, ConfigEditSessionManager sessManager) {
        this.plugin = plugin;
        this.sessManager = sessManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        ConfigEditSessionManager.Session s = sessManager.getSession(uid);
        if (s == null || !s.awaitingChatInput) return;
        e.setCancelled(true);
        String msg;
        try {
            msg = PlainTextComponentSerializer.plainText().serialize(e.message());
        } catch (Throwable t) {
            msg = PlainTextComponentSerializer.plainText().serialize(e.originalMessage());
        }
        Object old = s.oldValue;
        Object parsed;
        try {
            if (old instanceof Boolean) parsed = Boolean.parseBoolean(msg);
            else if (old instanceof Integer) parsed = Integer.parseInt(msg);
            else if (old instanceof Double || old instanceof Float) parsed = Double.parseDouble(msg);
            else parsed = msg;
        } catch (Throwable t) { parsed = msg; }
        sessManager.setNewValue(uid, parsed);
        plugin.getLogger().info("Admin ChatSet: " + e.getPlayer().getName() + " -> " + s.path + " = " + parsed);
        e.getPlayer().sendMessage("§aNeuer Wert vorgeschlagen für " + s.path + ": §f" + parsed + " §7( /msconfig confirm | /msconfig cancel )");
    }
}
