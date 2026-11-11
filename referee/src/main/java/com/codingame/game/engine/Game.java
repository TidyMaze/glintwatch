package com.codingame.game.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** One game, DESIGN FINAL RULES F6; switches and constants in Config. */
public final class Game {
    public final Config cfg;
    public final Board board;
    public final int playerCount;
    public final int[] seatOf, score, mirrorsLeft;
    public final boolean[] active;
    /** streak[player][cell], 0..streakCap. */
    public final int[][] streak;
    /** Per cell: player who lit the crystal alone last turn, -1 nobody, -2 contested. */
    public final int[] litBy;
    /** Mirrors that shattered at the end of the last turn: {cell, final type} (V5.8 echo, information only). */
    public final java.util.List<int[]> lastShattered = new java.util.ArrayList<>();
    /** Consecutive turns each player did nothing (WAIT or an impossible action). */
    public final int[] idleTurns;
    private String earlyReason;
    private boolean fastForwarding;
    /** Turns played; the current turn number is turn + 1. */
    public int turn;
    private int suppliedTurn;

    public Game(Board board, int playerCount, Config cfg) {
        this(board, Seats.seatsFor(playerCount), cfg);
    }

    /** @param seatOf seat of each player id (bots use relative ids: themselves first, then clockwise). */
    public Game(Board board, int[] seatOf, Config cfg) {
        this.cfg = cfg;
        this.board = board;
        this.playerCount = seatOf.length;
        this.seatOf = seatOf.clone();
        score = new int[playerCount];
        mirrorsLeft = new int[playerCount];
        active = new boolean[playerCount];
        streak = new int[playerCount][board.n * board.n];
        idleTurns = new int[playerCount];
        litBy = new int[board.n * board.n];
        Arrays.fill(litBy, -1);
        for (int id = 0; id < playerCount; id++) {
            mirrorsLeft[id] = cfg.mirrorStock;
            active[id] = true;
        }
        for (int p = 0; p < board.n * board.n; p++)
            if (board.mirrorType[p] >= 0 && board.pending[p] == 0) board.mirrorLife[p] = cfg.mirrorLife;
        // F6.2 / QA Q5: the lamp cell of a seat without a player is an ordinary Empty cell
        boolean[] held = new boolean[4];
        for (int s : seatOf) held[s] = true;
        for (int s = 0; s < 4; s++)
            if (!held[s]) {
                int p = Seats.lampCell(s, board.n, board.e);
                board.kind[p] = Board.EMPTY;
                board.lampSeat[p] = -1;
            }
    }

    public static Game fromSeed(long seed, int playerCount, Config cfg) {
        return new Game(MapGenerator.generate(seed, cfg.walls, cfg.multiValues), playerCount, cfg);
    }

    /** Deep copy, optionally under other rules (bots simulate with buildDelay off). */
    public Game copyWith(Config rules) {
        Board b = board.copy();
        int[] life = b.mirrorLife.clone();
        Game g = new Game(b, seatOf, rules);
        System.arraycopy(life, 0, b.mirrorLife, 0, life.length);
        System.arraycopy(score, 0, g.score, 0, playerCount);
        System.arraycopy(mirrorsLeft, 0, g.mirrorsLeft, 0, playerCount);
        System.arraycopy(active, 0, g.active, 0, playerCount);
        for (int id = 0; id < playerCount; id++) System.arraycopy(streak[id], 0, g.streak[id], 0, streak[id].length);
        System.arraycopy(litBy, 0, g.litBy, 0, litBy.length);
        g.turn = turn;
        g.suppliedTurn = suppliedTurn;
        return g;
    }

    public Game copy() {
        return copyWith(cfg);
    }

    public int currentTurn() {
        return turn + 1;
    }

    public boolean awake(int p, int turnNumber) {
        return !cfg.tides || (turnNumber + board.phase[p]) % cfg.tideCycle < cfg.tideAwake;
    }

    /** Per seat: Board.SEAT_ACTIVE, SEAT_INACTIVE or SEAT_EMPTY. */
    public int[] seatStates() {
        int[] s = {Board.SEAT_EMPTY, Board.SEAT_EMPTY, Board.SEAT_EMPTY, Board.SEAT_EMPTY};
        for (int id = 0; id < playerCount; id++) s[seatOf[id]] = active[id] ? Board.SEAT_ACTIVE : Board.SEAT_INACTIVE;
        return s;
    }

    /** F8: lamp becomes a wall, hand discarded, its mirrors stay, zone stays protected, score -1. */
    public void deactivate(int id) {
        active[id] = false;
        mirrorsLeft[id] = 0;
        score[id] = -1;
    }

    public int activeCount() {
        int c = 0;
        for (boolean a : active) if (a) c++;
        return c;
    }

    public boolean isOver() {
        return earlyReason != null || turn >= cfg.maxTurns || activeCount() < 2;
    }

