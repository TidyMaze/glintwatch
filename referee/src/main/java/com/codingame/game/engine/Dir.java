package com.codingame.game.engine;

/** Beam directions; screen coordinates (y grows downwards). */
public enum Dir {
    E(1, 0), S(0, 1), W(-1, 0), N(0, -1);

    public final int dx, dy;

    Dir(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** 90 degrees clockwise on screen: (dx, dy) -> (-dy, dx). */
    public Dir rotate() {
        return values()[(ordinal() + 1) % 4];
    }

    public Dir opposite() {
        return values()[(ordinal() + 2) % 4];
    }
}
