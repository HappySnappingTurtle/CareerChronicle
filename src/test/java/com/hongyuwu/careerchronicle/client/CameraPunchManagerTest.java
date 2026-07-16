package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.4-07 B group: CameraPunchManager. */
class CameraPunchManagerTest {

    @AfterEach
    void reset() {
        CameraPunchManager.clearForTesting();
    }

    // B1: right after trigger, the offset at partialTick=0 is close to the initial kick (decay not yet applied beyond t=0).
    @Test
    void trigger_initialOffsetNearKick() {
        CameraPunchManager.trigger(2.0F, 1.0F, 0.5F, 6);
        assertEquals(2.0F, CameraPunchManager.getYawOffset(0.0F), 0.01F);
        assertEquals(1.0F, CameraPunchManager.getPitchOffset(0.0F), 0.01F);
        assertEquals(0.5F, CameraPunchManager.getRollOffset(0.0F), 0.01F);
    }

    // B2: exponential decay is monotonically non-increasing in magnitude as ticks pass.
    @Test
    void decay_isMonotonicallyNonIncreasing() {
        CameraPunchManager.trigger(3.0F, 0.0F, 0.0F, 10);
        float previous = Math.abs(CameraPunchManager.getYawOffset(0.0F));
        for (int i = 0; i < 8; i++) {
            CameraPunchManager.tick();
            float current = Math.abs(CameraPunchManager.getYawOffset(0.0F));
            assertTrue(current <= previous, "offset should not increase after tick " + i);
            previous = current;
        }
    }

    // B3: after the full duration, the punch is inactive and offsets are zero.
    @Test
    void afterDuration_inactiveAndZero() {
        CameraPunchManager.trigger(1.0F, 0.0F, 0.0F, 4);
        for (int i = 0; i < 4; i++) {
            CameraPunchManager.tick();
        }
        assertFalse(CameraPunchManager.isActive());
        assertEquals(0.0F, CameraPunchManager.getYawOffset(0.0F));
    }

    // B4: zero kicks still activate (duration>0) but every offset is zero throughout.
    @Test
    void zeroKicks_activeButAllOffsetsZero() {
        CameraPunchManager.trigger(0.0F, 0.0F, 0.0F, 6);
        assertTrue(CameraPunchManager.isActive());
        assertEquals(0.0F, CameraPunchManager.getYawOffset(0.0F));
        assertEquals(0.0F, CameraPunchManager.getPitchOffset(0.3F));
        assertEquals(0.0F, CameraPunchManager.getRollOffset(0.9F));
    }

    // B5: decay formula does not produce NaN/Infinity across the partialTick range.
    @Test
    void decay_neverProducesNanOrInfinity() {
        CameraPunchManager.trigger(5.0F, 5.0F, 5.0F, 6);
        for (float partial : new float[]{0.0F, 0.5F, 0.99F}) {
            float yaw = CameraPunchManager.getYawOffset(partial);
            assertFalse(Float.isNaN(yaw) || Float.isInfinite(yaw));
        }
        for (int i = 0; i < 6; i++) {
            CameraPunchManager.tick();
            float yaw = CameraPunchManager.getYawOffset(0.99F);
            assertFalse(Float.isNaN(yaw) || Float.isInfinite(yaw));
        }
    }
}