    /** Why the game ended: "decided", "stale", "fewer than two players", "max turns"; null while it runs. */
    public String endReason() {
        if (earlyReason != null) return earlyReason;
        if (activeCount() < 2) return "fewer than two players";
        return turn >= cfg.maxTurns ? "max turns" : null;
    }

    /** B of V5.5: most points one player can score in a turn (streak cap x sum of values, + strike). */
    public int maxGainPerTurn() {
        int sum = 0;
        for (int p = 0; p < board.n * board.n; p++) if (board.kind[p] == Board.CRYSTAL) sum += board.value[p];
        return (cfg.contested ? cfg.streakCap : 1) * sum + cfg.strikeBonus;
    }

    /** V5.5 decided: every pair of active players is further apart than turns left x B. */
    boolean decided() {
        long bound = (long) (cfg.maxTurns - turn) * maxGainPerTurn();
        for (int i = 0; i < playerCount; i++)
            for (int j = i + 1; j < playerCount; j++)
                if (active[i] && active[j] && Math.abs(score[i] - score[j]) <= bound) return false;
        return true;
    }

    boolean stale() {
        for (int id = 0; id < playerCount; id++) if (active[id] && idleTurns[id] < cfg.staleTurns) return false;
        return true;
    }

    /** End of turn (F6 step 8 + V5.5): decided, or stale = the referee plays the remaining turns with everyone waiting. */
    private void earlyEndCheck() {
        if (!cfg.earlyEnd || fastForwarding || turn >= cfg.maxTurns || activeCount() < 2) return;
        if (decided()) earlyReason = "decided";
        else if (stale()) {
            fastForwarding = true;
            while (turn < cfg.maxTurns && activeCount() >= 2) playTurn(new Command[playerCount]);
            fastForwarding = false;
            earlyReason = "stale";
        }
    }

    /** F6 step 0, before the input of the current turn is sent; idempotent within a turn. */
    public void beginTurn() {
        int t = currentTurn();
        if (suppliedTurn == t) return;
        suppliedTurn = t;
        if (cfg.supplyEvery > 0 && t % cfg.supplyEvery == 0)
            for (int id = 0; id < playerCount; id++)
                if (active[id]) mirrorsLeft[id] = Math.min(cfg.handMax, mirrorsLeft[id] + 1);
    }

    /** @param cmds by player id; null entries (or inactive players) count as WAIT. */
    public TurnReport playTurn(Command[] cmds) {
        beginTurn();
        TurnReport r = new TurnReport(playerCount, cmds);
        lastShattered.clear();
        int[] placeAt = new int[playerCount], turnAt = new int[playerCount];
        for (int id = 0; id < playerCount; id++) {
            placeAt[id] = -1;
            turnAt[id] = -1;
            if (!active[id] || cmds[id] == null) continue;
            String why = validate(id, cmds[id]);
            if (why != null) r.invalid[id] = why;
            else if (cmds[id].kind == Command.Kind.PLACE) placeAt[id] = Geometry.idx(cmds[id].x, cmds[id].y, board.n);
            else if (cmds[id].kind == Command.Kind.TURN) turnAt[id] = Geometry.idx(cmds[id].x, cmds[id].y, board.n);
        }
        boolean[] placed = new boolean[board.n * board.n], turned = new boolean[board.n * board.n];
        applyPlaces(cmds, placeAt, r, placed);
        applyTurns(turnAt, r, placed, turned);
        int[] states = seatStates();
        for (int id = 0; id < playerCount; id++) if (active[id]) r.beams[id] = BeamTracer.trace(board, seatOf[id], states);
        scoreCrystals(r);
        for (int id = 0; id < playerCount; id++)
            if (r.beams[id] != null && r.beams[id].stop == Beam.Stop.STRIKE) r.gain[id] += cfg.strikeBonus;
        for (int id = 0; id < playerCount; id++) if (active[id]) score[id] += r.gain[id];
        advanceMirrors(r, placed, turned);
        for (int id = 0; id < playerCount; id++) {
            boolean acted = placeAt[id] >= 0 && r.invalid[id] == null || turnAt[id] >= 0;
            idleTurns[id] = acted ? 0 : idleTurns[id] + 1;
        }
        turn++;
        earlyEndCheck();
        return r;
    }

