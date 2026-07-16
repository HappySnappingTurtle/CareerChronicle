package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

final class CareerDataParsers {
    // Defaults baked into the auto-converted components for the legacy
    // seven-field fx format (0.4-06 syntax sugar) -- these are exactly the
    // constants FxDispatcher used to hardcode before the component-array
    // migration, preserved here so legacy-format skills keep producing
    // byte-for-byte identical FxOpSpec params (E2 regression requirement).
    private static final int CAST_PARTICLE_COUNT = 12;
    private static final float CAST_PARTICLE_SPREAD = 0.6F;
    private static final int HIT_PARTICLE_COUNT = 8;
    private static final float HIT_PARTICLE_SPREAD = 0.4F;

    private static final Set<String> LEGACY_FX_FIELDS = Set.of(
            "cast_sound", "cast_particle", "hit_sound", "hit_particle",
            "camera_shake", "camera_shake_ticks", "cast_circle");

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

    static SkillDef skill(ResourceLocation id, JsonObject json, Map<String, List<FxComponent>> fxTemplates) {
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
                resolveFx(id, json, fxTemplates)
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

    // ---- 0.4-06 fx component array (see 0.4-06-设计文档-ChronicleFX引擎Schema定案.md) ----

    /**
     * Resolves a skill's final fx component list: its own {@code fx} field
     * (new array format or legacy seven-field object, auto-expanded), merged
     * on top of an optional {@code fx_template} (B group). No template ->
     * the skill's own components are the whole result, unchanged (B4).
     */
    static List<FxComponent> resolveFx(ResourceLocation skillId, JsonObject json, Map<String, List<FxComponent>> fxTemplates) {
        List<FxComponent> own = parseFxComponents(json);
        if (!json.has("fx_template")) {
            return own;
        }
        String templateName = JsonDataUtil.string(json, "fx_template");
        List<FxComponent> template = fxTemplates.get(templateName);
        if (template == null) {
            throw new RegistryValidationException(
                    "Skill " + skillId + " references unknown fx_template '" + templateName + "'");
        }
        // 引擎审计修复 任务B / A6 (表现引擎全面审计报告_2026-07-15.md A6): only reachable once a
        // skill actually references a fx_template (this is the branch that builds the map below) --
        // without one, resolveFx already returned `own` unchanged above, so a same-op+when-no-key
        // pattern within a template-free skill's own fx array (e.g. ground_slam's two
        // particles@cast entries, distinguished only by their own delay_ticks) is never at risk.
        requireNoAmbiguousDuplicates(skillId, templateName, "its own fx array", own);
        requireNoAmbiguousDuplicates(skillId, templateName, "fx_template '" + templateName + "'", template);
        // op+when composite key (or the component's own explicit 'key', see FxComponent's doc)
        // -> map merge (not array concatenation), so the result is independent of declaration
        // order in either the template or the skill's own fx list (B2/B3/B6): template entries
        // seed the map in template order, then the skill's own components either replace a
        // matching key slot in place (B2, full replace of that component's params, not a
        // field-level merge) or get appended as new entries (B3). A shared key between a template
        // entry and an own entry is the deliberate "override the template's default" mechanic --
        // only *within* one list is a collision a red flag (see requireNoAmbiguousDuplicates above).
        Map<String, FxComponent> merged = new LinkedHashMap<>();
        for (FxComponent component : template) {
            merged.put(mergeKey(component), component);
        }
        for (FxComponent component : own) {
            merged.put(mergeKey(component), component);
        }
        return List.copyOf(merged.values());
    }

    private static String mergeKey(FxComponent component) {
        return component.key() != null ? component.key() : component.op() + "|" + component.when();
    }

    /**
     * 引擎审计修复 任务B / A6: within a single list (a skill's own fx array, or one fx_template's
     * component list -- never *across* the two, see {@link #resolveFx}'s own comment for why that
     * case is a deliberate override, not a bug), two or more components that fall back to the same
     * implicit {@code op|when} key (neither declares an explicit {@code key}) would silently
     * collapse to just the last one once merged with a template -- this is exactly the "静默吞组件"
     * failure the audit found live in ground_slam's data shape (two {@code particles}@{@code cast}
     * entries). Caught here at data-load time instead of discovered as a missing effect in-game.
     */
    private static void requireNoAmbiguousDuplicates(ResourceLocation skillId, String templateName,
            String label, List<FxComponent> components) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FxComponent component : components) {
            if (component.key() != null) {
                continue; // explicit key -- unambiguous by construction
            }
            counts.merge(component.op() + "|" + component.when(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                throw new RegistryValidationException(
                        "Skill " + skillId + ": " + label + " has " + entry.getValue()
                                + " fx components with op/when='" + entry.getKey() + "' and no distinguishing "
                                + "'key' field, while merging with fx_template '" + templateName + "' -- this "
                                + "would silently drop all but the last one. Add a unique 'key' string field to "
                                + "each of them.");
            }
        }
    }

