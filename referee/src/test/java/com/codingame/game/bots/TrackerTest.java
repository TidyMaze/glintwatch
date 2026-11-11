package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

/**
 * DESIGN V5.8 task 1b: from one player's V5.1c inputs only, the Tracker rebuilds the turn, the player count, every
 * player's score and streaks, and the crystals' cells, equal to the referee's state after every turn.
 * Seeds: -Dtracker.seeds (default 12 in mvn test; the hand-over run uses 200).
 */
class TrackerTest {
    private static final int SEEDS = Integer.getInteger("tracker.seeds", 12);

    @Test
    void trackerRebuildsTheDroppedFieldsForEverySeatLeagueAndPlayerCount() throws Exception {
        long checks = 0;
        for (int level = 1; level <= 4; level++)
            for (int pc = 2; pc <= 4; pc++)
                for (long seed = 0; seed < SEEDS; seed++) checks += playAndCheck(level, pc, seed);
        assertTrue(checks > 0);
        System.out.println("tracker checks " + checks + " (" + SEEDS + " seeds x 4 levels x 2/3/4 players x every seat)");
    }

    /** One game driven by seeded random actions (some invalid, one random elimination); every seat is tracked. */
    static long playAndCheck(int level, int pc, long seed) throws Exception {
        Config cfg = Config.forLeague(level);
        cfg.maxTurns = Math.min(cfg.maxTurns, 60);
        Game g = Game.fromSeed(seed, pc, cfg);
        Tracker[] tr = new Tracker[pc];
        for (int v = 0; v < pc; v++) {
            tr[v] = new Tracker(Config.forLeague(level));
            tr[v].init(Protocol.initLinesV5(g, v));
        }
        Random r = new Random(seed * 7919 + level * 31 + pc);
        int killer = r.nextInt(3) == 0 ? r.nextInt(pc) : -1, killTurn = 5 + r.nextInt(40);
        long checks = 0;
        while (!g.isOver()) {
            g.beginTurn();
            for (int v = 0; v < pc; v++) {
                if (!g.active[v]) continue;
                tr[v].turn(Protocol.turnLinesV5(g, v));
                checks += compare(g, v, tr[v], level, pc, seed);
            }
            if (g.turn + 1 == killTurn && killer >= 0 && g.active[killer]) g.deactivate(killer);
            Command[] c = new Command[pc];
            for (int i = 0; i < pc; i++) {
                int k = r.nextInt(4);
                String line = k == 0 ? "WAIT" : k == 1 ? "TURN " + r.nextInt(g.board.n) + " " + r.nextInt(g.board.n)
                    : "PLACE " + r.nextInt(g.board.n) + " " + r.nextInt(g.board.n) + " " + r.nextInt(2);
                c[i] = CommandParser.parse(line);
            }
            g.playTurn(c);
        }
        return checks;
    }

    static long compare(Game g, int viewer, Tracker t, int level, int pc, long seed) {
        String tag = "level " + level + " pc " + pc + " seed " + seed + " viewer " + viewer + " turn " + g.currentTurn();
        int[] order = Protocol.relativeOrder(g, viewer);
        assertEquals(g.currentTurn(), t.turn, tag);
        assertEquals(pc, t.playerCount, tag);
        for (int k = 0; k < pc; k++) {
            int real = order[k];
            assertEquals(g.score[real], t.score[k], tag + " score of rel " + k);
            for (int p = 0; p < g.board.n * g.board.n; p++)
                if (g.board.kind[p] == Board.CRYSTAL) assertEquals(g.streak[real][p], t.streak[k][p], tag + " streak rel " + k + " cell " + p);
        }
        int idx = 0;
        for (int p = 0; p < g.board.n * g.board.n; p++)
            if (g.board.kind[p] == Board.CRYSTAL) assertEquals(p, (int) t.crystalCells.get(idx++), tag + " crystal cell");
        return 1;
    }
}
