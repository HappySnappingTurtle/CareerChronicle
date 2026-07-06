package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.skill.CareerArrowTags;
import com.hongyuwu.careerchronicle.skill.SkillCastHelper;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

public final class ArrowOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        ServerLevel serverLevel = ctx.level();

        int count = params.has("count") ? params.get("count").getAsInt() : 1;
        float speed = params.has("speed") ? params.get("speed").getAsFloat() : 3.0F;
        double damageBonus = params.has("damage_bonus") ? params.get("damage_bonus").getAsDouble() : 2.0;
        double perLevelDamage = params.has("per_level_damage") ? params.get("per_level_damage").getAsDouble() : 0;
        float spread = params.has("spread") ? params.get("spread").getAsFloat() : 10.0F;
        boolean fire = params.has("fire") && params.get("fire").getAsBoolean();
        boolean snare = params.has("snare") && params.get("snare").getAsBoolean();
        boolean pierce = params.has("pierce") && params.get("pierce").getAsBoolean();
        boolean glow = params.has("glow") && params.get("glow").getAsBoolean();

        double actualDamage = damageBonus + perLevelDamage * (ctx.skillLevel() - 1);

        for (int i = 0; i < count; i++) {
            float yawOffset = count > 1 ? spread * ((float) i / (count - 1) - 0.5F) : 0;
            double sideOffset = count > 1 ? (i - (count - 1) / 2.0) * 0.5 : 0;

            Vec3 origin = SkillCastHelper.castOrigin(ctx.caster(), sideOffset, 0.58, -0.56);
            Vec3 direction = SkillCastHelper.aimDirection(ctx.caster(), origin, 42.0, yawOffset);

            Arrow arrow = new Arrow(ctx.level(), ctx.caster());
            arrow.setPos(origin.x, origin.y, origin.z);
            arrow.setBaseDamage(arrow.getBaseDamage() + actualDamage);
            if (fire) arrow.setSecondsOnFire(8);
            if (pierce) arrow.setPierceLevel((byte) 1);
            if (glow) arrow.setGlowingTag(true);
            CareerArrowTags.mark(arrow, ctx.skillId(), fire, snare);
            arrow.shoot(direction.x, direction.y, direction.z, speed, 0.16F);
            arrow.setCritArrow(true);
            ctx.level().addFreshEntity(arrow);

            Vec3 fxTarget = origin.add(arrow.getDeltaMovement().normalize().scale(2.7));
            NetworkHandler.playSkillFx(ctx.caster(), ctx.skillId(), "cast", origin, fxTarget);
        }
    }
}