    /** Parses a {@code fx_templates/*.json} file: top-level content IS the component array (no wrapping key). */
    static List<FxComponent> fxTemplate(ResourceLocation fileId, JsonElement rootElement) {
        if (!rootElement.isJsonArray()) {
            throw new RegistryValidationException("fx_templates file " + fileId + " must be a JSON array of fx components");
        }
        return parseComponentArray(rootElement.getAsJsonArray());
    }

    /**
     * Parses a skill's own {@code fx} field: either the new component array
     * (A1) or the legacy seven-field object, auto-expanded (A2). Missing
     * entirely -> empty list (A4). Both formats present in the same object
     * (legacy fields alongside a nested {@code components} array) -> throws,
     * ambiguity is not allowed (A3): the schema doesn't have a way for "fx"
     * to be simultaneously an array and legacy object at the JSON-type level
     * (a single JSON value can only be one type), so the object form is the
     * one place both representations could coexist and needs an explicit
     * guard.
     */
    static List<FxComponent> parseFxComponents(JsonObject json) {
        if (!json.has("fx")) {
            return List.of();
        }
        JsonElement fxElement = json.get("fx");
        if (fxElement.isJsonArray()) {
            return parseComponentArray(fxElement.getAsJsonArray());
        }
        if (!fxElement.isJsonObject()) {
            throw new RegistryValidationException("fx must be a JSON array (component list) or object (legacy fields)");
        }
        JsonObject fxObj = fxElement.getAsJsonObject();
        boolean hasComponentsArray = fxObj.has("components") && fxObj.get("components").isJsonArray();
        boolean hasLegacyField = LEGACY_FX_FIELDS.stream().anyMatch(fxObj::has);
        if (hasComponentsArray && hasLegacyField) {
            throw new RegistryValidationException(
                    "fx has both legacy fields and a 'components' array; specify exactly one format");
        }
        if (hasComponentsArray) {
            return parseComponentArray(fxObj.getAsJsonArray("components"));
        }
        return expandLegacyFx(parseLegacyFxSpec(fxObj));
    }

    private static List<FxComponent> parseComponentArray(JsonArray array) {
        List<FxComponent> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new RegistryValidationException("fx component must be a JSON object");
            }
            result.add(parseComponent(element.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    private static FxComponent parseComponent(JsonObject obj) {
        String op = JsonDataUtil.string(obj, "op");
        String when = JsonDataUtil.string(obj, "when");
        // 引擎审计修复 任务B / A6: optional disambiguator, not forwarded into params (it's an
        // FxComponent-level field, not something any FxOp implementation reads).
        String key = obj.has("key") ? JsonDataUtil.string(obj, "key") : null;
        CompoundTag params = new CompoundTag();
        for (String jsonKey : obj.keySet()) {
            if ("op".equals(jsonKey) || "when".equals(jsonKey) || "key".equals(jsonKey)) {
                continue;
            }
            putJsonValue(params, jsonKey, obj.get(jsonKey));
        }
        return new FxComponent(op, when, key, params);
    }

    /** Best-effort JsonElement -> CompoundTag entry; non-primitive extras are ignored (forward-compat, no current op needs them). */
    private static void putJsonValue(CompoundTag tag, String key, JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            tag.putBoolean(key, primitive.getAsBoolean());
        } else if (primitive.isNumber()) {
            String raw = primitive.getAsString();
            if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
                tag.putFloat(key, primitive.getAsFloat());
            } else {
                tag.putInt(key, primitive.getAsInt());
            }
        } else {
            tag.putString(key, primitive.getAsString());
        }
    }

