package com.hongyuwu.careerchronicle.client;

import java.util.Locale;
import java.util.Map;

/**
 * 阶段3-任务1-设计文档-JSON关键帧Schema.md §二: the 8 controllable bones on
 * {@link CustomLegPlayerModel} (and their armor-model mirrors on {@link CustomLegArmorModel}).
 * Stage 4 will append forearm bones once elbow joints exist -- old animation JSON files stay
 * valid unchanged since a bone simply not appearing in an animation's {@code tracks} means "leave
 * this bone alone" (see {@link AnimationClip}).
 */
public enum Bone {
    HEAD("head"),
    BODY("body"),
    RIGHT_ARM("right_arm"),
    LEFT_ARM("left_arm"),
    RIGHT_LEG("right_leg"),
    LEFT_LEG("left_leg"),
    RIGHT_SHIN("right_shin"),
    LEFT_SHIN("left_shin");

    private static final Map<String, Bone> BY_JSON_KEY;
    static {
        Map<String, Bone> map = new java.util.HashMap<>();
        for (Bone bone : values()) {
            map.put(bone.jsonKey, bone);
        }
        BY_JSON_KEY = Map.copyOf(map);
    }

    private final String jsonKey;

    Bone(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String jsonKey() {
        return jsonKey;
    }

    /** @return the matching {@link Bone}, or {@code null} if {@code key} is not a recognized bone
     * name (callers must not throw -- an unrecognized bone name in an animation file is a data
     * error to be logged and rejected, not a crash, per project convention). */
    public static Bone fromJsonKey(String key) {
        if (key == null) {
            return null;
        }
        return BY_JSON_KEY.get(key.toLowerCase(Locale.ROOT));
    }
}
