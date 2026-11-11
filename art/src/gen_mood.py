# Mood / cover concept generator (Glintwatch). Writes SVGs to art/concept/. Original art, no references copied.
import math, os, random
OUT = os.path.join(os.path.dirname(__file__), '..', 'concept')
P = ['#56B4E9', '#E69F00', '#009E73', '#CC79A7']   # seat 0..3 = Sky ring, Amber triangle, Jade square, Orchid star (art-common/STYLE.md)
MOODS = {'A': dict(bg0='#13284A', bg1='#060C18', floor0='#1B2D45', floor1='#18283E', rock='#0A111D', edge='#2A4363', wave='#3B6E9A'),
         'B': dict(bg0='#22193F', bg1='#090714', floor0='#251F3D', floor1='#211B37', rock='#0C0A16', edge='#3E3560', wave='#6A5AA8')}

def defs(m):
    g = [f'<radialGradient id="sea" cx="50%" cy="45%" r="75%"><stop offset="0" stop-color="{m["bg0"]}"/><stop offset="1" stop-color="{m["bg1"]}"/></radialGradient>',
         '<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>',
         '<filter id="glow2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>',
         '<linearGradient id="silver" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F4F7FF"/><stop offset=".5" stop-color="#AEB8CC"/><stop offset="1" stop-color="#E2E8F5"/></linearGradient>',
         '<linearGradient id="gem" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8E9BB3"/><stop offset="1" stop-color="#55617A"/></linearGradient><linearGradient id="gemw" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#DDE6F5"/></linearGradient>']
    for i, c in enumerate(P):
        g.append(f'<radialGradient id="halo{i}"><stop offset="0" stop-color="{c}" stop-opacity=".9"/><stop offset="1" stop-color="{c}" stop-opacity="0"/></radialGradient>')
    return '<defs>' + ''.join(g) + '</defs>'

def waves(m, W, H, n=26, seed=3):
    r = random.Random(seed); o = []
    for k in range(n):
        y = r.uniform(0, H); x = r.uniform(-200, W); L = r.uniform(120, 360); a = r.uniform(.05, .14)
        o.append(f'<path d="M{x:.0f},{y:.0f} q{L/4:.0f},-10 {L/2:.0f},0 t{L/2:.0f},0" fill="none" stroke="{m["wave"]}" stroke-opacity="{a:.2f}" stroke-width="3" stroke-linecap="round"/>')
    return ''.join(o)

def crystal(cx, cy, s, v, tint=None, contested=False):
    # faceted gem; size and facet count grow with value
    k = [0, .62, .78, .94][v]; r = s * .42 * k; n = [0, 5, 6, 8][v]
    fill = tint or ('url(#gemw)' if contested else 'url(#gem)')
    pts = [(cx + r * math.cos(2 * math.pi * i / n - math.pi / 2), cy + r * 1.12 * math.sin(2 * math.pi * i / n - math.pi / 2)) for i in range(n)]
    poly = ' '.join(f'{x:.1f},{y:.1f}' for x, y in pts)
    o = ''
    if tint: o += f'<circle cx="{cx}" cy="{cy}" r="{s*.55:.1f}" fill="{tint}" opacity=".55" filter="url(#glow)"/>'
    if contested: o += f'<circle cx="{cx}" cy="{cy}" r="{s*.55:.1f}" fill="#FFFFFF" opacity=".8" filter="url(#glow)"/><circle cx="{cx}" cy="{cy}" r="{s*.47:.1f}" fill="none" stroke="#FFFFFF" stroke-width="{max(2,s*.04):.1f}" stroke-dasharray="{s*.1:.1f} {s*.08:.1f}"/>'
    o += f'<polygon points="{poly}" fill="{fill}" stroke="#0A0F1A" stroke-width="{max(1.5, s*.04):.1f}" stroke-linejoin="round"/>'
    # facets: lines from centre-top to vertices, light top-left
    o += f'<polygon points="{pts[0][0]:.1f},{pts[0][1]:.1f} {cx:.1f},{cy:.1f} {pts[-1][0]:.1f},{pts[-1][1]:.1f}" fill="#FFFFFF" opacity=".55"/>'
    o += f'<polygon points="{pts[n//2][0]:.1f},{pts[n//2][1]:.1f} {cx:.1f},{cy:.1f} {pts[n//2-1][0]:.1f},{pts[n//2-1][1]:.1f}" fill="#000" opacity=".22"/>'
    for i in range(v):  # value pips
        o += f'<circle cx="{cx - (v-1)*s*.07 + i*s*.14:.1f}" cy="{cy + s*.36:.1f}" r="{s*.045:.1f}" fill="#F4F6FF" stroke="#0A0F1A" stroke-width="1"/>'
    if contested:
        o += f'<path d="M{cx-r*.9:.1f},{cy-r*.2:.1f} l{r*.5:.1f},{r*.25:.1f} l{r*.3:.1f},-{r*.4:.1f} l{r*.6:.1f},{r*.5:.1f}" stroke="#FFFFFF" stroke-width="{max(2, s*.04):.1f}" fill="none" stroke-linecap="round"/>'
    return o

