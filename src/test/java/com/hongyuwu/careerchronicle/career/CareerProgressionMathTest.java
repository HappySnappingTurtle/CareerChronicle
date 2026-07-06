package com.hongyuwu.careerchronicle.career;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CareerProgressionMathTest {

    @Test
    void xpForNextLevelAlwaysPositive() {
        for (int level = 1; level <= 50; level++) {
            assertTrue(CareerProgressionMath.xpForNextLevel(level) > 0,
                    "xpForNextLevel(" + level + ") should be positive");
        }
    }

    @Test
    void xpForNextLevelMonotonicallyIncreasing() {
        for (int level = 2; level <= 50; level++) {
            assertTrue(CareerProgressionMath.xpForNextLevel(level) >= CareerProgressionMath.xpForNextLevel(level - 1),
                    "xpForNextLevel(" + level + ") should be >= xpForNextLevel(" + (level - 1) + ")");
        }
    }

    @Test
    void xpForLevelZeroIsSafe() {
        assertTrue(CareerProgressionMath.xpForNextLevel(0) > 0);
    }

    @Test
    void segmentRequiredLevels() {
        assertEquals(1, CareerProgressionMath.requiredLevelForNextSegment(0));
        assertEquals(20, CareerProgressionMath.requiredLevelForNextSegment(1));
        assertEquals(30, CareerProgressionMath.requiredLevelForNextSegment(2));
        assertEquals(40, CareerProgressionMath.requiredLevelForNextSegment(3));
        assertEquals(50, CareerProgressionMath.requiredLevelForNextSegment(4));
    }

    @Test
    void maxSegmentsLimit() {
        assertTrue(CareerProgressionMath.hasAlphaSegmentSlot(4));
        assertFalse(CareerProgressionMath.hasAlphaSegmentSlot(5));
    }
}
