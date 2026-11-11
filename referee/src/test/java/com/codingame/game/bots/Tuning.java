package com.codingame.game.bots;

import java.util.Properties;

import com.codingame.game.engine.Config;

/** Batch overrides for the (T) constants: -Dgw.lock=4 -Dgw.streakCap=3 -Dgw.mirrorLife=15 ... */
public final class Tuning {
    private Tuning() {}

    public static void apply(Config c, Properties p) {
        c.lock = Integer.parseInt(p.getProperty("gw.lock", String.valueOf(c.lock)));
        c.streakCap = Integer.parseInt(p.getProperty("gw.streakCap", String.valueOf(c.streakCap)));
        c.mirrorLife = Integer.parseInt(p.getProperty("gw.mirrorLife", String.valueOf(c.mirrorLife)));
        c.mirrorStock = Integer.parseInt(p.getProperty("gw.mirrorStock", String.valueOf(c.mirrorStock)));
        c.supplyEvery = Integer.parseInt(p.getProperty("gw.supplyEvery", String.valueOf(c.supplyEvery)));
        c.handMax = Integer.parseInt(p.getProperty("gw.handMax", String.valueOf(c.handMax)));
        c.strikeBonus = Integer.parseInt(p.getProperty("gw.strikeBonus", String.valueOf(c.strikeBonus)));
        c.tideCycle = Integer.parseInt(p.getProperty("gw.tideCycle", String.valueOf(c.tideCycle)));
        c.tideAwake = Integer.parseInt(p.getProperty("gw.tideAwake", String.valueOf(c.tideAwake)));
    }
}
