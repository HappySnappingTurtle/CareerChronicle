package com.hongyuwu.careerchronicle.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 阶段3-任务4-设计文档-接入驱动与移除旧库.md §二. Replaces {@code PlayerAnimatorDriver} (deleted by this
 * task along with the player-animation-lib dependency it wrapped) as the real backing
 * implementation of {@link IAnimationDriver}, selected by {@link AnimationDriverRegistry} whenever
 * no conflicting mod (Epic Fight / FirstPersonMod) is present.
 */
final class CustomSkeletonAnimationDriver implements IAnimationDriver {

    @Override
    public boolean playAnimation(LivingEntity entity, String animId, boolean upperBodyOnly, float speed,
            boolean isBasicAttack) {
        // Same constraint PlayerAnimatorDriver documented and lived with: the custom skeleton
        // (thigh/shin split model) is only ever swapped into PlayerRenderer (see
        // CustomLegModelSwap), so only AbstractClientPlayer targets have anywhere to apply a pose.
        //
        // Split into playForPlayer() below (same reasoning as AnimFxOp.applyAnimation/
        // resolveAndPlay, see AnimFxOpTest's class doc): constructing a real AbstractClientPlayer
        // in plain JUnit is infeasible here (LivingEntity's static init chain needs
        // Bootstrap.bootStrap(), and Mockito can't instrument it either), so everything past "we
        // have a player's UUID" is exposed as a UUID-keyed method the test suite can call
        // directly, while this instanceof check itself is exercised by passing entity=null (null
        // instanceof AbstractClientPlayer is false, same branch as any non-player entity).
        if (!(entity instanceof AbstractClientPlayer player)) {
            return false;
        }
        if (!swapSucceededForModelName(player.getModelName())) {
            return false;
        }
        CustomAnimationPlayer.ClipSource source = isBasicAttack
                ? CustomAnimationPlayer.ClipSource.BASIC_ATTACK
                : CustomAnimationPlayer.ClipSource.SKILL;
        return playForPlayer(player.getUUID(), animId, speed, source);
    }

    /**
     * 引擎审计修复 任务A / A4 (表现引擎全面审计报告_2026-07-15.md A4): if the reflective model swap
     * failed for this player's skin variant (default vs slim), {@code CustomLegPlayerModel.setupAnim}
     * never runs for them -- nothing will ever read {@code CustomAnimationPlayers}' registry entry
     * for this UUID, so letting {@link #playAnimation} return {@code true} here would silently
     * swallow {@code AnimFxOp}'s {@code swing()} fallback for no visible benefit. Checking per-skin
     * (not the OR'd {@link CustomLegModelSwap#isSwapSucceeded()}) matters: a mixed failure (e.g.
     * slim swap failed, default succeeded) must not falsely gate default-skin players just because
     * some *other* skin variant's swap failed.
     *
     * <p>Extracted as a pure {@code String -> boolean} function (rather than inlined in
     * {@link #playAnimation}) purely so it's unit-testable without a real {@code AbstractClientPlayer}
     * (see this class's own {@code playAnimation} doc for why constructing one is infeasible here).
     */
    static boolean swapSucceededForModelName(String modelName) {
        return "slim".equals(modelName)
                ? CustomLegModelSwap.isSlimSwapSucceeded()
                : CustomLegModelSwap.isDefaultSwapSucceeded();
    }

    /** 阶段3-任务4-单元测试用例文档 B组 B2-B4. Everything downstream of "we've resolved a player's
     * UUID" -- fully testable without a real entity. Deliberately does not check
     * {@link CustomLegModelSwap}'s swap state (that's {@link #playAnimation}'s job, using the real
     * entity's skin variant) -- kept swap-agnostic so the JUnit suite can exercise clip
     * registration/playback without needing a swapped-in model to exist. Defaults to
     * {@link CustomAnimationPlayer.ClipSource#SKILL} -- kept as the pre-A7 3-arg signature so the
     * existing B2-B4 test suite (predating source-aware arbitration) is unaffected. */
    static boolean playForPlayer(UUID playerId, String animId, float speed) {
        return playForPlayer(playerId, animId, speed, CustomAnimationPlayer.ClipSource.SKILL);
    }

    /** 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): source-aware overload used by
     * production ({@link #playAnimation}); see {@link CustomAnimationPlayer#play(AnimationClip,
     * float, CustomAnimationPlayer.ClipSource)} for the arbitration rule itself. */
    static boolean playForPlayer(UUID playerId, String animId, float speed, CustomAnimationPlayer.ClipSource source) {
        AnimationClip clip = AnimationClipRegistry.get(animId);
        if (clip == null) {
            return false;
        }
        return CustomAnimationPlayers.getOrCreate(playerId).play(clip, speed, source);
    }

    @Override
    public boolean isAvailable() {
        // Unlike PlayerAnimatorDriver, there's no external library that could be "not initialized
        // yet" or "failed to load" -- this driver's only dependency is our own code.
        return true;
    }
}
