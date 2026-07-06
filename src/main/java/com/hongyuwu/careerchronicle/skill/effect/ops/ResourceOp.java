package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;

public final class ResourceOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        String type = params.get("type").getAsString();
        int amount = params.get("amount").getAsInt();
        int perLevel = params.has("per_level") ? params.get("per_level").getAsInt() : 0;

        int actual = amount + perLevel * (ctx.skillLevel() - 1);

        CareerDataAccess.get(ctx.caster()).ifPresent(data -> {
            if (actual > 0) {
                data.getRuntimeState().restoreResource(type, actual);
            } else if (actual < 0) {
                data.getRuntimeState().consumeResource(type, -actual);
            }
        });
    }
}
