import com.codingame.game.bots.Agents;
import com.codingame.game.engine.MapGenerator;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

public class SizeSweep {
    public static void main(String[] a) {
        Class<?>[][] mixes = {{Agents.Random.class, Agents.Random.class, Agents.Random.class, Agents.Random.class},
            {Agents.Plus.class, Agents.Plus.class, Agents.Plus.class, Agents.Plus.class}};
        String[] names = {"random4", "plus4"};
        for (int league = 3; league <= 4; league++)
            for (int m = 0; m < mixes.length; m++)
                for (long seed = 0; seed < 40 ; seed++) {
                    if (MapGenerator.generate(seed, false).n != 19 && seed % 8 != 0) continue;
                    System.setProperty("glintwatch.league", String.valueOf(league));
                    MultiplayerGameRunner r = new MultiplayerGameRunner();
                    r.setSeed(seed);
                    r.setLeagueLevel(league);
                    for (Class<?> c : mixes[m]) r.addAgent(c, "b");
                    GameResult g = r.simulate();
                    long size = 0;
                    for (String v : g.views) size += v == null ? 0 : v.length();
                    System.out.println("league " + league + " " + names[m] + " seed " + seed + " n " + MapGenerator.generate(seed, false).n + " chars " + size);
                }
        System.exit(0);
    }
}
