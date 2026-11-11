package com.codingame.game.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandParserTest {
    @Test
    void place() throws Exception {
        Command c = CommandParser.parse("PLACE 5 3 1");
        assertEquals(Command.Kind.PLACE, c.kind);
        assertEquals(5, c.x);
        assertEquals(3, c.y);
        assertEquals(1, c.type);
        assertEquals("", c.message);
    }

    @Test
    void turnAndWait() throws Exception {
        Command t = CommandParser.parse("TURN 4 7");
        assertEquals(Command.Kind.TURN, t.kind);
        assertEquals(4, t.x);
        assertEquals(7, t.y);
        assertEquals(Command.Kind.WAIT, CommandParser.parse("WAIT").kind);
    }

    @Test
    void textAfterTheLastTokenIsAMessageCutAtThirtyChars() throws Exception {
        assertEquals("hello world", CommandParser.parse("PLACE 5 3 1 hello world").message);
        assertEquals("go", CommandParser.parse("TURN 1 2 go").message);
        assertEquals("zzz", CommandParser.parse("WAIT zzz").message);
        assertEquals("123456789012345678901234567890", CommandParser.parse("WAIT 123456789012345678901234567890XYZ").message);
    }

    @Test
    void outOfRangeTypeAndCoordinatesParseButAreLeftToValidation() throws Exception {
        Command c = CommandParser.parse("PLACE -1 99 7");
        assertEquals(-1, c.x);
        assertEquals(99, c.y);
        assertEquals(7, c.type);
    }

    @Test
    void surroundingAndRepeatedSpacesAreTolerated() throws Exception {
        Command c = CommandParser.parse("  PLACE  5 3   1  ");
        assertEquals(1, c.type);
    }

    @Test
    void malformedLinesAreRejected() {
        String[] bad = {"", "   ", "wait", "Place 1 2 0", "MOVE 1 2", "PLACE 5 3", "PLACE 5 x 1", "TURN 4",
            "TURN 4 7.5", "PLACE 99999999999 3 1", "WAIT\tx", null};
        for (String line : bad)
            assertThrows(CommandParser.InvalidCommand.class, () -> CommandParser.parse(line), String.valueOf(line));
    }
}
