package com.codingame.game.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codingame.game.Player;
import com.codingame.game.engine.*;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.*;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;

/** Viewer with the art v1 sprites (art/png), laid out per ux/UX.md sections 1, 2, 8 and 12. */
public final class Viewer {
    private static final String FONT = "Arial";
    /** Floor texture: the artist's seamless 2x2 checker (256 px = 2 cells). */
    private static final String FLOOR_IMAGE = "floor_checker.png";
    private static final double FLOOR_TILE_CELL = 128.0;

    private final GraphicEntityModule g;
    private final TooltipModule tips;
    private final ToggleModule toggles;
    private final MultiplayerGameManager<Player> gm;
    private final Game game;
    private final Layout l;
    private final int n;

    private final Map<Integer, CrystalView> crystals = new HashMap<>();
    private final Map<Integer, MirrorView> mirrors = new HashMap<>();
    /** Views of broken mirrors, reused for new ones (creating sprites costs view data). */
    private final List<MirrorView> freeMirrors = new ArrayList<>();
    private final List<List<Entity<?>[]>> beamPool = new ArrayList<>();
    private final Map<Integer, String> lastBeam = new HashMap<>();
    private final PanelView[] panels;
    private final Sprite[] lampCross = new Sprite[4];
    private Text turnText, nextMirrorText, contestedText, awakeText;
    private Rectangle progress;

    public Viewer(GraphicEntityModule g, TooltipModule tips, ToggleModule toggles, MultiplayerGameManager<Player> gm, Game game) {
        this.g = g;
        this.tips = tips;
        this.toggles = toggles;
        this.gm = gm;
        this.game = game;
        this.n = game.board.n;
        this.l = new Layout(n);
        panels = new PanelView[game.playerCount];
    }

    private String name(int id) {
        return gm.getPlayer(id).getNicknameToken();
    }

    /** Static tooltips and labels, sent with the first turn: frame 0 must stay under the runner's 30,000-byte read. */
    private List<Runnable> deferred = new ArrayList<>();

    /** Only called from init(): the list is drained (and dropped) by the first update(). */
    private void later(Runnable r) {
        deferred.add(r);
    }

    private void tip(Entity<?> e, String text) {
        later(() -> tips.setTooltipText(e, text));
    }

    /** Last value sent per entity and property: the SDK serialises every set, even an unchanged one. */
    private final Map<Entity<?>, Map<String, Object>> sent = new HashMap<>();

    private boolean changed(Entity<?> e, String prop, Object v) {
        Map<String, Object> m = sent.computeIfAbsent(e, k -> new HashMap<>());
        if (v.equals(m.get(prop))) return false;
        m.put(prop, v);
        return true;
    }

    private void visible(Entity<?> e, boolean v) {
        if (changed(e, "v", v)) e.setVisible(v);
    }

    private void alpha(Entity<?> e, double a) {
        if (changed(e, "a", a)) e.setAlpha(a);
    }

    private void tint(SpriteBasedEntity<?> e, int c) {
        if (changed(e, "t", c)) e.setTint(c);
    }

    private void textOf(Text e, String t) {
        if (changed(e, "T", t)) e.setText(t);
    }

    private void fill(Text e, int c) {
        if (changed(e, "f", c)) e.setFillColor(c);
    }

    /** Centred sprite without an anchor (anchors cost view bytes): top-left = centre - size / 2. */
    private Sprite spriteAt(String image, double cx, double cy, double w, double h) {
        int iw = (int) Math.round(w), ih = (int) Math.round(h);
        return g.createSprite().setImage(image).setX((int) Math.round(cx - iw / 2.0)).setY((int) Math.round(cy - ih / 2.0))
            .setBaseWidth(iw).setBaseHeight(ih);
    }

    private Sprite sprite(String image, double w, double h) {
        return g.createSprite().setImage(image).setAnchor(0.5).setBaseWidth((int) Math.round(w)).setBaseHeight((int) Math.round(h));
    }

    private Text text(String s, int size, int colour) {
        return g.createText(s).setFontFamily(FONT).setFontSize(size).setFillColor(colour);
    }

