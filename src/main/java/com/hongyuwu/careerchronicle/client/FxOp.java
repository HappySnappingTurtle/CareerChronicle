package com.hongyuwu.careerchronicle.client;

import net.minecraft.nbt.CompoundTag;

/** Client-side fx component, mirroring the server-side EffectOp shape (0.4-05a). */
public interface FxOp {
    void play(FxPlayContext ctx, CompoundTag params);
}
