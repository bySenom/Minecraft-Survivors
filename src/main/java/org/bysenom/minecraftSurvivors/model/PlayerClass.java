// File: src/main/java/org/bysenom/minecraftSurvivors/model/PlayerClass.java
package org.bysenom.minecraftSurvivors.model;

public enum PlayerClass {
    SHAMAN("Shamanen", "Beschwört Blitz als Hauptangriff (trifft zufälligen Gegner)");

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
