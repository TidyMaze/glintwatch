# Contact sheet + integration check, composed from the exported PNGs (png/*.png) with PIXI-like multiply tint.
# Writes art/contact-sheet.png (every sprite, tinted variants) and art/integration-check.png (N=15 board per UX.md 1-2).
import base64, math, os, random, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__)); ART = os.path.join(HERE, '..'); PNG = os.path.join(ART, 'png')
sys.path.insert(0, HERE)
from gen_mood import trace
P = ['#56B4E9', '#E69F00', '#009E73', '#CC79A7']; EMB = ['ring', 'triangle', 'square', 'star']; INK = '#0E1018'
PAT = ['beam_solid', 'beam_dash', 'beam_dots', 'beam_double']
_b64 = {}
VECTOR = False  # True: embed the SVG sources (sharp when scaled up, used by the 1920 cover)
def uri(n):
    key = (n, VECTOR)
    if key not in _b64:
        if VECTOR: _b64[key] = 'data:image/svg+xml;base64,' + base64.b64encode(open(os.path.join(HERE, 'sprites', n + '.svg'), 'rb').read()).decode()
        else: _b64[key] = 'data:image/png;base64,' + base64.b64encode(open(os.path.join(PNG, n + '.png'), 'rb').read()).decode()
    return _b64[key]
_filters = {}
def tint(c):
    if c is None: return ''
    if c not in _filters:
        # sips (CoreSVG) ignores feColorMatrix; flood + multiply blend + alpha mask = PIXI tint (checked: white -> exact colour)
        _filters[c] = (f'<filter id="t{c[1:]}" x="0" y="0" width="1" height="1"><feFlood flood-color="{c}" result="f"/>'
                       f'<feBlend in="SourceGraphic" in2="f" mode="multiply" result="m"/><feComposite in="m" in2="SourceAlpha" operator="in"/></filter>')
    return f' filter="url(#t{c[1:]})"'
def img(n, x, y, w, h, c=None, ax=.5, ay=.5, rot=0, alpha=1, extra=''):
    tr = f' transform="rotate({rot} {x:.1f} {y:.1f})"' if rot else ''
    # filter on the outer group: sips mis-rotates a filtered element inside a rotated group (seen: nose pointing W instead of S)
    # sips (CoreSVG): filters on <g> are ignored, and a filtered image inside a rotated <g> turns the wrong way;
    # transform + filter on the <image> itself is correct (checked: lamp_nose rot 90 points S, tinted 230,159,0)
    return (f'<g opacity="{alpha}"><image x="{x - w*ax:.1f}" y="{y - h*ay:.1f}" width="{w:.1f}" height="{h:.1f}" '
            f'href="{uri(n)}" preserveAspectRatio="none"{tr}{tint(c)}{extra}/></g>')
def frame(n, fw, fh, i, x, y, w, h, c=None):  # one frame of a horizontal strip, centred at x,y
    cid = f'cl{abs(hash((n, i, x, y))) % 10**8}'
    s = w / fw
    return (f'<clipPath id="{cid}"><rect x="{x - w/2:.1f}" y="{y - h/2:.1f}" width="{w:.1f}" height="{h:.1f}"/></clipPath>'
            f'<g clip-path="url(#{cid})"><image x="{x - w/2 - fw*i*s:.1f}" y="{y - h/2:.1f}" width="{fw*4*s:.1f}" height="{h:.1f}" href="{uri(n)}"{tint(c)}/></g>')
def text(x, y, s, size, fill='#E8EAF0', anchor='start', weight='bold', style=''):
    return (f'<text x="{x}" y="{y}" font-family="Helvetica Neue, Helvetica" font-weight="{weight}" font-style="{style}" font-size="{size}" '
            f'text-anchor="{anchor}" fill="{fill}">{s}</text>')
def wrap(W, H, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
            f'<defs>{"".join(_filters.values())}</defs>{body}</svg>')
