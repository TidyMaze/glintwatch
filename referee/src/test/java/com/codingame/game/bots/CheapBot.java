package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codingame.game.engine.*;

/**
 * Red-team cheap bots (M5): one 1-ply simulating rule each, to test the "no cheap bot first in >= 40 % of mixed tables" cap.
 * CUTTER: also tries one mirror on the top rival's lane outside its zone, valued by the rival streak value it kills.
 * RENEWER: rebuilds each of its mirrors at the same cell and type once it shatters, greedy otherwise.
 * SPOILER: greedy with rivals weighted 2.0 and only the leader counted.
 * FREE: greedy G+ that only edits its own zone (rides the neutral mirrors of others).
 * PLUS_W: G+ with rival weight from system property cheap.w (default 0.5), for weight sweeps.
 */
public final class CheapBot implements Bot {
    public enum Mode { CUTTER, RENEWER, SPOILER, FREE, PLUS_W }

    private static final double LOSS_W = Double.parseDouble(System.getProperty("cheap.lw", "1.5"));
    private static final double CUT_COST = Double.parseDouble(System.getProperty("cheap.cost", "5"));
    private final BotState s;
    private final Mode mode;
    private final double rivalWeight;
    /** Renewer memory: cell -> type of mirrors this bot placed. */
    private final Map<Integer, Integer> mine = new LinkedHashMap<>();

    public CheapBot(Config cfg, Mode mode) {
        s = new BotState(cfg);
        this.mode = mode;
        double w = Double.parseDouble(System.getProperty("cheap.w", "0.5"));
        rivalWeight = mode == Mode.SPOILER ? 2.0 : w;
    }

    @Override
    public void init(List<String> initLines) {
        s.parseInit(initLines);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        Game base = s.game;
        int me = s.myId, n = base.board.n;
        if (mode == Mode.RENEWER) {
            for (Map.Entry<Integer, Integer> en : new ArrayList<>(mine.entrySet())) {
                int p = en.getKey();
                if (base.board.mirrorType[p] >= 0) continue;
                Command c = Command.place(Geometry.x(p, n), Geometry.y(p, n), en.getValue());
                if (base.validate(me, c) == null) {
                    s.myPlaced.add(p);
                    return c.text();
                }
            }
        }
        Command best = Command.waitCommand();
        double bestValue = evaluate(best, false);
        for (Command c : candidates()) {
            double v = evaluate(c, false);
            if (v > bestValue) {
                bestValue = v;
                best = c;
            }
        }
        if (mode == Mode.CUTTER) {
            for (Command c : cuts()) {
                double v = evaluate(c, true);
                if (v > bestValue) {
                    bestValue = v;
                    best = c;
                }
            }
        }
        if (best.kind == Command.Kind.PLACE) {
            int p = Geometry.idx(best.x, best.y, n);
            s.myPlaced.add(p);
            mine.put(p, best.type);
        }
        return best.text();
    }

    private boolean allowed(Game g, int me, Command c) {
        if (s.game.validate(me, c) != null) return false;
        if (mode == Mode.FREE) {
            int n = g.board.n;
            return Seats.inZone(g.seatOf[me], c.x, c.y, n, g.board.e);
        }
        return true;
    }

    private List<Command> candidates() {
        Game g = s.simulation();
        int me = s.myId, n = g.board.n;
        Beam path = BeamTracer.trace(g.board, g.seatOf[me], g.seatStates());
        List<Command> places = new ArrayList<>(), turns = new ArrayList<>();
        for (int p : path.cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 2; t++) {
                Command c = Command.place(x, y, t);
                if (allowed(g, me, c)) places.add(c);
            }
            Command c = Command.turn(x, y);
            if (g.board.mirrorType[p] >= 0 && allowed(g, me, c)) turns.add(c);
        }
        places.addAll(turns);
        return places;
    }

    /** Cutter candidates: both types on every cell of the top rival's lane outside its zone. */
    private List<Command> cuts() {
        List<Command> out = new ArrayList<>();
        Game g = s.simulation();
        int me = s.myId, n = g.board.n, top = topRival(g);
        if (top < 0) return out;
        Beam lane = BeamTracer.trace(g.board, g.seatOf[top], g.seatStates());
        for (int p : lane.cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 2; t++) {
                Command c = Command.place(x, y, t);
                if (s.game.validate(me, c) == null) out.add(c);
            }
        }
        return out;
    }

    private int topRival(Game g) {
        int top = -1;
        for (int id = 0; id < g.playerCount; id++)
            if (id != s.myId && g.active[id] && (top < 0 || g.score[id] > g.score[top])) top = id;
        return top;
    }

    private double evaluate(Command mineCmd, boolean cut) {
        Game g = s.simulation();
        int me = s.myId;
        Game ref = s.simulation();
        Command[] wait = new Command[g.playerCount];
        Command[] cmds = new Command[g.playerCount];
        for (int id = 0; id < g.playerCount; id++) {
            wait[id] = Command.waitCommand();
            cmds[id] = id == me ? mineCmd : Command.waitCommand();
        }
        TurnReport before = ref.playTurn(wait);
        TurnReport r = g.playTurn(cmds);
        int rival = 0;
        int top = topRival(g);
        for (int id = 0; id < g.playerCount; id++) {
            if (id == me || !g.active[id]) continue;
            if (mode == Mode.SPOILER && id != top) continue;
            rival = Math.max(rival, r.gain[id]);
        }
        double v = r.gain[me] - rivalWeight * rival;
        if (mode == Mode.CUTTER && cut && top >= 0) {
            // streak value of the top rival's crystals this move stops lighting (keeps its next streak step)
            double loss = 0;
            for (int p : before.beams[top].lit)
                if (!r.beams[top].lit.contains(p))
                    loss += ref.board.value[p] * Math.min(ref.streak[top][p] + 1, ref.cfg.streakCap);
            v = r.gain[me] + LOSS_W * loss - CUT_COST;
        }
        if (mode != Mode.CUTTER || !cut) {
            if (mode == Mode.FREE || mode == Mode.PLUS_W || mode == Mode.CUTTER || mode == Mode.RENEWER) {
                for (int p = 0; p < g.board.n * g.board.n; p++)
                    if (g.litBy[p] == me)
                        v += g.board.value[p] * Math.min(g.streak[me][p] + 1, g.cfg.streakCap);
            }
        }
        return v;
    }
}
