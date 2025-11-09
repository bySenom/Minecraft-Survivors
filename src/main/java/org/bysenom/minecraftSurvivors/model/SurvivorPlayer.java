// File: src/main/java/org/bysenom/minecraftSurvivors/model/SurvivorPlayer.java
package org.bysenom.minecraftSurvivors.model;

import java.util.UUID;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

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
    private double bonusDamage = 0.0;    // additive bonus damage (legacy, wird in flatDamage zusammengeführt)
    private int bonusStrikes = 0;        // zusätzliche Treffer pro Tick
    private double flatDamage = 0.0;     // flacher zusätzlicher Schaden (kanonisch)
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
    private final java.util.Map<String, Integer> skillLevels = new java.util.HashMap<>();
    private boolean ready = false;
    // Visual preferences
    private boolean fxEnabled = true;
    public boolean isFxEnabled() { return fxEnabled; }
    public void setFxEnabled(boolean enabled) { this.fxEnabled = enabled; }

    // Weapons (passive), ähnlich wie Skills
    private int maxWeaponSlots = 6;
    private final java.util.List<String> weapons = new java.util.ArrayList<>(); // keys like w_lightning, w_fire
    private final java.util.Map<String, Integer> weaponLevels = new java.util.HashMap<>();

    // Unified Abilities (ersetzt Skills+Weapons)
    private int maxAbilitySlots = 5;
    private final java.util.List<String> abilities = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> abilityLevels = new java.util.HashMap<>();
    private final java.util.Map<String, String> abilityOrigins = new java.util.HashMap<>(); // e.g. ability -> "class" | "levelup" | "loot"

    // Glyphen: je Ability bis zu 3 Sockel
    private final java.util.Map<String, java.util.List<String>> abilityGlyphs = new java.util.HashMap<>();
    // Unlocks (purchased/unlocked items)
    private final java.util.Set<String> unlockedAbilities = new java.util.HashSet<>();
    private final java.util.Set<String> unlockedGlyphs = new java.util.HashSet<>();
    // Zähler/Trigger (z.B. Lightning-Hits)
    private final java.util.Map<String, Integer> glyphCounters = new java.util.HashMap<>();

    // --- Combat stat runtime state ---
    private double shieldCurrent = 0.0; // current shield value (absorbs damage first)
    private long lastDamagedAtMillis = 0L; // for shield regen delay

    public double getShieldCurrent() { return Math.max(0.0, shieldCurrent); }
    public void setShieldCurrent(double v) { this.shieldCurrent = Math.max(0.0, v); }
    public void addShield(double d) { this.shieldCurrent = Math.max(0.0, this.shieldCurrent + d); }
    public long getLastDamagedAtMillis() { return lastDamagedAtMillis; }
    public void markDamagedNow() { this.lastDamagedAtMillis = System.currentTimeMillis(); }

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
        this.maxWeaponSlots = 6;
        this.weapons.clear();
        this.weaponLevels.clear();
        this.maxAbilitySlots = 5;
        this.abilities.clear();
        this.abilityLevels.clear();
        this.abilityGlyphs.clear();
        this.glyphCounters.clear();
        this.ready = false;
        this.fxEnabled = true;
    }

    public void softReset() {
        // preserve selectedClass; reset stats
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
        this.maxWeaponSlots = 6;
        this.weapons.clear();
        this.weaponLevels.clear();
        this.maxAbilitySlots = 5;
        this.abilities.clear();
        this.abilityLevels.clear();
        this.abilityGlyphs.clear();
        this.glyphCounters.clear();
        this.ready = false;
        this.fxEnabled = true;
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
        // Waffen zurücksetzen, da run-basiert
        this.maxWeaponSlots = 6;
        this.weapons.clear();
        this.weaponLevels.clear();
        // unified abilities sind run-basiert
        this.maxAbilitySlots = 5;
        // Preserve class-origin abilities: collect them, clear all abilities, then re-add class ones
        java.util.List<String> preserved = new java.util.ArrayList<>();
        java.util.Map<String, Integer> preservedLevels = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> en : abilityOrigins.entrySet()) {
            if ("class".equals(en.getValue())) {
                String key = en.getKey();
                preserved.add(key);
                preservedLevels.put(key, abilityLevels.getOrDefault(key, 1));
            }
        }
        this.abilities.clear();
        this.abilityLevels.clear();
        this.abilityOrigins.clear();
        // re-add preserved class abilities
        for (String k : preserved) {
            if (k == null) continue;
            if (this.abilities.size() < this.maxAbilitySlots) {
                this.abilities.add(k);
                this.abilityLevels.put(k, Math.max(1, preservedLevels.getOrDefault(k, 1)));
                this.abilityOrigins.put(k, "class");
            }
        }
        this.abilityGlyphs.clear();
        this.glyphCounters.clear();
        this.ready = false;
        this.fxEnabled = true;
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

    public boolean addXp(int amount) {
        if (amount <= 0) return false;
        int gained = (int)Math.round(amount * (1.0 + getXpGain()));
        if (gained <= 0) return false;
        xp += gained;
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
        return Math.max(1, 5 * level);
    }

    // Upgrade-APIs
    public double getBonusDamage() {
        return 0.0;
    } // Legacy: nicht mehr separat verwendet

    public void addBonusDamage(double val) {
        this.flatDamage += val;
    } // leite auf flatDamage um

    public void setBonusDamage(double bonusDamage) {
        this.flatDamage = bonusDamage;
        this.bonusDamage = 0.0;
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

    // Einheitliche additive Schadenssumme
    public double getDamageAddTotal() {
        return Math.max(0.0, this.flatDamage + this.bonusDamage);
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

    public int getExtraHearts() {
        return extraHearts;
    }

    public void addExtraHearts(int val) {
        this.extraHearts += val;
    }

    public void setExtraHearts(int extraHearts) {
        this.extraHearts = extraHearts;
    }

    public double getRadiusMult() {
        return radiusMult;
    }

    public void addRadiusMult(double delta) {
        this.radiusMult = Math.max(0.0, this.radiusMult + delta);
    }

    public void setRadiusMult(double radiusMult) {
        this.radiusMult = Math.max(0.0, radiusMult);
    }

    public double getDamageMult() {
        return damageMult;
    }

    public void addDamageMult(double delta) {
        this.damageMult = Math.max(0.0, this.damageMult + delta);
    }

    public void setDamageMult(double damageMult) {
        this.damageMult = Math.max(0.0, damageMult);
    }

    public int getIgniteBonusTicks() {
        return igniteBonusTicks;
    }

    public void addIgniteBonusTicks(int delta) {
        this.igniteBonusTicks = Math.max(0, this.igniteBonusTicks + delta);
    }

    public void setIgniteBonusTicks(int igniteBonusTicks) {
        this.igniteBonusTicks = Math.max(0, igniteBonusTicks);
    }

    public double getKnockbackBonus() {
        return knockbackBonus;
    }

    public void addKnockbackBonus(double delta) {
        this.knockbackBonus = Math.max(0.0, this.knockbackBonus + delta);
    }

    public void setKnockbackBonus(double knockbackBonus) {
        this.knockbackBonus = Math.max(0.0, knockbackBonus);
    }

    public double getHealBonus() {
        return healBonus;
    }

    public void addHealBonus(double delta) {
        this.healBonus = Math.max(0.0, this.healBonus + delta);
    }

    public void setHealBonus(double healBonus) {
        this.healBonus = Math.max(0.0, healBonus);
    }

    public double getMoveSpeedMult() {
        return moveSpeedMult;
    }

    public void addMoveSpeedMult(double d) {
        this.moveSpeedMult = Math.max(0.0, this.moveSpeedMult + d);
    }

    public void setMoveSpeedMult(double v) {
        this.moveSpeedMult = Math.max(0.0, v);
    }

    public double getAttackSpeedMult() {
        return attackSpeedMult;
    }

    public void addAttackSpeedMult(double d) {
        this.attackSpeedMult = Math.max(0.0, this.attackSpeedMult + d);
    }

    public void setAttackSpeedMult(double v) {
        this.attackSpeedMult = Math.max(0.0, v);
    }

    public double getDamageResist() {
        return damageResist;
    }

    public void addDamageResist(double d) {
        this.damageResist = Math.max(0.0, Math.min(0.9, this.damageResist + d));
    }

    public void setDamageResist(double v) {
        this.damageResist = Math.max(0.0, Math.min(0.9, v));
    }

    public double getLuck() {
        return luck;
    }

    public void addLuck(double d) {
        this.luck = Math.max(0.0, this.luck + d);
    }

    public void setLuck(double v) {
        this.luck = Math.max(0.0, v);
    }

    // --- Shop limits ---
    public int getShopPurchasesRun() {
        return shopPurchasesRun;
    }

    public int getShopPurchasesToday() {
        return shopPurchasesToday;
    }

    public String getShopLastDay() {
        return shopLastDay;
    }

    public void setShopPurchasesToday(int v) {
        this.shopPurchasesToday = Math.max(0, v);
    }

    public void setShopLastDay(String s) {
        this.shopLastDay = s;
        this.perDayCounts.clear();
    }

    public void incrementShopRun() {
        this.shopPurchasesRun++;
    }

    public void incrementShopToday() {
        this.shopPurchasesToday++;
    }

    public int getPerRunCount(String key) {
        return perRunCounts.getOrDefault(key, 0);
    }

    public int getPerDayCount(String key) {
        return perDayCounts.getOrDefault(key, 0);
    }

    public void incPerRun(String key) {
        perRunCounts.put(key, getPerRunCount(key) + 1);
    }

    public void incPerDay(String key) {
        perDayCounts.put(key, getPerDayCount(key) + 1);
    }

    public boolean hasPurchased(String key) {
        return key != null && purchasedKeys.contains(key);
    }

    public void markPurchased(String key) {
        if (key != null) purchasedKeys.add(key);
    }

    // Ranger
    public int getRangerPierce() {
        return rangerPierce;
    }

    public void addRangerPierce(int d) {
        this.rangerPierce = Math.max(0, this.rangerPierce + d);
    }

    // Evo flags
    public boolean isEvoPyroNova() {
        return evoPyroNova;
    }

    public void setEvoPyroNova(boolean v) {
        this.evoPyroNova = v;
    }

    public int getMaxSkillSlots() {
        return maxSkillSlots;
    }

    public void setMaxSkillSlots(int v) {
        this.maxSkillSlots = Math.max(1, Math.min(5, v));
    }

    public java.util.List<String> getSkills() {
        return skills;
    }

    public boolean addSkill(String key) {
        if (key == null) return false;
        if (skills.contains(key)) {
            skillLevels.put(key, skillLevels.getOrDefault(key, 1) + 1);
            return true;
        }
        if (skills.size() >= maxSkillSlots) return false;
        skills.add(key);
        skillLevels.putIfAbsent(key, 1);
        return true;
    }

    public int getSkillLevel(String key) {
        return skillLevels.getOrDefault(key, 0);
    }

    public boolean removeSkill(String key) {
        if (key == null) return false;
        boolean removed = skills.remove(key);
        skillLevels.remove(key);
        return removed;
    }

    public void clearSkills() {
        skills.clear();
        skillLevels.clear();
    }

    // Unified Abilities API
    public int getMaxAbilitySlots() {
        return maxAbilitySlots;
    }

    public void setMaxAbilitySlots(int v) {
        this.maxAbilitySlots = Math.max(1, Math.min(5, v));
    }

    public java.util.List<String> getAbilities() {
        return abilities;
    }

    public int getAbilityLevel(String key) {
        return abilityLevels.getOrDefault(key, 0);
    }

    public boolean addAbility(String key) {
        if (key == null) return false;
        if (abilities.contains(key)) {
            abilityLevels.put(key, abilityLevels.getOrDefault(key, 1) + 1);
            return true;
        }
        if (abilities.size() >= maxAbilitySlots) return false;
        abilities.add(key);
        abilityLevels.putIfAbsent(key, 1);
        return true;
    }

    public boolean replaceAbilityAt(int index, String newKey, int newLevel) {
        if (newKey == null) return false;
        if (index < 0 || index >= abilities.size()) return false;
        String old = abilities.set(index, newKey);
        // Level-Map anpassen
        abilityLevels.remove(old);
        abilityLevels.put(newKey, Math.max(1, newLevel));
        // move origin: remove old origin, set new origin to null (caller should set)
        abilityOrigins.remove(old);
        abilityOrigins.remove(newKey);
        return true;
    }

    public boolean hasAbility(String key) {
        return key != null && abilities.contains(key);
    }

    public boolean addNewAbilityWithLevel(String key, int level) {
        if (key == null) return false;
        if (abilities.contains(key)) return false;
        if (abilities.size() >= maxAbilitySlots) return false;
        abilities.add(key);
        abilityLevels.put(key, Math.max(1, level));
        return true;
    }

    /**
     * Try to add ability into the first free slot. Returns true if added, false if no free slot or already exists.
     */
    public boolean addAbilityAtFirstFree(String key, int level) {
        if (key == null) return false;
        if (abilities.contains(key)) return false;
        // find explicit null/empty slots first
        for (int i = 0; i < abilities.size(); i++) {
            String v = abilities.get(i);
            if (v == null || v.isEmpty()) {
                abilities.set(i, key);
                abilityLevels.put(key, Math.max(1, level));
                return true;
            }
        }
        if (abilities.size() < maxAbilitySlots) {
            abilities.add(key);
            abilityLevels.put(key, Math.max(1, level));
            return true;
        }
        return false;
    }

    /**
     * Add ability into first free slot and return the index where it was placed, or -1 if failed/existing.
     */
    public int addAbilityAtFirstFreeIndex(String key, int level) {
        if (key == null) return -1;
        if (abilities.contains(key)) return -1;
        // find explicit null/empty slots first
        for (int i = 0; i < abilities.size(); i++) {
            String v = abilities.get(i);
            if (v == null || v.isEmpty()) {
                abilities.set(i, key);
                abilityLevels.put(key, Math.max(1, level));
                return i;
            }
        }
        if (abilities.size() < maxAbilitySlots) {
            abilities.add(key);
            abilityLevels.put(key, Math.max(1, level));
            return abilities.size() - 1;
        }
        return -1;
    }

    public boolean incrementAbilityLevel(String key, int delta) {
        if (key == null) return false;
        if (!abilities.contains(key)) return false;
        int cur = abilityLevels.getOrDefault(key, 1);
        abilityLevels.put(key, Math.max(1, cur + Math.max(1, delta)));
        return true;
    }

    // Ability origin helpers (class/levelup/loot)
    public void setAbilityOrigin(String abilityKey, String origin) {
        if (abilityKey == null) return;
        if (origin == null) abilityOrigins.remove(abilityKey);
        else abilityOrigins.put(abilityKey, origin);
    }

    public String getAbilityOrigin(String abilityKey) {
        return abilityKey == null ? null : abilityOrigins.get(abilityKey);
    }

    // Weapons API
    public java.util.List<String> getWeapons() {
        return weapons;
    }

    public int getWeaponLevel(String key) {
        return weaponLevels.getOrDefault(key, 0);
    }

    public int getMaxWeaponSlots() {
        return maxWeaponSlots;
    }

    public void setMaxWeaponSlots(int v) {
        this.maxWeaponSlots = Math.max(1, Math.min(8, v));
    }

    public boolean addWeapon(String key) {
        if (key == null) return false;
        if (weapons.contains(key)) {
            weaponLevels.put(key, weaponLevels.getOrDefault(key, 1) + 1);
            return true;
        }
        if (weapons.size() >= maxWeaponSlots) return false;
        weapons.add(key);
        weaponLevels.putIfAbsent(key, 1);
        return true;
    }

    // Glyph API
    public java.util.List<String> getGlyphs(String abilityKey) {
        return abilityGlyphs.computeIfAbsent(String.valueOf(abilityKey), k -> new java.util.ArrayList<>());
    }
    // Track created modifier IDs per glyphKey so we can remove them later
    private final java.util.Map<String, java.util.List<java.util.UUID>> glyphModifierIds = new java.util.HashMap<>();

    private void applyGlyphModifiers(String glyphKey) {
        if (glyphKey == null) return;
        try {
            org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def def = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.get(glyphKey);
            if (def == null) return;
            java.util.List<org.bysenom.minecraftSurvivors.model.StatModifier> mods = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.createModifiersFor(def.key);
            if (mods == null || mods.isEmpty()) return;
            java.util.List<java.util.UUID> ids = glyphModifierIds.computeIfAbsent(glyphKey, k -> new java.util.ArrayList<>());
            for (var m : mods) {
                java.util.UUID id = addStatModifier(m);
                if (id != null) ids.add(id);
            }
        } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("applyGlyphModifiers failed for glyph " + glyphKey + ": ", t); }
    }

    private void removeGlyphModifiers(String glyphKey) {
        if (glyphKey == null) return;
        try {
            java.util.List<java.util.UUID> ids = glyphModifierIds.remove(glyphKey);
            if (ids == null) return;
            for (java.util.UUID id : ids) {
                try { removeStatModifier(id); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("removeStatModifier failed for id " + id + ": ", t); }
            }
        } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("removeGlyphModifiers failed for glyph " + glyphKey + ": ", t); }
    }

    public boolean addGlyph(String abilityKey, String glyphKey) {
        if (abilityKey == null || glyphKey == null) return false;
        java.util.List<String> list = getGlyphs(abilityKey);
        if (list.size() >= 3) return false;
        if (list.contains(glyphKey)) return false;
        list.add(glyphKey);
        // apply modifiers for this glyph (non-persistent temporary buffs)
        applyGlyphModifiers(glyphKey);
        return true;
    }

    public boolean replaceGlyph(String abilityKey, int index, String glyphKey) {
        if (abilityKey == null) return false;
        java.util.List<String> list = getGlyphs(abilityKey);
        if (index < 0 || index >= 3) return false;
        while (list.size() <= index) list.add(null);
        // If removing
        String old = list.get(index);
        if (glyphKey == null) {
            // remove old glyph and its modifiers
            list.set(index, null);
            if (old != null && !old.isEmpty()) removeGlyphModifiers(old);
            return true;
        }
        // Prevent duplicate glyphs for same ability
        if (list.contains(glyphKey)) return false;
        // Replace: remove old modifiers, set new glyph, apply new modifiers
        if (old != null && !old.isEmpty()) removeGlyphModifiers(old);
        list.set(index, glyphKey);
        applyGlyphModifiers(glyphKey);
        return true;
    }

    public int getCounter(String key) {
        return glyphCounters.getOrDefault(String.valueOf(key), 0);
    }

    public void setCounter(String key, int v) {
        glyphCounters.put(String.valueOf(key), Math.max(0, v));
    }

    public int incCounter(String key, int d) {
        int v = getCounter(key) + d;
        setCounter(key, v);
        return v;
    }

    // --- StatModifier system ---
    // Modifiers are additive values attached to a player (source: loot/glyph/weapon)
    private final java.util.List<org.bysenom.minecraftSurvivors.model.StatModifier> statModifiers = new java.util.ArrayList<>();

    public java.util.UUID addStatModifier(org.bysenom.minecraftSurvivors.model.StatModifier m) {
        if (m == null) return null;
        statModifiers.add(m);
        return m.id;
    }

    public boolean removeStatModifier(java.util.UUID id) {
        if (id == null) return false;
        return statModifiers.removeIf(sm -> sm.id.equals(id));
    }

    public void removeStatModifiersIf(java.util.function.Predicate<org.bysenom.minecraftSurvivors.model.StatModifier> pred) {
        if (pred == null) return;
        statModifiers.removeIf(pred);
    }

    public java.util.List<org.bysenom.minecraftSurvivors.model.StatModifier> getModifiersFor(org.bysenom.minecraftSurvivors.model.StatType t) {
        java.util.List<org.bysenom.minecraftSurvivors.model.StatModifier> out = new java.util.ArrayList<>();
        for (var m : statModifiers) if (m.type == t) out.add(m);
        return out;
    }

    public double getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType t) {
        double s = 0.0;
        for (var m : statModifiers) if (m.type == t) s += m.value;
        return s;
    }

    // Effective getters combine base persistent stats with dynamic modifiers
    public double getEffectiveDamageMult() {
        return Math.max(0.0, this.damageMult + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT));
    }

    public double getEffectiveDamageAdd() {
        double add = this.flatDamage + this.bonusDamage;
        add += getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD);
        return Math.max(0.0, add);
    }

    public double getEffectiveRadiusMult() {
        return Math.max(0.0, this.radiusMult + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT));
    }

    public double getEffectiveMoveSpeedMult() {
        return Math.max(0.0, this.moveSpeedMult + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.SPEED));
    }

    public double getEffectiveAttackSpeedMult() {
        return Math.max(0.0, this.attackSpeedMult + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.ATTACK_SPEED));
    }

    public double getEffectiveDamageResist() {
        return Math.max(0.0, Math.min(0.9, this.damageResist + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.RESIST)));
    }

    public double getEffectiveLuck() {
        return Math.max(0.0, this.luck + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.LUCK));
    }

    public int getEffectiveExtraHearts() { return this.extraHearts + (int)Math.round(getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.HEALTH_HEARTS)); }

    // --- Combat stat runtime state ---
    // (duplicate definitions removed below)

    // --- Effective combat stat getters (from StatModifiers) ---
    public double getArmor() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.ARMOR)); return clampByConfig("combat.cap.armor", raw, 0.90); }
    public double getEvasion() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.EVASION)); return clampByConfig("combat.cap.evasion", raw, 0.90); }
    public double getLifesteal() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.LIFESTEAL)); return clampByConfig("combat.cap.lifesteal", raw, 1.00); }
    public double getThorns() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.THORNS)); return clampByConfig("combat.cap.thorns", raw, 1.50); }
    public double getCritChance() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.CRIT_CHANCE)); return clampByConfig("combat.cap.crit_chance", raw, 1.00); }
    public double getCritDamage() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.CRIT_DAMAGE)); return clampByConfig("combat.cap.crit_damage", raw, 5.00); }
    public double getShieldMax() { double raw = Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.SHIELD)); return clampByConfig("combat.cap.shield", raw, raw); }
    // New effective multipliers for size & duration
    public double getEffectiveSizeMult() { return Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.SIZE)); }
    public double getEffectiveDurationMult() { return Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.DURATION)); }
    private double clampByConfig(String path, double value, double defCap) {
        try {
            MinecraftSurvivors pl = MinecraftSurvivors.getInstance();
            if (pl != null && pl.getConfigUtil() != null) {
                double cap = pl.getConfigUtil().getDouble(path, defCap);
                return Math.min(value, Math.max(0.0, cap));
            }
        } catch (Throwable ignored) {}
        return Math.min(value, defCap);
    }

    public boolean removeAbility(String key) {
        if (key == null) return false;
        int idx = abilities.indexOf(key);
        if (idx < 0) return false;
        abilities.remove(idx);
        abilityLevels.remove(key);
        abilityOrigins.remove(key);
        // remove any glyph modifiers tied to glyphs of this ability
        java.util.List<String> glyphs = abilityGlyphs.remove(key);
        if (glyphs != null) {
            for (String g : glyphs) try { removeGlyphModifiers(g); } catch (Throwable t) { org.bysenom.minecraftSurvivors.util.LogUtil.logFine("removeGlyphModifiers failed during removeAbility for glyph " + g + ": ", t); }
        }
        return true;
    }

    private double hpRegen = 0.0; // regen pro Sekunde
    public void addHpRegen(double v){ this.hpRegen = Math.max(0.0, this.hpRegen + v); }
    public double getHpRegen(){ return Math.max(0.0, this.hpRegen + getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN)); }

    // --- Readiness & Unlock APIs (restored) ---
    public boolean isReady() { return ready; }
    public void setReady(boolean r) { this.ready = r; }

    public java.util.Set<String> getUnlockedAbilities() { return java.util.Collections.unmodifiableSet(unlockedAbilities); }
    public java.util.Set<String> getUnlockedGlyphs() { return java.util.Collections.unmodifiableSet(unlockedGlyphs); }

    public boolean hasUnlockedAbility(String abilityKey) { return abilityKey != null && unlockedAbilities.contains(abilityKey); }
    public boolean unlockAbility(String abilityKey) { if (abilityKey == null) return false; if (unlockedAbilities.contains(abilityKey)) return false; unlockedAbilities.add(abilityKey); return true; }
    public boolean lockAbility(String abilityKey) { if (abilityKey == null) return false; return unlockedAbilities.remove(abilityKey); }

    public boolean hasUnlockedGlyph(String glyphKey) { return glyphKey != null && unlockedGlyphs.contains(glyphKey); }
    public boolean unlockGlyph(String glyphKey) { if (glyphKey == null) return false; if (unlockedGlyphs.contains(glyphKey)) return false; unlockedGlyphs.add(glyphKey); return true; }
    public boolean lockGlyph(String glyphKey) { if (glyphKey == null) return false; return unlockedGlyphs.remove(glyphKey); }

    // --- New base stat integration (POWERUP_MULT amplification) ---
    public double getPowerupMult() { return Math.max(0.0, getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.POWERUP_MULT)); }
    private double amp(double base) { double m = 1.0 + getPowerupMult(); return base * Math.max(0.0, m); }
    public double getMaxHealthBonusHearts() { return Math.max(0.0, amp(getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.MAX_HEALTH))); }
    public double getXpGain() { return Math.max(0.0, amp(getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.XP_GAIN))); }
    public double getDamageEliteBoss() { return Math.max(0.0, amp(getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ELITE_BOSS))); }
    public double getKnockbackEffective() { return Math.max(0.0, this.knockbackBonus + amp(getStatModifierSum(org.bysenom.minecraftSurvivors.model.StatType.KNOCKBACK))); }
}
