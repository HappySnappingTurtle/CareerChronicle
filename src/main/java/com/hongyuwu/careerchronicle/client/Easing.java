package com.hongyuwu.careerchronicle.client;

import java.util.Locale;
import java.util.Map;

/**
 * 阶段3-任务1-设计文档-JSON关键帧Schema.md §二. Describes the blend curve from one keyframe into
 * the next. Only the two curves actually used by the 4 existing player-animation-lib prototype
 * animations are ported over (renamed from their KosmX names {@code INOUTSINE}/{@code OUTEXPO} so
 * task 5's migration is a value-for-value transcription, not a re-derivation), plus {@code linear}
 * as the simplest baseline. More can be added later without touching any existing animation file.
 */
public enum Easing {
    LINEAR("linear") {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    /** Standard "ease in-out sine": {@code -(cos(pi*t) - 1) / 2}. Matches KosmX's INOUTSINE. */
    EASE_IN_OUT_SINE("ease_in_out_sine") {
        @Override
        public float apply(float t) {
            return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
        }
    },
    /** Standard "ease out expo": {@code 1 - 2^(-10t)}, t=1 excepted (already 1.0 exactly there
     * since the formula asymptotically approaches but the domain is clamped to [0,1] by callers).
     * Matches KosmX's OUTEXPO. */
    EASE_OUT_EXPO("ease_out_expo") {
        @Override
        public float apply(float t) {
            if (t >= 1.0F) {
                return 1.0F;
            }
            return (float) (1.0 - Math.pow(2.0, -10.0 * t));
        }
    };

    private static final Map<String, Easing> BY_JSON_KEY;
    static {
        Map<String, Easing> map = new java.util.HashMap<>();
        for (Easing easing : values()) {
            map.put(easing.jsonKey, easing);
        }
        BY_JSON_KEY = Map.copyOf(map);
    }

    private final String jsonKey;

    Easing(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public abstract float apply(float t);

    /** @return the matching {@link Easing}, or {@code null} if unrecognized (data error, not a
     * crash -- see {@link Bone#fromJsonKey} for the same convention). */
    public static Easing fromJsonKey(String key) {
        if (key == null) {
            return null;
        }
        return BY_JSON_KEY.get(key.toLowerCase(Locale.ROOT));
    }
}
