package com.codingame.game.bots;

import java.util.List;
import java.util.Random;

import com.codingame.game.engine.*;

/** Half the turns WAIT, else a random legal PLACE or TURN (seeded). */
public final class RandomBot implements Bot {
    private final BotState s;
    private final long seed;
    private Random rnd;

    public RandomBot(Config cfg, long seed) {
        s = new BotState(cfg);
        this.seed = seed;
    }

    @Override
    public void init(List<String> initLines) {
        s.parseInit(initLines);
        rnd = new Random(seed * 31 + s.myId);
    }

    @Override
    public String act(List<String> turnLines) {
        s.parseTurn(turnLines);
        if (rnd.nextBoolean()) return "WAIT";
        int n = s.game.board.n;
        for (int k = 0; k < 50; k++) {
            int x = rnd.nextInt(n), y = rnd.nextInt(n);
            Command c = rnd.nextInt(4) == 0 ? Command.turn(x, y) : Command.place(x, y, rnd.nextInt(2));
            if (s.game.validate(s.myId, c) == null) return c.text();
        }
        return "WAIT";
    }
}
