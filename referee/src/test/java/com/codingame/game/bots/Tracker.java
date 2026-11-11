package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codingame.game.engine.*;

/**
 * Reference reader of the V5.1c inputs of one player (DESIGN V5.8): rebuilds what the protocol leaves out (turn,
 * player count, every player's score and streaks, crystal cells) from the reef, the mirror lists and the hands.
 * Ids are relative: 0 is the reading player, then clockwise. The beams of turn t come from inputs t and t+1:
 * first leagues every listed mirror of t+1; with build delay the active mirrors of t+1 that were not pending at t,
 * plus the shattered-mirror echoes (life 0).
 */
public final class Tracker {
    public final Config cfg;
    public int n, e, playerCount, turn;
    /** Seat of each relative player. */
    public int[] seatOf;
    public Board board;
    public final List<Integer> crystalCells = new ArrayList<>();
    public int[] score, hands;
    public int[][] streak;
    public boolean[] active;
    /** Mirrors of the last input: cell -> {type, lock, life}; type -1 = hidden (a rival's pending mirror). */
    public Map<Integer, int[]> mirrors = new HashMap<>();
    /** Tide of each crystal in the last input (index in crystalCells). */
    public int[] tide;
    /** litBy (relative) of each crystal in the last input. */
    public int[] litBy;

    public Tracker(Config cfg) {
        this.cfg = cfg;
    }

    /** Seat of a lamp from its edge: x = 0 seat 0 (fires E), y = 0 seat 1 (S), x = n-1 seat 2 (W), y = n-1 seat 3 (N). */
    static int seatOfLamp(int x, int y, int n) {
        return x == 0 ? 0 : y == 0 ? 1 : x == n - 1 ? 2 : 3;
    }

    /** e from a lamp of a known seat (seat s lamp is R^s(0, e)). */
    static int offsetOf(int seat, int x, int y, int n) {
        return seat == 0 ? y : seat == 1 ? n - 1 - x : seat == 2 ? n - 1 - y : x;
    }

    public void init(List<String> lines) {
        String reef = lines.get(0).trim();
        n = Integer.parseInt(lines.get(1).trim());
        int[] seatByRel = {-1, -1, -1, -1};
        for (int p = 0; p < n * n; p++) {
            char ch = reef.charAt(p);
            if (ch >= 'A' && ch <= 'D') {
                int x = Geometry.x(p, n), y = Geometry.y(p, n), seat = seatOfLamp(x, y, n);
                seatByRel[ch - 'A'] = seat;
                e = offsetOf(seat, x, y, n);
            }
        }
        playerCount = 0;
        while (playerCount < 4 && seatByRel[playerCount] >= 0) playerCount++;
        seatOf = java.util.Arrays.copyOf(seatByRel, playerCount);
        board = Board.empty(n, e);
        boolean[] held = new boolean[4];
        for (int s : seatOf) held[s] = true;
        for (int s = 0; s < 4; s++)
            if (!held[s]) {
                int p = Seats.lampCell(s, n, e);
                board.kind[p] = Board.EMPTY;
                board.lampSeat[p] = -1;
            }
        for (int p = 0; p < n * n; p++) {
            char ch = reef.charAt(p);
            if (ch == '#') board.kind[p] = Board.WALL;
            else if (ch >= '1' && ch <= '3') {
                board.kind[p] = Board.CRYSTAL;
                board.value[p] = ch - '0';
                crystalCells.add(p);
            }
        }
        score = new int[playerCount];
        hands = new int[playerCount];
        active = new boolean[playerCount];
        java.util.Arrays.fill(active, true);
        streak = new int[playerCount][n * n];
        tide = new int[crystalCells.size()];
        litBy = new int[crystalCells.size()];
    }

