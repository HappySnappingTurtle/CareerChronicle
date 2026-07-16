package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.nbt.CompoundTag;

final class ShakeFxOp implements FxOp {
    // 引擎审计修复 任务A / A3 (表现引擎全面审计报告_2026-07-15.md A3): 0 used to be able to
    // "poison" CameraShakeManager permanently if a skill's fx data omitted 'ticks' (see that
    // class's trigger() javadoc). CameraShakeManager.trigger now rejects durationTicks<=0 outright
    // as the primary fix; this default is bumped from 0 to 4 so the same missing-field data still
    // produces a usable (if short) shake instead of silently doing nothing.
    private static final int DEFAULT_TICKS = 4;

    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        float strength = FxParams.getFloat(params, "strength", 0.0F);
        if (strength <= 0.0F) {
            return;
        }
        int ticks = FxParams.getInt(params, "ticks", DEFAULT_TICKS);
        CameraShakeManager.trigger(strength, ticks);
    }
}
