package com.codingame.game.engine;

/** Rule switches and constants (DESIGN F1); every (T) tunable value lives here only. */
public final class Config {
    public int maxTurns = 150;
    /** DESIGN V5.5 early end: "decided" (no pair can change order) and "stale" (all idle STALE_TURNS turns). */
    public boolean earlyEnd = true;
    public int staleTurns = 20;
    /** Mirrors in hand at the start (Wood: the whole stock). */
    public int mirrorStock = 4;
    /** +1 mirror before the input of turns t % supplyEvery == 0 (0 = no supply), hand capped at handMax. */
    public int supplyEvery = 4;
    public int handMax = 6;
    public int lock = 5;
    public boolean turnAllowed = true;
    /** Placed mirrors are pending 1 turn (reflect from t+2) and their type is hidden from rivals. */
    public boolean buildDelay = true;
    public boolean sealed = true;
    /** Active turns before a mirror breaks; -1 = never. */
    public int mirrorLife = 20;
    /** Crystals sleep: awake when (turn + phase) % tideCycle < tideAwake. */
    public boolean tides = false;
    public int tideCycle = 45;
    public int tideAwake = 30;
    /** false: every beam on a crystal scores it (Wood 2). */
    public boolean contested = true;
    public int streakCap = 4;
    public int strikeBonus = 0;
    /** Map options: rocks and start mirrors; crystal values 1-3 (else all 1). */
    public boolean walls = true;
    public boolean multiValues = false;

    public Config copy() {
        Config c = new Config();
        c.maxTurns = maxTurns;
        c.earlyEnd = earlyEnd;
        c.staleTurns = staleTurns;
        c.mirrorStock = mirrorStock;
        c.supplyEvery = supplyEvery;
        c.handMax = handMax;
        c.lock = lock;
        c.turnAllowed = turnAllowed;
        c.buildDelay = buildDelay;
        c.sealed = sealed;
        c.mirrorLife = mirrorLife;
        c.tides = tides;
        c.tideCycle = tideCycle;
        c.tideAwake = tideAwake;
        c.contested = contested;
        c.streakCap = streakCap;
        c.strikeBonus = strikeBonus;
        c.walls = walls;
        c.multiValues = multiValues;
        return c;
    }

    public static Config bronze() {
        return new Config();
    }

    public static Config silver() {
        Config c = bronze();
        c.tides = true;
        c.multiValues = true;
        c.strikeBonus = 2;
        return c;
    }

    public static Config woodOne() {
        Config c = bronze();
        c.mirrorStock = 12;
        c.supplyEvery = 0;
        c.handMax = 12;
        c.buildDelay = false;
        c.sealed = false;
        c.mirrorLife = -1;
        return c;
    }

    public static Config woodTwo() {
        Config c = woodOne();
        c.maxTurns = 100;
        c.turnAllowed = false;
        c.lock = 0; // F6.3: no TURN in Wood 2, so the lock always reads 0 (qa BUG-4)
        c.walls = false;
        c.contested = false;
        c.streakCap = 1;
        return c;
    }

    /** DESIGN v1 Bronze, the baseline of the hardness test. */
    public static Config v1Bronze() {
        Config c = woodOne();
        c.mirrorStock = 20;
        c.handMax = 20;
        c.lock = 3;
        c.streakCap = 1;
        c.multiValues = true;
        c.strikeBonus = 2;
        return c;
    }

    /** CG league level: 1 Wood 2, 2 Wood 1, 3 Bronze, 4 and up Silver rules. */
    public static Config forLeague(int level) {
        return level <= 1 ? woodTwo() : level == 2 ? woodOne() : level == 3 ? bronze() : silver();
    }
}
