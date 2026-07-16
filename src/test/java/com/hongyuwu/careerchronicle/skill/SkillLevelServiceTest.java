package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.data.UpgradeRule;
import com.hongyuwu.careerchronicle.player.CareerData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillLevelServiceTest {

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("careerchronicle", path);
    }

    // --- null / NONE upgrade rule ---

    @Test
    void levelOf_nullRule_returns1() {
        CareerData data = new CareerData();
        assertEquals(1, SkillLevelService.levelOf(data, (UpgradeRule) null));
    }

    @Test
    void levelOf_noneRule_returns1() {
        CareerData data = new CareerData();
        assertEquals(1, SkillLevelService.levelOf(data, UpgradeRule.NONE));
    }

    // --- class:X source resolution ---

    @Test
    void levelOf_classSource_countsMatchingEntries() {
        CareerData data = new CareerData();
        data.addClassHistory(rl("warrior"));
        data.addClassHistory(rl("warrior"));
        data.addClassHistory(rl("fire_mage"));

        UpgradeRule rule = new UpgradeRule("class:warrior", 10);
        assertEquals(2, SkillLevelService.levelOf(data, rule));
    }

    // --- class:X with empty history ---

    @Test
    void levelOf_classSource_emptyHistory_clampedToMin1() {
        CareerData data = new CareerData();

        UpgradeRule rule = new UpgradeRule("class:warrior", 10);
        assertEquals(1, SkillLevelService.levelOf(data, rule),
                "Empty class history should resolve score=0, clamped to level 1");
    }

    // --- max level capping ---

    @Test
    void levelOf_classSource_cappedAtMaxLevel() {
        CareerData data = new CareerData();
        for (int i = 0; i < 5; i++) {
            data.addClassHistory(rl("warrior"));
        }

        UpgradeRule rule = new UpgradeRule("class:warrior", 3);
        assertEquals(3, SkillLevelService.levelOf(data, rule),
                "Level should be capped at maxLevel=3 even though score=5");
    }

    // --- unknown source ---

    @Test
    void levelOf_unknownSource_returns1() {
        CareerData data = new CareerData();

        UpgradeRule rule = new UpgradeRule("unknown:foo", 10);
        assertEquals(1, SkillLevelService.levelOf(data, rule),
                "Unknown source prefix should resolve score=1, yielding level 1");
    }

    // --- source "none" string ---

    @Test
    void levelOf_noneSourceString_returns1() {
        CareerData data = new CareerData();
        data.addClassHistory(rl("warrior"));
        data.addClassHistory(rl("warrior"));

        UpgradeRule rule = new UpgradeRule("none", 5);
        assertEquals(1, SkillLevelService.levelOf(data, rule),
                "Source 'none' should short-circuit to level 1 regardless of history");
    }
}
