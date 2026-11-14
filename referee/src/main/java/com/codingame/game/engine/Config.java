package com.codingame.game.engine;

/** Rule switches and constants (DESIGN F1); every (T) tunable value lives here only. */
public final class Config {
    /** V6: 150 turns in every league. */
    public int maxTurns = 150;
    /** V6 early end: "stale" only (all idle STALE_TURNS turns, then WAIT is played to the end). */
    public boolean earlyEnd = true;
    public int staleTurns = 20;
    /** V6: every league starts with 12 mirrors. */
    public int mirrorStock = 12;
    /** Supply: +1 mirror before the input of turns t % supplyEvery == 0 (0 = no supply), no hand cap. */
    public int supplyEvery = 10;
    /** V6: lock 2 after a TURN or a placement (leagues with TURN). */
    public int lock = 2;
    public boolean turnAllowed = true;
    /** Placed mirrors are pending 1 turn (reflect from t+2); their type is shown to everyone (V6). */
    public boolean buildDelay = true;
    /** Active turns before a mirror breaks; -1 = never. */
    public int mirrorLife = 20;
    /** false: every beam on a crystal scores it (Wood 2). */
    public boolean contested = true;
    public int streakCap = 4;
    /** Points per strike (a beam ending on another active player's lamp), each turn: 0 below the top league. */
    public static final int STRIKE_BONUS = 4;
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
        c.lock = lock;
        c.turnAllowed = turnAllowed;
        c.buildDelay = buildDelay;
        c.mirrorLife = mirrorLife;
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
        c.multiValues = true;
        c.strikeBonus = STRIKE_BONUS;
        return c;
    }

    public static Config woodOne() {
        Config c = bronze();
        c.buildDelay = false;
        c.mirrorLife = -1;
        return c;
    }

    public static Config woodTwo() {
        Config c = woodOne();
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
        c.supplyEvery = 0;
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
