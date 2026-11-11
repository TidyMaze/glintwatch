package com.codingame.game.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

class LayoutTest {
    @Test
    void cellSizesAndBoardOriginPerUx1() {
        int[][] expect = {{13, 76, 466, 46}, {15, 66, 465, 45}, {17, 58, 467, 47}, {19, 52, 466, 46}};
        for (int[] e : expect) {
            Layout l = new Layout(e[0]);
            assertEquals(e[1], l.cell);
            assertEquals(e[2], l.x0);
            assertEquals(e[3], l.y0);
        }
    }

    @Test
    void beamWidthIsMaxSevenOrFourteenPercentOfCell() {
        assertEquals(11, new Layout(13).beamWidth);
        assertEquals(9, new Layout(15).beamWidth);
        assertEquals(8, new Layout(17).beamWidth);
        assertEquals(7, new Layout(19).beamWidth);
    }

    @Test
    void laneOffsetIsADiagonalCentredOnSeatOneAndAHalf() {
        Layout l = new Layout(15); // w 9 -> step 11
        assertEquals(-16.5, l.laneOffset(0), 1e-9);
        assertEquals(-5.5, l.laneOffset(1), 1e-9);
        assertEquals(5.5, l.laneOffset(2), 1e-9);
        assertEquals(16.5, l.laneOffset(3), 1e-9);
    }

    @Test
    void panelSlotsSitOnTheSideOfTheirLamp() {
        assertArrayEquals(new int[] {40, 40}, Layout.panelOrigin(0));
        assertArrayEquals(new int[] {1500, 40}, Layout.panelOrigin(1));
        assertArrayEquals(new int[] {1500, 740}, Layout.panelOrigin(2));
        assertArrayEquals(new int[] {40, 740}, Layout.panelOrigin(3));
    }

    @Test
    void beamPolylineGoesLampCentreCornersThenStopFaceOrBoardEdge() {
        Board b = Board.empty(13, 3);
        b.setMirror(5, 3, 1);
        b.setWall(5, 10);
        Beam beam = BeamTracer.trace(b, 0, new int[4]);
        Layout l = new Layout(13);
        List<double[]> pts = l.beamPoints(beam);
        assertEquals(3, pts.size());
        assertArrayEquals(l.centre(0, 3), pts.get(0), 1e-9);
        assertArrayEquals(l.centre(5, 3), pts.get(1), 1e-9);
        double[] wallCentre = l.centre(5, 10);
        assertArrayEquals(new double[] {wallCentre[0], wallCentre[1] - 38}, pts.get(2), 1e-9);
        Beam exit = BeamTracer.trace(Board.empty(13, 3), 0, new int[4]);
        List<double[]> e = l.beamPoints(exit);
        assertEquals(2, e.size());
        assertEquals(l.x0 + 13 * 76, e.get(1)[0], 1e-9);
    }

    @Test
    void beamEndsRememberTheLastDirection() {
        Board b = Board.empty(13, 3);
        b.setMirror(5, 3, 0);
        assertEquals(Dir.N, BeamTracer.trace(b, 0, new int[4]).endDir);
    }

    @Test
    void playerColoursAreTheStylePalette() {
        assertEquals(0x56B4E9, Layout.COLOUR[0]);
        assertEquals(0xE69F00, Layout.COLOUR[1]);
        assertEquals(0x009E73, Layout.COLOUR[2]);
        assertEquals(0xCC79A7, Layout.COLOUR[3]);
    }

    @Test
    void crystalSizeGrowsWithValue() {
        Layout l = new Layout(15);
        assertEquals(0.40 * 66, l.crystalSize(1), 1e-9);
        assertEquals(0.52 * 66, l.crystalSize(2), 1e-9);
        assertEquals(0.64 * 66, l.crystalSize(3), 1e-9);
    }

