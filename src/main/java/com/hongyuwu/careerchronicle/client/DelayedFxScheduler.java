package com.hongyuwu.careerchronicle.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;

/**
 * Lets an individual fx component in a skill's {@code fx} array declare an optional
 * {@code delay_ticks} param so it fires N client ticks after the rest of that phase's
 * components (which otherwise all play in the same tick {@link FxClientDispatch} receives
 * the packet, per 0.4-06 D4's "pure cosmetic, decoupled from mechanics" decision -- the
 * animation itself plays out over several ticks after cast, so a same-tick particle burst
 * reads as disconnected from e.g. a stomp's actual foot-impact frame). Ticked from
 * {@code CareerClientEvents.onClientTick} alongside {@link HitstopManager}/{@link CameraPunchManager}.
 */
final class DelayedFxScheduler {
    private record Pending(FxOp op, FxPlayContext ctx, CompoundTag params, int ticksRemaining) {
        Pending decremented() {
            return new Pending(op, ctx, params, ticksRemaining - 1);
        }
    }

    private static final Deque<Pending> QUEUE = new ArrayDeque<>();

    private DelayedFxScheduler() {
    }

    static void schedule(FxOp op, FxPlayContext ctx, CompoundTag params, int delayTicks) {
        if (delayTicks <= 0) {
            op.play(ctx, params);
            return;
        }
        QUEUE.add(new Pending(op, ctx, params, delayTicks));
    }

    static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        int size = QUEUE.size();
        for (int i = 0; i < size; i++) {
            Pending pending = QUEUE.poll();
            if (pending.ticksRemaining() <= 1) {
                pending.op().play(pending.ctx(), pending.params());
            } else {
                QUEUE.add(pending.decremented());
            }
        }
    }

    static int pendingCount() {
        return QUEUE.size();
    }

    /** 引擎审计修复 任务A / A5 (表现引擎全面审计报告_2026-07-15.md A5): production entry point,
     * called on logout/world-unload (see {@code CareerClientEvents}) so a pending delayed fx --
     * whose captured {@link FxPlayContext} holds a reference to the {@code ClientLevel} being left
     * -- never executes against a stale level after the player has already logged out or the world
     * has already unloaded. */
    static void clear() {
        QUEUE.clear();
    }

    static void clearForTesting() {
        clear();
    }
}
