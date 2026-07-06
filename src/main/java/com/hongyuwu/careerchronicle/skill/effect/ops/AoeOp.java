package com.hongyuwu.careerchronicle.skill.effect.ops;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.config.ModConfig;
import com.hongyuwu.careerchronicle.skill.effect.EffectContext;
import com.hongyuwu.careerchronicle.skill.effect.EffectOp;
import com.hongyuwu.careerchronicle.skill.effect.EffectPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AoeOp implements EffectOp, EffectPipeline.AoeTargetProvider {
    private List<LivingEntity> lastTargets = List.of();

    @Override
    public void apply(EffectContext ctx, JsonObject params) {
        double radius = params.has("radius") ? params.get("radius").getAsDouble() : 3.0;
        double perLevelRadius = params.has("per_level_radius") ? params.get("per_level_radius").getAsDouble() : 0;
        double height = params.has("height") ? params.get("height").getAsDouble() : 1.0;
        String filter = params.has("filter") ? params.get("filter").getAsString() : "hostile";

        double actualRadius = radius + perLevelRadius * (ctx.skillLevel() - 1);

        lastTargets = new ArrayList<>(ctx.level().getEntitiesOfClass(
                LivingEntity.class,
                ctx.caster().getBoundingBox().inflate(actualRadius, height, actualRadius),
                target -> filterTarget(ctx, target, filter)
        ));
    }

    @Override
    public List<LivingEntity> lastTargets() {
        return lastTargets;
    }

    private static boolean filterTarget(EffectContext ctx, LivingEntity target, String filter) {
        if (target == ctx.caster() || !target.isAlive()) {
            return false;
        }
        return switch (filter) {
            case "ally" -> target.isAlliedTo(ctx.caster());
            case "all" -> true;
            default -> {
                if (target instanceof Player targetPlayer) {
                    yield ModConfig.ENABLE_SKILL_PVP.get() && ctx.caster().canHarmPlayer(targetPlayer);
                }
                yield true;
            }
        };
    }
}
