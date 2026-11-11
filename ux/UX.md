# Glintwatch: UX/UI spec (v1, 2026-11-11)

Inputs: `../DESIGN.md` (rules v1, viewer section 12), `../art-common/STYLE.md`, `../docs/VOTE.md`.
Mockup: `mockup.svg` / `mockup.png` (1920x1080, N = 15, turn 37, 4 players), made by `mock.py` + `state.py`
(beams traced with the real optics of DESIGN 6.1). Regenerate: `python3 -I mock.py mockup.svg && sips -s format png mockup.svg --out mockup.png`.

Goal, by value: a spectator at 1x speed must see in one glance **whose beam goes where, which crystals pay whom,
which pay nobody, and what each player did this turn**. Everything else is second.

## 0. Changes against DESIGN.md 12 (sent to the designer)
| Item | DESIGN 12 | UX | Why |
|---|---|---|---|
| Colours / emblems | id0 orange circle ... | STYLE.md: id0 Sky ring, id1 Amber triangle, id2 Jade square, id3 Orchid star | one palette for all contrib games |
| Background | `#0B1426` | STYLE `#1A1D29` -> `#0E1018` gradient | STYLE |
| Panels | 380x220, avatar 100 | 380x300, avatar 112 | room for stock, last action, message at >= 22 px |
| Turn counter | top centre over the board edge | left middle box | nothing above the playfield |
| Messages | above the lamp | last line of the panel | a bubble over the board covers cells |
| Floating +N | on crystals | HUD gain and strikes only | lit crystals pay every turn: one float per crystal per frame is noise |
| Beam width | `max(4, cell/8)` | `max(7, round(0.14*cell))` | 1.3-1.7 px at phone width otherwise |
| Beam look | Line, colour only | TilingSprite pattern per player + fixed lane | colour-blind: min dE 16 between players |

## 1. Canvas layout (1920 x 1080, px boxes x, y, w, h)
| Box | x | y | w | h | Content |
|---|---|---|---|---|---|
| Board frame | 460 | 40 | 1000 | 1000 | ink `#0E1018`, rx 10, 2 px `#3A4054` border |
| Board cells | `x0 = (1920 - N*cell)/2` | `y0 = 40 + (1000 - N*cell)/2` | `N*cell` | `N*cell` | `cell = floor(1000/N)`: N13 76, N15 66, N17 58, N19 52 |
| Panel seat 0 (lamp on W edge, upper) | 40 | 40 | 380 | 300 | |
| Panel seat 1 (N edge, right) | 1500 | 40 | 380 | 300 | |
| Panel seat 2 (E edge, lower) | 1500 | 740 | 380 | 300 | |
| Panel seat 3 (S edge, left) | 40 | 740 | 380 | 300 | |
| Turn box | 40 | 380 | 380 | 320 | "TURN", `37 / 150` (72 px), progress bar 324x14, next mirror turn, contested count, awake crystals `17 / 21` |
| Legend box | 1500 | 380 | 380 | 320 | 6 icon rows (36 px pitch) + beam pattern key |
| Coordinate labels (toggle) | margins | 0-34 top, 1046-1080 bottom, 420-460 / 1460-1500 sides | | | 20 px, `#A0A6B8` |

Each panel sits on the side of its lamp (seat 0 lamp at row `e < c`, seat 1 at column `N-1-e > c`, etc.).
Gaps: 40 px between board and panels, 40 px between stacked boxes. Nothing overlaps the board.
2 players (seats 0 and 2): panels top-left and bottom-right, the other two slots stay empty (no box).
3 players: seat-3 slot empty.

