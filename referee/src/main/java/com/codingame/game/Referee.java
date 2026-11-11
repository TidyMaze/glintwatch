package com.codingame.game;

import java.util.List;

import com.codingame.game.engine.*;
import com.codingame.game.view.Viewer;
import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.endscreen.EndScreenModule;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

/** Glintwatch referee: the rules live in engine.Game; this class only does I/O, summaries and the end screen. */
public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject private TooltipModule tooltips;
    @Inject private EndScreenModule endScreenModule;

    private Game game;
    private Viewer viewer;

    @Override
    public void init() {
        Config cfg = Config.forLeague(gameManager.getLeagueLevel());
        game = Game.fromSeed(gameManager.getSeed(), gameManager.getPlayerCount(), cfg);
        gameManager.setMaxTurns(cfg.maxTurns);
        gameManager.setFirstTurnMaxTime(1000);
        gameManager.setTurnMaxTime(50);
        gameManager.setFrameDuration(600);
        viewer = new Viewer(graphicEntityModule, tooltips, gameManager, game);
        viewer.init();
    }

    @Override
    public void gameTurn(int turn) {
        game.beginTurn();
        for (Player p : gameManager.getActivePlayers()) {
            if (turn == 1)
                for (String l : Protocol.initLines(game.board, game.playerCount, p.getIndex(), game.cfg)) p.sendInputLine(l);
            for (String l : Protocol.turnLines(game, p.getIndex())) p.sendInputLine(l);
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
        if (game.isOver()) gameManager.endGame();
    }

    private void eliminate(Player p, String reason) {
        p.deactivate(String.format("$%d eliminated: %s", p.getIndex(), reason));
        gameManager.addToGameSummary(String.format("$%d eliminated: %s", p.getIndex(), reason));
        game.deactivate(p.getIndex());
    }

    @Override
    public void onEnd() {
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
