package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-06 A/B groups: {@code CareerDataParsers.parseFxComponents}/{@code resolveFx}
 * (see 0.4-06-单元测试用例文档-ChronicleFX引擎Schema定案.md).
 */
class CareerDataParsersFxTest {

    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("careerchronicle", "test_skill");

    // ---- A group ----

    // A1: pure component array format.
    @Test
    void parseFxComponents_componentArray_returnsVerbatim() {
        JsonObject json = obj("""
                {"fx": [{"op":"sound","when":"cast","id":"x"}]}
                """);

        List<FxComponent> components = CareerDataParsers.parseFxComponents(json);

        assertEquals(1, components.size());
        assertEquals("sound", components.get(0).op());
        assertEquals("cast", components.get(0).when());
        assertEquals("x", components.get(0).params().getString("id"));
    }

    // A2: legacy seven-field format auto-converts to the equivalent component array.
    @Test
    void parseFxComponents_legacySevenFields_expandsToEquivalentComponents() {
        JsonObject json = obj("""
                {"fx": {
                    "cast_sound": "careerchronicle:skill.cast.fire",
                    "cast_particle": "minecraft:flame",
                    "hit_sound": "careerchronicle:skill.hit.fire",
                    "hit_particle": "minecraft:flame",
                    "camera_shake": 0.3,
                    "camera_shake_ticks": 6,
                    "cast_circle": false
                }}
                """);

        List<FxComponent> components = CareerDataParsers.parseFxComponents(json);

        assertEquals(5, components.size());
        assertEquals("sound", components.get(0).op());
        assertEquals("cast", components.get(0).when());
        assertEquals("careerchronicle:skill.cast.fire", components.get(0).params().getString("id"));
        assertEquals("particles", components.get(1).op());
        assertEquals("cast", components.get(1).when());
        assertEquals("minecraft:flame", components.get(1).params().getString("id"));
        assertEquals(12, components.get(1).params().getInt("count"));
        assertEquals(0.6F, components.get(1).params().getFloat("spread"));
        assertEquals("shake", components.get(2).op());
        assertEquals("cast", components.get(2).when());
        assertEquals(0.3F, components.get(2).params().getFloat("strength"));
        assertEquals(6, components.get(2).params().getInt("ticks"));
        assertEquals("sound", components.get(3).op());
        assertEquals("hit", components.get(3).when());
        assertEquals("particles", components.get(4).op());
        assertEquals("hit", components.get(4).when());
        assertEquals(8, components.get(4).params().getInt("count"));
        assertEquals(0.4F, components.get(4).params().getFloat("spread"));
    }

    // A3: both formats present (legacy fields + nested components array) -> explicit exception.
    @Test
    void parseFxComponents_bothFormatsPresent_throws() {
        JsonObject json = obj("""
                {"fx": {
                    "cast_sound": "careerchronicle:skill.cast.fire",
                    "components": [{"op":"shake","when":"cast","strength":0.3}]
                }}
                """);

        RegistryValidationException exception = assertThrows(RegistryValidationException.class,
                () -> CareerDataParsers.parseFxComponents(json));
        assertTrue(exception.getMessage().contains("both"));
    }

    // A4: fx field entirely absent -> empty list, no throw.
    @Test
    void parseFxComponents_noFxField_returnsEmptyList() {
        JsonObject json = obj("{}");

        List<FxComponent> components = CareerDataParsers.parseFxComponents(json);

        assertNotNull(components);
        assertTrue(components.isEmpty());
    }

    // A5: component missing 'when' -> validation exception.
    @Test
    void parseFxComponents_componentMissingWhen_throws() {
        JsonObject json = obj("""
                {"fx": [{"op":"sound","id":"x"}]}
                """);

        assertThrows(RegistryValidationException.class, () -> CareerDataParsers.parseFxComponents(json));
    }

    // A5b: component missing 'op' -> also a validation exception.
    @Test
    void parseFxComponents_componentMissingOp_throws() {
        JsonObject json = obj("""
                {"fx": [{"when":"cast","id":"x"}]}
                """);

        assertThrows(RegistryValidationException.class, () -> CareerDataParsers.parseFxComponents(json));
    }

    // A6: unregistered op string -> parse-time is lenient (runtime FxOpRegistry lookup warns instead).
    @Test
    void parseFxComponents_unknownOp_doesNotThrowAtParseTime() {
        JsonObject json = obj("""
                {"fx": [{"op":"unknown_op","when":"cast"}]}
                """);

        List<FxComponent> components = assertDoesNotThrow(() -> CareerDataParsers.parseFxComponents(json));
        assertEquals(1, components.size());
        assertEquals("unknown_op", components.get(0).op());
    }

    // ---- B group ----

    private static List<FxComponent> frostActiveTemplate() {
        return CareerDataParsers.fxTemplate(
                ResourceLocation.fromNamespaceAndPath("careerchronicle", "fx_templates/frost_active"),
                JsonParser.parseString("""
                        [
                            {"op":"sound","when":"cast","id":"careerchronicle:skill.cast.frost"},
                            {"op":"particles","when":"cast","id":"minecraft:snowflake","count":10,"spread":0.5}
                        ]
                        """));
    }

