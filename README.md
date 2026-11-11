# Glintwatch

> Bend your lamp's beam with shared mirrors and hold the reef's crystals alone. 2 to 4 players.

Glintwatch is an original multiplayer bot-programming game for the [CodinGame](https://www.codingame.com) platform. It is a community contribution in progress: the game, its referee and its viewer are written, and it is being prepared for submission.

## The game

Night on the Tidereef: up to four keepers fire a lamp beam from the edge of a square reef. Place and turn mirrors to bend your light onto crystals. A crystal pays only the player who lights it alone, and pays more each turn you hold it. Every mirror bends every beam, so each one you place can help a rival.

Four leagues, each adding rules:

1. **Wood 2**: place mirrors, every crystal you light pays 1.
2. **Wood 1**: crystals pay only the player who lights them alone, with a streak multiplier (x1 to x4); `TURN` action; placed mirrors are locked for 5 turns.
3. **Bronze**: limited mirror supply that grows over time; a mirror acts 2 turns after placing, its type hidden from rivals; mirrors shatter after 20 turns.
4. **Silver**: crystals sleep in tides; a beam ending on a rival's lamp pays points each turn.

The full rules are in [`statement-silver.md`](statement-silver.md) and [`DESIGN.md`](DESIGN.md).

## Build and run

Requires Java 17+ and Maven.

```
cd referee
mvn -q test          # unit and integration tests
```

Local viewer (opens at http://localhost:8888/test.html):

```
cd referee
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
timeout 600 java -cp "target/classes:target/test-classes:$(cat cp.txt)" Main <seed> <league 1-4> <bot> <bot> [<bot> <bot>]
```

Bots available locally: `wait`, `random` (2 to 4 per game). Example: `Main 1 4 random wait random`.

The `referee/config/levelN/Boss.py` files are placeholders (they print `WAIT`); the real league bosses are provided on CodinGame.

## Repository layout

| Path | Content |
|---|---|
| `referee/` | Maven project: game engine, referee, viewer (CodinGame SDK 4.7.8), tests, simple bots, league config and statements |
| `art/` | sprites (PNG and SVG sources), generator scripts, concept art, style guide |
| `art-common/` | shared palette and emblem style guide |
| `ux/` | viewer layout notes and mockups |
| `story/` | lore, statement intro, store descriptions |
| `statement-silver.md` | full statement of the top league |
| `DESIGN.md`, `IDEAS.md`, `STATEMENT-REVIEW.md` | design documents |
| `docs/` | panel vote, survey of existing CodinGame games, research notes |

## Credits

Designed and directed by TidyMaze. Rules, code, art and texts produced with AI agents under the owner's review. All content is original; no third-party assets.

## Licenses

Code: MIT ([`LICENSE`](LICENSE)). Art, UX mockups, story, statements and design documents: CC BY 4.0 ([`LICENSE-ASSETS`](LICENSE-ASSETS)).
