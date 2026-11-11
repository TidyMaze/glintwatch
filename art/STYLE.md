# Glintwatch art style (concept stage, 2026-11-11)

This file builds on `../art-common/STYLE.md` (palette, emblems, outline and light rules). Final sprite names and sizes will follow `UX.md` once cg-contrib-ux writes it. Until then, the sprite list is the one in DESIGN.md 12.4.

## Mood (chosen: A, "night reef")
- The sea is a radial gradient `#13284A` -> `#060C18` with faint wave strokes `#3B6E9A` at 5-14 % opacity. DESIGN.md uses `#0B1426` as the mean.
- Floor tiles `#1B2D45` / `#18283E` (checker), rx 12 % of a cell, 1 px gap. Rocks `#0A111D` with a 7 % white top bevel.
- Mood B ("violet night", `#22193F`) was tried in `concept/*_B.png`. I rejected it: it is less "reef" and the orchid player blends into it.
- The light is the subject: only beams, lit crystals and lamps glow. Everything else stays matte and dark.

## Readability rules (checked on `concept/mock_viewer_A.png` at 960x540)
- **Crystals have 3 states, told apart by brightness first:**
  - unlit: matte grey-blue (`#8E9BB3` -> `#55617A`);
  - lit by one player: tinted in that player's colour, with a soft halo;
  - contested: bright white, glow, dashed white ring and crack.
  
  In the first draft, unlit crystals were white and looked the same as contested ones at half size. Dimming them fixed it.
- Crystal value is shown by size (62 / 78 / 94 % of the gem box), facet count (5 / 6 / 8) and 1-3 pips. The pips cannot be read at half size, so size carries the value.
- Beams have 3 layers: a glow (3x width, 35 %, blur), the core (`max(4, cell/8)`, 90 %) and a white centre line (30 % width, 70 %).
- **Beam offset:** give a player the same diagonal offset `(d, d)` with `d = (seat-1.5)*w*0.7` on every point. A per-segment perpendicular offset opens a gap at each mirror corner; the constant diagonal offset keeps joints closed and still separates overlapping beams. This is a proposal for cg-contrib-coder and replaces DESIGN.md 12.4's "perpendicular" offset if the team agrees.
- Lamp: a dark body with a lens in the player colour, the emblem on the body and a halo. The zone tint at 16 % opacity is visible for all 4 colours (jade is the weakest).
- Mirrors: a silver bar with a white highlight and an ink outline. It stays neutral, never tinted. The `lastToucher` cue, if any, should be a small coloured pip, not a tint.

## Player order (one mapping everywhere)
Seat 0 is Sky `#56B4E9` with the ring, seat 1 Amber `#E69F00` with the triangle, seat 2 Jade `#009E73` with the square and seat 3 Orchid `#CC79A7` with the 4-point star. The mock and cover use this mapping. UX.md and DESIGN.md 12.2 have to match it: DESIGN.md 12.2 still says orange/circle first and a diamond.

## Sprites v1 (M4, follows ux/UX.md 9 + 12)
- 41 PNGs in `png/`. `png/manifest.json` lists `w, h, anchor, tint` for each one. `spark_sheet`, `flicker_sheet` and `shatter_sheet` come with PIXI spritesheet JSON (4 frames each).
- Board sprites are drawn at 128 px and scaled to `cell`. Tinted sprites are white and are multiplied by `tint`. The parts drawn in ink stay dark under the tint.
- `lamp_disc`: one tint gives both shades. The grey `#5A5A5A` fill becomes the dark player colour and the white ring the main colour.
- `crystal`: the gem is 92 px wide inside the 128 box, so `scale = (0.40|0.52|0.64) * cell / 92`. `crystal_outline` (dormant, `#8E9BB3`, not tinted) uses the same box.
- Panel = `panel_fill` (not tinted) + `panel_frame` (tinted). A single tinted panel would also tint the fill.
- `lock_arc`: 54 deg centred on the top. Rotate the copies by `72*k` for k = 0..4.
- `mirror_crack_1/2`: overlays on `mirror_bar`, with the same rotation.
- `streak_badge` (64x51, drawn at 2x) is the chip for the streak (x2-x4) and the phase timer. Scale it to `1.45*fs x 1.15*fs` with `fs = max(17, round(0.32*cell))`. The tint colours the border and the ink fill stays dark. Tint with the player colour for a streak and `#8E9BB3` for the timer; the text is a Text entity. `streak_badge_dashed` covers the contested case (white) and the decaying one (60 %). Pips were dropped (owner, 2026-11-11). `pending_tick` is 6x12 at display size.
- Deviations from UX.md 9:
  - `zone_border` is 20x8, because dash 12/8 has a period of 20 (32 does not tile).
  - The panel is split into `panel_fill` + `panel_frame`.
  - `highlight_inner` was added for the 3 px outline in the actor's colour.
  - `streak_badge_dashed` was added for contested and decaying streaks.
- Emblem on a lamp: draw it at **0.55*cell**. At 0.42 it is about 14 px at half size and the star reads as a dot.
- Panel 380x300 (UX.md 1.1) replaces the 260x96 draft in `art-common/STYLE.md`.
- Rock: body `#0A111D`, rim and bevel `#6E7590` (3.06:1 on `#1B2D45`), as UX.md 12 accepted.

## Checks run
- `contact-sheet.png` (1920x1080) shows every sprite, tinted the way PIXI tints, on a checker.
- `integration-check.png` (1920x1080) is an N=15 board built from the PNGs themselves, with traced beams, every state in UX.md 12 and the 4 panels.
- `cover.png` (1200x675) is also built from the PNGs.
- I read all three at full size and at 960x540. Two bugs were found and fixed in the sheets:
  - Opposite-direction beams overlapped with a per-segment normal offset. The constant `(d, d)` offset fixes this, as UX.md 12 adopted.
  - The tint was not applied at first: an export quirk, see below.
- sips quirks (CoreSVG):
  - `feColorMatrix` is ignored. Use `feFlood` + `feBlend multiply` + `feComposite in SourceAlpha` instead.
  - Filters on `<g>` are ignored.
  - A filtered image inside a rotated `<g>` turns the wrong way. Put the `transform` and the `filter` on the element itself.
  - `paint-order` is ignored. Draw the stroke text first, then the fill text.

## Files
- `src/gen_sprites.py` generates sprites (SVG in `src/sprites/`, PNG in `png/`). `src/gen_sheet.py` builds the contact sheet, the integration check and the cover from the PNGs.
- `src/gen_mood.py` generates the concept SVGs. The mock scene is traced with the real beam rules (DESIGN.md 4), so the beams are correct.
- `concept/mock_viewer_{A,B}.{svg,png}` are 1920x1080 mocks of the DESIGN.md 12.1 layout.
- `concept/cover_{A,B}.{svg,png}` are 1200x675 cover concepts: 4 beams crossing on a contested crystal.
- Export: `sips -s format png in.svg --out out.png`.
