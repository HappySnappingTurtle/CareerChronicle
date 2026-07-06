package com.hongyuwu.careerchronicle.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = COMMON_BUILDER
            .comment("Enable extra debug logging for Career Chronicle development builds.")
            .define("enableDebugLogging", true);

    public static final ForgeConfigSpec.DoubleValue CAREER_XP_MULTIPLIER = COMMON_BUILDER
            .comment("Multiplier for all Career Chronicle career XP gains. 1.0 keeps default progression, 0 disables career XP gains.")
            .defineInRange("careerXpMultiplier", 1.0D, 0.0D, 10.0D);

    public static final ForgeConfigSpec.BooleanValue ENABLE_SKILL_PVP = COMMON_BUILDER
            .comment("Allow Career Chronicle offensive skills to damage or debuff other players. Vanilla server PVP and team rules still apply.")
            .define("enableSkillPvp", false);

    public static final ForgeConfigSpec.DoubleValue SKILL_PARTICLE_MULTIPLIER = COMMON_BUILDER
            .comment("Multiplier for Career Chronicle skill particles. 1.0 keeps default visuals, 0 disables most client-side particles.")
            .defineInRange("skillParticleMultiplier", 1.0D, 0.0D, 2.0D);

    public static final ForgeConfigSpec.IntValue MAX_TARGET_HIT_FX_PER_SKILL_CAST = COMMON_BUILDER
            .comment("Maximum entity hit FX packets emitted by one non-projectile skill cast. Lower this on busy servers.")
            .defineInRange("maxTargetHitFxPerSkillCast", 8, 0, 64);

    public static final ForgeConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    private ModConfig() {
    }
}
