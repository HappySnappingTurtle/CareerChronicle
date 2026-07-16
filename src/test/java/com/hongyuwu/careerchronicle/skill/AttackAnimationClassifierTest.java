package com.hongyuwu.careerchronicle.skill;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 阶段3-任务6-单元测试用例文档-普通攻击动作系统.md A组. {@link AttackAnimationClassifier#classify} is
 * exercised directly with a fake tag-membership predicate -- {@code ItemStack.is(TagKey)} needs a
 * real item/tag registry that plain JUnit can't construct (same constraint documented on
 * {@code AnimFxOpTest}), so {@link AttackAnimationClassifier#classifyItem} itself is left to
 * GameTest (C组).
 */
class AttackAnimationClassifierTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("careerchronicle", path);
    }

    private static java.util.function.Predicate<ResourceLocation> matching(String... paths) {
        Set<ResourceLocation> set = Set.of(java.util.Arrays.stream(paths)
                .map(AttackAnimationClassifierTest::id).toArray(ResourceLocation[]::new));
        return set::contains;
    }

    @Test
    void a1_onlyDagger_returnsDagger() {
        assertEquals(id("attack_dagger"), AttackAnimationClassifier.classify(matching("attack_dagger")));
    }

    @Test
    void a2_daggerAndLongsword_daggerWins() {
        assertEquals(id("attack_dagger"),
                AttackAnimationClassifier.classify(matching("attack_dagger", "attack_longsword")));
    }

    @Test
    void a3_allSevenMatch_daggerWins() {
        assertEquals(id("attack_dagger"), AttackAnimationClassifier.classify(matching(
                "attack_dagger", "attack_axe", "attack_blunt", "attack_longsword",
                "attack_staff", "attack_sigil", "attack_bow")));
    }

    @Test
    void a4_axeAndStaff_axeWins() {
        assertEquals(id("attack_axe"), AttackAnimationClassifier.classify(matching("attack_axe", "attack_staff")));
    }

    @Test
    void a5_noneMatch_returnsNull() {
        assertNull(AttackAnimationClassifier.classify(matching()));
    }

    @Test
    void a6_onlyBow_returnsBow() {
        assertEquals(id("attack_bow"), AttackAnimationClassifier.classify(matching("attack_bow")));
    }

    @Test
    void a7_fullPriorityOrder_adjacentPairsResolveToHigherPriority() {
        // 阶段3-任务6-设计文档 §二: 匕首 > 斧 > 锤盾 > 长剑 > 法杖 > 圣物 > 弓弩
        String[] order = {"attack_dagger", "attack_axe", "attack_blunt", "attack_longsword",
                "attack_staff", "attack_sigil", "attack_bow"};
        for (int i = 0; i < order.length - 1; i++) {
            ResourceLocation higher = id(order[i]);
            ResourceLocation lower = id(order[i + 1]);
            assertEquals(higher, AttackAnimationClassifier.classify(matching(order[i], order[i + 1])),
                    order[i] + " should outrank " + order[i + 1]);
        }
    }
}
