package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

/** V18 bosses: the build-delay shortcut must choose exactly what full rollouts choose; the bosses are deterministic. */
class StrongBotTest {
    private static void sameAsFullRollouts(int level, int players, long seed) throws Exception {
        Config cfg = Config.forLeague(level);
        Game g = Game.fromSeed(seed, players, cfg);
        StrongBot fast = new StrongBot(cfg, level), slow = new StrongBot(cfg, level);
        slow.slow = true;
        Bot[] others = new Bot[players];
        for (int id = 1; id < players; id++) others[id] = new PlannerBot(cfg, "planner:h=3");
        fast.init(Protocol.initLinesV5(g, 0));
        slow.init(Protocol.initLinesV5(g, 0));
        for (int id = 1; id < players; id++) others[id].init(Protocol.initLinesV5(g, id));
        int turns = 0;
        while (!g.isOver() && turns < 60) {
            turns++;
            Command[] cmds = new Command[players];
            String a = fast.act(Protocol.turnLinesV5(g, 0)), b = slow.act(Protocol.turnLinesV5(g, 0));
            assertEquals(b, a, "level " + level + " players " + players + " seed " + seed + " turn " + turns);
            cmds[0] = CommandParser.parse(a);
            for (int id = 1; id < players; id++) cmds[id] = CommandParser.parse(others[id].act(Protocol.turnLinesV5(g, id)));
            g.playTurn(cmds);
        }
    }

    @Test
    void buildDelayShortcutEqualsFullRolloutsLevel3() throws Exception {
        sameAsFullRollouts(3, 2, 11);
        sameAsFullRollouts(3, 4, 12);
    }

    @Test
    void buildDelayShortcutEqualsFullRolloutsLevel4() throws Exception {
        sameAsFullRollouts(4, 2, 13);
        sameAsFullRollouts(4, 3, 14);
    }

    @Test
    void sameInputSameMoves() throws Exception {
        for (int level = 1; level <= 4; level++) {
            Config cfg = Config.forLeague(level);
            Game g = Game.fromSeed(21, 2, cfg);
            StrongBot a = new StrongBot(cfg, level), b = new StrongBot(cfg, level);
            a.init(Protocol.initLinesV5(g, 0));
            b.init(Protocol.initLinesV5(g, 0));
            Bot other = new GreedyBot(cfg, GreedyBot.Mode.FULL);
            other.init(Protocol.initLinesV5(g, 1));
            for (int t = 0; t < 30 && !g.isOver(); t++) {
                String x = a.act(Protocol.turnLinesV5(g, 0));
                assertEquals(x, b.act(Protocol.turnLinesV5(g, 0)), "level " + level + " turn " + t);
                g.playTurn(new Command[] {CommandParser.parse(x), CommandParser.parse(other.act(Protocol.turnLinesV5(g, 1)))});
            }
        }
    }
}
