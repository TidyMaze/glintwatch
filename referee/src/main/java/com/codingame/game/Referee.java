package com.codingame.game;

import java.util.List;

import com.codingame.game.engine.*;
import com.codingame.game.view.BoardModule;
import com.codingame.game.view.Viewer;
import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.endscreen.EndScreenModule;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

/** Glintwatch referee: the rules live in engine.Game; this class only does I/O, summaries and the end screen. */
public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject private TooltipModule tooltips;
    @Inject private EndScreenModule endScreenModule;
    @Inject private ToggleModule toggleModule;
    @Inject private BoardModule boardModule;

    private Game game;
    private Viewer viewer;

    /** Local diagnostics on stderr, only with -Dglintwatch.diag=true (CG never shows referee stderr). */
    private static void log(String message) {
        if (Boolean.getBoolean("glintwatch.diag")) System.err.println("[glintwatch] " + message);
    }

    /** Test hook: "init" or "turn N" makes that step throw, to prove the referee never crashes (Help Center rule). */
    static volatile String failAt;

    private static void injectedFault(String where) {
        if (where.equals(failAt)) throw new IllegalStateException("injected fault at " + where);
    }

    /** Runs an action; on failure logs the stack ("<where> failed: ...") on stderr and returns it (never rethrows). */
    static Throwable guarded(String where, Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException | Error e) {
            java.io.StringWriter w = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(w));
            for (String line : w.toString().split("\n")) log(where + " failed: " + line);
            return e;
        }
    }

    /** An unexpected referee failure ends the game cleanly: scores of the last complete turn, one summary line. */
    private void endOnFailure(String where, Throwable failure) {
        if (failure == null) return;
        gameManager.addToGameSummary("Game over: referee error at " + where + "; the scores are those of the last complete turn.");
        gameManager.endGame();
    }

    @Override
    public void init() {
        endOnFailure("init", guarded("init", this::initGame));
    }

    private void initGame() {
        injectedFault("init");
        long t0 = System.nanoTime();
        log("init start league " + gameManager.getLeagueLevel() + " seed " + gameManager.getSeed() + " players "
            + gameManager.getPlayerCount() + " java " + System.getProperty("java.version"));
        Config cfg = Config.forLeague(gameManager.getLeagueLevel());
        game = Game.fromSeed(gameManager.getSeed(), gameManager.getPlayerCount(), cfg);
        log("map n " + game.board.n + " in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
        gameManager.setMaxTurns(cfg.maxTurns);
        gameManager.setFirstTurnMaxTime(1000);
        gameManager.setTurnMaxTime(50);
        gameManager.setFrameDuration(600);
        viewer = new Viewer(graphicEntityModule, tooltips, toggleModule, boardModule, gameManager, game);
        viewer.init();
        log("init done in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
    }

    @Override
    public void gameTurn(int turn) {
        long t0 = System.nanoTime();
        endOnFailure("turn " + turn, guarded("turn " + turn, () -> playTurn(turn)));
        if (turn <= 2 || turn % 50 == 0) log("turn " + turn + " done in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
    }

    private void playTurn(int turn) {
        injectedFault("turn " + turn);
        game.beginTurn();
        for (Player p : gameManager.getActivePlayers()) {
            if (turn == 1)
                for (String l : Protocol.initLinesV5(game, p.getIndex())) p.sendInputLine(l);
            for (String l : Protocol.turnLinesV5(game, p.getIndex())) p.sendInputLine(l);
            p.execute();
        }
        Command[] cmds = new Command[game.playerCount];
        for (Player p : gameManager.getActivePlayers()) {
            try {
                List<String> out = p.getOutputs();
                cmds[p.getIndex()] = CommandParser.parse(out.get(0));
            } catch (TimeoutException e) {
                eliminate(p, "timeout");
            } catch (CommandParser.InvalidCommand e) {
                eliminate(p, e.getMessage());
            }
        }
        TurnReport r = game.playTurn(cmds);
        viewer.update(r);
        for (int id = 0; id < game.playerCount; id++)
            if (r.invalid[id] != null)
                gameManager.addToGameSummary(String.format("$%d: %s refused: %s", id, cmds[id].text(), r.invalid[id]));
        for (Player p : gameManager.getPlayers()) p.setScore(game.score[p.getIndex()]);
        if (game.isOver()) {
            String reason = game.endReason();
            if ("stale".equals(reason))
                gameManager.addToGameSummary("Game over: no player acted for " + game.cfg.staleTurns + " turns; the remaining turns were played with every player waiting.");
            else if ("decided".equals(reason))
                gameManager.addToGameSummary("Game over: the ranking can no longer change.");
            gameManager.endGame();
        }
    }

    private void eliminate(Player p, String reason) {
        p.deactivate(String.format("$%d eliminated: %s", p.getIndex(), reason));
        gameManager.addToGameSummary(String.format("$%d eliminated: %s", p.getIndex(), reason));
        game.deactivate(p.getIndex());
    }

    @Override
    public void onEnd() {
        if (game == null) return; // init failed: nothing to score
        int[] scores = new int[game.playerCount];
        String[] texts = new String[game.playerCount];
        for (Player p : gameManager.getPlayers()) {
            scores[p.getIndex()] = game.score[p.getIndex()];
            p.setScore(scores[p.getIndex()]);
            texts[p.getIndex()] = game.active[p.getIndex()] ? scores[p.getIndex()] + " points" : "eliminated";
        }
        endScreenModule.setScores(scores, texts);
        endScreenModule.setTitleRankingsSprite("logo.png");
    }
}
