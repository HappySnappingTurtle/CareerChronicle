package com.hongyuwu.careerchronicle.skill;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7): the server-side per-player
 * resend debounce. {@link BasicAttackAnimationEvents#allowDispatch} is a pure
 * {@code (UUID, long) -> boolean} function (plus the side effect of recording the accepted call)
 * so it's unit-testable without a real {@code ServerPlayer}/{@code Level} (same split rationale
 * as {@code AnimFxOp.applyAnimation}).
 */
class BasicAttackAnimationEventsTest {

    @AfterEach
    void reset() {
        BasicAttackAnimationEvents.clearForTesting();
    }

    @Test
    void firstCallForPlayer_alwaysAllowed() {
        assertTrue(BasicAttackAnimationEvents.allowDispatch(UUID.randomUUID(), 0L));
    }

    @Test
    void secondCallWithinInterval_rejected() {
        UUID playerId = UUID.randomUUID();
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerId, 100L));
        assertFalse(BasicAttackAnimationEvents.allowDispatch(playerId, 103L), "3 ticks later, well within the debounce window, must be rejected");
    }

    @Test
    void callAfterIntervalElapsed_allowed() {
        UUID playerId = UUID.randomUUID();
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerId, 100L));
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerId, 200L), "100 ticks later must clear the debounce window");
    }

    @Test
    void independentPlayers_debouncedSeparately() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerA, 100L));
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerB, 100L), "a different player's own first call must not be affected by playerA's debounce");
    }

    @Test
    void rapidSpam_onlyFirstOfBurstAccepted() {
        UUID playerId = UUID.randomUUID();
        int accepted = 0;
        for (long t = 0; t < 20; t++) {
            if (BasicAttackAnimationEvents.allowDispatch(playerId, t)) {
                accepted++;
            }
        }
        assertTrue(accepted >= 2 && accepted <= 3, "20 ticks at an 8-tick floor should accept ~2-3 calls, got " + accepted);
    }

    @Test
    void clearForTesting_resetsDebounceState() {
        UUID playerId = UUID.randomUUID();
        BasicAttackAnimationEvents.allowDispatch(playerId, 100L);
        BasicAttackAnimationEvents.clearForTesting();
        assertTrue(BasicAttackAnimationEvents.allowDispatch(playerId, 101L), "after clearing, even 1 tick later must be allowed again");
    }
}