### 1.1 Player panel (relative to panel origin; 380 x 300, panel `#232736`, rx 12, 3 px border, 12 px colour strip)
| Element | Box / anchor | Font |
|---|---|---|
| Avatar `getAvatarToken()` | 28, 24, 112 x 112, rx 10, 3 px colour border | |
| Emblem badge | centre (136, 132), r 24 disc + emblem r 15 | |
| Nickname | (156, 56) baseline, max 200 px wide, cut with "..." | bold 30, `#E8EAF0` |
| Score | (172, 130) baseline | bold 72, player colour |
| Gain of the turn `+N` | right-aligned at (352, 254), on the last-action line | bold 34, `#F4F6FF`; alpha 0 when 0 (moved off the score row: 4-digit scores need the width) |
| Mirrors in hand (v3: 0-6) | icon r 15 at (44, 182) + count | bold 32 + "/6 hand" 22 `#A0A6B8` |
| Crystals lit alone | diamond icon at (232, 182) + count | bold 32 + "lit" 22 |
| Divider | y 214, 28-352 | 2 px `#3A4054` |
| Last action | (28, 252) baseline: `WAIT`, `PLACE 9 14 /`, `TURN 3 8`, `refused: zone`, `collision` | bold 28; refused/collision in Danger `#FF4D4D` with a cross icon |
| Message | (28, 284) baseline, max 24 chars shown, full text in the panel tooltip | italic 24, `#A0A6B8` |

### 1.2 Scaled views (checked on the mockup with `sips -Z`)
- **960 x 540** (CG default embed): every text of 22 px or more stays >= 11 px and reads; board icons, lock rings,
  beam patterns, contested badge and strike spark all read (checked on the half-size PNG).
- **Phone width 360 px** (scale 0.19): scores (72 -> 13.5 px), turn number, beams (7-11 -> 1.3-2 px), crystal
  colours and the white contested crystal read; names, legend and last actions do not. Accepted: on a phone a
  player is identified by panel colour + position (same side as its lamp). Not measured on a real phone.

## 2. Visual hierarchy and states (brightest = most important)
1. Beams (saturated, glow) and lit crystals. 2. Scores. 3. Mirrors (silver, high contrast, neutral). 4. Lamps.
5. Rocks, yards, floor (dark, low contrast).

| Element | State | Look |
|---|---|---|
| Beam of player s | active | glow `Line` width `2w+6`, alpha 0.22 under a `TilingSprite` of width `w`, tinted player colour, pattern: s0 solid, s1 dash 22/10, s2 dots (round, pitch 15), s3 double line (colour with 3 px ink core). Lane offset `(s - 1.5) * (w + 2)` px perpendicular to each segment, so 4 beams on one row never cover each other |
| Beam | path changed this turn | glow alpha 0.6 -> 0.22 over 0.3 s |
| Beam end | exits board | ends on the board edge |
| Beam end | rock | ends on the rock face, small white spark (2 frames) |
| Beam end | strike | ends on the lamp face, 8-ray white spark + `+2` float in the shooter colour (bold 34, ink stroke 8) |
| Crystal | unlit | grey `#9AA0B4` diamond, width `0.40/0.52/0.64 * cell` for value 1/2/3, value digit in ink (bold `0.30*cell`) |
| Crystal | lit by one | fill = owner colour, soft glow, owner emblem badge (r 12) top-right |
| Crystal | contested (2+) | fill white `#F4F6FF`, dashed white ring, crackle marks, badge `0` (ink box, white digit) |
| Mirror | free | silver bar `#D8DEEA` with ink outline on a `#3A4054` round base (r `0.30*cell`) |
| Mirror | locked n (1-3) | ring of 3 arcs (r `0.40*cell`, 6 px): n arcs in the last toucher's colour, the rest `#4A5064` |
| Mirror | placed / turned this turn | white cell outline 5 px + actor-colour outline 3 px + white fill 18 %, actor emblem top-left; one frame |
| Lamp | active | disc r `0.42*cell`, dark-colour fill, 5 px colour ring, emblem, firing nose towards its direction |
| Lamp | player eliminated | disc grey `#4A5064`, Danger cross over the emblem; beam gone |
| Yard (zone) | always | colour fill 10 % + 3 px dashed border (12/8) alpha 0.8 |
| Panel | eliminated / timeout | panel alpha 0.4, emblem crossed in Danger, last action line `ELIMINATED: timeout` in Danger |
| Collision | both PLACE on one cell | white cross flash on that cell 0-0.3 s; both panels say `collision` |

