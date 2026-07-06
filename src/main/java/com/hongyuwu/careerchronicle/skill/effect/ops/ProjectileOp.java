package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.NetworkHandler;
import com.hongyuwu.careerchronicle.skill.SkillCastHelper;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import com.hongyuwu.careerchronicle.world.entity.CareerProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ProjectileOp implements EffectOp {
    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        float speed = params.has("speed") ? params.get("speed").getAsFloat() : 1.5F;
        int count = params.has("count") ? params.get("count").getAsInt() : 1;
        float spread = params.has("spread") ? params.get("spread").getAsFloat() : 0;
        float damage = params.has("damage") ? params.get("damage").getAsFloat() : 6.0F;
        float perLevelDamage = params.has("per_level_damage") ? params.get("per_level_damage").getAsFloat() : 0;
        int burnSeconds = params.has("burn_seconds") ? params.get("burn_seconds").getAsInt() : 0;

        float actualDamage = damage + perLevelDamage * (ctx.skillLevel() - 1);

        for (int i = 0; i < count; i++) {
            float yawOffset = count > 1 ? spread * (i - (count - 1) / 2.0F) : 0;
            Vec3 origin = SkillCastHelper.castOrigin(ctx.caster(), 0.62, 0.72, -0.62);
            Vec3 direction = SkillCastHelper.aimDirection(ctx.caster(), origin, 28.0, yawOffset);

            CareerProjectileEntity projectile = new CareerProjectileEntity(
                    ctx.level(), ctx.caster(), ctx.skillId(), actualDamage, burnSeconds);
            projectile.setPos(origin.x, origin.y, origin.z);
            projectile.shoot(direction.x, direction.y, direction.z, speed, 0.08F);
            ctx.level().addFreshEntity(projectile);

            NetworkHandler.playSkillFx(ctx.caster(), ctx.skillId(), "cast",
                    origin, origin.add(direction.scale(2.4)));
        }
    }
}
