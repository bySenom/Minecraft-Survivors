package org.bysenom.minecraftSurvivors.model;

import java.util.UUID;

public final class StatModifier {
    public final UUID id;
    public final StatType type;
    public final double value; // additive or multiplicative depending on type semantics
    public final String source; // origin (loot, glyph, weapon)

    public StatModifier(StatType type, double value, String source) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.value = value;
        this.source = source;
    }
}
