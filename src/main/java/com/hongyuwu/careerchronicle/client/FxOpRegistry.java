package com.hongyuwu.careerchronicle.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Client-side counterpart to EffectOpRegistry (0.4-05a) — same shape, opposite side of the wire. */
public final class FxOpRegistry {
    private static final Map<String, FxOp> OPS = new LinkedHashMap<>();

    private FxOpRegistry() {
    }

    public static void register(String id, FxOp op) {
        if (OPS.containsKey(id)) {
            throw new IllegalStateException("Duplicate FxOp: " + id);
        }
        OPS.put(id, op);
    }

    public static FxOp get(String id) {
        return OPS.get(id);
    }

    public static Set<String> allIds() {
        return Set.copyOf(OPS.keySet());
    }

    /** Idempotent: safe to call more than once (mirrors registerBuiltins guard needs for FMLClientSetupEvent). */
    public static void registerBuiltins() {
        if (!OPS.isEmpty()) {
            return;
        }
        register("sound", new SoundFxOp());
        register("particles", new ParticlesFxOp());
        register("shake", new ShakeFxOp());
        // 0.4-06: anim has a real (if minimal) fallback; hitstop/camera_punch
        // are placeholders (0.4-07 implements them for real); circle is the
        // 0.4-05b particles-approximation placeholder for the future magic
        // circle renderer (0.5-12). Registering all four now means skill JSON
        // can declare them today without triggering FxClientDispatch's
        // "unknown FxOp" warning.
        register("anim", new AnimFxOp());
        register("hitstop", new HitstopFxOp());
        register("camera_punch", new CameraPunchFxOp());
        register("circle", new CircleFxOp());
        // 0.4-07: layered hit sound (base + accent, independently pitch-randomized).
        register("hit_layered", new HitLayeredSoundFxOp());
    }

    public static void clearForTesting() {
        OPS.clear();
    }
}
