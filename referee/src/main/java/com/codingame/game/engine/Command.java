package com.codingame.game.engine;

/** One parsed player line. Range checks belong to validation, not parsing. */
public final class Command {
    public enum Kind { PLACE, TURN, WAIT }

    public final Kind kind;
    public final int x, y, type;
    public final String message;

    public Command(Kind kind, int x, int y, int type, String message) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.type = type;
        this.message = message;
    }

    public static Command place(int x, int y, int type) {
        return new Command(Kind.PLACE, x, y, type, "");
    }

    public static Command turn(int x, int y) {
        return new Command(Kind.TURN, x, y, -1, "");
    }

    public static Command waitCommand() {
        return new Command(Kind.WAIT, -1, -1, -1, "");
    }

    /** Protocol text, without the message. */
    public String text() {
        return kind == Kind.PLACE ? "PLACE " + x + " " + y + " " + type : kind == Kind.TURN ? "TURN " + x + " " + y : "WAIT";
    }
}
