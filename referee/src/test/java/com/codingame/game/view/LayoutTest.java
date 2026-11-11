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
        assertArrayEquals(new int[] {1500, 730}, Layout.panelOrigin(2));
        assertArrayEquals(new int[] {40, 730}, Layout.panelOrigin(3));
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

/** UX.md 14 (owner order): canvas text >= 36 px, icons instead of words, chips option B. */
class LayoutUx14Test {
    @org.junit.jupiter.api.Test
    void panelsAre380By310AtY40AndY730() {
        org.junit.jupiter.api.Assertions.assertEquals(310, Layout.PANEL_H);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {40, 40}, Layout.panelOrigin(0));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {1500, 730}, Layout.panelOrigin(2));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {40, 730}, Layout.panelOrigin(3));
        org.junit.jupiter.api.Assertions.assertEquals(390, Layout.MID_BOX_Y);
        org.junit.jupiter.api.Assertions.assertEquals(36, Layout.MIN_FONT);
    }

    @org.junit.jupiter.api.Test
    void refusedReasonsBecomeOneWord() {
        org.junit.jupiter.api.Assertions.assertEquals("zone", Layout.refusedWord("cell in zone of player 2"));
        org.junit.jupiter.api.Assertions.assertEquals("lock", Layout.refusedWord("mirror locked 3 more turns"));
        org.junit.jupiter.api.Assertions.assertEquals("lock", Layout.refusedWord("mirror still pending"));
        org.junit.jupiter.api.Assertions.assertEquals("empty", Layout.refusedWord("no mirror in hand"));
        org.junit.jupiter.api.Assertions.assertEquals("cell", Layout.refusedWord("cell not empty"));
        org.junit.jupiter.api.Assertions.assertEquals("cell", Layout.refusedWord("collision"));
        org.junit.jupiter.api.Assertions.assertEquals("cell", Layout.refusedWord("no mirror there"));
    }

    @org.junit.jupiter.api.Test
    void actionIsAnIconPlusCoordinates() throws Exception {
        String[] place = Layout.actionParts(com.codingame.game.engine.CommandParser.parse("PLACE 10 12 1"), null);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"icon_place.png", "10 12 " + Layout.BACKSLASH_GLYPH}, place);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"icon_turn.png", "3 8"},
            Layout.actionParts(com.codingame.game.engine.CommandParser.parse("TURN 3 8"), null));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"icon_wait.png", ""},
            Layout.actionParts(com.codingame.game.engine.CommandParser.parse("WAIT"), null));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"icon_refused.png", "zone"},
            Layout.actionParts(com.codingame.game.engine.CommandParser.parse("PLACE 1 1 0"), "cell in zone of player 1"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"", ""}, Layout.actionParts(null, null));
    }

    @org.junit.jupiter.api.Test
    void messageShowsFourteenCharsThenAnEllipsis() {
        org.junit.jupiter.api.Assertions.assertEquals("hello", Layout.shortMessage("hello"));
        org.junit.jupiter.api.Assertions.assertEquals("12345678901234...", Layout.shortMessage("1234567890123456789"));
        org.junit.jupiter.api.Assertions.assertEquals("12345678901234", Layout.shortMessage("12345678901234"));
    }

    @org.junit.jupiter.api.Test
    void streakChipIsADigitOnlyAt36PxOnTheCellCorner() {
        Layout l = new Layout(19);
        org.junit.jupiter.api.Assertions.assertEquals("4", Layout.streakDigit(4));
        org.junit.jupiter.api.Assertions.assertEquals("", Layout.streakDigit(1));
        org.junit.jupiter.api.Assertions.assertEquals(36, Layout.CHIP_W);
        org.junit.jupiter.api.Assertions.assertEquals(40, Layout.CHIP_H);
        double[] c = l.centre(5, 5);
        double[] chip = l.streakChipCentre(5, 5);
        org.junit.jupiter.api.Assertions.assertEquals(c[0] + l.cell / 2.0, chip[0], 1e-9, "centred on the corner");
        org.junit.jupiter.api.Assertions.assertEquals(c[1] + l.cell / 2.0, chip[1], 1e-9);
    }

    @org.junit.jupiter.api.Test
    void tideRuleForTheTooltip() {
        org.junit.jupiter.api.Assertions.assertEquals("awake when (turn + 7) % 45 < 30", Layout.tideRule(7, 45, 30));
    }

    @org.junit.jupiter.api.Test
    void legendIsSixIconWords() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] {"pays", "0 pts", "asleep", "pending", "locked", "yard"}, Layout.LEGEND_WORDS);
    }
}

