package org.bysenom.lobby.net;

import io.netty.channel.*;
import java.lang.reflect.*;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.npc.PlayerNpcRegistry;

public class PacketInterceptor {
    private final LobbySystem plugin;
    private final PlayerNpcRegistry registry;

    public PacketInterceptor(LobbySystem plugin, PlayerNpcRegistry registry) {
        this.plugin = plugin; this.registry = registry;
    }

    public void inject(Player p) {
        try {
            Object craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").cast(p);
            Object handle = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object connection = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection").get(handle);
            Object conn = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").cast(connection);
            Object nmsConnection = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").getField("connection").get(conn);
            Channel ch = (Channel) Class.forName("net.minecraft.network.Connection").getField("channel").get(nmsConnection);
            if (ch.pipeline().get("msnpc_handler") != null) return;
            ch.pipeline().addBefore("packet_handler", "msnpc_handler", new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    try {
                        String cn = msg.getClass().getName();
                        if (cn.endsWith("ServerboundInteractPacket")) {
                            Integer id = extractEntityId(msg);
                            if (id != null) {
                                boolean handled = registry.handleClick(p, id);
                                if (handled) {
                                    // swallow packet to be safe (server kennt Entity nicht)
                                    return;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                    super.channelRead(ctx, msg);
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("PacketInjector failed for " + p.getName() + ": " + t.getMessage());
        }
    }

    public void uninject(Player p) {
        try {
            Object craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").cast(p);
            Object handle = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object connection = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection").get(handle);
            Object conn = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").cast(connection);
            Object nmsConnection = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").getField("connection").get(conn);
            Channel ch = (Channel) Class.forName("net.minecraft.network.Connection").getField("channel").get(nmsConnection);
            ChannelHandler h = ch.pipeline().get("msnpc_handler");
            if (h != null) ch.pipeline().remove(h);
        } catch (Throwable ignored) {}
    }

    private Integer extractEntityId(Object serverboundInteractPacket) {
        try {
            // try getEntityId()
            try {
                Method m = serverboundInteractPacket.getClass().getMethod("getEntityId");
                m.setAccessible(true);
                Object o = m.invoke(serverboundInteractPacket);
                return (Integer) o;
            } catch (NoSuchMethodException e) {
                // try field 'entityId'
                Field f = serverboundInteractPacket.getClass().getDeclaredField("entityId");
                f.setAccessible(true);
                return (Integer) f.get(serverboundInteractPacket);
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
