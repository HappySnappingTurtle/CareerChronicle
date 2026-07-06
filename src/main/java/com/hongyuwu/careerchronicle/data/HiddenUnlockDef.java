package com.hongyuwu.careerchronicle.data;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record HiddenUnlockDef(
        ResourceLocation id,
        String displayKey,
        String clueKey,
        String revealedKey,
        Map<ResourceLocation, Integer> requiredClassCounts,
        Map<ResourceLocation, Integer> requiredTagScores,
        ResourceLocation unlockFlag
) {
    public HiddenUnlockDef {
        requiredClassCounts = Map.copyOf(requiredClassCounts);
        requiredTagScores = Map.copyOf(requiredTagScores);
    }
}
