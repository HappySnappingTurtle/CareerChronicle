package com.hongyuwu.careerchronicle.client;

import net.minecraft.world.entity.LivingEntity;

/**
 * Abstraction over "how do we play a skeletal cast animation on a player/entity"
 * (0.4-09a 设计文档 §三.1). The concrete backing implementation (currently
 * {@link CustomSkeletonAnimationDriver}, our own JSON-keyframe engine -- formerly
 * {@code PlayerAnimatorDriver} wrapping KosmX's player-animation-lib, replaced in full by
 * 阶段3-任务4-设计文档-接入驱动与移除旧库.md) is isolated behind this interface so it can be swapped
 * without touching {@code anim} fx op JSON or {@link AnimFxOp} itself
 * (0.4-09a 设计文档 §六 R1: "playerAnimator 1.20.1 线已冻结... 最坏情况可换实现，
 * 上层 Schema/资产不受影响" -- exactly the swap that happened).
 */
public interface IAnimationDriver {

    /**
     * Plays the animation identified by {@code animId} (the {@code anim} fx op's
     * {@code id} param in skill JSON, e.g. {@code "careerchronicle:cast_onehand_quick"})
     * on {@code entity}.
     *
     * @param entity        the entity to animate (the skill caster).
     * @param animId        the animation id declared by the skill's anim fx component.
     * @param upperBodyOnly when {@code true}, the animation only covers arms/torso and
     *                      leaves leg/walk animation untouched (skills usable while moving).
     * @param speed         playback speed multiplier ({@code 1.0} = normal speed).
     * @param isBasicAttack 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7):
     *                      {@code true} for a basic (non-skill) attack's animation -- forwarded to
     *                      {@link CustomAnimationPlayer.ClipSource} so a basic attack landing
     *                      mid-cast doesn't hard-cut a skill's animation back to frame 0. {@code
     *                      false} for every {@code anim} fx component originating from a skill's
     *                      own {@code fx} array (the only case that existed before A7).
     * @return {@code false} to mean "this driver could not play the animation" (unregistered
     *         animId, driver unavailable/degraded, lost the {@code isBasicAttack} arbitration
     *         against an in-progress skill animation, or any other reason) -- the caller
     *         ({@link AnimFxOp}) falls back to vanilla {@code swing} in that case. Implementations
     *         must not let exceptions escape driver-internal problems as a substitute for
     *         returning {@code false}, but callers must defensively catch anyway (0.4-09a
     *         设计文档 §三.1: "任何一层失败都不能让技能施放的其他表现连带失败").
     */
    boolean playAnimation(LivingEntity entity, String animId, boolean upperBodyOnly, float speed,
            boolean isBasicAttack);

    /** Whether this driver is currently usable (no conflicting mod detected, library initialized). */
    boolean isAvailable();
}
