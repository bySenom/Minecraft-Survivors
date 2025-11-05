// File: src/main/java/org/bysenom/minecraftSurvivors/model/PlayerClass.java
package org.bysenom.minecraftSurvivors.model;

public enum PlayerClass {
    SHAMAN("Shamanen", "Beschwört Blitz als Hauptangriff (trifft zufälligen Gegner)"),
    PYROMANCER("Pyromant", "Entfacht Flammen auf Gegnern in der Nähe (DoT & AoE)"),
    RANGER("Waldläufer", "Gezielter Schuss auf weit entfernte Gegner (starker Single-Target)"),
    PALADIN("Paladin", "Heilige Nova um dich herum (AoE-Schaden + leichte Heilung)");

    private final String displayName;
    private final String description;

    PlayerClass(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
