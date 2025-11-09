package org.bysenom.minecraftSurvivors.glyph;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Registry für Glyphen: je Ability >= 3 Vorschläge.
 * Glyph-Key Schema: abilityKey + ":" + glyphId, z.B. "ab_lightning:genkidama".
 */
public final class GlyphCatalog {

    public static final class Def {
        public final String key;        // ab_lightning:genkidama
        public final String abilityKey; // ab_lightning
        public final String name;
        public final Material icon;
        public final String desc;
        public Def(String key, String abilityKey, String name, Material icon, String desc) {
            this.key = key; this.abilityKey = abilityKey; this.name = name; this.icon = icon; this.desc = desc;
        }
        public java.util.List<Component> lore(SurvivorPlayer sp) {
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text(desc).color(NamedTextColor.GRAY));
            lore.add(Component.text("§8———"));
            lore.add(Component.text("Max 3 Glyphen pro Ability").color(NamedTextColor.DARK_GRAY));
            return lore;
        }
    }

    private static final Map<String, List<Def>> BY_ABILITY = new HashMap<>();
    private static final Map<String, Def> BY_KEY = new HashMap<>();

    static {
        // Lightning
        reg("ab_lightning", new Def("ab_lightning:genkidama", "ab_lightning", "Genkidama", Material.NETHER_STAR, "Chance, eine gewaltige Genkidama herabzurufen"));
        reg("ab_lightning", new Def("ab_lightning:storm_chain", "ab_lightning", "Sturmkette", Material.LIGHTNING_ROD, "Kettenblitze springen auf weitere Ziele"));
        reg("ab_lightning", new Def("ab_lightning:overcharge", "ab_lightning", "Überladung", Material.REDSTONE, "Alle 100 Lightning-Hits: massiver Überspannungs-Schlag"));
        // Fire
        reg("ab_fire", new Def("ab_fire:inferno", "ab_fire", "Inferno", Material.BLAZE_POWDER, "Erhöht Branddauer und AoE-Hitzeimpulse"));
        reg("ab_fire", new Def("ab_fire:phoenix", "ab_fire", "Phönix", Material.FIRE_CHARGE, "Chance auf Phönix-Explosion bei Kill"));
        reg("ab_fire", new Def("ab_fire:combust", "ab_fire", "Überschwelung", Material.MAGMA_CREAM, "Staut Hitze: Jeder 10. Tick massiver Feuerschaden"));
        // Ranged
        reg("ab_ranged", new Def("ab_ranged:multishot", "ab_ranged", "Mehrfachschuss", Material.TIPPED_ARROW, "Schießt zusätzliche Projektile"));
        reg("ab_ranged", new Def("ab_ranged:headshot", "ab_ranged", "Headshot", Material.CROSSBOW, "Chance auf doppelten Schaden"));
        reg("ab_ranged", new Def("ab_ranged:ricochet", "ab_ranged", "Abpraller", Material.SPYGLASS, "Geschosse prallen ab und treffen weitere Ziele"));
        // Holy
        reg("ab_holy", new Def("ab_holy:consecration", "ab_holy", "Weihe", Material.HEART_OF_THE_SEA, "Hinterlässt geweihtes Gebiet mit DoT"));
        reg("ab_holy", new Def("ab_holy:divine_shield", "ab_holy", "Göttlicher Schild", Material.SHIELD, "Chance, kurzzeitig zu schützen"));
        reg("ab_holy", new Def("ab_holy:penance", "ab_holy", "Buße", Material.GHAST_TEAR, "Erhöht Schaden gegen geheiligte Ziele"));
        // Shockwave
        reg("ab_shockwave", new Def("ab_shockwave:earthsplit", "ab_shockwave", "Erdspalter", Material.STONE, "Erzeugt Risse mit Nachbeben"));
        reg("ab_shockwave", new Def("ab_shockwave:vacuum", "ab_shockwave", "Vakuum", Material.ENDER_EYE, "Zieht Gegner leicht zur Mitte"));
        reg("ab_shockwave", new Def("ab_shockwave:fracture", "ab_shockwave", "Fraktur", Material.ANVIL, "Erhöht Knockback und Bruchschaden"));
        // Frost Nova
        reg("ab_frost_nova", new Def("ab_frost_nova:brittle", "ab_frost_nova", "Spröde", Material.SNOWBALL, "Verleiht Sprödigkeit: mehr Folgeschaden"));
        reg("ab_frost_nova", new Def("ab_frost_nova:shatter", "ab_frost_nova", "Zersplittern", Material.ICE, "Chance, Gefrorene zu zersplittern"));
        reg("ab_frost_nova", new Def("ab_frost_nova:glacier", "ab_frost_nova", "Gletscher", Material.PACKED_ICE, "Erzeugt länger anhaltende Eisfelder"));
        // Heal Totem
        reg("ab_heal_totem", new Def("ab_heal_totem:aegis", "ab_heal_totem", "Aegis", Material.TOTEM_OF_UNDYING, "Chance auf Schild statt Heal"));
        reg("ab_heal_totem", new Def("ab_heal_totem:pulse", "ab_heal_totem", "Heilpuls", Material.GLOW_BERRIES, "Zusätzliche Heilimpulse in Wellen"));
        reg("ab_heal_totem", new Def("ab_heal_totem:beacon", "ab_heal_totem", "Leuchtfeuer", Material.BEACON, "Stärkere Aura, aber seltener"));

        // Void Nova glyphs
        reg("ab_void_nova", new Def("ab_void_nova:gravity_well", "ab_void_nova", "Gravity Well", Material.ENDER_EYE, "Erzeugt ein Gravitationsfeld, das Gegner zur Mitte zieht"));
        reg("ab_void_nova", new Def("ab_void_nova:rupture", "ab_void_nova", "Rupture", Material.ENDER_PEARL, "Bei Explosion reißt der Raum: zusätzlicher Flächenschaden"));
        reg("ab_void_nova", new Def("ab_void_nova:lingering_void", "ab_void_nova", "Lingering Void", Material.BLACK_CONCRETE, "Hinterlässt ein kurzes Feld, das Schaden über Zeit verursacht"));

        // Temporal Rift glyphs
        reg("ab_time_rift", new Def("ab_time_rift:haste_burst", "ab_time_rift", "Haste Burst", Material.GOLDEN_APPLE, "Gewährt kurzzeitig erhöhtes Angriffstempo nach Aktivierung"));
        reg("ab_time_rift", new Def("ab_time_rift:slow_field", "ab_time_rift", "Slow Field", Material.SLIME_BALL, "Verstärkt die Verlangsamungseffekte auf Gegner im Radius"));
        reg("ab_time_rift", new Def("ab_time_rift:temporal_anchor", "ab_time_rift", "Temporal Anchor", Material.CLOCK, "Anker: Gegner kehren kurz zu ihrer Position vor Aktivierung zurück"));

        // Venom Spire glyphs
        reg("ab_venom_spire", new Def("ab_venom_spire:toxic_bloom", "ab_venom_spire", "Toxic Bloom", Material.SPIDER_EYE, "Spawns breitere Giftfelder, erhöht DoT-Area"));
        reg("ab_venom_spire", new Def("ab_venom_spire:neurotoxin", "ab_venom_spire", "Neurotoxin", Material.POISONOUS_POTATO, "Gift hat Chance, Gegner für kurze Zeit lahmzulegen"));
        reg("ab_venom_spire", new Def("ab_venom_spire:corrosive_venom", "ab_venom_spire", "Corrosive Venom", Material.GLASS_BOTTLE, "Erhöht DoT und reduziert gegnerische Rüstung (Resist) kurz") );
    }

    private static void reg(String ability, Def d) {
        BY_ABILITY.computeIfAbsent(ability, k -> new ArrayList<>()).add(d);
        BY_KEY.put(d.key, d);
    }

    public static List<Def> forAbility(String abilityKey) {
        return BY_ABILITY.getOrDefault(abilityKey, java.util.Collections.emptyList());
    }
    public static Def get(String glyphKey) { return BY_KEY.get(glyphKey); }

    /**
     * Create StatModifier objects for a given glyph key. These are non-persistent, temporary modifiers.
     */
    public static java.util.List<org.bysenom.minecraftSurvivors.model.StatModifier> createModifiersFor(String glyphKey) {
        if (glyphKey == null) return java.util.Collections.emptyList();
        switch (glyphKey) {
            // Lightning
            case "ab_lightning:genkidama": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 4.0, "glyph:genkidama"));
            case "ab_lightning:storm_chain": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.12, "glyph:storm_chain"));
            case "ab_lightning:overcharge": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.20, "glyph:overcharge"));
            // Fire
            case "ab_fire:inferno": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.12, "glyph:inferno"));
            case "ab_fire:phoenix": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.HEALTH_HEARTS, 1.0, "glyph:phoenix"));
            case "ab_fire:combust": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 3.0, "glyph:combust"));
            // Ranged
            case "ab_ranged:multishot": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 2.0, "glyph:multishot"));
            case "ab_ranged:headshot": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.10, "glyph:headshot"));
            case "ab_ranged:ricochet": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.08, "glyph:ricochet"));
            // Holy
            case "ab_holy:consecration": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN, 0.5, "glyph:consecration"));
            case "ab_holy:divine_shield": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RESIST, 0.08, "glyph:divine_shield"));
            case "ab_holy:penance": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.08, "glyph:penance"));
            // Shockwave
            case "ab_shockwave:earthsplit": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 2.0, "glyph:earthsplit"));
            case "ab_shockwave:vacuum": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.10, "glyph:vacuum"));
            case "ab_shockwave:fracture": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.12, "glyph:fracture"));
            // Frost Nova
            case "ab_frost_nova:brittle": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 1.5, "glyph:brittle"));
            case "ab_frost_nova:shatter": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.07, "glyph:shatter"));
            case "ab_frost_nova:glacier": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.10, "glyph:glacier"));
            // Heal Totem
            case "ab_heal_totem:aegis": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN, 1.0, "glyph:aegis"));
            case "ab_heal_totem:pulse": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.HEALTH_HEARTS, 1.0, "glyph:pulse"));
            case "ab_heal_totem:beacon": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN, 0.8, "glyph:beacon"));
            // Void Nova
            case "ab_void_nova:gravity_well": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.12, "glyph:gravity_well"));
            case "ab_void_nova:rupture": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 3.0, "glyph:rupture"));
            case "ab_void_nova:lingering_void": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.10, "glyph:lingering_void"));
            // Time Rift
            case "ab_time_rift:haste_burst": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.ATTACK_SPEED, 0.15, "glyph:haste_burst"));
            case "ab_time_rift:slow_field": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.10, "glyph:slow_field"));
            case "ab_time_rift:temporal_anchor": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD, 2.0, "glyph:temporal_anchor"));
            // Venom Spire
            case "ab_venom_spire:toxic_bloom": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT, 0.15, "glyph:toxic_bloom"));
            case "ab_venom_spire:neurotoxin": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT, 0.10, "glyph:neurotoxin"));
            case "ab_venom_spire:corrosive_venom": return java.util.List.of(new org.bysenom.minecraftSurvivors.model.StatModifier(org.bysenom.minecraftSurvivors.model.StatType.RESIST, -0.05, "glyph:corrosive_venom"));
            default: return java.util.Collections.emptyList();
        }
    }
}
