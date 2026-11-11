package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class BeamTracerTest {
    private static final int[] ALL_ACTIVE = {Board.SEAT_ACTIVE, Board.SEAT_ACTIVE, Board.SEAT_ACTIVE, Board.SEAT_ACTIVE};

    /** Design section 10 board: N=13, e=3, crystals (5,6)=2 (8,6)=1 (6,6)=3, wall (5,10). */
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

    @Test
    void straightBeamOnEmptyBoardExitsAndLightsNothing() {
        Beam beam = BeamTracer.trace(Board.empty(13, 3), 0, ALL_ACTIVE);
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertTrue(beam.lit.isEmpty());
        assertEquals(13, beam.steps);
        assertEquals(Arrays.asList(at(0, 3)), beam.corners);
        assertEquals(13, beam.endX);
        assertEquals(3, beam.endY);
    }

    @Test
    void workedExampleOneBackslashSendsBeamSouthOntoCrystalThenWall() {
        Board b = example();
        b.setMirror(5, 3, 1);
        Beam beam = BeamTracer.trace(b, 0, ALL_ACTIVE);
        assertEquals(Arrays.asList(at(5, 6)), beam.lit);
        assertEquals(Beam.Stop.ABSORBED, beam.stop);
        assertEquals(Arrays.asList(at(0, 3), at(5, 3)), beam.corners);
        assertEquals(5, beam.endX);
        assertEquals(10, beam.endY);
    }

    @Test
    void workedExampleFourSlashSendsSeatOneWestThroughThreeCrystals() {
        Board b = example();
        b.setMirror(9, 6, 0);
        Beam beam = BeamTracer.trace(b, 1, ALL_ACTIVE);
        assertEquals(Arrays.asList(at(8, 6), at(6, 6), at(5, 6)), beam.lit);
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertEquals(-1, beam.endX);
    }

    @Test
    void workedExampleFourBackslashSendsSeatOneEastOffTheBoard() {
        Board b = example();
        b.setMirror(9, 6, 1);
        Beam beam = BeamTracer.trace(b, 1, ALL_ACTIVE);
        assertTrue(beam.lit.isEmpty());
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertEquals(13, beam.endX);
    }

    @Test
    void beamOnAnotherActiveLampStrikesIt() {
        Board b = Board.empty(13, 3);
        b.setMirror(12, 3, 1); // seat 0 row 3 east, turn south along x=12 to seat 2 lamp (12,9)
        Beam beam = BeamTracer.trace(b, 0, ALL_ACTIVE);
        assertEquals(Beam.Stop.STRIKE, beam.stop);
        assertEquals(2, beam.struckSeat);
    }

    @Test
    void inactiveLampAbsorbsWithoutStrike() {
        Board b = Board.empty(13, 3);
        b.setMirror(12, 3, 1);
        int[] states = ALL_ACTIVE.clone();
        states[2] = Board.SEAT_INACTIVE;
        Beam beam = BeamTracer.trace(b, 0, states);
        assertEquals(Beam.Stop.ABSORBED, beam.stop);
        assertEquals(-1, beam.struckSeat);
    }

    @Test
    void emptySeatLampCellLetsTheBeamPass() {
        Board b = Board.empty(13, 3);
        b.setMirror(12, 3, 1);
        int[] states = ALL_ACTIVE.clone();
        states[2] = Board.SEAT_EMPTY;
        Beam beam = BeamTracer.trace(b, 0, states);
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertEquals(13, beam.endY);
    }

    @Test
    void reversibleOpticsNeverBringABeamBackToItsOwnLamp() {
        Board b = Board.empty(13, 3);
        // (4,3) \ E->S, (4,5) / S->W, (1,5) \ W->N: passes x=1 beside the lamp and exits north
        b.setMirror(4, 3, 1);
        b.setMirror(4, 5, 0);
        b.setMirror(1, 5, 1);
        Beam beam = BeamTracer.trace(b, 0, ALL_ACTIVE);
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertEquals(Arrays.asList(at(0, 3), at(4, 3), at(4, 5), at(1, 5)), beam.corners);
    }

    @Test
    void crystalCrossedTwiceIsLitOnce() {
        Board b = Board.empty(13, 3);
        b.setCrystal(6, 3, 1);
        b.setCrystal(6, 5, 1);
        // row 3 east through (6,3); (8,3) \ -> S; (8,7) / -> W; (6,7) \ -> N up column 6 through (6,5) and (6,3)
        b.setMirror(8, 3, 1);
        b.setMirror(8, 7, 0);
        b.setMirror(6, 7, 1);
        Beam beam = BeamTracer.trace(b, 0, ALL_ACTIVE);
        assertEquals(Arrays.asList(at(6, 3), at(6, 5)), beam.lit);
        assertEquals(Beam.Stop.EXIT, beam.stop);
        assertEquals(-1, beam.endY);
    }

    @Test
    void firstBeamsOfGeneratedMapsAreStraightAndScoreless() {
        for (long s = 0; s < 200; s++) {
            Board b = MapGenerator.generate(s, false);
            for (int seat = 0; seat < 4; seat++) {
                Beam beam = BeamTracer.trace(b, seat, ALL_ACTIVE);
                assertEquals(Beam.Stop.EXIT, beam.stop, "seed " + s + " seat " + seat);
                assertTrue(beam.lit.isEmpty());
                assertEquals(1, beam.corners.size());
            }
        }
    }

    @Test
    void stepCapGuardThrows() {
        assertThrows(IllegalStateException.class, () -> BeamTracer.trace(Board.empty(13, 3), 0, ALL_ACTIVE, 5));
    }

    @Test
    void randomMirrorFieldsAlwaysTerminateUnderTheCap() {
        Random r = new Random(7);
        for (int g = 0; g < 500; g++) {
            Board b = MapGenerator.generate(g, false);
            for (int k = 0; k < b.n * b.n / 2; k++) {
                int p = r.nextInt(b.n * b.n);
                if (b.kind[p] == Board.EMPTY && b.mirrorType[p] < 0) b.mirrorType[p] = r.nextInt(2);
            }
            for (int seat = 0; seat < 4; seat++) {
                Beam beam = BeamTracer.trace(b, seat, ALL_ACTIVE);
                assertTrue(beam.steps <= 4 * b.n * b.n);
                assertNotEquals(seat, beam.struckSeat);
            }
        }
    }

    @Test
    void traceIsSymmetricUnderRotationOnSymmetricMirrorFields() {
        Random r = new Random(11);
        for (long s = 0; s < 300; s++) {
            Board b = MapGenerator.generate(s, false);
            for (int k = 0; k < b.n * b.n / 8; k++) {
                int p = r.nextInt(b.n * b.n), t = r.nextInt(2);
                if (b.kind[p] != Board.EMPTY || b.mirrorType[p] >= 0) continue;
                for (int rot = 0; rot < 4; rot++, p = Geometry.rot(p, b.n)) b.mirrorType[p] = rot % 2 == 0 ? t : 1 - t;
            }
            Beam b0 = BeamTracer.trace(b, 0, ALL_ACTIVE);
            for (int seat = 1; seat < 4; seat++) {
                Beam bs = BeamTracer.trace(b, seat, ALL_ACTIVE);
                assertEquals(b0.steps, bs.steps, "seed " + s);
                assertEquals(b0.stop, bs.stop);
                assertEquals(b0.lit.size(), bs.lit.size());
                for (int i = 0; i < b0.lit.size(); i++) {
                    int q = b0.lit.get(i);
                    for (int rot = 0; rot < seat; rot++) q = Geometry.rot(q, b.n);
                    assertEquals(q, bs.lit.get(i).intValue());
                }
                if (b0.stop == Beam.Stop.STRIKE) assertEquals((b0.struckSeat + seat) % 4, bs.struckSeat);
            }
        }
    }
}
