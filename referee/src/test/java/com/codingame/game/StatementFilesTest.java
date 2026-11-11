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

    @Test
    void noImageLinkToAFileThatIsNotShipped() throws Exception {
        for (int k = 1; k <= 4; k++)
            assertFalse(level(k).contains("art/png/statement/"), "level " + k + ": diagrams are dropped until the public repo has them");
    }
}
