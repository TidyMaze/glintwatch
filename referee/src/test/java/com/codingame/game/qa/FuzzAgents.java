package com.codingame.game.qa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.codingame.game.bots.Agents;
import com.codingame.game.bots.Bot;

/**
 * QA fuzz agents (M7). Each seat class P0..P3 reads its Plan from {@link #plans}; games run one at a time.
 * A FuzzBot sends a seeded mix of valid, refused and malformed lines, can sleep or crash on a chosen turn,
 * records every line it sent and checks every input line it received against DESIGN.md F5.
 */
public final class FuzzAgents {
    private FuzzAgents() {}

    public static final class Plan {
        public final long seed;
        /** Probability that a turn's line is malformed (deactivates). */
        public final double malformedRate;
        /** Turn on which the bot sleeps sleepMs before answering (0 = never). */
        public final int sleepTurn, sleepMs;
        /** Turn on which the bot throws instead of answering (0 = never). */
        public final int crashTurn;
        public final List<String> sent, violations;
        int n = -1, playerCount, myId, crystalCount;
        List<String> rows = new ArrayList<>();
        List<String> crystals = new ArrayList<>();
        List<int[]> mirrors = new ArrayList<>();

        public Plan(long seed, double malformedRate, int sleepTurn, int sleepMs, int crashTurn) {
            this(seed, malformedRate, sleepTurn, sleepMs, crashTurn, new ArrayList<>(), new ArrayList<>());
        }

        Plan(long seed, double malformedRate, int sleepTurn, int sleepMs, int crashTurn, List<String> sent, List<String> violations) {
            this.sent = sent;
            this.violations = violations;
            this.seed = seed;
            this.malformedRate = malformedRate;
            this.sleepTurn = sleepTurn;
            this.sleepMs = sleepMs;
            this.crashTurn = crashTurn;
        }

        void bad(String what) {
            if (violations.size() < 20) violations.add("turn " + (sent.size() + 1) + ": " + what);
        }
    }

    /**
     * The SDK loads each agent class in its own class loader, so statics are not shared with the runner side:
     * and System.in/out are rewritten only inside the agent class itself, so P0..P3 pass them in;
     * the plan goes through a system property and the bot writes into JDK lists kept in System.getProperties().
     */
    public static void publish(int seat, Plan p) {
        System.setProperty("gw.fuzz.plan." + seat, p.seed + "," + p.malformedRate + "," + p.sleepTurn + "," + p.sleepMs + "," + p.crashTurn);
        System.getProperties().put("gw.fuzz.sent." + seat, p.sent);
        System.getProperties().put("gw.fuzz.bad." + seat, p.violations);
    }