class LayoutUx14FixesTest {
    @org.junit.jupiter.api.Test
    void coordinatesAtSizeSeventeenAndUpLabelEvenIndicesOnly() {
        for (int k = 0; k < 15; k++) org.junit.jupiter.api.Assertions.assertTrue(Layout.coordLabelShown(k, 15));
        org.junit.jupiter.api.Assertions.assertTrue(Layout.coordLabelShown(0, 19));
        org.junit.jupiter.api.Assertions.assertFalse(Layout.coordLabelShown(1, 19));
        org.junit.jupiter.api.Assertions.assertTrue(Layout.coordLabelShown(18, 19));
        org.junit.jupiter.api.Assertions.assertFalse(Layout.coordLabelShown(11, 17));
        org.junit.jupiter.api.Assertions.assertEquals(6, Layout.COORD_GAP);
    }

    @org.junit.jupiter.api.Test
    void strikeFloatIs40PxAndRisesThirtyPixels() {
        org.junit.jupiter.api.Assertions.assertEquals(40, Layout.STRIKE_FONT);
        org.junit.jupiter.api.Assertions.assertEquals(30, Layout.STRIKE_RISE);
    }
}

/** Lead option (a): the glow is baked into the beam tiles (artist): one TilingSprite per segment, 3w tall. */
class LayoutGlowTilesTest {
    @org.junit.jupiter.api.Test
    void glowTilesPerPlayerAndHeightThreeTimesTheBeamWidth() {
        org.junit.jupiter.api.Assertions.assertEquals("beam_solid_glow.png", Layout.BEAM_GLOW_PATTERN[0]);
        org.junit.jupiter.api.Assertions.assertEquals("beam_double_glow.png", Layout.BEAM_GLOW_PATTERN[3]);
        Layout l = new Layout(15); // w = 9
        org.junit.jupiter.api.Assertions.assertEquals(27, l.beamTileHeight());
        org.junit.jupiter.api.Assertions.assertEquals(27 / 48.0, l.beamTileScale(), 1e-9);
    }

    /** UX 15 (art STYLE): one pre-rendered crystal image per state and value. */
    @Test
    void crystalImageNamesTheStateAndTheValue() {
        assertEquals("crystal_dormant_v2.png", Layout.crystalImage(1, false, 2));
        assertEquals("crystal_contested_v3.png", Layout.crystalImage(-2, true, 3));
        assertEquals("crystal_lit3_v1.png", Layout.crystalImage(3, true, 1));
        assertEquals("crystal_unlit_v1.png", Layout.crystalImage(-1, true, 1));
    }

    /** UX 17: an unlit crystal whose best streak (2+) is fading shows the owner emblem (crystal_decay{owner}). */
    @Test
    void decayingUnlitCrystalShowsItsOwner() {
        assertEquals("crystal_decay3_v2.png", Layout.crystalImage(-1, true, 2, 3, 2));
        assertEquals("crystal_unlit_v2.png", Layout.crystalImage(-1, true, 2, 3, 1));
        assertEquals("crystal_lit1_v2.png", Layout.crystalImage(1, true, 2, 3, 4));
        assertEquals("crystal_contested_v2.png", Layout.crystalImage(-2, true, 2, 3, 4));
        assertEquals("crystal_dormant_v2.png", Layout.crystalImage(-1, false, 2, 3, 4));
        assertEquals("crystal_unlit_v2.png", Layout.crystalImage(-1, true, 2, -1, 0));
    }

    @Test
    void lampYardAndCoordinateImagesFollowTheSeatAndTheBoard() {
        assertEquals("lamp_2_W.png", Layout.lampImage(2, Dir.W, false));
        assertEquals("lamp_out_N.png", Layout.lampImage(2, Dir.N, true));
        assertEquals("yard_h.png", Layout.yardImage(5, 3));
        assertEquals("yard_v.png", Layout.yardImage(3, 5));
        assertEquals("coords_top_N17.png", Layout.coordsImage(true, 17));
        assertEquals("coords_left_N13.png", Layout.coordsImage(false, 13));
        assertEquals("panel_1_wood.png", Layout.panelImage(1, false));
        assertEquals("panel_3.png", Layout.panelImage(3, true));
    }

