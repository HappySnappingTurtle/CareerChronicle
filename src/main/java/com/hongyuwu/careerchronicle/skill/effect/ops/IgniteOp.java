package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.entity.LivingEntity;

public final class IgniteOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        int seconds = params.get("seconds").getAsInt();
        int perLevel = params.has("per_level") ? params.get("per_level").getAsInt() : 0;
        int actual = seconds + perLevel * (ctx.skillLevel() - 1);

        for (LivingEntity target : ctx.targets()) {
            if (target.isAlive() && target != ctx.caster()) {
                target.setSecondsOnFire(actual);
            }
        }
    }
}
