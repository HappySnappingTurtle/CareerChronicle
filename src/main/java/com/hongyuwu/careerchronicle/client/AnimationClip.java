package com.hongyuwu.careerchronicle.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A fully-parsed, validated animation (阶段3-任务1-设计文档-JSON关键帧Schema.md §二/§三). Immutable;
 * produced only by {@link AnimationClipParser}.
 *
 * @param tracks per-bone keyframe lists, each already sorted ascending by tick and validated
 *               (see {@link AnimationClipParser}). A bone absent from this map means "not
 *               controlled by this animation" -- {@link #sample} returns {@link Optional#empty()}
 *               for it, and the caller (阶段3-任务2's player) must leave that bone untouched
 *               rather than writing anything, which is exactly how "upper-body only" animations
 *               already work today (no separate flag, confirmed against the real
 *               player-animation-lib behavior this replaces).
 */
public record AnimationClip(String id, int durationTicks, boolean loop, Map<Bone, List<Keyframe>> tracks) {

    /**
     * Samples {@code bone}'s pose at animation-relative {@code time} (ticks, may have a fractional
     * part for sub-tick/partialTick smoothness). Time before the first keyframe or after the last
     * one clamps to that edge keyframe's value (no extrapolation) -- when {@link #loop} is true,
     * {@code time} is first wrapped into {@code [0, durationTicks)} via modulo, then the same
     * clamp-based bracket search runs on the wrapped value. This does not blend across the
     * loop seam (last keyframe back to first) -- none of stage 3's animations use looping, so a
     * true cyclic blend is left for whenever a looping animation actually needs it.
     */
    public Optional<BonePose> sample(Bone bone, float time) {
        List<Keyframe> track = tracks.get(bone);
        if (track == null || track.isEmpty()) {
            return Optional.empty();
        }

        float t = time;
        if (loop && durationTicks > 0) {
            t = t % durationTicks;
            if (t < 0) {
                t += durationTicks;
            }
        }

        Keyframe first = track.get(0);
        if (t <= first.tick()) {
            return Optional.of(toPose(first));
        }
        Keyframe last = track.get(track.size() - 1);
        if (t >= last.tick()) {
            return Optional.of(toPose(last));
        }

        for (int i = 0; i < track.size() - 1; i++) {
            Keyframe k1 = track.get(i);
            Keyframe k2 = track.get(i + 1);
            if (t >= k1.tick() && t <= k2.tick()) {
                float span = k2.tick() - k1.tick();
                float localT = span <= 0F ? 0F : (t - k1.tick()) / span;
                float eased = k1.easing().apply(localT);
                return Optional.of(lerp(k1, k2, eased));
            }
        }
        // Unreachable: the two clamp checks above cover t <= first.tick and t >= last.tick, and
        // the track is validated sorted-ascending-no-duplicates by AnimationClipParser, so every
        // remaining t falls in exactly one bracket. Kept only as a defensive non-throwing fallback.
        return Optional.of(toPose(last));
    }

    private static BonePose toPose(Keyframe k) {
        return new BonePose(k.pitch(), k.yaw(), k.roll());
    }

    private static BonePose lerp(Keyframe a, Keyframe b, float t) {
        return new BonePose(
                lerp(a.pitch(), b.pitch(), t),
                lerp(a.yaw(), b.yaw(), t),
                lerp(a.roll(), b.roll(), t));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
