package com.codingame.game.bots;

import java.util.List;

/** A player bot fed with the exact protocol lines (DESIGN 5.1 and V2.3). */
public interface Bot {
    void init(List<String> initLines);

    /** @return one output line. */
    String act(List<String> turnLines);
}
