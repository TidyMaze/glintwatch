package com.codingame.game.bots;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.Config;

class TuningTest {
    @Test
    void overridesOnlyTheGivenConstants() {
        Properties p = new Properties();
        p.setProperty("gw.lock", "4");
        p.setProperty("gw.tideAwake", "25");
        Config c = Config.silver();
        Tuning.apply(c, p);
        assertEquals(4, c.lock);
        assertEquals(25, c.tideAwake);
        assertEquals(Config.silver().streakCap, c.streakCap);
        assertEquals(Config.silver().mirrorLife, c.mirrorLife);
    }
}
