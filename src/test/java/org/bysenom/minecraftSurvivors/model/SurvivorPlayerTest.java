package org.bysenom.minecraftSurvivors.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SurvivorPlayerTest {

    private SurvivorPlayer sp;
    private final String ability = "ab_unit_test";

    @BeforeEach
    public void setup() {
        sp = new SurvivorPlayer(UUID.randomUUID());
    }

    @Test
    public void testAddGlyph_basicFlow() {
        // Add three distinct glyphs
        assertTrue(sp.addGlyph(ability, ability + ":g1"));
        assertTrue(sp.addGlyph(ability, ability + ":g2"));
        assertTrue(sp.addGlyph(ability, ability + ":g3"));
        // Now full - adding another should fail
        assertFalse(sp.addGlyph(ability, ability + ":g4"));
        // Duplicate should fail
        assertFalse(sp.addGlyph(ability, ability + ":g2"));
        // Check list size and contents
        var glyphs = sp.getGlyphs(ability);
        assertEquals(3, glyphs.size());
        assertTrue(glyphs.contains(ability + ":g1"));
        assertTrue(glyphs.contains(ability + ":g2"));
        assertTrue(glyphs.contains(ability + ":g3"));
    }

    @Test
    public void testReplaceGlyph_and_remove() {
        // prepare two glyphs
        assertTrue(sp.addGlyph(ability, ability + ":a"));
        assertTrue(sp.addGlyph(ability, ability + ":b"));
        // replace index 0
        assertTrue(sp.replaceGlyph(ability, 0, ability + ":c"));
        var glyphs = sp.getGlyphs(ability);
        assertEquals(2, glyphs.size());
        assertEquals(ability + ":c", glyphs.get(0));
        // remove slot 1
        assertTrue(sp.replaceGlyph(ability, 1, null));
        glyphs = sp.getGlyphs(ability);
        // After removal, slot 1 should be null (list keeps size)
        assertTrue(glyphs.size() >= 2);
        assertNull(glyphs.get(1));
    }

    @Test
    public void testAbilityAddAndIncrement() {
        // add ability
        boolean added = sp.addAbilityAtFirstFreeIndex("ab_fire", 1) >= 0;
        assertTrue(added);
        // increment level
        assertTrue(sp.incrementAbilityLevel("ab_fire", 1));
        assertEquals(2, sp.getAbilityLevelsMap().getOrDefault("ab_fire", 0).intValue());
        // increment non-existing ability fails
        assertFalse(sp.incrementAbilityLevel("ab_unknown", 1));
    }

    @Test
    public void testAddAbility_fullSlots() {
        sp.setMaxAbilitySlots(2);
        assertTrue(sp.addAbilityAtFirstFreeIndex("ab1", 1) >= 0);
        assertTrue(sp.addAbilityAtFirstFreeIndex("ab2", 1) >= 0);
        // third should fail
        assertEquals(-1, sp.addAbilityAtFirstFreeIndex("ab3", 1));
    }
}