def mirror(cx, cy, s, t):
    a = -45 if t == 0 else 45
    return (f'<g transform="translate({cx},{cy}) rotate({a})">'
            f'<rect x="{-s*.48:.1f}" y="{-s*.07:.1f}" width="{s*.96:.1f}" height="{s*.14:.1f}" rx="{s*.05:.1f}" fill="url(#silver)" stroke="#0A0F1A" stroke-width="{max(1.5,s*.035):.1f}"/>'
            f'<rect x="{-s*.44:.1f}" y="{-s*.05:.1f}" width="{s*.88:.1f}" height="{s*.03:.1f}" fill="#FFFFFF" opacity=".9"/></g>')

def emblem(i, cx, cy, r, c, ink='#0A0F1A'):
    s = f'fill="{c}" stroke="{ink}" stroke-width="{max(1.5, r*.16):.1f}" stroke-linejoin="round"'
    if i == 0: return f'<circle cx="{cx}" cy="{cy}" r="{r*.9:.1f}" {s}/><circle cx="{cx}" cy="{cy}" r="{r*.35:.1f}" fill="{ink}"/>'
    if i == 1: return f'<path d="M{cx},{cy-r} L{cx+r*.95:.1f},{cy+r*.75:.1f} L{cx-r*.95:.1f},{cy+r*.75:.1f} Z" {s}/>'
    if i == 2: return f'<rect x="{cx-r*.8:.1f}" y="{cy-r*.8:.1f}" width="{r*1.6:.1f}" height="{r*1.6:.1f}" rx="{r*.15:.1f}" {s}/><rect x="{cx-r*.3:.1f}" y="{cy-r*.3:.1f}" width="{r*.6:.1f}" height="{r*.6:.1f}" fill="{ink}"/>'
    pts = ' '.join(f'{cx+(r if k%2==0 else r*.42)*math.cos(k*math.pi/4-math.pi/2):.1f},{cy+(r if k%2==0 else r*.42)*math.sin(k*math.pi/4-math.pi/2):.1f}' for k in range(8))
    return f'<polygon points="{pts}" {s}/>'

def lamp(i, cx, cy, s, ang):
    c = P[i]
    return (f'<circle cx="{cx}" cy="{cy}" r="{s*.75:.1f}" fill="url(#halo{i})"/>'
            f'<g transform="translate({cx},{cy}) rotate({ang})">'
            f'<rect x="{-s*.42:.1f}" y="{-s*.36:.1f}" width="{s*.62:.1f}" height="{s*.72:.1f}" rx="{s*.12:.1f}" fill="#2A3346" stroke="#0A0F1A" stroke-width="{s*.05:.1f}"/>'
            f'<path d="M{s*.18:.1f},{-s*.3:.1f} L{s*.48:.1f},{-s*.2:.1f} L{s*.48:.1f},{s*.2:.1f} L{s*.18:.1f},{s*.3:.1f} Z" fill="{c}" stroke="#0A0F1A" stroke-width="{s*.05:.1f}" stroke-linejoin="round"/>'
            f'<rect x="{s*.38:.1f}" y="{-s*.16:.1f}" width="{s*.08:.1f}" height="{s*.32:.1f}" fill="#FFFFFF" opacity=".85"/></g>'
            + emblem(i, cx - s*.12*math.cos(math.radians(ang)), cy - s*.12*math.sin(math.radians(ang)), s*.2, c))

