package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;

public final class ShieldOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        float amount = params.get("amount").getAsFloat();
        float perLevel = params.has("per_level") ? params.get("per_level").getAsFloat() : 0;
        int duration = params.has("duration") ? params.get("duration").getAsInt() : 200;

        float actual = amount + perLevel * (ctx.skillLevel() - 1);
        int amplifier = Mth.clamp((int) (actual / 4.0F) - 1, 0, 4);

        ctx.caster().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, true, true));
    }
}
