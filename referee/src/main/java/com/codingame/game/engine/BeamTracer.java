package com.codingame.game.engine;

/** Beam trace, DESIGN.md section 6.1. */
public final class BeamTracer {
    private BeamTracer() {}

    /** @param seatState per seat: Board.SEAT_ACTIVE, SEAT_INACTIVE (lamp acts as a wall) or SEAT_EMPTY (no lamp). */
    public static Beam trace(Board b, int seat, int[] seatState) {
        return trace(b, seat, seatState, 4 * b.n * b.n);
    }

    /** @param cap step guard; reversible optics from an edge cannot loop, so the default cap is never reached. */
    static Beam trace(Board b, int seat, int[] seatState, int cap) {
        int n = b.n;
        int start = Seats.lampCell(seat, n, b.e);
        Dir d = Seats.lampDir(seat);
        int x = Geometry.x(start, n), y = Geometry.y(start, n);
        Beam beam = new Beam(seat);
        beam.corners.add(start);
        boolean[] seen = new boolean[n * n];
        while (true) {
            x += d.dx;
            y += d.dy;
            beam.steps++;
            if (!Geometry.inside(x, y, n)) {
                beam.stop = Beam.Stop.EXIT;
                break;
            }
            if (beam.steps > cap) throw new IllegalStateException("beam cap reached, seat " + seat);
            int p = Geometry.idx(x, y, n);
            if (b.kind[p] == Board.WALL) {
                beam.stop = Beam.Stop.ABSORBED;
                break;
            }
            if (b.kind[p] == Board.LAMP && seatState[b.lampSeat[p]] != Board.SEAT_EMPTY) {
                int other = b.lampSeat[p];
                boolean strike = other != seat && seatState[other] == Board.SEAT_ACTIVE;
                beam.stop = strike ? Beam.Stop.STRIKE : Beam.Stop.ABSORBED;
                if (strike) beam.struckSeat = other;
                break;
            }
            beam.cells.add(p);
            if (b.kind[p] == Board.CRYSTAL && !seen[p]) {
                seen[p] = true;
                beam.lit.add(p);
            }
            if (b.mirrorType[p] >= 0 && b.pending[p] == 0) {
                d = Optics.reflect(b.mirrorType[p], d);
                beam.corners.add(p);
            }
        }
        beam.endX = x;
        beam.endY = y;
        beam.endDir = d;
        return beam;
    }
}
