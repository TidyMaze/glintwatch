package com.codingame.game.bots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.codingame.game.engine.Config;

/** SDK agents (stdin/stdout). League from system property glintwatch.league (default 3). */
public final class Agents {
    private Agents() {}

    public static Config cfg() {
        return Config.forLeague(Integer.getInteger("glintwatch.league", 3));
    }

    /** Builds a bot by name: wait, random, self, greedy, plus, crash, slow. */
    public static Bot create(String name, Config cfg, long seed) {
        switch (name) {
            case "wait": return new WaitBot();
            case "random": return new RandomBot(cfg, seed);
            case "self": return new GreedyBot(cfg, GreedyBot.Mode.SELF);
            case "greedy": return new GreedyBot(cfg, GreedyBot.Mode.FULL);
            case "plus": return new GreedyBot(cfg, GreedyBot.Mode.PLUS);
            case "crash": return new CrashBot();
            case "slow": return new SlowBot();
            default: throw new IllegalArgumentException("unknown bot " + name);
        }
    }

    /** Streams come from the agent class: the SDK rewrites System.in/out only inside that class. */
    public static void loop(Bot bot, java.io.InputStream stdin, java.io.PrintStream stdout) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stdin));
        List<String> init = new ArrayList<>();
        String first = in.readLine();
        if (first == null) return;
        init.add(first);
        int players = Integer.parseInt(first.split(" ")[0]);
        String nLine = in.readLine();
        init.add(nLine);
        int n = Integer.parseInt(nLine.trim());
        for (int i = 0; i < players + n; i++) init.add(in.readLine());
        readBlock(in, init);
        bot.init(init);
        while (true) {
            List<String> turn = new ArrayList<>();
            String turnLine = in.readLine();
            if (turnLine == null) return;
            turn.add(turnLine);
            for (int i = 0; i < players; i++) turn.add(in.readLine());
            readBlock(in, turn);
            readBlock(in, turn);
            stdout.println(bot.act(turn));
            stdout.flush();
        }
    }

    private static void readBlock(BufferedReader in, List<String> out) throws IOException {
        String count = in.readLine();
        out.add(count);
        for (int i = Integer.parseInt(count.trim()); i > 0; i--) out.add(in.readLine());
    }

    public static class Wait {
        public static void main(String[] a) throws IOException { loop(new WaitBot(), System.in, System.out); }
    }

    public static class Random {
        public static void main(String[] a) throws IOException { loop(new RandomBot(cfg(), 1), System.in, System.out); }
    }

    public static class Self {
        public static void main(String[] a) throws IOException { loop(new GreedyBot(cfg(), GreedyBot.Mode.SELF), System.in, System.out); }
    }

    public static class Greedy {
        public static void main(String[] a) throws IOException { loop(new GreedyBot(cfg(), GreedyBot.Mode.FULL), System.in, System.out); }
    }

    public static class Plus {
        public static void main(String[] a) throws IOException { loop(new GreedyBot(cfg(), GreedyBot.Mode.PLUS), System.in, System.out); }
    }

    /** Prints an unknown command: the referee must deactivate it. */
    public static class Garbage {
        public static void main(String[] a) throws IOException { loop(new FixedBot("HELLO"), System.in, System.out); }
    }

    /** WAIT with a message longer than the 24 chars the panel shows. */
    public static class Chatty {
        public static void main(String[] a) throws IOException {
            loop(new FixedBot("WAIT back\\slash and a message longer than 30"), System.in, System.out);
        }
    }

    /** Always answers the same line. */
    public static final class FixedBot implements Bot {
        private final String line;

        public FixedBot(String line) { this.line = line; }
        public void init(List<String> l) {}
        public String act(List<String> l) { return line; }
    }

    public static final class WaitBot implements Bot {
        public void init(List<String> l) {}
        public String act(List<String> l) { return "WAIT"; }
    }

    /** WAIT after 3 ms: exercises the batch time budget. */
    public static final class SlowBot implements Bot {
        public void init(List<String> l) {}
        public String act(List<String> l) {
            long end = System.nanoTime() + 3_000_000L;
            while (System.nanoTime() < end) Thread.onSpinWait();
            return "WAIT";
        }
    }

    public static final class CrashBot implements Bot {
        public void init(List<String> l) {}
        public String act(List<String> l) { throw new IllegalStateException("crash bot"); }
    }
}
