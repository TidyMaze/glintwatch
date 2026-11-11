# Art style guide (common to all contrib games)

Status: generic draft, 2026-11-11. The game theme, sprite list and layout come later, after the game is chosen and the UX/UI phase is done.
Check image: `png/palette-check.png`, made from `src/palette-check.svg`.

## Target
- Canvas 1920x1080 (CodinGame SDK GraphicEntityModule, PixiJS). It is often viewed scaled down to about 960x540, so every key element must still read at **half size**.
- Flat vector shapes with one soft light. No photo textures and no third-party assets (CC0 only, credited in `art/CREDITS.md`).
- Mood: calm dark board, bright player colours. The players are the only saturated thing on screen.

## Player colours (Okabe-Ito, colour-blind safe)
| Player | Main | Dark (owned fill, shadow) | Emblem | Area pattern |
|---|---|---|---|---|
| P1 Sky | `#56B4E9` | `#1F5F86` | ring (circle with dot) | solid |
| P2 Amber | `#E69F00` | `#7A5300` | triangle | 45° stripes |
| P3 Jade | `#009E73` | `#004D38` | square with core | dots |
| P4 Orchid | `#CC79A7` | `#6E3A58` | 4-point star | grid |

- Simulated colour-blindness (Machado matrices, CIELAB): the smallest distance between two player colours is dE 16 (tritan Sky/Jade, deutan Jade/Orchid). That is enough to see a difference but not to identify a player for sure. So **colour is never the only cue**: each player always shows its emblem, and areas also carry their pattern (pattern ink `#0E1018` at 35 % opacity).
- I tried 7 other palettes. None did better (best one: dE 16.4), so we keep the standard values.
- Never use pure yellow or orange for UI states: they would read as Amber.

## Neutrals and states
| Role | Hex |
|---|---|
| Background deep / ink | `#0E1018` |
| Background | `#1A1D29` (gradient to deep, top to bottom) |
| Panel | `#232736` |
| Grid line / border | `#3A4054` |
| Muted text | `#6B7185` |
| Text | `#E8EAF0` |
| Selection / flash | `#F4F6FF` (white outline or glow, never a hue) |
| Danger | `#FF4D4D`, always paired with a shape (cross, crack). It is too close to Jade under protanopia (dE 13). |

## Shapes, lines and light
- Silhouettes are simple and convex. Each unit or piece should be identifiable from its filled shape alone.
- Outline: ink `#0E1018`, stroke 6-8 % of the sprite size (min 2 px at 1920x1080), round joins.
- Light from top-left: a +15 % lighter top face or a highlight, a −25 % darker bottom or a drop shadow (ink at 40 %, offset 2-4 px down). No other light sources.
- Corner radius: 6-10 % of the size for tiles and panels.
- Minimum emblem size on the board: **24 px**. At 12 px the ring and the square merge (seen on the check image).
- Effects (capture, explosion): use the player colour plus white flash, 6-12 frames, additive blend allowed. No new hues.

## Typography
- Helvetica/Arial bold (in the viewer: system sans-serif, so nothing has to be shipped). Names 20 px, scores 30 px in the player colour, labels 13-16 px in muted grey. No text smaller than 13 px at 1920x1080.

## HUD player panel
- 260x96 px, panel `#232736`, rx 10, 3 px border in the player colour, 10 px colour strip on the left.
- Avatar slot: 72x72 at (20,12), rx 8, deep background, 2 px player-colour border. The avatar texture is cropped into it.
- Name: bold 20 px `#E8EAF0` at (104,38). Score: bold 30 px in the player colour at (104,76). Emblem at r 13 in the top-right corner (236,24).
- Eliminated player: panel at 40 % opacity and the emblem crossed out in Danger.
- The 4 panels go in a row or in the screen corners. The final placement is decided in the UX/UI phase.

## Export pipeline (checked locally)
- `sips -s format png in.svg --out out.png` (macOS built-in). It renders at the SVG's own width and height and keeps alpha, gradients, blur filters and text. I checked this on a test SVG.
- No rsvg-convert, inkscape, ImageMagick, cairosvg or PIL on this machine. `qlmanage` scales SVGs inconsistently: **do not use it**.
- `tools/pngcrop.py` is a pure-Python PNG reader/writer for crops and spritesheet assembly.
- Source SVGs have integer `width`/`height` equal to the target pixel size. Put the anchor in the file name or in the sheet JSON (default: centre).
