# UX 15 pre-rendered images (entity budget <= 200): static HUD parts and crystal states baked into single PNGs.
# Coordinates copy referee/src/main/java/com/codingame/game/view/{Viewer,Layout}.java (UX 14 layout), so a baked image
# drops in where the coder's entities were. Writes art/png/*.png (minified). usage: python3 gen_ux15.py
import math, os, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import gen_sheet as G
G.VECTOR = True
PNG = os.path.join(HERE, '..', 'png'); P, INK = G.P, G.INK
EMB = ['ring', 'triangle', 'square', 'star']; PAT = ['beam_solid', 'beam_dash', 'beam_dots', 'beam_double']
PANEL, FRAME, TEXT, LABEL, WHITE, GREY, LOCK_OFF = '#232736', '#3A4054', '#E8EAF0', '#A0A6B8', '#F4F6FF', '#9AA0B4', '#55617A'
OUT = []

def save(name, W, H, body):
    p = os.path.join(PNG, name + '.png'); G.render_to(p, W, H, body); OUT.append(p)

def t36(x, y_top, s, col=TEXT, anchor='start', weight='bold', size=36):  # PIXI Text y = top of the box; SVG wants the baseline
    return G.text(x, y_top + round(size * .80), s, size, col, anchor, weight)

# ---- crystals: 160x160 drawn for a 128 cell (viewer scale = cell/128), one image per state and value
def crystals():
    c = 128; X = Y = 80
    for v in (1, 2, 3):
        size = 128 * [0, .40, .52, .64][v] * c / 92            # crystal.png box, as Viewer (gem 92 of 128)
        for state in ['unlit', 'dormant', 'contested', 'lit0', 'lit1', 'lit2', 'lit3', 'decay0', 'decay1', 'decay2', 'decay3']:
            o = []
            if state == 'dormant': o.append(G.img('crystal_outline', X, Y, size, size))
            elif state == 'unlit': o.append(G.img('crystal', X, Y, size, size, GREY))
            elif state == 'contested':
                o.append(G.img('crystal_glow', X, Y, size * 1.4, size * 1.4, WHITE, alpha=.6))
                o.append(G.img('mirror_pending', X, Y, .9 * c, .9 * c, WHITE))
                o.append(G.img('crystal', X, Y, size, size, WHITE))
                z = .34 * c; o.append(G.img('badge_zero', X + .32 * c, Y - .32 * c, z, z))
            elif state.startswith('decay'):  # UX 17: streak decaying, nobody lights it: unlit (grey) crystal + owner emblem
                s = int(state[-1]); o.append(G.img('crystal', X, Y, size, size, GREY))
                o.append(G.img('emblem_' + EMB[s], X + .30 * c, Y - .30 * c, 60, 60, P[s]))  # UX 17 fix: 100 % (50 % was 2.0-2.6:1)
            else:
                s = int(state[-1]); o.append(G.img('crystal_glow', X, Y, size * 1.4, size * 1.4, P[s], alpha=.6))
                o.append(G.img('crystal', X, Y, size, size, P[s]))
                o.append(G.img('emblem_' + EMB[s], X + .30 * c, Y - .30 * c, 60, 60, P[s]))  # UX: 60 per 128 cell = 24.4 px at N19 (min 24), 31 at N15
            save(f'crystal_{state}_v{v}', 160, 160, ''.join(o))

# ---- coordinate strips per N (labels 36 px #A0A6B8; even indices only at N >= 17, as Layout.coordLabelShown)
def coords():
    for n in (13, 15, 17, 19):
        cell = 1000 // n; W = n * cell
        top = [t36(k * cell + cell / 2, 40 - 2 - 36, str(k), LABEL, 'middle', 'normal') for k in range(n) if n < 17 or k % 2 == 0]
        save(f'coords_top_N{n}', W, 40, ''.join(top))      # place at (x0, y0 - 40)
        left = [t36(37, k * cell + cell / 2 - 18, str(k), LABEL, 'end', 'normal') for k in range(n) if n < 17 or k % 2 == 0]
        save(f'coords_left_N{n}', 42, W, ''.join(left))    # place at (x0 - 6 - 42, y0): fits the 40 px gap after the panels

def box(W, H, extra=''):
    return f'<rect x="1.5" y="1.5" width="{W-3}" height="{H-3}" fill="{PANEL}" stroke="{FRAME}" stroke-width="3"/>' + extra

# ---- legend 380x300 (Viewer.legend: rows y = 14 + 46k, icon centre x 46, word x 84)
def legend():
    o = [box(380, 300)]; words = ['pays', '0 pts', 'asleep', 'pending', 'locked', 'yard']
    for k, w in enumerate(words):
        y = 14 + 46 * k; cy = y + 20; ix = 46
        if k == 0: o.append(G.img('crystal', ix, cy, 40, 40, P[0]))
        elif k == 1: o.append(G.img('badge_zero', ix, cy, 32, 32))
        elif k == 2: o.append(G.img('crystal_outline', ix, cy, 40, 40))
        elif k == 3: o.append(G.img('mirror_pending', ix, cy, 40, 40, P[1]))
        elif k == 4:
            o.append(G.img('mirror_base', ix, cy, 40, 40))
            for a in range(5): o.append(G.img('lock_arc', ix, cy, 40, 40, P[2] if a < 3 else LOCK_OFF, rot=72 * a))
        else: o.append(G.img('zone_tile', ix, cy, 32, 32, P[0], alpha=.5))
        o.append(t36(84, y, w))
    save('legend', 380, 300, ''.join(o))

