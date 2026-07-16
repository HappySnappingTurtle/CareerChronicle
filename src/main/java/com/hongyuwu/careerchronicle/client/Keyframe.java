package com.hongyuwu.careerchronicle.client;

/**
 * One keyframe on a single {@link Bone}'s track (阶段3-任务1-设计文档-JSON关键帧Schema.md §二).
 * {@code pitch}/{@code yaw}/{@code roll} are degrees, mapped 1:1 onto {@code ModelPart}'s
 * {@code xRot}/{@code yRot}/{@code zRot}. Any field a keyframe's JSON omits is 0 for *that
 * keyframe* -- not carried over from the previous keyframe -- confirmed by cross-checking every
 * existing prototype animation file, none of which ever changes which axes a bone uses
 * mid-animation.
 *
 * <p><b>引擎审计修复 任务A / 决策D1:</b> v1 has no positional (x/y/z) channel -- the previous
 * design that let a keyframe also carry an absolute {@code ModelPart.x/y/z} write was a confirmed
 * bug (表现引擎全面审计报告_2026-07-15.md A1): every existing clip omits x/y/z, so every tracked
 * bone's pivot was silently forced to (0,0,0) during playback, and for {@code rightLeg.x}/
 * {@code leftShin.y} -- pivots vanilla's own {@code setupAnim()} never rewrites -- the damage was
 * permanent on the shared model instance across all players until client restart. Removed rather
 * than fixed-to-be-an-offset: zero of the 11 shipped clips ever used it, and doing an offset
 * channel correctly needs its own "what restores it when the clip stops" design, deferred to
 * whenever a dash/lunge-style animation actually needs one. {@link AnimationClipParser} now
 * rejects any keyframe JSON that still declares an {@code x}/{@code y}/{@code z} key.
 *
 * <p>{@code easing} describes the blend curve used on the segment from this keyframe *into* the
 * next one on the same track.
 */
public record Keyframe(int tick, float pitch, float yaw, float roll, Easing easing) {
}
