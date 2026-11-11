package com.codingame.game.view;

import java.util.LinkedHashMap;
import java.util.Map;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * UX 15.2: mirrors, beams and streak chips are drawn by the JS view module "board" (view/board-module), not as GEM
 * entities. The referee sends one compact string per key, only in the frames where it changed.
 */
@Singleton
public final class BoardModule implements Module {
    public static final String NAME = "board";
    private final GameManager<AbstractPlayer> gm;
    private final Map<String, String> last = new LinkedHashMap<>();
    private final Map<String, String> pending = new LinkedHashMap<>();

    @Inject
    BoardModule(GameManager<AbstractPlayer> gm) {
        this.gm = gm;
        gm.registerModule(this);
    }

    /** Layout numbers, the GEM group ids the module draws into and the starting mirrors; sent once as global data. */
    public void setup(int[] layout, int[] layers, String mirrors) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("l", layout);
        g.put("z", layers);
        g.put("m0", mirrors);
        last.put("m", mirrors);
        gm.setViewGlobalData(NAME, g);
    }

    /** Queues a value for the next frame when it differs from the last value sent under that key. */
    public void set(String key, String value) {
        if (value.equals(last.get(key))) return;
        last.put(key, value);
        pending.put(key, value);
    }

    @Override
    public void onGameInit() {
    }

    @Override
    public void onAfterGameTurn() {
        if (pending.isEmpty()) return;
        gm.setViewData(NAME, new LinkedHashMap<>(pending));
        pending.clear();
    }

    @Override
    public void onAfterOnEnd() {
    }
}