    /** The legacy seven-field object -> FxSpec (unchanged parsing, "the syntax sugar entry point" FxSpec.java's Javadoc refers to). */
    private static FxSpec parseLegacyFxSpec(JsonObject fxObj) {
        return new FxSpec(
                JsonDataUtil.optionalString(fxObj, "cast_sound", null),
                JsonDataUtil.optionalString(fxObj, "cast_particle", null),
                JsonDataUtil.optionalString(fxObj, "hit_sound", null),
                JsonDataUtil.optionalString(fxObj, "hit_particle", null),
                (float) JsonDataUtil.optionalDouble(fxObj, "camera_shake", 0),
                JsonDataUtil.optionalInt(fxObj, "camera_shake_ticks", 0),
                JsonDataUtil.optionalBoolean(fxObj, "cast_circle", false)
        );
    }

    /**
     * FxSpec -> component array, the literal expansion of the seven legacy
     * fields into their equivalent {@code op}/{@code when} components (0.4-06
     * §2.1 mapping table). Unlike the pre-migration FxDispatcher.toOps, which
     * recognized castCircle but deliberately emitted nothing for it, this now
     * emits a real {@code circle} component -- 0.4-06 adds the circle op
     * (particles-approximation placeholder) instead of leaving it a pure
     * no-op, per §2.3/§3. All 63 shipped skills currently have
     * {@code cast_circle: false}, so this change has zero effect on their
     * migrated output (E2 regression scope).
     */
    static List<FxComponent> expandLegacyFx(FxSpec fx) {
        if (fx == null || fx == FxSpec.EMPTY) {
            return List.of();
        }
        List<FxComponent> components = new ArrayList<>();
        if (fx.castSound() != null) {
            components.add(soundComponent(fx.castSound(), "cast"));
        }
        if (fx.castParticle() != null) {
            components.add(particlesComponent(fx.castParticle(), "cast", CAST_PARTICLE_COUNT, CAST_PARTICLE_SPREAD));
        }
        if (fx.cameraShakeStrength() > 0) {
            components.add(shakeComponent(fx.cameraShakeStrength(), fx.cameraShakeTicks()));
        }
        if (fx.hitSound() != null) {
            components.add(soundComponent(fx.hitSound(), "hit"));
        }
        if (fx.hitParticle() != null) {
            components.add(particlesComponent(fx.hitParticle(), "hit", HIT_PARTICLE_COUNT, HIT_PARTICLE_SPREAD));
        }
        if (fx.castCircle()) {
            // No particle id carried by the legacy boolean flag; reuse the
            // cast particle for visual consistency with the rest of the
            // skill's cast fx, falling back to a neutral default otherwise.
            components.add(circleComponent(fx.castParticle() != null ? fx.castParticle() : "minecraft:end_rod"));
        }
        return List.copyOf(components);
    }

    private static FxComponent soundComponent(String soundId, String when) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", soundId);
        return new FxComponent("sound", when, tag);
    }

    private static FxComponent particlesComponent(String particleId, String when, int count, float spread) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", particleId);
        tag.putInt("count", count);
        tag.putFloat("spread", spread);
        return new FxComponent("particles", when, tag);
    }

    private static FxComponent shakeComponent(float strength, int ticks) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("strength", strength);
        tag.putInt("ticks", ticks);
        return new FxComponent("shake", "cast", tag);
    }

    private static FxComponent circleComponent(String particleId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", particleId);
        return new FxComponent("circle", "cast", tag);
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
