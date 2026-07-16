package com.hongyuwu.careerchronicle.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 阶段3-任务4-单元测试用例文档-接入驱动与移除旧库.md A组. */
class AnimationClipRegistryTest {

    private static AnimationClip clip(String id) {
        return new AnimationClip(id, 10, false, Map.of());
    }

    @AfterEach
    void clearRegistry() {
        // No production clearForTesting() exists (registry is only ever refreshed wholesale by
        // reload()), so tests self-isolate by using unique ids per test instead of relying on
        // registry emptiness.
    }

    @Test
    void a1_registerThenGet_returnsSameClip() {
        AnimationClip original = clip("careerchronicle:test_a1");
        AnimationClipRegistry.register(original);
        assertSame(original, AnimationClipRegistry.get("careerchronicle:test_a1"));
    }

    @Test
    void a2_getUnregisteredId_returnsNull() {
        assertNull(AnimationClipRegistry.get("careerchronicle:test_a2_never_registered"));
    }

    @Test
    void a3_reRegisterSameId_overwritesPrevious() {
        AnimationClip first = new AnimationClip("careerchronicle:test_a3", 10, false, Map.of());
        AnimationClip second = new AnimationClip("careerchronicle:test_a3", 20, true, Map.of());
        AnimationClipRegistry.register(first);
        AnimationClipRegistry.register(second);
        AnimationClip result = AnimationClipRegistry.get("careerchronicle:test_a3");
        assertSame(second, result);
        assertEquals(20, result.durationTicks());
        assertEquals(true, result.loop());
    }
}
