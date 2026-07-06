package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class KnockbackOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        double strength = params.get("strength").getAsDouble();
        String direction = params.has("direction") ? params.get("direction").getAsString() : "away";

        for (LivingEntity target : ctx.targets()) {
            if (!target.isAlive() || target == ctx.caster()) continue;
            if ("look".equals(direction)) {
                Vec3 look = ctx.caster().getLookAngle().normalize();
                target.knockback(strength, -look.x, -look.z);
            } else {
                double dx = ctx.caster().getX() - target.getX();
                double dz = ctx.caster().getZ() - target.getZ();
                target.knockback(strength, dx, dz);
            }
        }
    }
}
