package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.data.UpgradeRule;
import com.hongyuwu.careerchronicle.player.CareerData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillLevelOrderIndependenceTest {

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("careerchronicle", path);
    }

    private static final ResourceLocation WARRIOR = rl("warrior");
    private static final ResourceLocation FIRE_MAGE = rl("fire_mage");
    private static final ResourceLocation ARCHER = rl("archer");

    private static CareerData dataWith(ResourceLocation... history) {
        CareerData data = new CareerData();
        for (ResourceLocation id : history) {
            data.addClassHistory(id);
        }
        return data;
    }

    // --- B2: class counts are order-independent ---

    @Test
    void classCount_twoOrderings_sameResult() {
        CareerData dataA = dataWith(WARRIOR, FIRE_MAGE, WARRIOR);
        CareerData dataB = dataWith(FIRE_MAGE, WARRIOR, WARRIOR);

        UpgradeRule rule = new UpgradeRule("class:warrior", 10);

        int levelA = SkillLevelService.levelOf(dataA, rule);
        int levelB = SkillLevelService.levelOf(dataB, rule);

        assertEquals(2, levelA, "Data A: warrior count should be 2");
        assertEquals(2, levelB, "Data B: warrior count should be 2");
        assertEquals(levelA, levelB, "Both orderings must produce the same level");
    }

    @Test
    void classCount_multiplePermutations_allEqual() {
        List<List<ResourceLocation>> permutations = Arrays.asList(
                Arrays.asList(WARRIOR, WARRIOR, FIRE_MAGE, ARCHER),
                Arrays.asList(FIRE_MAGE, WARRIOR, ARCHER, WARRIOR),
                Arrays.asList(ARCHER, FIRE_MAGE, WARRIOR, WARRIOR),
                Arrays.asList(WARRIOR, ARCHER, WARRIOR, FIRE_MAGE),
                Arrays.asList(WARRIOR, FIRE_MAGE, ARCHER, WARRIOR),
                Arrays.asList(ARCHER, WARRIOR, FIRE_MAGE, WARRIOR)
        );

        UpgradeRule warriorRule = new UpgradeRule("class:warrior", 5);
        UpgradeRule mageRule = new UpgradeRule("class:fire_mage", 5);
        UpgradeRule archerRule = new UpgradeRule("class:archer", 5);

        for (int i = 0; i < permutations.size(); i++) {
            CareerData data = new CareerData();
            for (ResourceLocation id : permutations.get(i)) {
                data.addClassHistory(id);
            }

            String label = "Permutation " + i + " " + permutations.get(i);
            assertEquals(2, SkillLevelService.levelOf(data, warriorRule),
                    label + ": warrior count should be 2");
            assertEquals(1, SkillLevelService.levelOf(data, mageRule),
                    label + ": fire_mage count should be 1");
            assertEquals(1, SkillLevelService.levelOf(data, archerRule),
                    label + ": archer count should be 1");
        }
    }
}
