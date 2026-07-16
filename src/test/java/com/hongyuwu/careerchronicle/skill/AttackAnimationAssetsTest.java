package com.hongyuwu.careerchronicle.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hongyuwu.careerchronicle.client.AnimationClip;
import com.hongyuwu.careerchronicle.client.AnimationClipParser;
import com.hongyuwu.careerchronicle.client.Bone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段3-任务6-单元测试用例文档-普通攻击动作系统.md C1/C10/C12/C13 (asset-file-level checks that need
 * no Minecraft bootstrap -- {@code AnimationClipParser.parse(JsonObject)} is pure JSON parsing,
 * and reading {@code data/}/{@code assets/} JSON off the test classpath is plain file IO). C2-C9
 * (real-item classification via {@code ItemStack.is(TagKey)}) need a real item/tag registry and
 * live in GameTest instead.
 */
class AttackAnimationAssetsTest {

    private static final String[] ATTACK_CATEGORIES = {
            "attack_longsword", "attack_dagger", "attack_axe", "attack_blunt",
            "attack_staff", "attack_sigil", "attack_bow"
    };

    private static final Map<String, Integer> EXPECTED_DURATION_TICKS = Map.of(
            "attack_longsword", 12,
            "attack_dagger", 8,
            "attack_axe", 18,
            "attack_blunt", 16,
            "attack_staff", 14,
            "attack_sigil", 10,
            "attack_bow", 12);

