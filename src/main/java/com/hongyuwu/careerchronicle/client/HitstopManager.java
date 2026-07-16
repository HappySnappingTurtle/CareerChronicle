package com.hongyuwu.careerchronicle.client;

/**
 * 0.4-07 {@code hitstop} state machine. This is a camera-freeze
 * <em>approximation</em>, not a true global render-interpolation freeze: this
 * project has no Mixin dependency (everything goes through Forge's public
 * event bus), and Forge 1.20.1 does not expose a hook to override the
 * partialTick value fed into entity/particle rendering. What IS achievable
 * without Mixin is freezing the camera's own yaw/pitch/roll for a few ticks
 * via the same {@code ViewportEvent.ComputeCameraAngles} hook {@link CameraShakeManager}
 * already uses — the player's view stops responding to mouse input, which is
 * the single most perceptible "everything just stopped" cue, even though
 * entity animations elsewhere in the frame keep interpolating normally. See
 * 0.4-07-设计文档-打击感三件套.md §一 D1 for the full reasoning; do not treat
 * this as a full engine-level freeze in future work.
 */
public final class HitstopManager {
    private static int durationTicks;
    private static int timer;
    private static boolean captured;
    private static float frozenYaw;
    private static float frozenPitch;
    private static float frozenRoll;

    private HitstopManager() {
    }

    public static void trigger(int ticks) {
        if (ticks <= 0) {
            return;
        }
        durationTicks = ticks;
        timer = 0;
        captured = false; // capture the actual frozen angles on the next ComputeCameraAngles call
    }

    public static void tick() {
        if (durationTicks <= 0) {
            return;
        }
        timer++;
        if (timer >= durationTicks) {
            durationTicks = 0;
            timer = 0;
            captured = false;
        }
    }

    public static boolean isActive() {
        return durationTicks > 0;
    }

    /** Called once per frame while active, with the angles the camera would otherwise use — captures them on first call, returns the same frozen values every subsequent call until the freeze ends. */
    public static void captureIfNeeded(float yaw, float pitch, float roll) {
        if (!captured) {
            frozenYaw = yaw;
            frozenPitch = pitch;
            frozenRoll = roll;
            captured = true;
        }
    }

    public static float frozenYaw() {
        return frozenYaw;
    }

    public static float frozenPitch() {
        return frozenPitch;
    }

    public static float frozenRoll() {
        return frozenRoll;
    }

    public static void clearForTesting() {
        durationTicks = 0;
        timer = 0;
        captured = false;
        frozenYaw = 0;
        frozenPitch = 0;
        frozenRoll = 0;
    }
}
