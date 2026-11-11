package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.Config;

class BatchRunnerTest {
    @Test
    void reportsWinSharesThatSumToGamesAndIsDeterministic() {
        BatchRunner.Result a = BatchRunner.run(new String[] {"greedy", "wait", "random", "self"}, 8, 0L, Config.bronze());
        BatchRunner.Result b = BatchRunner.run(new String[] {"greedy", "wait", "random", "self"}, 8, 0L, Config.bronze());
        double sum = 0;
        for (double w : a.wins) sum += w;
        assertEquals(8.0, sum, 1e-9);
        assertArrayEquals(a.wins, b.wins, 1e-9);
        assertArrayEquals(a.totalScore, b.totalScore);
        assertEquals(0, a.crashes);
        assertEquals(150.0, a.avgTurns(), 1e-9);
        assertTrue(a.totalScore[1] >= 0, "WAIT can be bent by neutral mirrors");
        assertTrue(a.refereeMsPerTurn() >= 0);
    }

    @Test
    void crashingBotIsDeactivatedAndCounted() {
        BatchRunner.Result r = BatchRunner.run(new String[] {"crash", "wait"}, 2, 0L, Config.woodTwo());
        assertEquals(2, r.crashes);
        assertEquals(1.0, r.avgTurns(), 1e-9);
        assertEquals(2.0, r.wins[1], 1e-9);
    }
}

class BatchRunnerRecordsTest {
    @org.junit.jupiter.api.Test
    void perGameRecordsHoldSeatsRanksAndNonWaitShare() {
        BatchRunner.Result r = BatchRunner.run(new String[] {"greedy", "wait", "self"}, 3, 5L, com.codingame.game.engine.Config.woodOne());
        org.junit.jupiter.api.Assertions.assertEquals(3, r.records.size());
        double seatSum = 0;
        for (double w : r.seatWins) seatSum += w;
        org.junit.jupiter.api.Assertions.assertEquals(3.0, seatSum, 1e-9);
        for (BatchRunner.GameRecord g : r.records) {
            org.junit.jupiter.api.Assertions.assertEquals(3, g.slot.length);
            for (int id = 0; id < 3; id++) {
                org.junit.jupiter.api.Assertions.assertTrue(g.rank[id] >= 1 && g.rank[id] <= 3);
                if (g.bot[id].equals("wait")) org.junit.jupiter.api.Assertions.assertEquals(0.0, g.nonWaitShare[id], 1e-9);
            }
        }
        BatchRunner.GameRecord g0 = r.records.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(5L, g0.seed);
        org.junit.jupiter.api.Assertions.assertEquals("greedy", g0.bot[0]);
        org.junit.jupiter.api.Assertions.assertEquals("wait", r.records.get(1).bot[0], "seats rotate");
    }

    @org.junit.jupiter.api.Test
    void ranksShareTiesAndDeactivatedAreLast() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {1, 1, 3, 4}, BatchRunner.ranks(new int[] {5, 5, 2, -1}));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {2, 1}, BatchRunner.ranks(new int[] {0, 7}));
    }

    @org.junit.jupiter.api.Test
    void csvHasAHeaderAndOneRowPerPlayerPerGame() throws Exception {
        BatchRunner.Result r = BatchRunner.run(new String[] {"self", "wait"}, 2, 0L, com.codingame.game.engine.Config.woodTwo());
        java.io.StringWriter w = new java.io.StringWriter();
        BatchRunner.writeCsv(r, w);
        String[] lines = w.toString().split("\n");
        org.junit.jupiter.api.Assertions.assertEquals("seed,players,id,seat,bot,score,rank,nonWaitShare,maxBotMs,overTime,crashed", lines[0]);
        org.junit.jupiter.api.Assertions.assertEquals(1 + 2 * 2, lines.length);
        org.junit.jupiter.api.Assertions.assertTrue(lines[1].startsWith("0,2,0,0,self,"));
        java.io.StringWriter j = new java.io.StringWriter();
        BatchRunner.writeJson(r, j);
        org.junit.jupiter.api.Assertions.assertTrue(j.toString().startsWith("{\"games\":2,"));
        org.junit.jupiter.api.Assertions.assertTrue(j.toString().contains("\"seatWins\":["));
    }

    @org.junit.jupiter.api.Test
    void slowBotTurnsAreCountedAndEnforcementDeactivates() {
        BatchRunner.Options o = new BatchRunner.Options();
        o.turnBudgetMs = 1;
        o.firstTurnBudgetMs = 1;
        BatchRunner.Result counted = BatchRunner.run(new String[] {"slow", "wait"}, 1, 0L, com.codingame.game.engine.Config.woodTwo(), o);
        org.junit.jupiter.api.Assertions.assertTrue(counted.records.get(0).overTime[0] > 0);
        org.junit.jupiter.api.Assertions.assertEquals(0, counted.timeouts);
        o.enforceTimeouts = true;
        BatchRunner.Result enforced = BatchRunner.run(new String[] {"slow", "wait"}, 1, 0L, com.codingame.game.engine.Config.woodTwo(), o);
        org.junit.jupiter.api.Assertions.assertEquals(1, enforced.timeouts);
        org.junit.jupiter.api.Assertions.assertEquals(-1, enforced.records.get(0).score[0]);
    }
}
