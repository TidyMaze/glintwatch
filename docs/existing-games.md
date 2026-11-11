# Existing CodinGame bot-programming games (originality reference)

Source: public API `Puzzle/findProgressByIds` (types ARENA + BOT_PROGRAMMING, 99 games, read 2026-11-11, no login),
forum.codingame.com search + Puzzles category (anonymous JSON).
Not reachable without login: the community contribution list (`Contribution/getAcceptedContributions` returns
error 610 when anonymous), so pending/draft multiplayer contributions are known only through the forum.
O = official, C = community. Mechanic = one line from memory of the statement or this repo's notes; "(unverified)"
marks lines not checked against a statement.

| Game | O/C | Players | Mechanic |
|---|---|---|---|
| Tron Battle | O | 2-4 | light cycles leave walls on a grid, last alive wins |
| Game of Drones | O | 2-4 | drones hold circular zones, points per zone held |
| Poker Chip Race | O | 2-4 | physics chips absorb smaller chips |
| Platinum Rift 1/2 | O | 2-4 | pods conquer hex zones on a world map, platinum income |
| Great Escape | O | 2-3 | race to the far side, place walls (Quoridor-like) |
| Back to the Code | O | 2-4 | paint grid cells by enclosing areas, time-travel back |
| Mad Pod Racing | O | 2 (2 pods each) | physics racing through checkpoints |
| Smash The Code | O | 2 | falling coloured blocks, combos send garbage |
| CodeBusters | O | 2-4 teams | busters trap ghosts in fog, stun |
| Hypersonic | O | 2-4 | bomber on grid, boxes, items |
| Fantastic Bits | O | 2 | wizards physics, throw snaffles into goals, spells |
| Ghost in the Cell | O | 2 | factories graph, troop sending, bombs |
| Coders of the Caribbean | O | 2 | hex ships, rum barrels, cannonballs, mines |
| Wondev Woman | O | 2 | Santorini-like build and climb |
| Mean Max | O | 3 | physics reaper/destroyer/doof collect water |
| Code4Life | O | 2 | robots collect molecules to complete samples |
| Ultimate Tic-Tac-Toe | O | 2 | nested tic-tac-toe |
| Botters of the Galaxy | O | 2 | MOBA heroes, units, towers |
| Code Royale | O | 2 | queens build sites, mines, barracks, towers |
| Code of Kutulu | O | 4 | explorers keep sanity, avoid wanderers, co-op/hostile |
| Legends of Code & Magic (+Constructed) | O/C | 2 | card draft and duel |
| Xmas Rush | O | 2 | sliding tile labyrinth, quests |
| Code a la Mode | O | 3 (2 cooperate) | kitchen chefs build orders |
| Crystal Rush | O | 2 | robots dig ore, radar, traps |
| Ocean of Code | O | 2 | hidden submarine, sonar, torpedo |
| Spring 2020 (Pac) | O | 2 | pacs eat pellets, rock-paper-scissors forms |
| Fall 2020 | O | 2 | witches brew potions from ingredient spells |
| Spring 2021 | O | 2 | hex forest, trees grow by sun, shadows |
| Spider Attack (Spring 2022) | O | 2 | heroes defend base from monsters, wind/control |
| Keep Off The Grass (Fall 2022) | O | 2 | robots claim tiles, recyclers erode |
| Ants (Spring 2023) | O | 2 | beacons route ant chains to resources |
| Seabed Security (Fall 2023) | O | 2 | drones scan fish in dark, monsters |
| Olymbits (Summer 2024) | O | 3 | four mini-games at once, shared inputs |
| Cellularena (Winter 2024) | O | 2 | organism grows organs, proteins |
| Soak Overflow (Summer 2025) | O | 2 | agents throw water, cover, splash |
| SnakeByte (Winter 2026) | O | 2 | snake bots on a platform grid (unverified) |
| Troll Farm (Spring 2026) | O | 2 | trolls harvest fruits into shacks |
| Back Track King (Summer 2026) | O | 2 | paint rail tracks between towns |
| TryAngle Catch (Fall 2026) | C contest | 2 | triangle-catch board contest |
| Vindinium | C | 4 | heroes take mines, taverns, fights |
| Langton's Ant | C | 2 | players steer a Langton ant colouring cells |
| Checkers, Chess, Othello, Connect 4, Hex, Yavalath, Oware, Breakthrough, Onitama, Twixt-PP, Yinsh, Atari Go (2 sizes), Dots and Boxes, Clobber, Amazons, Abalone, Lines of Action, Nine Men's Morris, Isola, Minishogi, Dice Shogi, Impasse, Ataxx, Brandubh, Counting TicTacToe | C | 2 | classic abstract board games |
| Cultist Wars | C | 2 | units convert neutral cultists, shoot |
| Bit Runner 2048 | C | 2v2 | physics cars push a ball |
| Bandas | C | 2 | push rows off a shrinking board (unverified) |
| A Code of Ice And Fire | C | 2 | train units, claim territory, cut HQ links |
| Paper Soccer | C | 2 | draw ball path on dot grid |
| Tower Dereference | C | 2 | tower defense build vs waves |
| Coders of the Realm (1v1 and multi) | C | 2-4 | territory armies (unverified) |
| Tulips and Daisies | C | 2 | plant flowers, lines of 5 |
| Penguins | C | 2-4 | penguins slide on hex floes, eat fish |
| Blocking | C | 2-4 | polyomino placement corner-to-corner (Blokus-like) |
| Fireworks | C | 2-5 coop | cooperative card game (Hanabi-like) |
| Night Of War | C | 2 | tactical grid soldiers (unverified) |
| Dice Duel | C | 2 | dice game |
| Volcanoes | C | 2 | volcano tiles eruptions on a ring board |
| Space Shooter | C | 2 | ship physics, missiles |
| Clash of Bots | C | 2 | program interpreter bots on a grid |
| Code Keeper - The Hero | C | 1-2 | dungeon hero (unverified) |
| Start-up! | C | 2-4 | companies hire, market share (unverified) |
| Green Circle | C | 2 | card deck-building, release apps |
| 6 nimmt! | C | 2-10 | card rows, take penalty heads |
| Git Patchwork | C | 2 | Patchwork-like tile placement (unverified) |
| Mad Knights | C | 3 | each knight blocks visited squares, last mover wins |
| Poker | C | 2+ | hold'em |
| Gargoyles versus Santas | C | 2-4 | gargoyles catch gifts dropped by santas |
| Domain Expansion | C | 2 | 7x7 tokens move and build walls, territory |
| Game of Life or Death | C | 2 | Conway life with player cells |
| BeeMineGame | C | 2 | bees flower a meadow, territory |
| Chain Reaction | C | 2+ | orbs explode into neighbours |
| Elemental Wars | C | 2 | (unverified, not read) |

