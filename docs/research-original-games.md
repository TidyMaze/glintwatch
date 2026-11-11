# Research: acclaimed games whose core mechanic is absent from CodinGame

Read 2026-11-11. Only mechanics are borrowed; names, themes, art and exact rules are not.
BoardGameGeek mechanic browser returned HTTP 403 to an anonymous fetch (not used).
Award lists opened: Spiel des Jahres / Kennerspiel winners 2000-2026 (https://en.wikipedia.org/wiki/Spiel_des_Jahres),
IGF Grand Prize winners 1999-2026 (https://en.wikipedia.org/wiki/Seumas_McNally_Grand_Prize).
CG inventory: ./existing-games.md (section "Inventory by genre").

| Source game | Core mechanic | Why praised (URL opened) | On CG? | 4-player simultaneous bot translation |
|---|---|---|---|---|
| Diplomacy | secret simultaneous orders; move/hold/support; strongest support wins, ties stand off; an attacked supporter loses its support | AAGAD Hall of Fame 1994, "Hobby Games: The 100 Best" (https://en.wikipedia.org/wiki/Diplomacy_(game)) | no (troop counts in Ghost in the Cell, Platinum Rift; no support orders) | already simultaneous: each bot sends one order per unit, referee resolves strengths |
| Camel Up | bet on a race of neutral runners; earlier bets pay more | Spiel des Jahres 2014 (https://en.wikipedia.org/wiki/Camel_Up) | no | each turn bet on a runner (odds fall as bets come in) or nudge the race; seeded runner moves |
| Colt Express | players program a sequence of actions, then actions resolve one by one | Spiel des Jahres 2015 + 6 awards (https://en.wikipedia.org/wiki/Colt_Express) | no (Clash of Bots runs a fixed program, no interleaved plans vs rivals) | every K turns each bot commits K actions; steps resolve interleaved; collisions push |
| El Grande | area majority; scoring rounds pay 1st/2nd/3rd by presence per region | Spiel des Jahres + Deutscher Spiele Preis 1996 (https://en.wikipedia.org/wiki/El_Grande) | no (Game of Drones pays only the holder of a zone) | place influence each turn; periodic scoring with ranked prizes; hidden reserve |
| Acquire | tile placement grows chains; buy stock; mergers pay majority holders | "a superb abstract game of strategy and capital" (Dragon) (https://en.wikipedia.org/wiki/Acquire) | no | each turn place 1 tile + buy up to 3 shares; mergers pay bonuses |
| Power Grid | auctions for plants, network expansion with connection costs, resource market with rising prices | SdJ recommended 2005, Games 100 (https://en.wikipedia.org/wiki/Power_Grid) | no (no auction or rising-price market) | sealed bids each turn for one lot; build cities paid by link cost; supply curve |
| Tigris and Euphrates | tile kingdoms, conflicts on merge, score = your weakest colour | Deutscher Spielepreis 1998 (https://en.wikipedia.org/wiki/Tigris_and_Euphrates) | no ("weakest colour" scoring absent) | used as a scoring twist (min over categories) in idea 5 |
| Quacks of Quedlinburg | bag building + push your luck (draw until you stop or bust) | Kennerspiel 2018 + 3 awards (https://en.wikipedia.org/wiki/The_Quacks_of_Quedlinburg) | no | weak interaction in 4p (parallel solitaire); kept as research only |
| Into the Breach | enemies telegraph next turn's attacks; you push and block | GDC Best Design, DICE Strategy GOTY (https://en.wikipedia.org/wiki/Into_the_Breach) | partly (Hypersonic bomb timers, Kutulu wanderers) | neutral hazards announce targets; bots push hazards and each other |
| Lemmings | autonomous walkers follow fixed rules; player assigns limited skills | among the finest early-90s games (https://en.wikipedia.org/wiki/Lemmings_(video_game)) | no (Ants route your own ants; no neutral walkers) | neutral walkers spawn centrally; bots assign roles to any walker to steer flows to their exits |
| Laser-reflection puzzles / board games | rays bounce off mirrors; place or rotate mirrors | genre widely known; no specific page opened | no | idea 1 (Glintwatch, full draft in ./glintwatch/DESIGN.md) |
| Electronic trading (order book, owner's idea) | limit orders matched when bid >= ask; top of book = best bid/ask (https://en.wikipedia.org/wiki/Order_book) | owner request | no | batch auction each turn on a seeded synthetic price; bots' orders move the price |
| Brass | industrial network economy | BGG #1 Feb 2023, Jogo do Ano 2007 (https://en.wikipedia.org/wiki/Brass_(board_game)) | no | page did not describe the shared-network rule; not used further |

Other winners checked in the lists (Carcassonne, Ticket to Ride, Azul, Cascadia, Kingdomino, Dorfromantik:
tile/pattern drafting) map to CG's tile-placement group (Blocking, Git Patchwork), so they are not absent.
Co-op winners (Hanabi, The Crew, Sky Team) map to Fireworks / co-op; IGF winners are mostly narrative or
action titles with no bot-friendly mechanic, except Lemmings-like flow ideas and Crayon Physics (physics: on CG).