def beam(pts, i, w):
    c = P[i]; d = 'M' + ' L'.join(f'{x:.1f},{y:.1f}' for x, y in pts)
    return (f'<path d="{d}" fill="none" stroke="{c}" stroke-width="{w*3:.1f}" stroke-opacity=".35" stroke-linejoin="round" filter="url(#glow)"/>'
            f'<path d="{d}" fill="none" stroke="{c}" stroke-width="{w:.1f}" stroke-opacity=".9" stroke-linejoin="round" stroke-linecap="round"/>'
            f'<path d="{d}" fill="none" stroke="#FFFFFF" stroke-width="{w*.3:.1f}" stroke-opacity=".7" stroke-linejoin="round"/>')


def trace(N, lamps, dirs, rocks, mirrors, crystals):
    """Return beams {seat: [cell points]} and crystal owners following DESIGN.md 4 (walls stop, mirrors bend, lamps stop)."""
    M = {(x, y): t for x, y, t in mirrors}; R = set(rocks); L = {p: s for s, p in enumerate(lamps)}
    Cr = {(x, y) for x, y, _ in crystals}; lit = {}; beams = {}
    for s, ((x, y), (dx, dy)) in enumerate(zip(lamps, dirs)):
        pts = [(x, y)]; seen = set()
        while True:
            x += dx; y += dy
            if not (0 <= x < N and 0 <= y < N): pts.append((x - dx * .5, y - dy * .5)); break
            if (x, y) in R: pts.append((x - dx * .45, y - dy * .45)); break
            if (x, y) in L: pts.append((x, y)); break
            if (x, y, dx, dy) in seen: break
            seen.add((x, y, dx, dy))
            if (x, y) in Cr: lit.setdefault((x, y), set()).add(s)
            if (x, y) in M:
                pts.append((x, y))
                dx, dy = ((-dy, -dx) if M[(x, y)] == 0 else (dy, dx))
        beams[s] = pts
    own = {}
    for c, ss in lit.items(): own[c] = next(iter(ss)) if len(ss) == 1 else 'X'
    return beams, [(x, y, v, own.get((x, y))) for x, y, v in crystals]

def board(m, N, cell, x0, y0, scene):
    o = [f'<rect x="{x0-10}" y="{y0-10}" width="{N*cell+20}" height="{N*cell+20}" rx="18" fill="{m["edge"]}" opacity=".7"/>']
    for y in range(N):
        for x in range(N):
            f = m['floor0'] if (x + y) % 2 == 0 else m['floor1']
            o.append(f'<rect x="{x0+x*cell+1}" y="{y0+y*cell+1}" width="{cell-2}" height="{cell-2}" rx="{cell*.12:.1f}" fill="{f}"/>')
    C = lambda x, y: (x0 + x * cell + cell / 2, y0 + y * cell + cell / 2)
    for s, (zx, zy) in enumerate(scene['lamps']):
        for y in range(N):
            for x in range(N):
                if max(abs(x - zx), abs(y - zy)) <= 2:
                    o.append(f'<rect x="{x0+x*cell+1}" y="{y0+y*cell+1}" width="{cell-2}" height="{cell-2}" rx="{cell*.12:.1f}" fill="{P[s]}" opacity=".16"/>')
    for (x, y) in scene['rocks']:
        cx, cy = C(x, y); o.append(f'<rect x="{cx-cell*.42:.1f}" y="{cy-cell*.42:.1f}" width="{cell*.84:.1f}" height="{cell*.84:.1f}" rx="{cell*.16:.1f}" fill="{m["rock"]}" stroke="#04070D" stroke-width="2"/><rect x="{cx-cell*.34:.1f}" y="{cy-cell*.38:.1f}" width="{cell*.5:.1f}" height="{cell*.12:.1f}" rx="3" fill="#FFFFFF" opacity=".07"/>')
    w = max(4, cell / 8)
    for i, path in scene['beams'].items():
        off = (i - 1.5) * w * .7
        pts = [(C(x, y)[0] + off, C(x, y)[1] + off) for x, y in path]
        o.append(beam(pts, i, w))
    for (x, y, v, own) in scene['crystals']:
        cx, cy = C(x, y)
        o.append(crystal(cx, cy, cell, v, tint=P[own] if isinstance(own, int) else None, contested=own == 'X'))
    for (x, y, t) in scene['mirrors']:
        cx, cy = C(x, y); o.append(mirror(cx, cy, cell, t))
    angs = [0, 90, 180, 270]
    for s, (x, y) in enumerate(scene['lamps']):
        cx, cy = C(x, y); o.append(lamp(s, cx, cy, cell, angs[s]))
    return ''.join(o)

