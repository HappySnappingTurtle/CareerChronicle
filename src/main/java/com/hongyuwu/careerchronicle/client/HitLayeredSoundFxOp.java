package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code hit_layered} fx op (0.4-07 §2.3): plays the same registered hit
 * sound twice — a base layer (0.5-0.8 volume) and a quieter accent layer
 * (~0.2 volume) — each independently pitch-randomized via a division-based
 * formula ("拔刀剑" recipe: {@code pitch = k / (rand*a + b)}, clamped to a
 * sane range). Zero new audio assets: both layers reuse whatever sound id the
 * skill already declares. A separate op from {@code sound} (not a parameter
 * on it) so "this is a layered hit sound" stays explicit and readable in
 * skill JSON, and {@code sound}'s existing single-play semantics (also used
 * for cast fx) don't gain an unrelated second responsibility.
 */
final class HitLayeredSoundFxOp implements FxOp {
    private static final float DEFAULT_BASE_VOLUME = 0.65F;
    private static final float MIN_BASE_VOLUME = 0.5F;
    private static final float MAX_BASE_VOLUME = 0.8F;
    private static final float DEFAULT_ACCENT_VOLUME = 0.2F;
    private static final float PITCH_MIN = 0.85F;
    private static final float PITCH_MAX = 1.3F;

    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        String idString = FxParams.getString(params, "id");
        if (idString == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(idString);
        SoundEvent sound = id == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) {
            CareerChronicleMod.LOGGER.warn("Unknown fx hit_layered sound id '{}', skipping.", idString);
            return;
        }
        float baseVolume = Mth.clamp(FxParams.getFloat(params, "base_volume", DEFAULT_BASE_VOLUME),
                MIN_BASE_VOLUME, MAX_BASE_VOLUME);
        float accentVolume = FxParams.getFloat(params, "accent_volume", DEFAULT_ACCENT_VOLUME);

        RandomSource random = ctx.level().getRandom();
        float basePitch = layeredPitch(random);
        float accentPitch = layeredPitch(random);

        ctx.level().playLocalSound(ctx.origin().x, ctx.origin().y, ctx.origin().z,
                sound, SoundSource.PLAYERS, baseVolume, basePitch, false);
        ctx.level().playLocalSound(ctx.origin().x, ctx.origin().y, ctx.origin().z,
                sound, SoundSource.PLAYERS, accentVolume, accentPitch, false);
    }

    /** {@code pitch = k / (rand*a + b)}, clamped -- see 0.4-07 §一 D3 for why division (not linear jitter). */
    static float layeredPitch(RandomSource random) {
        float raw = 1.0F / (random.nextFloat() * 0.6F + 0.55F);
        return Mth.clamp(raw, PITCH_MIN, PITCH_MAX);
    }
}
