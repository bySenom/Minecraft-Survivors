package org.bysenom.minecraftSurvivors.ability;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Zentrale Registry der Abilities: Key -> Definition (Name, Item, Stat-Formeln).
 */
public final class AbilityCatalog {

    public static final class Stats {
        public final double damage;     // Schaden pro Auslösung oder Tick
        public final double attacksPerSec; // Attack Speed (Hz) oder 1/cooldown
        public final double radius;     // Reichweite/Radius (falls zutreffend)
        public final double size;       // Größe/Skalierung (generisch)
        public final double duration;   // Dauer (s)
        public Stats(double damage, double attacksPerSec, double radius, double size, double duration) {
            this.damage = damage; this.attacksPerSec = attacksPerSec; this.radius = radius; this.size = size; this.duration = duration;
        }
    }

    public static final class Def {
        public final String key;
        public final String display;
        public final Material icon;
        public final String desc;
        public Def(String key, String display, Material icon, String desc) {
            this.key = key; this.display = display; this.icon = icon; this.desc = desc;
        }
        public Stats compute(SurvivorPlayer sp, int level) {
            double atkMult = sp != null ? (1.0 + sp.getAttackSpeedMult()) : 1.0;
            switch (key) {
                case "ab_lightning": {
                    long baseCd = Math.max(400, 1600 - level * 120L);
                    double effCd = Math.max(150.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double dmg = 2.0 + level * 0.8 + (sp!=null?sp.getFlatDamage():0.0);
                    double rad = 8.0 * (1.0 + (sp!=null?sp.getRadiusMult():0.0));
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 0.0);
                }
                case "ab_fire": {
                    long baseCd = Math.max(400, 1400 - level * 100L);
                    double effCd = Math.max(150.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = (4.0 + level * 0.5) * (1.0 + (sp!=null?sp.getRadiusMult():0.0));
                    double dmg = 0.8 + level * 0.4 + (sp!=null?sp.getFlatDamage():0.0) * 0.3;
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 2.0);
                }
                case "ab_ranged": {
                    long baseCd = Math.max(300, 900 - level * 60L);
                    double effCd = Math.max(120.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = (16.0 + level * 2.0) * (1.0 + (sp!=null?sp.getRadiusMult():0.0));
                    double dmg = 1.8 + level * 0.6 + (sp!=null?sp.getFlatDamage():0.0) * 0.6;
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 0.0);
                }
                case "ab_holy": {
                    long baseCd = Math.max(600, 1800 - level * 120L);
                    double effCd = Math.max(200.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = (5.0 + level * 0.6 + (sp!=null?sp.getRadiusMult():0.0) * 2.0);
                    double dmg = 1.2 + level * 0.5 + (sp!=null?sp.getFlatDamage():0.0) * 0.4;
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 0.0);
                }
                case "ab_shockwave": {
                    long baseCd = 800 - Math.min(600, level * 60L);
                    double effCd = Math.max(150.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 4.0 + level * 0.6;
                    double dmg = 1.5 + level * 0.5 + (sp!=null?sp.getFlatDamage():0.0) * 0.2;
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 0.0);
                }
                case "ab_frost_nova": {
                    long baseCd = 5000 - Math.min(3000, level * 250L);
                    double effCd = Math.max(300.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 5.0 + level * 0.5;
                    double dmg = 1.0 + level * 0.3 + (sp!=null?sp.getFlatDamage():0.0) * 0.15;
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, 2.0);
                }
                case "ab_heal_totem": {
                    long baseCd = 3000 - Math.min(2000, level * 200L);
                    double effCd = Math.max(300.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 6.0 + level * 0.5;
                    double heal = 0.6 + level * 0.2 + (sp!=null?sp.getHealBonus():0.0);
                    return new Stats(heal, aps, rad, 1.0, 2.0);
                }
                case "ab_void_nova": {
                    // Powerful void explosion that pulls and damages over time
                    long baseCd = Math.max(800, 2200 - level * 140L);
                    double effCd = Math.max(200.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 5.0 + level * 0.7 + (sp!=null?sp.getRadiusMult():0.0)*2.0;
                    double dmg = 2.2 + level * 0.9 + (sp!=null?sp.getFlatDamage():0.0) * 0.5;
                    double dur = 2.0 + Math.min(6.0, level * 0.25);
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, dur);
                }
                case "ab_time_rift": {
                    // Temporal rift that briefly slows enemies and grants player attack speed bursts
                    long baseCd = Math.max(1000, 3000 - level * 160L);
                    double effCd = Math.max(220.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 4.0 + level * 0.5;
                    double effect = 0.6 + level * 0.15; // represented as damage-like placeholder for balancing
                    double dur = 1.0 + Math.min(4.0, level * 0.15);
                    return new Stats(effect * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, dur);
                }
                case "ab_venom_spire": {
                    // Summons spires that apply poison DoT in an area
                    long baseCd = Math.max(600, 2000 - level * 130L);
                    double effCd = Math.max(180.0, baseCd / atkMult);
                    double aps = 1000.0 / effCd;
                    double rad = 3.0 + level * 0.6;
                    double dmg = 0.9 + level * 0.45 + (sp!=null?sp.getFlatDamage():0.0) * 0.25;
                    double dur = 3.0 + Math.min(6.0, level * 0.3);
                    return new Stats(dmg * (1.0 + (sp!=null?sp.getDamageMult():0.0)), aps, rad, 1.0, dur);
                }
            }
            return new Stats(1.0, 1.0, 0.0, 1.0, 0.0);
        }

        public List<Component> buildLore(SurvivorPlayer sp, int level) {
            Stats s = compute(sp, level);
            List<Component> out = new ArrayList<>();
            out.add(Component.text("§8———"));
            out.add(Component.text("LEVEL: "+level, NamedTextColor.AQUA));
            out.add(Component.text(String.format("⚔ DMG: %.2f", s.damage), NamedTextColor.GOLD));
            out.add(Component.text(String.format("⟳ ATK SPD: %.2f/s", s.attacksPerSec), NamedTextColor.YELLOW));
            if (s.radius > 0.0) out.add(Component.text(String.format("◯ RADIUS: %.1f", s.radius), NamedTextColor.GREEN));
            if (s.duration > 0.0) out.add(Component.text(String.format("⏱ DURATION: %.1fs", s.duration), NamedTextColor.GRAY));
            out.add(Component.text("§8———"));
            if (desc != null && !desc.isEmpty()) out.add(Component.text(desc, NamedTextColor.DARK_GRAY));
            return out;
        }

        public List<Component> buildDeltaLore(SurvivorPlayer sp, int level, double rarMult) {
            Stats cur = compute(sp, Math.max(1, level));
            Stats next = compute(sp, Math.max(1, level) + 1);
            List<Component> out = new ArrayList<>();
            out.add(Component.text("✦ "+rarText(rarMult)+" ✦", rarColor(rarMult)));
            if (desc != null && !desc.isEmpty()) out.add(Component.text(desc, NamedTextColor.DARK_GRAY));
            out.add(Component.text("§8———"));
            out.add(delta("DMG", cur.damage, next.damage));
            out.add(delta("ATK SPD", cur.attacksPerSec, next.attacksPerSec));
            if (cur.radius > 0.0 || next.radius > 0.0) out.add(delta("RADIUS", cur.radius, next.radius));
            if (cur.duration > 0.0 || next.duration > 0.0) out.add(delta("DURATION", cur.duration, next.duration));
            out.add(Component.text("§8———"));
            return out;
        }

        private Component delta(String label, double from, double to) {
            double d = to - from;
            return Component.text(String.format("%s: %.2f -> %.2f (+%.2f)", label, from, to, d), NamedTextColor.WHITE);
        }

        private static String rarText(double m) { return m >= 1.5 ? "EPIC" : (m >= 1.2 ? "RARE" : "COMMON"); }
        private static NamedTextColor rarColor(double m) { return m >= 1.5 ? NamedTextColor.LIGHT_PURPLE : (m >= 1.2 ? NamedTextColor.AQUA : NamedTextColor.WHITE); }
    }

    private static final Map<String, Def> MAP = new HashMap<>();
    static {
        reg(new Def("ab_lightning", "Lightning", Material.LIGHTNING_ROD, "Chain lightning hits a random target"));
        reg(new Def("ab_fire", "Fire Aura", Material.FIRE_CHARGE, "Burn enemies around you"));
        reg(new Def("ab_ranged", "Auto Shot", Material.BOW, "Auto projectile at enemies"));
        reg(new Def("ab_holy", "Holy Nova", Material.HEART_OF_THE_SEA, "Holy burst around you"));
        reg(new Def("ab_shockwave", "Shockwave", Material.STICK, "Push and damage around you"));
        reg(new Def("ab_frost_nova", "Frost Nova", Material.SNOWBALL, "Freeze and damage around you"));
        reg(new Def("ab_heal_totem", "Heal Totem", Material.TOTEM_OF_UNDYING, "Periodic healing for allies"));
        // New abilities
        reg(new Def("ab_void_nova", "Void Nova", Material.ENDER_EYE, "A void explosion that pulls enemies and deals sustained void damage"));
        reg(new Def("ab_time_rift", "Temporal Rift", Material.CLOCK, "Slows enemies briefly and grants short attack-speed bursts"));
        reg(new Def("ab_venom_spire", "Venom Spire", Material.SPIDER_EYE, "Summons poisonous spires that apply DoT in an area"));
    }

    private static void reg(Def d) { MAP.put(d.key, d); }

    public static Def get(String key) { return MAP.get(key); }
    public static Collection<Def> all() { return MAP.values(); }
}
