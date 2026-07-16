package com.hongyuwu.careerchronicle.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FxOpSpecParamsTest {

    // C1: sound params 缺省
    @Test
    void getFloat_missingVolumeOrPitch_defaultsTo1() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "careerchronicle:skill.cast.fire");

        assertEquals(1.0F, FxParams.getFloat(tag, "volume", 1.0F));
        assertEquals(1.0F, FxParams.getFloat(tag, "pitch_base", 1.0F));
    }

    // C2: particles count 乘法
    @Test
    void scaledParticleCount_appliesMultiplier() {
        assertEquals(0, FxParams.scaledParticleCount(12, 0.0F));
        assertEquals(6, FxParams.scaledParticleCount(12, 0.5F));
        assertEquals(24, FxParams.scaledParticleCount(12, 2.0F));
    }

    // C3: shake params 完整性
    @Test
    void getFloatAndInt_missingShakeParams_defaultToZero() {
        CompoundTag tag = new CompoundTag();

        assertEquals(0.0F, FxParams.getFloat(tag, "strength", 0.0F));
        assertEquals(0, FxParams.getInt(tag, "ticks", 0));
    }
}
