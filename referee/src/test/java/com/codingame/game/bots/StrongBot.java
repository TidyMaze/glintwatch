package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codingame.game.engine.*;

/**
 * V18 league bosses (owner 2026-11-14: stronger bosses, about the level of a good bot of each league).
 * Core: a rollout planner over WAIT, PLACE and TURN on the cells of every active beam; each candidate is played on a copy
 * with the real rules for H turns (others WAIT after turn 0); value = sum(own gain - rivalWeight * best rival gain) - spend
 * per placed mirror. Additions by league:
 * - all leagues: a placement refused because a rival chose the same cell bans that cell for 3 turns;
 * - level 1 (no TURN, no contest, mirrors stay): hold while a rival is still placing (answer its moves instead of playing
 *   at the same time), and a two-mirror plan when no single mirror helps;
 * - level 4: each rival's move is predicted with the same planner (h 3, rw 0.25, spend 0.5, the others waiting), and the
 *   best K moves are replayed with the rivals re-planning for RD turns.
 * Work caps only (rollout counts), no clock: play does not depend on load.
 */
public final class StrongBot implements Bot {
    private final BotState s;
    private final int level;
    final int pairs;
    final double rivalWeight, spendCost;
    final boolean hold, predict;
    /** Rollouts per decision (all planners of one turn together). */
    final int budget;
    static final int WARMUP = Integer.getInteger("gw.strong.warmup", 2);
    private boolean warmed;
    private int[] prevHands;
    private Command last;
    private final Map<Integer, Integer> banned = new HashMap<>();
    private int spent;

    public StrongBot(Config cfg, int level) {
        s = new BotState(cfg);
        this.level = level;
        rivalWeight = level == 1 ? 1.0 : 0.25;
        spendCost = 0.5;
        hold = level == 1;
        predict = level >= 4;
        pairs = level == 1 ? 12 : 0;
        budget = Integer.getInteger("gw.strong.budget", 6000);
    }

    private int h, kk, rdd;

    @Override
    public void init(List<String> l) {
        s.parseInit(l);
        boolean two = s.playerCount == 2;
        h = predict && two ? 5 : 3;
        kk = predict ? (two ? 4 : 2) : 0;
        rdd = predict ? (two ? 4 : 2) : 0;
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        if (!warmed) {
            warmed = true;
            for (int i = 0; i < WARMUP; i++) decide(false); // JIT warm-up on the first turn (1 s allowed), results thrown away
        }
        Command c = decide(true);
        last = c;
        if (c.kind == Command.Kind.PLACE) s.myPlaced.add(Geometry.idx(c.x, c.y, s.game.board.n));
        return c.text();
    }

    private Command decide(boolean real) {
        Game g = s.game;
        int me = s.myId, pc = s.playerCount, t = g.currentTurn(), n = g.board.n;
        int[] hands = g.mirrorsLeft.clone();
        int[] prev = prevHands;
        if (real) {
            prevHands = hands;
            // a placement that came back refused (hand unchanged, cell still empty): leave that cell for 3 turns
            if (last != null && last.kind == Command.Kind.PLACE && prev != null && hands[me] >= prev[me]
                && g.board.mirrorType[Geometry.idx(last.x, last.y, n)] < 0)
                banned.put(Geometry.idx(last.x, last.y, n), t + 3);
            banned.values().removeIf(u -> u <= t);
        }
        if (hold && hands[me] > 0 && t <= 40) {
            int sup = g.cfg.supplyEvery > 0 && t % g.cfg.supplyEvery == 0 ? 1 : 0;
            for (int id = 0; id < pc; id++) {
                if (id == me || !g.active[id] || hands[id] <= 0) continue;
                boolean placed = prev != null && hands[id] < prev[id] + sup;
                if (prev == null || placed || sup == 1) return Command.waitCommand();
            }
        }
        spent = 0;
        Game base = g.copyWith(s.cfg);
        Command[] pred = new Command[pc];
        if (predict)
            for (int id = 0; id < pc; id++)
                if (id != me && base.active[id]) pred[id] = plan(base, id, new Command[pc], 3, 0.25, 0.5, Set.of(), 0).best;
        Plan p = plan(base, me, pred, h, rivalWeight, spendCost, banned.keySet(), kk);
        Command best = p.best;
        if (kk > 0) {
            List<Command> top = new ArrayList<>();
            top.add(Command.waitCommand());
            for (int i = 0; i < Math.min(kk, p.ranked.size()); i++) top.add(p.ranked.get(i));
            double bv = Double.NEGATIVE_INFINITY;
            for (Command c : top) {
                if (spent >= budget) break;
                double v = respond(base, me, pred, c);
                if (v > bv + 1e-9) {
                    bv = v;
                    best = c;
                }
            }
        }
        if (pairs > 0 && best.kind == Command.Kind.WAIT && hands[me] >= 2) {
            Command c = pairMove(base, me);
            if (c != null) best = c;
        }
        return best;
    }

