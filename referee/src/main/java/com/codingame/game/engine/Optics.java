package com.codingame.game.engine;

/** Mirror reflections: type 0 = '/', type 1 = '\'. */
public final class Optics {
    private Optics() {}

    public static Dir reflect(int type, Dir d) {
        if (type == 0) return new Dir[] {Dir.N, Dir.W, Dir.S, Dir.E}[d.ordinal()];
        if (type == 1) return new Dir[] {Dir.S, Dir.E, Dir.N, Dir.W}[d.ordinal()];
        throw new IllegalArgumentException("mirror type " + type);
    }
}
