package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.List;

/** Result of one lamp's trace. */
public final class Beam {
    public enum Stop { EXIT, ABSORBED, STRIKE }

    public final int seat;
    /** Crystal cells lit, in first-hit order, each once. */
    public final List<Integer> lit = new ArrayList<>();
    /** Lamp cell, then every mirror cell that turned the beam (for the viewer segments). */
    public final List<Integer> corners = new ArrayList<>();
    /** Every board cell the beam crossed, in order (lamp and stop cell excluded). */
    public final List<Integer> cells = new ArrayList<>();
    public Stop stop;
    public int struckSeat = -1;
    /** Last position: the absorbing or struck cell, or the first cell outside the board. */
    public int endX, endY;
    /** Direction of travel when the beam stopped. */
    public Dir endDir;
    public int steps;

    Beam(int seat) {
        this.seat = seat;
    }
}
