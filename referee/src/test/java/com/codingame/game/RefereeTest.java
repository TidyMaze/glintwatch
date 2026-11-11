package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codingame.game.bots.BatchRunner;
import com.codingame.game.bots.Agents;
import com.codingame.game.engine.Config;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

class RefereeTest {
    private static GameResult run(int league, long seed, Class<?>... agents) {
        System.setProperty("glintwatch.league", String.valueOf(league));
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.setSeed(seed);
        runner.setLeagueLevel(league);
        for (int i = 0; i < agents.length; i++) runner.addAgent(agents[i], "Bot" + i);
        GameResult r = runner.simulate();
        for (String err : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
            assertTrue(err == null || err.isEmpty(), "league " + league + " seed " + seed + ": " + err);
        assertEquals(agents.length, r.scores.size(), "scores " + r.scores + " summaries " + r.summaries);
        return r;
    }

    @Test
    void sdkRefereeMatchesTheEngineScoresForTheSameSeed() {
        GameResult r = run(3, 42L, Agents.Greedy.class, Agents.Self.class, Agents.Wait.class, Agents.Plus.class);
        BatchRunner.Result e = BatchRunner.run(new String[] {"greedy", "self", "wait", "plus"}, 1, 42L, Config.bronze());
        for (int i = 0; i < 4; i++) assertEquals(e.totalScore[i], r.scores.get(i).intValue(), "player " + i);
    }

    @Test
    void silverRefereeMatchesTheEngine() {
        GameResult r = run(4, 7L, Agents.Plus.class, Agents.Greedy.class, Agents.Self.class);
        BatchRunner.Result e = BatchRunner.run(new String[] {"plus", "greedy", "self"}, 1, 7L, Config.silver());
        for (int i = 0; i < 3; i++) assertEquals(e.totalScore[i], r.scores.get(i).intValue(), "player " + i);
    }

    @Test
    void viewerRaisesNoRefereeErrorAndStaysUnderTheSdkSoftViewQuota() {
        for (int league = 1; league <= 4; league++) {
            GameResult r = run(league, 11L, Agents.Plus.class, Agents.Greedy.class, Agents.Self.class, Agents.Random.class);
            for (String err : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
                assertTrue(err == null || err.isEmpty(), "league " + league + ": " + err);
            long size = 0;
            for (String v : r.views) size += v == null ? 0 : v.length();
            System.out.println("league " + league + " replay chars " + size + " frames " + r.views.size());
            assertTrue(size < 512 * 1024, "replay size " + size + " (SDK warns above 512 KiB, fails above 1 MiB)");
        }
    }

    /** RefereeAgent.getOutput stops past 30,000 bytes whenever the pipe is momentarily empty, frame 0 included. */
    @Test
    void everyFrameStaysUnderTheRunnerReadBufferOnTheLargestBoards() {
        long[][] cases = {{4, 301}, {3, 301}, {4, 34}, {2, 1}}; // 301: fullest N=19 map of seeds 0..4999
        for (long[] c : cases) {
            GameResult r = run((int) c[0], c[1], Agents.Plus.class, Agents.Plus.class, Agents.Plus.class, Agents.Plus.class);
            int max = 0, at = -1;
            for (int i = 0; i < r.views.size(); i++) {
                String v = r.views.get(i);
                int bytes = v == null ? 0 : v.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                if (bytes > max) {
                    max = bytes;
                    at = i;
                }
            }
            System.out.println("league " + c[0] + " seed " + c[1] + " max frame bytes " + max + " at " + at);
            assertTrue(max < 30_000, "league " + c[0] + " seed " + c[1] + ": frame " + at + " has " + max + " bytes");
        }
    }

    @Test
    void chattyBotMessageIsShownWithoutError() {
        GameResult r = run(1, 2L, Agents.Chatty.class, Agents.Wait.class);
        for (String err : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
            assertTrue(err == null || err.isEmpty(), err);
        assertEquals(0, r.scores.get(0).intValue());
    }

    @Test
    void woodTwoTwoPlayersRunsToOneHundredTurns() {
        GameResult r = run(1, 3L, Agents.Greedy.class, Agents.Wait.class);
        assertEquals(0, r.scores.get(1).intValue());
        assertTrue(r.scores.get(0) > 0);
    }

    @Test
    void unparseableOutputDeactivatesWithScoreMinusOne() {
        GameResult r = run(2, 5L, Agents.Garbage.class, Agents.Wait.class, Agents.Wait.class);
        assertEquals(-1, r.scores.get(0).intValue());
        assertTrue(String.join("", r.summaries).contains("eliminated: unknown command"), "parse error, not timeout");
        assertEquals(0, r.scores.get(1).intValue());
    }
}
