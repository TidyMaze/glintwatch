package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** v20: level 1 has its own stub (no rock, no TURN in the texts) that reads exactly the same lines as the shared stub. */
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
        for (int level = 2; level <= 4; level++) assertFalse(Files.exists(Path.of("config/level" + level + "/stub.txt")), "level " + level + " uses the shared stub");
    }

    /** A text line ending with a backslash becomes a line continuation in the C/C++ starter comments (stubcheck v20). */
    @Test
    void noStubTextLineEndsWithABackslash() throws Exception {
        for (String f : new String[] {"config/stub.txt", "config/level1/stub.txt"})
            for (String line : Files.readString(Path.of(f)).split("\n")) assertFalse(line.endsWith("\\"), f + ": " + line);
    }
}
