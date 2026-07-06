package com.hongyuwu.careerchronicle.skill.effect;

import com.google.gson.JsonObject;

public interface EffectOp {
    void apply(EffectContext ctx, JsonObject params);
}
