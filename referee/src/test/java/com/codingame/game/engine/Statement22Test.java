package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Statement v22 (reviewer 5DN1L and the user-test feedback of 2026-11-15): each claim pinned by the referee code. */
class Statement22Test {
    private static Command c(String s) throws Exception {
        return CommandParser.parse(s);
    }

    /** Wood 1 rules with the top-league strike bonus, mirrors act at once (no build time). */
    private static Config immediate() {
        Config cfg = Config.woodOne();
        cfg.strikeBonus = Config.forLeague(4).strikeBonus;
        return cfg;
    }

    private static Command[] waits(int n) throws Exception {
        Command[] cmds = new Command[n];
        for (int i = 0; i < n; i++) cmds[i] = c("WAIT");
        return cmds;
    }

    /** Refusal text names the yard, as the statement does (it used to say "zone"). */
    @Test
    void refusalInAnotherYardSaysYard() throws Exception {
        Config cfg = immediate();
        cfg.lock = 0;
        Game g = new Game(Board.empty(13, 3), 4, cfg);
        assertEquals("cell in yard of player 0", g.validate(1, c("PLACE 1 3 0")), "PLACE in the yard of player 0");
        g.playTurn(new Command[] {c("PLACE 1 3 0"), c("WAIT"), c("WAIT"), c("WAIT")}); // own yard: allowed
        String turn = g.validate(1, c("TURN 1 3"));
        assertEquals("cell in yard of player 0", turn, "TURN in the yard of player 0");
    }

    /** Rules: a beam that reaches its own lamp stops there, scores the crystals lit on the way and gets no strike. */
    @Test
    void beamOnItsOwnLampScoresWhatItLitAndNoStrike() throws Exception {
        Board b = Board.empty(13, 3);
        b.setCrystal(1, 3, 2); // on the straight line of player 0's lamp (0,3)
        Game g = new Game(b, 4, immediate());
        g.playTurn(new Command[] {c("PLACE 3 3 0"), c("WAIT"), c("WAIT"), c("WAIT")}); // east -> north
        g.playTurn(new Command[] {c("PLACE 3 0 1"), c("WAIT"), c("WAIT"), c("WAIT")}); // north -> west
        TurnReport r = g.playTurn(new Command[] {c("PLACE 0 0 0"), c("WAIT"), c("WAIT"), c("WAIT")}); // west -> south
        Beam beam = r.beams[0];
        assertEquals(Beam.Stop.ABSORBED, beam.stop, "own lamp stops the beam, no strike");
        assertEquals(0, beam.endX);
        assertEquals(3, beam.endY, "the beam ends on its own lamp");
        assertEquals(-1, beam.struckSeat);
        // crystal (1,3) lit on turns 1, 2, 3 by this beam alone: streak 1, 2, 3, value 2: 6 points, and no +4 strike
        assertEquals(6, r.gain[0], "crystals lit on the way score, the own lamp adds nothing");
    }

    /** Rules: an eliminated player's lamp acts as a rock: the beam stops there and lights nothing beyond it. */
    @Test
    void beamStopsAtAnEliminatedLampLikeARock() throws Exception {
        Board b = Board.empty(13, 3);
        b.setCrystal(12, 11, 3); // beyond the lamp of seat 2 at (12,9), on the same column
        Game g = new Game(b, 4, immediate());
        g.playTurn(new Command[] {c("PLACE 12 3 1"), c("WAIT"), c("WAIT"), c("WAIT")}); // east -> south on column 12
        g.deactivate(2);
        TurnReport r = g.playTurn(waits(4));
        Beam beam = r.beams[0];
        assertEquals(Beam.Stop.ABSORBED, beam.stop, "the eliminated lamp stops the beam");
        assertEquals(12, beam.endX);
        assertEquals(9, beam.endY, "the beam ends on the lamp cell (12,9)");
        assertFalse(beam.cells.contains(Geometry.idx(12, 9, 13)), "the lamp is not a cell the beam goes through");
        assertTrue(beam.lit.isEmpty(), "nothing beyond the lamp is lit");
        assertEquals(0, r.gain[0], "no strike on an eliminated lamp");
        assertEquals(-1, g.score[2]);
    }

    /** Same beam with the seat-2 player still in: the lamp is a strike, which proves the elimination is what changed it. */
    @Test
    void sameBeamStrikesWhileTheLampIsActive() throws Exception {
        Board b = Board.empty(13, 3);
        b.setCrystal(12, 11, 3);
        Game g = new Game(b, 4, immediate());
        g.playTurn(new Command[] {c("PLACE 12 3 1"), c("WAIT"), c("WAIT"), c("WAIT")});
        TurnReport r = g.playTurn(waits(4));
        assertEquals(Beam.Stop.STRIKE, r.beams[0].stop);
        assertEquals(2, r.beams[0].struckSeat);
        assertEquals(4, r.gain[0]);
    }

    /** Input: a starting reef mirror shows life 20 on turn 1 in Bronze and Silver (the viewer's -1 is the last-toucher field). */
    @Test
    void startMirrorsShowLifeTwentyOnTurnOneInBronzeAndSilver() {
        int mirrorLines = 0;
        for (int league = 3; league <= 4; league++)
            for (long seed = 0; seed < 20; seed++) {
                Game g = Game.fromSeed(seed, 4, Config.forLeague(league));
                List<String> lines = Protocol.turnLinesV5(g, 0);
                int count = Integer.parseInt(lines.get(1));
                for (int i = 2; i < 2 + count; i++) {
                    mirrorLines++;
                    assertTrue(lines.get(i).endsWith(" 20"), "league " + league + " seed " + seed + ": " + lines.get(i));
                }
            }
        assertTrue(mirrorLines > 0, "the maps have starting mirrors");
    }
}