    public void init() {
        g.createSprite().setImage("background.png").setZIndex(0);
        g.createRectangle().setX(460).setY(40).setWidth(1000).setHeight(1000).setFillColor(Layout.INK)
            .setLineColor(Layout.FRAME).setLineWidth(2).setZIndex(10);
        Board b = game.board;
        // one tiling floor (a per-cell floor costs ~27 K chars of frame 0, capped at 100 K by the SDK)
        g.createTilingSprite().setImage(FLOOR_IMAGE).setX(l.x0).setY(l.y0).setBaseWidth(n * l.cell).setBaseHeight(n * l.cell)
            .setTileScale(l.cell / FLOOR_TILE_CELL).setZIndex(11);
        later(this::coordinateLabels);
        later(this::legend);
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++) {
                int p = Geometry.idx(x, y, n);
                double[] c = l.centre(x, y);
                if (b.kind[p] == Board.WALL) {
                    Sprite rock = spriteAt("rock.png", c[0], c[1], l.cell, l.cell).setZIndex(30);
                    tip(rock, "(" + x + ", " + y + ") rock");
                }
                if (b.kind[p] == Board.CRYSTAL) crystals.put(p, new CrystalView(p));
                if (b.mirrorType[p] >= 0) mirror(p).show();
            }
        for (int id = 0; id < game.playerCount; id++) yard(id);
        for (int id = 0; id < game.playerCount; id++) {
            lamp(id);
            panels[id] = new PanelView(id);
            beamPool.add(new ArrayList<>());
        }
        turnBox();
        for (CrystalView cv : crystals.values()) cv.refresh();
        for (MirrorView m : mirrors.values()) {
            m.refreshType();
            m.refresh();
        }
        refreshTurnBox();
    }

    /** Legend box (UX 1): board-sized icons (24-28 px), one row per state, the yard rule, then the beam key. */
    private void legend() {
        g.createRectangle().setX(1500).setY(380).setWidth(380).setHeight(320).setFillColor(Layout.PANEL).setLineColor(Layout.FRAME)
            .setLineWidth(3).setZIndex(90);
        int ix = 1534, tx = 1562;
        String[] texts = {"lit by one: pays", "lit by 2+: pays nobody", "dormant: pays nothing", "pending: reflects in 2 turns",
            "lock: turns before TURN", "streak multiplier", "yard: owner only"};
        for (int k = 0; k < texts.length; k++) {
            int y = 392 + 34 * k, cy = y + 13;
            switch (k) {
                case 0: spriteAt("crystal.png", ix, cy, 30, 30).setTint(Layout.COLOUR[0]).setZIndex(91); break;
                case 1:
                    spriteAt("crystal.png", ix, cy, 30, 30).setTint(Layout.WHITE).setZIndex(91);
                    spriteAt("badge_zero.png", ix + 10, cy - 10, 14, 14).setZIndex(92);
                    break;
                case 2: spriteAt("crystal_outline.png", ix, cy, 30, 30).setZIndex(91); break;
                case 3: spriteAt("mirror_pending.png", ix, cy, 28, 28).setTint(Layout.COLOUR[1]).setZIndex(91); break;
                case 4:
                    spriteAt("mirror_base.png", ix, cy, 28, 28).setZIndex(91);
                    for (int a = 0; a < 5; a++)
                        sprite("lock_arc.png", 28, 28).setX(ix).setY(cy).setRotation(Math.toRadians(72 * a))
                            .setTint(a < 3 ? Layout.COLOUR[2] : Layout.LOCK_OFF).setZIndex(92);
                    break;
                case 5:
                    spriteAt("streak_badge.png", ix, cy, 32, 25).setTint(Layout.COLOUR[3]).setZIndex(91);
                    text("x3", 16, Layout.WHITE).setX(ix).setY(cy).setAnchor(0.5).setFontWeight(Text.FontWeight.BOLD).setZIndex(92);
                    break;
                default: spriteAt("zone_tile.png", ix, cy, 24, 24).setTint(Layout.COLOUR[0]).setAlpha(0.4).setZIndex(91);
            }
            text(texts[k], 22, Layout.TEXT).setX(tx).setY(y).setZIndex(91);
        }
        text("empty seat: open to all", 18, Layout.LABEL).setX(tx).setY(392 + 34 * 6 + 26).setZIndex(91);
        for (int id = 0; id < game.playerCount; id++)
            g.createTilingSprite().setImage(Layout.BEAM_PATTERN[id]).setTint(Layout.COLOUR[id]).setX(1522).setY(652 + 10 * id)
                .setBaseWidth(80).setBaseHeight(8).setTileScale(0.5).setZIndex(91);
        text("beams: colour + pattern", 20, Layout.LABEL).setX(1612).setY(656).setZIndex(91);
    }

    private void coordinateLabels() {
        for (int k = 0; k < n; k++) {
            toggles.displayOnToggleState(text(String.valueOf(k), 20, Layout.LABEL).setX(l.cellX(k) + l.cell / 2).setY(l.y0 - 4)
                .setAnchorX(0.5).setAnchorY(1).setZIndex(100), "coords", true);
            toggles.displayOnToggleState(text(String.valueOf(k), 20, Layout.LABEL).setX(l.x0 - 8).setY(l.cellY(k) + l.cell / 2)
                .setAnchorX(1).setAnchorY(0.5).setZIndex(100), "coords", true);
        }
    }

    /** A yard is the board-clipped Chebyshev-2 square around the lamp: one tinted sprite. */
    private void yard(int id) {
        int lamp = Seats.lampCell(game.seatOf[id], n, game.board.e), r = Seats.ZONE_RADIUS;
        int x0 = Math.max(0, Geometry.x(lamp, n) - r), x1 = Math.min(n - 1, Geometry.x(lamp, n) + r);
        int y0 = Math.max(0, Geometry.y(lamp, n) - r), y1 = Math.min(n - 1, Geometry.y(lamp, n) + r);
        Sprite yard = g.createSprite().setImage("zone_tile.png").setX(l.cellX(x0)).setY(l.cellY(y0))
            .setBaseWidth((x1 - x0 + 1) * l.cell).setBaseHeight((y1 - y0 + 1) * l.cell).setTint(Layout.COLOUR[id]).setAlpha(0.16)
            .setZIndex(20);
        tip(yard, "yard of " + name(id));
        toggles.displayOnToggleState(yard, "yards", true);
    }

    private void lamp(int id) {
        int seat = game.seatOf[id], p = Seats.lampCell(seat, n, game.board.e);
        double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
        Dir d = Seats.lampDir(seat);
        int x = (int) c[0], y = (int) c[1];
        sprite("lamp_nose.png", l.cell, l.cell).setX(x).setY(y).setRotation(Math.atan2(d.dy, d.dx)).setTint(Layout.COLOUR[id]).setZIndex(70);
        Sprite disc = sprite("lamp_disc.png", l.cell, l.cell).setX(x).setY(y).setTint(Layout.COLOUR[id]).setZIndex(71);
        sprite(Layout.EMBLEM[id], 0.55 * l.cell, 0.55 * l.cell).setX(x).setY(y).setTint(Layout.COLOUR[id]).setZIndex(72);
        lampCross[id] = sprite("emblem_cross.png", 0.55 * l.cell, 0.55 * l.cell).setX(x).setY(y).setVisible(false).setZIndex(73);
        tip(disc, name(id) + " lamp (" + Geometry.x(p, n) + ", " + Geometry.y(p, n) + "), fires " + d);
    }

    private void turnBox() {
        g.createRectangle().setX(40).setY(380).setWidth(380).setHeight(320).setFillColor(Layout.PANEL).setLineColor(Layout.FRAME)
            .setLineWidth(3).setZIndex(90);
        text("TURN", 26, Layout.LABEL).setX(68).setY(400).setFontWeight(Text.FontWeight.BOLD).setZIndex(91);
        turnText = text("0 / " + game.cfg.maxTurns, 72, Layout.TEXT).setX(68).setY(432).setFontWeight(Text.FontWeight.BOLD).setZIndex(91);
        g.createRectangle().setX(68).setY(530).setWidth(324).setHeight(14).setFillColor(Layout.INK).setLineWidth(0).setZIndex(91);
        progress = g.createRectangle().setX(68).setY(530).setWidth(0).setHeight(14).setFillColor(Layout.WHITE).setLineWidth(0).setZIndex(92);
        nextMirrorText = text("", 26, Layout.TEXT).setX(68).setY(566).setZIndex(91);
        contestedText = text("", 26, Layout.TEXT).setX(68).setY(606).setZIndex(91);
        awakeText = text("", 26, Layout.TEXT).setX(68).setY(646).setZIndex(91);
    }

    /** Phases (UX 3): t 0 actions, t 0.3 mirrors turn and beams and crystals snap, t 0.6 locks and wear; one world commit each. */
    public void update(TurnReport r) {
        if (deferred != null) {
            List<Runnable> todo = deferred;
            deferred = null;
            for (Runnable job : todo) job.run();
        }
        for (int p : r.placed) mirror(p).show().group.setScale(0);
        for (int id = 0; id < game.playerCount; id++) panels[id].update(r);
        g.commitWorldState(0);
        for (int p : r.placed) mirror(p).group.setScale(1, Curve.EASE_OUT);
        for (MirrorView m : mirrors.values()) m.refreshType();
        drawBeams(r);
        for (CrystalView c : crystals.values()) c.refresh();
        g.commitWorldState(0.3);
        for (MirrorView m : new ArrayList<>(mirrors.values())) m.refresh();
        g.commitWorldState(0.6);
        for (int id = 0; id < game.playerCount; id++) if (!game.active[id] && !lampCross[id].isVisible()) lampCross[id].setVisible(true);
        refreshTurnBox();
    }

    private void refreshTurnBox() {
        int t = game.turn, max = game.cfg.maxTurns;
        turnText.setText(t + " / " + max);
        progress.setWidth(324 * t / max);
        int every = game.cfg.supplyEvery;
        nextMirrorText.setText(every > 0 ? "Next mirror (all) in " + Layout.turnsToSupply(t, every) : "Mirrors: " + game.cfg.mirrorStock + " each");
        int contested = 0, awake = 0;
        for (int p : crystals.keySet()) {
            if (game.litBy[p] == -2) contested++;
            if (game.awake(p, Math.max(1, t))) awake++;
        }
        contestedText.setText("Contested " + contested);
        awakeText.setText("Awake " + awake + " / " + crystals.size());
    }

    private void drawBeams(TurnReport r) {
        for (int id = 0; id < game.playerCount; id++) {
            List<Entity<?>[]> pool = beamPool.get(id);
            Beam beam = r.beams[id];
            List<double[]> pts = beam == null ? new ArrayList<>() : l.lanePoints(beam, game.seatOf[id]);
            StringBuilder key = new StringBuilder();
            for (double[] pt : pts) key.append((int) pt[0]).append(',').append((int) pt[1]).append(';');
            if (key.toString().equals(lastBeam.get(id))) continue;
            lastBeam.put(id, key.toString());
            int segs = Math.max(0, pts.size() - 1), w = l.beamWidth;
            while (pool.size() < segs) {
                Line glow = g.createLine().setLineColor(Layout.COLOUR[id]).setAlpha(0.22).setLineWidth(2 * w + 6).setZIndex(40);
                TilingSprite core = g.createTilingSprite().setImage(Layout.BEAM_PATTERN[id]).setTint(Layout.COLOUR[id])
                    .setBaseHeight(w).setTileScale(w / 16.0).setAnchorY(0.5).setZIndex(41);
                toggles.displayOnToggleState(glow, "beam" + id, true);
                toggles.displayOnToggleState(core, "beam" + id, true);
                pool.add(new Entity<?>[] {glow, core});
            }
            for (int k = 0; k < pool.size(); k++) {
                Line glow = (Line) pool.get(k)[0];
                TilingSprite core = (TilingSprite) pool.get(k)[1];
                boolean on = k < segs;
                if (on) {
                    double[] a = pts.get(k), b = pts.get(k + 1), seg = Layout.segment(a, b);
                    int ax = (int) Math.round(a[0]), ay = (int) Math.round(a[1]);
                    int bx = (int) Math.round(b[0]), by = (int) Math.round(b[1]), len = (int) Math.round(seg[0]);
                    if (changed(glow, "x", ax)) glow.setX(ax, Curve.NONE);
                    if (changed(glow, "y", ay)) glow.setY(ay, Curve.NONE);
                    if (changed(glow, "X", bx)) glow.setX2(bx, Curve.NONE);
                    if (changed(glow, "Y", by)) glow.setY2(by, Curve.NONE);
                    if (changed(core, "x", ax)) core.setX(ax, Curve.NONE);
                    if (changed(core, "y", ay)) core.setY(ay, Curve.NONE);
                    if (changed(core, "bw", len)) core.setBaseWidth(len);
                    if (changed(core, "r", seg[1])) core.setRotation(seg[1], Curve.NONE);
                }
                visible(glow, on);
                visible(core, on);
            }
        }
    }

    private MirrorView mirror(int p) {
        MirrorView m = mirrors.get(p);
        if (m == null) {
            m = freeMirrors.isEmpty() ? new MirrorView(p) : freeMirrors.remove(freeMirrors.size() - 1).moveTo(p);
            mirrors.put(p, m);
        }
        return m;
    }

    private final class CrystalView {
        final int p;
        final double cx, cy, size;
        final Sprite body;
        final Text digit;
        final String tip;
        /** Created on first use: most crystals never need them, and frame 0 is limited. */
        Sprite outline, glow, zero, streakChip, dashedChip, timerChip, ownerBadge, contestRing;
        Text streakText, timerText;
        String lastState = "";

        CrystalView(int p) {
            this.p = p;
            double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
            cx = c[0];
            cy = c[1];
            int v = game.board.value[p];
            size = l.crystalScale(v) * 128;
            body = spriteAt("crystal.png", cx, cy, size, size).setTint(Layout.CRYSTAL_GREY).setZIndex(51);
            // default font: a font family on 61 digits costs ~0.7 KB of the limited frame 0
            digit = g.createText(String.valueOf(v)).setFontSize((int) (0.30 * l.cell)).setFillColor(Layout.INK).setX((int) cx)
                .setY((int) cy).setAnchor(0.5).setFontWeight(Text.FontWeight.BOLD).setZIndex(52);
            tip = "(" + Geometry.x(p, n) + "," + Geometry.y(p, n) + ") " + v + " pt" + (game.cfg.tides ? ", phase " + game.board.phase[p] : "");
            tip(body, tip);
        }

        Sprite outline() {
            if (outline == null) {
                outline = spriteAt("crystal_outline.png", cx, cy, size, size).setZIndex(51);
                tips.setTooltipText(outline, tip);
            }
            return outline;
        }

        Sprite glow() {
            if (glow == null) glow = spriteAt("crystal_glow.png", cx, cy, size * 1.4, size * 1.4).setAlpha(0).setZIndex(50);
            return glow;
        }

        /** UX 2: owner emblem top-right of a crystal lit by one player, so colour is not the only cue. */
        Sprite ownerBadge() {
            if (ownerBadge == null)
                // the 48 px emblem art has padding: 34 px gives a ~24 px visible shape (UX r 12)
                ownerBadge = spriteAt(Layout.EMBLEM[0], cx + 0.30 * l.cell, cy - 0.30 * l.cell, 34, 34).setZIndex(53);
            return ownerBadge;
        }

        /** UX 2: dashed white ring around a contested crystal (the tintable dashed ring of mirror_pending). */
        Sprite contestRing() {
            if (contestRing == null)
                contestRing = spriteAt("mirror_pending.png", cx, cy, 0.9 * l.cell, 0.9 * l.cell).setTint(Layout.WHITE).setZIndex(50);
            return contestRing;
        }

        Sprite zero() {
            if (zero == null) zero = spriteAt("badge_zero.png", cx + 0.32 * l.cell, cy - 0.32 * l.cell, 0.34 * l.cell, 0.34 * l.cell).setZIndex(53);
            return zero;
        }

        double chipY() {
            return cy + l.cell / 2.0 - l.chipHeight() / 2;
        }

        Sprite chip(boolean dashed) {
            double x = cx + l.cell / 2.0 - l.chipWidth() / 2;
            if (dashed && dashedChip == null) dashedChip = spriteAt("streak_badge_dashed.png", x, chipY(), l.chipWidth(), l.chipHeight()).setZIndex(54);
            if (!dashed && streakChip == null) streakChip = spriteAt("streak_badge.png", x, chipY(), l.chipWidth(), l.chipHeight()).setZIndex(54);
            return dashed ? dashedChip : streakChip;
        }

        Text streakText() {
            if (streakText == null)
                streakText = text("", l.chipFont(), Layout.WHITE).setX((int) (cx + l.cell / 2.0 - l.chipWidth() / 2)).setY((int) chipY())
                    .setAnchor(0.5).setFontWeight(Text.FontWeight.BOLD).setZIndex(55);
            return streakText;
        }

        Sprite timerChip() {
            if (timerChip == null) {
                double x = cx - l.cell / 2.0 + l.chipWidth() / 2;
                timerChip = spriteAt("streak_badge.png", x, chipY(), l.chipWidth(), l.chipHeight()).setTint(Layout.DORMANT).setZIndex(54);
                timerText = text("", l.chipFont(), Layout.WHITE).setX((int) x).setY((int) chipY()).setAnchor(0.5)
                    .setFontWeight(Text.FontWeight.BOLD).setZIndex(55);
            }
            return timerChip;
        }

        void refresh() {
            int by = game.litBy[p], t = Math.max(1, game.turn);
            boolean awake = game.awake(p, t);
            int fill = by >= 0 ? Layout.COLOUR[by] : by == -2 ? Layout.WHITE : Layout.CRYSTAL_GREY;
            int best = 0, bestId = -1;
            for (int id = 0; id < game.playerCount; id++)
                if (game.streak[id][p] > best) {
                    best = game.streak[id][p];
                    bestId = id;
                }
            boolean contested = by == -2, decaying = bestId >= 0 && by != bestId && !contested;
            String streak = best >= 2 ? "x" + best : "";
            String timer = game.cfg.tides ? Layout.tideTimer(t + 1, game.board.phase[p], game.cfg.tideCycle, game.cfg.tideAwake) : "";
            String state = fill + " " + awake + " " + streak + " " + bestId + " " + contested + " " + decaying + " " + timer;
            if (state.equals(lastState)) return;
            lastState = state;
            tint(body, fill);
            visible(body, awake);
            if (!awake) visible(outline(), true);
            else if (outline != null) visible(outline, false);
            fill(digit, awake ? Layout.INK : Layout.DORMANT);
            boolean glowing = by >= 0 && awake, white = contested && awake;
            if (glowing || white) {
                alpha(glow(), 0.6);
                tint(glow, fill);
            } else if (glow != null) alpha(glow, 0.0);
            if (white) visible(contestRing(), true);
            else if (contestRing != null) visible(contestRing, false);
            if (glowing) {
                Sprite badge = ownerBadge();
                visible(badge, true);
                if (changed(badge, "i", Layout.EMBLEM[by])) badge.setImage(Layout.EMBLEM[by]);
                tint(badge, Layout.COLOUR[by]);
            } else if (ownerBadge != null) visible(ownerBadge, false);
            if (contested && awake) visible(zero(), true);
            else if (zero != null) visible(zero, false);
            boolean dashed = contested || decaying;
            if (!streak.isEmpty()) {
                Sprite chip = chip(dashed);
                visible(chip, true);
                tint(chip, contested ? Layout.WHITE : Layout.COLOUR[bestId]);
                alpha(chip, decaying ? 0.6 : 1.0);
                alpha(streakText(), decaying ? 0.6 : 1.0);
                textOf(streakText, streak);
            } else if (streakText != null) textOf(streakText, "");
            if (dashed || streak.isEmpty()) if (streakChip != null) visible(streakChip, false);
            if (!dashed || streak.isEmpty()) if (dashedChip != null) visible(dashedChip, false);
            if (!timer.isEmpty()) {
                visible(timerChip(), true);
                textOf(timerText, timer);
            } else if (timerChip != null) {
                visible(timerChip, false);
                textOf(timerText, "");
            }
        }
    }

    private final class MirrorView {
        int p;
        final Group group, barGroup;
        final Sprite base, bar;
        /** Created on first use (frame 0 is limited). */
        Sprite pendingRing, crack1, crack2;
        Sprite[] arcs;
        int shownType = -2;
        String lastState = "", lastTip = "";

        MirrorView(int p) {
            this.p = p;
            double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
            int s = l.cell, h = -s / 2;
            base = g.createSprite().setImage("mirror_base.png").setX(h).setY(h).setBaseWidth(s).setBaseHeight(s);
            bar = g.createSprite().setImage("mirror_bar.png").setX(h).setY(h).setBaseWidth(s).setBaseHeight(s);
            barGroup = g.createGroup(bar);
            group = g.createGroup(base, barGroup).setX((int) c[0]).setY((int) c[1]).setZIndex(60);
        }

        private Sprite overlay(String image, Group into) {
            int s = l.cell, h = -s / 2;
            Sprite sp = g.createSprite().setImage(image).setX(h).setY(h).setBaseWidth(s).setBaseHeight(s);
            into.add(sp);
            return sp;
        }

        Sprite pendingRing() {
            if (pendingRing == null) pendingRing = overlay("mirror_pending.png", group);
            return pendingRing;
        }

        Sprite crack(int k) {
            if (k == 1 && crack1 == null) crack1 = overlay("mirror_crack_1.png", barGroup);
            if (k == 2 && crack2 == null) crack2 = overlay("mirror_crack_2.png", barGroup);
            return k == 1 ? crack1 : crack2;
        }

        Sprite[] arcs() {
            if (arcs == null) {
                arcs = new Sprite[5];
                for (int k = 0; k < 5; k++) {
                    arcs[k] = sprite("lock_arc.png", l.cell, l.cell).setRotation(Math.toRadians(72 * k));
                    toggles.displayOnToggleState(arcs[k], "locks", true);
                    group.add(arcs[k]);
                }
            }
            return arcs;
        }

        MirrorView show() {
            visible(group, true);
            return this;
        }

        MirrorView moveTo(int cell) {
            p = cell;
            double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
            group.setX((int) c[0], Curve.NONE).setY((int) c[1], Curve.NONE);
            shownType = -2;
            lastState = "";
            lastTip = "";
            return this;
        }

        /** Phase 0.3: a new or turned mirror shows its type. */
        void refreshType() {
            int type = game.board.mirrorType[p];
            if (type < 0 || type == shownType) return;
            barGroup.setRotation(type == 0 ? 0 : Math.PI / 2, shownType == -2 ? Curve.NONE : Curve.EASE_OUT);
            shownType = type;
        }

        /** Phase 0.6: pending, wear, locks; a broken mirror disappears and its view is pooled. */
        void refresh() {
            Board b = game.board;
            if (b.mirrorType[p] < 0) {
                visible(group, false);
                mirrors.remove(p);
                freeMirrors.add(this);
                return;
            }
            int type = b.mirrorType[p];
            boolean pending = b.pending[p] > 0;
            int toucher = b.lastToucher[p], wear = Layout.wearTier(b.mirrorLife[p]), lit = Layout.litArcs(b.mirrorLock[p]);
            int colour = toucher >= 0 ? Layout.COLOUR[toucher] : Layout.WHITE;
            String state = pending + " " + toucher + " " + wear + " " + lit;
            if (!state.equals(lastState)) {
                lastState = state;
                if (pending) {
                    alpha(pendingRing(), 1.0);
                    tint(pendingRing, colour);
                } else if (pendingRing != null) alpha(pendingRing, 0.0);
                alpha(bar, pending ? 0.4 : wear == 2 ? 0.65 : 1.0);
                if (!pending && wear == 1) visible(crack(1), true);
                else if (crack1 != null) visible(crack1, false);
                if (!pending && wear == 2) visible(crack(2), true);
                else if (crack2 != null) visible(crack2, false);
                if (lit > 0)
                    for (int k = 0; k < 5; k++) {
                        visible(arcs()[k], true);
                        tint(arcs[k], k < lit ? colour : Layout.LOCK_OFF);
                    }
                else if (arcs != null) for (Sprite a : arcs) visible(a, false);
            }
            int now = game.turn;
            String tip = "(" + Geometry.x(p, n) + ", " + Geometry.y(p, n) + ") mirror " + Layout.mirrorGlyph(type)
                + (pending ? "\npending (reflects next turn)\ntype hidden from rivals" : "")
                + (b.mirrorLock[p] > 0 ? "\nlocked: free from turn " + (now + 1 + b.mirrorLock[p]) : "\nfree")
                + (b.mirrorLife[p] >= 0 && !pending ? "\nbreaks after turn " + (now + b.mirrorLife[p]) : "")
                + (toucher >= 0 ? "\nlast touched by " + name(toucher) : "");
            if (!tip.equals(lastTip)) {
                lastTip = tip;
                tips.setTooltipText(base, tip);
            }
        }
    }

    private final class PanelView {
        final int id;
        final Group group;
        final Text score, hand, lit, action, message, gain;
        final Sprite refused;
        final Map<Text, String> shown = new HashMap<>();

        PanelView(int id) {
            this.id = id;
            int[] o = Layout.panelOrigin(game.seatOf[id]);
            int c = Layout.COLOUR[id];
            Player pl = gm.getPlayer(id);
            List<Entity<?>> parts = new ArrayList<>();
            parts.add(g.createSprite().setImage("panel_fill.png"));
            parts.add(g.createSprite().setImage("panel_frame.png").setTint(c));
            parts.add(g.createSprite().setImage(pl.getAvatarToken()).setX(28).setY(24).setBaseWidth(112).setBaseHeight(112));
            parts.add(g.createCircle().setX(136).setY(132).setRadius(24).setFillColor(Layout.INK).setLineColor(c).setLineWidth(3));
            parts.add(sprite(Layout.EMBLEM[id], 30, 30).setX(136).setY(132).setTint(c));
            parts.add(text(pl.getNicknameToken(), 30, Layout.TEXT).setX(156).setY(28).setFontWeight(Text.FontWeight.BOLD).setMaxWidth(200));
            score = text("0", 72, c).setX(172).setY(66).setFontWeight(Text.FontWeight.BOLD);
            parts.add(sprite("icon_mirror.png", 30, 30).setX(44).setY(182));
            hand = text("", 26, Layout.TEXT).setX(66).setY(168).setFontWeight(Text.FontWeight.BOLD);
            parts.add(sprite("icon_crystal.png", 30, 30).setX(270).setY(182).setTint(c));
            lit = text("", 26, Layout.TEXT).setX(290).setY(168).setFontWeight(Text.FontWeight.BOLD);
            parts.add(g.createRectangle().setX(28).setY(214).setWidth(324).setHeight(2).setFillColor(Layout.FRAME).setLineWidth(0));
            refused = spriteAt("icon_refused.png", 44, 244, 28, 28).setVisible(false);
            parts.add(refused);
            action = text("", 28, Layout.TEXT).setX(64).setY(226).setFontWeight(Text.FontWeight.BOLD);
            gain = text("", 34, Layout.WHITE).setX(352).setY(226).setAnchorX(1).setFontWeight(Text.FontWeight.BOLD);
            message = text("", 24, Layout.LABEL).setX(28).setY(262);
            parts.add(score);
            parts.add(hand);
            parts.add(lit);
            parts.add(action);
            parts.add(gain);
            parts.add(message);
            group = g.createGroup(parts.toArray(new Entity<?>[0])).setX(o[0]).setY(o[1]).setZIndex(90);
            set(hand, handText());
        }

        void set(Text t, String v) {
            if (!v.equals(shown.get(t))) {
                shown.put(t, v);
                t.setText(v);
            }
        }

        String handText() {
            int every = game.cfg.supplyEvery;
            return Layout.handText(game.mirrorsLeft[id], game.cfg.handMax, every, every > 0 ? Layout.turnsToSupply(game.turn, every) : 0);
        }

        void update(TurnReport r) {
            set(score, String.valueOf(game.score[id]));
            set(hand, handText());
            int alone = 0;
            if (r.beams[id] != null) for (int p : r.beams[id].lit) if (game.litBy[p] == id) alone++;
            set(lit, alone + " lit");
            if (!game.active[id]) {
                if (!"ELIMINATED".equals(shown.get(action))) {
                    action.setFillColor(Layout.DANGER);
                    group.setAlpha(0.4);
                }
                set(action, "ELIMINATED");
                set(message, "");
                set(gain, "");
                return;
            }
            Command c = r.commands[id];
            String label = Layout.actionLabel(c, r.invalid[id]);
            if (!label.equals(shown.get(action))) action.setFillColor(r.invalid[id] != null ? Layout.DANGER : Layout.TEXT);
            visible(refused, r.invalid[id] != null);
            set(action, label);
            String msg = Layout.safe(c.message); // an active player always has a parsed command
            set(message, msg.length() > 24 ? msg.substring(0, 24) : msg);
            set(gain, r.gain[id] > 0 ? "+" + r.gain[id] : "");
            if (!msg.isEmpty()) tips.setTooltipText(group, msg);
        }
    }
}
