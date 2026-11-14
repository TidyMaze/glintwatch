package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

/** DESIGN V5.3c "Level 1 boss": Wanderer's moves on one seed, from both seats, on league 1 rules. */
class WandererBotTest {
    static final long SEED = 20L;

    /** "turn:command" of every non-WAIT output of the Wanderer in a 2-player game against WAIT; also the qualifying-free turns. */
    static List<String> moves(long seed, int wandererId, List<Integer> emptyTurns) throws Exception {
        return moves(seed, wandererId, emptyTurns, WandererBot.PERIOD, WandererBot.CAP);
    }

    static List<String> moves(long seed, int wandererId, List<Integer> emptyTurns, int period, int cap) throws Exception {
        Config cfg = Config.forLeague(1);
        Game g = Game.fromSeed(seed, 2, cfg);
        Bot[] bots = new Bot[2];
        for (int id = 0; id < 2; id++) {
            bots[id] = id == wandererId ? new WandererBot(cfg, period, cap) : new Agents.WaitBot();
            bots[id].init(Protocol.initLinesV5(g, id));
        }
        List<String> out = new ArrayList<>();
        int t = 0;
        while (!g.isOver()) {
            t++;
            Command[] cmds = new Command[2];
            for (int id = 0; id < 2; id++) {
                String line = bots[id].act(Protocol.turnLinesV5(g, id));
                cmds[id] = CommandParser.parse(line);
                if (id != wandererId) continue;
                if (!line.equals("WAIT")) {
                    assertNull(g.validate(id, cmds[id]), "turn " + t + " " + line);
                    out.add(t + ":" + line);
                } else if (t % period == 0 && g.mirrorsLeft[id] > 0 && out.size() < cap) emptyTurns.add(t);
            }
            g.playTurn(cmds);
        }
        return out;
    }

    /** analyst-h V6 tuning: period 40, cap 2 (C=4 and C=12 give the same result at P=40). */
    @Test
    void wandererUsesThePickedPeriodAndCap() {
        assertEquals(40, WandererBot.PERIOD, "analyst-h V6 tuning: period 40 is the first measured value above 75 %");
        assertEquals(2, WandererBot.CAP);
    }

    @Test
    /** Golden moves of the algorithm at period 20, cap 12 (the tuned V6 values are pinned in wandererUsesThePickedPeriodAndCap). */
    void wandererPlaysOnlyEveryPeriodAndAimsAtAFreeCrystal() throws Exception {
        for (int id = 0; id < 2; id++) {
            List<Integer> empty = new ArrayList<>();
            List<String> m = moves(SEED, id, empty, 20, 12);
            System.out.println("wanderer id " + id + " moves " + m + " no-cell turns " + empty);
            assertFalse(m.isEmpty());
            assertTrue(m.size() <= 12);
            for (String s : m) assertEquals(0, Integer.parseInt(s.split(":")[0]) % 20, s);
        }
        String[][] expected = {{"20:PLACE 1 2 1", "40:PLACE 1 3 1", "60:PLACE 3 3 0"}, {"20:PLACE 17 16 1", "40:PLACE 17 15 1", "60:PLACE 15 15 0"}};
        for (int id = 0; id < 2; id++) {
            List<Integer> empty = new ArrayList<>();
            assertEquals(List.of(expected[id]), moves(SEED, id, empty, 20, 12), "seat of player " + id);
            // at turn 80 no (cell, type) on its beam lights a free crystal: it WAITs with mirrors in hand
            assertEquals(List.of(80), empty, "player " + id);
        }
    }

    @Test
    void wandererStopsAtItsMirrorCap() throws Exception {
        List<String> m = moves(7L, 0, new ArrayList<>(), 5, 12);
        assertEquals(12, m.size());
        assertEquals("60:PLACE 4 7 1", m.get(m.size() - 1));
    }
}
