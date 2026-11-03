package fr.needaname.mc_rpg_mod.effect.server;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {

    @Test
    void zeroCooldownIsAlwaysReady() {
        assertTrue(Cooldowns.isReady(OptionalLong.of(1000), 0, 1000));
        assertEquals(0, Cooldowns.remaining(OptionalLong.of(1000), 0, 1000));
    }

    @Test
    void neverUsedIsReady() {
        assertTrue(Cooldowns.isReady(OptionalLong.empty(), 900, 5000));
        assertEquals(0, Cooldowns.remaining(OptionalLong.empty(), 900, 5000));
    }

    @Test
    void readyOnceEnoughTicksElapsed() {
        // dernier usage à 1000, recharge 900 ticks.
        assertFalse(Cooldowns.isReady(OptionalLong.of(1000), 900, 1899));
        assertEquals(1, Cooldowns.remaining(OptionalLong.of(1000), 900, 1899));
        assertTrue(Cooldowns.isReady(OptionalLong.of(1000), 900, 1900));
        assertEquals(0, Cooldowns.remaining(OptionalLong.of(1000), 900, 1900));
        assertTrue(Cooldowns.isReady(OptionalLong.of(1000), 900, 5000));
    }
}
