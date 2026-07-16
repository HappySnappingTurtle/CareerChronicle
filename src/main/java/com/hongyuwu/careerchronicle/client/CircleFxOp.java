package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * {@code circle} fx op (0.4-06 §2.3, "0.4-05b占位"). Approximates a magic
 * circle with a ring of particles at the caster's feet — same
 * {@link ParticlesFxOp} particle-type support (SimpleParticleType only),
 * arranged on a circle instead of a small radial burst. 0.5-12 replaces this
 * with the real textured/shader MagicCircleRenderer; skill JSON using
 * {@code circle} today does not need to change when that happens (same
 * {@code id}/{@code count} params, {@code radius} is new but optional).
 */
final class CircleFxOp implements FxOp {
    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        String idString = FxParams.getString(params, "id");
        if (idString == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(idString);
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (!(type instanceof ParticleOptions particle)) {
            CareerChronicleMod.LOGGER.warn("Unsupported or unknown fx circle particle id '{}', skipping.", idString);
            return;
        }
        int baseCount = FxParams.getInt(params, "count", 16);
        int count = FxParams.scaledParticleCount(baseCount, ctx.particleMultiplier());
        if (count <= 0) {
            return;
        }
        float radius = FxParams.getFloat(params, "radius", 1.2F);
        Vec3 origin = ctx.origin();
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            ctx.level().addParticle(particle, x, origin.y, z, 0.0D, 0.01D, 0.0D);
        }
    }
}
