package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

final class CareerDataParsers {
    private CareerDataParsers() {
    }

    static RaceDef race(ResourceLocation id, JsonObject json) {
        return new RaceDef(
                id,
                JsonDataUtil.optionalString(json, "display_key", "race." + id.getNamespace() + "." + id.getPath()),
                JsonDataUtil.idList(json, "allowed_classes"),
                JsonDataUtil.idList(json, "traits"),
                JsonDataUtil.stringList(json, "biome_tags")
        );
    }

    static ClassDef careerClass(ResourceLocation id, JsonObject json) {
        return new ClassDef(
                id,
                JsonDataUtil.optionalString(json, "display_key", "class." + id.getNamespace() + "." + id.getPath()),
                JsonDataUtil.optionalInt(json, "segment_levels", 10),
                JsonDataUtil.idList(json, "grants_skills"),
                JsonDataUtil.idList(json, "tags"),
                repeatRewards(json),
                JsonDataUtil.optionalBoolean(json, "hidden", false),
                json.has("unlock_flag") ? JsonDataUtil.id(JsonDataUtil.string(json, "unlock_flag")) : null,
                parseRequiredAttributes(json)
        );
    }

    private static java.util.Map<String, Integer> parseRequiredAttributes(JsonObject json) {
        if (!json.has("required_attributes")) return java.util.Map.of();
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        JsonObject attrs = json.getAsJsonObject("required_attributes");
        for (String key : attrs.keySet()) {
            result.put(key, attrs.get(key).getAsInt());
        }
        return result;
    }

    private static List<RepeatRewardDef> repeatRewards(JsonObject json) {
        List<RepeatRewardDef> rewards = new ArrayList<>();
        for (JsonObject reward : JsonDataUtil.objectList(json, "repeat_rewards")) {
            rewards.add(new RepeatRewardDef(
                    JsonDataUtil.optionalInt(reward, "required_count", 2),
                    JsonDataUtil.id(JsonDataUtil.string(reward, "unlock_skill"))
            ));
        }
        return rewards;
    }

    static SkillDef skill(ResourceLocation id, JsonObject json) {
        List<com.google.gson.JsonObject> effects = JsonDataUtil.objectList(json, "effects");
        ResourceLocation executor = null;
        if (effects.isEmpty()) {
            executor = JsonDataUtil.id(JsonDataUtil.string(json, "executor"));
        } else if (json.has("executor")) {
            executor = JsonDataUtil.id(JsonDataUtil.optionalString(json, "executor", id.toString()));
        }
        return new SkillDef(
                id,
                JsonDataUtil.optionalString(json, "display_key", "skill." + id.getNamespace() + "." + id.getPath()),
                JsonDataUtil.optionalString(json, "type", "active"),
                JsonDataUtil.optionalString(json, "resource", "none"),
                JsonDataUtil.optionalInt(json, "resource_cost", 0),
                executor,
                JsonDataUtil.optionalInt(json, "cooldown_ticks", 0),
                skillRequirements(json),
                effects,
                parseUpgradeRule(json),
                parseFxSpec(json)
        );
    }

    private static UpgradeRule parseUpgradeRule(com.google.gson.JsonObject json) {
        if (!json.has("upgrade")) return UpgradeRule.NONE;
        com.google.gson.JsonObject obj = json.getAsJsonObject("upgrade");
        return new UpgradeRule(
                JsonDataUtil.optionalString(obj, "source", "none"),
                JsonDataUtil.optionalInt(obj, "max_level", 1)
        );
    }

    private static FxSpec parseFxSpec(com.google.gson.JsonObject json) {
        if (!json.has("fx")) return FxSpec.EMPTY;
        com.google.gson.JsonObject obj = json.getAsJsonObject("fx");
        return new FxSpec(
                JsonDataUtil.optionalString(obj, "cast_sound", null),
                JsonDataUtil.optionalString(obj, "cast_particle", null),
                JsonDataUtil.optionalString(obj, "hit_sound", null),
                JsonDataUtil.optionalString(obj, "hit_particle", null),
                (float) JsonDataUtil.optionalDouble(obj, "camera_shake", 0),
                JsonDataUtil.optionalInt(obj, "camera_shake_ticks", 0),
                JsonDataUtil.optionalBoolean(obj, "cast_circle", false)
        );
    }

    private static SkillDef.Requirements skillRequirements(JsonObject json) {
        if (!json.has("requirements")) {
            return SkillDef.Requirements.EMPTY;
        }
        if (!json.get("requirements").isJsonObject()) {
            throw new RegistryValidationException("Expected object field: requirements");
        }
        JsonObject requirements = json.getAsJsonObject("requirements");
        return new SkillDef.Requirements(
                JsonDataUtil.idList(requirements, "equipment_tags").stream().collect(java.util.stream.Collectors.toSet())
        );
    }

    static FusionDef fusion(ResourceLocation id, JsonObject json) {
        return new FusionDef(
                id,
                JsonDataUtil.idIntMap(json, "required_class_counts"),
                json.has("required_tag_scores") ? JsonDataUtil.idIntMap(json, "required_tag_scores") : java.util.Map.of(),
                JsonDataUtil.id(JsonDataUtil.string(json, "unlock_skill"))
        );
    }

    static HiddenUnlockDef hiddenUnlock(ResourceLocation id, JsonObject json) {
        return new HiddenUnlockDef(
                id,
                JsonDataUtil.optionalString(json, "display_key", "careerchronicle.hidden." + id.getPath()),
                JsonDataUtil.optionalString(json, "clue_key", "careerchronicle.hidden." + id.getPath() + ".clue"),
                JsonDataUtil.optionalString(json, "revealed_key", "careerchronicle.hidden." + id.getPath() + ".revealed"),
                JsonDataUtil.idIntMap(json, "required_class_counts"),
                JsonDataUtil.idIntMap(json, "required_tag_scores"),
                JsonDataUtil.id(JsonDataUtil.optionalString(json, "unlock_flag", id.toString()))
        );
    }

    static XpSourceDef xpSource(ResourceLocation id, JsonObject json) {
        return new XpSourceDef(
                id,
                JsonDataUtil.optionalString(json, "display_key", "careerchronicle.xp_source." + id.getPath()),
                JsonDataUtil.optionalInt(json, "base_amount", 0),
                JsonDataUtil.optionalDouble(json, "health_multiplier", 0.0D),
                JsonDataUtil.optionalInt(json, "min_amount", 0),
                JsonDataUtil.optionalInt(json, "max_amount", 0)
        );
    }
}
