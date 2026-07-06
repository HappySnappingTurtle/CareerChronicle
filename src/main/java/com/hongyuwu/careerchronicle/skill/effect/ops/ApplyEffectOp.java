package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class ApplyEffectOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        String effectId = params.get("effect").getAsString();
        int duration = params.get("duration").getAsInt();
        int amplifier = params.has("amplifier") ? params.get("amplifier").getAsInt() : 0;
        int perLevelDuration = params.has("per_level_duration") ? params.get("per_level_duration").getAsInt() : 0;
        String target = params.has("target") ? params.get("target").getAsString() : "enemy";

        int actualDuration = duration + perLevelDuration * (ctx.skillLevel() - 1);
        MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(effectId));
        if (mobEffect == null) {
            return;
        }

        if ("self".equals(target)) {
            ctx.caster().addEffect(new MobEffectInstance(mobEffect, actualDuration, amplifier, false, true, true));
        } else {
            for (LivingEntity entity : ctx.targets()) {
                if (entity.isAlive()) {
                    entity.addEffect(new MobEffectInstance(mobEffect, actualDuration, amplifier, false, true, true));
                }
            }
        }
    }
}
