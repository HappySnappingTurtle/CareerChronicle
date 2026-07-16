package com.hongyuwu.careerchronicle.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段3-任务1-单元测试用例文档-JSON关键帧Schema.md A组. */
class AnimationClipParserTest {

    private static JsonObject frame(int tick, String field, float value, String easing) {
        JsonObject o = new JsonObject();
        o.addProperty("tick", tick);
        if (field != null) {
            o.addProperty(field, value);
        }
        if (easing != null) {
            o.addProperty("easing", easing);
        }
        return o;
    }

    private static JsonObject baseClip(String id, int duration) {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("duration_ticks", duration);
        root.add("tracks", new JsonObject());
        return root;
    }

    @Test
    void a1_fullyValidClip_parsesAllFields() {
        JsonObject root = baseClip("careerchronicle:cast_onehand_quick", 10);
        JsonArray rightArm = new JsonArray();
        rightArm.add(frame(0, "pitch", 0F, "ease_in_out_sine"));
        rightArm.add(frame(3, "pitch", -20F, "ease_in_out_sine"));
        rightArm.add(frame(6, "pitch", -70F, "ease_out_expo"));
        rightArm.add(frame(10, "pitch", 0F, "ease_in_out_sine"));
        root.getAsJsonObject("tracks").add("right_arm", rightArm);

        AnimationClip clip = AnimationClipParser.parse(root);

        assertNotNull(clip);
        assertEquals("careerchronicle:cast_onehand_quick", clip.id());
        assertEquals(10, clip.durationTicks());
        assertFalse(clip.loop());
        assertEquals(4, clip.tracks().get(Bone.RIGHT_ARM).size());
    }

