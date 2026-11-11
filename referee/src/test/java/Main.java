import java.util.Arrays;
import java.util.List;

import com.codingame.game.bots.Agents;
import com.codingame.gameengine.runner.MultiplayerGameRunner;

/**
 * Local viewer at http://localhost:8888/test.html .
 * Args: [seed] [league 1-4] [bot ...] with bots among wait (stub) and random; 2 to 4 bots.
 * Default: seed 1, league 4 (Silver), wait random random random.
 */
public class Main {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1L;
        int league = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        List<String> bots = args.length > 2 ? Arrays.asList(args).subList(2, args.length) : Arrays.asList("wait", "random", "random", "random");
        System.setProperty("glintwatch.league", String.valueOf(league));
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
        gameRunner.setSeed(seed);
        gameRunner.setLeagueLevel(league);
        // -Dgw.houses=true: neutral nicknames (the four houses), e.g. to record the intro replay
        String[] houses = {"Sky Ring", "Amber Spire", "Jade Keep", "Orchid Star"};
        boolean neutral = Boolean.getBoolean("gw.houses");
        for (int i = 0; i < bots.size(); i++) gameRunner.addAgent(agent(bots.get(i)), neutral ? houses[i] : bots.get(i));
        gameRunner.start(Integer.getInteger("gw.port", 8888));
    }

    static Class<?> agent(String name) {
        switch (name) {
            case "wait": return Agents.Wait.class;
            case "random": return Agents.Random.class;
            default: throw new IllegalArgumentException("unknown bot " + name);
        }
    }
}
