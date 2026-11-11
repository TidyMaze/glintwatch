package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Moderator review (STATEMENT-HTML-SPEC.md): every level statement has the CG standard sections. */
class StatementFilesTest {
    private static final String[] REQUIRED = {"class=\"statement_back\"", "statement-league-alert-content", "This league:",
        "statement-section statement-goal", "icon-goal", "statement-section statement-rules", "statement-rules-content",
        "statement-victory-conditions", "statement-lose-conditions", "statement-section statement-expertrules",
        "statement-section statement-protocol", "<div class=\"title\">Initialization Input</div>",
        "<div class=\"title\">Input for one game turn</div>", "<div class=\"title\">Output for one game turn</div>",
        "<div class=\"title\">Constraints</div>", "statement-lineno", "<action>PLACE</action>", "<const>", "<var>",
        "href=\"https://github.com/TidyMaze/glintwatch\""};

    private static String level(int k) throws Exception {
        return new String(Files.readAllBytes(Path.of("config/level" + k + "/statement_en.html")), StandardCharsets.UTF_8);
    }

    @Test
    void everyLevelHasTheStandardSections() throws Exception {
        for (int k = 1; k <= 4; k++) {
            String s = level(k);
            for (String r : REQUIRED) assertTrue(s.contains(r), "level " + k + " lacks " + r);
        }
    }

    @Test
    void leaguePreviewInAllButTheTopLevel() throws Exception {
        for (int k = 1; k <= 3; k++) assertTrue(level(k).contains("What is in store for me in the higher leagues?"), "level " + k);
        assertFalse(level(4).contains("What is in store for me in the higher leagues?"));
    }

    /** The 11 diagrams in statement order, as tagged per league in Statement v5 (writer, commit 7731e77). */
    private static final java.util.List<?>[] EXPECTED_DIAGRAMS = {
        java.util.List.of("setup.png", "mirrors.png"),
        java.util.List.of("setup.png", "stops.png", "mirrors.png", "lock.png", "contested.png", "streak.png"),
        java.util.List.of("setup.png", "stops.png", "mirrors.png", "lock.png", "contested.png", "streak.png", "build.png", "wear.png", "order.png"),
        java.util.List.of("setup.png", "stops.png", "mirrors.png", "lock.png", "contested.png", "streak.png", "build.png", "wear.png", "tides.png",
            "strike.png", "order.png")};

    /** Diagrams (moderator point 4): only the artist's files from art/png/statement, live on the public repo. */
    @Test
    void diagramsAreTheArtistsFilesInTheRightLevels() throws Exception {
        java.util.regex.Pattern img = java.util.regex.Pattern.compile("art/png/statement/([a-z]+\\.png)");
        for (int k = 1; k <= 4; k++) {
            java.util.List<String> names = new java.util.ArrayList<>();
            java.util.regex.Matcher m = img.matcher(level(k));
            while (m.find()) {
                names.add(m.group(1));
                assertTrue(Files.exists(Path.of("../art/png/statement/" + m.group(1))), "level " + k + ": " + m.group(1));
            }
            assertEquals(EXPECTED_DIAGRAMS[k - 1], names, "level " + k);
        }
    }
}
