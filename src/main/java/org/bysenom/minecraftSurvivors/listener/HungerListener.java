package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        e.setCancelled(true);
        try {
            p.setFoodLevel(20);
            p.setSaturation(20f);
            p.setExhaustion(0f);
        } catch (Throwable ignored) {}
    }
}
