package com.hongyuwu.careerchronicle.data;

import net.minecraft.resources.ResourceLocation;

public record RepeatRewardDef(
        int requiredCount,
        ResourceLocation unlockSkill
) {
    public RepeatRewardDef {
        requiredCount = Math.max(1, requiredCount);
    }
}
