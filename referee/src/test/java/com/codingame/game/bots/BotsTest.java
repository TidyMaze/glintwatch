package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

class BotsTest {
    private static Board example() {
        Board b = Board.empty(13, 3);
        b.setCrystal(5, 6, 2);
        b.setCrystal(8, 6, 1);
        b.setCrystal(6, 6, 3);
        b.setWall(5, 10);
        return b;
    }

    private static Command[] waits(int n) throws Exception {
        Command[] c = new Command[n];
        for (int i = 0; i < n; i++) c[i] = CommandParser.parse("WAIT");
        return c;
    }

    private static String act(Bot bot, Game g, int id) {
        bot.init(Protocol.initLinesV5(g, id));
        g.beginTurn();
        return bot.act(Protocol.turnLinesV5(g, id));
    }

    @Test
    void botStateRebuildsTheGameFromProtocolLines() throws Exception {
        Board eb = example();
        eb.phase[Geometry.idx(6, 6, 13)] = 31;
        Game g = new Game(eb, 4, Config.silver());
        BotState[] states = new BotState[4];
        for (int v = 0; v < 4; v++) {
            states[v] = new BotState(Config.silver());
            states[v].parseInit(Protocol.initLinesV5(g, v));
        }
        String[][] script = {{"PLACE 5 3 1", "WAIT", "WAIT", "WAIT"}, {"WAIT", "WAIT", "WAIT", "WAIT"}, {"WAIT", "WAIT", "WAIT", "WAIT"},
            {"WAIT", "PLACE 9 6 0", "WAIT", "WAIT"}, {"WAIT", "WAIT", "WAIT", "WAIT"}};
        for (String[] turn : script) {
            g.beginTurn();
            for (int v = 0; v < 4; v++) states[v].parseTurn(Protocol.turnLinesV5(g, v));
            Command[] c = new Command[4];
            for (int v = 0; v < 4; v++) c[v] = CommandParser.parse(turn[v]);
            g.playTurn(c);
        }
        g.beginTurn();
        for (int viewer = 0; viewer < 4; viewer++) {
            BotState s = states[viewer];
            s.parseTurn(Protocol.turnLinesV5(g, viewer));
            Game r = s.game;
            int[] order = Protocol.relativeOrder(g, viewer);
            assertEquals(0, s.myId);
            assertArrayEquals(g.board.kind, r.board.kind);
            assertArrayEquals(g.board.value, r.board.value);
            assertArrayEquals(g.board.mirrorLock, r.board.mirrorLock);
            assertArrayEquals(g.board.pending, r.board.pending);
            assertArrayEquals(g.board.mirrorLife, r.board.mirrorLife);
            for (int p = 0; p < 169; p++) if (g.board.kind[p] == Board.CRYSTAL) assertEquals(g.board.phase[p], r.board.phase[p], "phase " + p);
            assertEquals(g.turn, r.turn);
            for (int k = 0; k < 4; k++) {
                assertEquals(g.seatOf[order[k]], r.seatOf[k]);
                assertEquals(g.score[order[k]], r.score[k]);
                assertEquals(g.mirrorsLeft[order[k]], r.mirrorsLeft[k]);
                assertArrayEquals(g.streak[order[k]], r.streak[k]);
            }
            for (int p = 0; p < 169; p++) {
                int by = g.litBy[p];
                assertEquals(by < 0 ? by : java.util.Arrays.stream(order).boxed().collect(java.util.stream.Collectors.toList()).indexOf(by), r.litBy[p]);
                boolean hidden = g.board.pending[p] > 0 && g.board.lastToucher[p] != viewer;
                assertEquals(hidden, s.unknownType[p]);
                if (!hidden) assertEquals(g.board.mirrorType[p], r.board.mirrorType[p]);
                else assertTrue(r.board.mirrorType[p] >= 0);
            }
        }
    }

    @Test
    void gameCopyIsIndependent() throws Exception {
        Game g = new Game(example(), 4, Config.bronze());
        Game c = g.copy();
        Command[] cmd = waits(4);
        cmd[0] = CommandParser.parse("PLACE 5 3 1");
        c.playTurn(cmd);
        assertEquals(-1, g.board.mirrorType[Geometry.idx(5, 3, 13)]);
        assertEquals(4, g.mirrorsLeft[0]);
        assertEquals(0, g.turn);
    }

    @Test
    void beamRecordsEveryCellItCrosses() {
        Board b = example();
        b.setMirror(5, 3, 1);
        Beam beam = BeamTracer.trace(b, 0, new int[4]);
        assertEquals(5 + 6, beam.cells.size());
        assertEquals(Geometry.idx(1, 3, 13), (int) beam.cells.get(0));
        assertEquals(Geometry.idx(5, 9, 13), (int) beam.cells.get(beam.cells.size() - 1));
    }

    @Test
    void greedyStealsTheRivalLaneOnTheCrossing() throws Exception {
        Board b = example();
        b.setMirror(9, 6, 0);
        Game g = new Game(b, 4, Config.woodOne());
        assertEquals("PLACE 9 3 1", act(new GreedyBot(Config.woodOne(), GreedyBot.Mode.FULL), g, 0));
        assertEquals("PLACE 9 3 1", act(new GreedyBot(Config.woodOne(), GreedyBot.Mode.SELF), g, 0));
        assertEquals("PLACE 9 3 1", act(new GreedyBot(Config.woodOne(), GreedyBot.Mode.PLUS), g, 0));
    }

    @Test
    void greedySelfTakesTheRichestOwnLaneIgnoringContest() throws Exception {
        Game g = new Game(example(), 4, Config.woodOne());
        assertEquals("PLACE 6 3 1", act(new GreedyBot(Config.woodOne(), GreedyBot.Mode.SELF), g, 0));
    }

    @Test
    void greedyWaitsWhenNothingScores() {
        Game g = new Game(Board.empty(13, 3), 4, Config.woodOne());
        assertEquals("WAIT", act(new GreedyBot(Config.woodOne(), GreedyBot.Mode.FULL), g, 0));
    }

    @Test
    void greedyUnderBuildDelayScoresItsOwnMirrorAsIfActive() {
        Game g = new Game(example(), 4, Config.bronze());
        assertEquals("PLACE 6 3 1", act(new GreedyBot(Config.bronze(), GreedyBot.Mode.FULL), g, 0));
    }

    @Test
    void randomAndGreedyBotsNeverSendAnInvalidCommandOverFullGames() throws Exception {
        for (long seed = 0; seed < 6; seed++)
            for (Config cfg : new Config[] {Config.woodTwo(), Config.woodOne(), Config.bronze(), Config.silver()}) {
                Game g = Game.fromSeed(seed, 4, cfg);
                List<Bot> bots = new ArrayList<>();
                bots.add(new RandomBot(cfg, seed));
                bots.add(new GreedyBot(cfg, GreedyBot.Mode.SELF));
                bots.add(new GreedyBot(cfg, GreedyBot.Mode.FULL));
                bots.add(new GreedyBot(cfg, GreedyBot.Mode.PLUS));
                for (int id = 0; id < 4; id++) bots.get(id).init(Protocol.initLinesV5(g, id));
                while (!g.isOver()) {
                    Command[] c = new Command[4];
                    g.beginTurn();
                    for (int id = 0; id < 4; id++) c[id] = CommandParser.parse(bots.get(id).act(Protocol.turnLinesV5(g, id)));
                    TurnReport r = g.playTurn(c);
                    for (int id = 0; id < 4; id++)
                        if (r.invalid[id] != null) assertEquals("collision", r.invalid[id], "seed " + seed + " id " + id);
                }
            }
    }
}
