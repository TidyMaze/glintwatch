package com.codingame.game.qa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.codingame.game.engine.Config;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/**
 * QA fuzz run (M7) through the SDK runner and the real Referee (parser, summaries, viewer).
 * Args: [seeds per (league, players)] [first seed] [replay check 0/1]. Prints one line per failure and a summary.
 * Asserts: no referee error, every input line well-formed (F5), deactivation exactly when a line is malformed,
 * a bot crashes or sleeps past the limit (F6.1, F8), game end when fewer than 2 are active, score -1 when
 * deactivated, and the same seed and plans give the same views, summaries and scores.
 */
public final class FuzzMain {
    public static final class Outcome {
        public int games, failures, eliminations, referee_errors, nondeterministic, harnessRetries;
        public final List<String> hashes = new ArrayList<>();
        public final List<String> lines = new ArrayList<>();
    }

    static final int FIRST_LIMIT = 1000, TURN_LIMIT = 50;

    /** false: no sleeping bot (JUnit: no assert depends on the wall clock beyond "no answer at all"). */
    public static boolean sleeps = true;

    public static FuzzAgents.Plan[] plans(long seed, int players) {
        Random r = new Random(seed * 31 + players);
        FuzzAgents.Plan[] ps = new FuzzAgents.Plan[players];
        double[] rates = {0, 0, 0.002, 0.01, 0.05};
        for (int i = 0; i < players; i++) {
            double rate = rates[r.nextInt(rates.length)];
            int sleepTurn = 0, sleepMs = 0, crash = 0;
            int k = r.nextInt(20);
            if (k == 0 && sleeps) { sleepTurn = 1; sleepMs = 1300; }
            else if (k == 1 && sleeps) { sleepTurn = 1 + r.nextInt(150); sleepMs = 300; }
            else if (k == 2) crash = 1 + r.nextInt(150);
            else if (k == 3) crash = 1;
            ps[i] = new FuzzAgents.Plan(seed * 1000 + i, rate, sleepTurn, sleepMs, crash);
        }
        return ps;
    }

    static GameResult play(int league, long seed, FuzzAgents.Plan[] ps) {
        for (int i = 0; i < ps.length; i++) FuzzAgents.publish(i, ps[i]);
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.setSeed(seed);
        runner.setLeagueLevel(league);
        for (int i = 0; i < ps.length; i++) runner.addAgent(FuzzAgents.SEATS[i], "Fuzz" + i);
        return runner.simulate();
    }

    static FuzzAgents.Plan[] copy(FuzzAgents.Plan[] ps) {
        FuzzAgents.Plan[] c = new FuzzAgents.Plan[ps.length];
        for (int i = 0; i < ps.length; i++)
            c[i] = new FuzzAgents.Plan(ps[i].seed, ps[i].malformedRate, ps[i].sleepTurn, ps[i].sleepMs, ps[i].crashTurn);
        return c;
    }

    /** Checks one game; returns failure texts. */
    public static List<String> check(int league, long seed, FuzzAgents.Plan[] ps, GameResult r, Outcome o) {
        List<String> f = new ArrayList<>();
        String tag = "league " + league + " players " + ps.length + " seed " + seed + ": ";
        if (r.failCause != null && !r.failCause.isEmpty()) f.add(tag + "failCause " + r.failCause);
        for (String e : r.errors.getOrDefault("referee", Collections.emptyList()))
            if (!com.codingame.game.RefereeTest.isDiagnostic(e)) {
                o.referee_errors++;
                f.add(tag + "referee error " + e.substring(0, Math.min(300, e.length())).replace('\n', '|'));
            }
        int maxTurns = Config.forLeague(league).maxTurns; // V6: 150 in every league
        int[] d = new int[ps.length];
        for (int i = 0; i < ps.length; i++) {
            d[i] = Integer.MAX_VALUE;
            for (int t = 1; t <= ps[i].sent.size() && d[i] == Integer.MAX_VALUE; t++) {
                boolean slow = t == ps[i].sleepTurn && ps[i].sleepMs > (t == 1 ? FIRST_LIMIT : TURN_LIMIT);
                if (!FuzzAgents.wellFormed(ps[i].sent.get(t - 1)) || t == ps[i].crashTurn || slow) d[i] = t;
            }
        }
        int end = maxTurns;
        for (int t = 1; t <= maxTurns; t++) {
            int active = 0;
            for (int i = 0; i < ps.length; i++) if (d[i] > t) active++;
            if (active < 2) {
                end = t;
                break;
            }
        }
        String all = String.join("\n", r.summaries.stream().map(s -> s == null ? "" : s).toArray(String[]::new));
        if (all.contains("Game over: ")) { // DESIGN V5.5 early end (stale or decided): the game stops before maxTurns
            int last = 0;
            for (int i = 0; i < ps.length; i++) if (d[i] > end) last = Math.max(last, ps[i].sent.size());
            end = Math.min(end, last);
        }
        for (int i = 0; i < ps.length; i++) {
            boolean out = d[i] <= end;
            int played = Math.min(d[i], end);
            if (ps[i].sent.size() != played)
                f.add(tag + "player " + i + " played " + ps[i].sent.size() + " turns, expected " + played + " (d=" + d[i] + ", end=" + end + ")");
            int sc = r.scores.getOrDefault(i, Integer.MIN_VALUE);
            if (out != (sc == -1)) f.add(tag + "player " + i + " score " + sc + " but eliminated=" + out);
            int count = 0;
            for (int at = all.indexOf("$" + i + " eliminated"); at >= 0; at = all.indexOf("$" + i + " eliminated", at + 1)) count++;
            if (count != (out ? 1 : 0)) f.add(tag + "player " + i + " has " + count + " elimination summaries, eliminated=" + out);
            if (out) o.eliminations++;
            for (String v : ps[i].violations) f.add(tag + "player " + i + " input " + v);
        }
        return f;
    }

