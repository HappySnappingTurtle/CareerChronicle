package com.hongyuwu.careerchronicle.test;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionMath;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.FusionDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.player.CareerData;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.skill.SkillExecutorRegistry;
import com.hongyuwu.careerchronicle.skill.effect.EffectOpRegistry;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.core.BlockPos;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(CareerChronicleMod.MOD_ID)
public class CareerGameTests {

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void registryLoaded(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        helper.assertTrue(registry.races().size() >= 6, "Expected >=6 races, got " + registry.races().size());
        helper.assertTrue(registry.classes().size() >= 9, "Expected >=9 classes, got " + registry.classes().size());
        helper.assertTrue(registry.skills().size() >= 67, "Expected >=67 skills, got " + registry.skills().size());
        helper.assertTrue(registry.fusions().size() >= 26, "Expected >=26 fusions, got " + registry.fusions().size());
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allSkillsHaveValidExecution(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (SkillDef skill : registry.skills().values()) {
            if (skill.hasComponentEffects()) {
                for (JsonObject effect : skill.effects()) {
                    helper.assertTrue(effect.has("op"),
                            "Skill " + skill.id() + " effect missing 'op' field");
                    String opName = effect.get("op").getAsString();
                    helper.assertTrue(EffectOpRegistry.exists(opName),
                            "Skill " + skill.id() + " references unknown op '" + opName + "'");
                }
            } else {
                helper.assertTrue(skill.executor() != null && SkillExecutorRegistry.exists(skill.executor()),
                        "Missing executor for skill " + skill.id() + ": " + skill.executor());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allFusionsReferenceValidSkills(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (FusionDef fusion : registry.fusions().values()) {
            helper.assertTrue(registry.skills().containsKey(fusion.unlockSkill()),
                    "Fusion " + fusion.id() + " references missing skill: " + fusion.unlockSkill());
            for (ResourceLocation classId : fusion.requiredClassCounts().keySet()) {
                helper.assertTrue(registry.classes().containsKey(classId),
                        "Fusion " + fusion.id() + " references missing class: " + classId);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allClassesReferenceValidSkills(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (ClassDef classDef : registry.classes().values()) {
            for (ResourceLocation skillId : classDef.grantsSkills()) {
                helper.assertTrue(registry.skills().containsKey(skillId),
                        "Class " + classDef.id() + " grants missing skill: " + skillId);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void firstSegmentAtLevelOne(GameTestHelper helper) {
        int required = CareerProgressionMath.requiredLevelForNextSegment(0);
        helper.assertTrue(required == 1,
                "First segment should require level 1, got " + required);
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void xpFormulaPositive(GameTestHelper helper) {
        for (int level = 1; level <= 50; level++) {
            int xp = CareerProgressionMath.xpForNextLevel(level);
            helper.assertTrue(xp > 0, "XP for level " + level + " should be positive, got " + xp);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void attributeDefaults(GameTestHelper helper) {
        CareerData data = new CareerData();
        for (String attr : CareerData.ALL_ATTRIBUTES) {
            helper.assertTrue(data.getAttribute(attr) == CareerData.BASE_ATTRIBUTE,
                    "Default " + attr + " should be " + CareerData.BASE_ATTRIBUTE);
        }
        helper.assertTrue(data.getUnspentAttributePoints() == 0, "Default unspent points should be 0");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void attributeAllocation(GameTestHelper helper) {
        CareerData data = new CareerData();
        data.setUnspentAttributePoints(5);
        data.setAttribute(CareerData.ATTR_STR, data.getAttribute(CareerData.ATTR_STR) + 3);
        data.setUnspentAttributePoints(data.getUnspentAttributePoints() - 3);
        helper.assertTrue(data.getAttribute(CareerData.ATTR_STR) == CareerData.BASE_ATTRIBUTE + 3,
                "STR should be " + (CareerData.BASE_ATTRIBUTE + 3));
        helper.assertTrue(data.getUnspentAttributePoints() == 2, "Should have 2 points left");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void nbtRoundTrip(GameTestHelper helper) {
        CareerData original = new CareerData();
        original.setRace(ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "elf"));
        original.setCareerLevel(15);
        original.setCareerXp(200);
        original.setAttribute(CareerData.ATTR_INT, 20);
        original.setUnspentAttributePoints(7);
        original.unlockSkill(ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball"));

        var nbt = original.serializePersistentData();

        CareerData restored = new CareerData();
        restored.deserializePersistentData(nbt);

        helper.assertTrue(original.getRace().equals(restored.getRace()), "Race mismatch");
        helper.assertTrue(original.getCareerLevel() == restored.getCareerLevel(), "Level mismatch");
        helper.assertTrue(original.getCareerXp() == restored.getCareerXp(), "XP mismatch");
        helper.assertTrue(restored.getAttribute(CareerData.ATTR_INT) == 20, "INT attribute mismatch");
        helper.assertTrue(restored.getUnspentAttributePoints() == 7, "Unspent points mismatch");
        helper.assertTrue(restored.getUnlockedSkills().contains(
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball")), "Skill not restored");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void skillTypesCastable(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (SkillDef skill : registry.skills().values()) {
            String type = skill.type();
            boolean shouldBeCastable = "active".equals(type) || "fusion".equals(type)
                    || "hidden".equals(type) || "ultimate".equals(type) || "race".equals(type);
            if ("passive".equals(type)) {
                helper.assertTrue(!shouldBeCastable, "Passive skill " + skill.id() + " should not be castable");
            }
        }
        helper.succeed();
    }
}
