package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.entity.LivingEntity;

public final class HealOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        float amount = params.get("amount").getAsFloat();
        float perLevel = params.has("per_level") ? params.get("per_level").getAsFloat() : 0;
        float actual = amount + perLevel * (ctx.skillLevel() - 1);
        String target = params.has("target") ? params.get("target").getAsString() : "self";

        if ("self".equals(target)) {
            healIfMissing(ctx.caster(), actual);
        } else if ("ally_area".equals(target)) {
            double radius = params.has("radius") ? params.get("radius").getAsDouble() : 3.5;
            for (LivingEntity entity : ctx.level().getEntitiesOfClass(
                    LivingEntity.class,
                    ctx.caster().getBoundingBox().inflate(radius),
                    e -> e.isAlive() && (e == ctx.caster() || e.isAlliedTo(ctx.caster()))
            )) {
                healIfMissing(entity, actual);
            }
        }
    }

    private static void healIfMissing(LivingEntity entity, float amount) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(amount);
        }
    }
}
