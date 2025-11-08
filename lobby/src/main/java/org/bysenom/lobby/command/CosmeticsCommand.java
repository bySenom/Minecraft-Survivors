package org.bysenom.lobby.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.jetbrains.annotations.NotNull;

public class CosmeticsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Nur ingame."); return true; }
        if (args.length == 0) { help(player); return true; }
        switch (args[0].toLowerCase()) {
            case "unlock":
                if (args.length < 2) { player.sendMessage("§c/cosmetics unlock <key>"); return true; }
                String key = args[1].toLowerCase();
                if (LobbySystem.get().getCosmeticManager().isUnlocked(player.getUniqueId(), key)) {
                    player.sendMessage("§eBereits freigeschaltet: §f" + key); return true;
                }
                LobbySystem.get().getCosmeticManager().unlock(player.getUniqueId(), key);
                player.sendMessage("§aCosmetic freigeschaltet: §f" + key);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
                return true;
            case "clear":
                LobbySystem.get().getCosmeticManager().clear(player.getUniqueId());
                player.sendMessage("§cAlle Cosmetics zurückgesetzt.");
                return true;
            case "status":
                java.util.Set<String> keys = LobbySystem.get().getCosmeticManager().getUnlockedKeys(player.getUniqueId());
                if (keys.isEmpty()) {
                    player.sendMessage("§7Keine Cosmetics freigeschaltet. Nutze §f/cosmetics unlock <key>§7.");
                } else {
                    player.sendMessage("§bFreigeschaltete Cosmetics (§f" + keys.size() + "§b): §f" + String.join(", ", keys));
                }
                return true;
            default: help(player); return true;
        }
    }

    private void help(Player p) {
        p.sendMessage("§bCosmetics: /cosmetics unlock <key>|clear|status");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return java.util.Arrays.asList("unlock", "clear", "status");
        if (args.length == 2 && "unlock".equalsIgnoreCase(args[0])) {
            List<String> keys = new ArrayList<>();
            keys.add("trail_flame");
            keys.add("trail_spark");
            keys.add("emote_wave");
            keys.add("emote_shrug");
            keys.add("hat_dragon");
            keys.sort(String.CASE_INSENSITIVE_ORDER);
            return keys;
        }
        return Collections.emptyList();
    }
}