    private static JsonObject readJson(String classpathResource) throws IOException {
        try (InputStream in = AttackAnimationAssetsTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "Missing classpath resource: " + classpathResource);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Set<String> tagValues(String category) throws IOException {
        JsonObject json = readJson("/data/careerchronicle/tags/items/" + category + ".json");
        Set<String> values = new HashSet<>();
        for (JsonElement e : json.getAsJsonArray("values")) {
            values.add(e.getAsString());
        }
        return values;
    }

    // C1: 7 tag files exist and are non-empty.
    @Test
    void c1_allSevenTagFilesExistAndNonEmpty() throws IOException {
        for (String category : ATTACK_CATEGORIES) {
            Set<String> values = tagValues(category);
            assertTrue(!values.isEmpty(), category + ".json has no values");
        }
    }

    // C10 (part 1): every item in the 4 pre-existing equipment tags is covered by exactly one of
    // the 7 attack_* tags (no omission).
    @Test
    void c10_everyEquipmentTagItemIsCoveredByExactlyOneAttackCategory() throws IOException {
        Set<String> equipmentItems = new HashSet<>();
        for (String equipmentTag : new String[] {"melee_weapon", "arcane_focus", "holy_focus", "ranged_focus"}) {
            equipmentItems.addAll(tagValues(equipmentTag));
        }

        Map<String, List<String>> itemToCategories = new HashMap<>();
        for (String category : ATTACK_CATEGORIES) {
            for (String item : tagValues(category)) {
                itemToCategories.computeIfAbsent(item, k -> new java.util.ArrayList<>()).add(category);
            }
        }

        for (String item : equipmentItems) {
            List<String> categories = itemToCategories.get(item);
            assertNotNull(categories, item + " is in an equipment tag but not in any attack_* category");
            assertEquals(1, categories.size(), item + " is in multiple attack_* categories: " + categories);
        }
    }

    // C10 (part 2): no attack_* category references an item outside the 4 known equipment tags
    // (every classified item must actually be a weapon the mod recognizes).
    @Test
    void c10_noAttackCategoryReferencesUnknownItem() throws IOException {
        Set<String> equipmentItems = new HashSet<>();
        for (String equipmentTag : new String[] {"melee_weapon", "arcane_focus", "holy_focus", "ranged_focus"}) {
            equipmentItems.addAll(tagValues(equipmentTag));
        }
        for (String category : ATTACK_CATEGORIES) {
            for (String item : tagValues(category)) {
                assertTrue(equipmentItems.contains(item),
                        category + " references '" + item + "' which is not in any of the 4 equipment tags");
            }
        }
    }

    // C12: each of the 7 attack_*.json custom_animation files parses successfully and its
    // duration_ticks matches the design doc.
    @Test
    void c12_allSevenAnimationsParseWithExpectedDuration() throws IOException {
        for (String category : ATTACK_CATEGORIES) {
            JsonObject json = readJson("/assets/careerchronicle/custom_animation/" + category + ".json");
            AnimationClip clip = AnimationClipParser.parse(json);
            assertNotNull(clip, category + " failed to parse");
            assertEquals(EXPECTED_DURATION_TICKS.get(category), clip.durationTicks(),
                    category + " duration_ticks mismatch");
        }
    }

    // C13: at every keyframe tick shared by both a leg track and its opposite, right_leg/right_shin
    // must differ from left_leg/left_shin somewhere in the clip (禁止镜像 red line, machine-checkable
    // slice of it).
    @Test
    void c13_noAttackAnimationMirrorsLeftAndRightLegs() throws IOException {
        for (String category : ATTACK_CATEGORIES) {
            JsonObject json = readJson("/assets/careerchronicle/custom_animation/" + category + ".json");
            AnimationClip clip = AnimationClipParser.parse(json);
            assertNotNull(clip, category + " failed to parse");

            boolean hasRightLeg = clip.tracks().containsKey(Bone.RIGHT_LEG);
            boolean hasLeftLeg = clip.tracks().containsKey(Bone.LEFT_LEG);
            if (!hasRightLeg && !hasLeftLeg) {
                // Upper-body-only category (none currently exist, but don't assume) -- nothing to check.
                continue;
            }
            assertTrue(hasRightLeg && hasLeftLeg, category + " has only one of right_leg/left_leg tracked");

            boolean foundDifference = false;
            for (int tick = 0; tick <= EXPECTED_DURATION_TICKS.get(category); tick++) {
                float right = clip.sample(Bone.RIGHT_LEG, tick).orElseThrow().pitch();
                float left = clip.sample(Bone.LEFT_LEG, tick).orElseThrow().pitch();
                if (Math.abs(right - left) > 0.01F) {
                    foundDifference = true;
                    break;
                }
            }
            assertTrue(foundDifference, category + ": right_leg and left_leg pitch never differ across the clip"
                    + " (looks mirrored)");
        }
    }

    // C13 (arms): same check for right_arm/left_arm on categories that track both.
    @Test
    void c13_noAttackAnimationMirrorsLeftAndRightArms() throws IOException {
        for (String category : ATTACK_CATEGORIES) {
            JsonObject json = readJson("/assets/careerchronicle/custom_animation/" + category + ".json");
            AnimationClip clip = AnimationClipParser.parse(json);
            assertNotNull(clip, category + " failed to parse");

            boolean hasRightArm = clip.tracks().containsKey(Bone.RIGHT_ARM);
            boolean hasLeftArm = clip.tracks().containsKey(Bone.LEFT_ARM);
            if (!hasRightArm || !hasLeftArm) {
                // Some categories (e.g. attack_bow, attack_blunt) legitimately drive only one arm's
                // "primary" role differently from the other -- both tracks existing is common but
                // not universally required; skip if either is untracked.
                continue;
            }

            boolean foundDifference = false;
            for (int tick = 0; tick <= EXPECTED_DURATION_TICKS.get(category); tick++) {
                float right = clip.sample(Bone.RIGHT_ARM, tick).orElseThrow().pitch();
                float left = clip.sample(Bone.LEFT_ARM, tick).orElseThrow().pitch();
                if (Math.abs(right - left) > 0.01F) {
                    foundDifference = true;
                    break;
                }
            }
            assertTrue(foundDifference, category + ": right_arm and left_arm pitch never differ across the clip"
                    + " (looks mirrored)");
        }
    }
}
