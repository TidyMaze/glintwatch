package com.codingame.game.bots;

import java.lang.management.ManagementFactory;
import java.util.*;

import com.codingame.game.engine.*;

/** CPU-time per act() of bot slot 0 (thread CPU time, robust to machine load). Usage: TimeProbe games league seedStart bot0 bot1.. */
public final class TimeProbe {
    public static void main(String[] a) throws Exception {
        int games = Integer.parseInt(a[0]);
        Config cfg = Config.forLeague(Integer.parseInt(a[1]));
        long seed0 = Long.parseLong(a[2]);
        String[] names = Arrays.copyOfRange(a, 3, a.length);
        int pc = names.length;
        java.lang.management.ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        Map<Integer, List<Double>> byN = new TreeMap<>();
        List<Double> first = new ArrayList<>();
        int wins = 0;
        for (int gi = 0; gi < games; gi++) {
            long seed = seed0 + gi;
            Game g = Game.fromSeed(seed, pc, cfg);
            Bot[] bots = new Bot[pc];
            for (int id = 0; id < pc; id++) {
                bots[id] = Agents.create(names[id], cfg, seed);
                long t = mx.getCurrentThreadCpuTime();
                bots[id].init(Protocol.initLinesV5(g, id));
                if (id == 0) first.add((mx.getCurrentThreadCpuTime() - t) / 1e6);
            }
            while (!g.isOver()) {
                Command[] cmds = new Command[pc];
                for (int id = 0; id < pc; id++) {
                    if (!g.active[id]) continue;
                    long t = mx.getCurrentThreadCpuTime();
                    cmds[id] = CommandParser.parse(bots[id].act(Protocol.turnLinesV5(g, id)));
                    if (id == 0) byN.computeIfAbsent(g.board.n, k -> new ArrayList<>()).add((mx.getCurrentThreadCpuTime() - t) / 1e6);
                }
                g.playTurn(cmds);
            }
            int best = Integer.MIN_VALUE;
            for (int s : g.score) best = Math.max(best, s);
            if (g.score[0] == best) wins++;
        }
        for (Map.Entry<Integer, List<Double>> e : byN.entrySet()) {
            List<Double> l = e.getValue();
            Collections.sort(l);
            System.out.printf("N=%d turns %d cpu ms/turn mean %.2f p99 %.2f max %.2f%n", e.getKey(), l.size(),
                l.stream().mapToDouble(d -> d).average().orElse(0), l.get((int) (l.size() * 0.99)), l.get(l.size() - 1));
        }
        System.out.printf("slot0 first %.1f%% (seat 0 fixed)%n", 100.0 * wins / games);
    }
}
