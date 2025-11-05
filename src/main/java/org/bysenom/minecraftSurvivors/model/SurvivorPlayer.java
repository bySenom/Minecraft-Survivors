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
    private double radiusMult = 0.0;     // +% radius (0.15 => +15%)
    private double damageMult = 0.0;     // +% damage multiplier
    private int igniteBonusTicks = 0;    // extra burn duration for Pyromancer
    private double knockbackBonus = 0.0; // +% knockback for Ranger
    private double healBonus = 0.0;      // + heal amount for Paladin

    // New: additional upgrade stats
    private double moveSpeedMult = 0.0;   // +% movement speed
    private double attackSpeedMult = 0.0; // +% attack speed (vanilla cooldown)
    private double damageResist = 0.0;    // % damage reduction (0.10 = 10%)
    private double luck = 0.0;            // generic luck factor 0..+

    private final java.util.Set<String> purchasedKeys = new java.util.HashSet<>();
    // shop limits tracking
    private int shopPurchasesRun = 0;
    private int shopPurchasesToday = 0;
    private String shopLastDay = null; // yyyyMMdd
    // per-item limits
    private final java.util.Map<String, Integer> perRunCounts = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> perDayCounts = new java.util.HashMap<>();

    // Ranger upgrades
    private int rangerPierce = 0;

    // Evo flags
    private boolean evoPyroNova = false;

    // Skill slots
    private int maxSkillSlots = 1;
    private final java.util.List<String> skills = new java.util.ArrayList<>(); // keys like "shockwave", "dash"
    private final java.util.Map<String,Integer> skillLevels = new java.util.HashMap<>();
    private boolean ready = false;

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

    // Setter for persistence
    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
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
        this.radiusMult = 0.0;
        this.damageMult = 0.0;
        this.igniteBonusTicks = 0;
        this.knockbackBonus = 0.0;
        this.healBonus = 0.0;
        this.moveSpeedMult = 0.0;
        this.attackSpeedMult = 0.0;
        this.damageResist = 0.0;
        this.luck = 0.0;
        this.purchasedKeys.clear();
        this.shopPurchasesRun = 0;
        this.perRunCounts.clear();
        // keep daily across runs, not reset here except run counts
        this.rangerPierce = 0;
        this.evoPyroNova = false;
        this.maxSkillSlots = 1;
        this.skills.clear();
        this.skillLevels.clear();
        this.ready = false;
    }

    public void softReset() {
        // preserve selectedClass; reset stats
        // keep level at 1 for new run
        this.kills = 0;
        this.coins = 0;
        this.classLevel = 1;
        this.xp = 0;
        this.xpToNext = 5;
        this.bonusDamage = 0.0;
        this.bonusStrikes = 0;
        this.flatDamage = 0.0;
        this.extraHearts = 0;
        this.radiusMult = 0.0;
        this.damageMult = 0.0;
        this.igniteBonusTicks = 0;
        this.knockbackBonus = 0.0;
        this.healBonus = 0.0;
        this.moveSpeedMult = 0.0;
        this.attackSpeedMult = 0.0;
        this.damageResist = 0.0;
        this.luck = 0.0;
        this.purchasedKeys.clear();
        this.shopPurchasesRun = 0;
        this.perRunCounts.clear();
        this.rangerPierce = 0;
        this.evoPyroNova = false;
        this.maxSkillSlots = 1;
        this.skills.clear();
        this.skillLevels.clear();
        this.ready = false;
        // daily persists
    }

    public void softResetPreserveSkills() {
        // preserve selectedClass, skills, skillLevels, maxSkillSlots
        this.kills = 0;
        this.coins = 0;
        this.classLevel = 1;
        this.xp = 0;
        this.xpToNext = 5;
        this.bonusDamage = 0.0;
        this.bonusStrikes = 0;
        this.flatDamage = 0.0;
        this.extraHearts = 0;
        this.radiusMult = 0.0;
        this.damageMult = 0.0;
        this.igniteBonusTicks = 0;
        this.knockbackBonus = 0.0;
        this.healBonus = 0.0;
        this.moveSpeedMult = 0.0;
        this.attackSpeedMult = 0.0;
        this.damageResist = 0.0;
        this.luck = 0.0;
        this.purchasedKeys.clear();
        this.shopPurchasesRun = 0;
        this.perRunCounts.clear();
        this.rangerPierce = 0;
        this.evoPyroNova = false;
        // keep maxSkillSlots, skills, skillLevels
        this.ready = false;
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

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public void setXpToNext(int xpToNext) {
        this.xpToNext = Math.max(1, xpToNext);
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

    public void setBonusDamage(double bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    public int getBonusStrikes() {
        return bonusStrikes;
    }

    public void addBonusStrikes(int val) {
        this.bonusStrikes += val;
    }

    public void setBonusStrikes(int bonusStrikes) {
        this.bonusStrikes = bonusStrikes;
    }

    public double getFlatDamage() {
        return flatDamage;
    }

    public void addFlatDamage(double val) {
        this.flatDamage += val;
    }

    public void setFlatDamage(double flatDamage) {
        this.flatDamage = flatDamage;
    }

    public int getExtraHearts() {
        return extraHearts;
    }

    public void addExtraHearts(int val) {
        this.extraHearts += val;
    }

    public void setExtraHearts(int extraHearts) {
        this.extraHearts = extraHearts;
    }

    // --- new upgrade stats ---
    public double getRadiusMult() { return radiusMult; }
    public void addRadiusMult(double delta) { this.radiusMult = Math.max(0.0, this.radiusMult + delta); }
    public void setRadiusMult(double radiusMult) { this.radiusMult = Math.max(0.0, radiusMult); }

    public double getDamageMult() { return damageMult; }
    public void addDamageMult(double delta) { this.damageMult = Math.max(0.0, this.damageMult + delta); }
    public void setDamageMult(double damageMult) { this.damageMult = Math.max(0.0, damageMult); }

    public int getIgniteBonusTicks() { return igniteBonusTicks; }
    public void addIgniteBonusTicks(int delta) { this.igniteBonusTicks = Math.max(0, this.igniteBonusTicks + delta); }
    public void setIgniteBonusTicks(int igniteBonusTicks) { this.igniteBonusTicks = Math.max(0, igniteBonusTicks); }

    public double getKnockbackBonus() { return knockbackBonus; }
    public void addKnockbackBonus(double delta) { this.knockbackBonus = Math.max(0.0, this.knockbackBonus + delta); }
    public void setKnockbackBonus(double knockbackBonus) { this.knockbackBonus = Math.max(0.0, knockbackBonus); }

    public double getHealBonus() { return healBonus; }
    public void addHealBonus(double delta) { this.healBonus = Math.max(0.0, this.healBonus + delta); }
    public void setHealBonus(double healBonus) { this.healBonus = Math.max(0.0, healBonus); }

    public double getMoveSpeedMult() { return moveSpeedMult; }
    public void addMoveSpeedMult(double d) { this.moveSpeedMult = Math.max(0.0, this.moveSpeedMult + d); }
    public void setMoveSpeedMult(double v) { this.moveSpeedMult = Math.max(0.0, v); }

    public double getAttackSpeedMult() { return attackSpeedMult; }
    public void addAttackSpeedMult(double d) { this.attackSpeedMult = Math.max(0.0, this.attackSpeedMult + d); }
    public void setAttackSpeedMult(double v) { this.attackSpeedMult = Math.max(0.0, v); }

    public double getDamageResist() { return damageResist; }
    public void addDamageResist(double d) { this.damageResist = Math.max(0.0, Math.min(0.9, this.damageResist + d)); }
    public void setDamageResist(double v) { this.damageResist = Math.max(0.0, Math.min(0.9, v)); }

    public double getLuck() { return luck; }
    public void addLuck(double d) { this.luck = Math.max(0.0, this.luck + d); }
    public void setLuck(double v) { this.luck = Math.max(0.0, v); }

    // --- Shop limits ---
    public int getShopPurchasesRun() { return shopPurchasesRun; }
    public int getShopPurchasesToday() { return shopPurchasesToday; }
    public String getShopLastDay() { return shopLastDay; }
    public void setShopPurchasesToday(int v) { this.shopPurchasesToday = Math.max(0, v); }
    public void setShopLastDay(String s) { this.shopLastDay = s; this.perDayCounts.clear(); }
    public void incrementShopRun() { this.shopPurchasesRun++; }
    public void incrementShopToday() { this.shopPurchasesToday++; }

    public int getPerRunCount(String key) { return perRunCounts.getOrDefault(key, 0); }
    public int getPerDayCount(String key) { return perDayCounts.getOrDefault(key, 0); }
    public void incPerRun(String key) { perRunCounts.put(key, getPerRunCount(key) + 1); }
    public void incPerDay(String key) { perDayCounts.put(key, getPerDayCount(key) + 1); }

    public boolean hasPurchased(String key) {
        if (key == null) return false;
        return purchasedKeys.contains(key);
    }

    public void markPurchased(String key) {
        if (key == null) return;
        purchasedKeys.add(key);
    }

    // Ranger
    public int getRangerPierce() { return rangerPierce; }
    public void addRangerPierce(int d) { this.rangerPierce = Math.max(0, this.rangerPierce + d); }

    // Evo flags
    public boolean isEvoPyroNova() { return evoPyroNova; }
    public void setEvoPyroNova(boolean v) { this.evoPyroNova = v; }

    public int getMaxSkillSlots() { return maxSkillSlots; }
    public void setMaxSkillSlots(int v) { this.maxSkillSlots = Math.max(1, Math.min(5, v)); }

    public java.util.List<String> getSkills() { return skills; }
    public boolean addSkill(String key) {
        if (key == null) return false;
        if (skills.contains(key)) { skillLevels.put(key, skillLevels.getOrDefault(key,1)+1); return true; }
        if (skills.size() >= maxSkillSlots) return false;
        skills.add(key);
        skillLevels.putIfAbsent(key, 1);
        return true;
    }
    public int getSkillLevel(String key) { return skillLevels.getOrDefault(key, 0); }

    public boolean isReady() { return ready; }
    public void setReady(boolean r) { this.ready = r; }

}
