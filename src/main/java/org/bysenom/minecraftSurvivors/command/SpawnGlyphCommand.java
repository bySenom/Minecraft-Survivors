package org.bysenom.minecraftSurvivors.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.ability.AbilityCatalog;
import org.bysenom.minecraftSurvivors.listener.GlyphPickupListener;

public class SpawnGlyphCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be run in-game by a player.");
            return true;
        }
        Player p = (Player) sender;
        String key = null;
        if (args.length == 0) {
            sender.sendMessage("Usage: /spawnglyph <abilityKey|random>");
            return true;
        }
        if ("random".equalsIgnoreCase(args[0])) {
            java.util.List<AbilityCatalog.Def> defs = new java.util.ArrayList<>(AbilityCatalog.all());
            if (defs.isEmpty()) { sender.sendMessage("No abilities available to spawn."); return true; }
            key = defs.get(new java.util.Random().nextInt(defs.size())).key;
        } else {
            key = args[0];
        }
        try {
            GlyphPickupListener.spawnGlyph(p.getLocation(), key);
            p.sendMessage("Spawned glyph: " + key);
        } catch (Throwable t) {
            p.sendMessage("Failed to spawn glyph: " + t.getMessage());
            MinecraftSurvivors.getInstance().getLogger().warning("SpawnGlyphCommand failed: " + t.getMessage());
        }
        return true;
    }
}
