# Glintwatch

This is a league-based game: new rules unlock as you climb.

The tide has come in. Each crystal sleeps 15 turns out of every 45 and forgets who held it.
Crystals now hold 1 to 3 points of light, and a beam thrown into a rival's lamp pays 2 each turn.

### Goal
Score more points than the other lamp keepers by lighting crystals with your lamp's beam.

### The reef
The reef is a square grid of `N` x `N` cells (`N` is odd, 13 to 19). `x` is the column (0 on the left), `y` is
the row (0 at the top); north is towards `y = 0`. 2 to 4 players play. Each player owns a **lamp** on one edge.
The reef looks the same from every lamp (90-degree rotational symmetry). With 2 players the lamps are on
opposite edges; with 3 players one edge has no lamp.

Cells:
- `.` empty: a beam goes through;
- `1` `2` `3` a crystal and its value: a beam goes through and **lights** the crystal;
- `A` `B` `C` `D` the lamp of player 0, 1, 2, 3: a beam that reaches it stops;
- `#` rock: a beam stops.

### Beams
After all actions of a turn, every lamp fires one beam. It leaves the lamp in the lamp's direction and moves
cell by cell until it leaves the reef, reaches a rock or reaches a lamp. Beams never block each other and a
beam cannot loop. A beam that crosses the same crystal twice lights it once. Your lamp's straight line holds no
crystal, rock or mirror at the start: to score, you must bend your beam.

### Mirrors
A mirror turns a beam by 90 degrees:

| Mirror | Beam moving east | north | west | south |
|---|---|---|---|---|
| type `0` = `/` | leaves north | leaves east | leaves south | leaves west |
| type `1` = `\` | leaves south | leaves west | leaves north | leaves east |

Mirrors belong to nobody: a mirror bends **every** beam that reaches it, yours and your opponents'.
Players cannot remove mirrors. Mirrors wear out (see below).

### Actions
Each turn you print **one** action:
- `PLACE x y type`: put one of your mirrors on an empty cell.
- `TURN x y`: flip a mirror (`/` becomes `\` and back).
- `WAIT`: do nothing.

You may add a message after your action, for example `PLACE 5 3 1 hello` (30 characters max).

**Yards.** Each lamp's yard is the 5 x 3 block of cells around it (the lamp cell, and every cell up to 2 cells
away along the edge and up to 2 cells deep, diagonals included). Only the yard's owner may place or turn
mirrors there. Beams cross yards freely.

**Same cell.** If two or more players place a mirror on the same cell in the same turn, all of them fail and
keep their mirror. If several players turn the same mirror in the same turn, it flips once.

**Locks.** Each mirror has a `lock`. You can turn a mirror only when its lock is 0. The lock is set
to 5 when the mirror becomes active or is turned, then drops by 1 at the end of each following turn
(so it reads 5 on the next turn's input).

**Impossible actions** are replaced by `WAIT` (the reason is shown in the game summary): a cell outside the
reef, a type other than 0 or 1, a cell that is not empty (rock, crystal, lamp, mirror, pending mirror),
a cell in another player's yard, no mirror in hand, turning where there is no active mirror or the
lock is not 0. A line that is not one of the three commands with whole numbers eliminates you.

### Scoring
Each player has a **streak** (0 to 4) on each crystal. Each turn, for each crystal:
- if the crystal is asleep: all streaks on it become 0 and nobody gains;
- lit by **exactly one** player: that player's streak grows by 1 (max 4) and the player gains
  `value x streak`; every other player's streak drops by 1 (min 0);
- lit by **two or more** players: nobody gains; those players keep their streak; the others' drop by 1;
- lit by **nobody**: every streak drops by 1 (min 0).
A crystal you keep alone pays x1, x2, x3, then x4 every turn.

### Mirror economy
- **Supply.** You start with 4 mirrors and get 1 more at the start of turns 4, 8, 12 ... (the input of that
  turn already counts it). You hold at most 6; extra mirrors are lost.
- **Build time.** A mirror placed on turn `t` is **pending** on turn `t+1` and bends beams from turn `t+2`.
  While pending, beams go through its cell, nobody can place there or turn it, and your opponents read its type
  as `-1` (you read the real type). A pending mirror shows `lock 0` and `life 20`.
- **Wear.** An active mirror bends beams for 20 turns, then shatters at the end of its 20th turn; its cell becomes
  empty. `life` gives the turns left.

### Tides, values and strikes
- **Tides.** Each crystal is awake 30 turns out of every 45: on turn `t` it is awake when `(t + phase) % 45 < 30`,
  otherwise asleep. `phase` is given at the start. An asleep crystal gives nothing and resets the streaks on it.
- **Values.** Crystals are worth 1, 2 or 3.
- **Strike.** Each turn, a beam that ends on another player's lamp gives 2 points to the player who fired it.

### Order of a turn
1. Supply (before the input is sent). 2. All placements, then all turns. 3. Every lamp fires.
4. Scoring. 5. Locks drop by 1; pending mirrors become active, active mirrors wear.

### End of the game
The game ends after 150 turns, or when fewer than two players remain. The highest
score wins; equal scores share the rank. A player who does not answer in time or prints an invalid line is
eliminated: score -1, its lamp becomes a rock, its yard stays protected.

### Example (an example reef, not a real map)
`N = 13`. Your lamp is at (0,3) and fires east; a crystal of value 1 is at (5,6). You print `PLACE 5 3 1`.
Your beam goes (1,3) ... (5,3), turns south on `\`, crosses (5,4), (5,5), lights (5,6) and goes on south.
If nobody else lights it you gain 1, 2, 3, then 4 per turn.
The mirror starts bending beams 2 turns after you place it.

### Game input
**Initialization**
- Line 1: `playerCount myId`
- Line 2: `N`
- Next `playerCount` lines: `playerId lampX lampY dir` (`dir` is `E`, `S`, `W` or `N`)
- Next `N` lines: one row of the reef, `N` characters, `y = 0` first
- Next line: `crystalCount`
- Next `crystalCount` lines: `x y value phase`

**Each turn**
- Line 1: `turn` (starts at 1)
- Next `playerCount` lines, by player id: `score mirrorsInHand active` (`active` 1, or 0 if eliminated)
- Next line: `mirrorCount`
- Next `mirrorCount` lines: `x y type lock pending life` (every listed mirror, pending or active, fills its cell: you cannot place there)
  - `type`: 0 (`/`) or 1 (`\`); -1 for an opponent's pending mirror
  - `lock`: 0 when it can be turned
  - `pending`: 1 if not yet bending beams, else 0
  - `life`: active turns left before it shatters
- Next line: `crystalCount`
- Next `crystalCount` lines: `x y awake litBy s_0 ... s_(playerCount-1)`
  - `awake`: 1 or 0
  - `litBy`: the player who lit it alone on the last turn, -1 nobody (and on turn 1), -2 several players
  - `s_i`: player i's streak on it, 0 to 4

**Output**: one line: `PLACE x y type`, `TURN x y` or `WAIT`, optionally followed by a message.
Tokens are separated by spaces; any other separator (a tab, for example) makes the line invalid.

**Constraints**: 2 <= `playerCount` <= 4; 13 <= `N` <= 19, odd; response time 1000 ms on turn 1, 50 ms after.
Rows and lists are sorted by `y`, then `x`.
