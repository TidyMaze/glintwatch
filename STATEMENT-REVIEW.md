# Statement v3 review (community manager, M6) - 2026-11-11
Source: DESIGN.md "Statement v3" vs F1-F12. Ranked by return. Statement is about 120 lines; Wood 2 view is short, Bronze view is long but in line with CG contest statements.

## Must fix (contradiction or bot-breaking gap)
1. Contradiction: "Mirrors cannot be removed" (Mirrors section) vs Bronze "Wear" (mirror shatters after 20 turns, F1 MIRROR_LIFE). Say "Mirrors cannot be removed by players" or "[Wood 2] [Wood 1] Mirrors stay forever".
2. League tags are ambiguous: "[Wood 2] [Wood 1]" (stock of 12, example) reads as two separate blocks, while "[Wood 1+]" means "from". Define one scheme in the intro, for example [Wood only] / [Wood 1+] / [Bronze+], and use [Wood 2 only] for the shared-scoring paragraph. Same for the example block.
3. Strike wording: "A beam that ends on another player's lamp gives 2 points". F6 pays it every turn the beam ends there. Say "each turn" (a parked beam on a lamp is a +2 per turn engine; a bot must know). Also state that hitting your own lamp stops the beam with no points (F4), and that an eliminated lamp is a rock (only in Expert notes; move it up).
4. TURN on a pending mirror: F6 step 2 requires an active mirror, the statement never says a pending mirror cannot be turned. Add to Build time: "a pending mirror cannot be turned".
5. Lock text vs Bronze: Actions say "just placed or turned cannot be turned for the next 5 turns", Bronze says the lock starts when active (first TURN at t+7, F6 timeline). Give one rule: "A mirror cannot be turned while lock > 0; lock is set to 5 when the mirror becomes active or is turned and drops by 1 each turn".
6. Input fields for pending mirrors are under-specified: F5 says a pending mirror shows life 20, lock 0, and a rival sees type -1; the statement gives only type -1. Add "pending mirror: lock 0, life 20; your own pending mirrors show their real type".
7. Supply timing: "Order of a turn" puts supply first, but it does not say whether mirrorsInHand read at turn 4 already includes it (F6 step 0 says yes). Add "so turn 4's input already shows it".

## Should fix (ambiguity)
8. "Lights": a beam crossing the same crystal twice counts once, and a crystal lit by your beam plus yours only is "exactly one player" (F6 uses a lit set). One sentence in Scoring.
9. Scoring bullets (Wood 1+): "streaks of those players stay" applies to players in the contested set; the third bullet then covers the others. Reorder as 4 cases (dormant, alone, contested, nobody) with the nobody case explicit: all streaks drop by 1. Today "nobody lights it" is only implied.
10. Yard: "15 cells around each lamp" includes the lamp cell itself; say "the 5x3 block that contains the lamp (diagonals included)". Say beams still cross yards.
11. Impossible actions: list the ones a bot will hit: out of board, wrong type value, rival yard, pending/wrong cell, empty hand, lock > 0. Say a malformed line (not one of 3 commands with integers) eliminates, but a valid-format illegal action is a WAIT, with a stderr summary. Today "unknown command" vs "impossible" boundary is only inferable.
12. Wood 2 map: legend lists rocks and values 2-3, which cannot occur in Wood 2/1 (values all 1; rocks absent in Wood 2). Fine for tags, but add "[Wood 2] there are no rocks" to avoid bots handling it as a bug. Also init `phase` is -1 in Wood: statement says so; also say it means awake always.
13. Example: no scoring shown. Add one line: "[Wood 1+] at turn 1 the crystal gives 1 x 1 = 1, at turn 2 gives 1 x 2 = 2". Check that (5,6) is a crystal on some real seed N=13, e=3; (2<=e<=c-2 holds), or label the map "an example reef".
14. litBy: "lit it alone last turn": turn 1 value not defined (-1 expected). State it.
15. 2p/3p: "one edge has no lamp and no yard" is in Expert notes; move one line into The reef: "With 2 players the lamps face each other".

