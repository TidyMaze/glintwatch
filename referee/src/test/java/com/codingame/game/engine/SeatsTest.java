package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SeatsTest {
    @Test
    void playerToSeatByPlayerCount() {
        assertArrayEquals(new int[] {0, 2}, Seats.seatsFor(2));
        assertArrayEquals(new int[] {0, 1, 2}, Seats.seatsFor(3));
        assertArrayEquals(new int[] {0, 1, 2, 3}, Seats.seatsFor(4));
        assertThrows(IllegalArgumentException.class, () -> Seats.seatsFor(1));
        assertThrows(IllegalArgumentException.class, () -> Seats.seatsFor(5));
    }

    @Test
    void lampCellsAndDirectionsMatchTheSeatTable() {
        int n = 13, e = 3;
        assertEquals(Geometry.idx(0, 3, n), Seats.lampCell(0, n, e));
        assertEquals(Geometry.idx(9, 0, n), Seats.lampCell(1, n, e));
        assertEquals(Geometry.idx(12, 9, n), Seats.lampCell(2, n, e));
        assertEquals(Geometry.idx(3, 12, n), Seats.lampCell(3, n, e));
        assertEquals(Dir.E, Seats.lampDir(0));
        assertEquals(Dir.S, Seats.lampDir(1));
        assertEquals(Dir.W, Seats.lampDir(2));
        assertEquals(Dir.N, Seats.lampDir(3));
    }

    @Test
    void lampLinesAreTheRowsAndColumnsOfTheLamps() {
        int n = 13, e = 3;
        assertTrue(Seats.onLampLine(0, 7, 3, n, e));
        assertTrue(Seats.onLampLine(1, 9, 7, n, e));
        assertTrue(Seats.onLampLine(2, 7, 9, n, e));
        assertTrue(Seats.onLampLine(3, 3, 7, n, e));
        assertFalse(Seats.onLampLine(0, 7, 4, n, e));
    }

    @Test
    void zoneIsChebyshevTwoAroundTheLampInsideTheBoard() {
        int n = 13, e = 3, count = 0;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                if (Seats.inZone(0, x, y, n, e)) {
                    count++;
                    assertTrue(x <= 2 && y >= 1 && y <= 5);
                }
        assertEquals(15, count);
        assertFalse(Seats.inZone(0, -1, 3, n, e), "outside the board");
        assertEquals(1, Seats.zoneSeat(9, 1, n, e));
        assertEquals(-1, Seats.zoneSeat(6, 6, n, e));
    }
}