    /** UX 15.2 chip record: x, y, digit, colour (0-3 player, 4 white), dashed, dimmed; empty below a streak of 2. */
    @Test
    void chipRecordFollowsUx14ChipRules() {
        assertEquals("", Layout.chipRecord(3, 4, 1, 0, 0));
        assertEquals("3,4,2,0,0,0", Layout.chipRecord(3, 4, 2, 0, 0));
        assertEquals("3,4,3,4,1,0", Layout.chipRecord(3, 4, 3, 1, -2));
        assertEquals("3,4,4,1,1,1", Layout.chipRecord(3, 4, 4, 1, 2));
        assertEquals("3,4,4,1,1,1", Layout.chipRecord(3, 4, 4, 1, -1));
    }

    /** UX 15.2 mirror record: x, y, type, pending, toucher, wear tier, lit lock arcs. */
    @Test
    void mirrorRecordPacksTheMirrorState() {
        assertEquals("1,2,0,1,-1,0,5", Layout.mirrorRecord(1, 2, 0, true, -1, -1, 9));
        assertEquals("1,2,1,0,3,2,0", Layout.mirrorRecord(1, 2, 1, false, 3, 2, 0));
    }

    @Test
    void beamRecordRoundsThePoints() {
        assertEquals("1,2,30,41", Layout.pointsRecord(List.of(new double[] {1.4, 2.0}, new double[] {29.6, 40.5})));
        assertEquals("", Layout.pointsRecord(List.of()));
    }

    /** Every image the viewer and the board module can name is shipped, and nothing else (UX 14.4 asset budget). */
    @Test
    void shippedAssetsAreExactlyTheImagesTheViewerNames() throws Exception {
        java.util.Set<String> named = new java.util.TreeSet<>(List.of("background.jpg", "floor_checker.png", "rock.png", "legend.png",
            "open_yard.png", "logo.png", "mirror_base.png", "mirror_bar.png", "mirror_crack_1.png", "mirror_crack_2.png", "mirror_pending.png",
            "lock_arc.png", "streak_badge.png", "streak_badge_dashed.png", "icon_place.png", "icon_turn.png", "icon_wait.png", "icon_refused.png"));
        for (String b : Layout.BEAM_GLOW_PATTERN) named.add(b);
        for (int v = 1; v <= 3; v++)
            for (int by = -2; by < 4; by++) {
                named.add(Layout.crystalImage(by, true, v));
                named.add(Layout.crystalImage(by, false, v));
                if (by >= 0) named.add(Layout.crystalImage(-1, true, v, by, 2));
            }
        for (int id = 0; id < 4; id++)
            for (Dir d : Dir.values()) {
                named.add(Layout.lampImage(id, d, false));
                named.add(Layout.lampImage(id, d, true));
            }
        for (int id = 0; id < 4; id++) for (boolean s : new boolean[] {true, false}) named.add(Layout.panelImage(id, s));
        named.add("boardbox.png");
        for (int n = 13; n <= 19; n += 2) for (boolean top : new boolean[] {true, false}) named.add(Layout.coordsImage(top, n));
        named.add(Layout.yardImage(5, 3));
        named.add(Layout.yardImage(3, 5));
        java.util.Set<String> shipped = new java.util.TreeSet<>();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(java.nio.file.Paths.get("src/main/resources/view/assets"))) {
            files.forEach(f -> shipped.add(f.getFileName().toString()));
        }
        assertEquals(named, shipped);
        String module = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/view/board-module/BoardModule.js")));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([a-z_0-9]+\\.png)'").matcher(module);
        while (m.find()) assertTrue(shipped.contains(m.group(1)), m.group(1));
    }

    /** UX 14 fix 2: the "+2" float starts one cell inside the board from the struck lamp, rises further in; shooters side by side. */
    @Test
    void strikeFloatStaysInsideTheBoard() {
        Layout l = new Layout(19); // cell 52
        double[] lamp = {100, 50};
        assertArrayEquals(new double[] {100 + 30, 50 + 52}, l.strikeFloat(lamp, Dir.S, 0, 0), 1e-9);
        assertArrayEquals(new double[] {100 - 30, 50 + 52 + Layout.STRIKE_RISE}, l.strikeFloat(lamp, Dir.S, 3, 1), 1e-9);
        assertArrayEquals(new double[] {100 - 52, 50 + 10}, l.strikeFloat(lamp, Dir.W, 1, 0), 1e-9);
        assertArrayEquals(new double[] {100 + 52, 50 + 10}, l.strikeFloat(lamp, Dir.E, 2, 0), 1e-9);
    }
}