    public void turn(List<String> lines) {
        int i = 0;
        String[] head = lines.get(i++).trim().split(" ");
        int[] newHands = new int[playerCount];
        for (int k = 0; k < playerCount; k++) newHands[k] = Integer.parseInt(head[1 + k]);
        Map<Integer, int[]> m = new HashMap<>();
        int count = Integer.parseInt(lines.get(i++).trim());
        for (int k = 0; k < count; k++) {
            String[] t = lines.get(i++).trim().split(" ");
            m.put(Geometry.idx(Integer.parseInt(t[0]), Integer.parseInt(t[1]), n),
                new int[] {Integer.parseInt(t[2]), Integer.parseInt(t[3]), Integer.parseInt(t[4])});
        }
        if (turn > 0) scorePreviousTurn(m, newHands);
        int crystals = Integer.parseInt(lines.get(i++).trim());
        for (int k = 0; k < crystals; k++) {
            String[] t = lines.get(i++).trim().split(" ");
            tide[k] = Integer.parseInt(t[0]);
            litBy[k] = Integer.parseInt(t[1]);
        }
        hands = newHands;
        mirrors = m;
        score[0] = Integer.parseInt(head[0]);
        turn++;
    }

    /** Beams of the previous turn from the previous and the new mirror lists, then F6 step 6 scoring and strikes. */
    private void scorePreviousTurn(Map<Integer, int[]> next, int[] newHands) {
        for (int p = 0; p < n * n; p++) {
            board.mirrorType[p] = -1;
            board.pending[p] = 0;
        }
        for (Map.Entry<Integer, int[]> en : next.entrySet()) {
            int p = en.getKey(), type = en.getValue()[0], lock = en.getValue()[1], life = en.getValue()[2];
            boolean reflected;
            if (!cfg.buildDelay) reflected = true;
            else {
                int[] before = mirrors.get(p);
                reflected = life == 0 || (lock >= 0 && before != null && before[1] >= 0);
            }
            if (reflected && type >= 0) board.mirrorType[p] = type;
        }
        int[] states = {Board.SEAT_EMPTY, Board.SEAT_EMPTY, Board.SEAT_EMPTY, Board.SEAT_EMPTY};
        boolean[] fired = new boolean[playerCount];
        for (int k = 0; k < playerCount; k++) {
            fired[k] = newHands[k] != -1;
            states[seatOf[k]] = fired[k] ? Board.SEAT_ACTIVE : Board.SEAT_INACTIVE;
        }
        int cells = n * n;
        boolean[][] lit = new boolean[playerCount][];
        int[] lighters = new int[cells], gain = new int[playerCount];
        for (int k = 0; k < playerCount; k++) {
            if (!fired[k]) continue;
            Beam b = BeamTracer.trace(board, seatOf[k], states);
            lit[k] = new boolean[cells];
            for (int p : b.lit) {
                lit[k][p] = true;
                lighters[p]++;
            }
            if (b.stop == Beam.Stop.STRIKE) gain[k] += cfg.strikeBonus;
        }
        for (int c = 0; c < crystalCells.size(); c++) {
            int p = crystalCells.get(c);
            if (!cfg.contested) {
                for (int k = 0; k < playerCount; k++) if (lit[k] != null && lit[k][p]) gain[k] += board.value[p];
                continue;
            }
            if (cfg.tides && tide[c] < 0) {
                for (int k = 0; k < playerCount; k++) streak[k][p] = 0;
                continue;
            }
            for (int k = 0; k < playerCount; k++) {
                boolean inL = lit[k] != null && lit[k][p];
                if (lighters[p] == 1 && inL) {
                    streak[k][p] = Math.min(streak[k][p] + 1, cfg.streakCap);
                    gain[k] += board.value[p] * streak[k][p];
                } else if (!inL) streak[k][p] = Math.max(streak[k][p] - 1, 0);
            }
        }
        for (int k = 0; k < playerCount; k++) {
            if (fired[k]) score[k] += gain[k];
            else if (active[k]) score[k] = -1;
            active[k] = fired[k];
        }
    }
}