    @Test
    void wearTierFromLife() {
        assertEquals(0, Layout.wearTier(-1));
        assertEquals(0, Layout.wearTier(20));
        assertEquals(0, Layout.wearTier(8));
        assertEquals(1, Layout.wearTier(7));
        assertEquals(1, Layout.wearTier(4));
        assertEquals(2, Layout.wearTier(3));
        assertEquals(2, Layout.wearTier(1));
    }

    @Test
    void actionLabel() throws Exception {
        assertEquals("PLACE 9 14 /", Layout.actionLabel(CommandParser.parse("PLACE 9 14 0"), null));
        assertEquals("TURN 3 8", Layout.actionLabel(CommandParser.parse("TURN 3 8"), null));
        assertEquals("WAIT", Layout.actionLabel(CommandParser.parse("WAIT"), null));
        assertEquals("collision", Layout.actionLabel(CommandParser.parse("PLACE 1 1 0"), "collision"));
        assertEquals("refused: cell not empty", Layout.actionLabel(CommandParser.parse("PLACE 1 1 0"), "cell not empty"));
        assertEquals("", Layout.actionLabel(null, null));
    }
}

class LayoutSpritesTest {
    @org.junit.jupiter.api.Test
    void crystalSpriteScaleUsesTheNinetyTwoPixelGem() {
        Layout l = new Layout(15);
        org.junit.jupiter.api.Assertions.assertEquals(0.40 * 66 / 92, l.crystalScale(1), 1e-9);
        org.junit.jupiter.api.Assertions.assertEquals(0.64 * 66 / 92, l.crystalScale(3), 1e-9);
    }

    @org.junit.jupiter.api.Test
    void chipFontAndBoxPerUx12() {
        org.junit.jupiter.api.Assertions.assertEquals(21, new Layout(15).chipFont());
        org.junit.jupiter.api.Assertions.assertEquals(17, new Layout(19).chipFont());
        Layout l = new Layout(15);
        org.junit.jupiter.api.Assertions.assertEquals(1.45 * 21, l.chipWidth(), 1e-9);
        org.junit.jupiter.api.Assertions.assertEquals(1.15 * 21, l.chipHeight(), 1e-9);
    }

    @org.junit.jupiter.api.Test
    void tideTimerShowsOnlyChangesWithinThreeTurns() {
        // awake when (t + phase) % 45 < 30
        org.junit.jupiter.api.Assertions.assertEquals("↓3", Layout.tideTimer(27, 0, 45, 30));
        org.junit.jupiter.api.Assertions.assertEquals("↓1", Layout.tideTimer(29, 0, 45, 30));
        org.junit.jupiter.api.Assertions.assertEquals("", Layout.tideTimer(26, 0, 45, 30));
        org.junit.jupiter.api.Assertions.assertEquals("↑2", Layout.tideTimer(43, 0, 45, 30));
        org.junit.jupiter.api.Assertions.assertEquals("", Layout.tideTimer(31, 0, 45, 30));
        org.junit.jupiter.api.Assertions.assertEquals("↑1", Layout.tideTimer(40, 4, 45, 30));
    }

    @org.junit.jupiter.api.Test
    void lockArcsLitOnlyForLocksOneToFive() {
        org.junit.jupiter.api.Assertions.assertEquals(0, Layout.litArcs(0));
        org.junit.jupiter.api.Assertions.assertEquals(3, Layout.litArcs(3));
        org.junit.jupiter.api.Assertions.assertEquals(5, Layout.litArcs(7));
    }

    @org.junit.jupiter.api.Test
    void beamPatternPerPlayerAndSegmentGeometry() {
        org.junit.jupiter.api.Assertions.assertEquals("beam_solid.png", Layout.BEAM_PATTERN[0]);
        org.junit.jupiter.api.Assertions.assertEquals("beam_double.png", Layout.BEAM_PATTERN[3]);
        double[] seg = Layout.segment(new double[] {10, 20}, new double[] {10, 120});
        org.junit.jupiter.api.Assertions.assertEquals(100, seg[0], 1e-9);
        org.junit.jupiter.api.Assertions.assertEquals(Math.PI / 2, seg[1], 1e-9);
        org.junit.jupiter.api.Assertions.assertEquals(Math.PI, Layout.segment(new double[] {50, 0}, new double[] {0, 0})[1], 1e-9);
    }

