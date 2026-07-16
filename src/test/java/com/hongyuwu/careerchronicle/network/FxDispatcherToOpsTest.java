package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.data.FxComponent;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-06 C group: {@code FxDispatcher.toOps(List<FxComponent>, String)}.
 * Supersedes the pre-0.4-06 FxSpecToOpsTest (which tested the now-removed
 * {@code toOps(FxSpec, String)} overload) — the ordering/empty/unknown-fxType
 * coverage that test had is preserved here against the new signature.
 */
class FxDispatcherToOpsTest {

    private static FxComponent component(String op, String when, String key, Object value) {
        CompoundTag tag = new CompoundTag();
        if (value instanceof String s) {
            tag.putString(key, s);
        } else if (value instanceof Integer i) {
            tag.putInt(key, i);
        } else if (value instanceof Float f) {
            tag.putFloat(key, f);
        }
        return new FxComponent(op, when, tag);
    }

    // C1: 5 components (3 cast + 2 hit), fxType="cast" -> only the 3 cast ones, in declared order.
    @Test
    void toOps_filtersByWhen_preservesDeclarationOrder() {
        List<FxComponent> components = List.of(
                component("sound", "cast", "id", "careerchronicle:skill.cast.fire"),
                component("particles", "cast", "id", "minecraft:flame"),
                component("shake", "cast", "strength", 0.3F),
                component("sound", "hit", "id", "careerchronicle:skill.hit.fire"),
                component("particles", "hit", "id", "minecraft:flame")
        );

        List<FxOpSpec> ops = FxDispatcher.toOps(components, "cast");

        assertEquals(3, ops.size());
        assertEquals("sound", ops.get(0).opId());
        assertEquals("particles", ops.get(1).opId());
        assertEquals("shake", ops.get(2).opId());
    }

    // C2: fxType="entity_hit" with only "hit" declared -> entity_hit does NOT reuse hit (strict when match).
    @Test
    void toOps_entityHit_doesNotReuseHitComponents() {
        List<FxComponent> components = List.of(
                component("sound", "hit", "id", "careerchronicle:skill.hit.fire"),
                component("particles", "hit", "id", "minecraft:flame")
        );

        List<FxOpSpec> entityHitOps = FxDispatcher.toOps(components, "entity_hit");

        assertTrue(entityHitOps.isEmpty());
    }

    // C3: empty component list -> empty result, no throw.
    @Test
    void toOps_emptyList_returnsEmptyNotNull() {
        assertNotNull(FxDispatcher.toOps(List.of(), "cast"));
        assertTrue(FxDispatcher.toOps(List.of(), "cast").isEmpty());
        assertTrue(FxDispatcher.toOps(null, "cast").isEmpty());
    }

    // C4: unknown fxType value -> empty result, no throw (forward-compatible).
    @Test
    void toOps_unknownFxType_returnsEmptyNotThrow() {
        List<FxComponent> components = List.of(
                component("sound", "cast", "id", "careerchronicle:skill.cast.fire")
        );

        assertDoesNotThrow(() -> {
            List<FxOpSpec> ops = FxDispatcher.toOps(components, "warmup");
            assertTrue(ops.isEmpty());
        });
    }

    // Params pass through verbatim (regression: no silent field renaming/dropping in translation).
    @Test
    void toOps_paramsPassThroughVerbatim() {
        List<FxComponent> components = List.of(
                component("particles", "cast", "id", "minecraft:flame")
        );
        CompoundTag params = components.get(0).params();
        params.putInt("count", 12);
        params.putFloat("spread", 0.6F);

        List<FxOpSpec> ops = FxDispatcher.toOps(components, "cast");

        assertEquals(1, ops.size());
        assertEquals("minecraft:flame", ops.get(0).params().getString("id"));
        assertEquals(12, ops.get(0).params().getInt("count"));
        assertEquals(0.6F, ops.get(0).params().getFloat("spread"));
    }
}
