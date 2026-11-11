# Contribution page texts

## Pitch (<= 120 chars; this one is 93)
Bend your lamp's beam with shared mirrors and hold the reef's crystals alone. 2 to 4 players.

## Short description
Night on the Tidereef: up to four keepers fire a lamp beam from the edge of a square reef.
Place and turn mirrors to bend your light onto crystals. A crystal pays only the player who lights it alone,
and pays more each turn you hold it. Every mirror bends every beam, so each one you place can help a rival.
Climb the leagues to unlock mirror supply, build time, wear, crystal tides and strikes.

## Rules summary (all numbers checked against DESIGN.md F1-F9 on 2026-11-11)
- 2-4 players, square grid 13x13 to 19x19 (odd), 90-degree symmetric; 100 turns in the first league, 150 after.
- Each turn every player prints one action: `PLACE x y type` (mirror `/` or `\`), `TURN x y` (from the second league), or `WAIT`.
- After all actions every lamp fires a beam; it passes crystals, turns 90 degrees on mirrors and stops on rocks, lamps or the edge. Mirrors belong to nobody.
- The 15 cells within 2 of your lamp are your yard: only you build or turn there.
- First league: every crystal you light pays 1. From the second league: a crystal pays only if you light it alone, value x streak (1 to 4); a shared crystal pays nobody.
- From the second league: a mirror placed or turned is locked 5 turns.
- From the third league: 4 mirrors at start, +1 every 4 turns, max 6; a mirror bends beams 2 turns after placing, its type hidden from rivals until then, and shatters after 20 turns.
- From the fourth league: crystals worth 1-3 sleep 15 turns in every 45 (streaks reset); a beam ending on a rival's lamp pays 2 each turn. Highest score wins; timeout (50 ms) or a bad command eliminates (score -1).

League split (internal: 1 Wood 2, 2 Wood 1, 3 Bronze, 4 Silver) confirmed final by cg-contrib-designer (Statement v4, 2026-11-11).

## Viewer flavour lines (max 30 chars each, for an end screen or a tooltip)
1. `Dawn. The light is counted.` (end, any result)
2. `<house> holds the reef.` (single winner)
3. `The reef is shared at dawn.` (tie for first)
4. `<name>'s lamp went dark.` (timeout or bad output)
5. `Clouded: two lights, no glint.` (contested crystal tooltip)
6. `Full warmth: x4.` (streak reaches 4)
7. `The tide covers it.` (crystal falls asleep, Silver+)
8. `Dazzled! +2` (strike, Silver+)
9. `Salt took the mirror.` (mirror shatters, Bronze+)
10. `Setting under cloth...` (rival pending mirror tooltip, Bronze+)
Lines 2 and 4 use the placeholder `<house>` (Sky, Amber, Jade, Orchid) or `<name>` (nickname); keep the placeholder whole for translators.
