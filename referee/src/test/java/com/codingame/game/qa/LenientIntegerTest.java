package com.codingame.game.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.codingame.game.engine.Command;
import com.codingame.game.engine.CommandParser;

/** Designer Q3 (2026-11-11): a sign or leading zeros still make a whole number; pinned so it cannot change silently. */
class LenientIntegerTest {
    @Test
    void plusSignLeadingZerosAndMinusZeroAreWholeNumbers() throws Exception {
        Command c = CommandParser.parse("PLACE +1 2 0");
        assertEquals(Command.Kind.PLACE, c.kind);
        assertEquals(1, c.x);
        assertEquals(2, c.y);
        assertEquals(0, c.type);
        Command d = CommandParser.parse("PLACE 01 002 -0");
        assertEquals(1, d.x);
        assertEquals(2, d.y);
        assertEquals(0, d.type);
        Command t = CommandParser.parse("TURN +3 07");
        assertEquals(3, t.x);
        assertEquals(7, t.y);
    }
}
