package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.4-07 A group: HitstopManager. */
class HitstopManagerTest {

    @AfterEach
    void reset() {
        HitstopManager.clearForTesting();
    }

    // A1: triggering makes it immediately active.
    @Test
    void trigger_makesActiveImmediately() {
        HitstopManager.trigger(3);
        assertTrue(HitstopManager.isActive());
    }

    // A2: frozen values are captured only once; a later captureIfNeeded with different values does not override.
    @Test
    void captureIfNeeded_onlyCapturesOnce() {
        HitstopManager.trigger(3);
        HitstopManager.captureIfNeeded(10.0F, 20.0F, 30.0F);
        HitstopManager.captureIfNeeded(99.0F, 99.0F, 99.0F);

        assertEquals(10.0F, HitstopManager.frozenYaw());
        assertEquals(20.0F, HitstopManager.frozenPitch());
        assertEquals(30.0F, HitstopManager.frozenRoll());
    }

    // A3: after `ticks` calls to tick(), the freeze ends.
    @Test
    void tick_endsAfterDuration() {
        HitstopManager.trigger(3);
        HitstopManager.tick();
        HitstopManager.tick();
        assertTrue(HitstopManager.isActive());
        HitstopManager.tick();
        assertFalse(HitstopManager.isActive());
    }

    // A4: triggering with ticks<=0 does not activate.
    @Test
    void trigger_nonPositiveTicks_doesNotActivate() {
        HitstopManager.trigger(0);
        assertFalse(HitstopManager.isActive());

        HitstopManager.trigger(-1);
        assertFalse(HitstopManager.isActive());
    }

    // A5: re-triggering while still active replaces the duration with the new one.
    @Test
    void trigger_whileActive_overridesWithNewDuration() {
        HitstopManager.trigger(3);
        HitstopManager.tick();
        HitstopManager.trigger(5);
        // 5 more ticks should now be required (timer reset by the new trigger).
        for (int i = 0; i < 4; i++) {
            HitstopManager.tick();
            assertTrue(HitstopManager.isActive(), "should still be active at tick " + i);
        }
        HitstopManager.tick();
        assertFalse(HitstopManager.isActive());
    }
}
