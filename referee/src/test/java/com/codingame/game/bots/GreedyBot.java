package com.codingame.game.bots;

import java.util.ArrayList;
import java.util.List;

import com.codingame.game.engine.*;

/**
 * DESIGN 7 / V2.5 / V2.6 greedy bots. Candidates: WAIT, legal PLACE (both types) on own beam path cells, then legal
 * TURN of mirrors on that path. One-turn simulation with the others on WAIT; first best kept (WAIT unless strictly better).
 * SELF = Greedy-self (own lit values, no contest); FULL = G (own gain - 0.5 max rival gain); PLUS = G+ (G + next streak
 * value of crystals it keeps, skipping crystals dormant when its mirror activates). DESIGN F9.
 */
public final class GreedyBot implements Bot {
    public enum Mode { SELF, FULL, PLUS }

    private final BotState s;
    private final Mode mode;

    public GreedyBot(Config cfg, Mode mode) {
        s = new BotState(cfg);
        this.mode = mode;
    }

    @Override
    public void init(List<String> initLines) {
        s.parseInit(initLines);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        Command best = Command.waitCommand();
        double bestValue = evaluate(best);
        for (Command c : candidates()) {
            double v = evaluate(c);
            if (v > bestValue) {
                bestValue = v;
                best = c;
            }
        }
        if (best.kind == Command.Kind.PLACE) s.myPlaced.add(Geometry.idx(best.x, best.y, s.game.board.n));
        return best.text();
    }

    List<Command> candidates() {
        Game g = s.simulation();
        int me = s.myId, n = g.board.n;
        Beam path = BeamTracer.trace(g.board, g.seatOf[me], g.seatStates());
        List<Command> places = new ArrayList<>(), turns = new ArrayList<>();
        for (int p : path.cells) {
            int x = Geometry.x(p, n), y = Geometry.y(p, n);
            for (int t = 0; t < 2; t++) {
                Command c = Command.place(x, y, t);
                if (s.game.validate(me, c) == null) places.add(c);
            }
            Command c = Command.turn(x, y);
            if (s.game.board.mirrorType[p] >= 0 && s.game.validate(me, c) == null) turns.add(c);
        }
        places.addAll(turns);
        return places;
    }

    double evaluate(Command mine) {
        Game g = s.simulation();
        int me = s.myId;
        Command[] cmds = new Command[g.playerCount];
        for (int id = 0; id < g.playerCount; id++) cmds[id] = id == me ? mine : Command.waitCommand();
        TurnReport r = g.playTurn(cmds);
        if (mode == Mode.SELF) {
            int v = 0;
            for (int p : r.beams[me].lit) v += g.board.value[p];
            return v;
        }
        int rival = 0;
        for (int id = 0; id < g.playerCount; id++) if (id != me && g.active[id]) rival = Math.max(rival, r.gain[id]);
        double v = r.gain[me] - 0.5 * rival;
        if (mode == Mode.PLUS) {
            int activation = s.game.currentTurn() + (s.cfg.buildDelay && mine.kind == Command.Kind.PLACE ? 2 : 0);
            for (int p = 0; p < g.board.n * g.board.n; p++)
                if (g.litBy[p] == me && s.game.awake(p, activation))
                    v += g.board.value[p] * Math.min(g.streak[me][p] + 1, g.cfg.streakCap);
        }
        return v;
    }
}
