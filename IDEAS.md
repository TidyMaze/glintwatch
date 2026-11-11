# Glintwatch: candidate ideas and choice

Reference list: docs/existing-games.md (99 CG bot games + forum proposals). No listed game uses light beams or mirrors.

## 1. Glintwatch (chosen)
Four lamp keepers on a square reef. Each owns a fixed lamp on its own edge that fires one beam every turn.
Players place neutral mirrors (`/` or `\`) and flip existing ones; any mirror reflects every beam.
Crystals score for the player whose beam is the only one crossing them that turn.
- Players: 2-4 (best at 4; 2 uses opposite seats; 3 leaves one seat empty).
- Nearest: Back to the Code (4-player grid scoring), Ants / Spring 2023 (route paths to resources),
  Back Track King (build paths). Differs: shared, neutral pieces that act on everyone at once; scoring by
  ray paths recomputed every turn; jamming by crossing a rival's crystal; no units, no movement, no capture.
  Outside CG, laser-reflection board games exist (2-player, turn-based, capture pieces); here it is
  simultaneous, 4-player, neutral mirrors, no capture.

## 2. Ebb Ledger (rejected)
Island grid, the tide rises one level every 10 turns and floods by BFS from the sea; players raise or dig
cells with sand to keep owned land dry. Nearest: Keep Off The Grass (eroding tiles), Back to the Code.
Rejected: defence dominates (everyone builds a ring), little interaction in 4-player, flood sim harder to read.

## 3. Quay Auction (rejected)
Four merchants bid sealed coins for cargo lots each turn, then sell them in shifting markets.
Nearest: 6 nimmt!, Poker, Green Circle. Rejected: weak viewer (numbers only), small action space close to a
solved bidding equilibrium, little spatial depth.

## Why 1
- Protocol: one line out (`PLACE x y t`, `TURN x y`, `WAIT`), input = static map once + mirror list per turn.
- Depth: every mirror bends all 4 beams; best moves weigh own gain, rival loss and future flips (locks).
- Viewer: 4 coloured beams on a dark board read at a glance; mirrors and crystals are simple sprites.
- Fair: map has 90-degree rotational symmetry; each seat gets a private launch zone.
- Not trivially solved: 4 simultaneous agents on shared pieces, branching ~2 x 361 + flips per turn.
- Fast referee: 4 ray traces of at most 4 x 19 x 19 steps per turn (< 1 ms).
