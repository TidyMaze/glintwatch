package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.List;

/** What happened in one turn, by player id (for the referee summary and the viewer). */
public final class TurnReport {
    public final Command[] commands;
    /** Reason the command became WAIT, or null. */
    public final String[] invalid;
    public final int[] gain;
    /** Null for inactive players. */
    public final Beam[] beams;
    public final List<Integer> placed = new ArrayList<>(), turned = new ArrayList<>(), activated = new ArrayList<>(), broken = new ArrayList<>();

    TurnReport(int players, Command[] commands) {
        this.commands = commands;
        invalid = new String[players];
        gain = new int[players];
        beams = new Beam[players];
    }
}
