package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class RefereeLogTest {
    @Test
    void guardedLogsTheStackWithThePrefixAndReturnsTheFailureWithoutRethrowing() {
        PrintStream old = System.err;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buf, true));
        System.setProperty("glintwatch.diag", "true");
        try {
            IllegalStateException boom = new IllegalStateException("boom");
            assertSame(boom, Referee.guarded("init", () -> {
                throw boom;
            }));
            assertInstanceOf(StackOverflowError.class, Referee.guarded("turn 3", () -> {
                throw new StackOverflowError();
            }));
        } finally {
            System.setErr(old);
            System.clearProperty("glintwatch.diag");
        }
        String log = buf.toString();
        assertTrue(log.contains("[glintwatch] init failed: java.lang.IllegalStateException: boom"), log);
        assertTrue(log.contains("[glintwatch] turn 3 failed: java.lang.StackOverflowError"), log);
        for (String line : log.split("\n")) assertTrue(line.startsWith("[glintwatch] "), line);
        assertNull(Referee.guarded("ok", () -> {}));
    }
}
