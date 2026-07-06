package com.hongyuwu.careerchronicle.skill.effect;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public record EffectContext(
        ServerPlayer caster,
        List<LivingEntity> targets,
        int skillLevel,
        String trigger,
        ResourceLocation skillId,
        ServerLevel level
) {
    public static EffectContext castContext(ServerPlayer caster, int skillLevel, ResourceLocation skillId) {
        return new EffectContext(
                caster, List.of(), skillLevel, "cast",
                skillId, (ServerLevel) caster.level()
        );
    }

    public EffectContext withTargets(List<LivingEntity> targets, String trigger) {
        return new EffectContext(caster, targets, skillLevel, trigger, skillId, level);
    }
}
