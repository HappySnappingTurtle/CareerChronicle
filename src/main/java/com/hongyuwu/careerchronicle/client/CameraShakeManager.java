package com.hongyuwu.careerchronicle.client;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class CameraShakeManager {
    private static float shakeIntensity;
    private static int shakeDuration;
    private static int shakeTimer;
    private static final RandomSource RANDOM = RandomSource.create();

    private CameraShakeManager() {}

    /**
     * 引擎审计修复 任务A / A3 (表现引擎全面审计报告_2026-07-15.md A3): rejects
     * {@code durationTicks <= 0} up front. Previously a call with {@code intensity > 0} but
     * {@code durationTicks <= 0} would still win the "stronger than current" check below and set
     * {@code shakeIntensity} to a non-zero value while leaving {@code shakeDuration <= 0} --
     * {@link #tick()}'s own {@code shakeDuration <= 0} early-return then means {@code shakeIntensity}
     * is never reset back to 0 by anything, permanently "poisoning" the manager: every subsequent
     * {@code trigger} call with an intensity not strictly greater than the poisoned value silently
     * loses the "stronger than current" comparison and does nothing, for the rest of the client
     * session. {@link ShakeFxOp}'s own {@code ticks} param defaults to 0 (matches this bug exactly
     * whenever a skill's shake fx component data omits it) -- see that class's own fix.
     */
    public static void trigger(float intensity, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        if (intensity > shakeIntensity) {
            shakeIntensity = intensity;
            shakeDuration = durationTicks;
            shakeTimer = 0;
        }
    }

    public static void tick() {
        if (shakeDuration <= 0) return;
        shakeTimer++;
        if (shakeTimer >= shakeDuration) {
            shakeIntensity = 0;
            shakeDuration = 0;
            shakeTimer = 0;
        }
    }

    public static float getYawOffset(float partialTick) {
        if (shakeDuration <= 0) return 0;
        float progress = (shakeTimer + partialTick) / shakeDuration;
        float decay = 1.0F - progress;
        return (RANDOM.nextFloat() - 0.5F) * 2.0F * shakeIntensity * decay;
    }

    public static float getPitchOffset(float partialTick) {
        if (shakeDuration <= 0) return 0;
        float progress = (shakeTimer + partialTick) / shakeDuration;
        float decay = 1.0F - progress;
        return (RANDOM.nextFloat() - 0.5F) * 2.0F * shakeIntensity * decay;
    }

    public static boolean isShaking() {
        return shakeDuration > 0;
    }

    /** Test-only: resets all static state so tests don't leak into each other (mirrors
     * {@code CameraPunchManager.clearForTesting()}). */
    static void clearForTesting() {
        shakeIntensity = 0;
        shakeDuration = 0;
        shakeTimer = 0;
    }
}
