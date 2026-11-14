package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.codingame.game.engine.*;

/**
 * M5 planner P. Candidates: WAIT, legal PLACE (both types) and TURN on cells of ANY active beam path (so it can cut or
 * deflect rivals). Each candidate is rolled out for H turns with the real rules (build delay, wear, streaks) while
 * everybody else WAITs; value = sum over the horizon of (own gain - rivalWeight * best rival gain) - spendCost.
 * Rival pending mirrors (hidden type) are treated as Empty. Knobs: -Dgw.p.h, -Dgw.p.rw, -Dgw.p.spend.
 */
public final class PlannerBot implements Bot {
    private final BotState s;
    final int horizon;
    final double rivalWeight, spendCost;
    /** Rollouts per turn after WAIT's (lead 2026-11-11: a work cap, not a clock, so play does not depend on load). */
    final int rolloutCap;
    static final int DEFAULT_CAP = 250; // worst seen 169 candidates (60 seeds, 4p L4); ~6 ms at 169
    /** Largest candidate count seen in this JVM (to set the cap above the worst case). */
    static int maxCandidates;
    /** JIT warm-up (analyst-h, lead 2026-11-11): full decisions on the first turn's state, results thrown away. Fixed count, no clock. */
    static final int DEFAULT_WARMUP = 30;
    final int warmupDecisions;
    private boolean warmed;
    /** compute-scaling knobs: rivals / me play 1-ply greedy for the first oppTurns / selfTurns rollout turns; allCells = candidates on every legal cell. */
    final int oppTurns, selfTurns;
    final boolean allCells;

    /** spec: "planner" or "planner:h=20:rw=1:spend=0.3" (overrides the -Dgw.p.* defaults). */
    public PlannerBot(Config cfg, String spec) {
        s = new BotState(cfg);
        int h = Integer.getInteger("gw.p.h", 8);
        double rw = Double.parseDouble(System.getProperty("gw.p.rw", "0.25"));
        double sp = Double.parseDouble(System.getProperty("gw.p.spend", "0.5"));
        int ot = 0, st = 0;
        boolean ac = false;
        int cap = Integer.getInteger("gw.p.cap", DEFAULT_CAP);
        int warm = Integer.getInteger("gw.warmup", DEFAULT_WARMUP); // -Dgw.warmup=N for measurement; the spec wins
        for (String part : spec.split(":")) {
            if (part.startsWith("warmup=")) warm = Integer.parseInt(part.substring(7));
            if (part.startsWith("opp=")) ot = Integer.parseInt(part.substring(4));
            if (part.startsWith("self=")) st = Integer.parseInt(part.substring(5));
            if (part.equals("all")) ac = true;
            if (part.startsWith("cap=")) cap = Integer.parseInt(part.substring(4));
            if (part.startsWith("h=")) h = Integer.parseInt(part.substring(2));
            else if (part.startsWith("rw=")) rw = Double.parseDouble(part.substring(3));
            else if (part.startsWith("spend=")) sp = Double.parseDouble(part.substring(6));
        }
        horizon = h;
        rivalWeight = rw;
        spendCost = sp;
        oppTurns = ot;
        selfTurns = st;
        allCells = ac;
        rolloutCap = cap;
        warmupDecisions = warm;
    }

    @Override
    public void init(List<String> l) {
        s.parseInit(l);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        if (!warmed) {
            warmed = true;
            for (int i = 0; i < warmupDecisions; i++) choose(); // results discarded: only the JIT sees them
        }
        Command best = choose();
        if (best.kind == Command.Kind.PLACE) s.myPlaced.add(Geometry.idx(best.x, best.y, s.game.board.n));
        return best.text();
    }

    /** The decision for the current turn; reads state, never writes it (rollouts run on copies). */
    private Command choose() {
        Game base = s.game.copyWith(s.cfg);
        for (int p = 0; p < base.board.n * base.board.n; p++)
            if (s.unknownType[p]) base.board.removeMirror(p);
        int me = s.myId, n = base.board.n;
        int[] states = base.seatStates();
        Set<Integer> cells = new LinkedHashSet<>();
        for (int id = 0; id < s.playerCount; id++)
            if (base.active[id]) cells.addAll(BeamTracer.trace(base.board, base.seatOf[id], states).cells);
        if (allCells) for (int p = 0; p < n * n; p++) cells.add(p);
        List<Command> cands = new ArrayList<>();
        for (int p : cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 2; t++) {
                Command c = Command.place(x, y, t);
                if (s.game.validate(me, c) == null) cands.add(c);
            }
            Command c = Command.turn(x, y);
            if (base.board.mirrorType[p] >= 0 && s.game.validate(me, c) == null) cands.add(c);
        }
        Command best = Command.waitCommand();
        double bestV = rollout(base, best);
        maxCandidates = Math.max(maxCandidates, cands.size());
        for (int k = 0; k < cands.size() && k < rolloutCap; k++) {
            Command c = cands.get(k);
            double v = rollout(base, c);
            if (v > bestV + 1e-9) {
                bestV = v;
                best = c;
            }
        }
        return best;
    }


    private double rollout(Game base, Command first) {
        Game g = base.copy();
        int me = s.myId, pc = g.playerCount;
        double v = first.kind == Command.Kind.PLACE ? -spendCost : 0;
        Command[] cmds = new Command[pc];
        for (int k = 0; k < horizon && !g.isOver(); k++) {
            for (int id = 0; id < pc; id++) {
                if (id == me) cmds[id] = k == 0 ? first : k <= selfTurns ? greedy(g, id) : Command.waitCommand();
                else cmds[id] = k < oppTurns && g.active[id] ? greedy(g, id) : Command.waitCommand();
            }
            TurnReport r = g.playTurn(cmds);
            int rival = 0;
            for (int id = 0; id < pc; id++) if (id != me && g.active[id]) rival = Math.max(rival, r.gain[id]);
            v += r.gain[me] - rivalWeight * rival;
        }
        return v;
    }

    /** 1-ply greedy (G style) for player id on g: own gain - 0.5 best other gain, others WAIT, candidates on its own beam path. */
    private Command greedy(Game g, int id) {
        int n = g.board.n, pc = g.playerCount;
        Beam path = BeamTracer.trace(g.board, g.seatOf[id], g.seatStates());
        Command best = Command.waitCommand();
        double bestV = value(g, id, best);
        for (int p : path.cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 3; t++) {
                Command c = t < 2 ? Command.place(x, y, t) : Command.turn(x, y);
                if (g.validate(id, c) != null) continue;
                double v = value(g, id, c);
                if (v > bestV + 1e-9) {
                    bestV = v;
                    best = c;
                }
            }
        }
        return best;
    }

    private double value(Game g, int id, Command c) {
        Game t = g.copy();
        Command[] cmds = new Command[t.playerCount];
        for (int i = 0; i < cmds.length; i++) cmds[i] = i == id ? c : Command.waitCommand();
        TurnReport r = t.playTurn(cmds);
        int rival = 0;
        for (int i = 0; i < cmds.length; i++) if (i != id && t.active[i]) rival = Math.max(rival, r.gain[i]);
        return r.gain[id] - 0.5 * rival;
    }
}
