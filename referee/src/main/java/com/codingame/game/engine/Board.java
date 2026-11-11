package com.codingame.game.engine;

import java.util.Arrays;

/** Board state: static cells (empty, wall, crystal, lamp of a seat) plus mirrors with lock and last toucher. */
public final class Board {
    public static final int EMPTY = 0, WALL = 1, CRYSTAL = 2, LAMP = 3;
    public static final int SEAT_ACTIVE = 0, SEAT_INACTIVE = 1, SEAT_EMPTY = 2;

    public final int n, e;
    /** pending[p] > 0: the mirror is built but does not reflect yet. */
    public final int[] kind, value, lampSeat, mirrorType, mirrorLock, lastToucher, pending;
    /** mirrorLife: active turns left, -1 = no wear. phase: crystal tide phase 0..44. */
    public final int[] mirrorLife, phase;

    private Board(int n, int e) {
        this.n = n;
        this.e = e;
        int size = n * n;
        kind = new int[size];
        value = new int[size];
        lampSeat = filled(size, -1);
        mirrorType = filled(size, -1);
        mirrorLock = new int[size];
        lastToucher = filled(size, -1);
        pending = new int[size];
        mirrorLife = filled(size, -1);
        phase = new int[size];
    }

    private static int[] filled(int size, int v) {
        int[] a = new int[size];
        Arrays.fill(a, v);
        return a;
    }

    /** Empty board with the 4 seat lamps on their cells. */
    public static Board empty(int n, int e) {
        Board b = new Board(n, e);
        for (int s = 0; s < 4; s++) {
            int p = Seats.lampCell(s, n, e);
            b.kind[p] = LAMP;
            b.lampSeat[p] = s;
        }
        return b;
    }

    public Board copy() {
        Board b = new Board(n, e);
        System.arraycopy(kind, 0, b.kind, 0, kind.length);
        System.arraycopy(value, 0, b.value, 0, value.length);
        System.arraycopy(lampSeat, 0, b.lampSeat, 0, lampSeat.length);
        System.arraycopy(mirrorType, 0, b.mirrorType, 0, mirrorType.length);
        System.arraycopy(mirrorLock, 0, b.mirrorLock, 0, mirrorLock.length);
        System.arraycopy(lastToucher, 0, b.lastToucher, 0, lastToucher.length);
        System.arraycopy(pending, 0, b.pending, 0, pending.length);
        System.arraycopy(mirrorLife, 0, b.mirrorLife, 0, mirrorLife.length);
        System.arraycopy(phase, 0, b.phase, 0, phase.length);
        return b;
    }

    public void setWall(int x, int y) {
        kind[Geometry.idx(x, y, n)] = WALL;
    }

    public void setCrystal(int x, int y, int v) {
        int p = Geometry.idx(x, y, n);
        kind[p] = CRYSTAL;
        value[p] = v;
    }

    public void setMirror(int x, int y, int type) {
        mirrorType[Geometry.idx(x, y, n)] = type;
    }

    public void removeMirror(int p) {
        mirrorType[p] = -1;
        mirrorLock[p] = 0;
        pending[p] = 0;
        mirrorLife[p] = -1;
        lastToucher[p] = -1;
    }

    /** Debug text: one row per line, mirrors as / and \. */
    public String describe() {
        StringBuilder sb = new StringBuilder().append(n).append(' ').append(e).append('\n');
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int p = Geometry.idx(x, y, n);
                if (mirrorType[p] >= 0) sb.append(mirrorType[p] == 0 ? '/' : '\\');
                else if (kind[p] == WALL) sb.append('#');
                else if (kind[p] == CRYSTAL) sb.append((char) ('0' + value[p]));
                else if (kind[p] == LAMP) sb.append((char) ('a' + lampSeat[p]));
                else sb.append('.');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
