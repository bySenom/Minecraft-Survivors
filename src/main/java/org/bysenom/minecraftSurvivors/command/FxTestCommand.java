package org.bysenom.minecraftSurvivors.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.fx.MeteorFx;

public class FxTestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur im Spiel nutzbar.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§e/fx testgenkidama §7- Zeigt einen Meteor/Genkidama Effekt");
            p.sendMessage("§e/fx meteor <height> <speed> <radius> <damage> §7- Custom Meteor");
            return true;
        }
        String sub = args[0].toLowerCase();
        MinecraftSurvivors plugin = MinecraftSurvivors.getInstance();
        switch (sub) {
            case "testgenkidama" -> {
                Location target = p.getTargetBlockExact(25) != null ? p.getTargetBlockExact(25).getLocation() : p.getLocation();
                target = target.clone().add(0, 1.0, 0);
                double height = plugin.getConfigUtil().getDouble("visuals.genkidama.height", 10.0);
                double radius = plugin.getConfigUtil().getDouble("visuals.genkidama.radius", 3.5);
                double damage = plugin.getConfigUtil().getDouble("visuals.genkidama.damage", 0.0);
                MeteorFx.spawnMeteor(plugin, target, height, 0.55, radius, damage, p);
                p.sendMessage("§aGenkidama Test ausgelöst!");
            }
            case "meteor" -> {
                double height = args.length > 1 ? parseDouble(args[1], 10.0) : 10.0;
                double speed = args.length > 2 ? parseDouble(args[2], 0.5) : 0.5;
                double radius = args.length > 3 ? parseDouble(args[3], 3.0) : 3.0;
                double damage = args.length > 4 ? parseDouble(args[4], 0.0) : 0.0;
                Location target = p.getTargetBlockExact(30) != null ? p.getTargetBlockExact(30).getLocation() : p.getLocation();
                target = target.clone().add(0, 1.0, 0);
                MeteorFx.spawnMeteor(plugin, target, height, Math.max(0.05, speed), radius, damage, p);
                p.sendMessage("§aCustom Meteor gestartet.");
            }
            case "lightning" -> {
                // Simpler beam between player and target block
                Location target = p.getTargetBlockExact(30) != null ? p.getTargetBlockExact(30).getLocation() : p.getLocation();
                target = target.clone().add(0.5, 0.5, 0.5);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnLine(p.getWorld(), p.getEyeLocation(), target, 24, org.bukkit.Particle.ELECTRIC_SPARK);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurst(p.getWorld(), target, org.bukkit.Particle.CRIT, 12, 0.3);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
                p.sendMessage("§aLightning Beam Test.");
            }
            case "holy" -> {
                Location center = p.getLocation();
                // Golden cascade upward
                for (int i=0;i<8;i++) {
                    Location l = center.clone().add(0, 0.3 + i*0.25, 0);
                    org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurst(p.getWorld(), l, org.bukkit.Particle.END_ROD, 6, 0.15);
                }
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(p.getWorld(), center.clone().add(0,0.2,0), 2.5, 40, org.bukkit.Particle.END_ROD);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnRing(p.getWorld(), center.clone().add(0,0.2,0), 1.2, 28, org.bukkit.Particle.CRIT);
                p.playSound(center, org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.8f);
                p.sendMessage("§aHoly Cascade Test.");
            }
            case "venom" -> {
                Location center = p.getLocation();
                int fanPts = 24;
                double maxR = 4.0;
                for (int i=0;i<fanPts;i++) {
                    double ang = Math.PI * i / fanPts; // half circle fan
                    double r = maxR;
                    Location tip = center.clone().add(Math.cos(ang)*r, 0.2, Math.sin(ang)*r);
                    org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnLine(p.getWorld(), center.clone().add(0,0.2,0), tip, 12, org.bukkit.Particle.PORTAL);
                }
                org.bukkit.Particle slimeParticle;
                try { slimeParticle = org.bukkit.Particle.valueOf("ITEM_SLIME"); } catch (Throwable ex) { slimeParticle = org.bukkit.Particle.HAPPY_VILLAGER; }
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnBurst(p.getWorld(), center.clone().add(0,0.3,0), slimeParticle, 18, 0.4);
                p.playSound(center, org.bukkit.Sound.BLOCK_SLIME_BLOCK_PLACE, 0.6f, 1.0f);
                p.sendMessage("§aVenom Spire Fog Test.");
            }
            default -> p.sendMessage("§cUnbekannter Subcommand. Nutze /fx für Hilfe.");
        }
        return true;
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Throwable ignored) { return def; }
    }
}
