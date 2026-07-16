package com.hongyuwu.careerchronicle.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hongyuwu.careerchronicle.skill.effect.EffectOpRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.4-07 D group: CareerRegistryValidator's non-throwing warnings channel. */
class CareerRegistryValidatorWarningsTest {

    @BeforeEach
    void setUp() {
        EffectOpRegistry.clearForTesting();
        EffectOpRegistry.registerBuiltins();
    }

    @AfterEach
    void tearDown() {
        EffectOpRegistry.clearForTesting();
    }

    private static SkillDef castableSkill(String path, List<FxComponent> fx) {
        JsonObject effect = JsonParser.parseString("""
                {"op":"damage","amount":5.0}
                """).getAsJsonObject();
        return new SkillDef(
                ResourceLocation.fromNamespaceAndPath("careerchronicle", path),
                "skill." + path, "active", "none", 0, null, 0,
                SkillDef.Requirements.EMPTY, List.of(effect), UpgradeRule.NONE, fx);
    }

    private static SkillDef executorSkill(String path) {
        // Must be a *real* registered executor id -- CareerRegistryValidator's hard
        // require() checks SkillExecutorRegistry.exists() before the warnings channel
        // is even reached, so an invented id would throw here instead of testing D2.
        return new SkillDef(
                ResourceLocation.fromNamespaceAndPath("careerchronicle", path),
                "skill." + path, "active", "none", 0,
                com.hongyuwu.careerchronicle.skill.SkillExecutorRegistry.EAGLE_EYE, 0,
                SkillDef.Requirements.EMPTY, List.of(), UpgradeRule.NONE, List.of());
    }

    private static List<String> validate(Map<ResourceLocation, SkillDef> skills) {
        List<String> warnings = new ArrayList<>();
        CareerRegistryValidator.validate(Map.of(), Map.of(), skills, Map.of(), Map.of(), Map.of(), warnings);
        return warnings;
    }

    // D1: a castable (component-effects) skill with no fx produces exactly one warning, no throw.
    @Test
    void castableSkillMissingFx_producesWarning_doesNotThrow() {
        SkillDef skill = castableSkill("no_fx_skill", List.of());
        Map<ResourceLocation, SkillDef> skills = Map.of(skill.id(), skill);

        List<String> warnings = assertDoesNotThrow(() -> validate(skills));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("no_fx_skill"));
        assertTrue(warnings.get(0).contains("no fx"));
    }

    // D2: the 10 known executor-only skills are exempt even though they have no fx.
    @Test
    void executorOnlySkills_exemptFromWarning() {
        String[] exempt = {
                "death_coil", "death_strike", "eagle_eye", "flame_arrow", "frost_arrow",
                "lich_form", "provoke", "shadow_strike", "smoke_bomb", "soul_drain"
        };
        Map<ResourceLocation, SkillDef> skills = new LinkedHashMap<>();
        for (String path : exempt) {
            SkillDef skill = executorSkill(path);
            skills.put(skill.id(), skill);
        }

        List<String> warnings = validate(skills);

        assertTrue(warnings.isEmpty(), "Expected no warnings for exempt executor skills, got: " + warnings);
    }

    // D3: a castable skill that DOES declare fx produces no warning.
    @Test
    void castableSkillWithFx_noWarning() {
        List<FxComponent> fx = List.of(new FxComponent("sound", "cast", new net.minecraft.nbt.CompoundTag()));
        SkillDef skill = castableSkill("has_fx_skill", fx);
        Map<ResourceLocation, SkillDef> skills = Map.of(skill.id(), skill);

        List<String> warnings = validate(skills);

        assertTrue(warnings.isEmpty());
    }

    // D4: warnings are independent of the hard require() checks -- a mix of a warning-triggering
    // skill and an otherwise-valid skill both pass through validate() without an exception, and
    // only the fx-missing one produces a warning.
    @Test
    void warningsIndependentOfHardValidation() {
        SkillDef missingFx = castableSkill("missing_fx", List.of());
        SkillDef withFx = castableSkill("with_fx",
                List.of(new FxComponent("sound", "cast", new net.minecraft.nbt.CompoundTag())));
        Map<ResourceLocation, SkillDef> skills = new LinkedHashMap<>();
        skills.put(missingFx.id(), missingFx);
        skills.put(withFx.id(), withFx);

        List<String> warnings = assertDoesNotThrow(() -> validate(skills));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("missing_fx"));
    }
}
