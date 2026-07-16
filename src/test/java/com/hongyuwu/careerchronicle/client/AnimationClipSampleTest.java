package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段3-任务1-单元测试用例文档-JSON关键帧Schema.md B组: interpolation sampling, built directly
 * against {@link AnimationClip} records (no JSON parsing involved -- isolates sampling math from
 * parsing correctness, which {@link AnimationClipParserTest} already covers separately). */
class AnimationClipSampleTest {

    private static AnimationClip clipWithBodyTrack(int duration, boolean loop, List<Keyframe> keyframes) {
        return new AnimationClip("test:sample", duration, loop, Map.of(Bone.BODY, keyframes));
    }

    @Test
    void b1_midpointLinear_exactAverage() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 100F, 0F, 0F, Easing.LINEAR)));
        assertEquals(50F, clip.sample(Bone.BODY, 5F).orElseThrow().pitch());
    }

    @Test
    void b2_exactKeyframeTick_returnsExactValue() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(3, -20F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 0F, 0F, 0F, Easing.LINEAR)));
        assertEquals(-20F, clip.sample(Bone.BODY, 3F).orElseThrow().pitch());
    }

    @Test
    void b3_timeBeforeFirstKeyframe_clampsToFirst() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(2, 5F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 50F, 0F, 0F, Easing.LINEAR)));
        assertEquals(5F, clip.sample(Bone.BODY, 0F).orElseThrow().pitch());
        assertEquals(5F, clip.sample(Bone.BODY, -100F).orElseThrow().pitch());
    }

    @Test
    void b4_timeAfterLastKeyframe_clampsToLast_whenNotLooping() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(8, 99F, 0F, 0F, Easing.LINEAR)));
        assertEquals(99F, clip.sample(Bone.BODY, 10F).orElseThrow().pitch());
        assertEquals(99F, clip.sample(Bone.BODY, 1000F).orElseThrow().pitch());
    }

    @Test
    void b5_loopingWrapsViaModulo() {
        AnimationClip clip = clipWithBodyTrack(10, true, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 100F, 0F, 0F, Easing.LINEAR)));
        // time=15 wraps to 5 within [0,10) -> midpoint -> 50
        assertEquals(50F, clip.sample(Bone.BODY, 15F).orElseThrow().pitch());
        // time=25 wraps to 5 as well (25 % 10 == 5)
        assertEquals(50F, clip.sample(Bone.BODY, 25F).orElseThrow().pitch());
    }

    @Test
    void b6_holdSegment_constantBetweenEqualKeyframes() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(3, 40F, 0F, 0F, Easing.LINEAR),
                new Keyframe(6, 40F, 0F, 0F, Easing.LINEAR), // hold: same value as tick 3
                new Keyframe(10, 0F, 0F, 0F, Easing.LINEAR)));
        assertEquals(40F, clip.sample(Bone.BODY, 3F).orElseThrow().pitch());
        assertEquals(40F, clip.sample(Bone.BODY, 4.5F).orElseThrow().pitch());
        assertEquals(40F, clip.sample(Bone.BODY, 6F).orElseThrow().pitch());
    }

    @Test
    void b7_easeInOutSine_midpointIsApproximatelyHalf() {
        assertEquals(0.5F, Easing.EASE_IN_OUT_SINE.apply(0.5F), 1e-5F);
    }

    @Test
    void b8_easeOutExpo_matchesFormulaAtHalf() {
        float expected = (float) (1.0 - Math.pow(2.0, -5.0));
        assertEquals(expected, Easing.EASE_OUT_EXPO.apply(0.5F), 1e-5F);
    }

    @Test
    void b9_queryingUntrackedBone_returnsEmpty() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR)));
        assertTrue(clip.sample(Bone.RIGHT_SHIN, 0F).isEmpty());
    }

    @Test
    void b10_fractionalPartialTick_interpolatesSmoothly() {
        AnimationClip clip = clipWithBodyTrack(10, false, List.of(
                new Keyframe(3, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(4, 10F, 0F, 0F, Easing.LINEAR)));
        assertEquals(5F, clip.sample(Bone.BODY, 3.5F).orElseThrow().pitch());
    }
}
