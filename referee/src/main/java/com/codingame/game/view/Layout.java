package com.codingame.game.view;

import java.util.ArrayList;
import java.util.List;

import com.codingame.game.engine.Beam;
import com.codingame.game.engine.Command;
import com.codingame.game.engine.Geometry;

/** Pixel layout of ux/UX.md sections 1, 2 and 12 (canvas 1920 x 1080); pure math, unit-tested. */
public final class Layout {
    public static final int[] COLOUR = {0x56B4E9, 0xE69F00, 0x009E73, 0xCC79A7};
    public static final int[] COLOUR_DARK = {0x1F5F86, 0x7A5300, 0x004D38, 0x6E3A58};
    public static final int BACKGROUND = 0x1A1D29, INK = 0x0E1018, FRAME = 0x3A4054, FLOOR_0 = 0x1B2D45, FLOOR_1 = 0x18283E,
        ROCK = 0x0A111D, ROCK_RIM = 0x6E7590, PANEL = 0x232736, TEXT = 0xE8EAF0, LABEL = 0xA0A6B8, WHITE = 0xF4F6FF,
        DANGER = 0xFF4D4D, CRYSTAL_GREY = 0x9AA0B4, DORMANT = 0x8E9BB3, SILVER = 0xD8DEEA, LOCK_OFF = 0x55617A;
    public static final int PANEL_W = 380, PANEL_H = 300;
    /** Beam core pattern per player id (UX 2) and emblem sprite per player id (STYLE). */
    public static final String[] BEAM_PATTERN = {"beam_solid.png", "beam_dash.png", "beam_dots.png", "beam_double.png"};
    public static final String[] EMBLEM = {"emblem_ring.png", "emblem_triangle.png", "emblem_square.png", "emblem_star.png"};

    public final int n, cell, x0, y0, beamWidth;

    public Layout(int n) {
        this.n = n;
        cell = 1000 / n;
        x0 = (1920 - n * cell) / 2;
        y0 = 40 + (1000 - n * cell) / 2;
        beamWidth = Math.max(7, (int) Math.round(0.14 * cell));
    }

    /** Constant diagonal lane offset (d, d) of a seat's beam. */
    public double laneOffset(int seat) {
        return (seat - 1.5) * (beamWidth + 2);
    }

    public double[] centre(int x, int y) {
        return new double[] {x0 + (x + 0.5) * cell, y0 + (y + 0.5) * cell};
    }

    public int cellX(int x) {
        return x0 + x * cell;
    }

    public int cellY(int y) {
        return y0 + y * cell;
    }

    /** Panel origin per seat: seat 0 top-left, 1 top-right, 2 bottom-right, 3 bottom-left. */
    public static int[] panelOrigin(int seat) {
        return new int[] {seat == 1 || seat == 2 ? 1500 : 40, seat >= 2 ? 740 : 40};
    }

    /** Lamp centre, each turning mirror centre, then the stop face (rock, lamp) or the board edge. */
    public List<double[]> beamPoints(Beam beam) {
        List<double[]> pts = new ArrayList<>();
        for (int p : beam.corners) pts.add(centre(Geometry.x(p, n), Geometry.y(p, n)));
        double[] end = centre(beam.endX, beam.endY);
        pts.add(new double[] {end[0] - beam.endDir.dx * cell / 2.0, end[1] - beam.endDir.dy * cell / 2.0});
        return pts;
    }

    /** Beam points with the seat's (d, d) lane offset; an exiting beam ends exactly on the board edge. */
    public List<double[]> lanePoints(Beam beam, int seat) {
        double d = laneOffset(seat);
        List<double[]> pts = new ArrayList<>();
        for (double[] p : beamPoints(beam)) pts.add(new double[] {p[0] + d, p[1] + d});
        if (beam.stop == Beam.Stop.EXIT) {
            double[] end = pts.get(pts.size() - 1);
            end[0] -= d * Math.abs(beam.endDir.dx);
            end[1] -= d * Math.abs(beam.endDir.dy);
        }
        return pts;
    }

    /** Turns from the last played turn to the next supply turn (a multiple of every). */
    public static int turnsToSupply(int turnsPlayed, int every) {
        return every - turnsPlayed % every;
    }

    /** Panel hand line (UX 12): "3 /6  +1 in 2"; no countdown without supply or with a full hand. */
    public static String handText(int hand, int max, int every, int turnsToSupply) {
        return every <= 0 || hand >= max ? hand + " /" + max : hand + " /" + max + "  +1 in " + turnsToSupply;
    }

    public double crystalSize(int value) {
        return (0.28 + 0.12 * value) * cell;
    }

    /** The gem is 92 px wide in the 128 px crystal sprite (art STYLE "Sprites v1"). */
    public double crystalScale(int value) {
        return crystalSize(value) / 92;
    }

    /** Chip font size fs = max(17, round(0.32 cell)); chip box 1.45 fs x 1.15 fs (UX 12). */
    public int chipFont() {
        return Math.max(17, (int) Math.round(0.32 * cell));
    }

    public double chipWidth() {
        return 1.45 * chipFont();
    }

    public double chipHeight() {
        return 1.15 * chipFont();
    }

    /** "↓k" sleeps in k turns, "↑k" wakes in k turns, shown only when k <= 3; else "". */
    public static String tideTimer(int turn, int phase, int cycle, int awakeTurns) {
        int m = (turn + phase) % cycle;
        boolean awake = m < awakeTurns;
        int k = awake ? awakeTurns - m : cycle - m;
        return k > 3 ? "" : (awake ? "\u2193" : "\u2191") + k;
    }

    /** Lit lock arcs: none at lock 0, at most 5. */
    public static int litArcs(int lock) {
        return Math.min(5, Math.max(0, lock));
    }

    /** {length, rotation in radians} of a beam segment from a to b. */
    public static double[] segment(double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        return new double[] {Math.hypot(dx, dy), Math.atan2(dy, dx)};
    }

    /** UX 12: life 20-8 intact, 7-4 one crack, 3-1 two cracks; -1 = no wear. */
    public static int wearTier(int life) {
        return life < 0 || life >= 8 ? 0 : life >= 4 ? 1 : 2;
    }

    /** Shown for '\': a raw backslash breaks the SDK text encoding of the viewer (seen 2026-11-11). */
    public static final String BACKSLASH_GLYPH = "\u2572";

    public static String safe(String s) {
        return s.replace("\\", BACKSLASH_GLYPH);
    }

    public static String mirrorGlyph(int type) {
        return type == 0 ? "/" : BACKSLASH_GLYPH;
    }

    /** Panel last-action line (UX 1.1). */
    public static String actionLabel(Command c, String invalid) {
        if (c == null) return "";
        if (invalid != null) return invalid.equals("collision") ? "collision" : "refused: " + invalid;
        if (c.kind == Command.Kind.PLACE) return "PLACE " + c.x + " " + c.y + " " + mirrorGlyph(c.type);
        return c.text();
    }
}
