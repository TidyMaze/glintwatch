package com.codingame.game.bots;

import java.util.List;

import com.codingame.game.engine.*;

/**
 * Level 1 boss "Wanderer" (DESIGN V5.3c): on turns t % PERIOD == 0, while it has mirrors and has placed fewer than CAP,
 * it walks its own beam from the lamp and plays the first legal (cell, type 0 then 1) whose re-traced beam lights a
 * crystal nobody lit alone last turn (litBy = -1); otherwise WAIT. No randomness, no look at rivals' beams.
 */
public final class WandererBot implements Bot {
    public static final int PERIOD = 40, CAP = 2;
    private final BotState s;
    private final int period, cap;
    private int placed;

    public WandererBot(Config cfg) {
        this(cfg, PERIOD, CAP);
    }

    /** Other period and cap: for the tuning tables and the cap test (CAP is out of reach in 100 turns at PERIOD 20). */
    public WandererBot(Config cfg, int period, int cap) {
        s = new BotState(cfg);
        this.period = period;
        this.cap = cap;
    }

    @Override
    public void init(List<String> initLines) {
        s.parseInit(initLines);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        Game g = s.game;
        if (s.turns % period != 0 || placed >= cap || g.mirrorsLeft[0] <= 0) return "WAIT";
        int n = g.board.n, seat = g.seatOf[0];
        for (int p : BeamTracer.trace(g.board, seat, g.seatStates()).cells)
            for (int type = 0; type < 2; type++) {
                Command c = Command.place(Geometry.x(p, n), Geometry.y(p, n), type);
                if (g.validate(0, c) == null && lightsAFreeCrystal(g, p, type)) {
                    placed++;
                    return "PLACE " + c.x + " " + c.y + " " + type;
                }
            }
        return "WAIT";
    }

    private static boolean lightsAFreeCrystal(Game g, int p, int type) {
        Board b = g.board.copy();
        b.mirrorType[p] = type;
        b.pending[p] = 0;
        for (int c : BeamTracer.trace(b, g.seatOf[0], g.seatStates()).lit) if (g.litBy[c] == -1) return true;
        return false;
    }
}