def render_to(pp, W, H, body):
    sp = pp + '.svg'; open(sp, 'w').write(wrap(W, H, body))
    subprocess.run(['sips', '-s', 'format', 'png', sp, '--out', pp], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    os.remove(sp); print(pp)
def render(name, W, H, body):
    sp = os.path.join(HERE, name + '.svg'); pp = os.path.join(ART, name + '.png')
    open(sp, 'w').write(wrap(W, H, body))
    subprocess.run(['sips', '-s', 'format', 'png', sp, '--out', pp], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    os.remove(sp); print(pp)

# ---------------- integration check: N=15 board, rotationally symmetric scene, traced beams
N = 15; cell = 1000 // N; x0 = (1920 - N * cell) // 2; y0 = 40 + (1000 - N * cell) // 2; e = 4
R = lambda p: (N - 1 - p[1], p[0])
def orbit(p):
    out = [p]
    for _ in range(3): out.append(R(out[-1]))
    return out
lamps = orbit((0, e)); dirs = [(1, 0), (0, 1), (-1, 0), (0, -1)]
def zone(s, x, y): return max(abs(x - lamps[s][0]), abs(y - lamps[s][1])) <= 2
rocks, crystals, mirrors = [], [], []
Q = [(3, 2, 'r'), (5, 6, 'c1'), (6, 3, 'c2'), (2, 6, 'c1'), (4, 1, 'c3'), (6, 6, 'c2'), (3, 6, 'm0'), (5, 3, 'm1'), (2, 2, 'c1')]
for (x, y, k) in Q:
    for j, p in enumerate(orbit((x, y))):
        if k == 'r': rocks.append(p)
        elif k[0] == 'c': crystals.append((p[0], p[1], int(k[1])))
        else: mirrors.append((p[0], p[1], int(k[1]) ^ (j % 2)))
crystals.append((7, 7, 3))
crystals.append((6, 2, 2))  # on the amber/jade crossing: contested
mirrors += [(4, 4, 1), (10, 2, 0), (6, 10, 1)]  # placed by players (break the symmetry like a real mid-game)
beams, lit = trace(N, lamps, dirs, rocks, mirrors, crystals)
lock = {(4, 4): (5, 0), (10, 2): (2, 1), (6, 10): (1, 3), (3, 6): (0, None), (8, 6): (3, 2)}
PENDING = (9, 12, 0, 2)  # x, y, type, placer: beams pass
CRACK = {(10, 2): 1, (6, 10): 2}
DORMANT = {(2, 6), (12, 8), (6, 12), (8, 2)}
STREAK = {(6, 3): 3, (6, 2): 2, (6, 6): 4}
TIMER = {(6, 3): '↓'+'2', (6, 6): '↑'+'1'}
C = lambda x, y: (x0 + x * cell + cell / 2, y0 + y * cell + cell / 2)

def board():
    o = [f'<rect x="460" y="40" width="1000" height="1000" rx="10" fill="{INK}" stroke="#3A4054" stroke-width="2"/>']
    for y in range(N):
        for x in range(N):
            o.append(img(f'floor_{(x + y) % 2}', x0 + x * cell, y0 + y * cell, cell, cell, ax=0, ay=0))
    for s in range(4):
        zx, zy = lamps[s]; xa, ya = max(0, zx - 2), max(0, zy - 2); xb, yb = min(N - 1, zx + 2), min(N - 1, zy + 2)
        o.append(img('zone_tile', x0 + xa * cell, y0 + ya * cell, (xb - xa + 1) * cell, (yb - ya + 1) * cell, P[s], 0, 0, alpha=.16))
        o.append(f'<rect x="{x0 + xa*cell + 1.5}" y="{y0 + ya*cell + 1.5}" width="{(xb-xa+1)*cell - 3}" height="{(yb-ya+1)*cell - 3}" fill="none" '
                 f'stroke="{P[s]}" stroke-opacity=".8" stroke-width="3" stroke-dasharray="12 8"/>')
    for (x, y) in rocks: o.append(img('rock', *C(x, y), cell, cell))
    w = max(7, round(.14 * cell))
    for s, pts in beams.items():  # glow Line + TilingSprite core per segment, lane offset perpendicular
        off = (s - 1.5) * (w + 2)
        for (ax_, ay_), (bx, by) in zip(pts, pts[1:]):
            (X1, Y1), (X2, Y2) = C(ax_, ay_), C(bx, by)
            L = math.hypot(X2 - X1, Y2 - Y1); ang = math.degrees(math.atan2(Y2 - Y1, X2 - X1))
            nx, ny = off, off  # UX 12: constant diagonal (d, d) per player; a per-segment normal flips with direction
            o.append(f'<line x1="{X1+nx:.1f}" y1="{Y1+ny:.1f}" x2="{X2+nx:.1f}" y2="{Y2+ny:.1f}" stroke="{P[s]}" stroke-opacity=".22" stroke-width="{2*w+6}" stroke-linecap="round"/>')
            pid = f'p{s}_{int(X1)}_{int(Y1)}'
            o.append(f'<pattern id="{pid}" patternUnits="userSpaceOnUse" x="0" y="{-w/2}" width="{32*w/16:.2f}" height="{w}"><image width="{32*w/16:.2f}" height="{w}" href="{uri(PAT[s])}" preserveAspectRatio="none"/></pattern>'
                     f'<rect x="0" y="{-w/2}" width="{L:.1f}" height="{w}" fill="url(#{pid})" transform="translate({X1+nx:.1f},{Y1+ny:.1f}) rotate({ang:.1f})"{tint(P[s])}/>')
    for (x, y, v, own) in lit:
        X, Y = C(x, y); gw = [0, .40, .52, .64][v] * cell; sz = 128 * gw / 92
        if (x, y) in DORMANT:
            o.append(img('crystal_outline', X, Y, sz, sz) + text(X, Y + .11 * cell, str(v), round(.30 * cell), '#8E9BB3', 'middle')); continue
        if isinstance(own, int): o.append(img('crystal_glow', X, Y, cell * 1.3, cell * 1.3, P[own], alpha=.6))
        if own == 'X':
            o.append(img('crystal_glow', X, Y, cell * 1.3, cell * 1.3, '#F4F6FF', alpha=.7))
            o.append(f'<circle cx="{X}" cy="{Y}" r="{cell*.44:.1f}" fill="none" stroke="#F4F6FF" stroke-width="3" stroke-dasharray="7 5"/>')
            o.append(frame('flicker_sheet', 128, 128, 1, X, Y, cell * 1.05, cell * 1.05))
        o.append(img('crystal', X, Y, sz, sz, P[own] if isinstance(own, int) else ('#F4F6FF' if own == 'X' else '#9AA0B4')))
        o.append(text(X, Y + .11 * cell, str(v), round(.30 * cell), INK, 'middle'))
        if isinstance(own, int): o.append(img('emblem_' + EMB[own], X + cell * .3, Y - cell * .3, 24, 24, P[own]))
        if own == 'X': o.append(img('badge_zero', X + cell * .3, Y - cell * .3, 24, 24))
        fs = max(17, round(.32 * cell)); bw, bh = 1.45 * fs, 1.15 * fs; by_ = Y + cell / 2 - bh / 2 - 1
        if (x, y) in STREAK and own is not None:
            k_ = STREAK[(x, y)]; c_ = P[own] if isinstance(own, int) else '#F4F6FF'; bx_ = X + cell / 2 - bw / 2 - 1
            o.append(img('streak_badge_dashed' if own == 'X' else 'streak_badge', bx_, by_, bw, bh, c_) + text(bx_, by_ + fs * .37, f'x{k_}', fs, c_, 'middle'))
        if (x, y) in TIMER:
            bx_ = X - cell / 2 + bw / 2 + 1
            o.append(img('streak_badge', bx_, by_, bw, bh, '#8E9BB3') + text(bx_, by_ + fs * .37, TIMER[(x, y)], fs, '#F4F6FF', 'middle'))
    for (x, y, t) in mirrors:
        X, Y = C(x, y); o.append(img('mirror_base', X, Y, cell, cell)); o.append(img('mirror_bar', X, Y, cell, cell, rot=90 * t))
        n, who = lock.get((x, y), (0, None))
        if (x, y) in CRACK: o.append(img(f'mirror_crack_{CRACK[(x, y)]}', X, Y, cell, cell, rot=90 * t))
        if n:  # UX review 2: nothing at lock 0
            for k in range(5):
                o.append(img('lock_arc', X, Y, cell, cell, P[who] if (k < n and who is not None) else '#55617A', rot=72 * k))
    X, Y = C(*PENDING[:2]); s_ = PENDING[3]
    o.append(img('mirror_base', X, Y, cell, cell) + img('mirror_bar', X, Y, cell, cell, rot=90 * PENDING[2], alpha=.4) + img('mirror_pending', X, Y, cell, cell, P[s_]))
    o.append(img('emblem_' + EMB[s_], X - cell * .34, Y - cell * .34, 20, 20, P[s_]))
    for k in range(2): o.append(img('pending_tick', X + cell * .26 + k * 8, Y + cell * .3, 6, 12))
    X, Y = C(12, 5); o.append(img('mirror_base', X, Y, cell, cell, alpha=.5)); o.append(frame('shatter_sheet', 128, 128, 1, X, Y, cell, cell))
    X, Y = C(4, 4); o.append(img('highlight_cell', X, Y, cell, cell)); o.append(img('highlight_inner', X, Y, cell, cell, P[0]))
    o.append(img('emblem_ring', X - cell * .32, Y - cell * .32, 22, 22, P[0]))
    for s in range(4):
        X, Y = C(*lamps[s]); o.append(img('lamp_disc', X, Y, cell, cell, P[s])); o.append(img('lamp_nose', X, Y, cell, cell, P[s], rot=90 * s))
        o.append(img('emblem_' + EMB[s], X, Y, cell * .55, cell * .55, P[s]))  # 0.55 cell: 0.42 was ~14 px at half size
    X, Y = C(*lamps[3]); o.append(frame('spark_sheet', 64, 64, 1, X + cell * .5, Y, cell * .9, cell * .9))
    return ''.join(o)

def panel(s, px, py, name, score, gain, stock, litn, act, msg, bad=False):
    c = P[s]; o = [img('panel_fill', px, py, 380, 300, ax=0, ay=0), img('panel_frame', px, py, 380, 300, c, 0, 0)]
    o.append(f'<rect x="{px+28}" y="{py+24}" width="112" height="112" rx="10" fill="{INK}" stroke="{c}" stroke-width="3"/>' + text(px + 84, py + 86, 'avatar', 18, '#6B7185', 'middle', 'normal'))
    o.append(f'<circle cx="{px+136}" cy="{py+132}" r="24" fill="{INK}" stroke="{c}" stroke-width="3"/>' + img('emblem_' + EMB[s], px + 136, py + 132, 30, 30, c))
    o.append(text(px + 156, py + 56, name, 30) + text(px + 172, py + 130, score, 72, c) + text(px + 356, py + 128, f'+{gain}', 36, '#F4F6FF', 'end'))
    o.append(img('icon_mirror', px + 44, py + 182, 30, 30) + text(px + 66, py + 193, stock, 32) + text(px + 66 + 22 * len(stock), py + 193, '/6 hand', 22, '#A0A6B8', weight='normal'))
    o.append(img('icon_crystal', px + 232, py + 182, 30, 30, c) + text(px + 254, py + 193, litn, 32) + text(px + 276, py + 193, 'lit', 22, '#A0A6B8', weight='normal'))
    o.append(f'<rect x="{px+28}" y="{py+213}" width="324" height="2" fill="#3A4054"/>')
    if bad: o.append(img('icon_refused', px + 42, py + 243, 28, 28) + text(px + 64, py + 252, act, 28, '#FF4D4D'))
    else: o.append(text(px + 28, py + 252, act, 28))
    o.append(text(px + 28, py + 284, msg, 24, '#A0A6B8', weight='normal', style='italic'))
    return ''.join(o)

def integration():
    o = [img('background', 0, 0, 1920, 1080, ax=0, ay=0), board()]
    o.append(panel(0, 40, 40, 'NeonKeeper', '142', 5, '3', '3', 'PLACE 4 4 \\', '"holding col 7"'))
    o.append(panel(1, 1500, 40, 'amber_bot', '131', 5, '2', '3', 'WAIT', ''))
    o.append(panel(2, 1500, 740, 'jade.v3', '97', 4, '6', '1', 'refused: zone', '"strike!"', bad=True))
    o.append(panel(3, 40, 740, 'OrchidAI', '118', 4, '4', '2', 'TURN 6 10', '"north road"'))
    o.append(img('panel_fill', 40, 380, 380, 320, ax=0, ay=0) + text(68, 430, 'TURN', 24, '#A0A6B8', weight='normal') + text(68, 510, '37 / 150', 72))
    o.append(img('panel_fill', 1500, 380, 380, 320, ax=0, ay=0) + text(1528, 430, 'LEGEND', 24, '#A0A6B8', weight='normal'))
    for i, p in enumerate(PAT):
        o.append(f'<pattern id="lg{i}" patternUnits="userSpaceOnUse" x="{1528 + i*84}" y="620" width="20" height="10"><image width="20" height="10" href="{uri(p)}" preserveAspectRatio="none"/></pattern>'
                 f'<rect x="{1528 + i*84}" y="{620}" width="70" height="10" fill="url(#lg{i})"{tint(P[i])}/>')
    o.append(text(960, 1072, 'SPRITES CHECK ONLY: panel, turn box and legend layout follow ux/UX.md 1.1 and 12, not this image', 16, '#A0A6B8', 'middle', 'normal'))
    render('integration-check', 1920, 1080, ''.join(o))

def contact():
    o = ['<rect width="1920" height="1080" fill="#14161F"/>']
    for y in range(0, 1080, 24):
        for x in range(0, 1920, 24):
            if (x // 24 + y // 24) % 2: o.append(f'<rect x="{x}" y="{y}" width="24" height="24" fill="#1B1E2A"/>')
    o.append(text(40, 52, 'Glintwatch sprites v1: art/png (checker = transparent; tinted rows use the 4 player colours)', 28))
    o.append(img('background', 40, 80, 384, 216, ax=0, ay=0) + text(40, 318, 'background 1920x1080', 18, '#A0A6B8', weight='normal'))
    o.append(img('logo', 470, 80, 450, 150, ax=0, ay=0) + text(470, 318, 'logo 600x200', 18, '#A0A6B8', weight='normal'))
    o.append(img('panel_fill', 960, 80, 253, 200, ax=0, ay=0) + img('panel_frame', 960, 80, 253, 200, P[0], 0, 0) + text(960, 318, 'panel_fill + panel_frame (tinted)', 18, '#A0A6B8', weight='normal'))
    o.append(img('spark_sheet', 1260, 90, 512, 128, ax=0, ay=0) + text(1260, 240, 'spark_sheet 4x64', 18, '#A0A6B8', weight='normal'))
    o.append(img('flicker_sheet', 1260, 250, 512, 128, ax=0, ay=0, alpha=1) + text(1780, 318, 'flicker_sheet 4x128', 18, '#A0A6B8', 'end', 'normal'))
    row = [('floor_0', None), ('floor_1', None), ('rock', None), ('crystal', '#9AA0B4'), ('crystal', P[1]), ('crystal', '#F4F6FF'), ('crystal_glow', P[0]),
           ('mirror_base', None), ('mirror_bar', None), ('lock_arc', P[2]), ('highlight_cell', None), ('highlight_inner', P[3]), ('lamp_disc', P[0]), ('lamp_nose', P[0]), ('zone_tile', P[2])]
    for i, (n, c) in enumerate(row):
        X = 40 + i * 124; o.append(img(n, X, 400, 110, 110, c, 0, 0) + text(X, 530, n, 15, '#A0A6B8', weight='normal'))
    for s in range(4):  # emblem + lamp assembled at 3 sizes, per player
        X = 40 + s * 300
        for k, sz in enumerate((96, 66, 52)):
            xx = X + [0, 110, 190][k] + sz / 2
            o.append(img('lamp_disc', xx, 620, sz, sz, P[s]) + img('lamp_nose', xx, 620, sz, sz, P[s], rot=90 * s) + img('emblem_' + EMB[s], xx, 620, sz * .42, sz * .42, P[s]))
        for k, sz in enumerate((48, 24)): o.append(img('emblem_' + EMB[s], X + 30 + k * 60, 720, sz, sz, P[s]))
        o.append(img('emblem_cross', X + 150, 720, 48, 48) + img('emblem_' + EMB[s], X + 210, 720, 48, 48, '#4A5064') + img('emblem_cross', X + 210, 720, 40, 40))
    o.append(text(1240, 600, 'lamps at cell 96/66/52, emblems 48/24, eliminated', 18, '#A0A6B8', weight='normal'))
    for s, p in enumerate(PAT):
        o.append(f'<pattern id="cs{s}" patternUnits="userSpaceOnUse" x="40" y="{780 + s*36}" width="32" height="16"><image width="32" height="16" href="{uri(p)}"/></pattern>'
                 f'<rect x="40" y="{780 + s*36}" width="600" height="16" fill="url(#cs{s})"{tint(P[s])}/>' + text(660, 794 + s * 36, p + ' 32x16', 18, '#A0A6B8', weight='normal'))
    o.append(f'<pattern id="zb" patternUnits="userSpaceOnUse" x="40" y="930" width="20" height="8"><image width="20" height="8" href="{uri("zone_border")}"/></pattern><rect x="40" y="930" width="600" height="8" fill="url(#zb)"{tint(P[1])}/>' + text(660, 940, 'zone_border 20x8', 18, '#A0A6B8', weight='normal'))
    for i, (n, c) in enumerate([('badge_zero', None), ('icon_mirror', None), ('icon_crystal', P[3]), ('icon_refused', None)]):
        o.append(img(n, 900 + i * 110, 800, 64, 64, c, 0, 0) + text(900 + i * 110, 885, n, 15, '#A0A6B8', weight='normal'))
    for i, (n, c) in enumerate([('mirror_pending', P[1]), ('mirror_crack_1', None), ('mirror_crack_2', None), ('crystal_outline', None), ('streak_badge', P[2]), ('streak_badge_dashed', '#F4F6FF'), ('streak_badge', '#8E9BB3'), ('pending_tick', None)]):
        X = 40 + i * 124; o.append(img('floor_0', X, 960, 96, 96, ax=0, ay=0))
        if n.startswith('mirror_crack'): o.append(img('mirror_bar', X + 48, 1008, 96, 96))
        w_, h_ = {'streak_badge': (64, 51), 'streak_badge_dashed': (64, 51), 'pending_tick': (12, 24)}.get(n, (96, 96))
        o.append(img(n, X + 48, 1008, w_, h_, c) + text(X + 48, 1074, n.replace('streak_badge_dashed', 'badge_dashed').replace('streak_badge', 'badge'), 14, '#A0A6B8', 'middle', 'normal'))
    o.append(img('shatter_sheet', 1050, 960, 384, 96, ax=0, ay=0) + text(1050, 1074, 'shatter_sheet 4x128', 14, '#A0A6B8', weight='normal'))
    # crystal sizes on floor, as on the board at cell 66
    for v in (1, 2, 3):
        X = 1380 + v * 120; o.append(img('floor_0', X - 33, 815, 66, 66, ax=0, ay=0)); sz = 128 * [0, .40, .52, .64][v] * 66 / 92
        o.append(img('crystal', X, 848, sz, sz, '#9AA0B4') + text(X, 848 + 7, str(v), 20, INK, 'middle'))
    o.append(text(1420, 910, 'crystal values 1-3 at cell 66', 18, '#A0A6B8', weight='normal'))
    o.append(img('floor_1', 1500, 960, 96, 96, ax=0, ay=0) + img('rock', 1548, 1008, 96, 96) + text(1500, 1074, 'rock on floor_1', 14, '#A0A6B8', weight='normal'))
    render('contact-sheet', 1920, 1080, ''.join(o))

def cover(W=1200, H=675, name='cover'):
    Wout, Hout = W, H; W, H = 1200, 675  # composed at 1200x675, scaled as a whole
    c = 96; ox, oy = -24, -70; Cc = lambda x, y: (ox + x * c + c / 2, oy + y * c + c / 2)
    o = [img('background', 0, 0, 1200, 675, ax=0, ay=0)]
    for y in range(0, 8):
        for x in range(0, 14):
            o.append(img(f'floor_{(x + y) % 2}', ox + x * c, oy + y * c, c, c, ax=0, ay=0))
    o.append(img('zone_tile', ox + 0 * c, oy + 1 * c, 3 * c, 3 * c, P[0], 0, 0, alpha=.16))
    rocks = [(9, 1), (3, 5)]; mir = [(5, 2, 1), (10, 2, 0), (10, 5, 1), (2, 3, 0)]  # traced by hand: sky E->S at (5,2); jade W->N at (10,5), N->E at (10,2)
    for (x, y) in rocks: o.append(img('rock', *Cc(x, y), c, c))
    w = 14
    paths = {0: [(0, 2), (5, 2), (5, 8.6)], 1: [(7, -1.6), (7, 8.6)], 2: [(14.6, 5), (10, 5), (10, 2), (14.6, 2)], 3: [(-0.6, 4), (14.6, 4)]}
    for s, pts in paths.items():
        off = (s - 1.5) * (w + 2)
        for (a1, b1), (a2, b2) in zip(pts, pts[1:]):
            (X1, Y1), (X2, Y2) = Cc(a1, b1), Cc(a2, b2); X1 += off; Y1 += off; X2 += off; Y2 += off
            L = math.hypot(X2 - X1, Y2 - Y1); ang = math.degrees(math.atan2(Y2 - Y1, X2 - X1))
            o.append(f'<line x1="{X1:.1f}" y1="{Y1:.1f}" x2="{X2:.1f}" y2="{Y2:.1f}" stroke="{P[s]}" stroke-opacity=".28" stroke-width="{2*w+10}" stroke-linecap="round"/>')
            pid = f'cv{s}_{int(X1)}_{int(Y1)}'
            if s == 0:  # solid: plain fill (a tiled pattern shows seams once the cover is scaled to 1920)
                o.append(f'<rect x="0" y="{-w/2}" width="{L:.1f}" height="{w}" fill="{P[0]}" transform="translate({X1:.1f},{Y1:.1f}) rotate({ang:.1f})"/>'); continue
            o.append(f'<pattern id="{pid}" patternUnits="userSpaceOnUse" x="0" y="{-w/2}" width="{2*w}" height="{w}"><image width="{2*w}" height="{w}" href="{uri(PAT[s])}" preserveAspectRatio="none"/></pattern>'
                     f'<rect x="0" y="{-w/2}" width="{L:.1f}" height="{w}" fill="url(#{pid})" transform="translate({X1:.1f},{Y1:.1f}) rotate({ang:.1f})"{tint(P[s])}/>')
    gems = [(7, 4, 3, 'X'), (7, 1, 2, 1), (12, 4, 1, 3), (3, 2, 1, 0), (5, 3, 2, 0), (12, 2, 2, 2), (9, 3, 1, None), (2, 1, 2, None), (3, 4, 1, 3)]
    for (x, y, v, own) in gems:
        X, Y = Cc(x, y); sz = 128 * [0, .40, .52, .64][v] * c / 92
        if own == 'X':
            o.append(img('crystal_glow', X, Y, c * 2.2, c * 2.2, '#F4F6FF', alpha=.9) + frame('flicker_sheet', 128, 128, 2, X, Y, c * 1.3, c * 1.3))
            o.append(f'<circle cx="{X}" cy="{Y}" r="{c*.5:.1f}" fill="none" stroke="#F4F6FF" stroke-width="4" stroke-dasharray="10 7"/>')
        elif own is not None: o.append(img('crystal_glow', X, Y, c * 1.5, c * 1.5, P[own], alpha=.7))
        o.append(img('crystal', X, Y, sz, sz, P[own] if isinstance(own, int) else ('#F4F6FF' if own == 'X' else '#9AA0B4')))
    for (x, y, t_) in mir:
        X, Y = Cc(x, y); o.append(img('mirror_base', X, Y, c, c) + img('mirror_bar', X, Y, c, c, rot=90 * t_))
    X, Y = Cc(0, 2); o.append(img('lamp_disc', X, Y, c, c, P[0]) + img('lamp_nose', X, Y, c, c, P[0]) + img('emblem_ring', X, Y, c * .55, c * .55, P[0]))
    o.append(f'<rect x="0" y="{H-170}" width="{W}" height="170" fill="#060B14" opacity=".78"/>')
    o.append(img('logo', 330, H - 92, 600, 200))
    o.append(text(330, H - 22, 'Bend the light. Hold the crystals. Four lamps, one reef.', 24, '#AFC0DA', 'middle', 'normal'))
    for i in range(4): o.append(img('emblem_' + EMB[i], 780 + i * 100, H - 88, 56, 56, P[i]))
    body = ''.join(o)
    if Wout != 1200: body = f'<g transform="scale({Wout/1200})">{body}</g>'
    render(name, Wout, Hout, body)

if __name__ == '__main__':
    contact(); integration(); cover()
    VECTOR = True; cover(1920, 1080, 'cover-1920')
