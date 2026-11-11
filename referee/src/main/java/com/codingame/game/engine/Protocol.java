package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.List;

/** Player input lines, DESIGN.md sections 5.1 and 5.2. */
public final class Protocol {
    private Protocol() {}

    public static List<String> initLines(Board b, int playerCount, int myId) {
        return initLines(b, playerCount, myId, null);
    }

    /** F5 init; phases are shown only when cfg has tides (else -1). */
    public static List<String> initLines(Board b, int playerCount, int myId, Config cfg) {
        int[] seats = Seats.seatsFor(playerCount);
        int[] playerOfSeat = {-1, -1, -1, -1};
        for (int id = 0; id < playerCount; id++) playerOfSeat[seats[id]] = id;
        List<String> lines = new ArrayList<>();
        lines.add(playerCount + " " + myId);
        lines.add(String.valueOf(b.n));
        for (int id = 0; id < playerCount; id++) {
            int lamp = Seats.lampCell(seats[id], b.n, b.e);
            lines.add(id + " " + Geometry.x(lamp, b.n) + " " + Geometry.y(lamp, b.n) + " " + Seats.lampDir(seats[id]));
        }
        for (int y = 0; y < b.n; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < b.n; x++) row.append(cellChar(b, Geometry.idx(x, y, b.n), playerOfSeat));
            lines.add(row.toString());
        }
        List<String> crystals = new ArrayList<>();
        for (int p = 0; p < b.n * b.n; p++)
            if (b.kind[p] == Board.CRYSTAL)
                crystals.add(Geometry.x(p, b.n) + " " + Geometry.y(p, b.n) + " " + b.value[p] + " "
                    + (cfg != null && cfg.tides ? b.phase[p] : -1));
        lines.add(String.valueOf(crystals.size()));
        lines.addAll(crystals);
        return lines;
    }

    private static char cellChar(Board b, int p, int[] playerOfSeat) {
        switch (b.kind[p]) {
            case Board.WALL: return '#';
            case Board.CRYSTAL: return (char) ('0' + b.value[p]);
            case Board.LAMP:
                int id = playerOfSeat[b.lampSeat[p]];
                return id < 0 ? '.' : (char) ('A' + id);
            default: return '.';
        }
    }

    /** F5 per-turn input for one viewer: turn, players, mirrors and crystals sorted by (y, x). */
    public static List<String> turnLines(Game g, int viewer) {
        Board b = g.board;
        List<String> lines = new ArrayList<>();
        lines.add(String.valueOf(g.currentTurn()));
        for (int id = 0; id < g.playerCount; id++)
            lines.add(g.score[id] + " " + g.mirrorsLeft[id] + " " + (g.active[id] ? 1 : 0));
        List<String> mirrors = new ArrayList<>(), crystals = new ArrayList<>();
        for (int p = 0; p < b.n * b.n; p++) {
            String xy = Geometry.x(p, b.n) + " " + Geometry.y(p, b.n);
            if (b.mirrorType[p] >= 0) {
                boolean hidden = g.cfg.sealed && b.pending[p] > 0 && b.lastToucher[p] != viewer;
                mirrors.add(xy + " " + (hidden ? -1 : b.mirrorType[p]) + " " + b.mirrorLock[p] + " " + b.pending[p] + " "
                    + b.mirrorLife[p]);
            }
            if (b.kind[p] == Board.CRYSTAL) {
                StringBuilder sb = new StringBuilder(xy).append(' ').append(g.awake(p, g.currentTurn()) ? 1 : 0)
                    .append(' ').append(g.litBy[p]);
                for (int id = 0; id < g.playerCount; id++) sb.append(' ').append(g.streak[id][p]);
                crystals.add(sb.toString());
            }
        }
        lines.add(String.valueOf(mirrors.size()));
        lines.addAll(mirrors);
        lines.add(String.valueOf(crystals.size()));
        lines.addAll(crystals);
        return lines;
    }
}
