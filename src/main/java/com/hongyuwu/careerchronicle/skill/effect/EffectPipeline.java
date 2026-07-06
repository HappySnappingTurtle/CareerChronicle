package com.hongyuwu.careerchronicle.skill.effect;

import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.CareerChronicleMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;

public final class EffectPipeline {
    private EffectPipeline() {
    }

    public static boolean execute(EffectContext ctx, List<JsonObject> effects) {
        boolean any = false;
        List<LivingEntity> currentTargets = new ArrayList<>(ctx.targets());
        EffectContext current = ctx;

        for (JsonObject effect : effects) {
            if (!effect.has("op") || !effect.get("op").isJsonPrimitive()) {
                CareerChronicleMod.LOGGER.warn("Effect missing 'op' field in skill {}", ctx.skillId());
                continue;
            }
            String opName = effect.get("op").getAsString();
            String on = effect.has("on") ? effect.get("on").getAsString() : "cast";
            if (!on.equals(current.trigger())) {
                continue;
            }
            EffectOp op = EffectOpRegistry.get(opName);
            if (op == null) {
                CareerChronicleMod.LOGGER.warn("Unknown effect op '{}' in skill {}", opName, ctx.skillId());
                continue;
            }
            try {
                if ("aoe".equals(opName)) {
                    op.apply(current, effect);
                    if (op instanceof AoeTargetProvider atp) {
                        currentTargets = atp.lastTargets();
                        current = current.withTargets(currentTargets, current.trigger());
                    }
                } else {
                    op.apply(current, effect);
                }
                any = true;
            } catch (Exception e) {
                CareerChronicleMod.LOGGER.error("Error executing op '{}' in skill {}", opName, ctx.skillId(), e);
            }
        }
        return any;
    }

    public interface AoeTargetProvider {
        List<LivingEntity> lastTargets();
    }
}
