package com.codingame.game.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.codingame.game.Player;
import com.codingame.game.engine.*;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.*;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;

/**
 * Viewer per ux/UX.md 1, 2, 8, 12, 14 and 15. UX 15: at most 200 GEM entities over a game, all created in init();
 * mirrors, beams and streak chips are drawn by {@link BoardModule}; static words and icons are baked in the art.
 */
public final class Viewer {
    private static final String FONT = "Arial";
    /** Floor texture: the artist's seamless 2x2 checker (256 px = 2 cells). */
    private static final String FLOOR_IMAGE = "floor_checker.png";
    private static final double FLOOR_TILE_CELL = 128.0;
    /** Pre-rendered crystals are 160 px for a 128 px cell (art STYLE "UX 15"). */
    private static final double CRYSTAL_IMAGE = 160.0;

    private final GraphicEntityModule g;
    private final TooltipModule tips;
    private final ToggleModule toggles;
    private final BoardModule board;
    private final MultiplayerGameManager<Player> gm;
    private final Game game;
    private final Layout l;
    private final int n;
    private final boolean supply;

    private final Map<Integer, CrystalView> crystals = new TreeMap<>();
    private final PanelView[] panels;
    private final Sprite[] lamps = new Sprite[4];
    /** UX 14 fix: "+2" strike float, one pooled Text per player (bold 40, shooter colour, rises at t 0.3-0.6). */
    private final Text[] strikeFloat = new Text[4];
    private Text contestedText, awakeText;

    public Viewer(GraphicEntityModule g, TooltipModule tips, ToggleModule toggles, BoardModule board, MultiplayerGameManager<Player> gm,
        Game game) {
        this.g = g;
        this.tips = tips;
        this.toggles = toggles;
        this.board = board;
        this.gm = gm;
        this.game = game;
        this.n = game.board.n;
        this.l = new Layout(n);
        this.supply = game.cfg.supplyEvery > 0;
        panels = new PanelView[game.playerCount];
    }

    private String name(int id) {
        return gm.getPlayer(id).getNicknameToken();
    }

    /** Static tooltips, sent with the first turn: frame 0 must stay under the runner's 30,000-byte read. */
    private List<Runnable> deferred = new ArrayList<>();

