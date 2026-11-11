package com.codingame.game.view;

import java.util.ArrayList;
import java.util.List;

import com.codingame.game.engine.Beam;
import com.codingame.game.engine.Command;
import com.codingame.game.engine.Dir;
import com.codingame.game.engine.Geometry;

/** Pixel layout of ux/UX.md sections 1, 2 and 12 (canvas 1920 x 1080); pure math, unit-tested. */
public final class Layout {
    public static final int[] COLOUR = {0x56B4E9, 0xE69F00, 0x009E73, 0xCC79A7};
    public static final int[] COLOUR_DARK = {0x1F5F86, 0x7A5300, 0x004D38, 0x6E3A58};
    public static final int BACKGROUND = 0x1A1D29, INK = 0x0E1018, FRAME = 0x3A4054, FLOOR_0 = 0x1B2D45, FLOOR_1 = 0x18283E,
        ROCK = 0x0A111D, ROCK_RIM = 0x6E7590, PANEL = 0x232736, TEXT = 0xE8EAF0, LABEL = 0xA0A6B8, WHITE = 0xF4F6FF,
        DANGER = 0xFF4D4D, CRYSTAL_GREY = 0x9AA0B4, DORMANT = 0x8E9BB3, SILVER = 0xD8DEEA, LOCK_OFF = 0x55617A;
    public static final int PANEL_W = 380, PANEL_H = 310;
    /** UX 14: every canvas text is at least 36 px; turn box and legend sit at y 390. */
    public static final int MIN_FONT = 36, MID_BOX_Y = 390;
    /** UX 16: the board box (contested, awake) replaces the turn box. */
    public static final int BOARD_BOX_Y = 470;
    /** UX 14.3 option B: the streak chip shows one digit at 36 px, chip 36 x 40 centred on the cell corner. */
    public static final int CHIP_W = 36, CHIP_H = 40;
    /** UX 14 fix: coordinate labels keep 6 px from the board; the strike float is bold 40 and rises 30 px. */
    public static final int COORD_GAP = 6, STRIKE_FONT = 40, STRIKE_RISE = 30;
    public static final String[] LEGEND_WORDS = {"pays", "0 pts", "asleep", "pending", "locked", "yard"};
    /** Beam core pattern per player id (UX 2) and emblem sprite per player id (STYLE). */
    public static final String[] BEAM_PATTERN = {"beam_solid.png", "beam_dash.png", "beam_dots.png", "beam_double.png"};
    /** Beam tiles with the glow baked in (32x48: glow band, pattern rows 16-31): one TilingSprite per segment. */
    public static final String[] BEAM_GLOW_PATTERN = {"beam_solid_glow.png", "beam_dash_glow.png", "beam_dots_glow.png", "beam_double_glow.png"};
    public static final String[] EMBLEM = {"emblem_ring.png", "emblem_triangle.png", "emblem_square.png", "emblem_star.png"};

    public final int n, cell, x0, y0, beamWidth;

    public Layout(int n) {
        this.n = n;
        cell = 1000 / n;
        x0 = (1920 - n * cell) / 2;
        y0 = 40 + (1000 - n * cell) / 2;
        beamWidth = Math.max(7, (int) Math.round(0.14 * cell));
    }

    /** Glow tile height 3w: the 16-row core of the 48-row tile is then exactly w. */
    public int beamTileHeight() {
        return 3 * beamWidth;
    }