    /** Null when valid, else the reason (the command becomes WAIT). */
    public String validate(int id, Command c) {
        if (c.kind == Command.Kind.WAIT) return null;
        int n = board.n;
        if (!Geometry.inside(c.x, c.y, n)) return "cell out of the board";
        int p = Geometry.idx(c.x, c.y, n);
        int zone = Seats.zoneSeat(c.x, c.y, n, board.e);
        int zoneOwner = zone >= 0 && zone != seatOf[id] ? playerAtSeat(zone) : -1;
        if (c.kind == Command.Kind.PLACE) {
            if (c.type != 0 && c.type != 1) return "type must be 0 or 1";
            if (board.kind[p] != Board.EMPTY || board.mirrorType[p] >= 0) return "cell not empty";
            if (zoneOwner >= 0) return "cell in zone of player " + zoneOwner;
            if (mirrorsLeft[id] <= 0) return "no mirror in hand";
            return null;
        }
        if (!cfg.turnAllowed) return "TURN not available in this league";
        if (board.mirrorType[p] < 0) return "no mirror there";
        if (board.pending[p] > 0) return "mirror still pending";
        if (board.mirrorLock[p] > 0) return "mirror locked " + board.mirrorLock[p] + " more turns";
        if (zoneOwner >= 0) return "cell in zone of player " + zoneOwner;
        return null;
    }

    private int playerAtSeat(int seat) {
        for (int id = 0; id < playerCount; id++) if (seatOf[id] == seat) return id;
        return -1;
    }

    private void applyPlaces(Command[] cmds, int[] placeAt, TurnReport r, boolean[] placed) {
        Map<Integer, Integer> count = new HashMap<>();
        for (int p : placeAt) if (p >= 0) count.merge(p, 1, Integer::sum);
        for (int id = 0; id < playerCount; id++) {
            int p = placeAt[id];
            if (p < 0) continue;
            if (count.get(p) > 1) {
                r.invalid[id] = "collision";
                continue;
            }
            board.mirrorType[p] = cmds[id].type;
            board.lastToucher[p] = id;
            board.mirrorLife[p] = cfg.mirrorLife;
            mirrorsLeft[id]--;
            if (cfg.buildDelay) {
                board.pending[p] = 1;
                board.mirrorLock[p] = 0;
            } else board.mirrorLock[p] = cfg.lock;
            placed[p] = true;
            r.placed.add(p);
        }
    }

    private void applyTurns(int[] turnAt, TurnReport r, boolean[] placed, boolean[] turned) {
        for (int id = 0; id < playerCount; id++) {
            int p = turnAt[id];
            // a valid TURN targets a mirror present at the start of the turn, so never one placed this turn
            if (p < 0 || turned[p]) continue;
            board.mirrorType[p] = 1 - board.mirrorType[p];
            board.mirrorLock[p] = cfg.lock;
            board.lastToucher[p] = id;
            turned[p] = true;
            r.turned.add(p);
        }
    }

    /** F6 step 6. */
    private void scoreCrystals(TurnReport r) {
        int cells = board.n * board.n, t = currentTurn();
        int[] lighters = new int[cells], lastLighter = new int[cells];
        boolean[][] lit = new boolean[playerCount][];
        for (int id = 0; id < playerCount; id++) {
            if (r.beams[id] == null) continue;
            lit[id] = new boolean[cells];
            for (int p : r.beams[id].lit) {
                lit[id][p] = true;
                lighters[p]++;
                lastLighter[p] = id;
            }
        }
        for (int p = 0; p < cells; p++) {
            if (board.kind[p] != Board.CRYSTAL) continue;
            litBy[p] = lighters[p] == 0 ? -1 : lighters[p] == 1 ? lastLighter[p] : -2;
            if (!cfg.contested) {
                for (int id = 0; id < playerCount; id++) if (lit[id] != null && lit[id][p]) r.gain[id] += board.value[p];
                continue;
            }
            if (!awake(p, t)) {
                for (int id = 0; id < playerCount; id++) streak[id][p] = 0;
                continue;
            }
            for (int id = 0; id < playerCount; id++) {
                boolean inL = lit[id] != null && lit[id][p];
                if (lighters[p] == 1 && inL) {
                    streak[id][p] = Math.min(streak[id][p] + 1, cfg.streakCap);
                    r.gain[id] += board.value[p] * streak[id][p];
                } else if (!inL) streak[id][p] = Math.max(streak[id][p] - 1, 0);
            }
        }
    }

    /** F6 step 7: activation, lock countdown, wear. */
    private void advanceMirrors(TurnReport r, boolean[] placed, boolean[] turned) {
        for (int p = 0; p < board.n * board.n; p++) {
            if (board.mirrorType[p] < 0 || placed[p]) continue;
            if (board.pending[p] > 0) {
                board.pending[p] = 0;
                board.mirrorLock[p] = cfg.lock;
                r.activated.add(p);
                continue;
            }
            if (turned[p]) continue;
            board.mirrorLock[p] = Math.max(board.mirrorLock[p] - 1, 0);
            if (board.mirrorLife[p] > 0 && --board.mirrorLife[p] == 0) {
                lastShattered.add(new int[] {p, board.mirrorType[p]});
                board.removeMirror(p);
                r.broken.add(p);
            }
        }
    }
}