    private void tip(Entity<?> e, String text) {
        deferred.add(() -> tips.setTooltipText(e, text));
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

    private void image(Sprite e, String image) {
        if (changed(e, "i", image)) e.setImage(image);
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

    private Text text(String s, int size, int colour) {
        return g.createText(s).setFontFamily(FONT).setFontSize(size).setFillColor(colour);
    }

    public void init() {
        g.createSprite().setImage("background.jpg").setZIndex(0);
        g.createRectangle().setX(460).setY(40).setWidth(1000).setHeight(1000).setFillColor(Layout.INK)
            .setLineColor(Layout.FRAME).setLineWidth(2).setZIndex(10);
        // one tiling floor (a per-cell floor costs ~27 K chars of frame 0, capped at 100 K by the SDK)
        g.createTilingSprite().setImage(FLOOR_IMAGE).setX(l.x0).setY(l.y0).setBaseWidth(n * l.cell).setBaseHeight(n * l.cell)
            .setTileScale(l.cell / FLOOR_TILE_CELL).setZIndex(11);
        toggles.displayOnToggleState(g.createSprite().setImage(Layout.coordsImage(true, n)).setX(l.x0).setY(l.y0 - 40).setZIndex(100),
            "coords", true);
        toggles.displayOnToggleState(g.createSprite().setImage(Layout.coordsImage(false, n)).setX(l.x0 - 48).setY(l.y0).setZIndex(100),
            "coords", true);
        g.createSprite().setImage("legend.png").setX(1500).setY(Layout.MID_BOX_Y).setZIndex(90);
        Board b = game.board;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++) {
                int p = Geometry.idx(x, y, n);
                if (b.kind[p] == Board.WALL) {
                    double[] c = l.centre(x, y);
                    tip(spriteAt("rock.png", c[0], c[1], l.cell, l.cell).setZIndex(30), "(" + x + ", " + y + ") rock");
                }
                if (b.kind[p] == Board.CRYSTAL) crystals.put(p, new CrystalView(p));
            }
        // UX 15.2: the board module draws into these three GEM groups, so beams stay under crystals and chips over mirrors
        int beams = g.createGroup().setZIndex(41).getId(), mirrors = g.createGroup().setZIndex(60).getId(),
            chips = g.createGroup().setZIndex(62).getId();
        board.setup(new int[] {n, l.cell, l.x0, l.y0, l.beamWidth, game.playerCount}, new int[] {beams, mirrors, chips}, mirrorsRecord());
        for (int id = 0; id < game.playerCount; id++) yard(id);
        for (int id = 0; id < game.playerCount; id++)
            strikeFloat[id] = text("+" + game.cfg.strikeBonus, Layout.STRIKE_FONT, Layout.COLOUR[id]).setAnchor(0.5)
                .setFontWeight(Text.FontWeight.BOLD).setStrokeColor(Layout.INK).setStrokeThickness(6).setAlpha(0).setZIndex(80);
        for (int id = 0; id < game.playerCount; id++) {
            lamp(id);
            panels[id] = new PanelView(id);
        }
        boardBox();
        emptySeats();
        refreshBoardBox();
    }

    /** UX 15: an empty seat's panel slot shows the pre-rendered "open yard" box (its yard is open to all). */
    private void emptySeats() {
        boolean[] held = new boolean[4];
        for (int seat : game.seatOf) held[seat] = true;
        for (int seat = 0; seat < 4; seat++) {
            if (held[seat]) continue;
            int[] o = Layout.panelOrigin(seat);
            g.createSprite().setImage("open_yard.png").setX(o[0]).setY(o[1]).setZIndex(90);
        }
    }

    /** A yard is the board-clipped Chebyshev-2 square around the lamp: one tinted sprite (fill and dashes baked). */
    private void yard(int id) {
        int lamp = Seats.lampCell(game.seatOf[id], n, game.board.e), r = Seats.ZONE_RADIUS;
        int x0 = Math.max(0, Geometry.x(lamp, n) - r), x1 = Math.min(n - 1, Geometry.x(lamp, n) + r);
        int y0 = Math.max(0, Geometry.y(lamp, n) - r), y1 = Math.min(n - 1, Geometry.y(lamp, n) + r);
        Sprite yard = g.createSprite().setImage(Layout.yardImage(x1 - x0 + 1, y1 - y0 + 1)).setX(l.cellX(x0)).setY(l.cellY(y0))
            .setBaseWidth((x1 - x0 + 1) * l.cell).setBaseHeight((y1 - y0 + 1) * l.cell).setTint(Layout.COLOUR[id]).setZIndex(20);
        tip(yard, "yard of " + name(id));
        toggles.displayOnToggleState(yard, "yards", true);
    }

    private void lamp(int id) {
        int seat = game.seatOf[id], p = Seats.lampCell(seat, n, game.board.e);
        double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
        Dir d = Seats.lampDir(seat);
        lamps[id] = spriteAt(Layout.lampImage(id, d, false), c[0], c[1], l.cell, l.cell).setZIndex(70);
        tip(lamps[id], name(id) + " lamp (" + Geometry.x(p, n) + ", " + Geometry.y(p, n) + "), fires " + d);
    }

    /** UX 16 board box (40, 470) 380 x 140: contested count and awake/total, bold 36; the CG player shows the turn. */
    private void boardBox() {
        int y = Layout.BOARD_BOX_Y;
        g.createSprite().setImage("boardbox.png").setX(40).setY(y).setZIndex(90);
        contestedText = text("", Layout.MIN_FONT, Layout.TEXT).setX(120).setY(y + 28).setFontWeight(Text.FontWeight.BOLD).setZIndex(91);
        awakeText = text("", Layout.MIN_FONT, Layout.TEXT).setX(120).setY(y + 84).setFontWeight(Text.FontWeight.BOLD).setZIndex(91);
    }

    /** Phases (UX 3): t 0 actions, t 0.3 beams and crystals snap, t 0.6 locks and wear (the board module follows them). */
    public void update(TurnReport r) {
        if (deferred != null) {
            List<Runnable> todo = deferred;
            deferred = null;
            for (Runnable job : todo) job.run();
        }
        for (int id = 0; id < game.playerCount; id++) panels[id].update(r);
        g.commitWorldState(0);
        for (CrystalView c : crystals.values()) c.refresh();
        int[] struck = new int[game.playerCount];
        for (int id = 0; id < game.playerCount; id++) {
            struck[id] = r.beams[id] != null && r.beams[id].stop == Beam.Stop.STRIKE && game.cfg.strikeBonus > 0 ? r.beams[id].struckSeat : -1;
            if (struck[id] < 0) continue;
            int p = Seats.lampCell(struck[id], n, game.board.e);
            double[] c = l.strikeFloat(l.centre(Geometry.x(p, n), Geometry.y(p, n)), Seats.lampDir(struck[id]), id, 0);
            strikeFloat[id].setX((int) c[0], Curve.NONE).setY((int) c[1], Curve.NONE).setAlpha(1, Curve.NONE);
        }
        g.commitWorldState(0.3);
        for (int id = 0; id < game.playerCount; id++)
            if (struck[id] >= 0) {
                int p = Seats.lampCell(struck[id], n, game.board.e);
                double[] c = l.strikeFloat(l.centre(Geometry.x(p, n), Geometry.y(p, n)), Seats.lampDir(struck[id]), id, 1);
                strikeFloat[id].setX((int) c[0]).setY((int) c[1]).setAlpha(0);
            }
        g.commitWorldState(0.6);
        for (int id = 0; id < game.playerCount; id++)
            if (!game.active[id]) image(lamps[id], Layout.lampImage(id, Seats.lampDir(game.seatOf[id]), true));
        sendBoard(r);
        refreshBoardBox();
    }

    /** UX 15.2 frame data: mirrors "m", beams "b0".."b3", streak chips "c"; the module sends a key only when it changed. */
    private void sendBoard(TurnReport r) {
        board.set("m", mirrorsRecord());
        for (int id = 0; id < game.playerCount; id++) {
            Beam beam = r.beams[id];
            board.set("b" + id, beam == null ? "" : Layout.pointsRecord(l.lanePoints(beam, game.seatOf[id])));
        }
        StringBuilder c = new StringBuilder();
        for (CrystalView cv : crystals.values()) {
            String chip = cv.chip();
            if (chip.isEmpty()) continue;
            if (c.length() > 0) c.append(';');
            c.append(chip);
        }
        board.set("c", c.toString());
    }

    private String mirrorsRecord() {
        Board b = game.board;
        StringBuilder m = new StringBuilder();
        for (int p = 0; p < n * n; p++) {
            if (b.mirrorType[p] < 0) continue;
            if (m.length() > 0) m.append(';');
            m.append(Layout.mirrorRecord(Geometry.x(p, n), Geometry.y(p, n), b.mirrorType[p], b.pending[p] > 0, b.lastToucher[p],
                b.mirrorLife[p], b.mirrorLock[p]));
        }
        return m.toString();
    }

    private void refreshBoardBox() {
        int t = game.turn;
        int contested = 0, awake = 0;
        for (int p : crystals.keySet()) {
            if (game.litBy[p] == -2) contested++;
            if (game.awake(p, Math.max(1, t))) awake++;
        }
        textOf(contestedText, String.valueOf(contested));
        textOf(awakeText, awake + "/" + crystals.size());
    }

    /** UX 15: one sprite per crystal; glow, emblem, ring and "0" badge are baked into its image. */
    private final class CrystalView {
        final int p;
        final Sprite body;

        CrystalView(int p) {
            this.p = p;
            double[] c = l.centre(Geometry.x(p, n), Geometry.y(p, n));
            double size = CRYSTAL_IMAGE * l.cell / 128;
            body = spriteAt(currentImage(), c[0], c[1], size, size).setZIndex(51);
            changed(body, "i", currentImage());
            int x = Geometry.x(p, n), y = Geometry.y(p, n), v = game.board.value[p];
            tip(body, "(" + x + "," + y + ") " + v + " pt"
                + (game.cfg.tides ? "\n" + Layout.tideRule(game.board.phase[p], game.cfg.tideCycle, game.cfg.tideAwake) : ""));
        }

        String currentImage() {
            int[] best = best();
            return Layout.crystalImage(game.litBy[p], game.awake(p, Math.max(1, game.turn)), game.board.value[p], best[1], best[0]);
        }

        /** {best streak, its player} on this crystal ({0, -1} when nobody has one). */
        int[] best() {
            int best = 0, bestId = -1;
            for (int id = 0; id < game.playerCount; id++)
                if (game.streak[id][p] > best) {
                    best = game.streak[id][p];
                    bestId = id;
                }
            return new int[] {best, bestId};
        }

        void refresh() {
            image(body, currentImage());
        }

        /** The best streak on this crystal as a chip record ("" below 2). */
        String chip() {
            int[] best = best();
            return Layout.chipRecord(Geometry.x(p, n), Geometry.y(p, n), best[0], best[1], game.litBy[p]);
        }
    }

    private final class PanelView {
        final int id;
        final Group group;
        final Text score, hand, supplyText, lit, action, message, gain;
        final Sprite actionIcon;
        final Map<Text, String> shown = new HashMap<>();

        PanelView(int id) {
            this.id = id;
            int[] o = Layout.panelOrigin(game.seatOf[id]);
            int c = Layout.COLOUR[id], f = Layout.MIN_FONT;
            Player pl = gm.getPlayer(id);
            List<Entity<?>> parts = new ArrayList<>();
            // the avatar sits under the panel image, which has a hole at its box and the emblem badge baked on top
            parts.add(g.createSprite().setImage(pl.getAvatarToken()).setX(28).setY(24).setBaseWidth(112).setBaseHeight(112));
            parts.add(g.createSprite().setImage(Layout.panelImage(id, supply)));
            parts.add(text(pl.getNicknameToken(), f, Layout.TEXT).setX(156).setY(24).setFontWeight(Text.FontWeight.BOLD).setMaxWidth(200));
            score = text("0", 72, c).setX(172).setY(70).setFontWeight(Text.FontWeight.BOLD);
            hand = text("", f, Layout.TEXT).setX(66).setY(158).setFontWeight(Text.FontWeight.BOLD);
            supplyText = text("", f, Layout.TEXT).setX(178).setY(158).setFontWeight(Text.FontWeight.BOLD);
            lit = text("", f, Layout.TEXT).setX(282).setY(158).setFontWeight(Text.FontWeight.BOLD);
            actionIcon = spriteAt("icon_wait.png", 46, 252, 32, 32).setVisible(false);
            parts.add(actionIcon);
            action = text("", f, Layout.TEXT).setX(70).setY(234).setFontWeight(Text.FontWeight.BOLD);
            gain = text("", f, Layout.WHITE).setX(352).setY(234).setAnchorX(1).setFontWeight(Text.FontWeight.BOLD);
            message = text("", f, Layout.LABEL).setX(28).setY(268);
            for (Text t : new Text[] {score, hand, supplyText, lit, action, gain, message}) parts.add(t);
            group = g.createGroup(parts.toArray(new Entity<?>[0])).setX(o[0]).setY(o[1]).setZIndex(90);
            refreshHand();
        }

        void refreshHand() {
            set(hand, game.mirrorsLeft[id] + "/" + game.cfg.handMax);
            int every = game.cfg.supplyEvery;
            set(supplyText, every > 0 && game.mirrorsLeft[id] < game.cfg.handMax ? String.valueOf(Layout.turnsToSupply(game.turn, every)) : "");
        }

        void set(Text t, String v) {
            if (!v.equals(shown.get(t))) {
                shown.put(t, v);
                t.setText(v);
            }
        }

        void update(TurnReport r) {
            set(score, String.valueOf(game.score[id]));
            refreshHand();
            int alone = 0;
            if (r.beams[id] != null) for (int p : r.beams[id].lit) if (game.litBy[p] == id) alone++;
            set(lit, String.valueOf(alone));
            if (!game.active[id]) {
                if (!"ELIMINATED".equals(shown.get(action))) {
                    action.setFillColor(Layout.DANGER);
                    group.setAlpha(0.4);
                }
                visible(actionIcon, false);
                set(action, "ELIMINATED");
                set(message, "");
                set(gain, "");
                return;
            }
            Command c = r.commands[id];
            String[] parts = Layout.actionParts(c, r.invalid[id]);
            if (!parts[1].equals(shown.get(action)) || r.invalid[id] != null)
                fill(action, r.invalid[id] != null ? Layout.DANGER : Layout.TEXT);
            visible(actionIcon, true);
            image(actionIcon, parts[0]);
            set(action, parts[1]);
            String msg = Layout.safe(c.message); // an active player always has a parsed command
            set(message, Layout.shortMessage(msg));
            set(gain, r.gain[id] > 0 ? "+" + r.gain[id] : "");
            if (!msg.isEmpty()) tips.setTooltipText(group, msg);
        }
    }
}