    // B1: skill declares fx_template, no own fx -> result equals the template verbatim.
    @Test
    void resolveFx_templateOnly_resultEqualsTemplate() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        List<FxComponent> template = frostActiveTemplate();
        templates.put("frost_active", template);
        JsonObject json = obj("""
                {"fx_template": "frost_active"}
                """);

        List<FxComponent> resolved = CareerDataParsers.resolveFx(SKILL_ID, json, templates);

        assertEquals(template, resolved);
    }

    // B2: skill declares fx_template + overrides one op+when -> that slot takes the skill's own params, rest from template.
    @Test
    void resolveFx_templateWithOverride_overriddenSlotUsesSkillParams() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("frost_active", frostActiveTemplate());
        JsonObject json = obj("""
                {"fx_template": "frost_active", "fx": [{"op":"sound","when":"cast","id":"careerchronicle:skill.cast.overridden"}]}
                """);

        List<FxComponent> resolved = CareerDataParsers.resolveFx(SKILL_ID, json, templates);

        assertEquals(2, resolved.size());
        FxComponent sound = resolved.stream().filter(c -> c.op().equals("sound")).findFirst().orElseThrow();
        assertEquals("careerchronicle:skill.cast.overridden", sound.params().getString("id"));
        FxComponent particles = resolved.stream().filter(c -> c.op().equals("particles")).findFirst().orElseThrow();
        assertEquals("minecraft:snowflake", particles.params().getString("id"));
    }

    // B3: skill declares fx_template + an additional, different op+when -> template entries kept, new one appended.
    @Test
    void resolveFx_templateWithExtraComponent_appendsNewEntry() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("frost_active", frostActiveTemplate());
        JsonObject json = obj("""
                {"fx_template": "frost_active", "fx": [{"op":"shake","when":"cast","strength":0.2,"ticks":4}]}
                """);

        List<FxComponent> resolved = CareerDataParsers.resolveFx(SKILL_ID, json, templates);

        assertEquals(3, resolved.size());
        assertTrue(resolved.stream().anyMatch(c -> c.op().equals("sound")));
        assertTrue(resolved.stream().anyMatch(c -> c.op().equals("particles")));
        assertTrue(resolved.stream().anyMatch(c -> c.op().equals("shake")));
    }

    // B4: no fx_template, only own fx -> result is the own fx verbatim, template map untouched.
    @Test
    void resolveFx_noTemplate_returnsOwnFxUnchanged() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("frost_active", frostActiveTemplate());
        JsonObject json = obj("""
                {"fx": [{"op":"sound","when":"cast","id":"careerchronicle:skill.cast.solo"}]}
                """);

        List<FxComponent> resolved = CareerDataParsers.resolveFx(SKILL_ID, json, templates);

        assertEquals(1, resolved.size());
        assertEquals("careerchronicle:skill.cast.solo", resolved.get(0).params().getString("id"));
    }

    // B5: fx_template references a template that doesn't exist -> explicit exception naming skill + template.
    @Test
    void resolveFx_missingTemplate_throwsWithSkillAndTemplateName() {
        Map<String, List<FxComponent>> templates = Map.of();
        JsonObject json = obj("""
                {"fx_template": "does_not_exist"}
                """);

        RegistryValidationException exception = assertThrows(RegistryValidationException.class,
                () -> CareerDataParsers.resolveFx(SKILL_ID, json, templates));
        assertTrue(exception.getMessage().contains(SKILL_ID.toString()));
        assertTrue(exception.getMessage().contains("does_not_exist"));
    }

    // B6: same templates, built via different insertion order -> merge result for the same skill is identical
    // (op+when keyed map merge does not depend on iteration/declaration order).
    @Test
    void resolveFx_templateMapInsertionOrderIndependent() {
        List<FxComponent> template = frostActiveTemplate();

        Map<String, List<FxComponent>> orderA = new LinkedHashMap<>();
        orderA.put("frost_active", template);
        orderA.put("other_template", List.of(new FxComponent("shake", "cast", new CompoundTag())));

        Map<String, List<FxComponent>> orderB = new LinkedHashMap<>();
        orderB.put("other_template", orderA.get("other_template"));
        orderB.put("frost_active", template);

        JsonObject json = obj("""
                {"fx_template": "frost_active"}
                """);

        List<FxComponent> resolvedA = CareerDataParsers.resolveFx(SKILL_ID, json, orderA);
        List<FxComponent> resolvedB = CareerDataParsers.resolveFx(SKILL_ID, json, orderB);

        assertEquals(resolvedA, resolvedB);
    }

    // ---- D group: 引擎审计修复 任务B / A6 (表现引擎全面审计报告_2026-07-15.md A6) ----

    // D1: this is the exact real-world data shape the bug depended on -- ground_slam's own fx
    // array has 2 particles@cast entries (distinguished only by delay_ticks) and no explicit
    // 'key'. Without a fx_template this is harmless (B4's early-return path), but the moment a
    // fx_template is added, the merge would silently drop one -- must be caught at load time.
    @Test
    void resolveFx_ownFxHasUnkeyedDuplicate_andReferencesTemplate_throws() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("frost_active", frostActiveTemplate());
        JsonObject json = obj("""
                {"fx_template": "frost_active", "fx": [
                    {"op":"particles","when":"cast","id":"minecraft:cloud","delay_ticks":4},
                    {"op":"particles","when":"cast","id":"minecraft:cloud","delay_ticks":7}
                ]}
                """);

        RegistryValidationException exception = assertThrows(RegistryValidationException.class,
                () -> CareerDataParsers.resolveFx(SKILL_ID, json, templates));
        assertTrue(exception.getMessage().contains("particles|cast"));
        assertTrue(exception.getMessage().contains("key"));
    }

    // D2: the fix in action -- the same shape as D1, but each duplicate declares a unique 'key',
    // so the ambiguity check passes and both entries survive the merge untouched.
    @Test
    void resolveFx_ownFxDuplicateWithExplicitKeys_bothSurviveMerge() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("frost_active", frostActiveTemplate());
        JsonObject json = obj("""
                {"fx_template": "frost_active", "fx": [
                    {"op":"particles","when":"cast","key":"windup_dust","id":"minecraft:cloud","delay_ticks":4},
                    {"op":"particles","when":"cast","key":"impact_dust","id":"minecraft:cloud","delay_ticks":7}
                ]}
                """);

        List<FxComponent> resolved = CareerDataParsers.resolveFx(SKILL_ID, json, templates);

        // frostActiveTemplate() itself contributes one particles@cast entry (implicit key
        // "particles|cast"), which does not collide with either explicit key below -- so all
        // three particles components survive the merge side by side (template's default plus
        // both of the skill's own keyed overrides), none silently dropped.
        List<String> particleKeys = resolved.stream()
                .filter(c -> c.op().equals("particles"))
                .map(FxComponent::key)
                .toList();
        assertEquals(3, particleKeys.size(), "template's own particles entry plus both keyed entries must survive the merge");
        assertTrue(particleKeys.contains("windup_dust"), "windup_dust-keyed entry must survive the merge");
        assertTrue(particleKeys.contains("impact_dust"), "impact_dust-keyed entry must survive the merge");
    }

    // D3: the same ambiguity, but living in the *template* itself rather than the skill's own fx.
    @Test
    void resolveFx_templateHasUnkeyedDuplicate_throws() {
        Map<String, List<FxComponent>> templates = new LinkedHashMap<>();
        templates.put("bad_template", List.of(
                new FxComponent("shake", "cast", new CompoundTag()),
                new FxComponent("shake", "cast", new CompoundTag())));
        JsonObject json = obj("""
                {"fx_template": "bad_template"}
                """);

        RegistryValidationException exception = assertThrows(RegistryValidationException.class,
                () -> CareerDataParsers.resolveFx(SKILL_ID, json, templates));
        assertTrue(exception.getMessage().contains("shake|cast"));
    }

    // D4: the exact bypass this bug relies on today -- a same-shape duplicate with NO fx_template
    // reference must not throw (resolveFx's early return never reaches the ambiguity check).
    @Test
    void resolveFx_ownFxHasUnkeyedDuplicate_noTemplate_doesNotThrow() {
        Map<String, List<FxComponent>> templates = Map.of();
        JsonObject json = obj("""
                {"fx": [
                    {"op":"particles","when":"cast","id":"minecraft:cloud","delay_ticks":4},
                    {"op":"particles","when":"cast","id":"minecraft:cloud","delay_ticks":7}
                ]}
                """);

        List<FxComponent> resolved = assertDoesNotThrow(() -> CareerDataParsers.resolveFx(SKILL_ID, json, templates));
        assertEquals(2, resolved.size(), "both entries must survive verbatim -- no template means no merge map at all");
    }

    // D5: 'key' is a component-level field, not forwarded into params (no FxOp reads a "key" NBT entry).
    @Test
    void parseFxComponents_keyField_notLeakedIntoParams() {
        JsonObject json = obj("""
                {"fx": [{"op":"particles","when":"cast","key":"windup_dust","id":"minecraft:cloud"}]}
                """);

        List<FxComponent> components = CareerDataParsers.parseFxComponents(json);

        assertEquals(1, components.size());
        assertEquals("windup_dust", components.get(0).key());
        assertFalse(components.get(0).params().contains("key"), "'key' must not leak into the params CompoundTag");
        assertEquals("minecraft:cloud", components.get(0).params().getString("id"));
    }

    // D6: no 'key' field present -> FxComponent.key() is null (not empty string), matching the
    // implicit-key fallback path in mergeKey.
    @Test
    void parseFxComponents_noKeyField_keyIsNull() {
        JsonObject json = obj("""
                {"fx": [{"op":"sound","when":"cast","id":"x"}]}
                """);

        List<FxComponent> components = CareerDataParsers.parseFxComponents(json);

        assertNull(components.get(0).key());
    }
}
