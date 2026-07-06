package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class DamageOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        float amount = params.get("amount").getAsFloat();
        float perLevel = params.has("per_level") ? params.get("per_level").getAsFloat() : 0;
        float actual = amount + perLevel * (ctx.skillLevel() - 1);
        String type = params.has("damage_type") ? params.get("damage_type").getAsString() : "player_attack";

        DamageSource source = switch (type) {
            case "magic" -> ctx.caster().damageSources().magic();
            case "fire" -> ctx.caster().damageSources().inFire();
            default -> ctx.caster().damageSources().playerAttack(ctx.caster());
        };

        for (LivingEntity target : ctx.targets()) {
            if (target.isAlive() && target != ctx.caster()) {
                target.hurt(source, actual);
            }
        }
    }
}
