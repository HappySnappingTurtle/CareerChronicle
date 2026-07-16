package com.hongyuwu.careerchronicle.client;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 引擎审计修复 任务A / A3 (表现引擎全面审计报告_2026-07-15.md A3): {@code ticks} default bump from
 * 0 to 4. {@link FxPlayContext} is passed as {@code null} throughout -- {@link ShakeFxOp} never
 * dereferences it (same pattern as {@code DelayedFxSchedulerTest}).
 */
class ShakeFxOpTest {

    @AfterEach
    void reset() {
        CameraShakeManager.clearForTesting();
    }

    /** A3-3: this is the exact real-world data shape the bug depended on -- a skill's shake fx
     * component that sets 'strength' but omits 'ticks' entirely. Before the fix (default 0),
     * {@link CameraShakeManager#trigger} would silently poison itself; now the default (4) makes it
     * actually shake. */
    @Test
    void strengthWithoutTicks_usesNonZeroDefaultAndActuallyShakes() {
        CompoundTag params = new CompoundTag();
        params.putFloat("strength", 0.22F);
        // 'ticks' deliberately omitted.

        new ShakeFxOp().play(null, params);

        assertTrue(CameraShakeManager.isShaking(), "omitting 'ticks' must still produce a usable "
                + "shake, not a silently-rejected zero-duration trigger");
    }

    @Test
    void zeroStrength_doesNothing() {
        CompoundTag params = new CompoundTag();
        params.putFloat("strength", 0F);

        new ShakeFxOp().play(null, params);

        assertFalse(CameraShakeManager.isShaking());
    }

    @Test
    void explicitTicks_overridesDefault() {
        CompoundTag params = new CompoundTag();
        params.putFloat("strength", 0.5F);
        params.putInt("ticks", 10);

        new ShakeFxOp().play(null, params);

        assertTrue(CameraShakeManager.isShaking());
    }
}