## 3. Animation per turn (`gameManager.setFrameDuration(600)`; t = fraction of the frame)
| t | What |
|---|---|
| 0.00-0.30 | actions: placed mirror scale 0 -> 1 (`Curve.EASE_OUT`) + highlight on; turned mirror rotation +90 deg (`EASE_OUT`); collision cross; panel last-action text set at 0.00 |
| 0.30 | beams snap to the new paths (`Curve.NONE`; a tweened path would show a wrong path); changed beams glow flash starts |
| 0.30-0.60 | crystals whose owner set changed re-tint at 0.30 and pulse 1 -> 1.15 (0.45) -> 1 (0.60); contested crackle on; strike spark and `+2` float (rise 30 px, alpha 1 -> 0); HUD gain `+N` shown at 0.30; score text set at 0.45 |
| 0.60 | lock rings show the new lock values; highlight stays until the next frame's 0.00 |

A 150-turn game plays in 90 s at 1x. Frame 0 (init): board, yards, lamps, start mirrors, panels; no beams.

## 4. Tooltips (TooltipModule, `tooltips.setTooltipText(entity, text)`)
| Entity | Text | Updated |
|---|---|---|
| Floor cell | `(x, y)` + `yard of <name>` if in a zone | static |
| Crystal | `(x, y) crystal 2 pts` / `lit by <name>` / `contested: <name>, <name>` / `unlit` | when its lit set changes |
| Mirror | `(x, y) mirror / ` + `free` or `locked 2` + `last touched by <name>` | when placed, turned, or lock changes |
| Rock | `(x, y) rock` | static |
| Lamp | `<name> lamp (x, y), fires E` + house line `House Sky Ring` (story/DESCRIPTION.md) + `eliminated: timeout` | on elimination |
| Panel | full 30-char message, mirrors left, score | each frame with a message |

Hover is mouse only; on touch, CG tooltip behaviour is not verified (not done).

## 5. Toggles (ToggleModule in the viewer options menu)
`toggleModule.displayOnToggleState(entity, name, true)`; `config.js`: `options: [ToggleModule.defineToggle({toggle: name, title, values: {ON: true, OFF: false}, default})]`
(checked in `module-toggle-4.1.6.jar`, `ToggleModule.js`: `defineToggle` reads `toggle`, `values`, `default`; the Java state is a boolean sent as `+`/`-` per entity).
| Toggle | Default | Entities |
|---|---|---|
| `beam0` .. `beam3` "Beams of <player>" | ON | that player's beam segments + glow (lets a viewer follow one player) |
| `yards` "Yards" | ON | zone tints and borders |
| `coords` "Coordinates" | ON (changed 2026-11-11: no floor tooltips, so the margins are the only coordinate source; bot authors need them) | axis labels in the margins |
| `locks` "Lock rings" | ON | lock arcs |

## 6. Accessibility (computed: WCAG 2 contrast; colour blindness = Machado 2009 severity 1.0, CIE76 dE)
| Pair | Ratio | Verdict |
|---|---|---|
| Text `#E8EAF0` on panel `#232736` | 12.3 | pass |
| White `#F4F6FF` on panel | 13.8 | pass |
| Label `#A0A6B8` on panel | 6.1 | pass; **STYLE muted `#6B7185` gives 3.06: fails 4.5 for 22-24 px labels, so labels use `#A0A6B8`** (artist asked) |
| Sky / Amber / Jade / Orchid on panel | 6.43 / 6.59 / 4.34 / 4.85 | names are white; colour text only at >= 30 px bold (large text, 3:1): pass |
| Beams on floor `#2C3142` (lighter tile) | 5.60 / 5.74 / 3.78 / 4.22 | pass 3:1 (graphics) |
| Beams inside own yard, 10 % tint | 4.69 / 4.85 / 3.36 / 3.68 | pass; an 18 % tint (DESIGN) drops Jade to 3.03 |
| Silver mirror `#D8DEEA` on floor | 9.6 | pass |
| Ink digit on crystal grey / Jade / Orchid / white | 7.3 / 5.6 / 6.2 / 17.6 | pass |
| Rock body `#4A5064` on floor | 1.6-1.8 | **fails 3:1**; top highlight `#6E7590` 2.8-3.1; rock reads by shape, outline and shadow. Open: artist to raise the rock face to >= 3:1 |
| Danger `#FF4D4D` on panel | 4.5 | pass, always with a cross |

