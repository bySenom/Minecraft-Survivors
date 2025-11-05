package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.ShopNpcManager;

public class ShopNpcListener implements Listener {
    private final MinecraftSurvivors plugin;
    private final ShopNpcManager mgr;

    public ShopNpcListener(MinecraftSurvivors plugin, ShopNpcManager mgr) {
        this.plugin = plugin; this.mgr = mgr;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() == null) return;
        if (!mgr.isShopNpc(e.getRightClicked())) return;
        e.setCancelled(true);
        mgr.openShop(e.getPlayer());
    }
}
