package com.hongyuwu.careerchronicle.client;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-09a B组: AnimFxOp's degrade-to-swing paths.
 *
 * {@link AnimFxOp#play(FxPlayContext, CompoundTag)} itself resolves the entity off a real
 * {@code ClientLevel} and swings a real {@code LivingEntity} -- like the rest of
 * {@code FxOp.play()} (see {@code FxOpRegistryTest}'s class doc), that needs a real Minecraft
 * bootstrap and is not JUnit-testable. That includes mocking: a spike attempt at
 * {@code Mockito.mock(LivingEntity.class)} here failed with
 * "Cannot instrument class net.minecraft.world.entity.LivingEntity because it or one of its
 * supertypes could not be initialized" (LivingEntity's static init chain reaches
 * {@code BuiltInRegistries}, which requires {@code Bootstrap.bootStrap()} to have run) -- so
 * Mockito is *not* a way around this constraint either, confirming the historical precedent.
 *
 * Everything downstream of "we have resolved an entity" is exposed via the package-private
 * {@link AnimFxOp#applyAnimation(CompoundTag, IAnimationDriver, LivingEntity, Runnable)} (fully
 * covered below with a hand-written fake {@link IAnimationDriver} and a counting {@code Runnable}
 * standing in for the real {@code swing()} call -- {@code living} itself is passed as
 * {@code null} throughout since {@code applyAnimation} never dereferences it directly, only
 * forwards it opaquely to the driver), and the entity-resolution edge case (B7) is covered via
 * {@link AnimFxOp#resolveAndPlay(net.minecraft.world.entity.Entity, CompoundTag, IAnimationDriver)}
 * with a literal {@code null} entity.
 */
class AnimFxOpTest {

    private static CompoundTag paramsWithId(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        return tag;
    }

    /** Hand-written fake -- IAnimationDriver is our own plain interface, no Minecraft bootstrap involved. */
    private static final class FakeDriver implements IAnimationDriver {
        private final Boolean result;
        private final RuntimeException failure;
        private int calls;
        private String lastAnimId;
        private boolean lastUpperBodyOnly;
        private float lastSpeed;
        private boolean lastIsBasicAttack;

        private FakeDriver(Boolean result, RuntimeException failure) {
            this.result = result;
            this.failure = failure;
        }

        static FakeDriver returning(boolean result) {
            return new FakeDriver(result, null);
        }

        static FakeDriver throwing(RuntimeException e) {
            return new FakeDriver(null, e);
        }

        @Override
        public boolean playAnimation(LivingEntity entity, String animId, boolean upperBodyOnly, float speed,
                boolean isBasicAttack) {
            calls++;
            lastAnimId = animId;
            lastUpperBodyOnly = upperBodyOnly;
            lastSpeed = speed;
            lastIsBasicAttack = isBasicAttack;
            if (failure != null) {
                throw failure;
            }
            return result;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    // B1: driver plays successfully -> no swing.
    @Test
    void driverPlaysSuccessfully_doesNotSwing() {
        FakeDriver driver = FakeDriver.returning(true);
        AtomicInteger swingCount = new AtomicInteger();

        AnimFxOp.applyAnimation(paramsWithId("careerchronicle:cast_onehand_quick"), driver, null, swingCount::incrementAndGet);

        assertEquals(0, swingCount.get());
        assertEquals(1, driver.calls);
    }

    // B2: driver returns false -> swing fallback.
    @Test
    void driverReturnsFalse_fallsBackToSwing() {
        FakeDriver driver = FakeDriver.returning(false);
        AtomicInteger swingCount = new AtomicInteger();

        AnimFxOp.applyAnimation(paramsWithId("careerchronicle:cast_onehand_quick"), driver, null, swingCount::incrementAndGet);

        assertEquals(1, swingCount.get());
    }

    // B3: driver throws -> exception caught, swing fallback, no propagation.
    @Test
    void driverThrows_caughtAndFallsBackToSwing() {
        FakeDriver driver = FakeDriver.throwing(new RuntimeException("boom"));
        AtomicInteger swingCount = new AtomicInteger();

        assertDoesNotThrow(() -> AnimFxOp.applyAnimation(
                paramsWithId("careerchronicle:cast_onehand_quick"), driver, null, swingCount::incrementAndGet));

        assertEquals(1, swingCount.get());
    }

    // B4: missing 'id' -> old unconditional-swing behavior, driver never consulted.
    @Test
    void missingId_swingsWithoutConsultingDriver() {
        FakeDriver driver = FakeDriver.returning(true);
        AtomicInteger swingCount = new AtomicInteger();
        CompoundTag params = new CompoundTag(); // no "id" key

        AnimFxOp.applyAnimation(params, driver, null, swingCount::incrementAndGet);

        assertEquals(1, swingCount.get());
        assertEquals(0, driver.calls);
    }

    // B5: missing 'upper_body' -> defaults to true.
    @Test
    void missingUpperBody_defaultsToTrue() {
        FakeDriver driver = FakeDriver.returning(true);
        CompoundTag params = paramsWithId("careerchronicle:cast_onehand_quick"); // no "upper_body" key

        AnimFxOp.applyAnimation(params, driver, null, () -> { });

        assertTrue(driver.lastUpperBodyOnly);
    }

    // B6: missing 'speed' -> defaults to 1.0.
    @Test
    void missingSpeed_defaultsToOne() {
        FakeDriver driver = FakeDriver.returning(true);
        CompoundTag params = paramsWithId("careerchronicle:cast_onehand_quick");
        params.putBoolean("upper_body", false); // pin this so we isolate the speed default

        AnimFxOp.applyAnimation(params, driver, null, () -> { });

        assertFalse(driver.lastUpperBodyOnly);
        assertEquals(1.0F, driver.lastSpeed);
        assertEquals("careerchronicle:cast_onehand_quick", driver.lastAnimId);
    }

    // B7: entity resolution failed (null caster lookup, mirroring ctx.level().getEntity()
    // returning null for a dead/removed entity -- equally covers "resolved but not a
    // LivingEntity", since both take the same `!(entity instanceof LivingEntity)` branch, and
    // constructing/mocking any real non-null Entity subclass is equally infeasible here without a
    // Minecraft bootstrap) -> quiet no-op, no driver call.
    @Test
    void nullEntity_quietlyNoOps() {
        FakeDriver driver = FakeDriver.returning(true);

        assertDoesNotThrow(() ->
                AnimFxOp.resolveAndPlay(null, paramsWithId("careerchronicle:cast_onehand_quick"), driver));

        assertEquals(0, driver.calls);
    }

    // B8/B9: 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7) -- 'source' param
    // translation to the driver's isBasicAttack flag.

    @Test
    void sourceBasicAttack_translatesToIsBasicAttackTrue() {
        FakeDriver driver = FakeDriver.returning(true);
        CompoundTag params = paramsWithId("careerchronicle:attack_longsword");
        params.putString("source", "basic_attack");

        AnimFxOp.applyAnimation(params, driver, null, () -> { });

        assertTrue(driver.lastIsBasicAttack);
    }

    @Test
    void missingSource_defaultsToIsBasicAttackFalse() {
        FakeDriver driver = FakeDriver.returning(true);
        CompoundTag params = paramsWithId("careerchronicle:cast_onehand_quick"); // no "source" key

        AnimFxOp.applyAnimation(params, driver, null, () -> { });

        assertFalse(driver.lastIsBasicAttack);
    }
}
