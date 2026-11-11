import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/** Reproduces the CG preview: league 1, starter binary vs Boss.py, external processes. Args: starter seed league. */
public class CgRepro {
    public static void main(String[] a) {
        long t0 = System.nanoTime();
        MultiplayerGameRunner r = new MultiplayerGameRunner();
        r.setSeed(Long.parseLong(a[1]));
        r.setLeagueLevel(Integer.parseInt(a[2]));
        r.addAgent(a[0], "starter");
        r.addAgent("python3 config/level" + a[2] + "/Boss.py", "boss");
        GameResult g = r.simulate();
        System.out.println("scores " + g.scores + " frames " + g.views.size() + " failCause " + g.failCause + " in "
            + (System.nanoTime() - t0) / 1_000_000 + " ms");
        g.errors.forEach((k, v) -> v.stream().filter(x -> x != null && !x.isEmpty()).limit(2)
            .forEach(x -> System.out.println("ERR " + k + " " + x.substring(0, Math.min(300, x.length())).replace('\n', '|'))));
        System.exit(0);
    }
}
