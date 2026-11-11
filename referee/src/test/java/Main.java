import java.util.Arrays;
import java.util.List;

import com.codingame.game.bots.Agents;
import com.codingame.gameengine.runner.MultiplayerGameRunner;

/**
 * Local viewer at http://localhost:8888/test.html .
 * Args: [seed] [league 1-4] [bot ...] with bots among wait (stub), random, self, greedy, plus; 2 to 4 bots.
 * Default: seed 1, league 4 (Silver), wait greedy random plus.
 */
public class Main {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1L;
        int league = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        List<String> bots = args.length > 2 ? Arrays.asList(args).subList(2, args.length) : Arrays.asList("wait", "greedy", "random", "plus");
        System.setProperty("glintwatch.league", String.valueOf(league));
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
        gameRunner.setSeed(seed);
        gameRunner.setLeagueLevel(league);
        for (String b : bots) gameRunner.addAgent(agent(b), b);
        gameRunner.start(Integer.getInteger("gw.port", 8888));
    }

    static Class<?> agent(String name) {
        switch (name) {
            case "wait": return Agents.Wait.class;
            case "random": return Agents.Random.class;
            case "self": return Agents.Self.class;
            case "greedy": return Agents.Greedy.class;
            case "plus": return Agents.Plus.class;
            default: throw new IllegalArgumentException("unknown bot " + name);
        }
    }
}
