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

    // XP/Level System
    private int xp = 0;
    private int xpToNext = 5;

    // Upgrade-Stats (einfaches System für jetzt)
    private double bonusDamage = 0.0;    // additive bonus damage
    private int bonusStrikes = 0;        // zusätzliche Treffer pro Tick
    private double flatDamage = 0.0;     // flacher zusätzlicher Schaden
    private int extraHearts = 0;         // zusätzliche halbe Herzen (2 = 1 Herz)

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
        this.xp = 0;
        this.xpToNext = 5;
        this.bonusDamage = 0.0;
        this.bonusStrikes = 0;
        this.flatDamage = 0.0;
        this.extraHearts = 0;
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

    // XP / Level
    public int getXp() {
        return xp;
    }

    public int getXpToNext() {
        return xpToNext;
    }

    /**
     * Fügt XP hinzu. Falls genügend XP für ein Level vorhanden sind, wird das Level erhöht
     * und true zurückgegeben (sonst false). Bei mehreren Levelups wird true zurückgegeben
     * und die Levelanzahl entsprechend erhöht.
     */
    public boolean addXp(int amount) {
        if (amount <= 0) return false;
        xp += amount;
        boolean leveled = false;
        while (xp >= xpToNext) {
            xp -= xpToNext;
            classLevel++;
            xpToNext = calculateXpForLevel(classLevel);
            leveled = true;
        }
        return leveled;
    }

    private int calculateXpForLevel(int level) {
        // einfache Formel: 5 * level (kann später durch Config ersetzt werden)
        return Math.max(1, 5 * level);
    }

    // Upgrade-APIs (einfaches additive System)
    public double getBonusDamage() {
        return bonusDamage;
    }

    public void addBonusDamage(double val) {
        this.bonusDamage += val;
    }

    public int getBonusStrikes() {
        return bonusStrikes;
    }

    public void addBonusStrikes(int val) {
        this.bonusStrikes += val;
    }

    public double getFlatDamage() {
        return flatDamage;
    }

    public void addFlatDamage(double val) {
        this.flatDamage += val;
    }

    public int getExtraHearts() {
        return extraHearts;
    }

    public void addExtraHearts(int val) {
        this.extraHearts += val;
    }
}
