package org.bysenom.lobby.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.lobby.LobbySystem;
import org.bysenom.lobby.npc.PlayerNpcRegistry;

public class LobbyNpcCommand implements CommandExecutor {
    private final LobbySystem plugin;
    private final PlayerNpcRegistry registry;
    public LobbyNpcCommand(LobbySystem plugin, PlayerNpcRegistry registry) { this.plugin = plugin; this.registry = registry; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lobby.admin")) { sender.sendMessage("§cKeine Berechtigung."); return true; }
        if (args.length == 0) {
            sender.sendMessage("§e/lobbynpc list | add <Name> | remove <index>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list": {
                int i = 0;
                for (PlayerNpcRegistry.Entry e : registry.entries()) {
                    sender.sendMessage("§7[" + i + "] §e" + e.name + "§7 pos=" + fmt(e.loc));
                    i++;
                }
                if (i == 0) sender.sendMessage("§7Keine NPCs.");
                return true;
            }
            case "add": {
                if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
                if (args.length < 2) { sender.sendMessage("§eVerwendung: /lobbynpc add <Name>"); return true; }
                String name = args[1];
                plugin.getConfig().set("npc.enabled", false); // legacy deaktivieren
                plugin.saveConfig();
                int idx = registry.entries().size();
                String base = "npcs." + idx;
                Player p = (Player) sender;
                plugin.getConfig().set(base + ".name", name);
                plugin.getConfig().set(base + ".lookAt", true);
                plugin.getConfig().set(base + ".command", "/lobby");
                plugin.getConfig().set(base + ".world", p.getWorld().getName());
                plugin.getConfig().set(base + ".x", p.getLocation().getX());
                plugin.getConfig().set(base + ".y", p.getLocation().getY());
                plugin.getConfig().set(base + ".z", p.getLocation().getZ());
                plugin.getConfig().set(base + ".yaw", p.getLocation().getYaw());
                plugin.saveConfig();
                registry.hideAll();
                registry.loadFromConfig();
                registry.spawnAllForOnlineViewers();
                sender.sendMessage("§aNPC hinzugefügt: " + name);
                return true;
            }
            case "remove": {
                if (args.length < 2) { sender.sendMessage("§eVerwendung: /lobbynpc remove <index>"); return true; }
                int idx;
                try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage("§cIndex ungültig"); return true; }
                java.util.List<PlayerNpcRegistry.Entry> list = new java.util.ArrayList<>(registry.entries());
                if (idx < 0 || idx >= list.size()) { sender.sendMessage("§cIndex außerhalb des Bereichs"); return true; }
                org.bukkit.configuration.ConfigurationSection sec = plugin.getConfig().getConfigurationSection("npcs");
                if (sec != null) {
                    sec.set(String.valueOf(idx), null);
                    java.util.List<PlayerNpcRegistry.Entry> remaining = new java.util.ArrayList<>();
                    for (int i = 0; i < list.size(); i++) if (i != idx) remaining.add(list.get(i));
                    plugin.getConfig().set("npcs", null);
                    int n = 0;
                    for (PlayerNpcRegistry.Entry e : remaining) {
                        String base = "npcs." + (n++);
                        plugin.getConfig().set(base + ".name", e.name);
                        plugin.getConfig().set(base + ".lookAt", e.lookAt);
                        plugin.getConfig().set(base + ".command", e.command);
                        plugin.getConfig().set(base + ".world", e.loc.getWorld().getName());
                        plugin.getConfig().set(base + ".x", e.loc.getX());
                        plugin.getConfig().set(base + ".y", e.loc.getY());
                        plugin.getConfig().set(base + ".z", e.loc.getZ());
                        plugin.getConfig().set(base + ".yaw", e.loc.getYaw());
                    }
                    plugin.saveConfig();
                }
                registry.hideAll();
                registry.loadFromConfig();
                registry.spawnAllForOnlineViewers();
                sender.sendMessage("§aNPC Index " + idx + " entfernt.");
                return true;
            }
            case "move": { // /lobbynpc move <index>
                if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
                if (args.length < 2) { sender.sendMessage("§eVerwendung: /lobbynpc move <index>"); return true; }
                int idx; try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage("§cIndex ungültig"); return true; }
                PlayerNpcRegistry.Entry e = registry.getEntry(idx);
                if (e == null) { sender.sendMessage("§cKein NPC an diesem Index."); return true; }
                Player p = (Player) sender;
                e.loc = p.getLocation().clone();
                e.loc.setPitch(0f); // vereinheitlichen
                registry.saveNpcConfig(idx, e);
                plugin.saveConfig();
                registry.respawnAll();
                sender.sendMessage("§aNPC " + e.name + " verschoben.");
                return true;
            }
            case "setcmd": { // /lobbynpc setcmd <index> <Befehl>
                if (args.length < 3) { sender.sendMessage("§eVerwendung: /lobbynpc setcmd <index> <Command ohne />"); return true; }
                int idx; try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage("§cIndex ungültig"); return true; }
                PlayerNpcRegistry.Entry e = registry.getEntry(idx);
                if (e == null) { sender.sendMessage("§cKein NPC an diesem Index."); return true; }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) { if (i > 2) sb.append(' '); sb.append(args[i]); }
                e.command = sb.toString().startsWith("/") ? sb.substring(1) : sb.toString();
                registry.saveNpcConfig(idx, e);
                plugin.saveConfig();
                sender.sendMessage("§aCommand für NPC " + e.name + " gesetzt: /" + e.command);
                return true;
            }
            case "togglelook": { // /lobbynpc togglelook <index>
                if (args.length < 2) { sender.sendMessage("§eVerwendung: /lobbynpc togglelook <index>"); return true; }
                int idx; try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage("§cIndex ungültig"); return true; }
                PlayerNpcRegistry.Entry e = registry.getEntry(idx);
                if (e == null) { sender.sendMessage("§cKein NPC an diesem Index."); return true; }
                e.lookAt = !e.lookAt;
                registry.saveNpcConfig(idx, e);
                plugin.saveConfig();
                registry.respawnAll();
                sender.sendMessage("§aNPC " + e.name + " LookAt=" + e.lookAt);
                return true;
            }
            case "info": { // /lobbynpc info <index>
                if (args.length < 2) { sender.sendMessage("§eVerwendung: /lobbynpc info <index>"); return true; }
                int idx; try { idx = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage("§cIndex ungültig"); return true; }
                PlayerNpcRegistry.Entry e = registry.getEntry(idx);
                if (e == null) { sender.sendMessage("§cKein NPC an diesem Index."); return true; }
                sender.sendMessage("§eNPC Info:");
                sender.sendMessage("§7Name: §f" + e.name);
                sender.sendMessage("§7Pos: §f" + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f (yaw %.1f)", e.loc.getX(), e.loc.getY(), e.loc.getZ(), e.loc.getYaw()));
                sender.sendMessage("§7LookAt: §f" + e.lookAt);
                sender.sendMessage("§7Command: §f/" + e.command);
                sender.sendMessage("§7World: §f" + e.loc.getWorld().getName());
                return true;
            }
            case "reload": { // /lobbynpc reload
                plugin.reloadConfig();
                registry.respawnAll();
                sender.sendMessage("§aNPC-Config neu geladen.");
                return true;
            }
            default:
                sender.sendMessage("§e/lobbynpc list | add <Name> | remove <index>");
                return true;
        }
    }

    private String fmt(org.bukkit.Location l) {
        return String.format(java.util.Locale.ROOT, "%.1f/%.1f/%.1f", l.getX(), l.getY(), l.getZ());
    }
}
