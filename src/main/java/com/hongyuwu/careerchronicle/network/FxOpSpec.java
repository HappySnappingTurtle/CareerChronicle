package com.hongyuwu.careerchronicle.network;

import net.minecraft.nbt.CompoundTag;

public record FxOpSpec(String opId, CompoundTag params) {
    public static final int MAX_OP_ID_LENGTH = 16;

    public FxOpSpec {
        if (opId == null || opId.isBlank()) {
            throw new IllegalArgumentException("opId is required");
        }
        if (opId.length() > MAX_OP_ID_LENGTH) {
            throw new IllegalArgumentException("opId too long (max " + MAX_OP_ID_LENGTH + "): " + opId);
        }
        params = params == null ? new CompoundTag() : params;
    }
}
