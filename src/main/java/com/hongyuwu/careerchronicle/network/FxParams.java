package com.hongyuwu.careerchronicle.network;

import net.minecraft.nbt.CompoundTag;

/** Defensive readers for FxOpSpec params, tolerant of keys omitted by older/newer senders. */
public final class FxParams {
    private FxParams() {
    }

    public static float getFloat(CompoundTag tag, String key, float defaultValue) {
        return tag.contains(key) ? tag.getFloat(key) : defaultValue;
    }

    public static int getInt(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key) ? tag.getInt(key) : defaultValue;
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getString(key) : null;
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
    }

    /** count scaled by the server-configured particle multiplier, rounded, never negative. */
    public static int scaledParticleCount(int baseCount, float multiplier) {
        return Math.max(0, Math.round(baseCount * multiplier));
    }
}
