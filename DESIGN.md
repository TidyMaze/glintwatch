# Glintwatch: design document (v1, 2026-11-11)

Name check: no CG game named Glintwatch (../existing-games.md); a web search for "Glintwatch" game found no
title. Slug: `glintwatch`.

# FINAL RULES (v3, red team HOLDS 2026-11-11) - single source for the referee

This section is complete on its own. Everything below "# History" is the design record (v1, v2, v3 deltas)
and is superseded by this section wherever they differ. Numbers marked (T) are tunable at M5 (Config class).

## F1 Constants

| Name | Wood 2 | Wood 1 | Bronze | Silver+ (top) |
|---|---|---|---|---|
| `MAX_TURNS` | 100 | 150 | 150 | 150 |
| Mirrors | stock 12 | stock 12 | start 4, +1 on turns `t % 4 == 0` (T), hold max 6 (T) | same as Bronze |
| `LOCK` (T) | - (no TURN) | 5 | 5 | 5 |
| Build delay + sealed type | no | no | yes (reflects from t+2) | yes |
| `MIRROR_LIFE` (T) | - | - | 20 active turns | 20 |
| Crystal cycle (T) | always awake | always awake | always awake | awake when `(turn + phase) % 45 < 30` |
| Crystal values | 1 | 1 | 1 | 1-3 |
| Scoring | shared | alone + streak | alone + streak | alone + streak |
| `STREAK_CAP` (T) | - | 4 | 4 | 4 |
| `STRIKE_BONUS` (T) | - | - | - | 4 per turn |
| Rocks, start mirrors | no | yes | yes | yes |
| Zone radius (Chebyshev) | 2 | 2 | 2 | 2 |

League split (statement review item 18): Bronze adds the mirror economy (supply, build delay, sealed type, wear);
Silver adds tides, values 1-3 and strike. The hardness gate (F11) runs on Silver rules.

Timeouts: 1000 ms first turn, 50 ms after. Turns are numbered from 1.

## F2 Board and seats
- Grid `N x N`, `x` = column (0 left), `y` = row (0 top). `c = (N-1)/2`. Directions E (1,0), W (-1,0), N (0,-1), S (0,1).
- Rotation `R(x,y) = (N-1-y, x)`; on directions `R(dx,dy) = (-dy,dx)`; R swaps mirror type 0 `/` <-> 1 `\`.
- Seat s lamp at `R^s(0,e)` firing `R^s(E)`: seat 0 `(0,e)` E, seat 1 `(N-1-e,0)` S, seat 2 `(N-1,N-1-e)` W, seat 3 `(e,N-1)` N.
- Player id -> seat: 4p seat = id; 2p ids 0,1 -> seats 0,2; 3p seat = id, seat 3 empty.
- Lamp line: the straight line a lamp fires along. Zone: cells at Chebyshev distance <= 2 from a lamp (15 cells).
  Only seats holding a player (active or deactivated) have a zone; empty-seat cells are ordinary.

## F3 Generation
`Random rnd = new Random(mix(seed))`, `mix` = SplitMix64 finalizer:
`z = seed + 0x9E3779B97F4A7C15L; z = (z ^ (z>>>30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z>>>27)) * 0x94D049BB133111EBL; return z ^ (z>>>31);`
Calls in this order:
1. `N = 13 + 2*rnd.nextInt(4)`. 2. `e = 2 + rnd.nextInt(c-3)`.
3. `Q0 = {0<=x<=c-1, 0<=y<=c}`. Candidates = Q0 cells sorted by (y,x), not on the border, not on any of the 4 lamp
   lines, not in any of the 4 zones (all seats). `A` = count. 4. `Collections.shuffle(candidates, rnd)`.
5. `K = A/4 + rnd.nextInt(3)`, `Wn = A/10 + rnd.nextInt(2)`, `M0 = 2 + rnd.nextInt(3)`, each clamped so the total <= A.
6. First K: crystals, each `r = rnd.nextInt(100)`: value 1 if r<50, 2 if r<85, else 3. Next Wn: walls. Next M0:
   mirrors with type `rnd.nextInt(2)`.
7. For each crystal in list order: `phase = rnd.nextInt(45)`.
8. Copy every object to R, R^2, R^3 (same value and phase; mirror type swapped on R and R^3).
9. Centre `(c,c)`: crystal value 3, phase 0.
League overrides (the rnd calls still happen, layout identical across leagues): Wood 2 drops walls and start
mirrors; below Silver all values are 1 and phase is ignored (always awake).
Start mirrors: active, lock 0, life 20 from Bronze (-1 = no decay in Wood 1).
Facts (computed): `A` = 12-17 (N=13), 22-27 (15), 34-39 (17), 48-53 (19); seeds 0..199 must give each N 30-70 times.

## F4 Cells
| Cell | Beam |
|---|---|
| Empty, pending mirror | passes |
| Wall | absorbed (stops) |
| Crystal | passes and lights it for the shooter |
| Active mirror `/` (0) | E->N, N->E, W->S, S->W |
| Active mirror `\` (1) | E->S, S->E, W->N, N->W |
| Lamp of an active player | stops; strike if it is another player's (Silver+). A beam cannot reach its own lamp (reversible optics: retracing needs a 180-degree turn) |
| Lamp of a deactivated player | wall |
| Lamp cell of an empty seat | Empty (it is '.' in the map); PLACE allowed (QA Q5) |
Beams never block beams. Mirrors belong to nobody.

## F5 Protocol
Init:
```
playerCount myId
N
playerCount lines: playerId lampX lampY dir          (dir in E S W N)
N lines: row y, N chars: '.' empty, '#' wall, '1'-'3' crystal, 'A'-'D' lamp of player 0-3
crystalCount
crystalCount lines: x y value phase                  (sorted by (y,x); phase -1 below Silver = always awake)
```
Each turn:
```
turn
playerCount lines: score mirrorsInHand active        (player id order; active 1/0)
mirrorCount
mirrorCount lines: x y type lock pending life        (sorted by (y,x))
   type 0/1, or -1 for a rival's pending mirror; lock = turns before it can be turned (0 = free);
   pending 1 = placed last turn, not reflecting yet, 0 = active; life = active turns left (-1 = no decay; pending shows 20)
crystalCount
crystalCount lines: x y awake litBy s_0 ... s_{playerCount-1}
   awake 1/0; litBy = player who lit it alone last turn, -1 nobody, -2 contested; s_i = player i's streak (0-4)
