package com.codingame.game.engine;

/** Cell indices (idx = y * n + x) and the 90-degree board rotation R(x, y) = (n-1-y, x). */
public final class Geometry {
    private Geometry() {}

    public static int idx(int x, int y, int n) {
        return y * n + x;
    }

    public static int x(int idx, int n) {
        return idx % n;
    }

    public static int y(int idx, int n) {
        return idx / n;
    }

    public static boolean inside(int x, int y, int n) {
        return x >= 0 && y >= 0 && x < n && y < n;
    }

    public static int rot(int idx, int n) {
        return idx(n - 1 - y(idx, n), x(idx, n), n);
    }
}
