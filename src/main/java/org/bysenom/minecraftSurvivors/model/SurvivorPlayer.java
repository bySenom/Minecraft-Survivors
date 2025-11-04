// File: src/main/java/org/bysenom/minecraftSurvivors/model/SurvivorPlayer.java
package org.bysenom.minecraftSurvivors.model;

import java.util.UUID;

public class SurvivorPlayer {

    private final UUID uuid;
    private int kills = 0;
    private int coins = 0;

    // Neu: gewählte Klasse und Platz für spätere Level/Upgrades
    private PlayerClass selectedClass = null;
    private int classLevel = 1;

    public SurvivorPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void reset() {
        this.kills = 0;
        this.coins = 0;
        this.selectedClass = null;
        this.classLevel = 1;
    }

    // Neue Methoden zur Klassenverwaltung
    public PlayerClass getSelectedClass() {
        return selectedClass;
    }

    public void setSelectedClass(PlayerClass selectedClass) {
        this.selectedClass = selectedClass;
    }

    public int getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(int classLevel) {
        this.classLevel = Math.max(1, classLevel);
    }
}