    @org.junit.jupiter.api.Test
    void emblemSpritePerPlayer() {
        org.junit.jupiter.api.Assertions.assertEquals("emblem_ring.png", Layout.EMBLEM[0]);
        org.junit.jupiter.api.Assertions.assertEquals("emblem_star.png", Layout.EMBLEM[3]);
    }
}

class LayoutTextSafetyTest {
    @org.junit.jupiter.api.Test
    void backslashNeverReachesTheViewerBecauseItBreaksTheSdkTextEncoding() throws Exception {
        String label = Layout.actionLabel(com.codingame.game.engine.CommandParser.parse("PLACE 9 14 1"), null);
        org.junit.jupiter.api.Assertions.assertFalse(label.contains("\\"), label);
        org.junit.jupiter.api.Assertions.assertEquals("PLACE 9 14 " + Layout.BACKSLASH_GLYPH, label);
        org.junit.jupiter.api.Assertions.assertEquals("a" + Layout.BACKSLASH_GLYPH + "b", Layout.safe("a\\b"));
        org.junit.jupiter.api.Assertions.assertEquals(Layout.BACKSLASH_GLYPH, Layout.mirrorGlyph(1));
        org.junit.jupiter.api.Assertions.assertEquals("/", Layout.mirrorGlyph(0));
    }
}

class LayoutLaneTest {
    @org.junit.jupiter.api.Test
    void laneOffsetBeamEndsExactlyOnTheBoardEdgeWhenItExits() {
        Layout l = new Layout(13);
        com.codingame.game.engine.Beam east = com.codingame.game.engine.BeamTracer.trace(com.codingame.game.engine.Board.empty(13, 3), 0, new int[4]);
        for (int seat = 0; seat < 4; seat++) {
            java.util.List<double[]> pts = l.lanePoints(east, seat);
            org.junit.jupiter.api.Assertions.assertEquals(l.x0 + 13 * 76, pts.get(pts.size() - 1)[0], 1e-9, "seat " + seat);
            org.junit.jupiter.api.Assertions.assertEquals(l.centre(0, 3)[1] + l.laneOffset(seat), pts.get(1)[1], 1e-9);
            org.junit.jupiter.api.Assertions.assertEquals(l.centre(0, 3)[0] + l.laneOffset(seat), pts.get(0)[0], 1e-9);
        }
    }

    @org.junit.jupiter.api.Test
    void laneOffsetKeepsInnerStopsUnchanged() {
        com.codingame.game.engine.Board b = com.codingame.game.engine.Board.empty(13, 3);
        b.setWall(6, 3);
        Layout l = new Layout(13);
        java.util.List<double[]> pts = l.lanePoints(com.codingame.game.engine.BeamTracer.trace(b, 0, new int[4]), 2);
        org.junit.jupiter.api.Assertions.assertEquals(l.centre(6, 3)[0] - 38 + l.laneOffset(2), pts.get(1)[0], 1e-9);
    }

    @org.junit.jupiter.api.Test
    void handTextShowsTheNextSupply() {
        org.junit.jupiter.api.Assertions.assertEquals("3 /6  +1 in 2", Layout.handText(3, 6, 4, 2));
        org.junit.jupiter.api.Assertions.assertEquals("6 /6", Layout.handText(6, 6, 4, 0));
        org.junit.jupiter.api.Assertions.assertEquals("7 /12", Layout.handText(7, 12, 0, 5));
        org.junit.jupiter.api.Assertions.assertEquals(4, Layout.turnsToSupply(4, 4));
        org.junit.jupiter.api.Assertions.assertEquals(1, Layout.turnsToSupply(7, 4));
        org.junit.jupiter.api.Assertions.assertEquals(4, Layout.turnsToSupply(8, 4));
    }
}