# N=13 demo scene, traced with the real rules. Lamps per DESIGN.md example: P0 (0,3) E, P1 (9,0) S, P2 (12,9) W, P3 (3,12) N.
_L = [(0, 3), (9, 0), (12, 9), (3, 12)]; _D = [(1, 0), (0, 1), (-1, 0), (0, -1)]
_R = [(5, 10), (2, 7), (10, 5), (7, 2), (10, 10)]
_M = [(5, 3, 1), (9, 6, 0), (5, 8, 0), (7, 9, 0), (3, 6, 1), (8, 11, 0)]
_C = [(5, 5, 2), (8, 6, 1), (7, 6, 3), (5, 6, 3), (3, 4, 1), (3, 9, 1), (6, 3, 1), (4, 4, 3), (8, 8, 3), (10, 7, 2), (2, 5, 1), (7, 11, 1), (11, 3, 1), (6, 9, 2), (1, 6, 2)]
_B, _CC = trace(13, _L, _D, _R, _M, _C)
SCENE = dict(lamps=_L, rocks=_R, mirrors=_M, crystals=_CC, beams=_B)

def mock(mood):
    m = MOODS[mood]; W, H = 1920, 1080; N = 13; cell = 1000 // N; x0 = (W - N * cell) // 2; y0 = 40 + (1000 - N * cell) // 2
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" data-opaque="1">', defs(m),
         f'<rect width="{W}" height="{H}" fill="url(#sea)"/>', waves(m, W, H)]
    o.append(board(m, N, cell, x0, y0, SCENE))
    pos = [(40, 120), (1500, 120), (1500, 740), (40, 740)]; names = ['Skyward', 'Amberly', 'Jadeite', 'Orchidea']; sc = [412, 388, 297, 351]
    for i, (px, py) in enumerate(pos):
        c = P[i]
        o.append(f'<g transform="translate({px},{py})"><rect width="380" height="220" rx="16" fill="#0E1626" fill-opacity=".88" stroke="{c}" stroke-width="4"/>'
                 f'<rect x="0" y="0" width="14" height="220" rx="6" fill="{c}"/>'
                 f'<rect x="30" y="20" width="100" height="100" rx="12" fill="#060B14" stroke="{c}" stroke-width="3"/><text x="80" y="76" text-anchor="middle" font-family="Helvetica" font-size="16" fill="#5C6880">avatar</text>'
                 f'<text x="150" y="56" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="30" fill="{c}">{names[i]}</text>'
                 f'<text x="150" y="112" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="48" fill="#EEF2FA">{sc[i]}</text>'
                 f'<text x="290" y="112" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="28" fill="#CFF5E0">+{[5,3,0,2][i]}</text>'
                 + emblem(i, 345, 44, 20, c) +
                 f'<g transform="translate(30,150)">{mirror(18,18,40,0)}<text x="46" y="30" font-family="Helvetica" font-size="26" fill="#C9D1E0">x {[14,11,16,12][i]}</text>'
                 f'{crystal(170,18,40,2,tint=c)}<text x="196" y="30" font-family="Helvetica" font-size="26" fill="#C9D1E0">x {[3,2,0,1][i]}</text></g></g>')
    o.append(f'<text x="960" y="34" text-anchor="middle" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="32" fill="#EEF2FA">Turn 37 / 150</text></svg>')
    return ''.join(o)

