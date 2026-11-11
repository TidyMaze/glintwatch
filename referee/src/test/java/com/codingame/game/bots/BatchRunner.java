package com.codingame.game.bots;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.codingame.game.engine.*;

/**
 * Headless batch: N games from seedStart, 2-4 in-JVM bots fed with the exact protocol text, seats rotated each game.
 * Reports win shares per bot and per seat (ties split), crashes, over-budget turns (optionally enforced as timeouts),
 * mean turns, engine ms per turn; per-game records as CSV or JSON.
 * Usage: BatchRunner games league(1-4|v1) seedStart bot1 bot2 [bot3 bot4]
 *   -Dgw.* constant overrides (Tuning), -Dbatch.csv=file -Dbatch.json=file -Dbatch.enforce=true
 */
public final class BatchRunner {
    public static final class Options {
        public long turnBudgetMs = 50, firstTurnBudgetMs = 1000;
        /** Deactivate a bot over its budget, as the referee does; off by default (in-JVM timing is noisy). */
        public boolean enforceTimeouts = false;
    }

    /** One game, arrays by player id. */
    public static final class GameRecord {
        public long seed;
        public int turns;
        public int[] slot, seat, score, rank, overTime;
        public String[] bot;
        public double[] nonWaitShare, maxBotMs;
        public boolean[] crashed;
    }

    public static final class Result {
        public final double[] wins, seatWins;
        public final long[] totalScore, botNanos;
        public final List<GameRecord> records = new ArrayList<>();
        public final String[] names;
        public int games, crashes, timeouts;
        public long turns, refereeNanos, maxRefereeNanos;

        Result(String[] names) {
            this.names = names;
            wins = new double[names.length];
            seatWins = new double[names.length];
            totalScore = new long[names.length];
            botNanos = new long[names.length];
        }

        public double avgTurns() {
            return games == 0 ? 0 : (double) turns / games;
        }

        public double refereeMsPerTurn() {
            return turns == 0 ? 0 : refereeNanos / 1e6 / turns;
        }
    }

    private BatchRunner() {}

    public static Result run(String[] names, int games, long seedStart, Config cfg) {
        return run(names, games, seedStart, cfg, new Options());
    }

    public static Result run(String[] names, int games, long seedStart, Config cfg, Options opt) {
        Result res = new Result(names);
        for (int gi = 0; gi < games; gi++) res.records.add(playOne(names, gi, seedStart + gi, cfg, opt, res));
        return res;
    }

    private static GameRecord playOne(String[] names, int gi, long seed, Config cfg, Options opt, Result res) {
        int players = names.length;
        Game g = Game.fromSeed(seed, players, cfg);
        GameRecord rec = new GameRecord();
        rec.seed = seed;
        rec.slot = new int[players];
        rec.seat = g.seatOf.clone();
        rec.bot = new String[players];
        rec.overTime = new int[players];
        rec.maxBotMs = new double[players];
        rec.crashed = new boolean[players];
        int[] acted = new int[players], nonWait = new int[players];
        Bot[] bots = new Bot[players];
        for (int id = 0; id < players; id++) {
            rec.slot[id] = (id + gi) % players;
            rec.bot[id] = names[rec.slot[id]];
            bots[id] = Agents.create(rec.bot[id], cfg, seed);
            bots[id].init(Protocol.initLines(g.board, players, id, cfg));
        }
        while (!g.isOver()) {
            g.beginTurn();
            long budget = (g.turn == 0 ? opt.firstTurnBudgetMs : opt.turnBudgetMs) * 1_000_000L;
            Command[] cmds = new Command[players];
            for (int id = 0; id < players; id++) {
                if (!g.active[id]) continue;
                long t0 = System.nanoTime();
                try {
                    cmds[id] = CommandParser.parse(bots[id].act(Protocol.turnLines(g, id)));
                } catch (Exception | Error ex) {
                    res.crashes++;
                    rec.crashed[id] = true;
                    g.deactivate(id);
                }
                long dt = System.nanoTime() - t0;
                res.botNanos[rec.slot[id]] += dt;
                rec.maxBotMs[id] = Math.max(rec.maxBotMs[id], dt / 1e6);
                if (dt > budget && g.active[id]) {
                    rec.overTime[id]++;
                    if (opt.enforceTimeouts) {
                        res.timeouts++;
                        g.deactivate(id);
                        cmds[id] = null;
                    }
                }
                if (cmds[id] != null) {
                    acted[id]++;
                    if (cmds[id].kind != Command.Kind.WAIT) nonWait[id]++;
                }
            }
            long t0 = System.nanoTime();
            g.playTurn(cmds);
            long dt = System.nanoTime() - t0;
            res.refereeNanos += dt;
            res.maxRefereeNanos = Math.max(res.maxRefereeNanos, dt);
            res.turns++;
        }
        res.games++;
        rec.turns = g.turn;
        rec.score = g.score.clone();
        rec.rank = ranks(rec.score);
        rec.nonWaitShare = new double[players];
        int tied = 0;
        for (int r : rec.rank) if (r == 1) tied++;
        for (int id = 0; id < players; id++) {
            rec.nonWaitShare[id] = acted[id] == 0 ? 0 : (double) nonWait[id] / acted[id];
            res.totalScore[rec.slot[id]] += rec.score[id];
            if (rec.rank[id] == 1) {
                res.wins[rec.slot[id]] += 1.0 / tied;
                res.seatWins[id] += 1.0 / tied;
            }
        }
        return rec;
    }

