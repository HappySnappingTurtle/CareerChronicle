package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug9 regression: SkillExecutorRegistry.exists(null) used to throw a
 * NullPointerException (Set.of(...).contains(null) rejects null), which
 * propagated out of CareerTestCommand's "check every skill has an executor"
 * loop -- that loop calls exists(skill.executor()) for every registered
 * skill, and most skills legitimately have a null executor() post-0.4-05a
 * (they run through the effect-component interpreter instead of the legacy
 * executor pattern). The crash aborted CareerTestCommand.runTests() mid-way,
 * which (before the accompanying backup/restore fix) left the invoking
 * player's real progression permanently overwritten by the test's own
 * destructive intermediate state -- this is what was misdiagnosed as
 * "death/respawn loses unlockedSkills" in the original bug report; a
 * diagnostic build proved PlayerEvent.Clone/PlayerRespawnEvent themselves
 * preserve classHistory/unlockedSkills correctly.
 */
class SkillExecutorRegistryTest {

    @Test
    void exists_nullExecutorId_returnsFalseNotThrow() {
        assertFalse(SkillExecutorRegistry.exists(null));
    }

    @Test
    void exists_knownExecutor_returnsTrue() {
        assertTrue(SkillExecutorRegistry.exists(SkillExecutorRegistry.EAGLE_EYE));
    }

    @Test
    void exists_unknownExecutor_returnsFalse() {
        ResourceLocation unknown = ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, "not_a_real_executor");
        assertFalse(SkillExecutorRegistry.exists(unknown));
    }
}
