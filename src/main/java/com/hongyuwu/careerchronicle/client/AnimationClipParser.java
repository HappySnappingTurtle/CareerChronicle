package com.hongyuwu.careerchronicle.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hongyuwu.careerchronicle.CareerChronicleMod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段3-任务1-设计文档-JSON关键帧Schema.md §三. Parses+validates a {@code JsonObject} into an
 * {@link AnimationClip}. Never throws for malformed input -- logs an ERROR and returns
 * {@code null}, matching the project-wide "bad data degrades gracefully, never crashes the
 * client" convention already established for {@code FxSpec}/{@code AnimationDriverRegistry}.
 */
public final class AnimationClipParser {

    private AnimationClipParser() {
    }

    public static AnimationClip parse(JsonObject json) {
        String id = optString(json, "id");
        if (id == null || id.isEmpty()) {
            logReject("missing or empty 'id'");
            return null;
        }
        if (!json.has("duration_ticks") || !json.get("duration_ticks").isJsonPrimitive()) {
            logReject(id, "missing 'duration_ticks'");
            return null;
        }
        int durationTicks = json.get("duration_ticks").getAsInt();
        if (durationTicks <= 0) {
            logReject(id, "duration_ticks must be positive, got " + durationTicks);
            return null;
        }
        boolean loop = json.has("loop") && json.get("loop").getAsBoolean();

        if (!json.has("tracks") || !json.get("tracks").isJsonObject()) {
            logReject(id, "missing 'tracks' object");
            return null;
        }
        Map<Bone, List<Keyframe>> tracks = new EnumMap<>(Bone.class);
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tracks").entrySet()) {
            Bone bone = Bone.fromJsonKey(entry.getKey());
            if (bone == null) {
                logReject(id, "unknown bone '" + entry.getKey() + "'");
                return null;
            }
            if (!entry.getValue().isJsonArray()) {
                logReject(id, "track for '" + entry.getKey() + "' must be an array");
                return null;
            }
            List<Keyframe> keyframes = parseTrack(id, entry.getValue().getAsJsonArray());
            if (keyframes == null) {
                return null; // reason already logged by parseTrack
            }
            tracks.put(bone, keyframes);
        }

        return new AnimationClip(id, durationTicks, loop, Map.copyOf(tracks));
    }

    private static List<Keyframe> parseTrack(String animId, JsonArray array) {
        List<Keyframe> keyframes = new ArrayList<>();
        int previousTick = Integer.MIN_VALUE;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                logReject(animId, "keyframe entries must be objects");
                return null;
            }
            JsonObject frame = element.getAsJsonObject();
            if (!frame.has("tick") || !frame.get("tick").isJsonPrimitive()) {
                logReject(animId, "keyframe missing 'tick'");
                return null;
            }
            int tick = frame.get("tick").getAsInt();
            if (tick < 0) {
                logReject(animId, "keyframe tick must be non-negative, got " + tick);
                return null;
            }
            if (tick <= previousTick) {
                logReject(animId, "keyframe ticks must be strictly ascending with no duplicates "
                        + "(got " + tick + " after " + previousTick + ")");
                return null;
            }
            previousTick = tick;

            Easing easing = Easing.LINEAR;
            if (frame.has("easing")) {
                easing = Easing.fromJsonKey(optString(frame, "easing"));
                if (easing == null) {
                    logReject(animId, "unknown easing '" + frame.get("easing") + "'");
                    return null;
                }
            }

            // 引擎审计修复 任务A / 决策D1 (表现引擎全面审计报告_2026-07-15.md A1): v1 has no
            // positional channel -- see Keyframe's doc. A keyframe that still declares x/y/z is
            // rejected outright rather than silently ignored, so stale/copy-pasted data from
            // before this fix is caught at load time instead of quietly doing nothing.
            if (frame.has("x") || frame.has("y") || frame.has("z")) {
                logReject(animId, "keyframe at tick " + tick + " declares 'x'/'y'/'z' -- "
                        + "positional keyframes are not supported in this engine version");
                return null;
            }

            keyframes.add(new Keyframe(
                    tick,
                    optFloat(frame, "pitch"),
                    optFloat(frame, "yaw"),
                    optFloat(frame, "roll"),
                    easing));
        }
        if (keyframes.isEmpty()) {
            logReject(animId, "track has no keyframes");
            return null;
        }
        return keyframes;
    }

    private static float optFloat(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : 0F;
    }

    private static String optString(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    private static void logReject(String reason) {
        CareerChronicleMod.LOGGER.error("[CareerChronicle] AnimationClipParser: rejected animation ({})", reason);
    }

    private static void logReject(String animId, String reason) {
        CareerChronicleMod.LOGGER.error("[CareerChronicle] AnimationClipParser: rejected animation '{}' ({})", animId, reason);
    }
}
