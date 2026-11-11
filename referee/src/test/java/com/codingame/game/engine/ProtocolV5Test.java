package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/** DESIGN V5.1c / V5.8: relative ids, reef init, compact turn lines, shattered-mirror echo. */
class ProtocolV5Test {
    private static Board example() {
        Board b = Board.empty(13, 3);
        b.setCrystal(5, 6, 2);
        b.setWall(5, 10);
        return b;
    }

    private static Command c(String s) throws Exception {
        return CommandParser.parse(s);
    }

    @Test
    void relativeIdsStartAtTheViewerAndGoClockwiseSkippingEmptySeats() {
        Game g4 = new Game(example(), 4, Config.woodOne());
        assertArrayEquals(new int[] {0, 1, 2, 3}, Protocol.relativeOrder(g4, 0));
        assertArrayEquals(new int[] {2, 3, 0, 1}, Protocol.relativeOrder(g4, 2));
        assertArrayEquals(new int[] {3, 0, 1, 2}, Protocol.relativeOrder(g4, 3));
        Game g2 = new Game(example(), 2, Config.woodOne()); // seats 0 and 2
        assertArrayEquals(new int[] {1, 0}, Protocol.relativeOrder(g2, 1));
        Game g3 = new Game(example(), 3, Config.woodOne()); // seats 0 1 2, seat 3 empty
        assertArrayEquals(new int[] {1, 2, 0}, Protocol.relativeOrder(g3, 1));
        assertArrayEquals(new int[] {2, 0, 1}, Protocol.relativeOrder(g3, 2));
    }

    @Test
    void initIsTheReefStringWithRelativeLampLettersThenTheSize() {
        Game g = new Game(example(), 4, Config.woodOne());
        List<String> me2 = Protocol.initLinesV5(g, 2);
        assertEquals(2, me2.size());
        assertEquals("13", me2.get(1));
        String reef = me2.get(0);
        assertEquals(169, reef.length());
        assertEquals('A', reef.charAt(Geometry.idx(12, 9, 13)), "own lamp (seat 2) is A");
        assertEquals('B', reef.charAt(Geometry.idx(3, 12, 13)), "next clockwise (seat 3)");
        assertEquals('C', reef.charAt(Geometry.idx(0, 3, 13)));
        assertEquals('D', reef.charAt(Geometry.idx(9, 0, 13)));
        assertEquals('2', reef.charAt(Geometry.idx(5, 6, 13)));
        assertEquals('#', reef.charAt(Geometry.idx(5, 10, 13)));
        Game g2 = new Game(example(), 2, Config.woodOne());
        String r2 = Protocol.initLinesV5(g2, 0).get(0);
        assertEquals('.', r2.charAt(Geometry.idx(9, 0, 13)), "empty seat lamp cell");
        assertEquals('B', r2.charAt(Geometry.idx(12, 9, 13)));
    }

    @Test
    void turnLinesWoodOneGolden() throws Exception {
        Game g = new Game(example(), 2, Config.woodOne());
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT")});
        g.beginTurn();
        assertEquals(java.util.Arrays.asList("2 11 12 -1 -1", "1", "5 3 1 5 -1", "1", "0 0 1"), Protocol.turnLinesV5(g, 0));
        assertEquals(java.util.Arrays.asList("0 12 11 -1 -1", "1", "5 3 1 5 -1", "1", "0 1 0"), Protocol.turnLinesV5(g, 1));
    }

    @Test
    void pendingMirrorShowsLockMinusOneAndIsSealedForRivals() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT"), c("WAIT"), c("WAIT")});
        g.beginTurn();
        assertEquals("5 3 1 -1 20", Protocol.turnLinesV5(g, 0).get(2));
        assertEquals("5 3 -1 -1 20", Protocol.turnLinesV5(g, 1).get(2));
        assertEquals("0 3 4 4 4", Protocol.turnLinesV5(g, 0).get(0));
        assertEquals("0 4 4 4 3", Protocol.turnLinesV5(g, 1).get(0), "hands in relative order");
    }

    @Test
    void shatteredMirrorIsEchoedOnceWithLifeZeroAndItsFinalType() throws Exception {
        Config cfg = Config.bronze();
        cfg.mirrorLife = 2;
        cfg.lock = 0;
        Game g = new Game(example(), 2, cfg);
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT")}); // pending
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});        // activates, life 2
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});        // life 1
        g.playTurn(new Command[] {c("TURN 5 3"), c("WAIT")});    // turned on its last turn: type 0, then life... kept by the turn
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});        // life 0: shatters
        g.beginTurn();
        List<String> lines = Protocol.turnLinesV5(g, 1);
        assertEquals("1", lines.get(1));
        assertEquals("5 3 0 0 0", lines.get(2));
        g.playTurn(new Command[] {c("WAIT"), c("WAIT")});
        g.beginTurn();
        assertEquals("0", Protocol.turnLinesV5(g, 1).get(1), "echoed once");
    }

    @Test
    void tideIsSignedTurnsLeftAndZeroWithoutTides() {
        Board b = example();
        b.phase[Geometry.idx(5, 6, 13)] = 20; // turn 1: m = 21, awake 9 more turns (21..29)
        Game g = new Game(b, 2, Config.silver());
        g.beginTurn();
        assertEquals("9 -1 0", Protocol.turnLinesV5(g, 0).get(3));
        for (int t = 1; t < 10; t++) g.playTurn(new Command[2]);
        g.beginTurn(); // turn 10: m = 30, asleep 15 more turns
        assertEquals("-15 -1 0", Protocol.turnLinesV5(g, 0).get(3));
        Game w = new Game(example(), 2, Config.bronze());
        w.beginTurn();
        assertEquals("0 -1 0", Protocol.turnLinesV5(w, 0).get(3));
    }

    @Test
    void litByIsRelativeAndEliminatedHandIsMinusOne() throws Exception {
        Game g = new Game(example(), 4, Config.woodOne());
        g.playTurn(new Command[] {c("PLACE 5 3 1"), c("WAIT"), c("WAIT"), c("WAIT")});
        g.deactivate(1);
        g.beginTurn();
        List<String> me3 = Protocol.turnLinesV5(g, 3);
        assertEquals("0 12 11 -1 12", me3.get(0), "seat order from 3: 3, 0, 1, 2");
        assertEquals("0 1 0", me3.get(4), "lit alone by player 0 = relative 1 for viewer 3");
    }
}