Forum-only (proposed or in progress, not in the list): Bacter.I.A. (2026-07, bacteria eat cells or claim territory),
war of the eight (2020-12, 8-player 4X with move/trade), robots-clone chase idea and Hammerwatch-like team idea
(thread "Multiplayer game ideas - general discussion", 2020-07).

Light/beam mechanics: none of the above uses light beams, mirrors or ray paths.

## Inventory by genre / core mechanic (what CG already has)

| Genre / mechanic | CG games |
|---|---|
| Abstract 2p board (perfect info) | Chess, Checkers, Othello, Hex, Connect 4, Amazons, Abalone, Yavalath, Oware, Breakthrough, Onitama, Twixt-PP, Yinsh, Atari Go, Dots and Boxes, Clobber, Lines of Action, Nine Men's Morris, Isola, Minishogi, Dice Shogi, Impasse, Ataxx, Brandubh, UTTT, Counting TicTacToe, Wondev Woman, Paper Soccer, Mad Knights |
| Grid survival / trail blocking | Tron, Mad Knights, Isola |
| Bomber / timed explosions | Hypersonic, Chain Reaction |
| Physics racing / sport | Mad Pod Racing, Poker Chip Race, Fantastic Bits, Mean Max, Bit Runner 2048, Space Shooter |
| Territory painting / claiming | Back to the Code, Keep Off The Grass, A Code of Ice And Fire, BeeMineGame, Domain Expansion, Bacter.I.A. (forum) |
| Zone holding with units | Game of Drones, Platinum Rift 1-2, Coders of the Realm |
| Troop sending on a graph | Ghost in the Cell |
| Collection / harvesting with units | Code4Life, Crystal Rush, Troll Farm, Vindinium, Seabed Security, CodeBusters, Spring 2020, Gargoyles vs Santas |
| RTS / MOBA / tower defence | Code Royale, Botters of the Galaxy, Tower Dereference, Spider Attack, Cultist Wars, Night of War |
| Growth / cellular automata | Cellularena, Game of Life or Death, Langton's Ant, Spring 2021 (trees) |
| Path / network building | Back Track King, Ants (Spring 2023, beacon chains), Great Escape (walls), Xmas Rush (tile slide) |
| Polyomino / tile placement | Blocking, Git Patchwork, Tulips and Daisies |
| Card games | Legends of Code & Magic, Green Circle, 6 nimmt!, Poker, Fireworks (coop), Fall 2020 (spell engine) |
| Hidden info / deduction | Ocean of Code (hidden sub), Seabed Security (dark), Poker |
| Co-op / semi co-op | Code a la Mode, Code of Kutulu, Fireworks |
| Multi-game | Olymbits |
| Program interpreter | Clash of Bots |

Absent (no CG game uses it as the core): light/ray reflection; Diplomacy-style simultaneous orders with support;
race betting with odds; programmed multi-step action queues; area majority with ranked prizes; stock/merger
markets; auctions (open or sealed) combined with networks; trading / order books / price prediction;
neutral autonomous walkers steered by role assignment; push-your-luck bag building; telegraphed hazards that
players redirect.
