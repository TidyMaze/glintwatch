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

    /** The V5.1c reader that rebuilds rivals' scores and streaks (DESIGN V5.8). */
    public Tracker tracker;

    public void parseInit(List<String> lines) {
        tracker = new Tracker(cfg);
        tracker.init(lines);
        playerCount = tracker.playerCount;
        myId = 0; // V5.1c: the reading player is always 0
        game = new Game(tracker.board.copy(), tracker.seatOf, cfg);
        unknownType = new boolean[tracker.n * tracker.n];
    }

    public void parseTurn(List<String> lines) {
        tracker.turn(lines);
        Board b = game.board;
        Arrays.fill(b.mirrorType, -1);
        Arrays.fill(b.mirrorLock, 0);
        Arrays.fill(b.pending, 0);
        Arrays.fill(b.mirrorLife, -1);
        Arrays.fill(unknownType, false);
        for (java.util.Map.Entry<Integer, int[]> en : tracker.mirrors.entrySet()) {
            int p = en.getKey(), type = en.getValue()[0], lock = en.getValue()[1], life = en.getValue()[2];
            if (life == 0) continue; // echo of a shattered mirror: its cell is empty
            unknownType[p] = type < 0;
            b.mirrorType[p] = Math.max(0, type);
            b.pending[p] = lock < 0 ? 1 : 0;
            b.mirrorLock[p] = Math.max(0, lock);
            b.mirrorLife[p] = life;
        }
        int t = tracker.turn;
        Arrays.fill(game.litBy, -1);
        for (int c = 0; c < tracker.crystalCells.size(); c++) {
            int p = tracker.crystalCells.get(c);
            game.litBy[p] = tracker.litBy[c];
            if (cfg.tides) b.phase[p] = phaseFromTide(tracker.tide[c], t);
        }
        for (int k = 0; k < playerCount; k++) {
            game.score[k] = tracker.score[k];
            game.active[k] = tracker.hands[k] != -1;
            game.mirrorsLeft[k] = Math.max(0, tracker.hands[k]);
            System.arraycopy(tracker.streak[k], 0, game.streak[k], 0, game.streak[k].length);
        }
        game.turn = t - 1;
        turns++;
    }

    /** Crystal phase from the signed tide of turn t (k > 0 awake k more turns, -k asleep k more turns). */
    int phaseFromTide(int tide, int t) {
        int m = tide > 0 ? cfg.tideAwake - tide : cfg.tideCycle + tide;
        return Math.floorMod(m - t, cfg.tideCycle);
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
