package com.codingame.game.qa;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codingame.game.bots.Agents;
import com.codingame.game.engine.*;
import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/** qa TEST-PLAN 2.3, 2.5, 3.9: a deactivated player's lamp and yard stay closed (F8); ties share the score. */
class QaRulesTest {
    private static Command c(String s) throws Exception {
        return CommandParser.parse(s);
    }

    @Test
    void deactivatedLampCellAndYardStayClosedToOthers() throws Exception {
        Game g = new Game(Board.empty(13, 3), 4, Config.forLeague(2)); // seat 1 lamp (9,0)
        g.deactivate(1);
        TurnReport r = g.playTurn(new Command[] {c("PLACE 9 0 0"), null, c("PLACE 9 1 1"), c("WAIT")});
        assertNotNull(r.invalid[0], "lamp cell of a deactivated player");
        assertNotNull(r.invalid[2], "yard of a deactivated player");
        assertEquals(12, g.mirrorsLeft[0]);
        assertEquals(12, g.mirrorsLeft[2]);
    }

    @Test
    void twoWaitingBotsTieAtZeroWithoutRefereeError() {
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.setSeed(3L);
        runner.setLeagueLevel(4);
        runner.addAgent(Agents.Wait.class, "A");
        runner.addAgent(Agents.Wait.class, "B");
        GameResult r = runner.simulate();
        for (String e : r.errors.getOrDefault("referee", java.util.Collections.emptyList()))
            assertTrue(com.codingame.game.RefereeTest.isDiagnostic(e), e);
        assertEquals(0, r.scores.get(0).intValue());
        assertEquals(0, r.scores.get(1).intValue());
    }
}