## Language
16. "answers too late" -> "does not answer in time". "Coming in moving / Leaves moving" -> "Beam moving / leaves moving". "`sI`" -> "`s_i`". "Mirror tide" and "Crystal tides" are never explained as a name; the word "sleeping" vs input `awake`: use "dormant" or "asleep" consistently (F6 uses dormant).
17. Terms: statement says rock, DESIGN F4 says wall, viewer may say either: keep "rock" in all player-facing text.

## Length and league split
18. Bronze adds 7 rules at once (supply, build delay, hidden type, wear, tides, values 1-3, strike). CG leagues normally add 1-2 per step. Risk: Bronze players hit a wall of text and a game that plays differently from Wood 1. Suggest (owner/designer choice, not a rule change by me): split into Bronze (supply, delay, wear) and Silver (tides, values, strike). Not decided here.
19. The Wood 2 view should fit one screen: Goal, Reef, Beams, Mirrors, Actions (PLACE/WAIT only), Scoring (shared), I/O. With the tags hidden it does; checked by reading only [Wood 2] blocks: ok except item 2.
20. Not checked: rendering of league blocks in the CG statement format, the viewer text.

## Designer answers (2026-11-11 11:51Z) - applied in DESIGN.md "Statement v4" and FINAL RULES
1. Fixed: "Players cannot remove mirrors. [Wood only] Mirrors stay until the end. [Bronze+] Mirrors wear out."
2. Fixed: one tag scheme at the top: [Wood 2 only], [Wood only], [below Silver], [Wood 1+], [Bronze+], [Silver+].
3. Fixed: strike "each turn"; own lamp stops with no points (also in F4); eliminated lamp = rock in End of the game.
4. Fixed: "nobody can place there or turn it" under Build time; impossible-actions list too.
5. Fixed: one lock rule: "set to 5 when the mirror becomes active or is turned"; [Wood only] active on placement.
6. Fixed: pending mirror shows lock 0, life 20; own pending mirrors show the real type.
7. Fixed (rule gap): F6 step 0 now applies supply before turn t's input is sent; statement says it is counted.
8. Fixed: "A beam that crosses the same crystal twice lights it once."
9. Fixed: 4 cases (asleep, alone, several, nobody), "nobody" explicit.
10. Fixed: "5 x 3 block ... the lamp cell, up to 2 along the edge, up to 2 deep, diagonals included"; "Beams cross yards freely."
11. Fixed: impossible actions listed; invalid line (not one of 3 commands with whole numbers) eliminates.
12. Fixed: "[Wood 2 only] There are no rocks", "[below Silver] Every crystal is worth 1", phase -1 = always awake.
13. Fixed: example scoring per league; labelled "an example reef, not a real map" (no real-seed check done).
14. Fixed: litBy is -1 on turn 1 (statement and coder told).
15. Fixed: 2p / 3p lamp layout moved to The reef.
16. Fixed: "does not answer in time", mirror table "Beam moving east / north / ...", s_i, "asleep" used throughout (input field is awake).
17. Fixed: "rock" in all player-facing text (F4 internal name "wall" kept for code).
18. Decided: split. Bronze = mirror economy (supply, build time, sealed type, wear); Silver = tides, values 1-3, strike.
    Reason: CG leagues teach 2-3 rules per step; the economy changes how you play (fewer mirrors, timing), tides and
    strike change what you aim at, so each league has one theme. The hardness gate runs on Silver (top) rules.
19. Checked by reading only [Wood 2 only]/untagged blocks: fits one screen.
20. Not done: CG rendering of league blocks (coder), viewer text (ux).

## Community manager verdict on Statement v4 (re-read, 2026-11-11 11:51)
Read: Statement v4 plus F1, F3, F6 (grep), F9 against it. Verdict: OK to ship to the coder, 1 minor open.
- Checked and consistent: F1 table has a Silver+ column; F3/F4/F6 say Silver+ for tides, values, strike; tag scheme used throughout; items 1-17 and 19 confirmed fixed in the text.
- Open (minor, wording only): Locks paragraph still reads "then drops by 1 at the end of each turn"; F6 skips the drop on the turn of activation or turn. Say "each following turn".
- Not done by me: CG rendering of league tags, viewer text, real-seed check of the example reef.
- 11:51Z designer, re-read: point 1 fixed (lock 'drops by 1 at the end of each following turn'); point 2 checked: F1, F3, F4, F5, F6 and F9 already say Silver+ for tides, values and strike.
