package org.bysenom.minecraftSurvivors.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SurvivorPlayerStatsTest {

    @Test
    public void testXpGainAmplifiesAddXp() {
        SurvivorPlayer sp = new SurvivorPlayer(java.util.UUID.randomUUID());
        sp.addStatModifier(new StatModifier(StatType.XP_GAIN, 0.5, "test")); // +50%
        int startLevel = sp.getClassLevel();
        sp.addXp(10); // should amplify to 15 and level multiple times per internal loop
        assertTrue(sp.getClassLevel() >= startLevel + 1, "Level should have increased");
        assertTrue(sp.getXp() >= 0, "Remaining XP non-negative");
    }

    @Test
    public void testPowerupMultAmplifiesMaxHealth() {
        SurvivorPlayer sp = new SurvivorPlayer(java.util.UUID.randomUUID());
        sp.addStatModifier(new StatModifier(StatType.MAX_HEALTH, 2.0, "test")); // +2 hearts base
        sp.addStatModifier(new StatModifier(StatType.POWERUP_MULT, 1.0, "test")); // +100%
        assertTrue(sp.getMaxHealthBonusHearts() >= 4.0 - 1e-6, "Powerup mult should double max health hearts");
    }

    @Test
    public void testCritStatsNonNegative() {
        SurvivorPlayer sp = new SurvivorPlayer(java.util.UUID.randomUUID());
        sp.addStatModifier(new StatModifier(StatType.CRIT_CHANCE, 0.25, "test"));
        sp.addStatModifier(new StatModifier(StatType.CRIT_DAMAGE, 1.0, "test"));
        assertTrue(sp.getCritChance() >= 0.0 && sp.getCritChance() <= 1.0, "Crit chance clamped to [0,1]");
        assertTrue(sp.getCritDamage() >= 0.0, "Crit damage non-negative");
    }

    @Test
    public void testDefensiveCaps() {
        SurvivorPlayer sp = new SurvivorPlayer(java.util.UUID.randomUUID());
        sp.addStatModifier(new StatModifier(StatType.EVASION, 2.0, "test"));
        sp.addStatModifier(new StatModifier(StatType.RESIST, 1.0, "test"));
        assertTrue(sp.getEvasion() <= 0.90 + 1e-6, "Evasion capped at 90%");
        assertTrue(sp.getEffectiveDamageResist() <= 0.90 + 1e-6, "Resist capped at 90%");
    }
}
