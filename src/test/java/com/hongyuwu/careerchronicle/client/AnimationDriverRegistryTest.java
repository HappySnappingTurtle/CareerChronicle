package com.hongyuwu.careerchronicle.client;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-09a A组 + C组: AnimationDriverRegistry driver-selection logic, exercised with an
 * injected modid probe (no real Forge ModList needed).
 */
class AnimationDriverRegistryTest {

    @AfterEach
    void resetRegistry() {
        AnimationDriverRegistry.clearForTesting();
    }

    // A1: no conflicting mod loaded -> CustomSkeletonAnimationDriver selected (阶段3-任务4: replaces
    // the former player-animation-lib-backed PlayerAnimatorDriver, deleted along with that dependency).
    @Test
    void init_noConflict_selectsCustomSkeletonAnimationDriver() {
        AnimationDriverRegistry.init(modid -> false);

        assertInstanceOf(CustomSkeletonAnimationDriver.class, AnimationDriverRegistry.current());
    }

    // A2: epicfight loaded -> NoopAnimationDriver selected, isAvailable() false.
    @Test
    void init_epicfightLoaded_selectsNoopDriver() {
        AnimationDriverRegistry.init(modid -> "epicfight".equals(modid));

        assertInstanceOf(NoopAnimationDriver.class, AnimationDriverRegistry.current());
        assertFalse(AnimationDriverRegistry.current().isAvailable());
    }

    // A3: firstpersonmod loaded -> NoopAnimationDriver selected.
    @Test
    void init_firstPersonModLoaded_selectsNoopDriver() {
        AnimationDriverRegistry.init(modid -> "firstpersonmod".equals(modid));

        assertInstanceOf(NoopAnimationDriver.class, AnimationDriverRegistry.current());
        assertFalse(AnimationDriverRegistry.current().isAvailable());
    }

    // A4: both conflicting mods loaded -> still NoopAnimationDriver, no error from "double hit".
    @Test
    void init_bothConflictsLoaded_stillSelectsNoopDriverWithoutError() {
        assertDoesNotThrow(() -> AnimationDriverRegistry.init(modid -> true));

        assertInstanceOf(NoopAnimationDriver.class, AnimationDriverRegistry.current());
    }

    // A5: init() is idempotent -- the second call must not throw, must not re-invoke the probe,
    // and must not replace the already-selected driver instance.
    @Test
    void init_isIdempotent_secondCallIsNoop() {
        AtomicInteger probeCalls = new AtomicInteger();
        Function<String, Boolean> countingProbe = modid -> {
            probeCalls.incrementAndGet();
            return false;
        };

        AnimationDriverRegistry.init(countingProbe);
        IAnimationDriver first = AnimationDriverRegistry.current();
        int callsAfterFirstInit = probeCalls.get();

        assertDoesNotThrow(() -> AnimationDriverRegistry.init(countingProbe));
        IAnimationDriver second = AnimationDriverRegistry.current();

        assertSame(first, second, "second init() must not replace the selected driver");
        assertEquals(callsAfterFirstInit, probeCalls.get(), "second init() must not re-invoke the probe");
    }

    // C1: CONFLICTING_MODIDS contains exactly epicfight + firstpersonmod, no more, no less.
    @Test
    void conflictingModIds_matchesExactExpectedSet() {
        assertEquals(Set.of("epicfight", "firstpersonmod"), AnimationDriverRegistry.CONFLICTING_MODIDS);
    }
}
