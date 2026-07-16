package com.hongyuwu.careerchronicle.client;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxParams;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class ParticlesFxOp implements FxOp {
    @Override
    public void play(FxPlayContext ctx, CompoundTag params) {
        String idString = FxParams.getString(params, "id");
        if (idString == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(idString);
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (!(type instanceof ParticleOptions particle)) {
            // Only SimpleParticleType (vanilla flame/snowflake/end_rod/etc.) is supported
            // in 0.4-05a; parameterized particle types (chronicle_rune) arrive in 0.5-03.
            CareerChronicleMod.LOGGER.warn("Unsupported or unknown fx particle id '{}', skipping.", idString);
            return;
        }
        int baseCount = FxParams.getInt(params, "count", 0);
        int count = FxParams.scaledParticleCount(baseCount, ctx.particleMultiplier());
        if (count <= 0) {
            return;
        }
        float spread = FxParams.getFloat(params, "spread", 0.5F);
        Vec3 origin = ctx.origin();
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double speed = 0.05D + (i % 3) * 0.01D;
            ctx.level().addParticle(
                    particle,
                    origin.x, origin.y, origin.z,
                    Math.cos(angle) * speed * spread,
                    0.02D,
                    Math.sin(angle) * speed * spread);
        }
    }
}
