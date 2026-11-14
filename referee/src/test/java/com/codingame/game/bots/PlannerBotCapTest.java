package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

/** Lead 2026-11-11: the level 4 boss stops on a fixed rollout cap, not a clock, so its play does not depend on load. */
class PlannerBotCapTest {
    static List<String> play(String spec, long seed) throws Exception {
        Config cfg = Config.forLeague(4);
        Game g = Game.fromSeed(seed, 4, cfg);
        Bot[] bots = {new PlannerBot(cfg, spec), new GreedyBot(cfg, GreedyBot.Mode.SELF), new GreedyBot(cfg, GreedyBot.Mode.FULL),
            new Boss2(cfg, GreedyBot.Mode.FULL, 10, 1.0)};
        for (int id = 0; id < 4; id++) bots[id].init(Protocol.initLinesV5(g, id));
        List<String> out = new ArrayList<>();
        while (!g.isOver()) {
            Command[] c = new Command[4];
            for (int id = 0; id < 4; id++)
                if (g.active[id]) {
                    String line = bots[id].act(Protocol.turnLinesV5(g, id));
                    if (id == 0) out.add(line);
                    c[id] = CommandParser.parse(line);
                }
            g.playTurn(c);
        }
        return out;
    }

    @Test
    void capZeroEvaluatesOnlyWait() throws Exception {
        for (String m : play("planner:own:h=3:cap=0", 301L)) assertEquals("WAIT", m);
    }

    @Test
    void sameSeedGivesTheSameMovesUnderLoad() throws Exception {
        List<String> quiet = play("planner:own:h=3", 301L);
        List<Thread> load = new ArrayList<>();
        java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean();
        for (int k = 0; k < 2 * Runtime.getRuntime().availableProcessors(); k++) {
            Thread t = new Thread(() -> { long x = 0; while (!stop.get()) x += System.nanoTime() % 7; });
            t.setDaemon(true);
            t.start();
            load.add(t);
        }
        try {
            assertEquals(quiet, play("planner:own:h=3", 301L));
        } finally {
            stop.set(true);
        }
    }

    /** Analyst-h: turn 1 JIT warm-up (fixed decision count, results thrown away) must not change a single move. */
    @Test
    void warmUpDoesNotChangeTheMoves() throws Exception {
        assertEquals(play("planner:all:h=3:warmup=0", 301L), play("planner:all:h=3:warmup=100", 301L));
    }

    @Test
    void warmUpIsAFixedCountOnTheFirstTurnOnly() throws Exception {
        Config cfg = Config.forLeague(4);
        assertEquals(30, new PlannerBot(cfg, "planner:all:h=3").warmupDecisions, "default warm-up count: ~400-500 ms CPU on this machine");
        assertEquals(0, new PlannerBot(cfg, "planner:all:h=3:warmup=0").warmupDecisions);
    }

    /** Lead 2026-11-11: -Dgw.warmup=N overrides the default count for measurement; an explicit warmup= in the spec wins. */
    @Test
    void warmUpCountIsOverridableByProperty() throws Exception {
        Config cfg = Config.forLeague(4);
        System.setProperty("gw.warmup", "7");
        try {
            assertEquals(7, new PlannerBot(cfg, "planner:all:h=3").warmupDecisions, "property");
            assertEquals(0, new PlannerBot(cfg, "planner:all:h=3:warmup=0").warmupDecisions, "spec wins");
        } finally {
            System.clearProperty("gw.warmup");
        }
        assertEquals(30, new PlannerBot(cfg, "planner:all:h=3").warmupDecisions, "default again");
    }

    @Test
    void sameMovesForAnyWarmUpCount() throws Exception {
        List<String> base = play("planner:all:h=3:warmup=0", 302L);
        for (int n : new int[] {1, 7, 30}) assertEquals(base, play("planner:all:h=3:warmup=" + n, 302L), "warm-up " + n);
    }
}