Colour-blind distances between the 4 beams: normal 56.7, protan 20.7 (Sky/Orchid), deutan 18.1 (Jade/Orchid),
tritan 16.2 (Sky/Jade). Beam vs floor >= 37.5, beam vs contested white >= 34.1. So colour is never alone: every
beam has its pattern and lane, every lit crystal its owner emblem, every panel its emblem and screen side.
Minimum sizes at 1920: text 22 px, numbers 28 px, scores 72 px, board emblems 24 px (STYLE min), beam 7 px.

## 7. End screen
`endScreenModule.setScores(int[])` (eliminated = -1), `setDisplayedText(String[])`: `"<score> pts"` or
`"eliminated"`, `setTitleRankingsSprite("logo.png")`. Optional: one flavour line (<= 30 chars, story/DESCRIPTION.md) under the title; panels keep nicknames.

## 8. Layers (z-index) and module calls for the coder
| z | Layer | Entities |
|---|---|---|
| 0 | background | Sprite `background.png` |
| 10 | board frame, floor | Rectangle frame; Sprite `floor_0/1` per cell (tooltip holders) |
| 20 | yards | Sprite `zone_tile` tinted alpha 0.10 + dashed border (Sprite `zone_border` 9-slice or 4 TilingSprites) |
| 30 | rocks | Sprite `rock` |
| 40 | beam glow | Line per segment, pool |
| 41 | beams | TilingSprite per segment, pool (anchorY 0.5, rotation 0/90/180/270 deg) |
| 50 | crystals | Group: glow, crystal sprite, digit Text, badge |
| 60 | mirrors | Group: base, lock ring arcs, bar (rotation by type) |
| 61 | mirror highlight | Sprite `highlight_cell` + actor emblem |
| 70 | lamps | Group: nose, disc, emblem |
| 80 | effects | SpriteAnimation `spark_sheet`, `flicker_sheet`; Text `+2` |
| 90 | HUD panels, turn box, legend | Groups |
| 100 | coordinate labels | Texts (toggle) |

Pools: a path has (mirrors on it + 1) segments; grow each player's pool on demand (never shrink) and hide unused ones (`setVisible(false)` at t 0.30). Commit beam changes with `graphics.commitEntityState(0.3, ...)`.
Modules: GraphicEntityModule, TooltipModule, ToggleModule, EndScreenModule.

