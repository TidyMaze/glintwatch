package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OpticsTest {
    @Test
    void slashMirrorTable() {
        assertEquals(Dir.N, Optics.reflect(0, Dir.E));
        assertEquals(Dir.E, Optics.reflect(0, Dir.N));
        assertEquals(Dir.S, Optics.reflect(0, Dir.W));
        assertEquals(Dir.W, Optics.reflect(0, Dir.S));
    }

    @Test
    void backslashMirrorTable() {
        assertEquals(Dir.S, Optics.reflect(1, Dir.E));
        assertEquals(Dir.E, Optics.reflect(1, Dir.S));
        assertEquals(Dir.N, Optics.reflect(1, Dir.W));
        assertEquals(Dir.W, Optics.reflect(1, Dir.N));
    }

    @Test
    void reflectionIsReversible() {
        for (int t = 0; t < 2; t++)
            for (Dir d : Dir.values())
                assertEquals(d.opposite(), Optics.reflect(t, Optics.reflect(t, d).opposite()));
    }

    @Test
    void rotatingTheBoardSwapsMirrorType() {
        for (int t = 0; t < 2; t++)
            for (Dir d : Dir.values())
                assertEquals(Optics.reflect(t, d).rotate(), Optics.reflect(1 - t, d.rotate()));
    }

    @Test
    void unknownTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Optics.reflect(2, Dir.E));
    }
}
