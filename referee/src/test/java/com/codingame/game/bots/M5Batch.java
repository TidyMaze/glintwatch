package com.codingame.game.bots;

import com.codingame.game.engine.*;

/**
 * M5 batch: like BatchRunner plus seat (player id) win shares, non-WAIT share per slot, board changes per turn and
 * mean rank per slot. Usage: M5Batch games league seedStart bot1 bot2 [bot3 bot4]. Same -Dgw.* overrides.
 */
public final class M5Batch {
    public static void main(String[] args) {
        int games = Integer.parseInt(args[0]);
        Config cfg = Config.forLeague(Integer.parseInt(args[1]));
        Tuning.apply(cfg, System.getProperties());
        long seedStart = Long.parseLong(args[2]);
        String[] names = java.util.Arrays.copyOfRange(args, 3, args.length);
        int pc = names.length;
        double[] slotWin = new double[pc], seatWin = new double[pc], rankSum = new double[pc];
        long[] actions = new long[pc], nonWait = new long[pc], score = new long[pc];
        long turns = 0, changes = 0, crashes = 0;
        for (int gi = 0; gi < games; gi++) {
            long seed = seedStart + gi;
            Game g = Game.fromSeed(seed, pc, cfg);
            int[] slotOf = new int[pc];
            Bot[] bots = new Bot[pc];
            for (int id = 0; id < pc; id++) {
                slotOf[id] = (id + gi) % pc;
                bots[id] = Agents.create(names[slotOf[id]], cfg, seed);
                bots[id].init(Protocol.initLinesV5(g, id));
            }
            while (!g.isOver()) {
                Command[] cmds = new Command[pc];
                for (int id = 0; id < pc; id++) {
                    if (!g.active[id]) continue;
                    try {
                        cmds[id] = CommandParser.parse(bots[id].act(Protocol.turnLinesV5(g, id)));
                    } catch (Exception | Error ex) {
                        crashes++;
                        g.deactivate(id);
                        continue;
                    }
                    actions[slotOf[id]]++;
                    if (cmds[id].kind != Command.Kind.WAIT) nonWait[slotOf[id]]++;
                }
                TurnReport r = g.playTurn(cmds);
                changes += r.placed.size() + r.turned.size() + r.broken.size();
                turns++;
            }
            int best = Integer.MIN_VALUE, tied = 0;
            for (int id = 0; id < pc; id++) best = Math.max(best, g.score[id]);
            for (int id = 0; id < pc; id++) if (g.score[id] == best) tied++;
            for (int id = 0; id < pc; id++) {
                score[slotOf[id]] += g.score[id];
                int better = 0;
                for (int o = 0; o < pc; o++) if (g.score[o] > g.score[id]) better++;
                rankSum[slotOf[id]] += better + 1;
                if (g.score[id] == best) {
                    slotWin[slotOf[id]] += 1.0 / tied;
                    seatWin[id] += 1.0 / tied;
                }
            }
        }
        System.out.printf("games %d league %s crashes %d boardChanges/turn %.2f%n", games, args[1], crashes, (double) changes / turns);
        StringBuilder seats = new StringBuilder("seat first%:");
        for (int i = 0; i < pc; i++) seats.append(String.format(" %.1f", 100 * seatWin[i] / games));
        System.out.println(seats);
        for (int i = 0; i < pc; i++)
            System.out.printf("%-8s first %5.1f%% meanRank %.2f meanScore %7.1f nonWait %4.1f%%%n", names[i], 100 * slotWin[i] / games,
                rankSum[i] / games, (double) score[i] / games, 100.0 * nonWait[i] / Math.max(1, actions[i]));
    }
}
