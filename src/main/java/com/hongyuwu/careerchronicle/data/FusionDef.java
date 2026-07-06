package com.hongyuwu.careerchronicle.data;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record FusionDef(
        ResourceLocation id,
        Map<ResourceLocation, Integer> requiredClassCounts,
        Map<ResourceLocation, Integer> requiredTagScores,
        ResourceLocation unlockSkill
) {
    public FusionDef {
        requiredClassCounts = Map.copyOf(requiredClassCounts);
        requiredTagScores = requiredTagScores == null ? Map.of() : Map.copyOf(requiredTagScores);
    }
}
