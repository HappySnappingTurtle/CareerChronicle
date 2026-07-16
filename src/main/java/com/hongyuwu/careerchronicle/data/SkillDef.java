package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record SkillDef(
        ResourceLocation id,
        String displayKey,
        String type,
        String resource,
        int resourceCost,
        ResourceLocation executor,
        int cooldownTicks,
        Requirements requirements,
        List<JsonObject> effects,
        UpgradeRule upgrade,
        List<FxComponent> fx
) {
    public SkillDef {
        type = type == null || type.isBlank() ? "active" : type.trim().toLowerCase(Locale.ROOT);
        resource = resource == null || resource.isBlank() ? "none" : resource.trim().toLowerCase(Locale.ROOT);
        resourceCost = Math.max(0, resourceCost);
        cooldownTicks = Math.max(0, cooldownTicks);
        requirements = requirements == null ? Requirements.EMPTY : requirements;
        effects = effects == null ? List.of() : List.copyOf(effects);
        upgrade = upgrade == null ? UpgradeRule.NONE : upgrade;
        fx = fx == null ? List.of() : List.copyOf(fx);
    }

    public boolean hasComponentEffects() {
        return !effects.isEmpty();
    }

    public record Requirements(Set<ResourceLocation> equipmentTags) {
        public static final Requirements EMPTY = new Requirements(Set.of());

        public Requirements {
            equipmentTags = Set.copyOf(equipmentTags);
        }

        public boolean hasEquipmentTags() {
            return !equipmentTags.isEmpty();
        }
    }
}
