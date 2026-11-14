package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.List;

/** Player input lines, DESIGN.md sections 5.1 and 5.2. */
public final class Protocol {
    private Protocol() {}

    // ---- V5.1c (DESIGN V5.8): the viewer is player 0, the others follow in clockwise seat order ----

    /** Real player ids in relative order for this viewer: index = relative id. */
    public static int[] relativeOrder(Game g, int viewer) {
        int[] order = new int[g.playerCount];
        int k = 0;
        for (int step = 0; step < 4; step++) {
            int seat = (g.seatOf[viewer] + step) % 4;
            for (int id = 0; id < g.playerCount; id++) if (g.seatOf[id] == seat) order[k++] = id;
        }
        return order;
    }

    static int relativeId(int[] order, int real) {
        for (int k = 0; k < order.length; k++) if (order[k] == real) return k;
        return -1;
    }

    /** Init: the reef (size*size chars, row by row; lamp letters by relative id), then the size. */
    public static List<String> initLinesV5(Game g, int viewer) {
        Board b = g.board;
        int[] order = relativeOrder(g, viewer);
        int[] relOfSeat = {-1, -1, -1, -1};
        for (int k = 0; k < order.length; k++) relOfSeat[g.seatOf[order[k]]] = k;
        StringBuilder reef = new StringBuilder();
        for (int p = 0; p < b.n * b.n; p++) {
            switch (b.kind[p]) {
                case Board.WALL: reef.append('#'); break;
                case Board.CRYSTAL: reef.append((char) ('0' + b.value[p])); break;
                case Board.LAMP:
                    int rel = relOfSeat[b.lampSeat[p]];
                    reef.append(rel < 0 ? '.' : (char) ('A' + rel));
                    break;
                default: reef.append('.');
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(reef.toString());
        lines.add(String.valueOf(b.n));
        return lines;
    }

    /** Turn (V6, the same layout in every league): myScore + 4 hands (-1 eliminated or missing); mirrors x y type lock life
     *  (lock -1 = pending, life 0 = echo of a mirror that shattered at the end of last turn); crystals tide litBy myStreak
     *  in reef order, tide always 0 (no tides since V6). */
    public static List<String> turnLinesV5(Game g, int viewer) {
        g.beginTurn(); // F6 step 0 before any input is written (idempotent): every consumer sees the supply
        Board b = g.board;
        int[] order = relativeOrder(g, viewer);
        List<String> lines = new ArrayList<>();
        StringBuilder head = new StringBuilder().append(g.score[viewer]);
        for (int k = 0; k < 4; k++) head.append(' ').append(k < order.length && g.active[order[k]] ? g.mirrorsLeft[order[k]] : -1);
        lines.add(head.toString());
        java.util.TreeMap<Integer, String> mirrors = new java.util.TreeMap<>();
        for (int p = 0; p < b.n * b.n; p++)
            if (b.mirrorType[p] >= 0) {
                boolean pending = b.pending[p] > 0;
                mirrors.put(p, Geometry.x(p, b.n) + " " + Geometry.y(p, b.n) + " " + b.mirrorType[p] + " "
                    + (pending ? -1 : b.mirrorLock[p]) + " " + b.mirrorLife[p]);
            }
        for (int[] s : g.lastShattered) mirrors.put(s[0], Geometry.x(s[0], b.n) + " " + Geometry.y(s[0], b.n) + " " + s[1] + " 0 0");
        lines.add(String.valueOf(mirrors.size()));
        lines.addAll(mirrors.values());
        List<String> crystals = new ArrayList<>();
        for (int p = 0; p < b.n * b.n; p++)
            if (b.kind[p] == Board.CRYSTAL) {
                int by = g.litBy[p];
                crystals.add("0 " + (by < 0 ? by : relativeId(order, by)) + " " + g.streak[viewer][p]);
            }
        lines.add(String.valueOf(crystals.size()));
        lines.addAll(crystals);
        return lines;
    }
}