```
Output, one line: `PLACE x y type` | `TURN x y` | `WAIT`, optional message after the last token (30 chars max).
Constraints: 2 <= playerCount <= 4; 13 <= N <= 19, odd; 0 <= mirrorsInHand <= 12.

## F6 Turn resolution
0. Supply (Bronze+), applied **before turn t's input is sent**: if `t % 4 == 0`, each active player gets +1 mirror,
   max 6 (so the input of turn 4 already shows it).
1. Read: timeout or a line that is not one of the 3 commands with integer arguments -> deactivate (F8).
2. Validate on the start-of-turn state; invalid -> WAIT with a summary line:
   PLACE: in board, type 0/1, cell Empty (not wall, crystal, the lamp cell of a seat holding a player, mirror active or pending), not in
   another player's zone, `mirrorsInHand > 0`. TURN (Wood 1+): an active mirror there, lock 0, not in another
   player's zone. Wood 2: TURN invalid.
3. Places: 2+ valid PLACE on one cell: all fail, no mirror spent. Else the mirror appears, hand -1.
   Bronze+: `pending = 1`, `lock = 0`, `life = 20`, type hidden from rivals. Wood: active at once, `lock = LOCK` in Wood 1, `lock = 0` in Wood 2 (no TURN there); Wood 2 streaks stay 0.
4. Turns: each mirror targeted by >= 1 valid TURN flips once, `lock = LOCK`.
5. Beams: each active player's lamp fires; trace: step forward; outside board: stop; wall/deactivated lamp: stop;
   active lamp: stop (strike if another player's); crystal: add to shooter's lit set; active mirror: reflect;
   else continue. Cap 4*N*N steps (cannot be reached; assert in tests).
6. Score, per crystal, `L` = players whose lit set holds it:
   - Wood 2: every player in L gains its value.
   - Wood 1+: dormant crystal (Silver+ only): every streak = 0, no gain. Awake and |L| = 1 (player P): `s_P = min(s_P+1, 4)`, P gains
     `value * s_P`; every other player Q: `s_Q = max(s_Q-1, 0)`. Awake and |L| >= 2: players in L keep their streak
     (frozen), nobody gains; players not in L: -1 (min 0). Awake and L empty: every streak -1 (min 0).
   - Strike (Silver+): +3 to the shooter of each beam stopped by another active player's lamp.
7. Mirrors: skip mirrors placed this turn. Bronze+: a mirror with pending 1 activates (`pending 0`, `lock = LOCK`,
   type public). Every active mirror not turned and not activated this turn: `lock = max(lock-1, 0)`; Bronze+ also
   `life -= 1`, removed at 0.
   Timeline (Bronze): placed at t; input t+1 pending 1; reflects on turns t+2 .. t+21; lock 5 at t+2, first TURN at t+7.
   Timeline (Wood 1): placed at t reflects at t; input t+1 lock 5; first TURN at t+6.
8. End: after turn MAX_TURNS, or when fewer than 2 players are active.

## F7 Ranking
Score descending, equal scores share a rank. Deactivated players: score -1.

## F8 Deactivation
Timeout or unparseable output: deactivated, score -1, lamp becomes a wall, hand discarded, its mirrors stay,
its zone stays protected.

## F9 Leagues and bosses
| League | Boss |
|---|---|
| Wood 2 | WAIT every turn |
| Wood 1 | Greedy-self: candidates WAIT, then PLACE (both types) on Empty cells of its own current beam path, then TURN on mirrors of that path; simulate own beam only; value = crystal values lit; first max wins |
| Bronze | G2: Greedy-full plus a 2-turn own search (beam width 10, rivals WAIT); source Boss2.java |
| Silver | Planner: own-beam search + rollout of all lanes over 8 turns; source PlannerBot.java |
Each boss must beat the previous one >= 80 % and lose to the next.
Measured by the coder (1v1, 200 seeds, FINAL RULES v3): Bronze G vs Greedy-self 57 %, Silver G+ vs G 51.8 %:
**target missed**. Not loosened. Re-run by vote-analyst (1v1 x200, seeds 1000+, ties half, M5-LOG.md): Bronze G2 vs Greedy-self
87.3 % PASS; Silver G+2 vs G2 48.3 % FAIL, so the Silver boss is the Planner: Planner vs G2 100 % PASS. Ladder above adopted.
Values 1-3 start in Silver (F1 wins over V3.8 history).

**F9 decision (designer, M5 iter 2).** CG convention: a player in level L is promoted when it ranks above level L's
boss; the boss of the last level gates nothing, it is the IDE default opponent and a leaderboard anchor.
Community games are named from the bottom with wood leagues and end at Bronze (seen on Gargoyles versus Santas:
Wood 4 start; not confirmed in the SDK docs). So the 4 levels are shown as:

| config | design name | CG name (expected) | Boss file | Gates |
|---|---|---|---|---|
| level1 | Wood 2 | Wood 3 | WAIT | promotion to level2 |
| level2 | Wood 1 | Wood 2 | Greedy-self | promotion to level3 |
| level3 | Bronze | Wood 1 | G2 (Boss2.java) | promotion to the top league |
| level4 | Silver | Bronze (top) | Planner (PlannerBot.java) | nothing (IDE opponent, anchor) |

Why Planner on top: it gates no promotion, so its 100 % does not block anyone; a strong anchor shows the depth of
the top rules. The real bar for new players is G2 at the level3 exit (87.3 % vs Greedy-self, like a CG Wood 1 boss).
Condition: the Planner must stay under 50 ms per turn (p99 and max at N=19, 4 players, measured by vote-analyst);
if not, cut its rollout depth until it does, and re-check it still beats G2 >= 80 %.
The statement names no boss and needs no change.
Condition met (vote-analyst, M5-LOG.md iter 3, CPU time, 100 games 4p Silver): N=19 mean 1.29 ms, p99 6.78, max 11.29;
N=17 p99 4.70, max 22.70 (one outlier); N=13/15 max 4.1/5.6. Planner vs 3 G2 (400 games): first 99.5 %, seats 24.8-25.5 %.
F9 ladder is final. Left for M7: CG measures wall time (JIT, GC): check the boss's max time on the CG preview.

## F10 Viewer
ux/UX.md (layout, panels 380x300, colours per ../_art-common/STYLE.md: id0 Sky ring, id1 Amber triangle, id2 Jade
square, id3 Orchid star) and art/ for sprites. Must show: beams, mirrors (pending = outline only for rivals),
lock and life, crystal awake/dormant and turns to change, streak pips per player, hand 0-6.

## F11 Hardness and fairness gate (M5): V3.5, V3.7, V3.9 below.

## F12 Statement (player-facing; league blocks marked)
See section "Statement v4" right below.

## Statement v4 (player-facing, English; after review STATEMENT-REVIEW.md)

League tags (the CG statement shows a block only in the leagues it names):
`[Wood 2 only]`, `[Wood only]` = Wood 2 and Wood 1, `[below Silver]` = Wood 2, Wood 1, Bronze, `[Wood 1+]`,
`[Bronze+]`, `[Silver+]`, and for the story openers only `[Wood 1 only]`, `[Bronze only]`.
Untagged text = all leagues. Tag names are internal labels (config level1-4); player-facing text never names a
league, because CG may show community leagues as Wood 3 / Wood 2 / Wood 1 / Bronze (open check M7: verify on the
CG contribution preview).

> ### League introduction
> This is a **league based** challenge. For this challenge, multiple leagues for the same game are available. Once
> you have proven your worth against the Boss, you will access the higher league.
>
> [Wood 2 only] **This league:** Night falls on the Tidereef. Its crystals drink lamplight and keep it until dawn.
> You keep a lamp on the reef's edge and a bag of 12 mirrors. Bend your beam onto the crystals:
> each turn, every crystal your light touches pays you.
>
> [Wood 1 only] **This league:** The reef is not empty any more: black rocks drink light, and a crystal lit by two
> lamps clouds over. A crystal held by your beam alone warms up and pays more each turn, up to x4.
> Mirrors can now be turned, but a moved mirror needs 5 turns to settle.
>
> [Bronze only] **This league:** The divers bring up only one mirror every 4 turns, and you can carry 6.
> A new mirror takes 2 turns to set, hidden from your rivals, and the salt breaks it after 20 turns.
> Every mirror is a choice now.
>
> [Silver+] **This league:** The tide has come in. Each crystal sleeps 15 turns out of every 45 and forgets who held
> it. Crystals now hold 1 to 3 points of light, and a beam thrown into a rival's lamp pays 2 each turn.
>
> ### Goal
> Score more points than the other lamp keepers by lighting crystals with your lamp's beam.
>
> ### The reef
> The reef is a square grid of `N` x `N` cells (`N` is odd, 13 to 19). `x` is the column (0 on the left), `y` is
> the row (0 at the top); north is towards `y = 0`. 2 to 4 players play. Each player owns a **lamp** on one edge.
> The reef looks the same from every lamp (90-degree rotational symmetry). With 2 players the lamps are on
> opposite edges; with 3 players one edge has no lamp.
>
> Cells:
> - `.` empty: a beam goes through;
> - `1` `2` `3` a crystal and its value: a beam goes through and **lights** the crystal;
> - `A` `B` `C` `D` the lamp of player 0, 1, 2, 3: a beam that reaches it stops;
> - [Wood 1+] `#` rock: a beam stops. [Wood 2 only] There are no rocks.
> - [below Silver] Every crystal is worth 1.
>
> ### Beams
> After all actions of a turn, every lamp fires one beam. It leaves the lamp in the lamp's direction and moves
> cell by cell until it leaves the reef, reaches a rock or reaches a lamp. Beams never block each other and a
> beam cannot loop. A beam that crosses the same crystal twice lights it once. Your lamp's straight line holds no
> crystal, rock or mirror at the start: to score, you must bend your beam.
>
> ### Mirrors
> A mirror turns a beam by 90 degrees:
>
> | Mirror | Beam moving east | north | west | south |
> |---|---|---|---|---|
> | type `0` = `/` | leaves north | leaves east | leaves south | leaves west |
> | type `1` = `\` | leaves south | leaves west | leaves north | leaves east |
>
> ![How the two mirror types bend a beam](img/mirrors.png)
>
> Mirrors belong to nobody: a mirror bends **every** beam that reaches it, yours and your opponents'.
> Players cannot remove mirrors. [Wood only] Mirrors stay until the end. [Bronze+] Mirrors wear out (see below).
>
> ### Actions
> Each turn you print **one** action:
> - `PLACE x y type`: put one of your mirrors on an empty cell. [Wood only] You have 12 mirrors for the whole game.
> - [Wood 1+] `TURN x y`: flip a mirror (`/` becomes `\` and back).
> - `WAIT`: do nothing.
>
> You may add a message after your action, for example `PLACE 5 3 1 hello` (30 characters max).
>
> **Yards.** Each lamp's yard is the 5 x 3 block of cells around it (the lamp cell, and every cell up to 2 cells
> away along the edge and up to 2 cells deep, diagonals included). Only the yard's owner may place[Wood 1+] or turn[/]
> mirrors there. Beams cross yards freely.
>
> **Same cell.** If two or more players place a mirror on the same cell in the same turn, all of them fail and
> keep their mirror. [Wood 1+] If several players turn the same mirror in the same turn, it flips once.
>
> [Wood 1+] **Locks.** Each mirror has a `lock`. You can turn a mirror only when its lock is 0. The lock is set
> to 5 when the mirror becomes active or is turned, then drops by 1 at the end of each following turn
> (so it reads 5 on the next turn's input).
> [Wood only] A mirror becomes active as soon as it is placed.
>
> **Impossible actions** are replaced by `WAIT` (the reason is shown in the game summary): a cell outside the
> reef, a type other than 0 or 1, a cell that is not empty (rock, crystal, lamp, mirror, [Bronze+] pending mirror),
> a cell in another player's yard, no mirror in hand, [Wood 1+] turning where there is no active mirror or the
> lock is not 0. A line that is not a valid command (keyword and whole numbers) eliminates you.
>
> ### Scoring
> [Wood 2 only] Each turn, every player whose beam lights a crystal gains its value. Several beams on one crystal
> all score it.
>
> [Wood 1+] Each player has a **streak** (0 to 4) on each crystal. Each turn, for each crystal:
> - [Silver+] if the crystal is asleep: all streaks on it become 0 and nobody gains;
> - lit by **exactly one** player: that player's streak grows by 1 (max 4) and the player gains
>   `value x streak`; every other player's streak drops by 1 (min 0);
> - lit by **two or more** players: nobody gains; those players keep their streak; the others' drop by 1;
> - lit by **nobody**: every streak drops by 1 (min 0).
> A crystal you keep alone pays x1, x2, x3, then x4 every turn.
>
> [Wood 1+] ![A crystal lit by one beam scores; lit by two beams it scores nothing](img/contested.png)
>
> [Wood 1+] ![Streak badges x1 to x4 under a crystal](img/streak.png)
>
> ### [Bronze+] Mirror economy
> - **Supply.** You start with 4 mirrors and get 1 more at the start of turns 4, 8, 12 ... (the input of that
>   turn already counts it). You hold at most 6; extra mirrors are lost.
> - **Build time.** A mirror placed on turn `t` is **pending** on turn `t+1` and bends beams from turn `t+2`.
>   While pending, beams go through its cell, nobody can place there or turn it, and your opponents read its type
>   as `-1` (you read the real type). A pending mirror shows `lock 0` and `life 20`.
> - **Wear.** An active mirror bends beams for 20 turns, then shatters at the end of its 20th turn; its cell becomes
>   empty. `life` gives the turns left.
>
> ### [Silver+] Tides, values and strikes
> - **Tides.** Each crystal is awake 30 turns out of every 45: on turn `t` it is awake when `(t + phase) % 45 < 30`,
>   otherwise asleep. `phase` is given at the start. An asleep crystal gives nothing and resets the streaks on it.
> - **Values.** Crystals are worth 1, 2 or 3.
> - **Strike.** Each turn, a beam that ends on another player's lamp gives 2 points to the player who fired it.
>
> ### Order of a turn
> - [Bronze+] Supply (before the input is sent).
> - All placements[Wood 1+], then all turns[/].
> - Every lamp fires.
> - Scoring.
> - [Wood 1+] Locks drop by 1.
> - [Bronze+] Pending mirrors become active, active mirrors wear.
>
> ### End of the game
> The game ends after [Wood 2 only] 100 [Wood 1+] 150[/] turns, or when fewer than two players remain.
> Equal scores share the rank. An eliminated player gets a score of -1; its lamp becomes a rock and its yard stays
> protected.
>
> ### Victory conditions
> - Have the highest score when the game ends.
>
> ### Defeat conditions
> - Have a lower score than another player when the game ends.
> - Your program does not answer in time, crashes, or prints a line that is not a valid command (keyword and whole
>   numbers): you are eliminated.
>
> ### Example (an example reef, not a real map)
> `N = 13`. Your lamp is at (0,3) and fires east; a crystal of value 1 is at (5,6). You print `PLACE 5 3 1`.
> Your beam goes (1,3) ... (5,3), turns south on `\`, crosses (5,4), (5,5), lights (5,6) and goes on south.
> [Wood 2 only] You gain 1 every turn. [Wood 1+] If nobody else lights it you gain 1, 2, 3, then 4 per turn.
> [Bronze+] The mirror starts bending beams 2 turns after you place it.
>
> ### Game input
> **Initialization input**
> - Line 1: two space-separated integers `playerCount` and `myId`: the number of players (`2` to `4`) and your
>   player id (`0` to `playerCount` - 1).
> - Line 2: an integer `N`: the width and height of the reef.
> - Next `playerCount` lines: three space-separated integers and a character, `playerId` `lampX` `lampY` `dir`:
>   a player's id, the cell of its lamp, and the direction its lamp fires (`E`, `S`, `W` or `N`).
> - Next `N` lines: a string `row` of `N` characters, one row of the reef from `y = 0` down: `.` empty,
>   [Wood 1+] `#` rock,[/] `1` `2` `3` crystal of that value, `A` `B` `C` `D` lamp of player `0` `1` `2` `3`.
> - Next line: an integer `crystalCount`: the number of crystals.
> - Next `crystalCount` lines: four space-separated integers `x` `y` `value` `phase`: a crystal's cell, its value
>   and its tide phase[below Silver] (always `-1` in this league: always awake)[/].
>
> **Input for one game turn**
> - Line 1: an integer `turn`: the turn number, starting at `1`.
> - Next `playerCount` lines, by player id: three space-separated integers `score` `mirrorsInHand` `active`: the
>   player's score, the mirrors it can still place, and `1` if it is in the game (`0` if eliminated).
> - Next line: an integer `mirrorCount`: the number of mirrors on the reef.
> - Next `mirrorCount` lines: six space-separated integers `x` `y` `type` `lock` `pending` `life`. Every listed
>   mirror, pending or active, fills its cell: you cannot place there.
>   - `x` `y`: the mirror's cell.
>   - `type`: `0` (`/`) or `1` (`\`)[Bronze+]; `-1` for an opponent's pending mirror.
>   - `lock`: turns before it can be turned; `0` when it can be turned[Wood 2 only] (always `0` in this league)[/].
>   - `pending`: `1` if not yet bending beams, else `0`[Wood only] (always `0` in this league)[/].
>   - `life`: active turns left before it shatters[Wood only] (always `-1` in this league: mirrors never wear)[/][Bronze+];
>     `0` for a mirror that shattered at the end of the last turn (listed once; its cell is empty)[/].
> - Next line: an integer `crystalCount`: the number of crystals.
> - Next `crystalCount` lines: `4 + playerCount` space-separated integers `x` `y` `awake` `litBy` `s_0` ... `s_(playerCount-1)`:
>   - `x` `y`: the crystal's cell.
>   - `awake`: `1` or `0`[below Silver] (always `1` in this league)[/].
>   - `litBy`: the player who lit it alone on the last turn; `-1` nobody (and on turn `1`); `-2` several players.
>   - `s_i`: player i's streak on it, `0` to `4`[Wood 2 only] (always `0` in this league)[/].
>
> **Output for one game turn**
> - A single line with one action, optionally followed by a space and a message (`30` characters max):
>   - `PLACE x y type`: place a mirror on cell (`x`, `y`); `type` is `0` (`/`) or `1` (`\`).
>   - [Wood 1+] `TURN x y`: flip the mirror on cell (`x`, `y`).
>   - `WAIT`: do nothing.
> - Tokens are separated by spaces; any other separator (a tab, for example) makes the line invalid.
> - Example: `PLACE 5 3 1 hello`
>
> **Constraints**
> - 2 ≤ `playerCount` ≤ 4
> - 13 ≤ `N` ≤ 19, `N` is odd
> - Response time for the first turn ≤ 1000 ms
> - Response time for one turn ≤ 50 ms
>
> ### Technical details
> - Rows and lists are sorted by `y`, then `x`.
> - You can read the referee source code on https://github.com/TidyMaze/glintwatch
> - Glintwatch is an original game; its idea comes from the genre of laser-and-mirror reflection puzzles.
>
> ### League preview
> [Wood 2 only] Rocks, turning mirrors with locks, and crystal streaks that pay more the longer you hold a crystal alone.
>
> [Wood 1 only] A mirror economy: a mirror supply, build time with hidden types, and wear.
>
> [Bronze only] Crystal tides, crystal values from 1 to 3, and strikes on rival lamps.

# History (design record; superseded by FINAL RULES above)

## 1. Pitch
Night on the Glint Reef. Four lamp keepers each own a lamp fixed on one edge of the reef. Every turn every lamp
fires a beam across the reef. Keepers place mirrors and flip them; a mirror belongs to nobody and bends every
beam that touches it. A crystal crossed by exactly one beam gives its value to that beam's owner; a crystal
crossed by two or more beams flickers and gives nothing. Most points after 150 turns wins.

## 2. Constants (all leagues unless the league table says otherwise)

| Name | Value |
|---|---|
| `MAX_TURNS` | 150 (Wood 2: 100) |
| `MIRROR_STOCK` | 20 per player |
| `LOCK` | 3 |
| `STRIKE_BONUS` | 4 |
| `ZONE_RADIUS` | 2 (Chebyshev) |
| `N` (board size) | odd, in {13, 15, 17, 19} |
| First-turn timeout | 1000 ms |
| Turn timeout | 50 ms |
| Message length | 30 chars max (longer is cut) |

## 3. Board

### 3.1 Coordinates and directions
- Square grid `N x N`, cell `(x, y)`, `x` = column 0..N-1 left to right, `y` = row 0..N-1 top to bottom.
- `c = (N - 1) / 2`; the centre cell is `(c, c)`.
- Directions: `E = (1,0)`, `W = (-1,0)`, `N = (0,-1)`, `S = (0,1)`.

### 3.2 Rotation and seats
- `R(x, y) = (N-1-y, x)` (90 degrees clockwise on screen). On directions: `R(dx, dy) = (-dy, dx)`, so E->S->W->N->E.
- Mirror types: `0` = `/` (joins bottom-left and top-right corners), `1` = `\`. **`R` swaps the type** (0 <-> 1).
- Seat `s` (0..3) has its lamp at `R^s(0, e)`, firing `R^s(E)`:

| Seat | Lamp cell | Fires |
|---|---|---|
| 0 | `(0, e)` | E |
| 1 | `(N-1-e, 0)` | S |
| 2 | `(N-1, N-1-e)` | W |
| 3 | `(e, N-1)` | N |

- Player id -> seat: 4 players: seat = id. 2 players: id 0 -> seat 0, id 1 -> seat 2. 3 players: seat = id,
  seat 3 is empty (its cells are ordinary cells; generation still keeps them clear, see 3.3).
- Lamp line of seat s: the straight line its lamp fires along (seat 0: row `e`; seat 1: column `N-1-e`;
  seat 2: row `N-1-e`; seat 3: column `e`).
- Zone of seat s: cells at Chebyshev distance <= 2 from its lamp, inside the board (15 cells for a lamp on an edge).

### 3.3 Generation (from the game seed; `Random rnd = new java.util.Random(seed)`, calls in this order)
1. `N = 13 + 2 * rnd.nextInt(4)`; `c = (N-1)/2`.
2. `e = 2 + rnd.nextInt(c - 3)` (so `2 <= e <= c-2`).
3. Fundamental quarter `Q0 = { (x, y) : 0 <= x <= c-1, 0 <= y <= c }`. `Q0, R(Q0), R^2(Q0), R^3(Q0)` and the
   centre partition the board.
4. Candidates = cells of `Q0`, sorted by `(y, x)`, that are not on the border, not on any of the 4 lamp lines,
   not in any of the 4 zones (all 4 seats, even when fewer players). `A` = number of candidates.
5. `Collections.shuffle(candidates, rnd)`.
6. Counts (each clamped so the running total stays <= `A`):
   - crystals `K = A / 4 + rnd.nextInt(3)` (integer division);
   - walls `Wn = A / 10 + rnd.nextInt(2)`;
   - start mirrors `M0 = 2 + rnd.nextInt(3)`.
7. First `K` candidates: crystals; for each in list order, `r = rnd.nextInt(100)`: value 1 if `r < 50`, 2 if `r < 85`, else 3.
   Next `Wn`: walls. Next `M0`: mirrors with type `rnd.nextInt(2)`, lock 0.
8. Copy every placed object to `R(p), R^2(p), R^3(p)`, same value; mirror type swapped on `R` and `R^3`, kept on `R^2`.
9. Centre `(c, c)`: crystal of value 3.

Wood 2 overrides: walls 0, start mirrors 0, all crystal values 1 (the `rnd` calls still happen so maps keep the
same crystal layout across leagues for a given seed).

Result: lamp lines are free of walls, crystals and start mirrors, so each lamp's first beam crosses the board
straight and scores 0. Candidate count `A` (computed): N=13: 12-17, N=15: 22-27, N=17: 34-39, N=19: 48-53
(e = 2 gives the most). Crystals then total `4K + 1`: 13-25 on N=13, 49-61 on N=19.

## 4. Cells and objects
| Object | Beam behaviour | Can hold a mirror |
|---|---|---|
| Empty | passes | yes |
| Wall | stops the beam (absorbed) | no |
| Crystal (value 1-3) | passes; the crystal is lit by this beam | no |
| Mirror type 0 `/` | E->N, N->E, W->S, S->W | already one |
| Mirror type 1 `\` | E->S, S->E, W->N, N->W | already one |
| Lamp of an active player | stops the beam; Strike if it is another player's lamp | no |
| Lamp of an inactive player or of an empty seat in 3p | absent: empty seat lamp cell is Empty; an inactive player's lamp acts as a Wall | no |

Beams never block each other. Mirrors are neutral: no owner in the rules (the viewer shows who touched it last).

## 5. Turn protocol

### 5.1 Initialization input
```
Line 1: playerCount myId
Line 2: N
Next playerCount lines (player id order): playerId lampX lampY dir      (dir is one of E S W N)
Next N lines: a string of N chars, row y = 0..N-1:
   '.' empty   '#' wall   '1' '2' '3' crystal of that value   'A' 'B' 'C' 'D' lamp of player 0 1 2 3
```
The static map never changes. Mirrors are not in it (they come every turn). In 3p the empty seat's lamp cell is '.'.

### 5.2 Per-turn input
```
Next playerCount lines (player id order): score mirrorsLeft active      (active: 1 or 0)
Next line: mirrorCount
Next mirrorCount lines: x y type lock      (type 0 = '/', 1 = '\'; lock = turns before it can be turned, 0 = free)
```
Mirrors are listed sorted by `(y, x)`.

### 5.3 Output (exactly one line per turn)
```
PLACE x y type   put one of your mirrors on an empty cell
TURN x y         flip the mirror at (x, y) ('/' <-> '\')
WAIT             do nothing
```
Any text after the last expected token is a message shown in the viewer (max 30 chars).
Tokens are separated by spaces; commands are upper case.

### 5.4 Constraints
`2 <= playerCount <= 4`, `13 <= N <= 19` (odd), `0 <= mirrorCount <= N*N`, `0 <= lock <= 3`,
`0 <= mirrorsLeft <= 20`. Response time: 1000 ms first turn, 50 ms after.

## 6. Resolution order (one game turn, all players at once)
1. **Read** one line from every active player. Timeout or a line that does not parse as one of the three
   commands (wrong keyword, missing or non-integer token): the player is **deactivated** (6.8).
2. **Validate** each command against the state at the start of the turn. An invalid command becomes WAIT and the
   summary says why. Invalid when:
   - PLACE: `x, y` out of the board; type not 0/1; cell not Empty (wall, crystal, lamp cell of any seat, a mirror);
     cell in another seat's zone (a cell in your own zone is fine); `mirrorsLeft = 0`.
   - TURN: no mirror at `(x, y)` at the start of the turn; `lock > 0`; cell in another seat's zone.
   - Wood 2: TURN is invalid (statement does not show it).
3. **Places**: if two or more valid PLACE target the same cell, all of them fail (no mirror placed, no stock
   used; summary "collision"). Otherwise the mirror appears, `mirrorsLeft -= 1`, `lock = LOCK`, `lastToucher = player`.
4. **Turns**: every mirror targeted by at least one valid TURN flips once (several TURNs on one mirror = one
   flip), `lock = LOCK`, `lastToucher` = lowest player id among those who turned it (viewer only).
   A TURN cannot target a mirror placed in step 3 (it did not exist at the start of the turn).
5. **Beams**: each active player's lamp fires (trace in 6.1) on the updated board.
6. **Score** (6.2) and **strike** (6.3).
7. **Locks**: every mirror not placed or turned this turn with `lock > 0` does `lock -= 1`.
   (Placed at turn t: inputs at t+1, t+2, t+3 show 3, 2, 1; it can be turned from turn t+4.)
8. **End check** (6.5).

### 6.1 Beam trace
```
pos = lamp cell, d = lamp dir, steps = 0
loop:
  pos += d; steps += 1
  if pos is outside the board: stop (exits)
  if steps > 4*N*N: stop (safety; cannot happen with these optics, assert in tests)
  cell = board[pos]
  if wall or inactive lamp: stop (absorbed)
  if lamp of an active player: if it is not the shooter, the shooter strikes it; stop
  if crystal: add pos to the shooter's lit set (a set: a beam crossing a crystal twice lights it once); continue
  if mirror: d = reflect(type, d); continue
  empty: continue
```
The path can cross itself and cross other beams. Mirror optics are reversible, so a beam that starts on the
edge cannot cycle; the cap is only a guard.

### 6.2 Crystal scoring
For each crystal: `L` = set of players whose lit set contains it.
- Wood 2: every player in `L` gains the crystal value.
- Wood 1 and up: if `|L| = 1`, that player gains the value; if `|L| >= 2`, nobody gains (contested).

### 6.3 Strike (Bronze and up)
A beam stopped by another active player's lamp gives its shooter `STRIKE_BONUS = 4` this turn.
The struck player loses nothing. At most one strike per beam per turn (a beam stops there).

### 6.4 Score
Score = sum of all gains since turn 1. Shown in the input as `score`.

### 6.5 End of game
- After turn `MAX_TURNS`, or
- at the end of a turn where fewer than 2 players are active (2p: the opponent of a deactivated player wins at once).

### 6.6 Ranking
Players are ranked by final score, higher first. Equal scores share the rank (CG `setScore` handles this).
A deactivated player gets score -1 (all deactivated players share the last rank).

### 6.7 Invalid output summary
Invalid command: kept in the game, WAIT this turn, red line in the game summary
(`Player X: PLACE 5 1 0 refused: cell in zone of player Y`).

### 6.8 Deactivation
Timeout or unparseable line: `player.deactivate(reason)`, score set to -1, its lamp stops firing and becomes a wall,
its unused mirrors are gone, its placed mirrors stay (neutral).

## 7. Leagues

| League | Rules added | Boss |
|---|---|---|
| Wood 2 | PLACE, WAIT; no walls, no start mirrors, crystals value 1; shared scoring (6.2 Wood 2); 100 turns | `WAIT` every turn |
| Wood 1 | TURN and locks; walls; start mirrors; contested crystals score 0; 150 turns | Greedy-self |
| Bronze | crystal values 1-3; strike | Greedy-full |

Zones exist in all leagues. Community leagues above Bronze use Bronze rules.

- **Greedy-self** (Wood 1 boss): candidate actions = WAIT, then every legal PLACE (both types) on an Empty cell of
  its own current beam path, then every legal TURN of a mirror on that path. For each, simulate the board with only
  this action and trace **its own beam only**; value = sum of crystal values it lights (no contest check).
  Pick the max; ties keep the first in list order (so WAIT unless strictly better). Path cells in list order of
  the trace.
- **Greedy-full** (Bronze boss): same candidates, but traces all active beams with the others' actions = WAIT and
  scores `ownGain - 0.5 * max over opponents of their gain`, with contest and strike rules.

## 8. Starter AI (stub)
Reads all inputs and prints `WAIT`. The stub comment says: "Your lamp fires east-like along its line; put a
mirror on that line to bend it towards crystals." (Stub generator: `read`/`loop` with the types in 5.1-5.2.)

## 9. Balance notes
- Score scale: a bot holding a beam over 5 uncontested crystals of mean value 1.65 scores ~8/turn, ~1200/game.
- Stock 20 over 150 turns: a placement every turn ends at turn 20; after that only flips. This rewards
  well-placed early mirrors and makes the mid game a flip fight under locks.
- `LOCK = 3`: a flip holds at least 3 turns. With LOCK 1 two players can flip-war every turn (stalemate);
  with LOCK 5+ the first mover keeps a mirror too long.
- Strike 2 ~ one mid crystal: worth it only as the end of a path that already scores.
- Zones: the 15 cells give each lamp a protected launch; zones hold no crystals, so camping scores 0.
- Players cannot absorb a beam (no player walls), so no lamp can be shut off by others: denial is by jamming
  (contest) or sending a beam into a wall or off the board.

### Exploit check
| Strategy | Outcome |
|---|---|
| WAIT forever | straight lamp line holds no crystals: 0 points |
| Mirror cage around a rival zone | zone border has 9+ exit cells and mirrors only reflect: needs >10 mirrors and turns; rival re-routes inside its zone |
| Flip war on one mirror | each flip holds 3 turns; alternating players each get half |
| Same-cell PLACE to block | both fail and both lose the turn: symmetric cost |
| Jam the leader | jammer also scores 0 on that crystal; 4p kingmaking accepted as in other 4p games |
| Mirror placed on own lamp line just outside zone by a rival | own zone mirrors bend the beam before it gets there |

### Metrics (for game-analyst)
- Seat fairness: over 1000 games Greedy-full x4 on random seeds, win rate per seat 25 % +/- 3 %.
- Draws (all 4 equal): < 5 % of games between different bots.
- Greedy-full beats Greedy-self >= 80 % in 1v1; WAIT bot scores 0 in 100 % of games.
- Contested crystals per turn after turn 50: 10-30 % of lit crystals (too low = no interaction, too high = chaos).
- Referee time per turn < 1 ms; game length = 150 turns in > 95 % of games.

## 10. Worked examples (N = 13, c = 6, e = 3)
Lamps: P0 (0,3) E, P1 (9,0) S, P2 (12,9) W, P3 (3,12) N. P0 zone: x 0..2, y 1..5.
Assume crystals at (5,6) value 2, (8,6) value 1, (6,6) value 3 (centre), wall at (5,10) (lamp lines rows 3, 9 and columns 3, 9 stay empty).

1. Turn 1, P0 `PLACE 5 3 1`. (5,3) is outside every zone. Beam P0: (1,3)..(4,3), (5,3) `\` E->S, (5,4), (5,5),
   (5,6) lit, (5,7), (5,8), (5,9), (5,10) wall: stop. P0 gains 2. Input of turn 2 lists `5 3 1 3`.
2. Turn 2, P0 `PLACE 5 3 0` again: invalid (cell holds a mirror) -> WAIT. P0 `TURN 5 3`: invalid (lock 3).
3. Turn 2, P1 `PLACE 9 6 0` and P2 `PLACE 9 6 1`: collision, both fail, both keep 20 mirrors.
4. Turn 3, P1 beam (9,1)..(9,5) S, P1 places `PLACE 9 6 1`: `\` S->E -> (10,6), (11,6), (12,6), exits: 0.
   With `PLACE 9 6 0` instead: `/` S->W -> (8,6) lit, (7,6), (6,6) lit, (5,6) lit, (4,6)... exits at x=-1.
   If P0 still lights (5,6): (5,6) contested, nobody gets 2; P1 gets 1 + 3 = 4.
5. Turn 5, P2 and P3 both `TURN 5 3` (lock now 0): one flip to `/`. P0 beam at (5,3) goes E->N: (5,2), (5,1),
   (5,0), exits: P0 scores 0 this turn, and (5,6) is now P1's alone.
6. Strike (Bronze): a beam that reaches (12,9) from the west side of the board, travelling E, stops on P2's lamp:
   shooter +2.

## 11. Statement draft (player-facing, Bronze; league tags hide parts)

> **Goal**
> Score more points than the other lamp keepers by lighting crystals with your beam.
>
> **Rules**
> The reef is a square grid of `N` x `N` cells (13 to 19). `x` grows to the right, `y` grows downwards.
> Each player owns a lamp on one edge of the reef. Every turn, every lamp fires a straight beam into the reef.
>
> Cells can be:
> - empty `.`: the beam goes through;
> - a rock `#`: the beam stops;
> - a crystal `1` `2` `3`: the beam goes through and lights the crystal;
> - a lamp `A` `B` `C` `D`: the beam stops.
>
> Mirrors change a beam's direction by 90 degrees. Type `0` is `/` and type `1` is `\`.
> A mirror belongs to nobody: it bends every beam that reaches it, yours and your opponents'.
>
> Each turn you may do **one** action:
> - `PLACE x y type`: put a mirror on an empty cell. You have 20 mirrors for the whole game.
> - `TURN x y`: flip a mirror (`/` becomes `\` and back). A mirror that was just placed or turned is locked for
>   the next 3 turns.
> - `WAIT`.
>
> The 5x3 area around each lamp is that keeper's yard: only its owner may place or turn mirrors there.
> If two players place a mirror on the same cell in the same turn, both fail and keep their mirror.
> If several players turn the same mirror in the same turn, it flips once.
>
> After all actions, every lamp fires. A crystal lit by exactly one player gives that player its value.
> A crystal lit by two or more players gives nothing.
> A beam that ends on another player's lamp gives its owner 2 points.
>
> **End of the game**
> After 150 turns, the highest score wins. A player who does not answer in time, or answers with an unknown
> command, is eliminated. An impossible action (for example placing on a rock) is replaced by WAIT.
>
> **Victory conditions**: have the highest score after 150 turns.
> **Defeat conditions**: a lower score, or eliminated.
>
> **Expert rules**: the map has a 90-degree rotational symmetry; your lamp's straight line never holds a crystal
> or rock at the start. A beam cannot loop. Mirrors placed this turn bend beams this turn.
>
> Input and output: as in section 5 (the statement shows the same blocks, with the constraints).
>
> Wood 2 text: no TURN, no rocks, all crystals worth 1, every beam on a crystal scores it, no lamp bonus, 100 turns.
> Wood 1 text: adds TURN and locks, rocks, shared crystals score nothing.

## 12. Viewer (CG SDK GraphicEntityModule, canvas 1920 x 1080)

### 12.1 Layout
- Board square of side `S = 1000` px, `cell = floor(1000 / N)` (N=13: 76, 15: 66, 17: 58, 19: 52).
  Board origin `x0 = (1920 - N*cell) / 2`, `y0 = 40 + (1000 - N*cell) / 2`.
- HUD: 4 panels 380 x 220, left column x = 40 (seat 0 at y = 120, seat 3 at y = 740), right column x = 1500
  (seat 1 at y = 120, seat 2 at y = 740), so each panel sits next to its lamp's side.
- Turn counter at top centre (y = 10, "Turn 37 / 150", font 32 px).

### 12.2 Player colours (Okabe-Ito, colour-blind safe) and second cue (shape)
| Id | Colour | Hex | Shape on lamp and HUD |
|---|---|---|---|
| 0 | orange | `#E69F00` | circle |
| 1 | sky blue | `#56B4E9` | triangle |
| 2 | bluish green | `#009E73` | square |
| 3 | reddish purple | `#CC79A7` | diamond |
Contested crystal: white `#FFFFFF`. Background: navy `#0B1426`.

### 12.3 Layers (z-index low to high)
0 background, 1 floor tiles, 2 zone tints, 3 rocks and crystals, 4 beams, 5 mirrors, 6 lamps, 7 effects,
8 HUD, 9 messages. Tooltips by TooltipModule.

### 12.4 Sprites (PNG, white or grey base where tinted at runtime; drawn at 128 px, scaled to `cell`)
| Name | Size px | Frames | Notes |
|---|---|---|---|
| `background.png` | 1920 x 1080 | 1 | dark sea with faint waves, no text |
| `floor_0.png`, `floor_1.png` | 128 x 128 | 1 each | reef stone, two variants (checker by `(x+y)%2`) |
| `rock.png` | 128 x 128 | 1 | dark basalt block |
| `crystal_1.png`, `crystal_2.png`, `crystal_3.png` | 128 x 128 | 1 each | grey-white crystal, 1/2/3 points (size and facets grow); tinted to the owner colour when lit |
| `crystal_glow.png` | 128 x 128 | 1 | soft radial glow, additive, tinted, alpha 0 when unlit |
| `mirror.png` | 128 x 128 | 1 | silver bar `/` with a small base; type 1 = same sprite rotated 90 degrees |
| `lock_pip.png` | 24 x 24 | 1 | small dot; 0-3 pips under a mirror show the lock |
| `lamp_base.png` | 128 x 128 | 1 | lighthouse lamp, white, tinted per player, faces E (rotated for other seats) |
| `shape_circle.png`, `shape_triangle.png`, `shape_square.png`, `shape_diamond.png` | 48 x 48 | 1 each | white, tinted, on the lamp and the HUD |
| `spark_sheet.png` | 4 x 64 x 64 | 4 | spark burst on a strike and at a beam stop on a rock |
| `flicker_sheet.png` | 4 x 128 x 128 | 4 | white crackle loop on a contested crystal |
| `hud_frame.png` | 380 x 220 | 1 | panel frame, white border tinted per player |
| `zone_tile.png` | 128 x 128 | 1 | plain white square, tinted per player, alpha 0.18 |

Beams are not sprites: `Line` entities, width `max(4, cell/8)`, player colour, alpha 0.85, a 2x wider line of
alpha 0.25 under it for glow. Each player has a pool of 4*N lines (one per segment, segment = straight run
between turning points); unused lines alpha 0. To keep crossing beams readable, each player's beam is offset
perpendicular to its direction by `(seat - 1.5) * width * 0.7` px.

### 12.5 HUD panel content
Avatar 100 x 100 (`player.getAvatarToken()`), nickname (font 30, player colour), score (font 48), "+gain" of the
last turn (font 28, green-white, fades out), mirrors left as `mirror icon x 14`, crystals lit alone this turn.
Deactivated player: panel alpha 0.4, name struck through.

### 12.6 Animations per frame (frame duration 600 ms)
- t 0.0-0.3: new mirrors scale 0 -> 1 (`Curve.EASE_OUT`); turned mirrors rotate 90 degrees.
- t 0.3: beams snap to the new paths (set at 0.3, no tween; tweening paths misleads).
- t 0.3-0.6: newly lit crystals tint to owner colour and pulse scale 1 -> 1.15 -> 1; contested ones show the
  flicker loop; strike spark at the struck lamp; HUD scores count up.
- Messages: speech text above the player's lamp for the frame.

### 12.7 Tooltips (TooltipModule, per cell entity)
`(x, y)`, type, crystal value, lit by (player names or "contested"), mirror type and lock, last touched by,
zone owner if any.

### 12.8 End screen
EndScreenModule with scores; deactivated players shown with "eliminated".

## 13. Referee implementation notes
- Module list: GraphicEntityModule, TooltipModule, EndScreenModule; `MultiplayerGameManager<Player>`;
  `gameManager.setMaxTurns(MAX_TURNS)`, `setFirstTurnMaxTime(1000)`, `setTurnMaxTime(50)`, league via
  `gameManager.getLeagueLevel()` (1 = Wood 2, 2 = Wood 1, 3 = Bronze).
- config.ini: `min_players=2`, `max_players=4`, type multi.
- Tests (TDD): rotation symmetry of generated maps for 200 seeds (every object at `p` has its image at `R(p)`
  with mirror type swapped); lamp lines empty; trace table for the 8 mirror reflections; collision and merge
  rules; lock count 3-2-1-0; contested crystal; strike; deactivation; 3p empty seat; seed reproducibility.

## 14. Left out (by value)
- Splitting prisms, coloured filters, movable lamps: deeper but harder to read; candidates for a later league.
- Pre-made art, Boss code, stub file: specified here, not produced.
- Exact pixel art direction beyond names, sizes and colours.
- Measured balance (metrics in 9 need the referee first).

---

# v2 changes (2026-11-11, after the panel vote, see ../VOTE.md)

v2 overrides v1 wherever they differ. v1 text above is kept for history.

## V2.1 Problem
vote-hardness: "1-ply greedy decent, ~700 actions, perfect information". In v1 a mirror acts at once, a crystal pays
the same every turn, and a 20-mirror stock with 3-turn locks makes mistakes cheap. So "pick the action that
maximises this turn's score" is close to optimal. v2 changes the rules so that the value of an action shows only
after rivals have reacted. **No random element is added**: the only hidden information is the rivals' choices.

## V2.2 Rule changes (Bronze and up unless said)

| # | Change | v1 | v2 |
|---|---|---|---|
| C1 | Build delay | a placed mirror acts the same turn | a placed mirror is **pending** for 2 turns: it occupies its cell but beams pass through it as if empty; it reflects from the beams of turn t+2 (placed at turn t) |
| C2 | Sealed type | type visible | a pending mirror's type is shown only to its owner (others read type -1) until it activates |
| C3 | Streak scoring (Wood 1 and up) | value per turn | a crystal lit alone by P on k consecutive turns (this one included) pays `value x min(k, 4)`; contested or not lit by P: P's streak on it resets to 0 |
| C4 | Scarcity | stock 20, LOCK 3 | `MIRROR_STOCK = 12`, `LOCK = 5` |
| C5 | Lock start | lock 3 when placed | lock 5 starts when the mirror activates (turn t+2) or is turned |

Unchanged: one action per turn, zones, collisions, TURN merge, strike +2, 150 turns, contest rule.
TURN acts at once (it is the fast reaction tool), only on an active mirror with lock 0.

### Why the greedy now loses
- C1 + C2: a greedy scores the board as it is; by the time its mirror acts (2 turns), rivals have seen the site,
  guessed its type and can contest the target crystals or turn a mirror upstream. A planner chooses sites that
  rivals cannot cheaply answer within 2 turns and guesses rivals' pending types from where they help them.
- C3: the value of a crystal depends on holding it 4+ turns. Greedy grabs the richest lane now, which is the lane
  rivals contest first; a 4x streak lost by one contest costs 3x the value. Holding quiet lanes and breaking a
  rival's long streak beat chasing this turn's maximum.
- C4 + C5: 12 mirrors for 150 turns and 5-turn locks make every commitment long; greedy spends its stock early on
  small gains and then cannot answer.
- Emergent, not written in the statement: decoy builds (a pending mirror as a bluff costs a real mirror), lanes that
  run behind a rival's zone, timing a flip so the rival's lock is still running.

### Exploit check of v2
| Strategy | Outcome |
|---|---|
| Build many decoys | each decoy costs 1 of 12 mirrors; affordable twice at most |
| Hide a long lane far from others | rivals see pending sites and a lit crystal pays only after streak builds: 1-3 turns of warning before 4x |
| Jam every high streak | jammer's own beam goes there, so it scores 0 on that crystal too and leaves its own lanes |
| Flip a mirror the turn it activates | it has lock 5 at activation: TURN refused |
| Place on a pending cell | refused (cell occupied) |

## V2.3 Protocol changes
Per-turn input, after the player lines:
```
Next line: mirrorCount
Next mirrorCount lines: x y type lock pending
   type: 0 or 1; -1 if the mirror is pending and not yours
   lock: turns before it can be turned (0 = free; pending mirrors show 0)
   pending: turns before it starts to reflect (0 = active, 1 or 2 = pending)
Next line: crystalCount   (all crystals, sorted by (y, x))
Next crystalCount lines: x y litBy streak
   litBy: id of the player who lit it alone last turn, -1 unlit, -2 contested
   streak: that player's current streak on it (0 if litBy < 0)
```
Output unchanged. In Wood 2 / Wood 1 `pending` is always 0 and `type` never -1.

## V2.4 Resolution order (replaces section 6 steps 3-7)
1. Read, 2. Validate (TURN needs an **active** mirror with lock 0; PLACE needs an Empty cell, pending mirrors are not Empty).
3. Places: as v1; the new mirror gets `pending = 1`, `lock = 0`.
4. Turns: as v1 (active mirrors only), `lock = LOCK`.
5. Beams: trace with active mirrors only (pending cells act as Empty).
6. Score with streaks (C3), strike.
7. Pending and locks: a mirror placed this turn is not touched. Every other mirror with `pending = 1` becomes
   active: `pending = 0`, `lock = LOCK`, type public. Every active mirror not turned or activated this turn with
   `lock > 0` does `lock -= 1`.
8. End check.

Timeline of a mirror placed at turn t: input t+1 shows `pending 1`; it activates at the end of t+1; the beams of
t+2 use it; inputs t+2..t+6 show lock 5..1; first TURN allowed at turn t+7.
Statement wording: "a mirror starts reflecting 2 turns after it is placed".

## V2.5 Leagues (replaces section 7 table)
| League | Rules |
|---|---|
| Wood 2 | as v1 Wood 2 (stock 12, no streak) |
| Wood 1 | + TURN, LOCK 5, walls, start mirrors, contest, streak (C3) |
| Bronze | + values 1-3, strike, build delay (C1), sealed type (C2) |
Bosses: Wood 1 = Greedy-self, Bronze = Greedy-full (v1 definitions, scoring this turn's gain with the v2 rules,
pending mirrors treated as Empty). They are the "obvious greedy" the test below must beat.

## V2.6 Hardness test (game-analyst, before the arena)
Bots: G = Greedy-full; G+ = Greedy-full whose eval adds streak value (`value x min(k+1,4)` for crystals it keeps);
P = planner: 4-turn search over own actions (beam width 20) with each rival modelled as G, pending rival types
taken as the type that helps that rival most.
Targets over 500 seeds, seats rotated, deterministic reruns:
- **M5 hardness target**: P first in 4p games (P vs 3 G): >= 75 %; P vs 3 G+: >= 55 % (25 % = chance).
- 1v1 P vs G: >= 85 % wins.
- Same tests on v1 rules as the baseline: v2 must raise the P-vs-G win rate by >= 15 points.
- Noise: same seed twice = identical replay; P's rank std over seeds reported.
- **Seat fairness target**: with 4 copies of the same bot (G and P), win rate per seat 25 % +/- 5 % over >= 400 games
  (ties counted as split wins).
If a target is missed, tune in this order and rerun: LOCK (4-6), streak cap (3-5), stock (10-14), delay (1-2).
These are design targets; nothing has been measured yet.

## V2.7 Worked example (streak + delay)
Turn 10, P0 places a mirror at (5,3) type 1 toward crystal (5,6) value 2. Rivals read `5 3 -1 0 1` at turn 11.
Turn 11: still pending, P0 beam straight. P1 guesses type 1 and places at (9,6) type 0, which will send its beam
west over (5,6) from turn 13.
Turn 12: P0's mirror is active: (5,6) lit by P0 alone, streak 1, P0 +2.
Turn 13: P1's mirror is active: (5,6) contested, both streaks 0, nobody scores.
Had P1 not answered, P0 would score 2, 4, 6, 8, 8 on turns 12-16 (28 points over 5 turns vs 10 under v1).

## V2.8 Colours and emblems (replaces 12.2; follows ../_art-common/STYLE.md)
| Id | Name | Hex | Dark | Emblem |
|---|---|---|---|---|
| 0 | Sky | `#56B4E9` | `#1F5F86` | ring |
| 1 | Amber | `#E69F00` | `#7A5300` | triangle |
| 2 | Jade | `#009E73` | `#004D38` | square with core |
| 3 | Orchid | `#CC79A7` | `#6E3A58` | 4-point star |
Neutrals, background and text colours: STYLE.md. Sprites `shape_diamond` -> `shape_star`. Beam offset and
emblems on lamps/HUD keep colour from being the only cue. New sprite needs: `mirror_pending.png` (128 px,
translucent outline of a mirror with no bar, for rivals' sealed builds; owner sees its bar at 40 % alpha),
streak pips (`streak_pip.png`, 24 px, 0-4 under a lit crystal, owner-tinted).

## V2.9 Seed mixing (replaces the first line of 3.3; proposed by cg-contrib-coder)
`java.util.Random(seed)` with small seeds (0..199) gives N = 17 for all of them (its first `nextInt(4)` is nearly
linear in small seeds). Use `Random rnd = new Random(mix(seed))` with the SplitMix64 finalizer:
```
long z = seed + 0x9E3779B97F4A7C15L;
z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
return z ^ (z >>> 31);
```
Test: seeds 0..199 give each N in {13, 15, 17, 19} between 30 and 70 times.

---

# v3 changes (answer to red team round 1, ../glintwatch/REDTEAM.md)

v3 overrides v2 and v1 where they differ. Still no RNG at play time: every new element is fixed by the seed and
shown to all players in advance.

## V3.1 Rule changes (Bronze and up unless said)

| # | Change | v2 | v3 |
|---|---|---|---|
| D1 | Mirror decay | mirrors stay forever | an active mirror shatters at the end of its 20th active turn (`MIRROR_LIFE = 20`); the cell becomes Empty |
| D2 | Mirror supply | stock 12 for the game | start with 4; +1 at the start of every turn whose number is a multiple of 4; hold at most 6 (the extra is lost) |
| D3 | Crystal cycle | always on | each crystal is **awake** when `(turn + phase) % 45 < 30`, else **dormant** (passes beams, scores nothing, streaks reset). `phase` is fixed per symmetric orbit by the seed and given in the init input |
| D4 | Contest freezes (Wood 1 and up) | a contest resets both streaks | a contest gives nobody points and **freezes** the streaks of the players lighting it; a streak resets only when that player's beam stops lighting the crystal or the crystal goes dormant |

Kept from v2: build delay (placed at t, reflects from t+2), sealed type while pending, streak `value x min(k, 4)`,
LOCK 5 from activation, TURN immediate, zones, collisions, strike +2, 150 turns.

### Generation addition (step 7b of 3.3, after the start mirrors)
For each crystal of Q0 in list order: `phase = rnd.nextInt(45)`; its 3 rotated copies get the same phase.
Centre crystal: phase 0. Start mirrors are created already active with `life = 20`.

## V3.2 Answers to the red team

| RT point | v3 answer |
|---|---|
| 1 Quasi-static board, mostly WAIT | D1 + D2: every mirror dies after 20 active turns and a new one arrives every 4 turns, so each player places ~37 mirrors per game and must rebuild lanes; D3 moves the value around the board on a known schedule, so lookahead pays (a lane built now must catch crystals that wake in 2-5 turns). Target: non-WAIT share >= 40 % for the planner. |
| 2 Opening dominates | an opening lane lives <= 20 turns (D1) and its crystals sleep 15 of every 45 turns (D3); the 1 s first-turn search solves only the first ~20 turns |
| 3 Cutter | the cut mirror is visible for 2 turns before it acts and its type is hidden: the victim can re-route first, so the cut lands on nothing unless the cutter predicted the next lane; the cut lasts <= 20 turns; supply 1 per 4 turns limits cutting to ~1 rival lane at a time |
| 4 Spoiler asymmetry | D4: a contest no longer destroys the leader's streak, it only costs the current turn; the spoiler's beam scores nothing on that crystal either |
| 5 Circular test | V3.4 adds Cutter, Spoiler, FreeRider and an agent-written bot to the pool |

## V3.3 Protocol changes
Init input, after the map rows:
```
Next line: crystalCount
Next crystalCount lines: x y value phase      (sorted by (y, x))
```
Per-turn input (replaces V2.3 mirror and crystal blocks):
```
Next playerCount lines: score mirrorsInHand active
Next line: mirrorCount
Next mirrorCount lines: x y type lock pending life      (life: active turns left before it shatters; pending mirrors show 20)
Next line: crystalCount
Next crystalCount lines: x y awake litBy streak         (awake: 1 or 0; litBy and streak as V2.3, after this rule's freeze)
```
Turn numbers start at 1; the input gives no turn number line: add `Line 1 of each turn: turn` before the player lines.

## V3.4 Resolution order (replaces V2.4 step 7 and adds a step 0)
0. Supply: if `turn % 4 == 0`, every active player gets +1 mirror (max 6).
1-6. As V2.4 (beams use active mirrors; dormant crystals score nothing).
7. Pending and locks as V2.4; new active mirrors get `life = 20`.
7b. Decay: every active mirror not activated this turn does `life -= 1`; at 0 it is removed (after this turn's beams).
8. End check.

## V3.5 Hardness test (replaces V2.6 targets; seat fairness target unchanged)
Pool: G (Greedy-full), G+ (streak-aware), Cutter (each turn place on the first non-zone cell of the top rival's
lane, else greedy), Spoiler (aim own beam at the leader's highest-streak crystal), FreeRider (only edits its own
zone, riding rivals' mirrors), A (agent-written bot from the statement only, 1 h, M5).
Targets over >= 400 seeds, seats rotated:
- P first vs 3 copies of each pool bot: >= 75 % vs G, >= 55 % vs each of G+, Cutter, Spoiler, FreeRider, A.
- P first in mixed tables (P + 3 different pool bots, all combinations): >= 50 %.
- P non-WAIT turn share >= 40 %; mean board changes (placements + flips + shatters) >= 1.5 per turn.
- No pool bot beats every other pool bot (no single dominant cheap rule): checked by a round robin.
Tuning order if missed: MIRROR_LIFE (15-25), supply period (3-5), cycle (45/30), LOCK (4-6).

## V3.6 Viewer: UX overrides (from cg-contrib-ux, accepted)
Section 12 is superseded by ux/UX.md where they differ: colours/emblems per V2.8 (STYLE.md), background
`#1A1D29` -> `#0E1018`, HUD panels 380 x 300, turn box and legend in the side middles, no text above the board,
messages shown in the player panel (not over the lamp), floating +N only in the HUD and on strikes. Frame 600 ms.
v3 adds what the viewer must show: mirror life (20 -> 0), crystal awake/dormant state and turns to the next
change, streak 1-4 and frozen streaks under contest, mirrors in hand (0-6).

## V3.7 Red team round 2 fixes
- **D5 Streak decay (Wood 1 and up; replaces the reset part of D4)**: on each turn where a crystal is awake and
  player P's beam does not light it, P's streak on it drops by 1 (min 0) instead of resetting. A dormant crystal
  still resets every streak to 0. A contest still freezes. So a cut costs the victim 1 step per turn, and a
  re-route that relights within 1-2 turns keeps most of the streak.
  Example: streak 4, cut on turns 20-21 (4 -> 3 -> 2), relit turn 22 -> 3 (pays x3), turn 23 -> 4.
- Streaks are now per player and per crystal (several players can hold a decaying streak on one crystal).
  Per-turn crystal line (replaces V3.3): `x y awake litBy s_0 .. s_{playerCount-1}` (s_i = player i's streak).
- **Supply check (RT point 3)**: intended. Supply 1 per 4 turns with life 20 (+1 pending turn) gives ~5 live
  mirrors per player in steady state; hoarding up to 6 allows a burst of at most ~11 live mirrors at the cost of
  idle turns before it.
- **Test (replaces the board-changes gate of V3.5)**: add **Renewer** (rebuilds the same lane after each shatter,
  greedy otherwise) to the pool; P first >= 55 % vs 3 Renewer. Mixed-table round robin: no cheap pool bot
  (Cutter, Renewer, Spoiler, FreeRider, G, G+) first in >= 40 % of mixed tables. Board changes per turn and
  non-WAIT share are reported, not gated.

## V3.8 Clarifications (coder questions)
- Zones exist only for seats that hold a player (active or deactivated). Zones of empty seats (2p: seats 1, 3;
  3p: seat 3) are ordinary cells: anyone may PLACE or TURN there. Generation still keeps them clear (3.3).
- Greedy bots under build delay: G and G+ score their own candidate mirror as if it were active now; their own
  pending mirrors count as active (type known); rivals' pending mirrors count as Empty.
- Crystal values: 1 in Wood 2 and Wood 1; values 1-3 from Bronze (as the v1 league table).

## V3.9 Red-team verdict: HOLDS (round 3, analysis only) and M5 revert conditions
Gate passed on analysis; all numbers are targets until M5 measures them. M5 must also check:
- the agent-written bot A is the real test: **BROKEN again if A ties P** (A first-place rate within 5 points of P's);
- P's edge must not come only from the 1 s first-turn plan: run P with its first-turn plan disabled
  (1-turn greedy opening) and report the drop;
- **BROKEN again if any cheap bot (Cutter, Renewer, G+, Spoiler) is first in >= 40 % of mixed tables.**
- **M5 outcome (2026-11-11 14:20Z)**: vote-hardness ruled BROKEN on the agent criterion. A statement-only bot A (about 30 min) is within
  3.0-7.5 points of P (2A+2P gap 3.0; 1v1 A 46.3 %); 10x compute on P adds +3 points (M5-LOG.md iter 4). Cheap bots
  stay <= 3.9 % with P present (the cheap-bot condition holds). **Owner decision: accept Glintwatch as is, no rule change;
  the red team's fix directions are not applied.** Known limit, recorded: the game resists brute force but a
  competent coding agent reaches near-top play quickly.

---

# V5 best-practice changes (draft 2026-11-11 16:34Z before QA's table; owner order "follow every CG best practice")

Scope: **protocol and bosses only; game rules unchanged** (F1-F8 hold). Where a change touches gameplay it says so.
To be aligned with qa/BEST-PRACTICES.md when it lands.

## V5.1 Protocol (replaces F5; no gameplay change)
Targets: each player sees itself as id 0 with its own data first; <= 2 loops in the stub; <= 10 variables per read;
generated Java stub <= 45 lines; every non-obvious variable described in the stub INPUT section.

**Relative ids.** In every input, you are player `0`; the others are `1`, `2`, `3` in clockwise seat order starting
after you (seat order 0 -> 1 -> 2 -> 3 -> 0, skipping empty seats). Map letters follow the same ids: `A` is your lamp.
The referee maps relative ids back to real ids; summaries and the viewer keep real names.

**Initialization input** (no loop):
```
Line 1: playerCount size
Line 2: reef — a string of size*size characters, row by row from y = 0 (cell (x, y) is reef[y*size + x]):
        '.' empty, '#' rock, '1'-'3' crystal of that value, 'A'-'D' lamp of player 0-3 (A = you)
```
Lamps and their directions come from the map: a lamp on the left edge (x = 0) fires E, top edge S, right edge W,
bottom edge N (corners cannot hold a lamp). The old per-player lamp lines and the crystal list are removed.

**Input for one game turn** (2 loops):
```
Line 1: turn score0 mirrors0 score1 mirrors1 score2 mirrors2 score3 mirrors3      (9 integers)
        scoreI / mirrorsI: score and mirrors in hand of player I (0 = you).
        Eliminated player: score -1, mirrors 0. Absent player (I >= playerCount): -1 -1.
Line 2: mirrorCount
Next mirrorCount lines: x y type lock pending life                                 (6 integers, as F5)
Next line: crystalCount
Next crystalCount lines: x y awake change litBy streak0 streak1 streak2 streak3   (9 integers)
        awake 1/0; change = turns until awake flips (-1 = never: below the top league);
        litBy = relative id of the player who lit it alone last turn, -1 nobody, -2 several;
        streakI = player I's streak (0 for absent players).
```
`change` replaces the init `phase` with the same information (period 45, 30 awake: known from the statement).
Fixed 4 player slots replace the variable-length crystal line (QA BUG-1 workaround `crystalLine:string(40)` is
no longer needed).

**Output**: unchanged.

**Stub** (2 loops, max 9 variables per read):
```
read playerCount:int size:int
read reef:string(361)
gameloop
read turn:int score0:int mirrors0:int score1:int mirrors1:int score2:int mirrors2:int score3:int mirrors3:int
read mirrorCount:int
loop mirrorCount read x:int y:int mirrorType:int lock:int pending:int life:int
read crystalCount:int
loop crystalCount read cx:int cy:int awake:int change:int litBy:int streak0:int streak1:int streak2:int streak3:int
write WAIT
```
INPUT descriptions: one line per variable (reef indexing, ids relative, -1 meanings, change, litBy codes).

## V5.2 Limits check (no change needed)
| Practice | Glintwatch | OK |
|---|---|---|
| entities per turn <= 200 | mirrors <= ~64 (Wood, no wear) or ~60 (top) + crystals <= 61: <= ~125 | yes |
| rounds <= 250 | 150 (100 in the first league) | yes |
| time per player <= 25 s | 1 s + 149 x 50 ms = 8.45 s | yes |

## V5.3 Bosses (gameplay-neutral; the ladder is a design target)
Best practice: each boss is easy but beating it needs the league's new rules. Current ladder: WAIT, Greedy-self,
G2, Planner. Conflict: the Planner (top league) beats G2 100 % and is not "easy"; my own ladder target (each boss
beats the previous >= 80 %) pushed the bosses up. Proposal, for the owner (I do not loosen the 80 % myself):
- level1 WAIT (beat it by placing any useful mirror) — keep.
- level2 Greedy-self (ignores contest and locks: beat it by contesting or holding streaks) — keep.
- level3 G (Greedy-full, 1-ply, pending mirrors as Empty: beat it by planning for build time and wear) instead of G2.
- level4 G2 with tide-aware eval (skips crystals asleep at activation; no strike logic: beat it with tides and strikes)
  instead of the Planner.
- Ladder target becomes "each boss beats the previous one in >= 80 % **under its own league's rules**" —
  to re-measure by vote-analyst; if G vs Greedy-self stays at 64.8 % the two goals conflict and the owner chooses
  (easy bosses vs 80 % ladder).

## V5.1b Protocol, final (replaces V5.1 after QA's stub check: V5.1 gave a 59-line Java stub)
Measured with QA's generator (SDK 4.7.8 cginput.js, 12 languages generate): Java stub **44 lines (wc -l; 45 with the
last line)**, Python 34, 2 loops, max 5 variables per read. File: qa/stubcheck/stub-v5-designer.txt.
```
read reef:string(361)          (string first: no nextLine quirk in Java/C++)
read size:int
gameloop
read myMirrors:int score0:int score1:int score2:int score3:int
read mirrorCount:int
loop mirrorCount read x:int y:int mirrorType:int lock:int life:int
read crystalCount:int
loop crystalCount read tide:int litBy:int myStreak:int
```
Changes from V5.1, all **information-only** (every dropped value can be rebuilt by a bot from earlier turns; no rule
changes): playerCount (= lamps in reef), turn number (count turns), other players' mirrors in hand (start + supply -
placements), others' streaks (beams are computable), crystal x y (crystals listed in reef order), `pending` (folded
into `lock = -1`), `awake` + `change` (folded into signed `tide`; `0` = always awake below the top league).
Kept: scores of all players (shows eliminations and the leader).
Statement v5 (staged) Game input is updated to this format.

## V5.4 Coder task list (after QA alignment; numbered)
1. Referee: relative ids in all inputs (map letters, score slots, litBy, streaks); keep real ids in summaries/viewer.
2. Referee: init = `playerCount size` + one reef string; drop lamp lines and the crystal list.
3. Referee: turn line 1 = turn + 4 score/mirror slots; crystal lines = 9 integers with `change`; absent slots -1 / 0.
4. stub.txt per V5.1 with INPUT descriptions; check Java stub <= 45 lines and all 12 languages (qa/stubcheck).
5. Statement v4 Game input section rewritten per V5.1 (designer writes the text; generator unchanged).
6. Bosses and test bots (Greedy-self, G, G2, Planner, bot A) read the new protocol.
7. Bosses per V5.3b: vote-analyst builds and measures Holder / Builder / Tide-rider (>= 80 % vs the previous boss,
   200 seeds, ties half; else best simple bot + reported number); coder wires them as config/level2-4 bosses (level1
   stays WAIT) and removes the Planner from the bosses (it stays a test bot).
8. Tests: protocol golden files per level and player count; relative-id mapping (2p, 3p, 4p, every seat).

## V5.3b Bosses: owner decision (replaces the V5.3 proposal)
Owner: "easy bosses, search >= 80 %": both goals kept. The Planner leaves the top league.
- **Simple boss** = 1-ply over its own actions (at most a 2-turn own lookahead), no rollouts, no rival search,
  under ~200 lines, and its evaluation uses the league's new rule explicitly.
- **Target**: level L boss beats level L-1 boss in >= 80 % of 1v1 games, 200 seeds, ties half, under level L rules.
- **If no simple bot reaches 80 % for a level**, that level keeps the best simple bot found and its number is reported
  in F9 and to the owner (no silent loosening).
Candidates (vote-analyst builds and measures; free to find better):
| Level | New rules | Candidate | Uses the new rule by |
|---|---|---|---|
| 1 | place, shared scoring | WAIT | (baseline: any useful mirror beats it) |
| 2 | turn, locks, rocks, contest, streak | Holder: Greedy-self + rivals' current beams in the trace; values crystals lit alone x (streak+1); turns a free mirror that bends its lane away | contest and streaks |
| 3 | supply, build time, sealed type, wear | Builder: Holder scored at activation (t+2); rivals' pending mirrors assumed of the type that helps them most; replaces a lane mirror when its life <= 2 | build time and wear |
| 4 | tides, values 1-3, strike | Tide-rider: Builder whose crystal value = value x awake turns left in the next 5; adds +2 when its lane can end on a rival lamp | tides and strike |

## V5.5 Contribution Guidelines: statement wording and early end
- **Statement**: no flavour text (the story openers leave the statement; lore stays in the welcome popup and cover),
  no "you/your" (neutral wording: "each player", "the player"). League introduction keeps the CG alert sentence
  plus a one-line neutral list of the league's new rules. Full text: "Statement v5 (staged)" below; it replaces
  "Statement v4" when V5.1 ships (same tag rules; the generator reads `## Statement v4`, so rename the heading then).
- **Coordinates**: indices from 0, origin top-left (x column from the left, y row from the top): already true.
- **Early end (touches gameplay timing only; F6 step 8 gains two cases)**. At the end of a turn, the game also ends when:
  1. **Decided**: for every pair of active players, `|score_i - score_j| > R x B`, where R = turns left and
     B = most points one player can score in a turn: first league `sum of crystal values`; Wood 1 and Bronze
     `4 x sum of crystal values`; top league `4 x sum + STRIKE_BONUS`. (Pairs with an eliminated player are always
     decided.) Ranking: current scores. Ranks cannot differ from a full game: B is an upper bound.
  2. **Stale**: every active player has done nothing (WAIT or an impossible action) on each of the last 20 turns.
     The referee then plays the remaining turns itself with every player waiting (no more input sent; supply, wear,
     tides and scoring run as usual) and ranks by the resulting scores. Note: this changes play only for a player
     who would wait 20+ turns and act later while everyone else also waits.
  Constants: `STALE_TURNS = 20` (T).

## V5.6 Coder task list additions
9. Statement: swap in "Statement v5 (staged)" with V5.1 (rename its heading to `## Statement v4 ...` or update
   the generator regex); welcome popup keeps the lore; proofread by the designer.
10. Referee: early end per V5.5. Tests first: decided (2p and 4p, B per league, a tie keeps the game going), stale
    (20 all-WAIT turns -> fast-forward equals a full game where everyone waits; 19 turns -> no end), impossible
    actions count as doing nothing, an eliminated player does not block either rule.
11. Viewer/summary: one line saying why the game ended (turn limit, decided, stale, players left).

## Statement v5 (staged)
> ### League introduction
> This is a **league based** challenge. For this challenge, multiple leagues for the same game are available. Once
> a program has proven its worth against the Boss, it accesses the higher league.
>
> [Wood 2 only] **This league:** players place mirrors to bend their lamp's beam onto crystals.
>
> [Wood 1 only] **This league:** rocks, turning mirrors with locks, contested crystals and streaks.
>
> [Bronze only] **This league:** build time and wear.
>
> [Silver+] **This league:** crystal values from 1 to 3, and strikes on rival lamps.
>
> ### Goal
> Score more points than the other players by lighting crystals with the beam of the player's lamp.
>
> ### The reef
> The reef is represented as a square grid. The `x`-coordinate indicates the column (`0` at the left), and the `y`-coordinate indicates the row (`0` at the top/north). Its side is `size` cells.
>
> `2` to `4` players play. Each player owns a **lamp** on one edge. The reef looks the same from every lamp (90-degree rotational symmetry). With `2` players the lamps are on opposite edges; with `3` players one edge has no lamp.
>
> ![The reef: lamps, yards and axes](img/setup.png "Lamps on the edges, their 5 x 3 yards and the unobstructed straight path of each beam at the start; x grows to the east, y to the south.")
>
> ### Cells
> Each cell of the reef is one of:
> - `.` empty: a beam goes through;
> - `1` `2` `3` a crystal and its value: a beam goes through and **lights** the crystal;
> - `A` `B` `C` `D` a lamp: a beam that reaches it stops;
> - [Wood 1+] `#` rock: a beam stops.
> - [below Silver] Every crystal is worth `1`.
>
> ### Lamps and beams
> A lamp fires a straight-line beam towards the opposite edge.
> - Every lamp fires one beam each turn, after all actions of the turn.
> - The beam moves cell by cell until it leaves the reef, [Wood 1+] reaches a rock[/] or reaches a lamp.
> - Beams never block each other.
> - A beam cannot loop: it never enters the same cell twice in the same direction.
> - A beam that crosses the same crystal twice lights it once.
> - At the start, the straight line of each lamp holds no crystal, [Wood 1+] rock[/] or mirror: a beam must be bent to score.
>
> [Wood 2 only] ![Where a beam stops](img/stops_wood.png "A beam stops when it leaves the reef or reaches a lamp.")
>
> [Wood 1+] ![Where a beam stops](img/stops.png "A beam stops when it leaves the reef, reaches a rock or reaches a lamp.")
>
> ### Mirrors
> A mirror turns a beam by `90` degrees:
>
> | Mirror | Beam moving east | north | west | south |
> |---|---|---|---|---|
> | type `0` = `/` | leaves north | leaves east | leaves south | leaves west |
> | type `1` = `\` | leaves south | leaves west | leaves north | leaves east |
>
> ![How the two mirror types bend a beam](img/mirrors.png)
>
> Mirrors belong to nobody: a mirror bends **every** beam that reaches it.
> Players cannot remove mirrors. [Wood only] Mirrors stay until the end. [Bronze+] Mirrors wear out (see below).
>
> [Wood 1+] The reef starts with a few mirrors. They appear in the mirror list from turn `1`; the reef rows
> show their cells as `.`.
>
> ### Mirror supply
> - Each player starts with `12` mirrors available for placement.
> - At the start of every 10th turn, beginning with turn `10`, each player gets `1` more mirror. The additional mirror is already included in the turn's input.
> - The number of available mirrors is not capped.
> - A shattered mirror is permanently lost and is not returned to the player.
>
> ### Placement restrictions
> **Yards.**
> - Each lamp's yard is the `5` x `3` block of cells around it (the lamp cell, and every cell up to `2` cells away along the edge and up to `2` cells deep, diagonals included).
> - Only the yard's owner may place[Wood 1+] or turn[/] mirrors there; on every other cell any player may place[Wood 1+] or turn[/] mirrors (mirrors have no owner).
> - Beams cross yards freely.
> - With `3` players the edge without a lamp has no yard.
> - Lamps are not always at the middle of their edge: read their cells from the reef.
> - Every lamp is at least `2` cells from a corner, so its yard fits inside the reef.
>
> **Same cell.**
> - If two or more players place a mirror on the same cell in the same turn, all of them fail and keep their mirror.
> - [Wood 1+] If several players turn the same mirror in the same turn, it flips once.
>
> ### Actions
> Each turn, each player outputs **one** action:
> - `PLACE x y type`: put one of the player's available mirrors on an empty cell (see Mirror supply).
> - [Wood 1+] `TURN x y`: flip a mirror (`/` becomes `\` and back).
> - `WAIT`: do nothing.
>
> [Wood 1+] **Locks.** Each mirror has a `lock`. A mirror can be turned only when its lock is `0`. The lock is set
> to `2` when the mirror becomes active or is turned, then drops by `1` at the end of each following turn
> (so it reads `2` on the next turn's input). A `TURN` is allowed from the first turn whose input shows `lock` `0`:
> [Wood only] turn `t+3` for a mirror placed on turn `t`[/][Bronze+] turn `t+4` for a mirror placed on turn `t`[/].
> [Wood only] A mirror becomes active as soon as it is placed.
>
> [Wood 1+] ![Turning a mirror and its lock](img/lock.png "After a `TURN` the lock reads 2, then drops by 1 each turn; `TURN` works again at 0.")
>
> **Impossible actions** are replaced by `WAIT` (the reason is shown in the game summary):
> - a cell outside the reef
> - a type other than `0` or `1`
> - a cell that is not empty ([Wood 1+] rock, [/]crystal, lamp, mirror, [Bronze+] pending mirror)
> - a cell in another player's yard
> - no available mirror
> - a number out of range (negative or too large)
> - [Wood 1+] turning where there is no active mirror or the lock is not `0`
> - [Wood 2 only] `TURN` (not available in this league)
>
> **Invalid lines** eliminate the player:
> - an empty line
> - an unknown keyword (keywords are in capitals)
> - missing numbers
> - a number that is not a whole number
>
> Spaces at the start and end of the line are ignored.
>
> ### Scoring
> [Wood 2 only] Each turn, every player whose beam lights a crystal gains its value. Several beams on one crystal
> all score it.
>
> [Wood 2 only] ![Scoring in this league](img/shared_wood.png "One beam on a crystal: +1. Two beams on one crystal: +1 to each.")
>
> [Wood 1+] Each player has a **streak** (`0` to `4`) on each crystal. Each turn, for each crystal:
> - lit by **exactly one** player: that player's streak grows by `1` (max `4`), then the player gains
>   `value` times the new streak; every other player's streak drops by `1` (min `0`);
> - lit by **two or more** players: nobody gains; those players keep their streak; the others' drop by `1`;
> - lit by **nobody**: every streak drops by `1` (min `0`).
> A crystal kept alone pays `x1`, `x2`, `x3`, then `x4` every turn.
>
> [Wood 1+] ![A crystal lit by one beam scores; lit by two beams it scores nothing](img/contested.png)
>
> [Wood 1+] ![Streak badges x1 to x4 under a crystal](img/streak.png)
>
> ### [Bronze+] Build time and wear
> - **Build time.** A mirror placed on turn `t` is **pending** on turn `t+1` and bends beams from turn `t+2`.
>   While pending, beams go through its cell and nobody can place there or turn it. Its type is shown to every player.
>   A pending mirror shows `lock -1` and `life 20`.
> - **Wear.** `life` drops by `1` at the end of each turn on which the mirror bends beams; at `0` the mirror shatters and its cell becomes empty. Timeline of a mirror placed on turn `t` and never
>   turned: input `t+1` shows it pending with `life 20`; inputs `t+2` to `t+21` show `life` `20` to `1` and it bends beams
>   on those turns; it shatters at the end of turn `t+21`; input `t+22` lists it once with `life 0`. Mirrors on the
>   reef at the start show `life 20` on turn `1`.
>
> ![Build time of a mirror](img/build.png "Placed on turn t, pending on turn t+1, bends beams from turn t+2.")
>
> ![Wear of a mirror](img/wear.png "An active mirror bends beams for 20 turns, then shatters.")
>
> ### [Silver+] Values and strikes
> - **Values.** Crystals are worth `1`, `2` or `3`.
> - **Strike.** Each turn, a beam that ends on another player's lamp gives `4` points to the player who fired it.
>   Each such beam counts. The struck player loses nothing. An eliminated player's lamp is a rock and gives nothing.
>
> ![Crystal values from 1 to 3](img/values.png "Crystals worth 1, 2 or 3, by size.")
>
> ![Strike on a rival lamp](img/strike.png "A beam that ends on another player's lamp: 4 points each turn.")
>
> ### Order of a turn
> - Mirror supply (at the start of every 10th turn)
> - All placements[Wood 1+], then all turns[/].
> - Every lamp fires.
> - Scoring.
> - [Wood 1+] Locks drop by `1`, except those set on this turn.
> - [Bronze+] Pending mirrors become active, active mirrors wear.
>
> [Bronze+] ![Order of a turn](img/order.png "Supply, placements, turns, every lamp fires, scoring, locks drop, mirrors activate and wear.")
>
> [Wood 2 only] ![Order of a turn](img/order_wood.png "Supply, then placements, then every lamp fires, then scoring.")
>
> ### End of the game
> The game lasts `150` turns, or ends earlier when:
> - fewer than two players remain;
> - every remaining player has done nothing (`WAIT`, an impossible action, or a placement that failed on a shared
>   cell) for `20` turns in a row: the referee then plays the remaining turns itself with every player waiting (lamps still fire and score each turn), and the
>   final scores are the scores after the last turn.
>
> Equal scores share the rank. An eliminated player gets a score of `-1`. Its lamp stops firing and [Wood 2 only] still stops beams[/][Wood 1+] acts as a rock[/];
> its yard stays protected; its mirrors stay on the reef. The input shows it with `-1` mirrors.
>
> ### Victory conditions
> - Have the highest score when the game ends.
>
> ### Defeat conditions
> - Have a lower score than another player when the game ends.
> - The program does not answer in time, crashes, or outputs a line that is not a valid command (keyword and whole
>   numbers): the player is eliminated.
>
> ### Example (an example reef, not a real map)
> `size` = `13`. A lamp at (`0`,`3`) fires east; a crystal of value `1` is at (`5`,`6`). Its owner outputs `PLACE 5 3 1`.
> The beam goes (`1`,`3`) ... (`5`,`3`), turns south on `\`, crosses (`5`,`4`), (`5`,`5`), lights (`5`,`6`) and goes on south.
> [Wood 2 only] The player gains `1` every turn. [Wood 1+] If no other beam lights it, the player gains `1`, `2`, `3`,
> then `4` per turn.
> [Bronze+] The mirror starts bending beams `2` turns after it is placed.
>
> ### Game input
> **Player ids**
> - Players are numbered `0` to number of players - `1`. In the input, the receiving player is always player `0`; the other players follow in clockwise order of the lamps around the reef, starting after player `0`'s lamp. Unused ids show `-1` mirrors.
> - The lamp letters follow these ids: `A` is player `0`'s lamp. The reef and the coordinates are the same for every player (not rotated); only the lamp letters change.
>
> **Initialization input**
> - Line 1: an integer `playerCount`: the number of players (`2` to `4`), also the number of lamps on the reef.
> - Line 2: an integer `size`: the width and height of the reef.
> - Next `size` lines: a string `row` of `size` characters, the reef rows from `y = 0`: character `x` of row `y` is
>   cell (`x`, `y`). `.` empty, [Wood 1+] `#` rock,[/] `1` `2` `3` crystal of that value, `A` `B` `C` `D`
>   lamp of player `0` `1` `2` `3`.
>
> **Input for one game turn**
> A field that this league does not use has a fixed value, stated below.
> Turns are numbered from `1`; the input is not numbered.
> - Line 1: five space-separated integers `myScore` `mirrors0` `mirrors1` `mirrors2` `mirrors3`:
>   - `myScore`: the score of player `0`.
>   - `mirrors0` `mirrors1` `mirrors2` `mirrors3`: the mirrors that player `0`, `1`, `2`, `3` can still place (`-1` for an eliminated or missing player).
> - Line 2: an integer `mirrorCount`: the number of mirrors on the reef.
> - Next `mirrorCount` lines: five space-separated integers `x` `y` `mirrorType` `lock` `life`. Every listed mirror[Bronze+],
>   pending or active,[/] fills its cell: no mirror can be placed there[Bronze+], except a mirror with `life` `0`[/].
>   - `x` `y`: integers, the mirror's cell.
>   - `mirrorType`: an integer, `0` (`/`) or `1` (`\`). The type is always real, pending or not.
>   - `lock`: an integer, the number of turns before it can be turned; `0` when it can be turned. [Wood 2 only] It is always `0` in this league (no `TURN`).[/] [Bronze+] `-1` while it is pending, when it does not bend beams yet.
>   - `life`: an integer. [Wood only] Always `-1`: mirrors never wear in this league.[/] [Bronze+] Active turns left before it shatters, and `0` for a mirror that shattered at the end of the last turn (listed once; its cell is empty).
> - Next line: an integer `crystalCount`: the number of crystals.
> - Next `crystalCount` lines, one per crystal in reef order (by `y`, then `x`): three space-separated integers
>   `tide` `litBy` `myStreak`:
>   - `tide`: an integer, always `0` (unused; crystals are always awake).
>   - `litBy`: an integer, the id of the player who lit it alone on the last turn; `-1` nobody (and on turn `1`); `-2` several
>     players.
>   - `myStreak`: an integer, player `0`'s streak on it, `0` to `4`. [Wood 2 only] It is always `0` in this league (no streaks).[/]
>
> **Output for one game turn**
> - One line: one action, optionally followed by a space and a message:
>   - `PLACE x y type`: place a mirror on cell (`x`, `y`); `type` is `0` (`/`) or `1` (`\`).
>   - [Wood 1+] `TURN x y`: flip the mirror on cell (`x`, `y`).
>   - `WAIT`: do nothing.
> - Variables:
>   - `x` `y`: integers, the column and the row of the cell.
>   - `type`: an integer, `0` (`/`) or `1` (`\`).
>   - `message`: an optional string of up to `30` characters: every token after the numbers. A longer message is cut, not refused.
> - Tokens are separated by spaces; any other separator (a tab, for example) makes the line invalid.
> - Example: `PLACE 5 3 1 hello`
>
> **Constraints**
> - 2 ≤ number of players ≤ 4
> - 13 ≤ `size` ≤ 19, `size` is odd
> - Response time for the first turn ≤ 1000 ms
> - Response time for one turn ≤ 50 ms
>
> ### Technical details
> - Rows and lists are sorted by `y`, then `x`.
> - Derived information: the turn number is the count of turns read (the first is `1`); the other players' scores and
>   streaks follow from the beams, which can be traced each turn from the mirror list of the next turn.
> - The referee source code is on https://github.com/TidyMaze/glintwatch
> - Glintwatch is an original game; its idea comes from the genre of laser-and-mirror reflection puzzles.
>
> ### League preview
> [Wood 2 only] Rocks, turning mirrors with locks, and crystal streaks that pay more the longer a crystal is held alone.
>
> [Wood 1 only] Build time and wear.
>
> [Bronze only] Crystal values from 1 to 3, and strikes on rival lamps.

## V5.7 Coder task list, final (hand-over; aligned with qa/BEST-PRACTICES.md)
Test first for every referee change. Rules F1-F8 unchanged except the early end (V5.5).
1. Protocol V5.1c (V5.8): relative ids (you = 0, others clockwise; map letters, hand slots, litBy); init = reef string
   then size; turn = myScore + 4 hands (-1 eliminated/missing), shattered-mirror echo with life 0, mirrors `x y type lock life` (lock -1 = pending), crystals `tide litBy myStreak`
   in reef order. Golden-file tests per level and player count; relative ids for every seat in 2p, 3p, 4p.
1b. Tracker protocol test (V5.8): 200 seeds x 4 levels x 2/3/4 players x every seat, exact match every turn.
2. stub.txt = qa/stubcheck/stub-v5-designer.txt (every variable has an INPUT line); QA's stubcheck must show 12/12
   generate, 9/9 compile and read real inputs, Java <= 45 lines.
3. Statement: swap in "Statement v5 (staged)" (rename heading or regex), regenerate 4 levels; designer proofreads.
4. Early end (V5.5): decided and stale rules + the end reason in the summary/viewer.
5. Bosses (V5.3b): wire vote-analyst's simple bosses into config/level2-4 (level1 WAIT); Planner out of the bosses.
6. Bots used in tests and M5 (Greedy-self, G, G2, Planner, bot A, quick bot) read V5.1b.
7. Entity budget <= 200 (QA measured 1142): with cg-contrib-ux (reuse/pool sprites, no per-frame creation).
8. Fonts >= 36 px (UX item 14) and lossless PNG minification (QA pngopt: -47.9 %), with ux and artist.
9. Re-run QA's best-practice table; every row PASS or an owner-approved exception.

## V5.8 Dropped inputs must stay exactly derivable (lead's guard; amends V5.1b)
Check of each dropped field. One was NOT derivable and is restored; one edge case needed an extra echo.
| Dropped field | Exact rebuild from the inputs | Derivable |
|---|---|---|
| turn number | count game-loop inputs, the first is turn 1 | yes |
| playerCount | number of `A`-`D` letters in `reef` (an empty seat's lamp cell is `.`) | yes |
| rivals' mirrors in hand | **no**: the mirror list has no owner, so a new mirror cannot be credited to a rival | **no -> restored** (`mirrors1..3`) |
| rivals' scores | sum of each player's gains from the beams of every turn (below), strike included; eliminated = `mirrorsI = -1` | yes, with the echo below |
| rivals' streaks | same beams + F6 step 6 from 0 at turn 1; tides from `tide` | yes, with the echo below |
| crystal x y | crystals are listed in reef order (by y, then x): the k-th line is the k-th crystal character of `reef` | yes |

**Beams of turn t from inputs t and t+1** (M_t = mirror list of input t):
- First two leagues: every mirror of M_{t+1}, with its M_{t+1} type (placements and flips happen before the
  beams; nothing is removed after them).
- Bronze and up: every mirror of M_{t+1} that is active (`lock >= 0`) **and** was not pending in M_t, plus every
  entry of M_{t+1} with `life = 0`; types from M_{t+1}.
- Lamps firing at turn t: players whose `mirrorsI` is not `-1` in input t+1 (a player eliminated at turn t did not
  fire at t).
**Edge case found**: a mirror with `life 1` turned on its last turn bends beams flipped, then shatters; without
an echo its final type is unknown to the others. **Fix (information only): input t+1 also lists every mirror that
shattered at the end of turn t, once, with `life = 0`, its final type and `lock = 0`; its cell is empty** (it does
not fill its cell; it is not on the reef).

**Protocol V5.1c (replaces V5.1b line 1)**: `myScore mirrors0 mirrors1 mirrors2 mirrors3` (own score kept;
`mirrorsI = -1` for an eliminated or missing player; rivals' scores dropped as derivable). Mirror lines add the
`life = 0` echo. Java stub still 44 lines (wc -l), 12/12 generate (qa/stubcheck/stub-v5-designer.txt).

**Protocol test (coder task 1b)**: a reference Tracker (test code) reads only the V5.1c inputs of one player and
rebuilds turn, playerCount, every player's score and streak per crystal, and crystal x y; on 200 seeds x levels 1-4 x
2/3/4 players x every seat, after every turn, they must equal the referee's state exactly. A mismatch = the field
goes back into the input.
Statement: a "Derived information" line in Technical details says the turn number, the player count, the other
players' scores and streaks can be computed from the input, and how (one sentence each).

## V5.9 Neutral welcome popups (QA flag; owner: every point followed)
config/levelN/welcome_en.html: what the game is + what this league adds; no story, no "you"; lore stays only in the
cover tagline and the public repo's story/ files. Same markup as today (keyword/const tags). Texts:
- **level1** — title "Glintwatch". "Each player owns a lamp on one edge of a square reef; every turn each lamp fires
  a beam. Mirrors bend every beam by 90 degrees and belong to nobody." List: "Each turn, each player prints one
  action: `PLACE x y type` (type 0 = /, 1 = \) or `WAIT`." / "Each turn, every crystal lit by a player's beam gives
  that player 1 point." / "Only a lamp's owner may place mirrors in its yard (the cells near the lamp)." / "Each
  player has 12 mirrors. The game lasts 100 turns; the highest score wins." / "Response time: 1000 ms on the first
  turn, 50 ms after."
- **level2** — title "New rules in this league". "Rocks stop beams." / "A crystal pays only a player whose beam
  lights it alone; held turn after turn it pays x1, x2, x3, then x4." / "New action `TURN x y` flips a mirror; a
  mirror placed or turned stays locked for 5 turns." / "Games last 150 turns." / "See the statement for details."
- **level3** — title "New rules in this league". "Each player starts with 4 mirrors, gets 1 more every 4 turns
  and holds at most 6." / "A placed mirror bends beams 2 turns later; until then the other players do not see its
  type." / "A mirror shatters after bending beams for 20 turns." / "See the statement for details."
- **level4** — title "New rules in this league". "Crystals are awake 30 turns out of every 45; an asleep crystal
  gives nothing and resets its streaks." / "Crystals are worth 1, 2 or 3." / "A beam that ends on another player's
  lamp gives 2 points per turn to the player who fired it." / "See the statement for details."

Early end check (QA fail 3: 100/100 all-WAIT turns in level1 IDE games, stub vs WAIT boss): the V5.5 stale rule
ends such a game after turn 20 (fast-forward of the remaining turns). Test: level1, stub vs boss, ends at turn 20 with
the fast-forwarded scores; a bot that places one mirror on turn 25 is cut only if both waited turns 1-20.

## V5.7 additions
10. Welcome popups per V5.9 (replace config/level1-4/welcome_en.html; coder, or cg-contrib-writer if it owns them).
11. Stale-rule test from V5.9 (level1 stub vs WAIT boss ends at turn 20).

## V5.3c Boss ladder after vote-analyst iteration 5 (M5-LOG.md)
Finding: with build delay, a placed mirror pays only from t+2, so 1- and 2-turn own lookahead scores 0 % in level3+
(own-path rollout h=1, h=2: 0.0 % vs self). "Simple" (V5.3b) is amended to: **own actions only, horizon <= 3 turns,
no rival modelling, about 100 lines or less**. Not a loosened threshold: the 80 % target is unchanged.
| Level | Boss | Lines | vs previous boss (1v1 x200, ties half) |
|---|---|---|---|
| 1 | Wanderer (owner decision for v14, spec below) | ~20 | greedy-self must beat it >= 80 % |
| 2 | Greedy-self | ~60 | 100.0 % vs WAIT |
| 3 | G2 (G + 2-turn own search) | ~100 | 87.8 % vs Greedy-self |
| 4 | all-lane planner h=3 (shipped as "planner:own:h=3": the own flag is not implemented, so it is the all-lane planner) | ~80 | 100 % vs G2 (1v1); 98.5 % first vs 3 G2 (4p) |

**Correction (vote-analyst, M5-LOG.md "CORRECTION")**: the iteration-5 "planner:own:h=3" runs lacked the own-lane
flag, so they measured the all-lane planner. The real own-lane planner h=3 scores 44.0 % vs G2 at level 4 (h=4 51.3,
h=6 52.0) and 52.3 % vs Greedy-self at level 3: it fails. Lead decision: v14 keeps the shipped all-lane planner h=3; the A/B measure is cancelled (candidate A stays in V5.3d for v15).
**Wired v14 bosses verified** (vote-analyst, bundled Boss.java of zip 1719, V5.1c, 1v1 x200, ties half): Boss4 vs
Boss3 at L4 100.0 %; Boss3 vs Boss2 at L3 87.8 %. For v15: candidate A (Boss2 all-lane w=1) vs Boss3 99.5 %, CPU at
N=19 4p mean 0.51 / p99 2.52 / max 3.28 ms; shipped planner mean 1.02 / p99 4.43 / max 5.95 ms. Ablation: it also wins 100 % with tides and strike ignored, so its win does not need level 4 rules
(reported, as in V5.3d). Check: the wired v14 level 4 boss must be this all-lane version (coder) and be re-measured
vs the wired level 3 boss on V5.1c (vote-analyst).
**Level 1 boss "Wanderer" (v14; owner change: it aims at a crystal; replaces the earlier lamp-line rule).**
League 1 rules only (PLACE, WAIT); no random numbers (deterministic per seed); about 25 lines.
- It counts turns itself (t = 1, 2, ...). Only on turns with `t % 5 == 0` and while it has mirrors in hand:
  trace its own beam on the current reef (active mirrors from the input). Walk that path from the lamp; for each
  empty cell not in another player's yard, try type 0 then type 1: re-trace its own beam with that mirror added.
  The first (cell, type) whose new beam lights at least one crystal with `litBy = -1` (nobody lit it alone last
  turn) is played: `PLACE x y type`.
- Otherwise (other turns, no such cell, no mirror) `WAIT`. No search beyond that one move, no look at rivals' beams.
- Easy by design: it acts 1 turn in 5, takes the first hit (not the best), and stops at 12 mirrors.
- Targets: greedy-self beats it >= 80 % (1v1, 200 seeds, ties half; league 2 rules as for every ladder step, league
  1 rules reported too); basic_bot.py vs it under league 1 rules (it should win most).
- **Measured (vote-analyst iter 7, period 5)**: greedy-self 98.0 / 95.0 % (L2), 97.5 / 96.5 % (L1): pass. basic_bot.py
  (ported to V5.1c in scratch) only 43.5 / 44.5 % at L1: **fails "a first bot beats it most games"**. The shipped
  bots/basic_bot.py still reads the old init and crashes 200/200 on V5.1c (port needed).
- **Fix (tunable, aiming unchanged; lead target)**: period P (`t % P == 0`) and a mirror cap C (it stops after C
  placements). Grid: P in {8, 10} x C in {4, 12}. Target: basic_bot.py (V5 port) >= 75 % at L1 on both seed sets,
  and greedy-self >= 80 % at L2. Pick (lead): the smallest P (8, 10, then 15) that passes with C = 12; if none,
  the same with C = 4. If none passes, the table goes to the lead/owner. basic_bot.py V5 port: owned by botA-writer. If greedy-self < 80 %, the number and options go to the owner (no loosening).
The all-lane Planner (h=8) leaves the bosses (test bot only). Open before it is final: the level4 boss must need
the level4 rules: ablation (same boss with tides and strike ignored) vs G2; if the ablated bot also beats G2 >= 80 %,
the level4 boss does not depend on its league's rules and needs a tide/strike term. Also its max time per turn.
Not reachable as a ladder: own h=3 at level3 (99.8 %) leaves nothing simple >= 80 % at level4 (best 65.5 %).

## V5.3d Boss ladder, PLANNED FOR v15 (vote-analyst iteration 6, M5-LOG.md)
**Status (lead decision): v14 ships the V5.3c bosses (already wired, ported to V5.1c, tested on both seats). V5.3d
goes into v15 only after: (1) port of the Boss2 chain to V5.1c, (2) 4p tables measured like V5.3c's, (3) level4 boss
timing. The coder does not wire V5.3d before that.**
All bosses: Boss2.java (132 lines), 1-ply + best second own action, no rollout, no rival modelling.
| Level | Boss | vs previous (1v1 x200, seeds 1000 / 3000, ties half) | League rule it uses |
|---|---|---|---|
| 1 | WAIT | - | - |
| 2 | Greedy-self | 100 % vs WAIT | TURN (its candidates include turning mirrors on its lane) |
| 3 | Builder `b2:rw=2` | 91.3 / 88.3 % vs Greedy-self | build time (scores a mirror at activation, t+2) |
| 4 | `b2:rw=1:all:w=1` (candidates on every beam path) | 100 / 100 % vs Builder | values 1-3 and strike through its true-rule 1-ply value; no tide-specific term |
Choice: Greedy-self at level 2 (every step >= 80 %) over Holder (contest/streak-aware, but the next step falls to
77 %): the owner's 80 % target wins over "uses contest and streaks". Reported, not hidden:
- Level 2 boss ignores contest and streak (it uses TURN only).
- Level 4 boss does not depend on tides: a tide term gave no gain; what separates bots is interfering with other
  lanes (all-lane candidates beat every own-lane bot 98-100 %).
Open: level4 boss max/p99 ms per turn at N=19, 4p.

## V5.10 Fresh-reader answers (qa/FRESH-READER-v14.md; text only, no rule change; checked against the referee code)
| Q | Answer added to Statement v5 (referee source checked) |
|---|---|
| 1 ids | clockwise = left, top, right, bottom edge after player 0; 2p other = 1; 3p = 1, 2; unused ids -1 mirrors (Protocol.relativeOrder) |
| 2 invalid | impossible list adds out-of-range numbers and, first league, TURN; new "Invalid lines" paragraph: empty, unknown or lower-case keyword, missing or non-integer numbers eliminate; outer spaces ignored; extra tokens = message, cut at 30 (CommandParser) |
| 3 stale | the remaining turns are played (lamps fire and score); final = scores after the last turn (Game.earlyEndCheck) |
| 4 tide | tide describes this turn: k > 0 awake now and k-1 more, -k asleep now and k-1 more; never 0 in the top league; 1 is followed by -15 (Protocol.tide) |
| 5, 15 life | timeline t+1 pending life 20, t+2..t+21 life 20..1 bending, shatters end of t+21, echo life 0 at t+22; life does not drop on a turn the mirror is turned; start mirrors life 20 on turn 1 (Game.advanceMirrors) |
| 6 | a shattered mirror does not return; the 6 cap counts the hand only |
| 7 lock | order list "except those set on this turn"; first TURN t+6 (first two leagues) / t+7 (Bronze+) |
| 8 bound | "remaining turns x (4 x total value + 2)": +2 per turn (Game.maxGainPerTurn) |
| 9 strike | each beam counts; struck player loses nothing; an eliminated lamp gives nothing (BeamTracer) |
| 10, 16 | any player may place/turn outside other players' yards; mirrors have no owner; 3p empty edge has no yard; read lamp cells from the reef |
| 11 loop | 90-degree turns: a beam never returns to a cell in the same direction |
| 12 eliminated | lamp stops firing, acts as a rock; its mirrors stay; input shows -1 mirrors |
| 13 | gain uses the new streak |
| 14 | already stated (league change of scoring); no text change |
| 17 | litBy also reports beams on an asleep crystal (Game scoring loop) |
Part 2 check: the L1 summary "place mirrors in own yard only" was wrong (the referee allows any cell outside other
players' yards): the yards paragraph now says so. The other three summaries match the referee.
Not a statement issue but worth knowing (spec F6 step 7, implemented): a turned mirror skips its wear that turn.

## V5.3e Wanderer values: SETTLED P = 20, C = 12 (lead pick; shared bots/basic_bot.py: basic 77.0 / 81.0 %, greedy-self
100 / 99 %, 3.9 placements per game, first at turn 20; table below). Iter 9 numbers, for reference:
basic_bot (V5.1c port) first % at level 1, by period P and cap C: P=8: 56-64; P=10: 58.5-68; P=15: 65-76.5 (fails 75
on seeds 1000); **P=20: C=12 77 | 81, C=4 77 | 81 (pass)**; P=25/30: 96 (Wanderer inert). Greedy-self at level 2: 96-100 %
everywhere. (Iter 9 rule would give P = 20, C = 12; superseded by the re-run.)
Side effect, reported: level 1 games end around turn 55 (early end), so Wanderer places about 2 mirrors per game
(turns 20 and 40): the replay is less "alive" than the owner hoped. A first placement earlier (e.g. turn 5, then every
20) was not measured.
**Re-run on the shared bots/basic_bot.py** (vote-analyst; basic first % seeds 1000 | 3000; greedy-self at L2;
placements per game; first placement turn):
| Cell | basic | greedy-self | placements | first | Result |
|---|---|---|---|---|---|
| P=15 C=12 | 72.0 / 76.0 | 99 / 97.5 | 5.5 | t15 | fail (seeds 1000) |
| P=20 C=12 | 77.0 / 81.0 | 100 / 99 | 3.9 | t20 | pass |
| P=5 C=1 | 97.0 / 96.0 | 100 / 100 | 1.0 | t5 | pass |
| P=5 C=2 | 58.5 / 63.5 | - | - | - | fail |
| P=8 C=1 | 96.5 / 96.0 | 100 / 100 | 1.0 | t8 | pass |
| P=8 C=2 | 57.5 / 64.0 | - | - | - | fail |
| t5 then every 20, C=12 | 72.0 / 73.5 | 99 / 98 | 4.7 | t5 | fail |
Designer recommendation (lead picks the most visible passing cell): **P=20 C=12** (3.9 placements per game, vs 1.0).

## V6 simplification (owner, 2026-11-11 21:03Z)

Owner decision: remove the mirror supply and the tides; keep the 4 leagues; the same input in every league.
Written by the game designer (Haiku). Older sections are kept as history; where V6 differs, V6 wins.

**What changes**
1. Mirror supply removed. Every player starts with 12 mirrors in every league. No "+1 every 4 turns", no hold cap of 6,
   no supply step in the order of a turn. A shattered mirror still does not come back to any hand.
2. Tides removed. Crystals are always awake in every league. Crystal values 1 to 3 and the strike (+4) stay in the
   top league. No sleep state, so no streak reset by sleep.
3. Same input in every league. Every field is sent in every league. A field a league does not use has a fixed value
   (the statement says which, and until which league).
4. Unchanged: TURN and the lock in league 2 and up, solo scoring and streaks, rocks, the 2-turn build with hidden type,
   wear (20 turns), the values, the strike.
5. Turns: 150 in every league (league 1 was 100).
6. Lock after a TURN or a placement: 2 turns (was 5), the same as the 2-turn build. First legal TURN: t+3 in the Wood
   leagues, t+4 from Bronze (was t+6 and t+7). The lock image is no longer referenced (it showed 5); it needs a redraw.
7. The "decided" early end is removed: the bound "turns left x (... + 3)" is gone from every league. Still in force:
   fewer than two players remain, and the stale end (20 turns where nobody acts, then the rest is auto-played with WAIT).

**Constant table (per league; the 4 leagues are kept)**

| Constant | Wood 2 (L1) | Wood 1 (L2) | Bronze (L3) | Silver (L4) |
|---|---|---|---|---|
| Mirrors at start | 12 | 12 | 12 (was 4) | 12 (was 4) |
| Supply (+1 every 4 turns) | none | none | none (removed) | none (removed) |
| Hand cap | none | none | none (removed; was 6) | none (removed; was 6) |
| Rocks | no | yes | yes | yes |
| TURN and lock | no | yes, lock 2, first TURN t+3 | yes, lock 2, first TURN t+4 | yes, lock 2, first TURN t+4 |
| Build time (pending) | no | no | 2 turns (bends from t+2) | 2 turns |
| Wear | no | no | 20 active turns | 20 active turns |
| Crystal awake | always | always | always | always (was 30 of 45) |
| Crystal values | 1 | 1 | 1 | 1 to 3 |
| Streaks (solo pay x1 to x4) | no (shared) | yes | yes | yes |
| Strike (+4 per beam on a rival lamp) | no | no | no | yes |
| Turns | 150 | 150 | 150 | 150 |

**Input (the same in every league; replaces the V5.1c tide and supply parts)**
- Line 1: `myScore mirrors0 mirrors1 mirrors2 mirrors3`. `mirrorsI` starts at 12; `-1` for an eliminated or missing player.
  No supply: the count only goes down (placements), and a placed mirror that shatters is not returned.
- Line 2: `mirrorCount`, then `x y mirrorType lock life` per mirror, as in V5.1c.
  `lock` is always `0` in Wood 2 (no TURN). `life` is always `-1` in both Wood leagues. `mirrorType` `-1` and `life` 20..0
  only from Bronze (build time and wear stay).
- Crystal lines: `tide litBy myStreak`. `tide` is always `0` in every league (crystals always awake; the field stays
  so the input is identical). `litBy` `-2` = several players (the asleep clause is gone). `myStreak` as in V5.1c.
- The V5.8 echo (a mirror that shattered at the end of turn t is listed once in input t+1 with `life 0`) stays.
- Rebuild check (V5.8 rule): with `tide` always `0`, every crystal is awake on every turn; the streak rebuild needs no
  tide input. The turn number, the player count and the rival scores stay derivable as before.

**Statement and popups**
- The statement is in the "## Statement v5 (staged)" block, edited in place. gen_statements.py reads "Statement v5"
  first, so no v6 block was created (that would need a generator change).
- Removed: the supply bullet and step, the tide bullet and the asleep rules, the tide image reference, the
  order.png reference (it shows the supply step), the "Crystal tides" phrase in the intro and the league preview.
- Welcome popups: level 1 says 150 turns; level 2 says the lock lasts 2 turns; level 3 replaces the supply line with "No new mirrors arrive: the 12 mirrors each player starts with
  are all there is."; level 4 drops the tide line; levels 1 and 2 need no change (they name no supply or tide).

**Images (to redraw or drop; artist-h)**
- `art/png/statement/tides.png`: no longer referenced. Drop it, or draw `values.png` (crystals 1, 2, 3 by size, no tide arc)
  and reference it again only once it exists.
- `art/png/statement/order.png`: no longer referenced. Redraw it without the supply step (placements, turns, every lamp
  fires, scoring, locks drop, mirrors activate and wear), then re-add the reference with that caption.
- `art/png/statement/lock.png`: redrawn with lock 2 by artist-h and referenced again in the Locks section.
- Unchanged: order_wood.png, build.png, wear.png, mirrors.png, setup.png, stops.png, stops_wood.png,
  shared_wood.png, contested.png, streak.png, strike.png.

**Balance flag (owner to confirm; not decided here)**
- Bronze and Silver go from 4 mirrors plus supply (hand max 6) to 12 mirrors with no supply. Over 150 turns a player
  now has 12 mirrors in total instead of about 4 plus 1 per 4 turns. The hardness and fairness gate (M5) must be re-run
  on the new rules before any league is promoted.

**Not done in this section**
- No statement_en.html or welcome build, no compile, no test run. The generator and the referee are coder-h's; the
  input format above is sent to coder-h for agreement.

### V6 later decisions (owner, 2026-11-11 21:15Z; these override the V6 items above)

8. Supply comes back, identical in every league: every player starts with 12 mirrors and gets 1 more at the start of
   turns 10, 20, 30 ... (the input of that turn already counts it). No hand cap. A shattered mirror still does not come back.
   The statement has it from league 1 ("Mirror supply" section and the order list). The popups: league 1 says it;
   league 3 no longer says "no new mirrors arrive".
9. The hidden type is dropped: a pending mirror shows its real type to everyone. `mirrorType` is always `0` or `1`
   (the `-1` value is gone). The rule "a turned mirror does not wear that turn" is dropped too; wear is 1 per turn the
   mirror bends beams, as in the wear timeline.
10. The 2-turn build delay stays: pending on turn t+1, bends beams from turn t+2.
11. Lock 2 and the first legal TURN timing (t+3 Wood, t+4 Bronze): coder-h's referee test is the reference; the Bronze
    input table is still awaited before the lock text is final.

**Images:** build.png must be checked for a hidden type (redraw if it shows one); order.png gets the supply step back
(artist-h); order_wood.png (league 1) gets the supply step too if it is shown.

12. Strike bonus is 4 points per turn (was 3), top league (owner, 2026-11-11 21:32Z). The statement, the parameter table, the V6 section and the level 4 popup say 4; strike.png is being redrawn with +4 by artist-h.
