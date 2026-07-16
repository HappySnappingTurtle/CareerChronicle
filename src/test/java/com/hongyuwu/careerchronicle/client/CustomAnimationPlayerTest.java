package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段3-任务2-单元测试用例文档-核心播放引擎.md A组+B组. Builds a *real* {@link CustomLegPlayerModel} via
 * the same baking path production code uses ({@link CustomLegModelSwap}) -- confirmed empirically
 * runnable in plain JUnit, no Minecraft bootstrap needed (pure geometry construction). */
class CustomAnimationPlayerTest {

    private static CustomLegPlayerModel realModel() {
        // CustomLegPlayerModel.createBodyLayer already returns a LayerDefinition (mirrors
        // LayerDefinitions.createRoots()'s own wrapping, per 自定义骨骼引擎-设计文档-手臂腿部关节扩展.md),
        // unlike CustomLegArmorModel.createBodyLayer which returns a bare MeshDefinition.
        ModelPart root = CustomLegPlayerModel.createBodyLayer(false).bakeRoot();
        return new CustomLegPlayerModel(root, false);
    }

    private static AnimationClip clipWithRightArm(int duration, boolean loop, List<Keyframe> keyframes) {
        return new AnimationClip("test:clip", duration, loop, Map.of(Bone.RIGHT_ARM, keyframes));
    }

    // A组

    // 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): source-aware arbitration.

