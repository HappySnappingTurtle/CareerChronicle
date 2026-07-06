package com.hongyuwu.careerchronicle.client;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class CameraShakeManager {
    private static float shakeIntensity;
    private static int shakeDuration;
    private static int shakeTimer;
    private static final RandomSource RANDOM = RandomSource.create();

    private CameraShakeManager() {}

    public static void trigger(float intensity, int durationTicks) {
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
}
