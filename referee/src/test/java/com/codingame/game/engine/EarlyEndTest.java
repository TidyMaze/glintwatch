package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** V6: the only early end is stale (STALE_TURNS = 20); the decided end is gone. */
class EarlyEndTest {
    private static Board example() {
        Board b = Board.empty(13, 3);
        b.setCrystal(5, 6, 2);
        b.setCrystal(8, 6, 1);
        b.setCrystal(6, 6, 3);
        b.setWall(5, 10);
        return b;
    }

    private static Command c(String s) throws Exception {
        return CommandParser.parse(s);
    }

    @Test
    void noDecidedEndEvenWithAHugeLead() throws Exception {
        for (int league = 1; league <= 4; league++) {
            Game g = new Game(example(), 2, Config.forLeague(league));
            g.score[0] = 100000;
            g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
            assertFalse(g.isOver(), "league " + league + ": no decided end in V6");
            assertNull(g.endReason());
        }
    }

    @Test
    void staleAfterTwentyIdleTurnsFastForwardsTheRestWithWaits() throws Exception {
        Game g = Game.fromSeed(3L, 2, Config.woodTwo());
        int played = 0;
        while (!g.isOver()) {
            g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
            played++;
        }
        assertEquals(20, played, "inputs stop after turn 20");
        assertEquals(150, g.turn, "the remaining turns are played by the referee (V6: 150 in every league)");
        assertEquals("stale", g.endReason());
    }

    @Test
    void fastForwardedScoresEqualAFullGameOfWaits() throws Exception {
        Config full = Config.bronze();
        full.earlyEnd = false; // both early ends off: the reference plays all turns
        Game a = new Game(example(), 2, full), b = new Game(example(), 2, Config.bronze());
        for (Game g : new Game[] {a, b}) g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT")});
        while (!a.isOver()) a.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        int played = 1;
        while (!b.isOver()) {
            b.playTurn(new Command[] {c("WAIT"), c("WAIT")});
            played++;
        }
        assertEquals(21, played, "idle from turn 2: stale after turn 21");
        assertEquals("stale", b.endReason());
        assertArrayEquals(a.score, b.score);
        assertEquals(a.turn, b.turn);
        assertEquals("max turns", a.endReason());
    }

    @Test
    void impossibleActionsCountAsIdleAndARealActionResetsTheCount() throws Exception {
        Game g = new Game(example(), 2, Config.woodOne());
        for (int t = 1; t <= 15; t++) g.playTurn(new Command[] {c("PLACE 5 6 0"), c("WAIT")}); // crystal cell: refused
        g.playTurn(new Command[] {c("WAIT"), c("PLACE 9 6 0")});
        for (int t = 17; t <= 35; t++) g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        assertFalse(g.isOver(), "player 1 acted on turn 16");
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        assertTrue(g.isOver(), "turns 17-36 idle for both");
        assertEquals("stale", g.endReason());
    }

    @Test
    void endReasonsForMaxTurnsAndElimination() throws Exception {
        Config cfg = Config.woodOne();
        cfg.maxTurns = 2;
        Game g = new Game(example(), 2, cfg);
        assertNull(g.endReason());
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("PLACE 9 6 0")});
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        assertEquals("max turns", g.endReason());
        Game h = new Game(example(), 2, Config.woodOne());
        h.deactivate(1);
        assertEquals("fewer than two players", h.endReason());
    }
}
