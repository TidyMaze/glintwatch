package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.List;

import com.codingame.game.engine.*;

/**
 * Ladder bosses with a 2-turn own search (M5). Ply 1: all candidates of GreedyBot (own beam path) valued like G / G+;
 * the best WIDTH go to ply 2: the state after the command (rivals WAIT), candidates from the new own beam path,
 * value = v1 + best v2 (WAIT = 0 gain delta valued the same way). Rivals always WAIT. FULL = G2, PLUS = G+2.
 */
public final class Boss2 implements Bot {
    private double rivalWeight = 0.5;
    private boolean allLanes;
    public Boss2 allLanes(boolean a) {
        allLanes = a;
        return this;
    }

    private final BotState s;
    private final GreedyBot.Mode mode;
    private final int width;
    private final double discount;
    public Boss2 rivalWeight(double w) {
        rivalWeight = w;
        return this;
    }


    public Boss2(Config cfg, GreedyBot.Mode mode, int width, double discount) {
        s = new BotState(cfg);
        this.mode = mode;
        this.width = width;
        this.discount = discount;
    }

    @Override
    public void init(List<String> l) {
        s.parseInit(l);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        Game g0 = s.simulation();
        List<Command> cands = new ArrayList<>();
        cands.add(Command.waitCommand());
        cands.addAll(candidates(g0, s.game));
        int k = cands.size();
        double[] v1 = new double[k];
        Game[] after = new Game[k];
        for (int i = 0; i < k; i++) {
            after[i] = g0.copy();
            v1[i] = step(after[i], cands.get(i));
        }
        Integer[] order = new Integer[k];
        for (int i = 0; i < k; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(v1[b], v1[a]));
        int best = 0;
        double bestV = Double.NEGATIVE_INFINITY;
        for (int r = 0; r < Math.min(width, k); r++) {
            int i = order[r];
            double v2 = 0;
            if (!after[i].isOver()) {
                for (Command c2 : candidates(after[i], after[i])) {
                    Game t = after[i].copy();
                    v2 = Math.max(v2, step(t, c2));
                }
            }
            double v = v1[i] + discount * v2;
            if (v > bestV + 1e-9 || (Math.abs(v - bestV) <= 1e-9 && i < best)) {
                bestV = v;
                best = i;
            }
        }
        Command c = cands.get(best);
        if (c.kind == Command.Kind.PLACE) s.myPlaced.add(Geometry.idx(c.x, c.y, s.game.board.n));
        return c.text();
    }

    private List<Command> candidates(Game g, Game validator) {
        int me = s.myId, n = g.board.n;
        java.util.Set<Integer> cells = new java.util.LinkedHashSet<>();
        for (int id = 0; id < g.playerCount; id++)
            if (g.active[id] && (allLanes || id == me)) cells.addAll(BeamTracer.trace(g.board, g.seatOf[id], g.seatStates()).cells);
        List<Command> places = new ArrayList<>(), turns = new ArrayList<>();
        for (int p : cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 2; t++) {
                Command c = Command.place(x, y, t);
                if (validator.validate(me, c) == null) places.add(c);
            }
            Command c = Command.turn(x, y);
            if (g.board.mirrorType[p] >= 0 && validator.validate(me, c) == null) turns.add(c);
        }
        places.addAll(turns);
        return places;
    }

    /** Plays mine (rivals WAIT) on g and returns the G / G+ value of that turn. */
    private double step(Game g, Command mine) {
        int me = s.myId;
        int turnNo = g.currentTurn();
        Command[] cmds = new Command[g.playerCount];
        for (int id = 0; id < g.playerCount; id++) cmds[id] = id == me ? mine : Command.waitCommand();
        TurnReport r = g.playTurn(cmds);
        int rival = 0;
        for (int id = 0; id < g.playerCount; id++) if (id != me && g.active[id]) rival = Math.max(rival, r.gain[id]);
        double v = r.gain[me] - rivalWeight * rival;
        if (mode == GreedyBot.Mode.PLUS) {
            for (int p = 0; p < g.board.n * g.board.n; p++)
                if (g.litBy[p] == me)
                    v += g.board.value[p] * Math.min(g.streak[me][p] + 1, g.cfg.streakCap);
        }
        return v;
    }
}
