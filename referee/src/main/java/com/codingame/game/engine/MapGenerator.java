package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Seeded map generation, DESIGN.md section 3.3; the order of Random calls is part of the spec. */
public final class MapGenerator {
    private MapGenerator() {}

    /** @param plain Wood 2 map: no walls, no start mirrors, crystals worth 1 (same Random calls). */
    public static Board generate(long seed, boolean plain) {
        return generate(seed, !plain, !plain);
    }

    /** @param withWalls walls and start mirrors; @param multiValues crystal values 1-3, else all 1. */
    public static Board generate(long seed, boolean withWalls, boolean multiValues) {
        Random rnd = new Random(mix(seed));
        int n = 13 + 2 * rnd.nextInt(4);
        int c = (n - 1) / 2;
        int e = 2 + rnd.nextInt(c - 3);
        List<Integer> cand = candidates(n, e);
        Collections.shuffle(cand, rnd);
        int a = cand.size();
        int k = Math.min(a, a / 4 + rnd.nextInt(3));
        int walls = Math.min(a - k, a / 10 + rnd.nextInt(2));
        int mirrors = Math.min(a - k - walls, 2 + rnd.nextInt(3));

        Board b = Board.empty(n, e);
        for (int i = 0; i < k; i++) {
            int r = rnd.nextInt(100);
            int v = !multiValues ? 1 : r < 50 ? 1 : r < 85 ? 2 : 3;
            placeRotated(b, cand.get(i), Board.CRYSTAL, v, -1);
        }
        for (int i = k; i < k + walls; i++)
            if (withWalls) placeRotated(b, cand.get(i), Board.WALL, 0, -1);
        for (int i = k + walls; i < k + walls + mirrors; i++) {
            int type = rnd.nextInt(2);
            if (withWalls) placeRotated(b, cand.get(i), Board.EMPTY, 0, type);
        }
        for (int i = 0; i < k; i++) {
            int ph = rnd.nextInt(45);
            for (int r = 0, p = cand.get(i); r < 4; r++, p = Geometry.rot(p, n)) b.phase[p] = ph;
        }
        b.setCrystal(c, c, multiValues ? 3 : 1);
        b.phase[Geometry.idx(c, c, n)] = 0;
        return b;
    }

    /** SplitMix64 finalizer: spreads consecutive seeds (java.util.Random's first draws are near-linear in them). */
    public static long mix(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Cells of the quarter x in [0, c-1], y in [0, c], sorted by (y, x), off the border, lamp lines and zones. */
    public static List<Integer> candidates(int n, int e) {
        int c = (n - 1) / 2;
        List<Integer> out = new ArrayList<>();
        for (int y = 0; y <= c; y++)
            for (int x = 0; x <= c - 1; x++)
                if (isCandidate(x, y, n, e)) out.add(Geometry.idx(x, y, n));
        return out;
    }

    private static boolean isCandidate(int x, int y, int n, int e) {
        if (x == 0 || y == 0) return false; // the quarter never reaches x = n-1 or y = n-1
        if (Seats.zoneSeat(x, y, n, e) >= 0) return false;
        for (int s = 0; s < 4; s++) if (Seats.onLampLine(s, x, y, n, e)) return false;
        return true;
    }

    private static void placeRotated(Board b, int p, int kind, int value, int mirrorType) {
        for (int r = 0; r < 4; r++) {
            b.kind[p] = kind;
            b.value[p] = value;
            if (mirrorType >= 0) b.mirrorType[p] = r % 2 == 0 ? mirrorType : 1 - mirrorType;
            p = Geometry.rot(p, b.n);
        }
    }
}
