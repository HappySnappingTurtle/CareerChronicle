package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 阶段3-任务6-设计文档-普通攻击动作系统.md §二: classifies a held item into one of 7 attack-animation
 * categories via the {@code attack_*} item tags. {@link #classify} is the pure, JUnit-testable
 * core (takes a tag-membership predicate, not a real {@link ItemStack} -- {@code ItemStack.is
 * (TagKey)} needs a real item/tag registry, which can't be constructed in plain JUnit, same
 * constraint already documented on {@code AnimFxOpTest}/{@code CustomSkeletonAnimationDriverTest}).
 * {@link #classifyItem} is the real entry point used by {@link BasicAttackAnimationEvents}.
 */
public final class AttackAnimationClassifier {

    /** 阶段3-任务6-设计文档 §二: "一个物品命中多个标签时按固定优先级判定
     * （匕首→斧→锤盾→长剑→法杖→圣物→弓弩）". Order here IS the priority -- first match wins. */
    private static final List<ResourceLocation> PRIORITY_ORDER = List.of(
            id("attack_dagger"),
            id("attack_axe"),
            id("attack_blunt"),
            id("attack_longsword"),
            id("attack_staff"),
            id("attack_sigil"),
            id("attack_bow"));

    private AttackAnimationClassifier() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, path);
    }

    /**
     * @param tagMatcher returns {@code true} if the held item is in the given attack-category tag.
     * @return the {@code careerchronicle:attack_*} animId of the highest-priority matching
     *         category, or {@code null} if the item matches none (unarmed/pickaxe/etc. -- caller
     *         must fall back to vanilla swing, no new behavior).
     */
    public static ResourceLocation classify(Predicate<ResourceLocation> tagMatcher) {
        for (ResourceLocation tagId : PRIORITY_ORDER) {
            if (tagMatcher.test(tagId)) {
                return tagId;
            }
        }
        return null;
    }

    /** Real entry point: resolves priority via {@link ItemStack#is(TagKey)} against the actual
     * item/tag registry. */
    public static ResourceLocation classifyItem(ItemStack stack) {
        return classify(tagId -> stack.is(TagKey.create(Registries.ITEM, tagId)));
    }
}
