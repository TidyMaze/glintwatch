package com.codingame.game.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.*;

/** qa BUG-4: level1 statement says a mirror's lock is "always 0 in this league" (no TURN there). */
class WoodTwoLockTest {
    @Test
    void woodTwoMirrorsAlwaysShowLockZero() throws Exception {
        Game g = new Game(Board.empty(13, 3), 2, Config.forLeague(1));
        g.playTurn(new Command[] {CommandParser.parse("PLACE 5 3 1"), CommandParser.parse("WAIT")});
        for (String l : Protocol.turnLinesV5(g, 0))
            if (l.startsWith("5 3 ")) assertEquals("0", l.split(" ")[3], "mirror line " + l);
        assertEquals(0, g.board.mirrorLock[Geometry.idx(5, 3, 13)]);
    }
}