def cover(mood, W=1200, H=675):
    m = MOODS[mood]; o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" data-opaque="1">', defs(m),
         f'<rect width="{W}" height="{H}" fill="url(#sea)"/>', waves(m, W, H, 18, 9)]
    # tilted board fragment, 4 beams converging on a central crystal (contest moment)
    cx, cy = 600, 360; cell = 70
    o.append(f'<g transform="translate({cx},{cy}) rotate(-8) translate({-cx},{-cy})">')
    sc = dict(lamps=[], rocks=[(4, 1), (11, 6)], mirrors=[(3, 3, 1), (12, 3, 0), (12, 7, 1)],
              crystals=[(8, 5, 3, 'X'), (3, 6, 2, 0), (10, 3, 1, 1), (6, 8, 2, 3), (14, 6, 1, None), (1, 2, 1, None)],
              beams={0: [(-2, 3), (3, 3), (3, 8), (-2, 8)], 1: [(8, -3), (8, 5), (8, 11)], 2: [(18, 3), (12, 3), (12, 7), (17, 7)], 3: [(-2, 5), (17, 5)]})
    bx = cx - 8.5 * cell; by = cy - 5.5 * cell
    o.append(board(m, 0, cell, bx, by, sc).split('</rect>', 1)[-1] if False else '')
    for y in range(-1, 12):
        for x in range(-1, 18):
            f = m['floor0'] if (x + y) % 2 == 0 else m['floor1']
            o.append(f'<rect x="{bx+x*cell+2}" y="{by+y*cell+2}" width="{cell-4}" height="{cell-4}" rx="9" fill="{f}" opacity=".9"/>')
    C = lambda x, y: (bx + x * cell + cell / 2, by + y * cell + cell / 2)
    for (x, y) in sc['rocks']:
        X, Y = C(x, y); o.append(f'<rect x="{X-cell*.42:.1f}" y="{Y-cell*.42:.1f}" width="{cell*.84:.1f}" height="{cell*.84:.1f}" rx="{cell*.16:.1f}" fill="{m["rock"]}"/>')
    for i, path in sc['beams'].items():
        o.append(beam([C(x, y) for x, y in path], i, 12))
    for (x, y, v, own) in sc['crystals']:
        X, Y = C(x, y); o.append(crystal(X, Y, cell * (1.5 if own == 'X' else 1), v, tint=P[own] if isinstance(own, int) else None, contested=own == 'X'))
    for (x, y, t) in sc['mirrors']:
        X, Y = C(x, y); o.append(mirror(X, Y, cell, t))
    o.append('</g>')
    o.append(f'<rect width="{W}" height="{H}" fill="url(#sea)" opacity="0"/>')
    o.append(f'<rect x="0" y="{H-150}" width="{W}" height="150" fill="#060B14" opacity=".72"/>')
    o.append(f'<text x="60" y="{H-62}" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="84" letter-spacing="6" fill="#F4F7FF">GLINTWATCH</text>')
    o.append(f'<text x="64" y="{H-24}" font-family="Helvetica" font-size="26" fill="#AFC0DA">Bend the light. Hold the crystals. 4 lamps, one reef.</text>')
    for i in range(4): o.append(emblem(i, 900 + i * 70, H - 78, 22, P[i]))
    o.append('</svg>')
    return ''.join(o)

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for mood in MOODS:
        open(os.path.join(OUT, f'mock_viewer_{mood}.svg'), 'w').write(mock(mood))
        open(os.path.join(OUT, f'cover_{mood}.svg'), 'w').write(cover(mood))
    print('ok')
