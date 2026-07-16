package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

final class SoundFxOp implements FxOp {
    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        String idString = FxParams.getString(params, "id");
        if (idString == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(idString);
        SoundEvent sound = id == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) {
            CareerChronicleMod.LOGGER.warn("Unknown fx sound id '{}', skipping.", idString);
            return;
        }
        float volume = FxParams.getFloat(params, "volume", 1.0F);
        float pitch = FxParams.getFloat(params, "pitch_base", 1.0F);
        ctx.level().playLocalSound(
                ctx.origin().x, ctx.origin().y, ctx.origin().z,
                sound, SoundSource.PLAYERS, volume, pitch, false);
    }
}