    /**
     * The SDK WorldState keeps entity states in a map keyed by entity identity, so the order of entity updates
     * inside a frame and group children lists change between runs; the content does not. Compare frames as sorted
     * update chunks with sorted children lists.
     */
    static final java.util.regex.Pattern CHILDREN = java.util.regex.Pattern.compile(" ch ([0-9,]+)");

    static List<String> canon(List<String> views) {
        List<String> c = new ArrayList<>();
        for (String v : views) {
            if (v == null) {
                c.add(null);
                continue;
            }
            String[] parts = v.split("[;\\n\"{}]|\\\\n");
            for (int k = 0; k < parts.length; k++) {
                parts[k] = parts[k].replaceFirst("^U(?=[0-9])", "");
                java.util.regex.Matcher m = CHILDREN.matcher(parts[k]);
                if (m.find()) {
                    String[] ids = m.group(1).split(",");
                    java.util.Arrays.sort(ids);
                    parts[k] = parts[k].substring(0, m.start(1)) + String.join(",", ids) + parts[k].substring(m.end(1));
                }
            }
            java.util.Arrays.sort(parts);
            c.add(String.join("\n", parts));
        }
        return c;
    }

    static String firstDiff(List<String> a, List<String> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            String x = String.valueOf(a.get(i)), y = String.valueOf(b.get(i));
            if (!x.equals(y)) {
                int k = 0;
                while (k < Math.min(x.length(), y.length()) && x.charAt(k) == y.charAt(k)) k++;
                int from = Math.max(0, k - 60);
                return " frame " + i + " at " + k + ": [" + x.substring(from, Math.min(x.length(), k + 60)) + "] vs ["
                    + y.substring(from, Math.min(y.length(), k + 60)) + "]";
            }
        }
        return " sizes " + a.size() + " vs " + b.size();
    }

    /** SDK in-process runner noise (random agent class names can collide: "frozen class"), not a referee fault. */
    static boolean harnessFault(GameResult r) {
        for (String e : r.errors.getOrDefault("referee", Collections.emptyList()))
            if (e != null && e.contains("Agent failed!")) return true;
        return false;
    }

    static String digest(GameResult r) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            for (String v : canon(r.views)) md.update(String.valueOf(v).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update(String.valueOf(r.summaries).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update(String.valueOf(r.scores).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] h = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param replay also replay each game in this JVM and compare summaries and scores. Views are compared across
     *     JVMs through the HASH lines (entity ids are a JVM-wide counter, so they only match in a fresh JVM, as on CG).
     */
    public static Outcome run(int seeds, long first, boolean replay, int[] leagues, int[] counts) {
        Outcome o = new Outcome();
        for (int league : leagues)
            for (int players : counts)
                for (long seed = first; seed < first + seeds; seed++) {
                    String tag = "league " + league + " players " + players + " seed " + seed;
                    FuzzAgents.Plan[] ps = null;
                    GameResult r = null;
                    try {
                        for (int attempt = 0; attempt < 3; attempt++) {
                            ps = plans(seed, players);
                            r = play(league, seed, ps);
                            if (!harnessFault(r)) break;
                            o.harnessRetries++;
                        }
                    } catch (RuntimeException e) {
                        o.failures++;
                        o.lines.add(tag + ": runner exception " + e);
                        continue;
                    }
                    o.games++;
                    List<String> f = check(league, seed, ps, r, o);
                    o.hashes.add("HASH " + tag + " " + digest(r));
                    if (replay) {
                        GameResult r2 = play(league, seed, copy(ps));
                        if (!harnessFault(r2) && (!r.summaries.equals(r2.summaries) || !r.scores.equals(r2.scores))) {
                            o.nondeterministic++;
                            f.add(tag + ": replay differs (summaries " + r.summaries.equals(r2.summaries) + ", scores " + r.scores
                                + " vs " + r2.scores + ")");
                        }
                    }
                    if (!f.isEmpty()) o.failures++;
                    o.lines.addAll(f);
                }
        return o;
    }

    public static void main(String[] a) {
        int seeds = a.length > 0 ? Integer.parseInt(a[0]) : 10;
        long first = a.length > 1 ? Long.parseLong(a[1]) : 1;
        boolean replay = a.length > 2 && a[2].equals("1");
        long t0 = System.nanoTime();
        Outcome o = run(seeds, first, replay, new int[] {1, 2, 3, 4}, new int[] {2, 3, 4});
        for (String l : o.lines) System.out.println("FAIL " + l);
        for (String l : o.hashes) System.out.println(l);
        System.out.printf("FUZZ games %d failing %d eliminations %d referee_errors %d nondeterministic %d harness_retries %d seconds %.1f%n",
            o.games, o.failures, o.eliminations, o.referee_errors, o.nondeterministic, o.harnessRetries, (System.nanoTime() - t0) / 1e9);
        System.exit(0);
    }
}
