package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;

public final class SummonOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        CareerChronicleMod.LOGGER.warn("Summon op not yet implemented (skill {})", ctx.skillId());
    }
}