    static final class Plan {
        Command best;
        final List<Command> ranked = new ArrayList<>();
    }

    private List<Command> candidates(Game base, int me, Set<Integer> ban) {
        int n = base.board.n;
        int[] states = base.seatStates();
        Set<Integer> cells = new LinkedHashSet<>();
        for (int i = 0; i < base.playerCount; i++) {
            int id = (me + i) % base.playerCount;
            if (base.active[id]) cells.addAll(BeamTracer.trace(base.board, base.seatOf[id], states).cells);
        }
        List<Command> out = new ArrayList<>();
        for (int p : cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            if (!ban.contains(p))
                for (int t = 0; t < 2; t++) {
                    Command c = Command.place(x, y, t);
                    if (base.validate(me, c) == null) out.add(c);
                }
            Command c = Command.turn(x, y);
            if (base.board.mirrorType[p] >= 0 && base.validate(me, c) == null) out.add(c);
        }
        return out;
    }

    /** Best move of player me with the others' turn-0 moves in pred (null = WAIT); ranked = candidates by value when rank > 0. */
    private Plan plan(Game base, int me, Command[] pred, int hz, double rw, double sp, Set<Integer> ban, int rank) {
        Plan out = new Plan();
        Command[] cmds = pred.clone();
        cmds[me] = Command.waitCommand();
        double bv = rollout(base, me, cmds, hz, rw, sp);
        out.best = Command.waitCommand();
        List<Command> cands = candidates(base, me, ban);
        List<double[]> vals = new ArrayList<>();
        // build delay: a mirror placed now reflects from turn 2, so turns 0-1 equal the WAIT line; replay only from there
        Game s2 = null;
        double pre2 = 0;
        Set<Integer> taken = new java.util.HashSet<>();
        if (base.cfg.buildDelay && hz >= 3 && !slow) {
            Game g = base.copy();
            Command[] wait = new Command[base.playerCount];
            pre2 = gainOf(g.playTurn(cmds), g, me, rw);
            if (!g.isOver()) pre2 += gainOf(g.playTurn(wait), g, me, rw);
            if (!g.isOver()) s2 = g;
            for (Command c : pred) if (c != null && c.kind == Command.Kind.PLACE) taken.add(Geometry.idx(c.x, c.y, base.board.n));
        }
        for (int i = 0; i < cands.size() && spent < budget; i++) {
            cmds[me] = cands.get(i);
            Command c = cands.get(i);
            double v = s2 != null && c.kind == Command.Kind.PLACE && !taken.contains(Geometry.idx(c.x, c.y, base.board.n))
                ? fromTurnTwo(s2, me, c, hz, rw, sp, pre2) : rollout(base, me, cmds, hz, rw, sp);
            if (v > bv + 1e-9) {
                bv = v;
                out.best = cands.get(i);
            }
            if (rank > 0) vals.add(new double[] {v, i});
        }
        if (rank > 0) {
            vals.sort((a, b) -> a[0] != b[0] ? Double.compare(b[0], a[0]) : Double.compare(a[1], b[1]));
            for (double[] e : vals) out.ranked.add(cands.get((int) e[1]));
        }
        return out;
    }

    private double rollout(Game base, int me, Command[] first, int hz, double rw, double sp) {
        spent++;
        Game g = base.copy();
        int pc = g.playerCount;
        double v = first[me] != null && first[me].kind == Command.Kind.PLACE ? -sp : 0;
        Command[] wait = new Command[pc];
        for (int t = 0; t < hz && !g.isOver(); t++) v += gainOf(g.playTurn(t == 0 ? first : wait), g, me, rw);
        return v;
    }

