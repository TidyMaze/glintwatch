package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeometryTest {
    @Test
    void rotationMapsXyToNMinusOneMinusYX() {
        assertEquals(Geometry.idx(12, 0, 13), Geometry.rot(Geometry.idx(0, 0, 13), 13));
        assertEquals(Geometry.idx(9, 0, 13), Geometry.rot(Geometry.idx(0, 3, 13), 13));
    }

    @Test
    void fourRotationsAreIdentity() {
        for (int n = 13; n <= 19; n += 2)
            for (int i = 0; i < n * n; i++)
                assertEquals(i, Geometry.rot(Geometry.rot(Geometry.rot(Geometry.rot(i, n), n), n), n));
    }

    @Test
    void quarterAndItsRotationsPlusCentrePartitionTheBoard() {
        for (int n = 13; n <= 19; n += 2) {
            int c = (n - 1) / 2;
            Set<Integer> seen = new HashSet<>();
            seen.add(Geometry.idx(c, c, n));
            for (int x = 0; x <= c - 1; x++)
                for (int y = 0; y <= c; y++) {
                    int p = Geometry.idx(x, y, n);
                    for (int r = 0; r < 4; r++) {
                        assertTrue(seen.add(p), "overlap n=" + n);
                        p = Geometry.rot(p, n);
                    }
                }
            assertEquals(n * n, seen.size());
        }
    }

    @Test
    void directionsRotateClockwiseEastSouthWestNorth() {
        assertEquals(Dir.S, Dir.E.rotate());
        assertEquals(Dir.W, Dir.S.rotate());
        assertEquals(Dir.N, Dir.W.rotate());
        assertEquals(Dir.E, Dir.N.rotate());
        for (Dir d : Dir.values()) {
            assertEquals(-d.dy, d.rotate().dx);
            assertEquals(d.dx, d.rotate().dy);
        }
    }
}
