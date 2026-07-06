package com.hongyuwu.careerchronicle.skill.effect;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EffectOpRegistryTest {

    @BeforeEach
    void setUp() {
        EffectOpRegistry.clearForTesting();
    }

    @AfterEach
    void tearDown() {
        EffectOpRegistry.clearForTesting();
    }

    private static final EffectOp NOOP = (ctx, params) -> {};

    @Test
    void registerAndGet() {
        EffectOpRegistry.register("damage", NOOP);
        assertSame(NOOP, EffectOpRegistry.get("damage"));
    }

    @Test
    void getNonexistentReturnsNull() {
        assertNull(EffectOpRegistry.get("nonexistent"));
    }

    @Test
    void duplicateRegistrationThrows() {
        EffectOpRegistry.register("damage", NOOP);
        assertThrows(IllegalStateException.class, () ->
                EffectOpRegistry.register("damage", NOOP));
    }

    @Test
    void existsCorrectness() {
        EffectOpRegistry.register("damage", NOOP);
        assertTrue(EffectOpRegistry.exists("damage"));
        assertFalse(EffectOpRegistry.exists("nope"));
    }

    @Test
    void allNamesReturnsFullSet() {
        EffectOpRegistry.register("a", NOOP);
        EffectOpRegistry.register("b", NOOP);
        EffectOpRegistry.register("c", NOOP);
        assertEquals(3, EffectOpRegistry.allNames().size());
        assertTrue(EffectOpRegistry.allNames().containsAll(java.util.Set.of("a", "b", "c")));
    }

    @Test
    void registerBuiltinsRegistersAtLeastTen() {
        EffectOpRegistry.registerBuiltins();
        assertTrue(EffectOpRegistry.allNames().size() >= 10,
                "Expected at least 10 builtin ops, got " + EffectOpRegistry.allNames().size());
    }
}