# ---- board box 380x140 at (40, 470) (UX 16: no turn display; turnbox images retired): 2 icon rows, numbers stay live.
# Row tops y 28 and 84 (Text y), icons 36 px with the left edge at x 28 and the centre at row top + 20, as the old rows.
def boardbox():
    o = [box(380, 140)]
    for y, ic in ((28, 'badge_zero'), (84, 'icon_crystal')): o.append(G.img(ic, 46, y + 20, 36, 36))
    save('boardbox', 380, 140, ''.join(o))

# ---- panels 380x310 per player id (Viewer.PanelView): fill, tinted frame, vertical beam-pattern strip, emblem badge,
# static icons, divider. Avatar, name, score, numbers, action icon and texts stay live. _wood = no supply icon.
def panels():
    W, H = 380, 310
    for s in range(4):
        for suffix, supply in (('', True), ('_wood', False)):
            # fill with a transparent hole at the avatar box (28,24 112x112): the live avatar goes UNDER this image,
            # so the baked emblem badge (which overlaps the avatar corner) stays on top, as in Viewer.PanelView
            o = [f'<path fill-rule="evenodd" fill="{PANEL}" d="M13.5,1.5 H366.5 A12,12 0 0 1 378.5,13.5 V296.5 A12,12 0 0 1 366.5,308.5 H13.5 A12,12 0 0 1 1.5,296.5 V13.5 A12,12 0 0 1 13.5,1.5 Z M28,24 V136 H140 V24 Z"/>',
                 G.img('panel_frame', 0, 0, W, H, P[s], 0, 0)]
            pid = f'ps{s}'
            o.append(f'<pattern id="{pid}" patternUnits="userSpaceOnUse" x="0" y="0" width="16" height="32">'
                     f'<image width="32" height="16" href="{G.uri(PAT[s])}" preserveAspectRatio="none" transform="translate(16,0) rotate(90)"/></pattern>'
                     f'<rect x="0" y="0" width="16" height="{H}" fill="url(#{pid})"{G.tint(P[s])}/>')
            o.append(f'<circle cx="136" cy="132" r="24" fill="{INK}" stroke="{P[s]}" stroke-width="3"/>' + G.img('emblem_' + EMB[s], 136, 132, 30, 30, P[s]))
            o.append(G.img('icon_mirror', 46, 178, 32, 32))
            if supply: o.append(G.img('icon_supply', 158, 178, 32, 32))
            o.append(G.img('icon_crystal', 262, 178, 32, 32, P[s]))
            o.append(f'<rect x="28" y="220" width="324" height="2" fill="{FRAME}"/>')
            save(f'panel_{s}{suffix}', W, H, ''.join(o))

def open_yard():
    W, H = 380, 310
    o = [f'<rect x="4" y="4" width="{W-8}" height="{H-8}" fill="none" stroke="{FRAME}" stroke-width="3" stroke-dasharray="12 8"/>',
         t36(W / 2, H / 2 - 22, 'open yard', LABEL, 'middle')]
    save('open_yard', W, H, ''.join(o))

# ---- lamps 128 (one cell): per player id and firing direction (the emblem stays upright), plus eliminated
def lamps():
    for d, rot in (('E', 0), ('S', 90), ('W', 180), ('N', 270)):
        for s in range(4):
            o = [G.img('lamp_nose', 64, 64, 128, 128, P[s], rot=rot), G.img('lamp_disc', 64, 64, 128, 128, P[s]),
                 G.img('emblem_' + EMB[s], 64, 64, 70, 70, P[s])]
            save(f'lamp_{s}_{d}', 128, 128, ''.join(o))
        o = [G.img('lamp_nose', 64, 64, 128, 128, '#4A5064', rot=rot), G.img('lamp_disc', 64, 64, 128, 128, '#4A5064'),
             G.img('emblem_cross', 64, 64, 70, 70)]
        save(f'lamp_out_{d}', 128, 128, ''.join(o))

# ---- yards, white (tinted): 16 % fill + dashed border at 0.8, drawn at 128 per cell (scale cell/128)
def yards():
    for name, cw, ch in (('yard_h', 5, 3), ('yard_v', 3, 5)):
        W, H = 128 * cw, 128 * ch
        o = [f'<rect width="{W}" height="{H}" fill="#FFFFFF" fill-opacity=".16"/>',
             f'<rect x="3" y="3" width="{W-6}" height="{H-6}" fill="none" stroke="#FFFFFF" stroke-opacity=".8" stroke-width="6" stroke-dasharray="23 15"/>']
        save(name, W, H, ''.join(o))

if __name__ == '__main__':
    crystals(); coords(); legend(); boardbox(); panels(); open_yard(); lamps(); yards()
    subprocess.run(['python3', os.path.join(HERE, '..', '..', 'art-common', 'tools', 'pngmin.py')] + OUT, stdout=subprocess.DEVNULL, check=True)
    print(len(OUT), 'files')
