package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.phys.Vec3;

public final class DashOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        double distance = params.get("distance").getAsDouble();
        String direction = params.has("direction") ? params.get("direction").getAsString() : "look";
        double vertical = params.has("vertical") ? params.get("vertical").getAsDouble() : 0.18;

        Vec3 look = ctx.caster().getLookAngle().normalize();
        double dx = "backward".equals(direction) ? -look.x * distance : look.x * distance;
        double dz = "backward".equals(direction) ? -look.z * distance : look.z * distance;

        ctx.caster().push(dx, vertical, dz);
        ctx.caster().hurtMarked = true;
    }
}
