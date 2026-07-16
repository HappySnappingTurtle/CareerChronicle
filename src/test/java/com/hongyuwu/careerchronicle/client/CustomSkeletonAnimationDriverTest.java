package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段3-任务4-单元测试用例文档-接入驱动与移除旧库.md B组. B1 exercises the real entry point with a null
 * entity (same {@code instanceof AbstractClientPlayer} branch as any non-player entity, per the
 * class's own doc comment); B2-B5 exercise {@link CustomSkeletonAnimationDriver#playForPlayer} --
 * everything past "we've resolved a player UUID" -- directly, since constructing a real
 * {@code AbstractClientPlayer} is infeasible in plain JUnit (see {@code AnimFxOpTest}).
 */
class CustomSkeletonAnimationDriverTest {

    private static AnimationClip clipWithBodyTrack(String id) {
        Keyframe kf0 = new Keyframe(0, 0F, 0F, 0F, Easing.LINEAR);
        Keyframe kf10 = new Keyframe(10, 90F, 0F, 0F, Easing.LINEAR);
        return new AnimationClip(id, 10, false, Map.of(Bone.BODY, List.of(kf0, kf10)));
    }

    @Test
    void b1_nullEntity_returnsFalse() {
        CustomSkeletonAnimationDriver driver = new CustomSkeletonAnimationDriver();
        assertFalse(driver.playAnimation(null, "careerchronicle:test_b1", true, 1F, false));
    }

    @Test
    void b2_unregisteredAnimId_returnsFalseAndCreatesNoPlayer() {
        UUID playerId = UUID.randomUUID();
        boolean result = CustomSkeletonAnimationDriver.playForPlayer(
                playerId, "careerchronicle:test_b2_never_registered", 1F);
        assertFalse(result);
        assertNull(CustomAnimationPlayers.getIfPresent(playerId));
    }

    @Test
    void b3_registeredAnimId_returnsTrueAndStartsPlaying() {
        UUID playerId = UUID.randomUUID();
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_b3"));

        boolean result = CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_b3", 1F);

        assertTrue(result);
        CustomAnimationPlayer player = CustomAnimationPlayers.getIfPresent(playerId);
        assertTrue(player != null && player.isPlaying());
    }

    @Test
    void b4_speedTwo_advancesElapsedTicksByTwoPerTick() {
        UUID playerId = UUID.randomUUID();
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_b4"));

        CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_b4", 2F);
        CustomAnimationPlayer player = CustomAnimationPlayers.getIfPresent(playerId);
        player.tick(); // elapsedTicks should now be 2, not 1

        CustomLegPlayerModel model = new CustomLegPlayerModel(
                CustomLegPlayerModel.createBodyLayer(false).bakeRoot(), false);
        player.applyTo(model, 0F);

        // Track goes 0deg at tick 0 -> 90deg at tick 10, linear: elapsedTicks=2 -> 18deg.
        float expectedRadians = 18F * (float) Math.PI / 180F;
        org.junit.jupiter.api.Assertions.assertEquals(expectedRadians, model.body.xRot, 1e-4F);
    }

    @Test
    void b5_isAvailable_alwaysTrue() {
        assertTrue(new CustomSkeletonAnimationDriver().isAvailable());
    }

    // C组: 引擎审计修复 任务A / A4 (表现引擎全面审计报告_2026-07-15.md A4). Exercises
    // swapSucceededForModelName directly (a real AbstractClientPlayer is infeasible here, same
    // constraint as B1-B5 above) -- this is the pure function playAnimation delegates to.

    @AfterEach
    void resetSwapState() {
        CustomLegModelSwap.setSwapSucceededForTesting(false, false);
    }

    @Test
    void c1_defaultModelName_defaultSwapFailed_returnsFalse() {
        CustomLegModelSwap.setSwapSucceededForTesting(false, true); // slim ok, default not
        assertFalse(CustomSkeletonAnimationDriver.swapSucceededForModelName("default"));
    }

    @Test
    void c2_defaultModelName_defaultSwapSucceeded_returnsTrue() {
        CustomLegModelSwap.setSwapSucceededForTesting(true, false);
        assertTrue(CustomSkeletonAnimationDriver.swapSucceededForModelName("default"));
    }

    @Test
    void c3_slimModelName_slimSwapFailed_returnsFalse() {
        CustomLegModelSwap.setSwapSucceededForTesting(true, false); // default ok, slim not
        assertFalse(CustomSkeletonAnimationDriver.swapSucceededForModelName("slim"));
    }

    @Test
    void c4_slimModelName_slimSwapSucceeded_returnsTrue() {
        CustomLegModelSwap.setSwapSucceededForTesting(false, true);
        assertTrue(CustomSkeletonAnimationDriver.swapSucceededForModelName("slim"));
    }

    /** C5: the exact "mixed failure" scenario the per-skin check exists to handle -- a default-skin
     * player must not be falsely gated just because the *slim* swap happened to fail. */
    @Test
    void c5_mixedFailure_defaultPlayerUnaffectedBySlimFailure() {
        CustomLegModelSwap.setSwapSucceededForTesting(true, false);
        assertTrue(CustomSkeletonAnimationDriver.swapSucceededForModelName("default"));
        assertFalse(CustomSkeletonAnimationDriver.swapSucceededForModelName("slim"));
    }

    // D组: 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7) -- the source-aware
    // playForPlayer overload used by production playAnimation.

    @Test
    void d1_basicAttackSource_losesArbitrationAgainstInProgressSkillClip() {
        UUID playerId = UUID.randomUUID();
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_d1_skill"));
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_d1_attack"));

        CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_d1_skill", 1F,
                CustomAnimationPlayer.ClipSource.SKILL);
        boolean result = CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_d1_attack", 1F,
                CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        assertFalse(result);
    }

    @Test
    void d2_threeArgOverload_defaultsToSkillSource() {
        // Existing B2-B4 callers never needed to distinguish source -- confirms the 3-arg
        // overload still behaves as ClipSource.SKILL (a subsequent BASIC_ATTACK attempt must lose
        // arbitration against it, same as an explicit SKILL call would).
        UUID playerId = UUID.randomUUID();
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_d2_skill"));
        AnimationClipRegistry.register(clipWithBodyTrack("careerchronicle:test_d2_attack"));

        CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_d2_skill", 1F);
        boolean result = CustomSkeletonAnimationDriver.playForPlayer(playerId, "careerchronicle:test_d2_attack", 1F,
                CustomAnimationPlayer.ClipSource.BASIC_ATTACK);

        assertFalse(result);
    }
}
