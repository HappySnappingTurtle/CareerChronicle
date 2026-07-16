package com.hongyuwu.careerchronicle.client;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2026-07-13 addition (user real-machine feedback on 0.4-09a: ground_slam's stomp particles
 * fired at cast time, well before the animation's foot-impact frame, reading as disconnected
 * from the visual). {@link DelayedFxScheduler} lets an fx component declare {@code delay_ticks}
 * so it fires N client ticks after the rest of its phase instead of immediately.
 *
 * {@link FxPlayContext} needs a real {@code ClientLevel} to construct (Minecraft bootstrap,
 * not available in plain JUnit here -- same constraint documented in {@code AnimFxOpTest}), so
 * every test below passes {@code null} for it: {@link DelayedFxScheduler} only ever forwards
 * the context opaquely to the {@link FxOp} it's holding, never dereferences it itself.
 */
class DelayedFxSchedulerTest {

    @AfterEach
    void clear() {
        DelayedFxScheduler.clearForTesting();
    }

    private static FxOp countingOp(AtomicInteger counter) {
        return (ctx, params) -> counter.incrementAndGet();
    }

    @Test
    void zeroDelayFiresImmediatelyOnSchedule() {
        AtomicInteger fired = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(fired), null, new CompoundTag(), 0);
        assertEquals(1, fired.get());
        assertEquals(0, DelayedFxScheduler.pendingCount());
    }

    @Test
    void negativeDelayTreatedAsImmediate() {
        AtomicInteger fired = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(fired), null, new CompoundTag(), -3);
        assertEquals(1, fired.get());
    }

    @Test
    void oneTickDelayFiresAfterOneTick() {
        AtomicInteger fired = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(fired), null, new CompoundTag(), 1);
        assertEquals(0, fired.get(), "must not fire synchronously when delay > 0");
        DelayedFxScheduler.tick();
        assertEquals(1, fired.get());
    }

    @Test
    void multiTickDelayDoesNotFireEarly() {
        AtomicInteger fired = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(fired), null, new CompoundTag(), 3);
        DelayedFxScheduler.tick();
        DelayedFxScheduler.tick();
        assertEquals(0, fired.get(), "two ticks elapsed, delay was three -- must still be pending");
        DelayedFxScheduler.tick();
        assertEquals(1, fired.get());
    }

    @Test
    void firesExactlyOnce() {
        AtomicInteger fired = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(fired), null, new CompoundTag(), 2);
        for (int i = 0; i < 10; i++) {
            DelayedFxScheduler.tick();
        }
        assertEquals(1, fired.get(), "must not re-fire on subsequent ticks after it has fired");
    }

    @Test
    void independentDelaysFireAtTheirOwnTick() {
        AtomicInteger firedAt1 = new AtomicInteger();
        AtomicInteger firedAt3 = new AtomicInteger();
        DelayedFxScheduler.schedule(countingOp(firedAt1), null, new CompoundTag(), 1);
        DelayedFxScheduler.schedule(countingOp(firedAt3), null, new CompoundTag(), 3);

        DelayedFxScheduler.tick();
        assertEquals(1, firedAt1.get());
        assertEquals(0, firedAt3.get());

        DelayedFxScheduler.tick();
        DelayedFxScheduler.tick();
        assertEquals(1, firedAt1.get());
        assertEquals(1, firedAt3.get());
    }

    @Test
    void paramsAreForwardedUnchangedToTheDelayedOp() {
        CompoundTag params = new CompoundTag();
        params.putString("id", "minecraft:cloud");
        CompoundTag[] received = new CompoundTag[1];
        FxOp op = (ctx, p) -> received[0] = p;

        DelayedFxScheduler.schedule(op, null, params, 2);
        DelayedFxScheduler.tick();
        DelayedFxScheduler.tick();

        assertNotNull(received[0]);
        assertEquals("minecraft:cloud", received[0].getString("id"));
    }

    @Test
    void tickWithEmptyQueueDoesNothing() {
        assertEquals(0, DelayedFxScheduler.pendingCount());
        assertDoesNotThrow(DelayedFxScheduler::tick);
    }

    @Test
    void clearForTestingDropsPendingEntries() {
        DelayedFxScheduler.schedule(countingOp(new AtomicInteger()), null, new CompoundTag(), 5);
        assertEquals(1, DelayedFxScheduler.pendingCount());
        DelayedFxScheduler.clearForTesting();
        assertEquals(0, DelayedFxScheduler.pendingCount());
    }

    /** 引擎审计修复 任务A / A5 (表现引擎全面审计报告_2026-07-15.md A5): {@code clear()} is the
     * production entry point called on logout/world-unload (see {@code CareerClientEvents}) --
     * verifies it drops pending entries exactly like {@code clearForTesting()} (which now delegates
     * to it), so a delayed fx scheduled just before leaving a world never fires against the stale
     * level afterward. */
    @Test
    void clear_dropsPendingEntriesJustLikeClearForTesting() {
        DelayedFxScheduler.schedule(countingOp(new AtomicInteger()), null, new CompoundTag(), 5);
        assertEquals(1, DelayedFxScheduler.pendingCount());
        DelayedFxScheduler.clear();
        assertEquals(0, DelayedFxScheduler.pendingCount());
    }
}
