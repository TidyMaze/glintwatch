package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import com.codingame.game.bots.Agents;
import com.codingame.game.bots.BatchRunner;
import com.codingame.game.engine.Config;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/** config/levelK boss files: single-file bots (DESIGN F9) that must play exactly like the in-JVM bots. */
class BossFilesTest {
    private static final String[] BOT = {null, "strong1", "strong2", "strong3", "strong4"};

    private static Path compile(int level) throws Exception {
        Path src = Path.of("config/level" + level + "/Boss.java");
        assertTrue(Files.exists(src), "missing " + src);
        Path dir = Files.createTempDirectory("boss" + level);
        Files.copy(src, dir.resolve("Player.java"));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, "-d", dir.toString(), dir.resolve("Player.java").toString()), "javac level " + level);
        return dir;
    }

    /** V18 (owner 2026-11-14): every level's boss is the StrongBot of that level; one Boss file per level. */
    @Test
    void everyLevelHasExactlyOneJavaBoss() throws Exception {
        for (int level = 1; level <= 4; level++)
            assertTrue(Files.readString(Path.of("config/level" + level + "/Boss.java")).contains("new StrongBot(cfg, " + level + ")"), "level " + level);
        for (int level = 1; level <= 4; level++) {
            File[] bosses = new File("config/level" + level).listFiles((d, n) -> n.startsWith("Boss."));
            assertEquals(1, bosses.length, "exactly one Boss file in level " + level);
        }
    }

    /** CG may compile the boss under its own name (Boss.java): a public class Player would not compile there. */
    @Test
    void javaBossesCompileUnderTheirOwnFileNameAndRunAsPlayer() throws Exception {
        for (int level = 1; level <= 4; level++) {
            Path dir = Files.createTempDirectory("bossname" + level);
            Path src = dir.resolve("Boss.java");
            Files.copy(Path.of("config/level" + level + "/Boss.java"), src);
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            assertEquals(0, javac.run(null, null, null, "-d", dir.toString(), src.toString()), "javac Boss.java level " + level);
            assertTrue(Files.exists(dir.resolve("Player.class")), "main class Player level " + level);
        }
    }

    @Test
    void javaBossesPlayLikeTheirInJvmBotsAgainstTheReferee() throws Exception {
        for (int level = 1; level <= 4; level++) {
            Path dir = compile(level);
            System.setProperty("glintwatch.league", String.valueOf(level));
            for (int seatOfBoss = 0; seatOfBoss < 2; seatOfBoss++) { // relative ids: the boss must also play from seat 2
                MultiplayerGameRunner runner = new MultiplayerGameRunner();
                runner.setSeed(3L);
                runner.setLeagueLevel(level);
                if (seatOfBoss == 1) runner.addAgent(Agents.Self.class, "self");
                runner.addAgent("java -cp " + dir + " Player", "Boss");
                if (seatOfBoss == 0) runner.addAgent(Agents.Self.class, "self");
                runner.addAgent(Agents.Greedy.class, "greedy");
                GameResult r = runner.simulate();
                String[] names = seatOfBoss == 0 ? new String[] {BOT[level], "self", "greedy"} : new String[] {"self", BOT[level], "greedy"};
                BatchRunner.Result e = BatchRunner.run(names, 1, 3L, Config.forLeague(level));
                for (int i = 0; i < 3; i++)
                    assertEquals(e.totalScore[i], r.scores.get(i).intValue(), "level " + level + " boss in slot " + seatOfBoss + " player " + i + " " + cut(String.join(" | ", r.summaries.stream().filter(x -> x != null && !x.isEmpty()).toList()), 600) + " stderr " + cut(r.errors.toString(), 400));
                assertTrue(r.scores.get(seatOfBoss) >= 0, "boss eliminated in level " + level);
            }
        }
    }

    private static String cut(String s, int n) {
        s = s.replace("\n", " ");
        return s.length() <= n ? s : s.substring(0, n);
    }
}
