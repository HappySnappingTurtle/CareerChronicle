package com.hongyuwu.careerchronicle.client;

/**
 * The result of sampling a {@link Bone}'s track at some point in time (阶段3-任务1-设计文档 §三).
 * Degrees for rotation, matching {@link Keyframe}.
 *
 * <p>引擎审计修复 任务A / 决策D1: no positional (x/y/z) fields -- see {@link Keyframe}'s doc for why.
 */
public record BonePose(float pitch, float yaw, float roll) {

    public static final BonePose ZERO = new BonePose(0F, 0F, 0F);
}
