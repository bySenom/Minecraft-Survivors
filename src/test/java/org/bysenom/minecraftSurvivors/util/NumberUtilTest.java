package org.bysenom.minecraftSurvivors.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class NumberUtilTest {

    @Test
    void safeParseInt_defaults_on_bad_input() {
        assertEquals(10, NumberUtil.safeParseInt(null, 10));
        assertEquals(10, NumberUtil.safeParseInt("abc", 10));
        assertEquals(5, NumberUtil.safeParseInt("5", 10));
    }

    @Test
    void safeParseDouble_defaults_on_bad_input() {
        assertEquals(0.5, NumberUtil.safeParseDouble(null, 0.5), 1e-9);
        assertEquals(0.5, NumberUtil.safeParseDouble("abc", 0.5), 1e-9);
        assertEquals(2.0, NumberUtil.safeParseDouble("2.0", 0.5), 1e-9);
    }
}
