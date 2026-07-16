package com.hongyuwu.careerchronicle.test;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionMath;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.ClassDef;
import com.hongyuwu.careerchronicle.data.FusionDef;
import com.hongyuwu.careerchronicle.data.HiddenUnlockDef;
import com.hongyuwu.careerchronicle.data.RaceDef;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.data.RepeatRewardDef;
import com.hongyuwu.careerchronicle.data.SkillDef;
import com.hongyuwu.careerchronicle.network.FxDispatcher;
import com.hongyuwu.careerchronicle.player.CareerData;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.skill.SkillExecutorRegistry;
import com.hongyuwu.careerchronicle.skill.SkillLevelService;
import com.hongyuwu.careerchronicle.skill.effect.EffectOpRegistry;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allRepeatRewardsValid(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        for (ClassDef classDef : registry.classes().values()) {
            for (RepeatRewardDef reward : classDef.repeatRewards()) {
                helper.assertTrue(registry.skills().containsKey(reward.unlockSkill()),
                        "Class " + classDef.id() + " repeat reward references missing skill: " + reward.unlockSkill());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allHiddenUnlocksValid(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        Set<ResourceLocation> allTags = new LinkedHashSet<>();
        for (ClassDef classDef : registry.classes().values()) {
            allTags.addAll(classDef.tags());
        }
        for (HiddenUnlockDef hidden : registry.hiddenUnlocks().values()) {
            for (ResourceLocation classId : hidden.requiredClassCounts().keySet()) {
                helper.assertTrue(registry.classes().containsKey(classId),
                        "Hidden unlock " + hidden.id() + " references missing class: " + classId);
            }
            for (ResourceLocation tag : hidden.requiredTagScores().keySet()) {
                helper.assertTrue(allTags.contains(tag),
                        "Hidden unlock " + hidden.id() + " references tag not found on any class: " + tag);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void attributeRequirementGating(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation guardianId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guardian");
        ResourceLocation iceMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ice_mage");
        ResourceLocation necromancerId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "necromancer");
        ResourceLocation rogueId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "rogue");

        ClassDef guardian = registry.classes().get(guardianId);
        CareerData data = new CareerData();
        helper.assertTrue(!guardian.meetsAttributeRequirements(data::getAttribute),
                "Guardian should fail with base attributes");
        data.setAttribute(CareerData.ATTR_CON, 12);
        data.setAttribute(CareerData.ATTR_STR, 8);
        helper.assertTrue(guardian.meetsAttributeRequirements(data::getAttribute),
                "Guardian should pass with con=12, str=8");

        ClassDef iceMage = registry.classes().get(iceMageId);
        data = new CareerData();
        helper.assertTrue(!iceMage.meetsAttributeRequirements(data::getAttribute),
                "Ice Mage should fail with base attributes");
        data.setAttribute(CareerData.ATTR_INT, 12);
        data.setAttribute(CareerData.ATTR_WIS, 8);
        helper.assertTrue(iceMage.meetsAttributeRequirements(data::getAttribute),
                "Ice Mage should pass with int=12, wis=8");

        ClassDef necromancer = registry.classes().get(necromancerId);
        data = new CareerData();
        helper.assertTrue(!necromancer.meetsAttributeRequirements(data::getAttribute),
                "Necromancer should fail with base attributes");
        data.setAttribute(CareerData.ATTR_INT, 12);
        data.setAttribute(CareerData.ATTR_CON, 8);
        helper.assertTrue(necromancer.meetsAttributeRequirements(data::getAttribute),
                "Necromancer should pass with int=12, con=8");

        ClassDef rogue = registry.classes().get(rogueId);
        data = new CareerData();
        helper.assertTrue(!rogue.meetsAttributeRequirements(data::getAttribute),
                "Rogue should fail with base attributes");
        data.setAttribute(CareerData.ATTR_DEX, 12);
        data.setAttribute(CareerData.ATTR_STR, 8);
        helper.assertTrue(rogue.meetsAttributeRequirements(data::getAttribute),
                "Rogue should pass with dex=12, str=8");

        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void raceClassRestrictions(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation iceMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ice_mage");
        ResourceLocation necromancerId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "necromancer");

        ResourceLocation[] universalClasses = {
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "warrior"),
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "archer"),
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage"),
                ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "guardian")
        };
        for (RaceDef race : registry.races().values()) {
            for (ResourceLocation classId : universalClasses) {
                helper.assertTrue(race.allowedClasses().contains(classId),
                        "Race " + race.id() + " should allow " + classId);
            }
        }

        String[] iceMageAllowed = {"dwarf", "elf", "human", "undead"};
        String[] iceMageDenied = {"demon", "orc"};
        for (String raceName : iceMageAllowed) {
            RaceDef race = registry.races().get(
                    ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, raceName));
            helper.assertTrue(race.allowedClasses().contains(iceMageId),
                    "Race " + raceName + " should allow ice_mage");
        }
        for (String raceName : iceMageDenied) {
            RaceDef race = registry.races().get(
                    ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, raceName));
            helper.assertTrue(!race.allowedClasses().contains(iceMageId),
                    "Race " + raceName + " should NOT allow ice_mage");
        }

        String[] necroAllowed = {"demon", "human", "orc", "undead"};
        String[] necroDenied = {"dwarf", "elf"};
        for (String raceName : necroAllowed) {
            RaceDef race = registry.races().get(
                    ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, raceName));
            helper.assertTrue(race.allowedClasses().contains(necromancerId),
                    "Race " + raceName + " should allow necromancer");
        }
        for (String raceName : necroDenied) {
            RaceDef race = registry.races().get(
                    ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, raceName));
            helper.assertTrue(!race.allowedClasses().contains(necromancerId),
                    "Race " + raceName + " should NOT allow necromancer");
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void fusionOrderIndependence(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation warriorId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "warrior");
        ResourceLocation fireMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage");

        CareerData dataA = new CareerData();
        dataA.addClassHistory(warriorId);
        dataA.addClassHistory(fireMageId);

        CareerData dataB = new CareerData();
        dataB.addClassHistory(fireMageId);
        dataB.addClassHistory(warriorId);

        Map<ResourceLocation, Integer> classCountsA = buildClassCounts(dataA, registry);
        Map<ResourceLocation, Integer> tagScoresA = buildTagScores(dataA, registry);
        Map<ResourceLocation, Integer> classCountsB = buildClassCounts(dataB, registry);
        Map<ResourceLocation, Integer> tagScoresB = buildTagScores(dataB, registry);

        helper.assertTrue(classCountsA.equals(classCountsB),
                "Class counts should be equal regardless of order");
        helper.assertTrue(tagScoresA.equals(tagScoresB),
                "Tag scores should be equal regardless of order");

        Set<ResourceLocation> fusionsA = new LinkedHashSet<>();
        Set<ResourceLocation> fusionsB = new LinkedHashSet<>();
        for (FusionDef fusion : registry.fusions().values()) {
            if (matchesScores(classCountsA, fusion.requiredClassCounts())
                    && matchesScores(tagScoresA, fusion.requiredTagScores())) {
                fusionsA.add(fusion.unlockSkill());
            }
            if (matchesScores(classCountsB, fusion.requiredClassCounts())
                    && matchesScores(tagScoresB, fusion.requiredTagScores())) {
                fusionsB.add(fusion.unlockSkill());
            }
        }
        helper.assertTrue(fusionsA.equals(fusionsB),
                "Fusions should match regardless of class order");

        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void repeatRewardAtCorrectCount(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation warriorId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "warrior");
        ResourceLocation ironVanguardId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "iron_vanguard");
        ResourceLocation unyieldingColossusId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "unyielding_colossus");
        ClassDef warrior = registry.classes().get(warriorId);

        CareerData data1 = new CareerData();
        data1.addClassHistory(warriorId);
        int count1 = countClass(data1, warriorId);
        for (RepeatRewardDef reward : warrior.repeatRewards()) {
            if (reward.unlockSkill().equals(ironVanguardId)) {
                helper.assertTrue(count1 < reward.requiredCount(),
                        "iron_vanguard should NOT be met with 1 warrior");
            }
        }

        CareerData data2 = new CareerData();
        data2.addClassHistory(warriorId);
        data2.addClassHistory(warriorId);
        int count2 = countClass(data2, warriorId);
        for (RepeatRewardDef reward : warrior.repeatRewards()) {
            if (reward.unlockSkill().equals(ironVanguardId)) {
                helper.assertTrue(count2 >= reward.requiredCount(),
                        "iron_vanguard SHOULD be met with 2 warriors");
            }
            if (reward.unlockSkill().equals(unyieldingColossusId)) {
                helper.assertTrue(count2 < reward.requiredCount(),
                        "unyielding_colossus should NOT be met with 2 warriors");
            }
        }

        CareerData data3 = new CareerData();
        data3.addClassHistory(warriorId);
        data3.addClassHistory(warriorId);
        data3.addClassHistory(warriorId);
        int count3 = countClass(data3, warriorId);
        for (RepeatRewardDef reward : warrior.repeatRewards()) {
            helper.assertTrue(count3 >= reward.requiredCount(),
                    "Both rewards should be met with 3 warriors, failed for " + reward.unlockSkill());
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void tagBasedSkillLevel(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation fireballId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fireball");
        ResourceLocation fireMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage");
        SkillDef fireball = registry.skills().get(fireballId);

        CareerData data0 = new CareerData();
        helper.assertTrue(SkillLevelService.levelOf(data0, fireball) == 1,
                "Fireball with no classes should be level 1");

        CareerData data1 = new CareerData();
        data1.addClassHistory(fireMageId);
        helper.assertTrue(SkillLevelService.levelOf(data1, fireball) == 1,
                "Fireball with 1 fire_mage should be level 1");

        CareerData data2 = new CareerData();
        data2.addClassHistory(fireMageId);
        data2.addClassHistory(fireMageId);
        helper.assertTrue(SkillLevelService.levelOf(data2, fireball) == 2,
                "Fireball with 2 fire_mages should be level 2");

        CareerData data3 = new CareerData();
        data3.addClassHistory(fireMageId);
        data3.addClassHistory(fireMageId);
        data3.addClassHistory(fireMageId);
        helper.assertTrue(SkillLevelService.levelOf(data3, fireball) == 3,
                "Fireball with 3 fire_mages should be level 3");

        helper.succeed();
    }

    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void hiddenUnlockConditions(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        ResourceLocation necromancerId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "necromancer");
        ResourceLocation iceMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ice_mage");
        ResourceLocation warriorId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "warrior");
        ResourceLocation fireMageId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "fire_mage");

        ResourceLocation lichClueId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "lich_clue");
        ResourceLocation deathKnightClueId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "death_knight_clue");
        ResourceLocation ashenWardenClueId = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "ashen_warden_clue");

        HiddenUnlockDef lichClue = registry.hiddenUnlocks().get(lichClueId);
        HiddenUnlockDef deathKnightClue = registry.hiddenUnlocks().get(deathKnightClueId);
        HiddenUnlockDef ashenWardenClue = registry.hiddenUnlocks().get(ashenWardenClueId);

        CareerData lichPass = new CareerData();
        lichPass.addClassHistory(necromancerId);
        lichPass.addClassHistory(necromancerId);
        lichPass.addClassHistory(iceMageId);
        helper.assertTrue(meetsHiddenCondition(lichPass, lichClue, registry),
                "lich_clue should trigger with [necromancer x2, ice_mage]");

        CareerData lichFail = new CareerData();
        lichFail.addClassHistory(necromancerId);
        lichFail.addClassHistory(iceMageId);
        helper.assertTrue(!meetsHiddenCondition(lichFail, lichClue, registry),
                "lich_clue should NOT trigger with [necromancer x1, ice_mage]");

        CareerData dkPass = new CareerData();
        dkPass.addClassHistory(warriorId);
        dkPass.addClassHistory(necromancerId);
        helper.assertTrue(meetsHiddenCondition(dkPass, deathKnightClue, registry),
                "death_knight_clue should trigger with [warrior, necromancer]");

        CareerData awPass = new CareerData();
        awPass.addClassHistory(fireMageId);
        awPass.addClassHistory(warriorId);
        helper.assertTrue(meetsHiddenCondition(awPass, ashenWardenClue, registry),
                "ashen_warden_clue should trigger with [fire_mage, warrior]");

        helper.succeed();
    }

    private static Map<ResourceLocation, Integer> buildClassCounts(CareerData data, RegistrySnapshot registry) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation classId : data.getClassHistory()) {
            counts.merge(classId, 1, Integer::sum);
        }
        return counts;
    }

    private static Map<ResourceLocation, Integer> buildTagScores(CareerData data, RegistrySnapshot registry) {
        Map<ResourceLocation, Integer> scores = new LinkedHashMap<>();
        for (ResourceLocation classId : data.getClassHistory()) {
            ClassDef classDef = registry.classes().get(classId);
            if (classDef != null) {
                for (ResourceLocation tag : classDef.tags()) {
                    scores.merge(tag, 1, Integer::sum);
                }
            }
        }
        return scores;
    }

    private static boolean matchesScores(Map<ResourceLocation, Integer> scores, Map<ResourceLocation, Integer> requirements) {
        for (Map.Entry<ResourceLocation, Integer> req : requirements.entrySet()) {
            if (scores.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static int countClass(CareerData data, ResourceLocation target) {
        int count = 0;
        for (ResourceLocation id : data.getClassHistory()) {
            if (id.equals(target)) {
                count++;
            }
        }
        return count;
    }

    private static boolean meetsHiddenCondition(CareerData data, HiddenUnlockDef hidden, RegistrySnapshot registry) {
        Map<ResourceLocation, Integer> classCounts = buildClassCounts(data, registry);
        Map<ResourceLocation, Integer> tagScores = buildTagScores(data, registry);
        return matchesScores(classCounts, hidden.requiredClassCounts())
                && matchesScores(tagScores, hidden.requiredTagScores());
    }

    // E1 (0.4-05a): data-level completeness check — every component-effects skill's
    // fx block must resolve to a non-empty cast op list. Runs headless, no entities.
    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allComponentSkillsHaveCastFx(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        int checked = 0;
        for (SkillDef skill : registry.skills().values()) {
            if (!skill.hasComponentEffects()) {
                continue;
            }
            helper.assertTrue(!FxDispatcher.toOps(skill.fx(), "cast").isEmpty(),
                    "Component skill " + skill.id() + " has no cast fx ops (fx block missing/empty?)");
            checked++;
        }
        helper.assertTrue(checked == 63, "Expected 63 component-effects skills, checked " + checked);
        helper.succeed();
    }

    // E2 (0.4-05a): the 10 executor-only skills exempted from 0.4-05a's fx fill are
    // exactly the skills with no component effects — guards against the exemption
    // scope silently drifting as new skills are added.
    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void executorSkillsExemptionScoped(GameTestHelper helper) {
        Set<ResourceLocation> expectedExecutorSkills = Set.of(
                rl("death_coil"), rl("death_strike"), rl("eagle_eye"), rl("flame_arrow"),
                rl("frost_arrow"), rl("lich_form"), rl("provoke"), rl("shadow_strike"),
                rl("smoke_bomb"), rl("soul_drain"));

        RegistrySnapshot registry = CareerRegistry.snapshot();
        Set<ResourceLocation> actualExecutorOnlySkills = new LinkedHashSet<>();
        for (SkillDef skill : registry.skills().values()) {
            if (!skill.hasComponentEffects()) {
                actualExecutorOnlySkills.add(skill.id());
            }
        }

        helper.assertTrue(expectedExecutorSkills.equals(actualExecutorOnlySkills),
                "Executor-only skill set drifted from the 0.4-05a exemption list. Expected "
                        + expectedExecutorSkills + " but registry has " + actualExecutorOnlySkills);
        helper.succeed();
    }

    // 0.4-06 E1: fx_templates load (Pass 1) and a real consuming skill
    // (frost_bolt, fx_template="frost_active") merges correctly through the
    // actual SimpleJsonResourceReloadListener pipeline (Pass 2) — not just the
    // in-memory CareerDataParsersFxTest JUnit simulation. Exact param values
    // are asserted so this also proves the template + append-on-top result is
    // byte-identical to what frost_bolt's fully-inline fx block was before it
    // was switched to use the template (zero behavior change, §五 risk note).
    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void fxTemplateAppliedToConsumingSkill(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();

        helper.assertTrue(registry.fxTemplates().containsKey("frost_active"),
                "fx_templates/frost_active.json should be loaded into RegistrySnapshot.fxTemplates");
        helper.assertTrue(registry.fxTemplates().get("frost_active").size() == 2,
                "frost_active template should have exactly 2 components (cast sound + cast particles)");

        SkillDef frostBolt = registry.skill(rl("frost_bolt")).orElse(null);
        helper.assertTrue(frostBolt != null, "frost_bolt skill should exist");

        var castOps = FxDispatcher.toOps(frostBolt.fx(), "cast");
        var hitOps = FxDispatcher.toOps(frostBolt.fx(), "hit");
        helper.assertTrue(castOps.size() == 3, "frost_bolt cast fx should be 3 ops (template's sound+particles, own shake), got " + castOps.size());
        helper.assertTrue(hitOps.size() == 2, "frost_bolt hit fx should be 2 ops (own sound+particles), got " + hitOps.size());

        helper.assertTrue("sound".equals(castOps.get(0).opId())
                        && "careerchronicle:skill.cast.frost".equals(castOps.get(0).params().getString("id")),
                "frost_bolt cast op 0 should be the template's cast sound");
        helper.assertTrue("particles".equals(castOps.get(1).opId())
                        && "minecraft:snowflake".equals(castOps.get(1).params().getString("id"))
                        && castOps.get(1).params().getInt("count") == 12,
                "frost_bolt cast op 1 should be the template's cast particles (count=12)");
        helper.assertTrue("shake".equals(castOps.get(2).opId())
                        && castOps.get(2).params().getFloat("strength") == 0.22F,
                "frost_bolt cast op 2 should be its own shake component (strength=0.22)");
        helper.assertTrue("sound".equals(hitOps.get(0).opId())
                        && "careerchronicle:skill.hit.frost".equals(hitOps.get(0).params().getString("id")),
                "frost_bolt hit op 0 should be its own hit sound");

        helper.succeed();
    }

    // 0.4-06 E2: the seven-field -> component-array migration (fill_skill_fx.py
    // rewrite, verified offline against a pre-migration snapshot for all 63
    // skills — see 0.4-06 completion evidence) is re-checked here through the
    // real runtime pipeline: every one of the 63 known component skills
    // resolves to AT LEAST 3 cast ops (sound+particles+shake) and AT LEAST 2
    // hit ops (sound-or-hit_layered+particles) -- ">= " rather than "==" since
    // 0.4-07 added extra hit-side ops (hitstop/camera_punch) to fireball as a
    // real demo, which is an addition on top of the legacy shape, not a loss
    // of it; the "equivalent to legacy" guarantee this test protects is
    // specifically "never fewer/different core ops than the seven-field
    // format used to produce", not "exactly these ops and no more forever".
    // frost_bolt (fx_template-based) is included and expected to match the
    // same shape via its own more detailed assertions above.
    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void allSkillsFxMigrationEquivalentToLegacyShape(GameTestHelper helper) {
        RegistrySnapshot registry = CareerRegistry.snapshot();
        int checked = 0;
        for (SkillDef skill : registry.skills().values()) {
            if (!skill.hasComponentEffects()) {
                continue;
            }
            var castOps = FxDispatcher.toOps(skill.fx(), "cast");
            var hitOps = FxDispatcher.toOps(skill.fx(), "hit");
            helper.assertTrue(castOps.size() >= 3,
                    "Skill " + skill.id() + " expected >=3 cast fx ops (sound+particles+shake), got " + castOps.size());
            helper.assertTrue(hitOps.size() >= 2,
                    "Skill " + skill.id() + " expected >=2 hit fx ops (sound+particles), got " + hitOps.size());
            checked++;
        }
        helper.assertTrue(checked == 63, "Expected 63 component-effects skills, checked " + checked);
        helper.succeed();
    }

    // REMOVED 2026-07-11: a 0.4-07 GameTest named castableSkillsDispatchAtLeastOneFxOp
    // used helper.makeMockServerPlayerInLevel() to call FxDispatcher.dispatchCast
    // directly and assert TestHooks.opCount("cast") >= 1 per skill. This is the
    // SAME makeMockServerPlayerInLevel() limitation already root-caused and
    // documented during 0.4-05a (see that task's completion audit + this
    // project's memory): placeNewPlayer() registers a player whose Connection
    // never binds a real Netty channel, and Minecraft/Forge's routine per-tick
    // broadcastAll(...) crashes reaching into it -- confirmed again here via a
    // real ./gradlew runGameTestServer run ("Cannot invoke Channel.pipeline()
    // because Connection.channel() is null"), despite this attempt's finally-block
    // mitigation (removing the mock player from PlayerList) -- the crash happens
    // on/immediately after placement, before the method body's own cleanup gets a
    // chance to run. Do NOT try this a third time in this environment.
    // The thing this test wanted to prove ("every castable skill's fx actually
    // resolves to >=1 real op, not an empty list") is already fully covered
    // without any mock player by allSkillsFxMigrationEquivalentToLegacyShape
    // above (calls FxDispatcher.toOps(...) directly -- the same function
    // dispatchCast delegates to -- and asserts >=3 cast / >=2 hit ops for all 63
    // skills). The remaining thing this test uniquely covered -- "the TestHooks
    // counters correctly reflect a real dispatchCast() call, not just a direct
    // toOps() call" -- is a dispatch-plumbing check, not a content-correctness
    // check, and is covered dynamically by the real autotest's fx_counter_smoke
    // step (AutoTestScenarios.java), which exercises a real connected player.
    // TestHooks.opCount(fxType) itself is kept (FxDispatcher.java) since
    // fx_counter_smoke can use it for a more precise dynamic assertion.

    // 阶段3-任务6-单元测试用例文档-普通攻击动作系统.md C2-C9: real ItemStack.is(TagKey) classification
    // via AttackAnimationClassifier.classifyItem -- needs the real item/tag registry a GameTest
    // server has (unlike plain JUnit, see AttackAnimationAssetsTest's class doc). No mock player
    // involved (unlike the removed castableSkillsDispatchAtLeastOneFxOp above), so this doesn't hit
    // the makeMockServerPlayerInLevel() network-layer limitation.
    @GameTest(template = "empty_3x3", timeoutTicks = 100)
    public static void attackAnimationClassifierResolvesRealItems(GameTestHelper helper) {
        Map<net.minecraft.world.item.Item, ResourceLocation> expected = new LinkedHashMap<>();
        // C2: longsword
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.RUNIC_BLADE.get(), rl("attack_longsword"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.IRON_RUNIC_BLADE.get(), rl("attack_longsword"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.DIAMOND_RUNIC_BLADE.get(), rl("attack_longsword"));
        expected.put(net.minecraft.world.item.Items.IRON_SWORD, rl("attack_longsword"));
        // C3: dagger
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.SHADOW_DAGGER.get(), rl("attack_dagger"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.VIPER_FANG.get(), rl("attack_dagger"));
        // C4: axe
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.BERSERKER_CLEAVER.get(), rl("attack_axe"));
        expected.put(net.minecraft.world.item.Items.DIAMOND_AXE, rl("attack_axe"));
        // C5: blunt
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.GUARDIAN_SHIELD.get(), rl("attack_blunt"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.DIAMOND_GUARDIAN_MACE.get(), rl("attack_blunt"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.TITANS_MAUL.get(), rl("attack_blunt"));
        // C6: staff
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.EMBER_STAFF.get(), rl("attack_staff"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.DARK_SCEPTER.get(), rl("attack_staff"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.STAFF_OF_AINZ.get(), rl("attack_staff"));
        // C7: sigil
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.SUNLIT_SIGIL.get(), rl("attack_sigil"));
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.DIVINE_CODEX.get(), rl("attack_sigil"));
        // C8: bow
        expected.put(com.hongyuwu.careerchronicle.registry.CareerItems.CHRONICLE_RECURVE.get(), rl("attack_bow"));
        expected.put(net.minecraft.world.item.Items.BOW, rl("attack_bow"));
        expected.put(net.minecraft.world.item.Items.CROSSBOW, rl("attack_bow"));

        for (Map.Entry<net.minecraft.world.item.Item, ResourceLocation> entry : expected.entrySet()) {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(entry.getKey());
            ResourceLocation actual = com.hongyuwu.careerchronicle.skill.AttackAnimationClassifier.classifyItem(stack);
            helper.assertTrue(entry.getValue().equals(actual),
                    entry.getKey() + " expected " + entry.getValue() + " got " + actual);
        }

        // C9: unmatched items (pickaxe, unarmed) classify to null -- vanilla swing, zero new behavior.
        net.minecraft.world.item.ItemStack pickaxe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        helper.assertTrue(com.hongyuwu.careerchronicle.skill.AttackAnimationClassifier.classifyItem(pickaxe) == null,
                "Pickaxe should not classify to any attack category");
        net.minecraft.world.item.ItemStack empty = net.minecraft.world.item.ItemStack.EMPTY;
        helper.assertTrue(com.hongyuwu.careerchronicle.skill.AttackAnimationClassifier.classifyItem(empty) == null,
                "Unarmed (empty stack) should not classify to any attack category");

        helper.succeed();
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, path);
    }
}
