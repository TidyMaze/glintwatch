package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codingame.game.bots.BatchRunner;
import com.codingame.game.bots.Agents;
import com.codingame.game.engine.Config;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

public class RefereeTest {
    private static GameResult run(int league, long seed, Class<?>... agents) {
        System.setProperty("glintwatch.league", String.valueOf(league));
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.setSeed(seed);
        runner.setLeagueLevel(league);
        for (int i = 0; i < agents.length; i++) runner.addAgent(agents[i], "Bot" + i);
        GameResult r = runner.simulate();
        for (String err : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
            assertTrue(isDiagnostic(err), "league " + league + " seed " + seed + ": " + err);
        assertEquals(agents.length, r.scores.size(), "scores " + r.scores + " summaries " + r.summaries);
        return r;
    }

    /** Referee stderr may only hold its own [glintwatch] diagnostic lines (read in CG's "Game information" tab). */
    public static boolean isDiagnostic(String err) {
        if (err == null || err.isEmpty()) return true;
        for (String line : err.split("\n")) if (!line.isEmpty() && !line.startsWith("[glintwatch] ")) return false;
        return true;
    }

    @Test
    void botOverTheTurnTimeIsEliminatedForTimeout() {
        GameResult r = run(1, 4L, Agents.Sleepy.class, Agents.Wait.class, Agents.Wait.class);
        assertEquals(-1, r.scores.get(0).intValue());
        assertTrue(String.join("", r.summaries).contains("eliminated: timeout"));
    }

    @Test
    void refereeLogsToStderrOnlyWithTheDiagSwitchAndNeverInTheSummary() {
        System.setProperty("glintwatch.diag", "true");
        GameResult on;
        try {
            on = run(1, 9L, Agents.Wait.class, Agents.Greedy.class);
        } finally {
            System.clearProperty("glintwatch.diag");
        }
        GameResult off = run(1, 9L, Agents.Wait.class, Agents.Greedy.class);
        String onErr = stderr(on), offErr = stderr(off);
        assertTrue(onErr.contains("[glintwatch] init start") && onErr.contains("[glintwatch] turn 1 done"), onErr);
        assertEquals("", offErr.trim(), "no referee stderr in shipped mode");
        assertFalse(String.join("", on.summaries).contains("[glintwatch]"), "players never see diagnostics");
        assertFalse(String.join("", off.summaries).contains("[glintwatch]"));
    }

    private static String stderr(GameResult r) {
        return String.join("\n", r.errors.getOrDefault("referee", java.util.Collections.emptyList()).stream()
            .filter(java.util.Objects::nonNull).toArray(String[]::new));
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
                assertTrue(isDiagnostic(err), "league " + league + ": " + err);
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

    /** UX 15: CG best practice "The total entities should not exceed 200", counted as GEM ids created over a whole game. */
    @Test
    void viewerCreatesAtMostTwoHundredEntitiesOverTheFullestGame() {
        long[][] cases = {{4, 301}, {3, 301}, {2, 301}, {1, 301}, {4, 34}};
        for (long[] c : cases) {
            GameResult r = run((int) c[0], c[1], Agents.Plus.class, Agents.Greedy.class, Agents.Self.class, Agents.Random.class);
            int created = createdEntities(r.views);
            System.out.println("league " + c[0] + " seed " + c[1] + " entities created " + created);
            assertTrue(created <= 200, "league " + c[0] + " seed " + c[1] + ": " + created + " entities");
        }
    }

    /** UX 16 (owner): the CG player shows the turn, so no viewer text holds it ("Turn", "t=", "/150", "/100"). */
    @Test
    void viewerShowsNoTurnNumber() {
        for (int league : new int[] {1, 4}) {
            GameResult r = run(league, 5L, Agents.Plus.class, Agents.Greedy.class);
            for (String v : r.views) {
                if (v == null) continue;
                for (String banned : new String[] {"Turn", "t=", "/150", "/100"})
                    assertFalse(v.contains(banned), "league " + league + " shows " + banned);
            }
        }
    }

    /** Help Center "The referee must not crash": an unexpected exception ends the game cleanly, scores kept, with a summary line. */
    @Test
    void anExceptionInATurnEndsTheGameCleanlyInsteadOfCrashing() {
        Referee.failAt = "turn 7";
        try {
            GameResult r = run(4, 5L, Agents.Plus.class, Agents.Greedy.class);
            assertTrue(String.join("", r.summaries).contains("Game over: referee error at turn 7"), r.summaries.toString());
            assertTrue(r.scores.get(0) > 0 || r.scores.get(1) > 0, "scores kept " + r.scores);
        } finally {
            Referee.failAt = null;
        }
    }

    @Test
    void anExceptionInInitEndsTheGameCleanly() {
        Referee.failAt = "init";
        try {
            GameResult r = run(1, 5L, Agents.Wait.class, Agents.Wait.class);
            assertTrue(String.join("", r.summaries).contains("Game over: referee error at init"), r.summaries.toString());
        } finally {
            Referee.failAt = null;
        }
    }

    /** Counts GEM create commands: lines of the "entitymodule" data starting with C, one id per ';'-separated type. */
    static int createdEntities(java.util.List<String> views) {
        int n = 0;
        for (String v : views) {
            if (v == null) continue;
            int brace = v.indexOf('{');
            if (brace < 0) continue;
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(v.substring(brace)).getAsJsonObject();
            if (o.has("frame")) o = o.getAsJsonObject("frame");
            if (!o.has("entitymodule")) continue;
            for (String line : o.get("entitymodule").getAsString().split("\n"))
                if (line.startsWith("C")) n += line.substring(1).split(";").length;
        }
        return n;
    }

    @Test
    void viewerRegistersBeamYardLockAndCoordinateToggles() {
        GameResult r = run(4, 5L, Agents.Plus.class, Agents.Greedy.class, Agents.Self.class, Agents.Random.class);
        String all = String.join("", r.views);
        for (String t : new String[] {"yards", "coords"})
            assertTrue(all.contains("\"" + t + "\""), "toggle " + t);
        // UX 15.2: beams and lock rings are drawn by the board module, which reads these toggles itself
        String module = readView("board-module/BoardModule.js"), config = readView("config.js");
        for (String t : new String[] {"beam", "locks"}) assertTrue(module.contains("'" + t), "module toggle " + t);
        for (String t : new String[] {"beam0", "beam1", "beam2", "beam3", "locks", "BoardModule"}) assertTrue(config.contains(t), "config " + t);
    }

    private static String readView(String file) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/view", file)),
                java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** DESIGN V5.9 task 11: a level-1 game of the stub (WAIT) against the WAIT boss stops after turn 20. */
    @Test
    void allWaitGameEndsAfterTwentyTurnsAndSaysWhy() {
        GameResult r = run(1, 3L, Agents.Wait.class, Agents.Wait.class);
        String summary = String.join("", r.summaries);
        assertTrue(summary.contains("Game over: no player acted for 20 turns"), summary);
        long turnFrames = r.views.stream().filter(v -> v != null && v.startsWith("KEY_FRAME")).count();
        assertTrue(turnFrames <= 22, "frames " + turnFrames);
        assertEquals(0, r.scores.get(0).intValue());
    }

    @Test
    void chattyBotMessageIsShownWithoutError() {
        GameResult r = run(1, 2L, Agents.Chatty.class, Agents.Wait.class);
        for (String err : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
            assertTrue(isDiagnostic(err), err);
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
