package com.hongyuwu.careerchronicle.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record RaceDef(
        ResourceLocation id,
        String displayKey,
        List<ResourceLocation> allowedClasses,
        List<ResourceLocation> traits,
        List<String> biomeTags
) {
    public RaceDef {
        allowedClasses = List.copyOf(allowedClasses);
        traits = List.copyOf(traits);
        biomeTags = biomeTags == null ? List.of() : List.copyOf(biomeTags);
    }

    public boolean matchesBiome(String biomeId) {
        if (biomeTags.isEmpty()) return true;
        for (String tag : biomeTags) {
            if (biomeId.contains(tag)) return true;
        }
        return false;
    }
}
