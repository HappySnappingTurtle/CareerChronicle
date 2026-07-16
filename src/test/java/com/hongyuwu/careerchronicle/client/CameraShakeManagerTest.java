package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 引擎审计修复 任务A / A3 (表现引擎全面审计报告_2026-07-15.md A3): the "poisoning" bug and its fix.
 */
class CameraShakeManagerTest {

    @AfterEach
    void reset() {
        CameraShakeManager.clearForTesting();
    }

    @Test
    void trigger_normalCase_activatesShaking() {
        CameraShakeManager.trigger(0.5F, 6);
        assertTrue(CameraShakeManager.isShaking());
    }

    /** A3-1: this is the exact bug scenario -- intensity>0 but ticks<=0 (the pre-fix default that
     * {@link ShakeFxOp} produced whenever a skill's fx data omitted 'ticks'). Must not activate,
     * and -- more importantly -- must not "poison" {@code shakeIntensity} for future calls. */
    @Test
    void trigger_zeroTicks_rejectedOutright_doesNotActivate() {
        CameraShakeManager.trigger(0.22F, 0);
        assertFalse(CameraShakeManager.isShaking());
        assertEquals(0F, CameraShakeManager.getYawOffset(0F));
    }

    @Test
    void trigger_negativeTicks_rejectedOutright() {
        CameraShakeManager.trigger(0.22F, -5);
        assertFalse(CameraShakeManager.isShaking());
    }

    /** A3-2: the actual regression -- a poisoned call must not block subsequent *valid* calls of
     * equal or lesser intensity. Before the fix, this sequence would leave every later trigger()
     * silently doing nothing for the rest of the client session. */
    @Test
    void trigger_afterRejectedPoisonAttempt_subsequentValidTriggerStillWorks() {
        CameraShakeManager.trigger(0.22F, 0); // the "poison" attempt -- must be a no-op now
        CameraShakeManager.trigger(0.22F, 4); // same intensity as the poison attempt
        assertTrue(CameraShakeManager.isShaking(), "a same-intensity trigger after a rejected "
                + "zero-duration call must still activate -- the manager must not be poisoned");
    }

    @Test
    void trigger_strongerIntensityOverridesWeaker() {
        CameraShakeManager.trigger(0.2F, 6);
        CameraShakeManager.trigger(0.5F, 4);
        assertTrue(CameraShakeManager.isShaking());
    }

    @Test
    void tick_reachesDuration_becomesInactive() {
        CameraShakeManager.trigger(0.3F, 3);
        for (int i = 0; i < 3; i++) {
            CameraShakeManager.tick();
        }
        assertFalse(CameraShakeManager.isShaking());
    }
}
