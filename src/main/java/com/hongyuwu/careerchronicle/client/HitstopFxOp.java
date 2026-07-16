package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * {@code hitstop} fx op (0.4-06 placeholder, 0.4-07 real implementation). See
 * {@link HitstopManager}'s Javadoc for the exact scope of this effect (a
 * camera-freeze approximation, not a true render-pipeline freeze).
 */
final class HitstopFxOp implements FxOp {
    private static final int DEFAULT_TICKS = 3;
    private static final int MIN_TICKS = 1;
    private static final int MAX_TICKS = 6;

    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        int ticks = Mth.clamp(FxParams.getInt(params, "ticks", DEFAULT_TICKS), MIN_TICKS, MAX_TICKS);
        HitstopManager.trigger(ticks);
    }
}
