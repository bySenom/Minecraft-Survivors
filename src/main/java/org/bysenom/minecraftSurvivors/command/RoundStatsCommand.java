package org.bysenom.minecraftSurvivors.command;

import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.RoundStatsManager;

public class RoundStatsCommand implements CommandExecutor {
    private final MinecraftSurvivors plugin;
    public RoundStatsCommand(MinecraftSurvivors plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftsurvivors.admin")) {
            sender.sendMessage(Component.text("Keine Berechtigung.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || "show".equalsIgnoreCase(args[0])) {
            int page = 0;
            if (args.length >= 2) {
                try { page = Math.max(0, Integer.parseInt(args[1]) - 1); } catch (Throwable ignored) {}
            }
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Nur ingame nutzbar."));
                return true;
            }
            RoundStatsManager rsm = plugin.getRoundStatsManager();
            if (rsm == null) { p.sendMessage(Component.text("RoundStats nicht initialisiert.").color(NamedTextColor.YELLOW)); return true; }
            RoundStatsManager.RoundSnapshot snap = rsm.getLastSnapshot();
            if (snap == null) { p.sendMessage(Component.text("Keine Runden-Daten vorhanden.").color(NamedTextColor.YELLOW)); return true; }
            // Build paginated inventory across sources and players.
            java.util.List<java.util.Map.Entry<String, Double>> srcs = new java.util.ArrayList<>(snap.damageBySource.entrySet());
            srcs.sort((a,b)-> Double.compare(b.getValue(), a.getValue()));
            java.util.List<java.util.Map.Entry<java.util.UUID, Double>> players = new java.util.ArrayList<>(snap.damageByPlayer.entrySet());
            players.sort((a,b)-> Double.compare(b.getValue(), a.getValue()));
            final int itemsPerPage = 52; // inventory size 54: header(0), items 1..52, prev 45, next 53
            int srcPages = Math.max(1, (int)Math.ceil(srcs.size() / (double)itemsPerPage));
            int playerPages = Math.max(1, (int)Math.ceil(players.size() / (double)itemsPerPage));
            int totalPages = srcPages + playerPages;
            int globalPage = Math.max(1, page + 1); // user provided page is zero-based earlier; normalize to 1-based
            if (globalPage > totalPages) globalPage = totalPages;

            // Determine whether this globalPage maps to sources or players
            boolean showSources = globalPage <= srcPages;
            int localPage = showSources ? globalPage : (globalPage - srcPages);
            Inventory inv = Bukkit.createInventory(null, 54, Component.text("Round Stats - ("+globalPage+"/"+totalPages+")"));
            ItemStack header = new ItemStack(org.bukkit.Material.PAPER);
            ItemMeta hm = header.getItemMeta();
            if (hm != null) {
                hm.displayName(Component.text("Round Report").color(NamedTextColor.GOLD));
                java.util.List<Component> hl = new java.util.ArrayList<>();
                hl.add(Component.text("Duration: " + ((snap.endMs - snap.startMs) / 1000.0) + "s"));
                hl.add(Component.text(showSources ? "Section: Sources" : "Section: Players"));
                hl.add(Component.text("Page: " + localPage + " / " + (showSources ? srcPages : playerPages)));
                // marker for GUI listener to read current global page
                hl.add(Component.text("MS_GLOBAL_PAGE:" + globalPage));
                hl.add(Component.text("MS_TOTAL_PAGES:" + totalPages));
                hm.lore(hl);
                header.setItemMeta(hm);
            }
            inv.setItem(0, header);

            int startIndex = (localPage - 1) * itemsPerPage;
            int slot = 1;
            if (showSources) {
                for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, srcs.size()); i++) {
                    var en = srcs.get(i);
                    ItemStack it = new ItemStack(org.bukkit.Material.PAPER);
                    ItemMeta im = it.getItemMeta();
                    if (im != null) { im.displayName(Component.text(en.getKey()).color(NamedTextColor.AQUA)); im.lore(java.util.List.of(Component.text("Damage: " + String.format("%.2f", en.getValue())))); it.setItemMeta(im); }
                    inv.setItem(slot++, it);
                }
            } else {
                for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, players.size()); i++) {
                    var en = players.get(i);
                    String name = "Player";
                    try { var pl = Bukkit.getPlayer(en.getKey()); if (pl != null) name = pl.getName(); } catch (Throwable ignored) {}
                    ItemStack it = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
                    ItemMeta im = it.getItemMeta();
                    if (im != null) { im.displayName(Component.text(name).color(NamedTextColor.GREEN)); im.lore(java.util.List.of(Component.text("Damage: " + String.format("%.2f", en.getValue())), Component.text("Kills: " + snap.killsByPlayer.getOrDefault(en.getKey(), 0) + " | Coins: " + snap.coinsByPlayer.getOrDefault(en.getKey(), 0)))); it.setItemMeta(im); }
                    inv.setItem(slot++, it);
                }
            }
            // Prev and Next arrows
            ItemStack prev = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta pm = prev.getItemMeta(); if (pm != null) { pm.displayName(Component.text("Prev").color(NamedTextColor.YELLOW)); prev.setItemMeta(pm); }
            ItemStack next = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta nm = next.getItemMeta(); if (nm != null) { nm.displayName(Component.text("Next").color(NamedTextColor.YELLOW)); next.setItemMeta(nm); }
            // place prev at slot 45 and next at slot 53
            inv.setItem(45, prev);
            inv.setItem(53, next);
            p.openInventory(inv);
            return true;
        }
        if (args.length >= 1 && "summary".equalsIgnoreCase(args[0])) {
            RoundStatsManager rsm = plugin.getRoundStatsManager();
            if (rsm == null) { sender.sendMessage(Component.text("RoundStats nicht initialisiert.").color(NamedTextColor.YELLOW)); return true; }
            try {
                java.nio.file.Path out = rsm.writeAggregateReportExport();
                if (out != null) {
                    if (sender instanceof Player p) p.sendMessage(org.bysenom.minecraftSurvivors.util.TextUtil.clickableComponent("Open summary file: " + out.getFileName(), "/say report path: " + out.toAbsolutePath()));
                    sender.sendMessage(Component.text("Summary written: " + out.toAbsolutePath()).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Summary generation failed or no export files.").color(NamedTextColor.RED));
                }
            } catch (Throwable t) { sender.sendMessage(Component.text("Summary failed: " + t.getMessage()).color(NamedTextColor.RED)); }
            return true;
        }

        if (args.length >= 2 && "export".equalsIgnoreCase(args[0])) {
            String fmt = "json";
            if (args.length >= 2) fmt = args[1].toLowerCase(java.util.Locale.ROOT);
            RoundStatsManager rsm = plugin.getRoundStatsManager();
            if (rsm == null) { sender.sendMessage(Component.text("RoundStats nicht initialisiert.").color(NamedTextColor.YELLOW)); return true; }
            var snap = rsm.getLastSnapshot();
            if (snap == null) { sender.sendMessage(Component.text("Keine Runden-Daten vorhanden.").color(NamedTextColor.YELLOW)); return true; }
            try {
                java.nio.file.Path out = null;
                if ("csv".equals(fmt)) out = rsm.writeCsvReportExport(snap);
                else if ("html".equals(fmt)) out = rsm.writeHtmlReportExport(snap);
                else out = rsm.getLastReportPathJsonExport();
                if (out != null) {
                    if (sender instanceof Player p) p.sendMessage(org.bysenom.minecraftSurvivors.util.TextUtil.clickableComponent("Open report file: " + out.getFileName(), "/say report path: " + out.toAbsolutePath()));
                    sender.sendMessage(Component.text("Report written: " + out.toAbsolutePath()).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Export failed.").color(NamedTextColor.RED));
                }
            } catch (Throwable t) { sender.sendMessage(Component.text("Export failed: " + t.getMessage()).color(NamedTextColor.RED)); }
            return true;
        }
        if (args.length >= 1 && "openfile".equalsIgnoreCase(args[0])) {
            if (args.length < 2) { sender.sendMessage(Component.text("Usage: /msroundstats openfile <filename>").color(NamedTextColor.YELLOW)); return true; }
            String name = args[1];
            File f = new File(plugin.getDataFolder(), name);
            if (!f.exists()) { sender.sendMessage(Component.text("File not found: " + f.getAbsolutePath()).color(NamedTextColor.RED)); return true; }
            try {
                sender.sendMessage(Component.text("Report file: " + f.getAbsolutePath()).color(NamedTextColor.GREEN));
                if (sender instanceof Player p) p.sendMessage(org.bysenom.minecraftSurvivors.util.TextUtil.clickableComponent("Open in file-explorer: " + f.getName(), "/say open " + f.getAbsolutePath()));
            } catch (Throwable ignored) { sender.sendMessage(Component.text("Report file: " + f.getAbsolutePath()).color(NamedTextColor.GREEN)); }
            return true;
        }
        sender.sendMessage(Component.text("Usage: /msroundstats show [page] | /msroundstats export [json|csv|html] | /msroundstats openfile <filename>"));
        return true;
    }
}
