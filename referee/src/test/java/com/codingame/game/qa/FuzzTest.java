package com.codingame.game.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Small fuzz smoke (M7, cg-contrib-qa): 2 games (levels 1 and 4, 3 players) + replays, no sleeping bot; the big run is FuzzMain. */
class FuzzTest {
    @Test
    void garbageAndCrashingBotsNeverBreakTheRefereeAndReplaysAreDeterministic() {
        FuzzMain.sleeps = false;
        try {
            FuzzMain.Outcome o = FuzzMain.run(1, 101, true, new int[] {1, 4}, new int[] {3});
            assertEquals(0, o.failures, String.join("\n", o.lines));
            assertEquals(2, o.games);
        } finally {
            FuzzMain.sleeps = true;
        }
    }
}