## 9. Sprites for the artist (PNG, drawn at 128 px unless said, scaled to `cell`; anchor centre unless said; white = tinted at runtime)
| Name | Size | Anchor | States / frames | Notes |
|---|---|---|---|---|
| `background.png` | 1920x1080 | 0,0 | 1 | gradient `#1A1D29` -> `#0E1018`, faint waves, no text |
| `floor_0.png`, `floor_1.png` | 128 | 0,0 | 1 | `#262A38` / `#2C3142`, low detail |
| `rock.png` | 128 | centre | 1 | face >= 3:1 vs floor, ink outline, top-left highlight, shadow |
| `crystal.png` | 128 | centre | 1, grey-white | scaled 0.40 / 0.52 / 0.64 of cell for values 1-3; tinted owner colour or white |
| `crystal_glow.png` | 128 | centre | 1 | white radial, tinted, additive |
| `badge_zero.png` | 32 | centre | 1 | ink box with white `0` (contested) |
| `mirror_base.png` | 128 | centre | 1 | round base `#3A4054` |
| `mirror_bar.png` | 128 | centre | 1 | silver `/`; type 1 = rotate 90 deg |
| `lock_arc.png` | 128 | centre | 1 | one 104-deg arc of radius 0.40; 3 copies rotated 0/120/240, tinted |
| `highlight_cell.png` | 128 | centre | 1 | white rounded outline 5 px + 18 % fill |
| `lamp_disc.png`, `lamp_nose.png` | 128 | centre | 1 | white, tinted (dark colour fill + main ring); nose faces E |
| `emblem_ring/triangle/square/star.png` | 48 | centre | 1 | white with ink outline, tinted; used at r 11-24 |
| `emblem_cross.png` | 48 | centre | 1 | Danger cross overlay (eliminated) |
| `beam_solid/dash/dots/double.png` | 32x16 | 0,0.5 | 1, tiling | white patterns: solid; 22 on 10 off; round dots pitch 15 (scale to fit 32: 16 px pitch); white with 3 px ink core |
| `spark_sheet.png` | 4 x 64x64 | centre | 4 | white rays |
| `flicker_sheet.png` | 4 x 128x128 | centre | 4 | white crackle loop (contested) |
| `zone_tile.png` | 128 | 0,0 | 1 | plain white |
| `zone_border.png` | 32x8 | 0,0.5 | 1, tiling | dashed 12/8 |
| `panel.png` | 380x300 | 0,0 | 1 | `#232736`, rx 12, white 3 px border and 12 px strip (tinted) |
| `icon_mirror.png`, `icon_crystal.png`, `icon_refused.png` | 32 | centre | 1 | HUD rows |
| `logo.png` | 600x200 | centre | 1 | end screen title |

## 10. Open questions (owner or designer)
1. Closed (DESIGN V3.8): yards of empty seats (2p: seats 1, 3; 3p: seat 3) are ordinary cells. The viewer draws no yard there; the legend says `empty seat: open to all`.
2. Closed: artist agreed to 380x300 and updates STYLE. Rock: dark body + 3 px rim and top bevel `#6E7590`, 3.06:1 on floor `#1B2D45`, accepted.
3. TilingSprite beams: pattern alignment across a corner is not continuous (each segment restarts the tile);
   accepted. Fallback if TilingSprite rotation misbehaves in the SDK: Line + emblem at each segment middle.

## 11. Not done
- No survey of existing CG 4-player replays: CG replay pages need a JS browser (WebFetch gets a loading page) and Playwright opens the owner's Chrome; skipped.
- No real phone check; no test of TooltipModule on touch; ToggleModule checked in the jar source only, not in a running viewer.
- Mockup is static (one frame); timing in section 3 is unseen until the coder's viewer runs.

## 12. v2/v3 rule states (DESIGN V2.2-V3.8; overrides the sections above where they differ)
Mockup updated for them (turn 37: one pending mirror, one mirror at life 3, lock arcs 0-5, 4 dormant crystals, streak badges x2-x4).
Artist mood A adopted: floor `#1B2D45` / `#18283E`, rocks `#0A111D` with a 3 px rim + top bevel `#6E7590` (3.06:1, accepted),
yard tint 16 % (Jade beam inside its own yard 3.31:1, pass). Beam glow stays solid; only the core carries the pattern.
Beam offset: the artist's constant diagonal `(d, d)`, `d = (seat - 1.5) * (w + 2)`, on every point (joins stay closed).

