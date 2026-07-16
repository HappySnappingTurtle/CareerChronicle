package com.hongyuwu.careerchronicle.client;

/**
 * 0.4-07 {@code camera_punch} state machine — the directional counterpart to
 * {@link CameraShakeManager}'s pure-random shake. A single kick
 * (yaw/pitch/roll) decays exponentially over its duration instead of
 * {@code CameraShakeManager}'s linear decay, matching an "impact" feel
 * (sharp then fast-fading) rather than a "rumble" feel. Coexists with
 * {@code CameraShakeManager} — {@code CareerClientEvents.onCameraAngles} adds
 * both offsets together when both are active.
 */
public final class CameraPunchManager {
    private static final float DECAY_RATE = 0.6F;

    private static float kickYaw;
    private static float kickPitch;
    private static float kickRoll;
    private static int durationTicks;
    private static int timer;

    private CameraPunchManager() {
    }

    public static void trigger(float yawKick, float pitchKick, float rollKick, int ticks) {
        if (ticks <= 0) {
            return;
        }
        kickYaw = yawKick;
        kickPitch = pitchKick;
        kickRoll = rollKick;
        durationTicks = ticks;
        timer = 0;
    }

    public static void tick() {
        if (durationTicks <= 0) {
            return;
        }
        timer++;
        if (timer >= durationTicks) {
            durationTicks = 0;
            timer = 0;
        }
    }

    public static boolean isActive() {
        return durationTicks > 0;
    }

    public static float getYawOffset(float partialTick) {
        return kickYaw * decay(partialTick);
    }

    public static float getPitchOffset(float partialTick) {
        return kickPitch * decay(partialTick);
    }

    public static float getRollOffset(float partialTick) {
        return kickRoll * decay(partialTick);
    }

    private static float decay(float partialTick) {
        if (durationTicks <= 0) {
            return 0.0F;
        }
        float t = timer + partialTick;
        return (float) Math.exp(-DECAY_RATE * t);
    }

    public static void clearForTesting() {
        kickYaw = 0;
        kickPitch = 0;
        kickRoll = 0;
        durationTicks = 0;
        timer = 0;
    }
}
