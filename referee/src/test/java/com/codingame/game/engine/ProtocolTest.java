package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProtocolTest {
    private static Board example() {
        Board b = Board.empty(13, 3);
        b.setCrystal(5, 6, 2);
        b.setWall(5, 10);
        return b;
    }

    @Test
    void initLinesEndWithTheCrystalListAndPhaseOnlyWithTides() {
        Board b = example();
        b.phase[Geometry.idx(5, 6, 13)] = 17;
        List<String> silver = Protocol.initLines(b, 4, 0, Config.silver());
        assertEquals(2 + 4 + 13 + 1 + 1, silver.size());
        assertEquals("1", silver.get(19));
        assertEquals("5 6 2 17", silver.get(20));
        assertEquals("5 6 2 -1", Protocol.initLines(b, 4, 0, Config.bronze()).get(20));
    }

    @Test
    void initLinesFourPlayers() {
        List<String> lines = Protocol.initLines(example(), 4, 2).subList(0, 19);
        assertEquals(2 + 4 + 13, lines.size());
        assertEquals("4 2", lines.get(0));
        assertEquals("13", lines.get(1));
        assertEquals("0 0 3 E", lines.get(2));
        assertEquals("1 9 0 S", lines.get(3));
        assertEquals("2 12 9 W", lines.get(4));
        assertEquals("3 3 12 N", lines.get(5));
        assertEquals(".........B...", lines.get(6));
        assertEquals("A............", lines.get(6 + 3));
        assertEquals(".....2.......", lines.get(6 + 6));
        assertEquals("............C", lines.get(6 + 9));
        assertEquals(".....#.......", lines.get(6 + 10));
        assertEquals("...D.........", lines.get(6 + 12));
    }

    @Test
    void initLinesTwoPlayersUseSeatsZeroAndTwoAndHideEmptySeats() {
        List<String> lines = Protocol.initLines(example(), 2, 1);
        assertEquals(2 + 2 + 13 + 2, lines.size());
        assertEquals("2 1", lines.get(0));
        assertEquals("0 0 3 E", lines.get(2));
        assertEquals("1 12 9 W", lines.get(3));
        assertEquals(".............", lines.get(4));
        assertEquals("............B", lines.get(4 + 9));
        assertEquals(".............", lines.get(4 + 12));
    }

    @Test
    void initLinesThreePlayersEmptySeatThree() {
        List<String> lines = Protocol.initLines(example(), 3, 0);
        assertEquals("2 12 9 W", lines.get(4));
        assertEquals(".............", lines.get(5 + 12));
    }

    @Test
    void mapRowsNeverShowMirrors() {
        Board b = example();
        b.setMirror(5, 3, 1);
        assertEquals("A............", Protocol.initLines(b, 4, 0).get(6 + 3));
    }

    @Test
    void turnLinesF5TurnPlayersMirrorsThenCrystalsWithEveryStreak() throws Exception {
        Config c = Config.woodOne();
        Game g = new Game(example(), 2, c);
        g.playTurn(new Command[] {CommandParser.parse("PLACE 5 3 1"), CommandParser.parse("WAIT")});
        g.board.setMirror(2, 4, 1);
        g.board.setMirror(9, 1, 0);
        java.util.List<String> lines = Protocol.turnLines(g, 1);
        assertEquals("2", lines.get(0));
        assertEquals("2 11 1", lines.get(1));
        assertEquals("0 12 1", lines.get(2));
        assertEquals("3", lines.get(3));
        assertEquals("9 1 0 0 0 -1", lines.get(4));
        assertEquals("5 3 1 5 0 -1", lines.get(5));
        assertEquals("2 4 1 0 0 -1", lines.get(6));
        assertEquals("1", lines.get(7));
        assertEquals("5 6 1 0 1 0", lines.get(8));
        assertEquals(9, lines.size());
    }

    @Test
    void dormantCrystalShowsAwakeZero() {
        Board b = example();
        b.phase[Geometry.idx(5, 6, 13)] = 20; // turn 1: (1+20)%45 = 21 < 30 awake; turn 10: 30 dormant
        Game g = new Game(b, 2, Config.silver());
        assertEquals("5 6 1 -1 0 0", Protocol.turnLines(g, 0).get(5));
        for (int t = 1; t < 10; t++) g.playTurn(new Command[2]);
        assertEquals("5 6 0 -1 0 0", Protocol.turnLines(g, 0).get(5));
    }
}
