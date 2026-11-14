package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** V6: every league gets the same input layout. One parser reads every league's lines; the shape is checked, not only the values. */
class InputLayoutTest {
    /** One turn: head = score and 4 hands; mirrors = x y type lock life; crystals = tide litBy streak. */
    record Turn(int[] head, List<int[]> mirrors, List<int[]> crystals) {}

    static int[] nums(String line) {
        return Arrays.stream(line.trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
    }

    /** The one parser: it fails if a line is missing or extra. */
    static Turn parse(List<String> lines) {
        int i = 0;
        int[] head = nums(lines.get(i++));
        int m = nums(lines.get(i++))[0];
        List<int[]> mirrors = new ArrayList<>();
        for (int k = 0; k < m; k++) mirrors.add(nums(lines.get(i++)));
        int c = nums(lines.get(i++))[0];
        List<int[]> crystals = new ArrayList<>();
        for (int k = 0; k < c; k++) crystals.add(nums(lines.get(i++)));
        assertEquals(lines.size(), i, "no line beyond the layout");
        return new Turn(head, mirrors, crystals);
    }

    @Test
    void everyLeagueSendsTheSameInitLayout() {
        for (int league = 1; league <= 4; league++) {
            Game g = Game.fromSeed(7, 4, Config.forLeague(league));
            List<String> init = Protocol.initLinesV5(g, 0);
            assertEquals(2 + g.board.n, init.size(), "league " + league);
            assertEquals("4", init.get(0), "league " + league);
            assertEquals(String.valueOf(g.board.n), init.get(1), "league " + league);
            for (int y = 0; y < g.board.n; y++) assertEquals(g.board.n, init.get(2 + y).length(), "league " + league + " row " + y);
        }
    }

    @Test
    void everyLeagueSendsTheSameTurnLayoutForEveryPlayer() {
        for (int league = 1; league <= 4; league++)
            for (long seed = 1; seed <= 3; seed++) {
                Game g = Game.fromSeed(seed, 4, Config.forLeague(league));
                for (int t = 0; t < 40 && !g.isOver(); t++) {
                    for (int v = 0; v < 4; v++) {
                        Turn turn = parse(Protocol.turnLinesV5(g, v));
                        assertEquals(5, turn.head().length, "league " + league);
                        for (int[] m : turn.mirrors()) assertEquals(5, m.length, "league " + league);
                        for (int[] cr : turn.crystals()) {
                            assertEquals(3, cr.length, "league " + league);
                            assertEquals(0, cr[0], "league " + league + ": tide is 0 in every league (V6)");
                        }
                    }
                    g.playTurn(new Command[4]);
                }
            }
    }

    @Test
    void everyLeagueShowsTheSameSupplyInHand() {
        for (int league = 1; league <= 4; league++) {
            Game g = Game.fromSeed(3, 4, Config.forLeague(league));
            for (int t = 0; t < 8; t++) g.playTurn(new Command[4]); // now turn 9
            assertEquals(12, parse(Protocol.turnLinesV5(g, 0)).head()[1], "league " + league + ": 12 mirrors at the start");
            g.playTurn(new Command[4]); // now turn 10: the input shows the supply
            assertEquals(13, parse(Protocol.turnLinesV5(g, 0)).head()[1], "league " + league + ": +1 on turn 10 (V6 supply)");
        }
    }
}
