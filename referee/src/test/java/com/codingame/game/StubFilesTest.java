package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** v20/v21: levels 1-3 have their own stub texts (matching their statement); level 4 uses the shared stub; every stub reads exactly the same lines. */
class StubFilesTest {
    private static List<String> readLines(String stub) {
        String code = stub.split("\n\n", 2)[0];
        return Arrays.asList(code.trim().split("\n"));
    }

    @Test
    void levelOneStubReadsTheSameLinesWithLevelOneTexts() throws Exception {
        String shared = Files.readString(Path.of("config/stub.txt"));
        Path l1 = Path.of("config/level1/stub.txt");
        assertTrue(Files.exists(l1), "config/level1/stub.txt");
        String one = Files.readString(l1);
        assertEquals(readLines(shared), readLines(one), "same read and write lines");
        String texts = one.split("\n\n", 2)[1];
        assertFalse(texts.contains("TURN"), "no TURN in level 1");
        assertFalse(texts.contains("#"), "no rock in level 1");
        assertTrue(one.contains("1 crystal (worth 1)"), "level 1: every crystal is worth 1");
        assertFalse(one.contains("1-3"), "level 1: no values 1-3");
        assertFalse(Files.exists(Path.of("config/level4/stub.txt")), "level 4 uses the shared stub");
        assertTrue(texts(shared).contains("1-3 crystal value"), "shared stub = level 4: values 1-3");
        assertTrue(texts(shared).contains("pending") && texts(shared).contains("shattered"), "shared stub = level 4: pending and wear");
    }

    private static String texts(String stub) {
        return stub.split("\n\n", 2)[1];
    }

    @Test
    void levelTwoStubHasRocksTurnsAndStreaksButNoPendingNoWear() throws Exception {
        String shared = Files.readString(Path.of("config/stub.txt"));
        Path l2 = Path.of("config/level2/stub.txt");
        assertTrue(Files.exists(l2), "config/level2/stub.txt");
        String two = Files.readString(l2);
        assertEquals(readLines(shared), readLines(two), "same read and write lines");
        String t = texts(two);
        assertTrue(t.contains("# rock") && t.contains("TURN x y"), "rock and TURN exist in level 2");
        assertTrue(t.contains("1 crystal (worth 1)") && !t.contains("1-3"), "crystals worth 1");
        assertFalse(t.contains("pending") || t.contains("shatter"), "no pending mirror, no wear in level 2");
        assertTrue(t.contains("your streak"), "streaks exist in level 2");
    }

    @Test
    void levelThreeStubHasPendingAndWearButCrystalsWorthOne() throws Exception {
        String shared = Files.readString(Path.of("config/stub.txt"));
        Path l3 = Path.of("config/level3/stub.txt");
        assertTrue(Files.exists(l3), "config/level3/stub.txt");
        String three = Files.readString(l3);
        assertEquals(readLines(shared), readLines(three), "same read and write lines");
        String t = texts(three);
        assertTrue(t.contains("pending") && t.contains("shattered"), "pending and wear exist in level 3");
        assertTrue(t.contains("1 crystal (worth 1)") && !t.contains("1-3"), "crystals worth 1");
    }

    /** A text line ending with a backslash becomes a line continuation in the C/C++ starter comments (stubcheck v20). */
    @Test
    void noStubTextLineEndsWithABackslash() throws Exception {
        for (String f : new String[] {"config/stub.txt", "config/level1/stub.txt", "config/level2/stub.txt", "config/level3/stub.txt"})
            for (String line : Files.readString(Path.of(f)).split("\n")) assertFalse(line.endsWith("\\"), f + ": " + line);
    }
}
