package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * {@code camera_punch} fx op (0.4-06 placeholder, 0.4-07 real implementation)
 * — the directional counterpart to {@code shake}'s pure-random camera
 * offset; both ops coexist ({@code CareerClientEvents.onCameraAngles} adds
 * both {@link CameraShakeManager} and {@link CameraPunchManager} offsets on
 * top of each other when both are active). See 0.4-07 §一 D2 for why
 * {@code direction} is resolved from the local player's eye position to the
 * fx's origin point rather than from {@code FxPlayContext.origin()/target()}
 * (which are the same point for hit-type fx and carry no direction on
 * their own).
 */
final class CameraPunchFxOp implements FxOp {
    private static final float DEFAULT_STRENGTH = 0.4F;
    private static final int DEFAULT_TICKS = 6;
    private static final float YAW_SCALE = 6.0F;
    private static final float PITCH_SCALE = 4.0F;
    private static final float ROLL_SCALE = 3.0F;

    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        float strength = FxParams.getFloat(params, "strength", DEFAULT_STRENGTH);
        int ticks = FxParams.getInt(params, "ticks", DEFAULT_TICKS);
        if (strength <= 0.0F || ticks <= 0) {
            return;
        }
        String direction = FxParams.getString(params, "direction");

        float yawKick;
        float pitchKick;
        if ("hit".equals(direction) && localPlayerAvailable()) {
            Player player = Minecraft.getInstance().player;
            Vec3 toImpact = ctx.origin().subtract(player.getEyePosition());
            if (toImpact.lengthSqr() < 1.0E-4) {
                yawKick = 0.0F;
                pitchKick = strength * PITCH_SCALE;
            } else {
                double impactYawDeg = Math.toDegrees(Math.atan2(-toImpact.x, toImpact.z));
                float relativeYaw = Mth.wrapDegrees((float) (impactYawDeg - player.getYRot()));
                double horizontalDist = Math.sqrt(toImpact.x * toImpact.x + toImpact.z * toImpact.z);
                double impactPitchDeg = Math.toDegrees(-Math.atan2(toImpact.y, horizontalDist));
                float relativePitch = Mth.wrapDegrees((float) (impactPitchDeg - player.getXRot()));
                // Recoil: kick opposite to the impact's relative offset (as if shoved by the hit).
                yawKick = -Math.signum(relativeYaw) * strength * YAW_SCALE;
                pitchKick = -Math.signum(relativePitch) * strength * PITCH_SCALE;
            }
        } else {
            // direction missing/unrecognized: a fixed forward-down punch, not a silent no-op.
            yawKick = 0.0F;
            pitchKick = strength * PITCH_SCALE;
        }
        float rollKick = Math.signum(yawKick) * strength * ROLL_SCALE;

        CameraPunchManager.trigger(yawKick, pitchKick, rollKick, ticks);
    }

    private static boolean localPlayerAvailable() {
        return Minecraft.getInstance().player != null;
    }
}