    public double beamTileScale() {
        return beamTileHeight() / 48.0;
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
        return new int[] {seat == 1 || seat == 2 ? 1500 : 40, seat >= 2 ? 730 : 40};
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

    /** UX 14 fix: two-digit labels touch at N >= 17, so only even indices are labelled there. */
    public static boolean coordLabelShown(int k, int n) {
        return n < 17 || k % 2 == 0;
    }

    /** UX 14.1: a refusal is one word: zone, lock, empty (no mirror in hand) or cell. */
    public static String refusedWord(String reason) {
        if (reason.contains("zone")) return "zone";
        if (reason.contains("locked") || reason.contains("pending")) return "lock";
        if (reason.contains("in hand")) return "empty";
        return "cell";
    }

    /** UX 14.1: the last action as {icon sprite, coordinates or refusal word}. */
    public static String[] actionParts(Command c, String invalid) {
        if (c == null) return new String[] {"", ""};
        if (invalid != null) return new String[] {"icon_refused.png", refusedWord(invalid)};
        if (c.kind == Command.Kind.PLACE) return new String[] {"icon_place.png", c.x + " " + c.y + " " + mirrorGlyph(c.type)};
        if (c.kind == Command.Kind.TURN) return new String[] {"icon_turn.png", c.x + " " + c.y};
        return new String[] {"icon_wait.png", ""};
    }

    /** UX 14.1: the first 14 characters of a message, then "...". */
    public static String shortMessage(String m) {
        return m.length() <= 14 ? m : m.substring(0, 14) + "...";
    }

    public static String streakDigit(int streak) {
        return streak >= 2 ? String.valueOf(streak) : "";
    }

    public double[] streakChipCentre(int x, int y) {
        return new double[] {x0 + (x + 1.0) * cell, y0 + (y + 1.0) * cell};
    }

    /** The tide rule shown in a crystal's tooltip (UX 14.3 B: the timer chip moves to the tooltip). */
    public static String tideRule(int phase, int cycle, int awake) {
        return "awake when (turn + " + phase + ") % " + cycle + " < " + awake;
    }

    /** Panel last-action line (UX 1.1). */
    public static String actionLabel(Command c, String invalid) {
        if (c == null) return "";
        if (invalid != null) return invalid.equals("collision") ? "collision" : "refused: " + invalid;
        if (c.kind == Command.Kind.PLACE) return "PLACE " + c.x + " " + c.y + " " + mirrorGlyph(c.type);
        return c.text();
    }

    /** UX 15: pre-rendered crystal per state (dormant, contested, lit by a player, unlit) and value 1-3. */
    public static String crystalImage(int litBy, boolean awake, int value) {
        return crystalImage(litBy, awake, value, -1, 0);
    }

    /** UX 17: an awake unlit crystal whose best streak (2+, the chip) is fading shows its owner: crystal_decay{owner}. */
    public static String crystalImage(int litBy, boolean awake, int value, int bestId, int bestStreak) {
        String state = !awake ? "dormant" : litBy == -2 ? "contested" : litBy >= 0 ? "lit" + litBy
            : bestId >= 0 && bestStreak >= 2 ? "decay" + bestId : "unlit";
        return "crystal_" + state + "_v" + value + ".png";
    }

    /** One lamp image per direction keeps the emblem upright; an eliminated lamp shows the grey "out" image. */
    public static String lampImage(int id, Dir d, boolean out) {
        return out ? "lamp_out_" + d + ".png" : "lamp_" + id + "_" + d + ".png";
    }

    public static String yardImage(int cellsWide, int cellsHigh) {
        return cellsWide >= cellsHigh ? "yard_h.png" : "yard_v.png";
    }

    public static String coordsImage(boolean top, int n) {
        return (top ? "coords_top_N" : "coords_left_N") + n + ".png";
    }

    /** Panel per player id; the Wood one (no supply) has no supply icon. */
    public static String panelImage(int id, boolean supply) {
        return "panel_" + id + (supply ? "" : "_wood") + ".png";
    }

    /** UX 15.2 chip "x,y,digit,colour,dashed,dimmed": colour 4 = white (contested); a fading streak is dashed and dimmed. */
    public static String chipRecord(int x, int y, int streak, int bestId, int litBy) {
        String digit = streakDigit(streak);
        if (digit.isEmpty()) return "";
        boolean contested = litBy == -2, decaying = !contested && litBy != bestId;
        return x + "," + y + "," + digit + "," + (contested ? 4 : bestId) + "," + (contested || decaying ? 1 : 0) + "," + (decaying ? 1 : 0);
    }

    /** UX 15.2 mirror "x,y,type,pending,toucher,wear,arcs". */
    public static String mirrorRecord(int x, int y, int type, boolean pending, int toucher, int life, int lock) {
        return x + "," + y + "," + type + "," + (pending ? 1 : 0) + "," + toucher + "," + wearTier(life) + "," + litArcs(lock);
    }

    /** Beam polyline "x,y,x,y,..." in rounded canvas pixels. */
    public static String pointsRecord(List<double[]> pts) {
        StringBuilder sb = new StringBuilder();
        for (double[] p : pts) sb.append(sb.length() == 0 ? "" : ",").append(Math.round(p[0])).append(',').append(Math.round(p[1]));
        return sb.toString();
    }

    /**
     * UX 14 fix 2: "+2" float position for shooter id on a lamp firing d (d points into the board): one cell in from
     * the lamp, shooters spread 20 px apart across d, and at rise = 1 it has moved STRIKE_RISE further in.
     */
    public double[] strikeFloat(double[] lamp, Dir d, int id, double rise) {
        double side = (id - 1.5) * 20, in = cell + rise * STRIKE_RISE;
        return new double[] {lamp[0] + d.dx * in - d.dy * side, lamp[1] + d.dy * in + d.dx * side};
    }
}
