package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameTest {
    /** DESIGN section 10 board: N=13, e=3; crystals (5,6)=2 (8,6)=1 (6,6)=3, wall (5,10). */
    private static Board example() {
        Board b = Board.empty(13, 3);
        b.setCrystal(5, 6, 2);
        b.setCrystal(8, 6, 1);
        b.setCrystal(6, 6, 3);
        b.setWall(5, 10);
        return b;
    }

    private static int at(int x, int y) {
        return Geometry.idx(x, y, 13);
    }

    private static Command place(int x, int y, int t) throws Exception {
        return CommandParser.parse("PLACE " + x + " " + y + " " + t);
    }

    private static Command turn(int x, int y) throws Exception {
        return CommandParser.parse("TURN " + x + " " + y);
    }

    private static Command waitCmd() throws Exception {
        return CommandParser.parse("WAIT");
    }

    private static Command[] cmds(Command... c) {
        return c;
    }

    /** Wood 1 rules plus strike: mirrors act at once, stock 12, no wear, no tides. */
    private static Config immediate() {
        Config c = Config.woodOne();
        c.strikeBonus = 2;
        return c;
    }

    private static Command[] waits(int n) throws Exception {
        Command[] c = new Command[n];
        for (int i = 0; i < n; i++) c[i] = waitCmd();
        return c;
    }

    @Test
    void leagueConfigsFollowF1() {
        Config w2 = Config.forLeague(1), w1 = Config.forLeague(2), br = Config.forLeague(3), si = Config.forLeague(4);
        assertFalse(w2.turnAllowed);
        assertFalse(w2.contested);
        assertFalse(w2.walls);
        assertEquals(100, w2.maxTurns);
        assertEquals(12, w2.mirrorStock);
        assertTrue(w1.turnAllowed && w1.contested && w1.walls);
        assertEquals(150, w1.maxTurns);
        assertEquals(5, w1.lock);
        assertEquals(4, w1.streakCap);
        assertFalse(w1.buildDelay);
        assertEquals(-1, w1.mirrorLife);
        assertEquals(0, w1.supplyEvery);
        assertTrue(br.buildDelay && br.sealed);
        assertEquals(4, br.mirrorStock);
        assertEquals(4, br.supplyEvery);
        assertEquals(6, br.handMax);
        assertEquals(20, br.mirrorLife);
        assertFalse(br.multiValues || br.tides);
        assertEquals(0, br.strikeBonus);
        assertTrue(si.multiValues && si.tides && si.buildDelay);
        assertEquals(2, si.strikeBonus);
        assertEquals(45, si.tideCycle);
        assertEquals(30, si.tideAwake);
        assertEquals(si.tides, Config.forLeague(9).tides);
        Config v1 = Config.v1Bronze();
        assertEquals(20, v1.mirrorStock);
        assertEquals(3, v1.lock);
        assertEquals(1, v1.streakCap);
        assertFalse(v1.buildDelay);
    }

    @Test
    void immediatePlaceBendsTheBeamTheSameTurnAndScores() throws Exception {
        Game g = new Game(example(), 4, immediate());
        TurnReport r = g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        assertEquals(2, r.gain[0]);
        assertEquals(2, g.score[0]);
        assertEquals(11, g.mirrorsLeft[0]);
        assertEquals(at(5, 6), (int) r.beams[0].lit.get(0));
    }

    @Test
    void lockTimelineWithoutDelayShowsLockThenCountsDown() throws Exception {
        Config c = immediate();
        c.lock = 3;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        int[] seen = new int[4];
        for (int t = 0; t < 4; t++) {
            seen[t] = g.board.mirrorLock[at(5, 3)];
            TurnReport r = g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd()));
            if (t < 3) assertNotNull(r.invalid[0], "turn refused while locked");
            else assertNull(r.invalid[0]);
        }
        assertArrayEquals(new int[] {3, 2, 1, 0}, seen);
        assertEquals(0, g.board.mirrorType[at(5, 3)]);
    }

    @Test
    void buildDelayTimelineF6() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        TurnReport r = g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd())); // turn t
        assertEquals(0, r.gain[0]);
        assertEquals(1, g.board.pending[at(5, 3)]);
        assertEquals(0, g.board.mirrorLock[at(5, 3)]);
        assertEquals(20, g.board.mirrorLife[at(5, 3)]);
        r = g.playTurn(waits(4)); // t+1: still pending, activates at the end
        assertEquals(0, r.gain[0]);
        assertEquals(0, g.board.pending[at(5, 3)]);
        assertEquals(5, g.board.mirrorLock[at(5, 3)]);
        assertEquals(20, g.board.mirrorLife[at(5, 3)]);
        r = g.playTurn(waits(4)); // t+2: reflects
        assertEquals(2, r.gain[0]);
        assertEquals(4, g.board.mirrorLock[at(5, 3)]);
        assertEquals(19, g.board.mirrorLife[at(5, 3)]);
        for (int t = 3; t <= 6; t++) assertNotNull(g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd())).invalid[0], "t+" + t);
        assertNull(g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd())).invalid[0]); // t+7
    }

    @Test
    void mirrorReflectsTwentyTurnsThenBreaks() throws Exception {
        Config cfg = Config.bronze();
        cfg.earlyEnd = false; // tests wear, not the end
        Game g = new Game(example(), 4, cfg);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(waits(4));
        int reflecting = 0;
        for (int t = 0; t < 25; t++) if (g.playTurn(waits(4)).beams[0].corners.size() > 1) reflecting++;
        assertEquals(20, reflecting);
        assertEquals(-1, g.board.mirrorType[at(5, 3)]);
        assertEquals(-1, g.board.mirrorLife[at(5, 3)]);
    }

    @Test
    void turnedMirrorKeepsItsLifeThatTurn() throws Exception {
        Config c = Config.bronze();
        c.lock = 0;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(waits(4));
        g.playTurn(waits(4));
        assertEquals(19, g.board.mirrorLife[at(5, 3)]);
        g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd()));
        assertEquals(19, g.board.mirrorLife[at(5, 3)]);
    }

    @Test
    void startMirrorsWearFromBronzeOnly() {
        for (long seed = 0; seed < 20; seed++) {
            Game br = Game.fromSeed(seed, 4, Config.bronze()), w1 = Game.fromSeed(seed, 4, Config.woodOne());
            for (int p = 0; p < br.board.n * br.board.n; p++) {
                if (br.board.mirrorType[p] < 0) continue;
                assertEquals(20, br.board.mirrorLife[p]);
                assertEquals(0, br.board.pending[p]);
                assertEquals(-1, w1.board.mirrorLife[p]);
            }
        }
    }

    @Test
    void supplyGivesOneMirrorOnTurnsDivisibleByFourBeforeInputCappedAtSix() throws Exception {
        Config cfg = Config.bronze();
        cfg.earlyEnd = false; // tests supply, not the end
        Game g = new Game(example(), 4, cfg);
        int[] hand = new int[30];
        for (int t = 1; t <= 29; t++) {
            g.beginTurn();
            hand[t] = g.mirrorsLeft[1];
            g.playTurn(waits(4));
        }
        assertEquals(4, hand[1]);
        assertEquals(4, hand[3]);
        assertEquals(5, hand[4]);
        assertEquals(6, hand[8]);
        assertEquals(6, hand[28]);
        g.deactivate(2);
        g.playTurn(waits(4));
        g.playTurn(waits(4));
        g.beginTurn();
        assertEquals(32, g.currentTurn());
        assertEquals(0, g.mirrorsLeft[2]);
        assertEquals(6, g.mirrorsLeft[1]);
    }

    @Test
    void beginTurnIsIdempotent() {
        Game g = new Game(example(), 4, Config.bronze());
        for (int t = 1; t < 4; t++) g.playTurn(new Command[4]);
        g.beginTurn();
        g.beginTurn();
        assertEquals(5, g.mirrorsLeft[0]);
        g.playTurn(new Command[4]);
        assertEquals(5, g.mirrorsLeft[0]);
    }

    @Test
    void pendingMirrorCannotBeTurnedNorBuiltOn() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        TurnReport r = g.playTurn(cmds(waitCmd(), turn(5, 3), place(5, 3, 0), waitCmd()));
        assertNotNull(r.invalid[1]);
        assertNotNull(r.invalid[2]);
        assertEquals(4, g.mirrorsLeft[2]);
    }

    @Test
    void workedExampleV27StreakPaysTwoFourSixEightEight() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd()));
        int[] gains = new int[5];
        for (int t = 0; t < 5; t++) gains[t] = g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd())).gain[0];
        assertArrayEquals(new int[] {2, 4, 6, 8, 8}, gains);
        assertEquals(28, g.score[0]);
        assertEquals(0, g.litBy[at(5, 6)]);
        assertEquals(4, g.streak[0][at(5, 6)]);
    }

    @Test
    void contestedCrystalPaysNobodyAndFreezesTheLightersStreaks() throws Exception {
        Game g = new Game(example(), 4, immediate());
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(waits(4));
        assertEquals(2, g.streak[0][at(5, 6)]);
        TurnReport r = g.playTurn(cmds(waitCmd(), place(9, 6, 0), waitCmd(), waitCmd()));
        assertEquals(0, r.gain[0]);
        assertEquals(1 + 3, r.gain[1]);
        assertEquals(2, g.streak[0][at(5, 6)], "frozen");
        assertEquals(0, g.streak[1][at(5, 6)]);
        assertEquals(-2, g.litBy[at(5, 6)]);
        assertEquals(1, g.litBy[at(6, 6)]);
    }

    @Test
    void streakFallsByOneWhenAnotherHoldsItAloneOrNobodyLightsIt() throws Exception {
        Config c = immediate();
        c.lock = 0;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(waits(4));
        g.playTurn(waits(4));
        assertEquals(3, g.streak[0][at(5, 6)]);
        g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd())); // P0 beam now goes north: (5,6) unlit
        assertEquals(2, g.streak[0][at(5, 6)]);
        assertEquals(-1, g.litBy[at(5, 6)]);
        g.playTurn(cmds(waitCmd(), place(9, 6, 0), waitCmd(), waitCmd())); // P1 alone on (5,6)
        assertEquals(1, g.streak[0][at(5, 6)]);
        assertEquals(1, g.streak[1][at(5, 6)]);
        g.playTurn(waits(4));
        g.playTurn(waits(4));
        assertEquals(0, g.streak[0][at(5, 6)]);
        assertEquals(3, g.streak[1][at(5, 6)]);
    }

    @Test
    void dormantCrystalPaysNothingAndClearsStreaks() throws Exception {
        Config c = immediate();
        c.tides = true;
        c.earlyEnd = false; // tests tides, not the end
        Board b = example();
        b.phase[at(5, 6)] = 44; // awake when (t+44)%45 < 30: turns 1..30, dormant 31..45
        Game g = new Game(b, 4, c);
        int[] gain = new int[33];
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        for (int t = 2; t <= 32; t++) gain[t] = g.playTurn(waits(4)).gain[0];
        assertEquals(8, gain[30]);
        assertEquals(0, gain[31]);
        assertEquals(0, g.streak[0][at(5, 6)]);
        assertFalse(g.awake(at(5, 6), 31));
        assertTrue(g.awake(at(5, 6), 30));
        assertTrue(g.awake(at(5, 6), 46));
    }

    @Test
    void sharedScoringInWoodTwoPaysEveryLighter() throws Exception {
        Game g = new Game(example(), 4, Config.forLeague(1));
        TurnReport r = g.playTurn(cmds(place(5, 3, 1), place(9, 6, 0), waitCmd(), waitCmd()));
        assertEquals(2, r.gain[0]);
        assertEquals(1 + 3 + 2, r.gain[1]);
        r = g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd()));
        assertEquals(2, r.gain[0], "no streak in Wood 2");
    }

    @Test
    void turnIsRefusedInWoodTwo() throws Exception {
        Game g = new Game(example(), 4, Config.forLeague(1));
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        for (int t = 0; t < 6; t++) g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd()));
        assertNotNull(g.playTurn(cmds(turn(5, 3), waitCmd(), waitCmd(), waitCmd())).invalid[0]);
    }

    @Test
    void samePlaceCellCollisionFailsForAllAndKeepsStock() throws Exception {
        Game g = new Game(example(), 4, immediate());
        TurnReport r = g.playTurn(cmds(waitCmd(), place(9, 6, 0), place(9, 6, 1), waitCmd()));
        assertEquals(-1, g.board.mirrorType[at(9, 6)]);
        assertEquals(12, g.mirrorsLeft[1]);
        assertEquals(12, g.mirrorsLeft[2]);
        assertTrue(r.invalid[1].contains("collision"));
        assertTrue(r.invalid[2].contains("collision"));
    }

    @Test
    void severalTurnsOnOneMirrorFlipItOnceAndLowestIdIsLastToucher() throws Exception {
        Config c = immediate();
        c.lock = 1;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd()));
        TurnReport r = g.playTurn(cmds(waitCmd(), waitCmd(), turn(5, 3), turn(5, 3)));
        assertNull(r.invalid[2]);
        assertEquals(0, g.board.mirrorType[at(5, 3)]);
        assertEquals(2, g.board.lastToucher[at(5, 3)]);
        assertEquals(0, r.gain[0], "P0 beam now goes north");
    }

    @Test
    void turnCannotTargetAMirrorPlacedThisTurn() throws Exception {
        Game g = new Game(example(), 4, immediate());
        TurnReport r = g.playTurn(cmds(place(5, 3, 1), turn(5, 3), waitCmd(), waitCmd()));
        assertNotNull(r.invalid[1]);
        assertEquals(1, g.board.mirrorType[at(5, 3)]);
    }

    @Test
    void placeValidation() throws Exception {
        Game g = new Game(example(), 4, immediate());
        String[] bad = {"PLACE -1 3 0", "PLACE 13 3 0", "PLACE 5 3 2", "PLACE 5 6 0", "PLACE 5 10 0",
            "PLACE 0 3 0", "PLACE 9 0 0", "PLACE 9 1 0"};
        for (String line : bad) {
            TurnReport r = g.playTurn(cmds(CommandParser.parse(line), waitCmd(), waitCmd(), waitCmd()));
            assertNotNull(r.invalid[0], line);
        }
        assertEquals(12, g.mirrorsLeft[0]);
        assertNull(g.playTurn(cmds(place(1, 3, 0), waitCmd(), waitCmd(), waitCmd())).invalid[0], "own zone is fine");
    }

    @Test
    void emptyStockRefusesPlace() throws Exception {
        Config c = immediate();
        c.mirrorStock = 1;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        assertNotNull(g.playTurn(cmds(place(6, 3, 1), waitCmd(), waitCmd(), waitCmd())).invalid[0]);
    }

    @Test
    void turnInAnotherSeatsZoneIsRefused() throws Exception {
        Config c = immediate();
        c.lock = 0;
        Game g = new Game(example(), 4, c);
        g.playTurn(cmds(place(1, 3, 0), waitCmd(), waitCmd(), waitCmd()));
        assertNotNull(g.playTurn(cmds(waitCmd(), turn(1, 3), waitCmd(), waitCmd())).invalid[1]);
        assertNull(g.playTurn(cmds(turn(1, 3), waitCmd(), waitCmd(), waitCmd())).invalid[0]);
        assertNotNull(g.playTurn(cmds(waitCmd(), turn(7, 7), waitCmd(), waitCmd())).invalid[1], "no mirror");
    }

    @Test
    void emptySeatZoneAndLampCellAreOpen() throws Exception {
        Game g = new Game(example(), 2, immediate()); // seats 0 and 2; seat 1 lamp (9,0) empty (QA Q5, F6.2)
        assertNull(g.playTurn(cmds(place(9, 1, 0), waitCmd())).invalid[0]);
        assertNull(g.playTurn(cmds(place(9, 0, 0), waitCmd())).invalid[0]);
        assertNotNull(g.playTurn(cmds(place(12, 9, 0), waitCmd())).invalid[0], "lamp of a seat with a player");
    }

    @Test
    void mirrorOnAnEmptySeatLampCellReflectsBeams() throws Exception {
        Board b = Board.empty(13, 3);
        Game g = new Game(b, 2, immediate());
        g.playTurn(cmds(place(9, 3, 0), waitCmd())); // seat 0 row 3 east, '/' at (9,3) -> north up column 9 to (9,0)
        g.playTurn(cmds(place(9, 0, 1), waitCmd())); // '\' at the empty lamp cell (9,0): N -> W
        Beam beam = g.playTurn(cmds(waitCmd(), waitCmd())).beams[0];
        assertEquals(Dir.W, beam.endDir);
        assertEquals(-1, beam.endX);
    }

    @Test
    void strikeGivesBonusOnlyWhenEnabled() throws Exception {
        Game g = new Game(Board.empty(13, 3), 4, immediate());
        TurnReport r = g.playTurn(cmds(place(12, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        assertEquals(2, r.gain[0]);
        assertEquals(2, r.beams[0].struckSeat);
        Game w = new Game(Board.empty(13, 3), 4, Config.forLeague(2));
        assertEquals(0, w.playTurn(cmds(place(12, 3, 1), waitCmd(), waitCmd(), waitCmd())).gain[0]);
        assertEquals(0, Config.bronze().strikeBonus);
    }

    @Test
    void deactivationTurnsLampIntoWallKeepsMirrorsAndScoresMinusOne() throws Exception {
        Game g = new Game(Board.empty(13, 3), 4, immediate());
        g.playTurn(cmds(waitCmd(), place(9, 5, 0), waitCmd(), waitCmd()));
        g.deactivate(2);
        assertEquals(-1, g.score[2]);
        assertEquals(0, g.mirrorsLeft[2]);
        assertFalse(g.active[2]);
        TurnReport r = g.playTurn(cmds(place(12, 3, 1), waitCmd(), null, waitCmd()));
        assertEquals(Beam.Stop.ABSORBED, r.beams[0].stop);
        assertEquals(0, r.gain[0]);
        assertNull(r.beams[2]);
        assertEquals(0, g.board.mirrorType[at(9, 5)]);
        assertEquals(-1, g.score[2]);
        assertNotNull(g.playTurn(cmds(place(12, 8, 0), waitCmd(), null, waitCmd())).invalid[0], "zone stays protected");
    }

    @Test
    void gameEndsAtMaxTurnsOrWithFewerThanTwoActive() throws Exception {
        Config c = immediate();
        c.maxTurns = 3;
        Game g = new Game(example(), 2, c);
        for (int t = 0; t < 2; t++) {
            g.playTurn(cmds(waitCmd(), waitCmd()));
            assertFalse(g.isOver());
        }
        g.playTurn(cmds(waitCmd(), waitCmd()));
        assertTrue(g.isOver());
        Game h = new Game(example(), 3, immediate());
        h.deactivate(0);
        assertFalse(h.isOver());
        h.deactivate(1);
        assertTrue(h.isOver());
    }

    @Test
    void sealedTypeHiddenFromRivalsUntilActive() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        g.playTurn(cmds(place(5, 3, 1), waitCmd(), waitCmd(), waitCmd()));
        assertTrue(Protocol.turnLinesV5(g, 0).contains("5 3 1 -1 20"));
        assertTrue(Protocol.turnLinesV5(g, 1).contains("5 3 -1 -1 20"));
        g.playTurn(cmds(waitCmd(), waitCmd(), waitCmd(), waitCmd()));
        assertTrue(Protocol.turnLinesV5(g, 1).contains("5 3 1 5 20"));
    }

    @Test
    void copyKeepsACustomSeatMapping() {
        Game g = new Game(Board.empty(13, 3), new int[] {2, 0}, Config.woodOne()); // relative ids: me at seat 2
        assertArrayEquals(new int[] {2, 0}, g.copy().seatOf);
        assertArrayEquals(new int[] {2, 0}, g.copyWith(Config.bronze()).seatOf);
    }

    @Test
    void seedFromGeneratorMatchesConfigMapOptions() {
        Game g = Game.fromSeed(5L, 4, Config.forLeague(2));
        for (int p = 0; p < g.board.n * g.board.n; p++)
            if (g.board.kind[p] == Board.CRYSTAL) assertEquals(1, g.board.value[p]);
    }
}
