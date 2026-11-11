package com.codingame.game.engine;

/** Seat geometry: seat s has its lamp at R^s(0, e) firing R^s(E); zone = Chebyshev 2 around the lamp. */
public final class Seats {
    public static final int ZONE_RADIUS = 2;

    private Seats() {}

    /** Seat of each player id, by player count (2p: seats 0 and 2). */
    public static int[] seatsFor(int playerCount) {
        switch (playerCount) {
            case 2: return new int[] {0, 2};
            case 3: return new int[] {0, 1, 2};
            case 4: return new int[] {0, 1, 2, 3};
            default: throw new IllegalArgumentException("player count " + playerCount);
        }
    }

    public static int lampCell(int seat, int n, int e) {
        int p = Geometry.idx(0, e, n);
        for (int r = 0; r < seat; r++) p = Geometry.rot(p, n);
        return p;
    }

    public static Dir lampDir(int seat) {
        return Dir.values()[seat];
    }

    public static boolean onLampLine(int seat, int x, int y, int n, int e) {
        int lamp = lampCell(seat, n, e);
        return seat % 2 == 0 ? y == Geometry.y(lamp, n) : x == Geometry.x(lamp, n);
    }

    public static boolean inZone(int seat, int x, int y, int n, int e) {
        int lamp = lampCell(seat, n, e);
        return Geometry.inside(x, y, n)
            && Math.max(Math.abs(x - Geometry.x(lamp, n)), Math.abs(y - Geometry.y(lamp, n))) <= ZONE_RADIUS;
    }

    /** Seat whose zone holds (x, y), or -1. Zones never overlap (e >= 2, lamps on different edges). */
    public static int zoneSeat(int x, int y, int n, int e) {
        for (int s = 0; s < 4; s++) if (inZone(s, x, y, n, e)) return s;
        return -1;
    }
}
