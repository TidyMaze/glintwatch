package com.codingame.game.bots;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.codingame.game.engine.*;

/** Rebuilds a Game from protocol lines, so bots only use what a real player reads. */
public final class BotState {
    public final Config cfg;
    public Game game;
    public int myId, playerCount, turns;
    /** Pending rival mirror whose type the input hides (stored as type 0 in the board). */
    public boolean[] unknownType;
    /** Cells where this bot placed a mirror. */
    public final Set<Integer> myPlaced = new HashSet<>();

    public BotState(Config cfg) {
        this.cfg = cfg;
    }

    public void parseInit(List<String> lines) {
        String[] head = lines.get(0).split(" ");
        playerCount = Integer.parseInt(head[0]);
        myId = Integer.parseInt(head[1]);
        int n = Integer.parseInt(lines.get(1).trim());
        int e = Integer.parseInt(lines.get(2).split(" ")[2]);
        Board b = Board.empty(n, e);
        for (int y = 0; y < n; y++) {
            String row = lines.get(2 + playerCount + y);
            for (int x = 0; x < n; x++) {
                char ch = row.charAt(x);
                if (ch == '#') b.setWall(x, y);
                else if (ch >= '1' && ch <= '3') b.setCrystal(x, y, ch - '0');
            }
        }
        int i = 2 + playerCount + n;
        int crystals = Integer.parseInt(lines.get(i++).trim());
        for (int k = 0; k < crystals; k++) {
            String[] t = lines.get(i++).split(" ");
            b.phase[Geometry.idx(Integer.parseInt(t[0]), Integer.parseInt(t[1]), n)] = Math.max(0, Integer.parseInt(t[3]));
        }
        game = new Game(b, playerCount, cfg);
        unknownType = new boolean[n * n];
    }

    public void parseTurn(List<String> lines) {
        Board b = game.board;
        int i = 0;
        game.turn = Integer.parseInt(lines.get(i++).trim()) - 1;
        for (int id = 0; id < playerCount; id++) {
            String[] t = lines.get(i++).split(" ");
            game.score[id] = Integer.parseInt(t[0]);
            game.mirrorsLeft[id] = Integer.parseInt(t[1]);
            game.active[id] = t[2].equals("1");
        }
        Arrays.fill(b.mirrorType, -1);
        Arrays.fill(b.mirrorLock, 0);
        Arrays.fill(b.pending, 0);
        Arrays.fill(b.mirrorLife, -1);
        Arrays.fill(unknownType, false);
        int mirrors = Integer.parseInt(lines.get(i++).trim());
        for (int k = 0; k < mirrors; k++) {
            String[] t = lines.get(i++).split(" ");
            int p = Geometry.idx(Integer.parseInt(t[0]), Integer.parseInt(t[1]), b.n);
            int type = Integer.parseInt(t[2]);
            unknownType[p] = type < 0;
            b.mirrorType[p] = Math.max(0, type);
            b.mirrorLock[p] = Integer.parseInt(t[3]);
            b.pending[p] = Integer.parseInt(t[4]);
            b.mirrorLife[p] = Integer.parseInt(t[5]);
        }
        Arrays.fill(game.litBy, -1);
        for (int[] s : game.streak) Arrays.fill(s, 0);
        int crystals = Integer.parseInt(lines.get(i++).trim());
        for (int k = 0; k < crystals; k++) {
            String[] t = lines.get(i++).split(" ");
            int p = Geometry.idx(Integer.parseInt(t[0]), Integer.parseInt(t[1]), b.n);
            game.litBy[p] = Integer.parseInt(t[3]);
            for (int id = 0; id < playerCount; id++) game.streak[id][p] = Integer.parseInt(t[4 + id]);
        }
        turns++;
    }

    /** Copy for a 1-turn simulation: own pending mirrors act now, rival pending ones stay Empty, own new mirror acts now. */
    public Game simulation() {
        Config rules = cfg.copy();
        rules.buildDelay = false;
        Game g = game.copyWith(rules);
        for (int p : myPlaced) if (g.board.mirrorType[p] >= 0 && !unknownType[p]) g.board.pending[p] = 0;
        return g;
    }
}
