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
        MinecraftSurvivors plugin = MinecraftSurvivors.getInstance();
        var sp = plugin.getPlayerManager().get(p.getUniqueId());
        if (args.length == 0) {
            p.sendMessage("§e/fx toggle §7- Fancy FX an/aus");
            p.sendMessage("§e/fx testgenkidama §7- Zeigt einen Meteor/Genkidama Effekt");
            p.sendMessage("§e/fx meteor <height> <speed> <radius> <damage> §7- Custom Meteor");
            p.sendMessage("§e/fx lightning | holy | venom | ranged | fire §7- Demo der FX-Visuals");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "toggle" -> {
                boolean cur = sp != null && sp.isFxEnabled();
                if (sp != null) sp.setFxEnabled(!cur);
                p.sendMessage("§aFancy FX: " + (!cur ? "AN" : "AUS"));
                return true;
            }
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
            case "ranged" -> {
                // Simuliere eine kurze Flugbahn in Blickrichtung mit Spiral + Sonic-Ringen
                org.bukkit.Location cur = p.getEyeLocation().clone();
                org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
                new org.bukkit.scheduler.BukkitRunnable() {
                    int t=0; @Override public void run() {
                        cur.add(dir.clone().multiply(1.1));
                        org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSpiral(cur.getWorld(), cur.clone().add(0,-0.2,0), 0.35, 0.6, 10, org.bukkit.Particle.END_ROD, 1.0);
                        if (t % 4 == 0) {
                            int pts = 14; double r = 0.6 + t*0.02;
                            for (int i=0;i<pts;i++) {
                                double ang = 2*Math.PI*i/pts;
                                double x = cur.getX()+Math.cos(ang)*r;
                                double z = cur.getZ()+Math.sin(ang)*r;
                                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnSafe(cur.getWorld(), org.bukkit.Particle.CRIT, new org.bukkit.Location(cur.getWorld(), x, cur.getY()+0.05, z), 1, 0.01,0.01,0.01,0.0);
                            }
                        }
                        if (++t>24) { cancel(); p.sendMessage("§aRanged FX Demo beendet."); }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
            case "fire" -> {
                // Zeige eine stationäre Flammen-Doppelhelix beim Spieler
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnHelix(p.getWorld(), p.getLocation().add(0,0.2,0), 0.6, 1.2, 24, org.bukkit.Particle.FLAME, 2);
                org.bysenom.minecraftSurvivors.util.ParticleUtil.spawnHelix(p.getWorld(), p.getLocation().add(0,0.2,0), 0.3, 1.2, 24, org.bukkit.Particle.SMOKE, 2);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_FLINTANDSTEEL_USE, 0.4f, 1.8f);
                p.sendMessage("§aFire FX Demo ausgelöst.");
            }
            default -> p.sendMessage("§cUnbekannter Subcommand. Nutze /fx für Hilfe.");
        }
        return true;
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Throwable ignored) { return def; }
    }
}