    /** F7: score descending, equal scores share the best rank (deactivated = -1 = last). */
    public static int[] ranks(int[] score) {
        int[] rank = new int[score.length];
        for (int i = 0; i < score.length; i++) {
            rank[i] = 1;
            for (int s : score) if (s > score[i]) rank[i]++;
        }
        return rank;
    }

    public static void writeCsv(Result r, Writer w) throws IOException {
        w.write("seed,players,id,seat,bot,score,rank,nonWaitShare,maxBotMs,overTime,crashed\n");
        for (GameRecord g : r.records)
            for (int id = 0; id < g.bot.length; id++)
                w.write(String.format(java.util.Locale.ROOT, "%d,%d,%d,%d,%s,%d,%d,%.4f,%.3f,%d,%b%n", g.seed, g.bot.length, id,
                    g.seat[id], g.bot[id], g.score[id], g.rank[id], g.nonWaitShare[id], g.maxBotMs[id], g.overTime[id], g.crashed[id]));
        w.flush();
    }

    public static void writeJson(Result r, Writer w) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"games\":").append(r.games).append(",\"crashes\":").append(r.crashes).append(",\"timeouts\":").append(r.timeouts)
            .append(",\"avgTurns\":").append(r.avgTurns()).append(",\"refereeMsPerTurn\":").append(r.refereeMsPerTurn())
            .append(",\"bots\":[");
        for (int i = 0; i < r.names.length; i++)
            sb.append(i > 0 ? "," : "").append("{\"name\":\"").append(r.names[i]).append("\",\"wins\":").append(r.wins[i])
                .append(",\"meanScore\":").append(r.games == 0 ? 0 : (double) r.totalScore[i] / r.games).append('}');
        sb.append("],\"seatWins\":").append(Arrays.toString(r.seatWins).replace(" ", "")).append('}');
        w.write(sb.toString());
        w.flush();
    }

    public static void main(String[] args) throws IOException {
        int games = Integer.parseInt(args[0]);
        String league = args[1];
        Config cfg = league.equals("v1") ? Config.v1Bronze() : Config.forLeague(Integer.parseInt(league));
        Tuning.apply(cfg, System.getProperties());
        long seedStart = Long.parseLong(args[2]);
        String[] names = Arrays.copyOfRange(args, 3, args.length);
        Options opt = new Options();
        opt.enforceTimeouts = Boolean.getBoolean("batch.enforce");
        long t0 = System.nanoTime();
        Result r = run(names, games, seedStart, cfg, opt);
        System.out.printf("games %d league %s players %d crashes %d timeouts %d avgTurns %.1f refereeMsPerTurn %.4f maxRefereeMs %.3f wall %.1fs%n",
            r.games, league, names.length, r.crashes, r.timeouts, r.avgTurns(), r.refereeMsPerTurn(), r.maxRefereeNanos / 1e6,
            (System.nanoTime() - t0) / 1e9);
        for (int i = 0; i < names.length; i++)
            System.out.printf("%-8s win %5.1f%%  meanScore %8.1f  botMsPerTurn %.3f%n", names[i], 100 * r.wins[i] / r.games,
                (double) r.totalScore[i] / r.games, r.botNanos[i] / 1e6 / Math.max(1, r.turns));
        StringBuilder seats = new StringBuilder("seat wins %:");
        for (double w : r.seatWins) seats.append(String.format(" %.1f", 100 * w / r.games));
        System.out.println(seats);
        String csv = System.getProperty("batch.csv"), json = System.getProperty("batch.json");
        if (csv != null) try (Writer w = new FileWriter(csv)) { writeCsv(r, w); }
        if (json != null) try (Writer w = new FileWriter(json)) { writeJson(r, w); }
    }
}