| Element | State | Look | Sprite |
|---|---|---|---|
| Mirror | pending (placed, beams pass) | dashed ring in the placer's colour (r `0.30*cell + 4`), bar at 40 % alpha dashed, placer emblem top-left, 1-2 bars bottom-right = turns before it reflects | `mirror_pending.png`, `pending_tick.png` (6x12) |
| Mirror | pending, spectator view | the viewer shows the **true type** at 40 % (spectators see bluffs); the tooltip says `type hidden from rivals` | |
| Mirror | lock 1-5 | 5 arcs of 54 deg (gap 18), n in the last toucher's colour, rest `#55617A` | `lock_arc.png` (54 deg) |
| Mirror | life 20-8 / 7-4 / 3-1 | intact / one crack / two cracks + bar alpha 0.65 | `mirror_crack_1.png`, `mirror_crack_2.png` overlays |
| Mirror | shatters (life 0) | at t 0.60-1.00 after this turn's beams: 4-frame shard burst, then gone | `shatter_sheet.png` 4 x 128 |
| Crystal | dormant | hollow: dashed outline `#8E9BB3` (4.97:1 on floor), grey digit, no glow; beams pass over it | `crystal_outline.png` |
| Crystal | wakes / sleeps this turn | at t 0.30 swap filled <-> hollow with a 0.15 s white flash | |
| Crystal | streak k (owner decision 2026-11-11) | badge `x2` / `x3` / `x4` at the right end of the crystal's badge strip, nothing at x1; ink pill (w `1.45*fs`, h `1.15*fs`, `fs = max(17, round(0.32*cell))`: 21 px at N15, 17 at N19), bold text and 2 px border in the lighter's colour (Jade on ink 5.55:1, lowest) | `streak_badge.png` (white pill outline, tinted; text is a Text entity) |
| Crystal | contested, streaks frozen | white crystal + `0` badge top-right; streak badge in white with a dashed border, value = highest frozen streak; every player's streak in the tooltip | same |
| Panel | mirrors in hand | `3 /6 hand` | |
| Panel + turn box | supply | panel: `3 /6  +1 in 3`; turn box: `Next mirror (all)  in 3` (turns to the next multiple of 4) | |

Readability at 960x540 (checked on the half-size mockup): pending, cracked, dormant, lock arcs and the `x2-x4` badge read (badge text ~10 px at N15, 8.5 px at N19 at half size). Streak pips are dropped (owner).
Lock at N = 19: arcs of ~20 px at 1920; "locked or not" reads, the count does not at half size.

Tooltip additions: mirror `pending 1 (reflects next turn)`, `life 7`, `lock 3`; crystal `awake, sleeps in 4` /
`dormant, wakes in 6`, `streaks: Sky 4, Amber 2 (frozen)`.
Crystal phase countdown: shares the crystal's **badge strip** (bottom row of the cell) with the streak badge: timer chip bottom-left, streak chip bottom-right, both the same size (`fs = max(17, round(0.32*cell))`, chip `1.45*fs x 1.15*fs`; 2 chips = 61 px of 66 at N15, 50 of 52 at N19). Timer: `↑2` (wakes in 2) or `↓3` (sleeps in 3), white on ink, border `#8E9BB3`, drawn only when the change is <= 3 turns away. Same sprite `streak_badge.png`, tinted grey-blue. At 960 px: ~10 px text, reads (was 8 px in a separate 16 px pill).
Toggle `timers` "All crystal timers": dropped (adds data on every crystal every frame; the <= 3-turn chip and the tooltip phase cover it).
Streak decay (D5): a crystal not lit by P while P's streak on it is >= 2 shows P's badge at 60 % opacity with a dashed border (one player shown: the highest decaying streak; all in the tooltip).
Animation addition: shatter at t 0.60-1.00; supply `+1` on the hand count at t 0.00 of turns multiple of 4.
Cover: 1200x675 (art/cover.png). Unverified: neither a web search nor the CG contribution skill (games.md, SKILL.md; only "at most 300 assets") gives a size; the owner checks it on the upload page.

## 13. SDK-forced changes (cg-contrib-coder, accepted by UX 2026-11-11)
- Floor: one tiling `floor_0` (no per-cell checker, no floor tooltips); coordinates are drawn in the margins instead.
- Crystal tooltip is static: `(x, y) v pt, phase p`. Owner and streak are read from the colour, the owner emblem and the x2-x4 chip.
- `\` mirror type is shown as U+2572 in texts (a raw backslash breaks SDK text).
