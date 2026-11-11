package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** DESIGN V5.5: decided and stale early ends (STALE_TURNS = 20). */
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
    void bestTurnBoundPerLeague() {
        Game w2 = new Game(example(), 2, Config.woodTwo()), w1 = new Game(example(), 2, Config.woodOne()),
            si = new Game(example(), 2, Config.silver());
        assertEquals(6, w2.maxGainPerTurn());
        assertEquals(24, w1.maxGainPerTurn());
        assertEquals(26, si.maxGainPerTurn());
    }

    @Test
    void decidedWhenNoPairCanChangeOrderInTheTurnsLeft() throws Exception {
        Config cfg = Config.woodOne();
        cfg.maxTurns = 10;
        Game g = new Game(example(), 2, cfg);
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT")}); // P0 holds (5,6): 2, 4, 6, 8, 8 ...
        while (!g.isOver()) g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        assertEquals(8, g.turn, "after turn 8: 52 > 2 turns x 24");
        assertEquals("decided", g.endReason());
        assertEquals(52, g.score[0]);
    }

    @Test
    void notDecidedWhileAPairCanStillSwap() throws Exception {
        Config cfg = Config.woodOne();
        cfg.maxTurns = 10;
        Game g = new Game(example(), 2, cfg);
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT")});
        for (int t = 2; t <= 7; t++) g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        assertFalse(g.isOver(), "after turn 7: 44 <= 3 x 24");
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
        assertEquals(100, g.turn, "the remaining turns are played by the referee");
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