    @SuppressWarnings("unchecked")
    public static void run(int seat, java.io.InputStream in, java.io.PrintStream out) throws IOException {
        String[] t = System.getProperty("gw.fuzz.plan." + seat).split(",");
        Plan p = new Plan(Long.parseLong(t[0]), Double.parseDouble(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]),
            Integer.parseInt(t[4]), (List<String>) System.getProperties().get("gw.fuzz.sent." + seat),
            (List<String>) System.getProperties().get("gw.fuzz.bad." + seat));
        if (Boolean.getBoolean("gw.fuzz.debug")) System.err.println("fuzz seat " + seat + " plan " + System.getProperty("gw.fuzz.plan." + seat) + " sent@" + System.identityHashCode(p.sent));
        Agents.loop(new FuzzBot(p), in, out);
    }

    public static class P0 { public static void main(String[] a) throws IOException { run(0, System.in, System.out); } }
    public static class P1 { public static void main(String[] a) throws IOException { run(1, System.in, System.out); } }
    public static class P2 { public static void main(String[] a) throws IOException { run(2, System.in, System.out); } }
    public static class P3 { public static void main(String[] a) throws IOException { run(3, System.in, System.out); } }

    public static final Class<?>[] SEATS = {P0.class, P1.class, P2.class, P3.class};

    /** Malformed lines: none of them is "one of the 3 commands with integer arguments" (F6 step 1). */
    static final String[] MALFORMED = {
        "", "wait", "Wait", "PLACE", "PLACE 1", "PLACE 1 2", "TURN", "TURN 3", "PLACE a b c", "PLACE 1.0 2 0",
        "PLACE 1 2 x", "TURN 1 y", "PLACE 99999999999 1 0", "TURN 2147483648 0", "PLACE 0x1 2 0", "MOVE 1 2",
        "PLACE\t1\t2\t0", "WAIT\tmsg", "PLACE １ 2 0", "\u0000", "null", "PLACE,1,2,0", "1 2 0", "PLACE -- 2 0"};

    static final class FuzzBot implements Bot {
        private final Plan p;
        private final Random rnd;

        FuzzBot(Plan p) {
            this.p = p;
            this.rnd = new Random(p.seed);
        }

        /** V5.1c init (DESIGN V5.8): the reef string, then the size; lamp letters are relative ids (A = me). */
        @Override
        public void init(List<String> l) {
            try {
                String reef = l.get(0);
                p.n = Integer.parseInt(l.get(1).trim());
                if (p.n < 13 || p.n > 19 || p.n % 2 == 0) p.bad("size " + p.n);
                if (l.size() != 2) p.bad("init has " + l.size() + " lines, expected 2");
                if (reef.length() != p.n * p.n || !reef.matches("[.#123ABCD]*")) p.bad("reef '" + reef + "'");
                for (int y = 0; y < p.n; y++) p.rows.add(reef.substring(y * p.n, Math.min(reef.length(), (y + 1) * p.n)));
                p.myId = 0;
                int letters = 0;
                while (letters < 4 && reef.indexOf((char) ('A' + letters)) >= 0) letters++;
                p.playerCount = letters;
                if (letters < 2) p.bad("only " + letters + " lamps");
                for (int k = letters; k < 4; k++) if (reef.indexOf((char) ('A' + k)) >= 0) p.bad("lamp letters not consecutive");
                for (int k = 0; k < letters; k++) {
                    int c = reef.indexOf((char) ('A' + k)), x = c % p.n, y = c / p.n;
                    boolean edge = x == 0 || y == 0 || x == p.n - 1 || y == p.n - 1, corner = (x == 0 || x == p.n - 1) && (y == 0 || y == p.n - 1);
                    if (!edge || corner) p.bad("lamp " + (char) ('A' + k) + " not on an edge at " + x + " " + y);
                }
                int digits = 0;
                for (int c = 0; c < reef.length(); c++)
                    if (reef.charAt(c) >= '1' && reef.charAt(c) <= '3') {
                        digits++;
                        p.crystals.add((c % p.n) + " " + (c / p.n));
                    }
                p.crystalCount = digits;
            } catch (RuntimeException e) {
                p.bad("init parse " + e);
            }
        }

        private char cell(int x, int y) {
            return x < 0 || y < 0 || x >= p.n || y >= p.n ? '?' : p.rows.get(y).charAt(x);
        }

        /** V5.1c turn: myScore + 4 hands; mirrors x y type lock life (lock -1 pending, life 0 echo); crystals tide litBy myStreak. */
        private void checkTurn(List<String> l) {
            try {
                int i = 0;
                String[] h = l.get(i++).split(" ");
                if (h.length != 5) p.bad("head line " + l.get(0));
                int myScore = Integer.parseInt(h[0]);
                if (myScore < 0) p.bad("own score " + myScore + " while still fed");
                for (int k = 0; k < 4; k++) {
                    int hand = Integer.parseInt(h[1 + k]);
                    if (hand < -1 || hand > 12) p.bad("hand " + k + " = " + hand);
                    if (k >= p.playerCount && hand != -1) p.bad("missing player " + k + " shows hand " + hand);
                    if (k == 0 && hand == -1) p.bad("own hand -1 while still fed");
                }
                int mc = Integer.parseInt(l.get(i++));
                p.mirrors.clear();
                int prev = -1;
                for (int k = 0; k < mc; k++, i++) {
                    String[] t = l.get(i).split(" ");
                    int x = Integer.parseInt(t[0]), y = Integer.parseInt(t[1]), ty = Integer.parseInt(t[2]);
                    int lock = Integer.parseInt(t[3]), life = Integer.parseInt(t[4]), pend = lock == -1 ? 1 : 0;
                    boolean echo = life == 0;
                    if (!echo) p.mirrors.add(new int[] {x, y, ty, Math.max(0, lock), pend, life});
                    boolean ok = t.length == 5 && ty >= -1 && ty <= 1 && lock >= -1 && lock <= 5 && (ty >= 0 || pend == 1)
                        && (life == -1 || (life >= 0 && life <= 20)) && (pend == 0 || life == 20 || life == -1) && (!echo || (lock == 0 && ty >= 0));
                    if (!ok) p.bad("mirror line " + l.get(i));
                    char c = cell(x, y);
                    if (c != '.' && !(c >= 'A' && c <= 'D')) p.bad("mirror on '" + c + "' at " + x + " " + y);
                    if (c >= 'A' && c <= 'D') p.bad("mirror on a lamp cell at " + x + " " + y);
                    if (y * p.n + x <= prev) p.bad("mirrors not sorted by (y,x) at " + l.get(i));
                    prev = y * p.n + x;
                }
                int cc = Integer.parseInt(l.get(i++));
                if (cc != p.crystalCount) p.bad("turn crystalCount " + cc + " vs reef " + p.crystalCount);
                for (int k = 0; k < cc; k++, i++) {
                    String[] t = l.get(i).split(" ");
                    boolean ok = t.length == 3;
                    int tide = Integer.parseInt(t[0]), by = Integer.parseInt(t[1]), st = Integer.parseInt(t[2]);
                    ok &= tide == 0 && by >= -2 && by < p.playerCount && st >= 0 && st <= 4;
                    if (!ok) p.bad("crystal turn line " + l.get(i));
                }
                if (i != l.size()) p.bad("turn has " + l.size() + " lines, expected " + i);
            } catch (RuntimeException e) {
                p.bad("turn parse " + e);
            }
        }

        @Override
        public String act(List<String> l) {
            if (Boolean.getBoolean("gw.fuzz.debug")) System.err.println("fuzz act " + l.get(0) + " n " + p.n);
            checkTurn(l);
            int turn = p.sent.size() + 1;
            String line = next();
            p.sent.add(line);
            if (turn == p.crashTurn) throw new IllegalStateException("fuzz crash at turn " + turn);
            if (turn == p.sleepTurn) {
                try {
                    Thread.sleep(p.sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return line;
        }

        private int coord() {
            int r = rnd.nextInt(20);
            if (r == 0) return -1;
            if (r == 1) return p.n;
            if (r == 2) return Integer.MAX_VALUE;
            if (r == 3) return Integer.MIN_VALUE;
            return rnd.nextInt(p.n);
        }

        private String msg() {
            int r = rnd.nextInt(6);
            if (r == 0) return " hi";
            if (r == 1) return " a message well over thirty characters long <b>&amp;</b> \\ $0 %s {}";
            if (r == 2) return " " + "x".repeat(5000);
            return "";
        }

        private String pad(String s) {
            int r = rnd.nextInt(8);
            if (r == 0) return "  " + s;
            if (r == 1) return s + "   ";
            if (r == 2) return s.replace(" ", "   ");
            return s;
        }

        private String next() {
            if (rnd.nextDouble() < p.malformedRate) {
                if (rnd.nextInt(10) == 0) return "PLACE 1 2 0" + "9".repeat(20000);
                return MALFORMED[rnd.nextInt(MALFORMED.length)];
            }
            int r = rnd.nextInt(100);
            if (r < 10) return pad("WAIT" + msg());
            if (r < 45) return pad("PLACE " + rnd.nextInt(p.n) + " " + rnd.nextInt(p.n) + " " + rnd.nextInt(2) + msg());
            if (r < 55) { // a special cell: lamp, crystal, wall, or a mirror cell
                for (int k = 0; k < 50; k++) {
                    int x = rnd.nextInt(p.n), y = rnd.nextInt(p.n);
                    if (cell(x, y) != '.') return "PLACE " + x + " " + y + " " + rnd.nextInt(2);
                }
            }
            if (r < 62 && !p.mirrors.isEmpty()) {
                int[] m = p.mirrors.get(rnd.nextInt(p.mirrors.size()));
                return "PLACE " + m[0] + " " + m[1] + " " + rnd.nextInt(2);
            }
            if (r < 85 && !p.mirrors.isEmpty()) {
                int[] m = p.mirrors.get(rnd.nextInt(p.mirrors.size()));
                return pad("TURN " + m[0] + " " + m[1] + msg());
            }
            if (r < 92) return "TURN " + coord() + " " + coord();
            return "PLACE " + coord() + " " + coord() + " " + (rnd.nextInt(4) - 1);
        }
    }

    /**
     * Independent reading of F5/F6 step 1: after trimming the line ends, tokens split on runs of spaces, an
     * upper-case keyword, then the integer arguments (Java int range), then any message.
     * Tabs inside a line are not separators (Q1); non-ASCII digits count as digits like parseInt (Q2).
     */
    public static boolean wellFormed(String line) {
        String[] t = line.trim().split(" +");
        int need = t[0].equals("PLACE") ? 4 : t[0].equals("TURN") ? 3 : t[0].equals("WAIT") ? 1 : -1;
        if (need < 0 || t.length < need) return false;
        for (int i = 1; i < need; i++) {
            if (!t[i].matches("[+-]?\\p{Nd}+")) return false; // Unicode digits: Integer.parseInt takes them (Q2)
            try {
                Integer.parseInt(t[i]);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
