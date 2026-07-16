package com.hongyuwu.careerchronicle.autotest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoTestBootstrapGiveUpTest {

    // P1: 还有剩余 tick
    @Test
    void shouldGiveUp_remainingTicksPositive_false() {
        assertFalse(AutoTestBootstrap.shouldGiveUp(1));
    }

    // P2: 恰好用尽
    @Test
    void shouldGiveUp_remainingTicksZero_true() {
        assertTrue(AutoTestBootstrap.shouldGiveUp(0));
    }

    // P3: 防御性负值
    @Test
    void shouldGiveUp_remainingTicksNegative_true() {
        assertTrue(AutoTestBootstrap.shouldGiveUp(-1));
    }

    // P4: 较大剩余值
    @Test
    void shouldGiveUp_remainingTicksLarge_false() {
        assertFalse(AutoTestBootstrap.shouldGiveUp(1000));
    }
}
