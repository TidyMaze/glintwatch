package com.codingame.game;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Internal bot names stay private (owner): none may appear in what players see (view resources, config texts). */
class ShippedTextTest {
    private static final Pattern INTERNAL = Pattern.compile(
        "\\b(planner|greedy2|greedy|self|random|cutter|renewer|spoiler|freerider|plusw|boss2|plus2)\\b", Pattern.CASE_INSENSITIVE);

    @Test
    void noInternalBotNameInShippedViewOrConfigFiles() throws Exception {
        List<String> hits = new ArrayList<>();
        for (String root : new String[] {"src/main/resources/view", "config"})
            try (Stream<Path> files = Files.walk(Path.of(root))) {
                for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                    String name = f.getFileName().toString();
                    if (name.startsWith("Boss.") || name.endsWith(".png")) continue; // boss code is not shown to players
                    Matcher m = INTERNAL.matcher(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
                    while (m.find()) hits.add(f + ": " + m.group());
                }
            }
        assertTrue(hits.isEmpty(), String.join("\n", hits));
    }

    @Test
    void introReplayUsesTheFourHouses() throws Exception {
        String demo = new String(Files.readAllBytes(Path.of("src/main/resources/view/demo.js")), StandardCharsets.UTF_8);
        for (String house : new String[] {"Sky Ring", "Amber Spire", "Jade Keep", "Orchid Star"})
            assertTrue(demo.contains("\"name\":\"" + house + "\""), house);
    }
}
