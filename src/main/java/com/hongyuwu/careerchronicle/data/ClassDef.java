package com.hongyuwu.careerchronicle.data;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ClassDef(
        ResourceLocation id,
        String displayKey,
        int segmentLevels,
        List<ResourceLocation> grantsSkills,
        List<ResourceLocation> tags,
        List<RepeatRewardDef> repeatRewards,
        boolean hidden,
        ResourceLocation unlockFlag,
        Map<String, Integer> requiredAttributes
) {
    public ClassDef {
        segmentLevels = Math.max(1, segmentLevels);
        grantsSkills = List.copyOf(grantsSkills);
        tags = List.copyOf(tags);
        repeatRewards = List.copyOf(repeatRewards);
        requiredAttributes = requiredAttributes == null ? Map.of() : Map.copyOf(requiredAttributes);
    }

    public boolean meetsAttributeRequirements(java.util.function.Function<String, Integer> attrGetter) {
        for (Map.Entry<String, Integer> req : requiredAttributes.entrySet()) {
            if (attrGetter.apply(req.getKey()) < req.getValue()) return false;
        }
        return true;
    }
}