    @Test
    void a2e_basicAttackClip_losesArbitrationAgainstInProgressSkillClip() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(20, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(20, 90F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.SKILL);
        player.tick(); // skill clip is mid-playback (elapsedTicks=1 of 20)

        boolean started = player.play(clipWithRightArm(8, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(8, 45F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        assertFalse(started, "a basic-attack clip must not interrupt an in-progress skill clip");
        assertTrue(player.isPlaying(), "the original skill clip must keep playing");
    }

    @Test
    void a2f_skillClip_alwaysCutsInOverBasicAttack() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(8, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(8, 45F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        boolean started = player.play(clipWithRightArm(20, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(20, 90F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.SKILL);

        assertTrue(started, "a skill clip must always cut in, even over an in-progress basic attack");
    }

    @Test
    void a2g_basicAttackClip_cutsInOverFinishedSkillClip() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(4, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(4, 45F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.SKILL);
        for (int i = 0; i < 4; i++) {
            player.tick(); // skill clip finished (elapsedTicks=4 >= duration 4)
        }
        assertFalse(player.isPlaying());

        boolean started = player.play(clipWithRightArm(8, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(8, 45F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        assertTrue(started, "a basic attack must cut in once the skill clip has already finished");
    }

    @Test
    void a2h_basicAttackClip_cutsInOverInProgressBasicAttackClip() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(20, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(20, 90F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.BASIC_ATTACK);
        player.tick();

        boolean started = player.play(clipWithRightArm(8, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(8, 45F, 0F, 0F, Easing.LINEAR))), 1F, CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        assertTrue(started, "same-source (BASIC_ATTACK->BASIC_ATTACK) hard-cut is unchanged by A7");
    }

    @Test
    void a1_freshPlayer_notPlaying() {
        assertFalse(new CustomAnimationPlayer().isPlaying());
    }

    @Test
    void a2_playSetsPlayingAndZerosElapsed() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip clip = clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR)));
        player.play(clip);
        assertTrue(player.isPlaying());

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0F);
        assertEquals(0F, model.rightArm.xRot, 1e-5F); // elapsedTicks=0 -> first keyframe
    }

    @Test
    void a3_stillPlayingBeforeDuration() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))));
        for (int i = 0; i < 9; i++) {
            player.tick();
        }
        assertTrue(player.isPlaying());
    }

    @Test
    void a4_notPlayingAfterNonLoopingDurationReached() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))));
        for (int i = 0; i < 10; i++) {
            player.tick();
        }
        assertFalse(player.isPlaying());
    }

    @Test
    void a5_loopingStaysPlayingPastDuration() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, true, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))));
        for (int i = 0; i < 1000; i++) {
            player.tick();
        }
        assertTrue(player.isPlaying());
    }

    // 引擎审计修复 任务A / A2 (表现引擎全面审计报告_2026-07-15.md A2): speed<=0 must be rejected,
    // not silently accepted -- speed==0 would freeze elapsedTicks forever (non-looping clip's
    // isPlaying() never becomes false -> CustomAnimationPlayers' registry entry never gets cleaned
    // up), and negative speed would run the timeline backwards.

    @Test
    void a2b_zeroSpeed_rejected_playerStaysNotPlaying() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        boolean started = player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))), 0F);

        assertFalse(started);
        assertFalse(player.isPlaying());
    }

    @Test
    void a2c_negativeSpeed_rejected() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        boolean started = player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))), -1F);

        assertFalse(started);
        assertFalse(player.isPlaying());
    }

    @Test
    void a2d_zeroSpeedRejection_doesNotDisturbAlreadyPlayingClip() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip first = clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR)));
        player.play(first);
        player.tick(); // elapsedTicks=1

        boolean rejected = player.play(clipWithRightArm(4, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(4, 45F, 0F, 0F, Easing.LINEAR))), 0F);

        assertFalse(rejected);
        assertTrue(player.isPlaying(), "the original clip must keep playing, unaffected by the "
                + "rejected play() attempt");
    }

    @Test
    void a1b_positiveSpeed_playReturnsTrue() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        boolean started = player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))), 1F);
        assertTrue(started);
    }

    @Test
    void a6_replayingResetsElapsedAndSwitchesClip() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip first = clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR)));
        player.play(first);
        for (int i = 0; i < 10; i++) {
            player.tick();
        }
        assertFalse(player.isPlaying());

        AnimationClip second = clipWithRightArm(4, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(4, 45F, 0F, 0F, Easing.LINEAR)));
        player.play(second);
        assertTrue(player.isPlaying());

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0F);
        assertEquals(0F, model.rightArm.xRot, 1e-5F); // back to elapsedTicks=0 on the new clip
    }

    // B组

    @Test
    void b1_writesInterpolatedRadiansOntoRightArm() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))));
        for (int i = 0; i < 5; i++) {
            player.tick(); // elapsedTicks = 5 -> midpoint -> pitch = 45 degrees
        }

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0F);
        float expectedRadians = 45F * ((float) Math.PI / 180F);
        assertEquals(expectedRadians, model.rightArm.xRot, 1e-4F);
    }

    @Test
    void b2_untrackedBoneIsNeverTouched() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR))));

        CustomLegPlayerModel model = realModel();
        model.leftArm.xRot = 99F; // sentinel: only right_arm has a track, leftArm must survive untouched
        player.applyTo(model, 0F);
        assertEquals(99F, model.leftArm.xRot, 0F);
    }

    @Test
    void b3_yawAndRollWriteToCorrectFields() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        AnimationClip clip = new AnimationClip("test:b3", 10, false, Map.of(Bone.BODY, List.of(
                new Keyframe(0, 11F, 22F, 33F, Easing.LINEAR))));
        player.play(clip);

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0F);
        float toRad = (float) Math.PI / 180F;
        assertEquals(11F * toRad, model.body.xRot, 1e-5F);
        assertEquals(22F * toRad, model.body.yRot, 1e-5F);
        assertEquals(33F * toRad, model.body.zRot, 1e-5F);
    }

    @Test
    void b4_partialTickInterpolatesBetweenIntegerTicks() {
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(clipWithRightArm(10, false, List.of(
                new Keyframe(3, 0F, 0F, 0F, Easing.LINEAR),
                new Keyframe(4, 10F, 0F, 0F, Easing.LINEAR))));
        for (int i = 0; i < 3; i++) {
            player.tick(); // elapsedTicks = 3
        }

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0.5F); // time = 3.5 -> halfway between tick3(0) and tick4(10) -> 5 degrees
        float expected = 5F * ((float) Math.PI / 180F);
        assertEquals(expected, model.rightArm.xRot, 1e-4F);
    }

    @Test
    void b5_allEightBonesMapToDistinctCorrectFields() {
        Map<Bone, List<Keyframe>> tracks = new java.util.EnumMap<>(Bone.class);
        for (Bone bone : Bone.values()) {
            // encode the bone's ordinal into pitch so each one is independently verifiable
            tracks.put(bone, List.of(new Keyframe(0, bone.ordinal() * 10F, 0F, 0F, Easing.LINEAR)));
        }
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(new AnimationClip("test:b5", 10, false, Map.copyOf(tracks)));

        CustomLegPlayerModel model = realModel();
        player.applyTo(model, 0F);
        float toRad = (float) Math.PI / 180F;
        assertEquals(Bone.HEAD.ordinal() * 10F * toRad, model.head.xRot, 1e-4F);
        assertEquals(Bone.BODY.ordinal() * 10F * toRad, model.body.xRot, 1e-4F);
        assertEquals(Bone.RIGHT_ARM.ordinal() * 10F * toRad, model.rightArm.xRot, 1e-4F);
        assertEquals(Bone.LEFT_ARM.ordinal() * 10F * toRad, model.leftArm.xRot, 1e-4F);
        assertEquals(Bone.RIGHT_LEG.ordinal() * 10F * toRad, model.rightLeg.xRot, 1e-4F);
        assertEquals(Bone.LEFT_LEG.ordinal() * 10F * toRad, model.leftLeg.xRot, 1e-4F);
        assertEquals(Bone.RIGHT_SHIN.ordinal() * 10F * toRad, model.rightShin.xRot, 1e-4F);
        assertEquals(Bone.LEFT_SHIN.ordinal() * 10F * toRad, model.leftShin.xRot, 1e-4F);
    }

    /**
     * B6: 引擎审计修复 任务A / A1 (表现引擎全面审计报告_2026-07-15.md A1) regression guard --
     * {@code applyTo} must never touch a tracked bone's position (x/y/z), only its rotation. This
     * is the exact bug: {@code writeToPart} used to unconditionally write {@code part.x/y/z} from
     * {@link BonePose}, and since no shipped clip ever declared a position, every tracked bone's
     * pivot was silently forced to (0,0,0) during playback -- permanently, for {@code rightLeg.x}/
     * {@code leftShin.y}, since vanilla's own {@code setupAnim()} never rewrites those. Sentinel
     * values planted on every bone's position before {@code applyTo} runs must survive untouched.
     */
    @Test
    void b6_applyTo_neverTouchesBonePosition() {
        Map<Bone, List<Keyframe>> tracks = new java.util.EnumMap<>(Bone.class);
        for (Bone bone : Bone.values()) {
            tracks.put(bone, List.of(new Keyframe(0, 30F, 20F, 10F, Easing.LINEAR)));
        }
        CustomAnimationPlayer player = new CustomAnimationPlayer();
        player.play(new AnimationClip("test:b6", 10, false, Map.copyOf(tracks)));

        CustomLegPlayerModel model = realModel();
        // Sentinel positions, deliberately different from each bone's baked pivot.
        for (net.minecraft.client.model.geom.ModelPart part : List.of(
                model.head, model.body, model.rightArm, model.leftArm,
                model.rightLeg, model.leftLeg, model.rightShin, model.leftShin)) {
            part.x = 123F;
            part.y = 456F;
            part.z = 789F;
        }

        player.applyTo(model, 0F);

        for (net.minecraft.client.model.geom.ModelPart part : List.of(
                model.head, model.body, model.rightArm, model.leftArm,
                model.rightLeg, model.leftLeg, model.rightShin, model.leftShin)) {
            assertEquals(123F, part.x, "applyTo must not touch bone position (x)");
            assertEquals(456F, part.y, "applyTo must not touch bone position (y)");
            assertEquals(789F, part.z, "applyTo must not touch bone position (z)");
        }
    }
}