    @Test
    void a2_missingBoneTrack_sampleReturnsEmpty() {
        JsonObject root = baseClip("test:a2", 10);
        JsonArray rightArm = new JsonArray();
        rightArm.add(frame(0, "pitch", 0F, null));
        rightArm.add(frame(10, "pitch", 10F, null));
        root.getAsJsonObject("tracks").add("right_arm", rightArm);

        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);
        assertTrue(clip.sample(Bone.LEFT_ARM, 5F).isEmpty());
    }

    @Test
    void a3_missingComponentDefaultsToZero() {
        JsonObject root = baseClip("test:a3", 10);
        JsonArray track = new JsonArray();
        JsonObject f0 = new JsonObject();
        f0.addProperty("tick", 0);
        f0.addProperty("pitch", 5F);
        // roll intentionally omitted
        track.add(f0);
        JsonObject f1 = new JsonObject();
        f1.addProperty("tick", 10);
        f1.addProperty("pitch", 5F);
        track.add(f1);
        root.getAsJsonObject("tracks").add("body", track);

        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);
        BonePose pose = clip.sample(Bone.BODY, 0F).orElseThrow();
        assertEquals(0F, pose.roll());
    }

    @Test
    void a4_unknownBoneName_rejected() {
        JsonObject root = baseClip("test:a4", 10);
        JsonArray track = new JsonArray();
        track.add(frame(0, "pitch", 0F, null));
        root.getAsJsonObject("tracks").add("right_hand", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a5_unknownEasing_rejected() {
        JsonObject root = baseClip("test:a5", 10);
        JsonArray track = new JsonArray();
        track.add(frame(0, "pitch", 0F, "bounce"));
        root.getAsJsonObject("tracks").add("body", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a6_outOfOrderTicks_rejected() {
        JsonObject root = baseClip("test:a6", 10);
        JsonArray track = new JsonArray();
        track.add(frame(0, "pitch", 0F, null));
        track.add(frame(6, "pitch", 1F, null));
        track.add(frame(3, "pitch", 2F, null));
        root.getAsJsonObject("tracks").add("body", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a7_duplicateTicks_rejected() {
        JsonObject root = baseClip("test:a7", 10);
        JsonArray track = new JsonArray();
        track.add(frame(0, "pitch", 0F, null));
        track.add(frame(3, "pitch", 1F, null));
        track.add(frame(3, "pitch", 2F, null));
        root.getAsJsonObject("tracks").add("body", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a8_nonPositiveDuration_rejected() {
        JsonObject root = baseClip("test:a8", 0);
        assertNull(AnimationClipParser.parse(root));

        JsonObject negative = baseClip("test:a8b", -5);
        assertNull(AnimationClipParser.parse(negative));
    }

    @Test
    void a9_negativeTick_rejected() {
        JsonObject root = baseClip("test:a9", 10);
        JsonArray track = new JsonArray();
        track.add(frame(-1, "pitch", 0F, null));
        root.getAsJsonObject("tracks").add("body", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a10_emptyTracksObject_parsesSuccessfully() {
        JsonObject root = baseClip("test:a10", 10);
        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);
        assertTrue(clip.tracks().isEmpty());
    }

    @Test
    void a11_singleKeyframeTrack_sampleAlwaysReturnsThatValue() {
        JsonObject root = baseClip("test:a11", 10);
        JsonArray track = new JsonArray();
        track.add(frame(5, "pitch", 42F, null));
        root.getAsJsonObject("tracks").add("body", track);

        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);
        assertEquals(42F, clip.sample(Bone.BODY, 0F).orElseThrow().pitch());
        assertEquals(42F, clip.sample(Bone.BODY, 5F).orElseThrow().pitch());
        assertEquals(42F, clip.sample(Bone.BODY, 10F).orElseThrow().pitch());
    }

    @Test
    void missingId_rejected() {
        JsonObject root = new JsonObject();
        root.addProperty("duration_ticks", 10);
        root.add("tracks", new JsonObject());
        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void missingTracksObject_rejected() {
        JsonObject root = new JsonObject();
        root.addProperty("id", "test:missing_tracks");
        root.addProperty("duration_ticks", 10);
        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void loopFieldDefaultsFalse_andParsesTrueWhenPresent() {
        JsonObject root = baseClip("test:loop_default", 10);
        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);
        assertFalse(clip.loop());

        JsonObject loopingRoot = JsonParser.parseString(
                "{\"id\":\"test:loop_true\",\"duration_ticks\":10,\"loop\":true,\"tracks\":{}}").getAsJsonObject();
        AnimationClip looping = AnimationClipParser.parse(loopingRoot);
        assertNotNull(looping);
        assertTrue(looping.loop());
    }

    @Test
    void emptyKeyframeArray_rejected() {
        JsonObject root = baseClip("test:empty_track", 10);
        root.getAsJsonObject("tracks").add("body", new JsonArray());
        assertNull(AnimationClipParser.parse(root));
    }

    // 引擎审计修复 任务A / A1 (表现引擎全面审计报告_2026-07-15.md A1): a keyframe still declaring
    // x/y/z must be rejected outright, not silently ignored -- see Keyframe's own doc for why v1
    // has no positional channel at all.

    @Test
    void a12_keyframeWithXKey_rejected() {
        JsonObject root = baseClip("test:a12x", 10);
        JsonArray track = new JsonArray();
        JsonObject frame = new JsonObject();
        frame.addProperty("tick", 0);
        frame.addProperty("pitch", 5F);
        frame.addProperty("x", 1.0F);
        track.add(frame);
        root.getAsJsonObject("tracks").add("body", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a13_keyframeWithYKey_rejected() {
        JsonObject root = baseClip("test:a13y", 10);
        JsonArray track = new JsonArray();
        JsonObject frame = new JsonObject();
        frame.addProperty("tick", 0);
        frame.addProperty("pitch", 5F);
        frame.addProperty("y", 6.0F);
        track.add(frame);
        root.getAsJsonObject("tracks").add("right_shin", track);

        assertNull(AnimationClipParser.parse(root));
    }

    @Test
    void a14_keyframeWithZKey_rejected() {
        JsonObject root = baseClip("test:a14z", 10);
        JsonArray track = new JsonArray();
        JsonObject frame = new JsonObject();
        frame.addProperty("tick", 0);
        frame.addProperty("pitch", 5F);
        frame.addProperty("z", -2.0F);
        track.add(frame);
        root.getAsJsonObject("tracks").add("right_leg", track);

        assertNull(AnimationClipParser.parse(root));
    }

    // C组: migration-equivalence prep, using hand-transcribed values from the real prototype files
    // (src/main/resources/assets/careerchronicle/player_animation/*.json), read and copied
    // verbatim -- not invented.

    @Test
    void c1_castOnehandQuickTranscription_valuesMatchOriginalKeyframesExactly() {
        JsonObject root = baseClip("careerchronicle:cast_onehand_quick", 10);
        JsonArray rightArm = new JsonArray();
        rightArm.add(pitchRoll(0, 0F, 0F, "ease_in_out_sine"));
        rightArm.add(pitchRoll(3, -20F, 10F, "ease_in_out_sine"));
        rightArm.add(pitchRoll(6, -70F, -15F, "ease_out_expo"));
        rightArm.add(pitchRoll(10, 0F, 0F, "ease_in_out_sine"));
        root.getAsJsonObject("tracks").add("right_arm", rightArm);
        JsonArray body = new JsonArray();
        body.add(pitchYaw(0, 0F, 0F, "ease_in_out_sine"));
        body.add(pitchYaw(3, 5F, -5F, "ease_in_out_sine"));
        body.add(pitchYaw(6, -5F, 5F, "ease_out_expo"));
        body.add(pitchYaw(10, 0F, 0F, "ease_in_out_sine"));
        root.getAsJsonObject("tracks").add("body", body);

        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);

        assertEquals(-20F, clip.sample(Bone.RIGHT_ARM, 3F).orElseThrow().pitch());
        assertEquals(10F, clip.sample(Bone.RIGHT_ARM, 3F).orElseThrow().roll());
        assertEquals(-70F, clip.sample(Bone.RIGHT_ARM, 6F).orElseThrow().pitch());
        assertEquals(-15F, clip.sample(Bone.RIGHT_ARM, 6F).orElseThrow().roll());
        assertEquals(-5F, clip.sample(Bone.BODY, 6F).orElseThrow().pitch());
        assertEquals(5F, clip.sample(Bone.BODY, 6F).orElseThrow().yaw());
    }

    @Test
    void c2_castStompTranscription_leftAndRightLegAreAsymmetric() {
        JsonObject root = baseClip("careerchronicle:cast_stomp", 14);
        JsonArray rightLeg = new JsonArray();
        rightLeg.add(pitchOnly(0, 0F, "ease_in_out_sine"));
        rightLeg.add(pitchOnly(4, 55F, "ease_in_out_sine"));
        rightLeg.add(pitchOnly(7, -55F, "ease_out_expo"));
        rightLeg.add(pitchOnly(9, -32F, "ease_in_out_sine"));
        rightLeg.add(pitchOnly(14, 0F, "ease_in_out_sine"));
        root.getAsJsonObject("tracks").add("right_leg", rightLeg);
        JsonArray leftLeg = new JsonArray();
        leftLeg.add(pitchOnly(0, 0F, "ease_in_out_sine"));
        leftLeg.add(pitchOnly(4, 40F, "ease_in_out_sine"));
        leftLeg.add(pitchOnly(7, 12F, "ease_out_expo"));
        leftLeg.add(pitchOnly(9, 8F, "ease_in_out_sine"));
        leftLeg.add(pitchOnly(14, 0F, "ease_in_out_sine"));
        root.getAsJsonObject("tracks").add("left_leg", leftLeg);

        AnimationClip clip = AnimationClipParser.parse(root);
        assertNotNull(clip);

        float rightAt4 = clip.sample(Bone.RIGHT_LEG, 4F).orElseThrow().pitch();
        float leftAt4 = clip.sample(Bone.LEFT_LEG, 4F).orElseThrow().pitch();
        assertEquals(55F, rightAt4);
        assertEquals(40F, leftAt4);
        assertTrue(rightAt4 != leftAt4, "right/left leg must diverge -- mirrored legs would fail this");

        float rightAt7 = clip.sample(Bone.RIGHT_LEG, 7F).orElseThrow().pitch();
        float leftAt7 = clip.sample(Bone.LEFT_LEG, 7F).orElseThrow().pitch();
        assertEquals(-55F, rightAt7);
        assertEquals(12F, leftAt7);
        assertTrue(rightAt7 != leftAt7);
    }

    private static JsonObject pitchRoll(int tick, float pitch, float roll, String easing) {
        JsonObject o = new JsonObject();
        o.addProperty("tick", tick);
        o.addProperty("pitch", pitch);
        o.addProperty("roll", roll);
        o.addProperty("easing", easing);
        return o;
    }

    private static JsonObject pitchYaw(int tick, float pitch, float yaw, String easing) {
        JsonObject o = new JsonObject();
        o.addProperty("tick", tick);
        o.addProperty("pitch", pitch);
        o.addProperty("yaw", yaw);
        o.addProperty("easing", easing);
        return o;
    }

    private static JsonObject pitchOnly(int tick, float pitch, String easing) {
        JsonObject o = new JsonObject();
        o.addProperty("tick", tick);
        o.addProperty("pitch", pitch);
        o.addProperty("easing", easing);
        return o;
    }
}