    /** The rollout of a placement under build delay, from the WAIT line's state at turn 2 with the mirror just activated. */
    private double fromTurnTwo(Game s2, int me, Command c, int hz, double rw, double sp, double pre2) {
        spent++;
        Game g = s2.copy();
        int p = Geometry.idx(c.x, c.y, g.board.n);
        g.board.mirrorType[p] = c.type;
        g.board.pending[p] = 0;
        g.board.mirrorLock[p] = g.cfg.lock;
        g.board.mirrorLife[p] = g.cfg.mirrorLife;
        g.board.lastToucher[p] = me;
        g.mirrorsLeft[me]--;
        double v = pre2 - sp;
        Command[] wait = new Command[g.playerCount];
        for (int t = 2; t < hz && !g.isOver(); t++) v += gainOf(g.playTurn(wait), g, me, rw);
        return v;
    }

    /** Test switch: full rollouts for every candidate (the reference the fast path must equal). */
    boolean slow;

    private static double gainOf(TurnReport r, Game g, int me, double rw) {
        int rival = 0;
        for (int id = 0; id < g.playerCount; id++) if (id != me && g.active[id]) rival = Math.max(rival, r.gain[id]);
        return r.gain[me] - rw * rival;
    }

    /** Value of c when the rivals re-plan (same planner, h 3) on the next rd turns, then everybody waits. */
    private double respond(Game base, int me, Command[] pred, Command c) {
        Game g = base.copy();
        int pc = g.playerCount;
        Command[] cmds = pred.clone();
        cmds[me] = c;
        double v = gainOf(g.playTurn(cmds), g, me, rivalWeight) - (c.kind == Command.Kind.PLACE ? spendCost : 0);
        int t = 1;
        for (; t < h && t <= rdd && !g.isOver(); t++) {
            Command[] rc = new Command[pc];
            for (int id = 0; id < pc; id++)
                if (id != me && g.active[id]) rc[id] = plan(g, id, new Command[pc], 3, 0.25, 0.5, Set.of(), 0).best;
            v += gainOf(g.playTurn(rc), g, me, rivalWeight);
        }
        Command[] wait = new Command[pc];
        for (; t < h && !g.isOver(); t++) v += gainOf(g.playTurn(wait), g, me, rivalWeight);
        return v;
    }

    /** Level 1: no single mirror helps; first mirror of the best two-mirror plan on static beams (no build delay there). */
    private Command pairMove(Game base, int me) {
        Board b = base.board.copy();
        int n = b.n;
        double baseV = staticValue(base, b, me);
        List<double[]> firsts = new ArrayList<>();
        List<Integer> cells = pairCells(base, b, me);
        for (int p : cells)
            for (int t = 0; t < 2; t++) {
                b.mirrorType[p] = t;
                firsts.add(new double[] {staticValue(base, b, me), p, t});
                b.mirrorType[p] = -1;
            }
        firsts.sort((x, y) -> Double.compare(y[0], x[0]));
        Command best = null;
        double bv = baseV + 2 * spendCost + 1e-9;
        for (int i = 0; i < Math.min(pairs, firsts.size()); i++) {
            int p = (int) firsts.get(i)[1], t = (int) firsts.get(i)[2];
            b.mirrorType[p] = t;
            for (int q : pairCells(base, b, me)) {
                if (q == p) continue;
                for (int u = 0; u < 2; u++) {
                    b.mirrorType[q] = u;
                    double v = staticValue(base, b, me);
                    b.mirrorType[q] = -1;
                    if (v > bv) {
                        bv = v;
                        best = Command.place(Geometry.x(p, n), Geometry.y(p, n), t);
                    }
                }
            }
            b.mirrorType[p] = -1;
        }
        return best;
    }

    private List<Integer> pairCells(Game base, Board b, int me) {
        int[] states = base.seatStates();
        Set<Integer> cells = new LinkedHashSet<>();
        for (int id = 0; id < base.playerCount; id++)
            if (base.active[id]) cells.addAll(BeamTracer.trace(b, base.seatOf[id], states).cells);
        List<Integer> out = new ArrayList<>();
        int n = b.n;
        for (int p : cells) {
            if (b.mirrorType[p] >= 0 || banned.containsKey(p)) continue;
            if (base.validate(me, Command.place(Geometry.x(p, n), Geometry.y(p, n), 0)) == null) out.add(p);
        }
        return out;
    }

    private double staticValue(Game base, Board b, int me) {
        int[] states = base.seatStates();
        double rival = 0, own = 0;
        for (int id = 0; id < base.playerCount; id++) {
            if (!base.active[id]) continue;
            int v = 0;
            for (int p : BeamTracer.trace(b, base.seatOf[id], states).lit) v += b.value[p];
            if (id == me) own = v;
            else rival = Math.max(rival, v);
        }
        return own - rivalWeight * rival;
    }
}
