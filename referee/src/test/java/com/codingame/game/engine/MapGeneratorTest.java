package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MapGeneratorTest {
    private static final int SEEDS = 200;

    @Test
    void sizeAndLampOffsetInRange() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            assertTrue(b.n >= 13 && b.n <= 19 && b.n % 2 == 1);
            assertTrue(b.e >= 2 && b.e <= (b.n - 1) / 2 - 2);
        }
    }

    @Test
    void sameSeedSameMap() {
        for (long s = 0; s < 20; s++)
            assertEquals(MapGenerator.generate(s, false).describe(), MapGenerator.generate(s, false).describe());
    }

    @Test
    void mapHasFourWayRotationalSymmetryWithMirrorTypeSwapped() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            for (int p = 0; p < b.n * b.n; p++) {
                int q = Geometry.rot(p, b.n);
                assertEquals(b.kind[p], b.kind[q], "seed " + s);
                assertEquals(b.value[p], b.value[q]);
                if (b.kind[p] == Board.LAMP) assertEquals((b.lampSeat[p] + 1) % 4, b.lampSeat[q]);
                if (b.mirrorType[p] < 0) assertEquals(-1, b.mirrorType[q]);
                else assertEquals(1 - b.mirrorType[p], b.mirrorType[q]);
            }
        }
    }

    @Test
    void lampLinesAndZonesHoldNoObjectsAndLampsSitOnTheirCells() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            for (int seat = 0; seat < 4; seat++) {
                int lamp = Seats.lampCell(seat, b.n, b.e);
                assertEquals(Board.LAMP, b.kind[lamp]);
                assertEquals(seat, b.lampSeat[lamp]);
            }
            for (int x = 0; x < b.n; x++)
                for (int y = 0; y < b.n; y++) {
                    int p = Geometry.idx(x, y, b.n);
                    boolean reserved = Seats.zoneSeat(x, y, b.n, b.e) >= 0;
                    for (int seat = 0; seat < 4; seat++) reserved |= Seats.onLampLine(seat, x, y, b.n, b.e);
                    if (reserved && b.kind[p] != Board.LAMP) {
                        assertEquals(Board.EMPTY, b.kind[p], "seed " + s + " " + x + "," + y);
                        assertEquals(-1, b.mirrorType[p]);
                    }
                }
        }
    }

    @Test
    void crystalPhasesAreSymmetricInRangeAndCentreIsZero() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            int c = (b.n - 1) / 2;
            assertEquals(0, b.phase[Geometry.idx(c, c, b.n)]);
            for (int p = 0; p < b.n * b.n; p++) {
                if (b.kind[p] != Board.CRYSTAL) continue;
                assertTrue(b.phase[p] >= 0 && b.phase[p] < 45);
                assertEquals(b.phase[p], b.phase[Geometry.rot(p, b.n)]);
            }
        }
    }

    @Test
    void phasesDoNotChangeTheLayout() {
        for (long s = 0; s < 50; s++)
            assertEquals(MapGenerator.generate(s, true, true).describe(), MapGenerator.generate(s, true, true).describe());
    }

    @Test
    void centreIsAValueThreeCrystal() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            int c = (b.n - 1) / 2;
            assertEquals(Board.CRYSTAL, b.kind[Geometry.idx(c, c, b.n)]);
            assertEquals(3, b.value[Geometry.idx(c, c, b.n)]);
        }
    }

    @Test
    void candidateCountsMatchTheDesignTable() {
        int[][] range = {{12, 17}, {22, 27}, {34, 39}, {48, 53}};
        for (int n = 13; n <= 19; n += 2) {
            int c = (n - 1) / 2;
            for (int e = 2; e <= c - 2; e++) {
                int a = MapGenerator.candidates(n, e).size();
                int[] r = range[(n - 13) / 2];
                assertTrue(a >= r[0] && a <= r[1], "n=" + n + " e=" + e + " A=" + a);
            }
        }
    }

    @Test
    void objectCountsFollowTheFormulas() {
        for (long s = 0; s < SEEDS; s++) {
            Board b = MapGenerator.generate(s, false);
            int a = MapGenerator.candidates(b.n, b.e).size();
            int crystals = 0, walls = 0, mirrors = 0;
            for (int p = 0; p < b.n * b.n; p++) {
                if (b.kind[p] == Board.CRYSTAL) {
                    crystals++;
                    assertTrue(b.value[p] >= 1 && b.value[p] <= 3);
                }
                if (b.kind[p] == Board.WALL) walls++;
                if (b.mirrorType[p] >= 0) {
                    mirrors++;
                    assertEquals(0, b.mirrorLock[p]);
                    assertEquals(-1, b.lastToucher[p]);
                }
            }
            int k = (crystals - 1) / 4;
            assertEquals(0, (crystals - 1) % 4);
            assertTrue(k >= Math.min(a, a / 4) && k <= a / 4 + 2);
            assertTrue(walls % 4 == 0 && walls / 4 <= a / 10 + 1);
            assertTrue(mirrors % 4 == 0 && mirrors / 4 <= 4);
            assertTrue(k + walls / 4 + mirrors / 4 <= a);
        }
    }

    @Test
    void plainMapKeepsCrystalLayoutWithValueOneAndNoWallsOrMirrors() {
        for (long s = 0; s < SEEDS; s++) {
            Board full = MapGenerator.generate(s, false), plain = MapGenerator.generate(s, true);
            assertEquals(full.n, plain.n);
            assertEquals(full.e, plain.e);
            for (int p = 0; p < full.n * full.n; p++) {
                assertNotEquals(Board.WALL, plain.kind[p]);
                assertEquals(-1, plain.mirrorType[p]);
                assertEquals(full.kind[p] == Board.CRYSTAL, plain.kind[p] == Board.CRYSTAL);
                if (plain.kind[p] == Board.CRYSTAL) assertEquals(1, plain.value[p]);
            }
        }
    }

    @Test
    void seedMixIsSplitMix64() {
        assertEquals(0xE220A8397B1DCDAFL, MapGenerator.mix(0L));
        assertNotEquals(MapGenerator.mix(1L), MapGenerator.mix(2L));
    }

    @Test
    void consecutiveSmallSeedsSpreadOverAllBoardSizes() {
        int[] count = new int[4];
        for (long s = 0; s < SEEDS; s++) count[(MapGenerator.generate(s, false).n - 13) / 2]++;
        for (int c : count) assertTrue(c >= 30 && c <= 70, java.util.Arrays.toString(count));
    }

    @Test
    void someSeedsGiveEachBoardSize() {
        boolean[] seen = new boolean[4];
        for (long s = 0; s < SEEDS; s++) seen[(MapGenerator.generate(s, false).n - 13) / 2] = true;
        for (boolean b : seen) assertTrue(b);
    }
}
