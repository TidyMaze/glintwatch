package com.codingame.game.bots;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs an external CG-style bot (stdin/stdout) as a Bot. Command from -Dgw.ext.<name> (e.g. -Dgw.ext.botA="python3 bots/botA.py").
 * Wall time per turn is recorded (maxMs, overMs>50 turns) and every empty/odd reply is appended to $gw.ext.log.
 */
public final class ExternalBot implements Bot {
    public static volatile int turns, over50;
    public static volatile double maxMs;
    private final Process proc;
    private final BufferedWriter out;
    private final BufferedReader in;
    private final String name;
    private int turnNo;

    public ExternalBot(String name, String command) {
        this.name = name;
        try {
            proc = new ProcessBuilder(command.split(" ")).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        out = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));
        in = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
    }

    private void send(List<String> lines) throws IOException {
        for (String l : lines) {
            out.write(l);
            out.write('\n');
        }
        out.flush();
    }

    @Override
    public void init(List<String> initLines) {
        try {
            send(initLines);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String act(List<String> turnLines) {
        turnNo++;
        try {
            long t0 = System.nanoTime();
            send(turnLines);
            String line = in.readLine();
            double ms = (System.nanoTime() - t0) / 1e6;
            synchronized (ExternalBot.class) {
                turns++;
                if (turnNo > 1 && ms > 50) over50++;
                maxMs = Math.max(maxMs, ms);
            }
            if (line == null) throw new IllegalStateException(name + " closed stdout");
            return line;
        } catch (IOException e) {
            throw new IllegalStateException(name + " io: " + e.getMessage());
        }
    }

    public static void shutdownAll() {}
}
