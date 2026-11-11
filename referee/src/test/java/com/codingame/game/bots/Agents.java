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

    /** Builds a bot by name: wait, random, crash, slow. */
    public static Bot create(String name, Config cfg, long seed) {
        switch (name) {
            case "wait": return new WaitBot();
            case "random": return new RandomBot(cfg, seed);
            case "crash": return new CrashBot();
            case "slow": return new SlowBot();
            default: throw new IllegalArgumentException("unknown bot " + name);
        }
    }

    /** Streams come from the agent class: the SDK rewrites System.in/out only inside that class. */
    public static void loop(Bot bot, java.io.InputStream stdin, java.io.PrintStream stdout) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stdin));
        List<String> init = new ArrayList<>();
        String reef = in.readLine();
        if (reef == null) return;
        init.add(reef);
        init.add(in.readLine());
        bot.init(init);
        while (true) {
            List<String> turn = new ArrayList<>();
            String head = in.readLine();
            if (head == null) return;
            turn.add(head);
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
        /** Seed from -Dglintwatch.botSeed (default 1): the engine's BatchRunner seeds its random bot with the game seed. */
        public static void main(String[] a) throws IOException { loop(new RandomBot(cfg(), Long.getLong("glintwatch.botSeed", 1L)), System.in, System.out); }
    }

    /** WAIT on turn 1, then sleeps past the 50 ms turn limit: the referee must eliminate it for timeout. */
    public static class Sleepy {
        public static void main(String[] a) throws IOException {
            loop(new SleepyBot(), System.in, System.out);
        }
    }

    public static final class SleepyBot implements Bot {
        private int turn;

        public void init(List<String> l) {}

        public String act(List<String> l) {
            if (++turn >= 2) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "WAIT";
        }
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
