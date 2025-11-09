package org.bysenom.minecraftSurvivors.listener;

import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.MetadataValue;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class MeteorBlockListener implements Listener {
    private final MinecraftSurvivors plugin;
    public MeteorBlockListener(MinecraftSurvivors plugin){ this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onFallingBlockPlace(EntityChangeBlockEvent e){
        if (!(e.getEntity() instanceof FallingBlock)) return;
        // cancel placement if tagged as meteor
        boolean meteor = false;
        try {
            for (MetadataValue mv : e.getEntity().getMetadata("ms_boss_meteor")) if (mv.getOwningPlugin() == plugin && mv.asBoolean()) { meteor = true; break; }
            if (!meteor) {
                for (MetadataValue mv : e.getEntity().getMetadata("ms_meteor")) if (mv.getOwningPlugin() == plugin && mv.asBoolean()) { meteor = true; break; }
            }
        } catch (Throwable ignored) {}
        if (meteor) {
            e.setCancelled(true);
            try { e.getEntity().remove(); } catch (Throwable ignored) {}
        }
    }
}
