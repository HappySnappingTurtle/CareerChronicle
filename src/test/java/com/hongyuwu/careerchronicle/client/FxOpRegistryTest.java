package com.hongyuwu.careerchronicle.client;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-06 D group: FxOpRegistry's new ops (anim/hitstop/camera_punch/circle).
 *
 * FxOp.play(FxPlayContext, CompoundTag) is inherently client-runtime shaped
 * (ClientLevel, live entities) -- GameTest can't reach it either (GameTest is
 * server-side only, there is no ClientLevel in that environment), matching
 * the pre-existing SoundFxOp/ParticlesFxOp/ShakeFxOp (0.4-05a), which also
 * have zero JUnit coverage of their play() bodies for the same reason. What
 * IS safely JUnit-testable without a Minecraft bootstrap:
 *  - registration (the op id resolves via FxOpRegistry, so FxClientDispatch's
 *    "unknown FxOp" warning path is not triggered for it) -- D1/D3.
 *  - hitstop/camera_punch's play() bodies are *entirely* empty at the 0.4-06
 *    stage (no ctx dereference at all), so calling them with a null context
 *    is a genuine, meaningful "does not throw" check, not a workaround -- D2.
 *  - FxOpRegistry.get() returning null for a truly unregistered id (the
 *    precondition FxClientDispatch's per-id "warn once" path relies on) -- D4.
 * anim/circle's actual behavior (swingArm triggered / particles spawned
 * without throwing) requires a real ClientLevel + entity and is covered by
 * real dynamic verification (./gradlew runClient -Pautotest), not JUnit.
 */
class FxOpRegistryTest {

    @AfterEach
    void resetRegistry() {
        FxOpRegistry.clearForTesting();
    }

    // D1 (registration half): anim resolves via the registry.
    @Test
    void anim_isRegistered() {
        FxOpRegistry.clearForTesting();
        FxOpRegistry.registerBuiltins();

        assertNotNull(FxOpRegistry.get("anim"));
        assertTrue(FxOpRegistry.allIds().contains("anim"));
    }

    // D2: hitstop/camera_punch are registered and their 0.4-06 no-op bodies
    // genuinely do not throw or touch the context (call with null ctx).
    @Test
    void hitstop_and_cameraPunch_registeredAndNoOp() {
        FxOpRegistry.clearForTesting();
        FxOpRegistry.registerBuiltins();

        assertNotNull(FxOpRegistry.get("hitstop"));
        assertNotNull(FxOpRegistry.get("camera_punch"));

        assertDoesNotThrow(() -> FxOpRegistry.get("hitstop").play(null, new CompoundTag()));
        assertDoesNotThrow(() -> FxOpRegistry.get("camera_punch").play(null, new CompoundTag()));
    }

    // D3 (registration half): circle resolves via the registry.
    @Test
    void circle_isRegistered() {
        FxOpRegistry.clearForTesting();
        FxOpRegistry.registerBuiltins();

        assertNotNull(FxOpRegistry.get("circle"));
        assertTrue(FxOpRegistry.allIds().contains("circle"));
    }

    // D4: a truly unregistered op id resolves to null (the precondition
    // FxClientDispatch.play()'s per-id "warn once, skip" branch relies on).
    @Test
    void unregisteredOp_resolvesToNull() {
        FxOpRegistry.clearForTesting();
        FxOpRegistry.registerBuiltins();

        assertNull(FxOpRegistry.get("totally_unknown_op_xyz"));
    }

    @Test
    void registerBuiltins_isIdempotent_allSevenOpsPresentExactlyOnce() {
        FxOpRegistry.clearForTesting();
        FxOpRegistry.registerBuiltins();
        FxOpRegistry.registerBuiltins(); // second call must be a no-op guard, not a duplicate-registration throw

        assertEquals(8, FxOpRegistry.allIds().size());
        assertTrue(FxOpRegistry.allIds().containsAll(java.util.Set.of(
                "sound", "particles", "shake", "anim", "hitstop", "camera_punch", "circle", "hit_layered")));
    }
}
