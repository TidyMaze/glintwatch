# Statement diagrams (moderator review): art/png/statement/{mirrors,contested,streak}.png, 400 px wide, dark STYLE background.
import math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import gen_sheet as G
G.VECTOR = True
OUT = os.path.join(HERE, '..', 'png', 'statement'); os.makedirs(OUT, exist_ok=True)
P, INK = G.P, G.INK
BG = '<rect x="0" y="0" width="400" height="400" fill="#0E1018"/>'  # sips: no % sizes

def tile(x, y, c):
    return G.img('floor_0', x, y, c, c, ax=0, ay=0)

def arrow(x1, y1, x2, y2, col, w=7, head=True):
    o = f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{col}" stroke-opacity=".3" stroke-width="{w*2.4}" stroke-linecap="round"/>'
    o += f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{col}" stroke-width="{w}" stroke-linecap="round"/>'
    if head:
        a = math.atan2(y2 - y1, x2 - x1); L = w * 2.6
        pts = [(x2 + L * .6 * math.cos(a), y2 + L * .6 * math.sin(a)),
               (x2 + L * math.cos(a + 2.5), y2 + L * math.sin(a + 2.5)),
               (x2 + L * math.cos(a - 2.5), y2 + L * math.sin(a - 2.5))]
        o += '<polygon points="' + ' '.join(f'{px:.1f},{py:.1f}' for px, py in pts) + f'" fill="{col}" stroke="{INK}" stroke-width="1.5" stroke-linejoin="round"/>'
    return o

D = {'E': (1, 0), 'W': (-1, 0), 'N': (0, -1), 'S': (0, 1)}
def mirrors():
    c = 100; o = [BG]
    cases = [(0, 'E', 'N'), (0, 'N', 'E'), (0, 'W', 'S'), (0, 'S', 'W'), (1, 'E', 'S'), (1, 'S', 'E'), (1, 'W', 'N'), (1, 'N', 'W')]
    for i, (t, din, dout) in enumerate(cases):
        x0, y0 = (i % 4) * c, (i // 4) * c; cx, cy = x0 + c / 2, y0 + c / 2
        o.append(tile(x0 + 2, y0 + 2, c - 4))
        dx, dy = D[din]; ox, oy = D[dout]; r = c * .47
        o.append(G.img('mirror_base', cx, cy, c * .8, c * .8))
        o.append(arrow(cx - dx * r, cy - dy * r, cx, cy, P[0], head=False))            # incoming leg
        o.append(arrow(cx, cy, cx + ox * r * .78, cy + oy * r * .78, P[0]))           # outgoing leg, head at the end
        o.append(G.img('mirror_bar', cx, cy, c * .8, c * .8, rot=90 * t))
        o.append(arrow(cx - dx * r, cy - dy * r, cx - dx * r * .45, cy - dy * r * .45, P[0]))  # head on the incoming leg
    o.append(f'<line x1="0" y1="{c}" x2="400" y2="{c}" stroke="#3A4054" stroke-width="3"/>')
    G.render_to(os.path.join(OUT, 'mirrors.png'), 400, 2 * c, ''.join(o))

def crystal_at(X, Y, c, col, glow=None):
    o = ''
    if glow: o += G.img('crystal_glow', X, Y, c * 1.4, c * 1.4, glow, alpha=.75)
    sz = 128 * .64 * c / 92
    return o + G.img('crystal', X, Y, sz, sz, col) + G.text(X, Y + c * .1, '2', round(.3 * c), INK, 'middle')

def contested():
    c = 120; W, H = 400, 190; o = [BG]
    for k in range(2):
        x0 = 20 + k * 200; o.append(tile(x0, 10, c + 40)); X, Y = x0 + 80, 90
        o.append(arrow(x0 + 2, Y, x0 + 158, Y, P[0], 9))
        if k:
            o.append(arrow(X, 12, X, 146, P[1], 9))
            o.append(G.img('crystal_glow', X, Y, c * 1.3, c * 1.3, '#F4F6FF', alpha=.8) + G.frame('flicker_sheet', 128, 128, 1, X, Y, c * .9, c * .9))
            o.append(f'<circle cx="{X}" cy="{Y}" r="{c*.42:.0f}" fill="none" stroke="#F4F6FF" stroke-width="3" stroke-dasharray="8 6"/>')
            o.append(crystal_at(X, Y, c * .8, '#F4F6FF'))
            o.append(G.text(X, 186, '0', 30, '#F4F6FF', 'middle'))
        else:
            o.append(crystal_at(X, Y, c * .8, P[0], P[0]))
            o.append(G.text(X, 186, '+2', 30, P[0], 'middle'))
    o.append(f'<line x1="200" y1="14" x2="200" y2="176" stroke="#3A4054" stroke-width="3"/>')
    G.render_to(os.path.join(OUT, 'contested.png'), W, H + 6, ''.join(o))

def streak():
    c = 90; W, H = 400, 150; o = [BG]
    for k in range(4):
        x0 = 8 + k * 98; X, Y = x0 + c / 2, 52
        o.append(tile(x0, 7, c))
        o.append(arrow(x0 + 2, Y, x0 + c - 2, Y, P[0], 7, head=False))
        o.append(crystal_at(X, Y, c * .75, P[0], P[0]))
        if k:
            bw, bh = 30, 37; bx, by = x0 + c - bw / 2 - 2, 7 + c - bh / 2 - 2  # owner 14.3 B: digit chip, as in the viewer
            o.append(G.img('streak_badge', bx, by, bw, bh, P[0]) + G.text(bx, by + 11, str(k + 1), 31, P[0], 'middle'))
        o.append(G.text(X, 132, f'x{k+1}', 28, '#E8EAF0', 'middle'))
        if k < 3: o.append(arrow(x0 + c + 1, 132 - 9, x0 + c + 7, 132 - 9, '#6B7185', 3))
    G.render_to(os.path.join(OUT, 'streak.png'), W, H, ''.join(o))

# ---------------- v14 rule diagrams (owner order): same palette, 400 px wide, numbers as the only text (+ axis letters)
PAT = ['beam_solid', 'beam_dash', 'beam_dots', 'beam_double']
EMB = ['ring', 'triangle', 'square', 'star']
def beam(pts, s, w, cell_px):
    # pts in pixels; glow + tinted tiling core (as the viewer: glow solid, pattern on the core)
    o = ''
    for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
        L = math.hypot(x2 - x1, y2 - y1); ang = math.degrees(math.atan2(y2 - y1, x2 - x1))
        o += f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{P[s]}" stroke-opacity=".28" stroke-width="{2*w+6:.1f}" stroke-linecap="round"/>'
        if s == 0:
            o += f'<rect x="0" y="{-w/2:.1f}" width="{L:.1f}" height="{w:.1f}" fill="{P[0]}" transform="translate({x1:.1f},{y1:.1f}) rotate({ang:.1f})"/>'
        else:
            pid = f'b{s}_{int(x1)}_{int(y1)}_{int(x2)}'
            o += (f'<pattern id="{pid}" patternUnits="userSpaceOnUse" x="0" y="{-w/2:.1f}" width="{2*w:.1f}" height="{w:.1f}"><image width="{2*w:.1f}" height="{w:.1f}" href="{G.uri(PAT[s])}" preserveAspectRatio="none"/></pattern>'
                  f'<rect x="0" y="{-w/2:.1f}" width="{L:.1f}" height="{w:.1f}" fill="url(#{pid})" transform="translate({x1:.1f},{y1:.1f}) rotate({ang:.1f})"{G.tint(P[s])}/>')
    return o
def lamp(s, X, Y, c, rot):
    return G.img('lamp_disc', X, Y, c, c, P[s]) + G.img('lamp_nose', X, Y, c, c, P[s], rot=rot) + G.img('emblem_' + EMB[s], X, Y, c * .55, c * .55, P[s])
def gemv(X, Y, c, v, col='#9AA0B4', glow=None, dormant=False):
    sz = 128 * [0, .40, .52, .64][v] * c / 92; o = ''
    if dormant: return G.img('crystal_outline', X, Y, sz, sz)
    if glow: o += G.img('crystal_glow', X, Y, c * 1.3, c * 1.3, glow, alpha=.7)
    return o + G.img('crystal', X, Y, sz, sz, col)
def floor(x, y, c, k=0): return G.img(f'floor_{k}', x, y, c, c, ax=0, ay=0)
def label(x, y, s, size=24, col='#E8EAF0', anchor='middle', weight='bold'): return G.text(x, y, s, size, col, anchor, weight)
def save(name, W, H, o): G.render_to(os.path.join(OUT, name + '.png'), W, H, f'<rect width="{W}" height="{H}" fill="#0E1018"/>' + ''.join(o))

def setup():  # 1: board, 4 lamps on the edges (F2 seats, N=13, e=3), yards, straight lamp lines, axes
    N, c, ox, oy = 13, 27, 34, 34; W = ox + N * c + 6; H = oy + N * c + 6
    C = lambda x, y: (ox + x * c + c / 2, oy + y * c + c / 2)
    lamps = [(0, 3), (9, 0), (12, 9), (3, 12)]; dirs = [(1, 0), (0, 1), (-1, 0), (0, -1)]
    o = []
    for y in range(N):
        for x in range(N): o.append(floor(ox + x * c, oy + y * c, c, (x + y) % 2))
    for s, (lx, ly) in enumerate(lamps):
        xa, ya, xb, yb = max(0, lx - 2), max(0, ly - 2), min(N - 1, lx + 2), min(N - 1, ly + 2)
        o.append(G.img('zone_tile', ox + xa * c, oy + ya * c, (xb - xa + 1) * c, (yb - ya + 1) * c, P[s], 0, 0, alpha=.18))
        o.append(f'<rect x="{ox + xa*c + 1.5}" y="{oy + ya*c + 1.5}" width="{(xb-xa+1)*c - 3}" height="{(yb-ya+1)*c - 3}" fill="none" stroke="{P[s]}" stroke-width="2" stroke-dasharray="6 4"/>')
    def orbit(p):
        out = [p]
        for _ in range(3): out.append((N - 1 - out[-1][1], out[-1][0]))
        return out
    # no rocks: setup.png is shown from league 1, which has none (lead)
    for p in orbit((5, 5)): o.append(gemv(*C(*p), c, 1))
    o.append(gemv(*C(6, 6), c, 1))
    w = 6
    for s, ((lx, ly), (dx, dy)) in enumerate(zip(lamps, dirs)):  # every lamp line is empty: the beam crosses the reef straight
        ex, ey = lx + dx * N, ly + dy * N; ex = min(max(ex, -0.5), N - .5); ey = min(max(ey, -0.5), N - .5)
        d = (s - 1.5) * (w + 2) * 0  # one beam per line, no lane offset needed
        o.append(beam([C(lx, ly), C(ex, ey)], s, w, c))
    for s, (lx, ly) in enumerate(lamps): o.append(lamp(s, *C(lx, ly), c, 90 * s))
    for i in range(N):
        o.append(label(ox + i * c + c / 2, 24, str(i), 15, '#A0A6B8'))
        o.append(label(26, oy + i * c + c / 2 + 5, str(i), 15, '#A0A6B8', 'end'))
    o.append(label(ox + N * c - 4, 12, 'x', 15, '#E8EAF0', 'end') + f'<path d="M{ox},{8} H{ox + N*c - 16}" stroke="#6B7185" stroke-width="2"/><polygon points="{ox+N*c-16},3 {ox+N*c-8},8 {ox+N*c-16},13" fill="#6B7185"/>')
    o.append(label(8, oy + N * c - 2, 'y', 15, '#E8EAF0', 'middle') + f'<path d="M8,{oy} V{oy + N*c - 22}" stroke="#6B7185" stroke-width="2"/><polygon points="3,{oy+N*c-22} 8,{oy+N*c-14} 13,{oy+N*c-22}" fill="#6B7185"/>')
    save('setup', W, H, o)

def stops():  # 2: a beam stops on a rock, at the reef edge, on a lamp
    c = 56; W, H = 400, 116; o = []
    for k in range(3):
        x0 = 8 + k * 132; y0 = 20
        for i in range(2): o.append(floor(x0 + i * c, y0 + c / 2, c, i % 2))
        Y = y0 + c
        if k == 0:  # rock: the beam ends on the rock face
            o.append(G.img('rock', x0 + 1.5 * c, Y, c, c)); o.append(beam([(x0 - 4, Y), (x0 + 1.5 * c - c * .42, Y)], 0, 8, c))
        elif k == 1:  # edge: the beam leaves the reef
            o.append(f'<rect x="{x0 + 2*c}" y="{y0 + c/2 - 6}" width="5" height="{c + 12}" fill="#3A4054"/>')
            o.append(beam([(x0 - 4, Y), (x0 + 2 * c, Y)], 0, 8, c))
        else:  # lamp: the beam ends on the lamp
            o.append(beam([(x0 - 4, Y), (x0 + 1.5 * c - c * .42, Y)], 0, 8, c)); o.append(lamp(1, x0 + 1.5 * c, Y, c, 180))
    # stop marker: a short white bar where each beam ends
    for k, xe in enumerate([8 + 1.5 * 56 - 56 * .42, 8 + 132 + 2 * 56, 8 + 264 + 1.5 * 56 - 56 * .42]):
        o.append(f'<rect x="{xe - 3}" y="{20 + 56 - 16}" width="6" height="32" rx="3" fill="#F4F6FF"/>')
    o.append(f'<line x1="136" y1="24" x2="136" y2="106" stroke="#3A4054" stroke-width="3"/><line x1="268" y1="24" x2="268" y2="106" stroke="#3A4054" stroke-width="3"/>')
    save('stops', W, H, o)

def lock():  # 3: TURN flips the mirror; lock 5 on the next input, then -1 per turn; TURN allowed again at 0
    c = 58; W, H = 400, 190; o = []
    # row 1: before / after the TURN
    o.append(floor(20, 10, c) + G.img('mirror_base', 20 + c / 2, 10 + c / 2, c, c) + G.img('mirror_bar', 20 + c / 2, 10 + c / 2, c, c, rot=0))
    o.append(G.img('icon_turn', 20 + c + 30, 10 + c / 2, 40, 40, '#E8EAF0'))
    o.append(floor(20 + c + 60, 10, c) + G.img('mirror_base', 20 + c * 1.5 + 60, 10 + c / 2, c, c) + G.img('mirror_bar', 20 + c * 1.5 + 60, 10 + c / 2, c, c, rot=90))
    for k in range(5): o.append(G.img('lock_arc', 20 + c * 1.5 + 60, 10 + c / 2, c, c, P[0], rot=72 * k))
    o.append(label(20 + c * 1.5 + 60, 10 + c + 22, '5', 22))
    # row 2: lock 5 4 3 2 1 0 on the following turns
    for i, n in enumerate([5, 4, 3, 2, 1, 0]):
        X = 36 + i * 64; Y = 128
        o.append(floor(X - 26, Y - 26, 52) + G.img('mirror_base', X, Y, 52, 52) + G.img('mirror_bar', X, Y, 52, 52, rot=90))
        for k in range(5):
            if n: o.append(G.img('lock_arc', X, Y, 52, 52, P[0] if k < n else '#55617A', rot=72 * k))
        o.append(label(X, Y + 50, str(n), 22, '#F4F6FF' if n else P[2]))
    o.append(G.img('icon_turn', 36 + 5 * 64 + 34, 128, 26, 26, P[2]))
    save('lock', W, H, o)

def build():  # 4: placed on t (beams pass), pending on t+1 (beams pass, type hidden from rivals), bends from t+2
    c = 64; W, H = 400, 236; o = []
    cols = [('t', True), ('+1', True), ('+2', False)]
    for i, (lab, pending) in enumerate(cols):
        x0 = 20 + i * 128; y0 = 8
        for r in range(2):  # row 0: your view (type known); row 1: a rival's view (type -1)
            yy = y0 + r * 96; X, Y = x0 + c / 2 + 16, yy + c / 2
            o.append(floor(x0 + 16, yy, c))
            if pending:
                o.append(beam([(x0 - 4, Y), (x0 + c + 36, Y)], 1, 8, c))  # amber beam passes through
                o.append(G.img('mirror_base', X, Y, c, c))
                if r == 0: o.append(G.img('mirror_bar', X, Y, c, c, rot=90, alpha=.4))
                o.append(G.img('mirror_pending', X, Y, c, c, P[0]))
            else:
                o.append(beam([(x0 - 4, Y), (X, Y), (X, yy + c + 6)], 1, 8, c))  # \ turns the eastward beam south
                o.append(G.img('mirror_base', X, Y, c, c) + G.img('mirror_bar', X, Y, c, c, rot=90))
            o.append(G.img('emblem_' + EMB[0 if r == 0 else 1], x0 + 4, yy + 10, 20, 20, P[0 if r == 0 else 1]))
        o.append(label(x0 + c / 2 + 16, 222, lab, 24))  # t = the turn you print PLACE
    o.append(G.img('icon_place', 20 + c / 2 + 40, 212, 28, 28))
    save('build', W, H, o)

def wear():  # 5: life 20 -> 8 intact, 7-4 one crack, 3-1 two cracks, shatters at the end of its 20th active turn
    c = 70; W, H = 400, 130; o = []
    st = [('20', None, 1), ('7', 1, 1), ('3', 2, .65), ('0', 'x', 1)]
    for i, (lab, cr, a) in enumerate(st):
        x0 = 16 + i * 96; X, Y = x0 + c / 2, 10 + c / 2
        o.append(floor(x0, 10, c))
        if cr == 'x': o.append(G.img('mirror_base', X, Y, c, c, alpha=.45) + G.frame('shatter_sheet', 128, 128, 1, X, Y, c, c))
        else:
            o.append(G.img('mirror_base', X, Y, c, c) + G.img('mirror_bar', X, Y, c, c, alpha=a))
            if cr: o.append(G.img(f'mirror_crack_{cr}', X, Y, c, c))
        o.append(label(X, 112, lab, 26))
        if i < 3: o.append(arrow(x0 + c + 6, Y, x0 + c + 20, Y, '#6B7185', 3))
    save('wear', W, H, o)

def tides():  # 6: values 1-3 (size), awake 30 turns of every 45, asleep = hollow crystal that pays nothing
    W, H = 400, 200; o = []; c = 76
    for v in (1, 2, 3):
        x0 = 10 + (v - 1) * 82; o.append(floor(x0, 8, c)); o.append(gemv(x0 + c / 2, 8 + c / 2, c, v, P[0], P[0]))
        o.append(label(x0 + c / 2, 106, str(v), 26, P[0]))
    x0 = 268; o.append(floor(x0, 8, c)); o.append(gemv(x0 + c / 2, 8 + c / 2, c, 2, dormant=True)); o.append(label(x0 + c / 2, 106, '0', 26, '#8E9BB3'))
    o.append(f'<line x1="256" y1="12" x2="256" y2="110" stroke="#3A4054" stroke-width="3"/>')
    # 45-turn cycle bar: 30 awake, 15 asleep
    bx, by, bw = 20, 140, 360; aw = bw * 30 / 45
    o.append(f'<rect x="{bx}" y="{by}" width="{aw:.1f}" height="18" rx="4" fill="{P[0]}"/>'
             f'<rect x="{bx + aw:.1f}" y="{by}" width="{bw - aw:.1f}" height="18" rx="4" fill="none" stroke="#8E9BB3" stroke-width="3" stroke-dasharray="7 5"/>')
    for t_, xx in (('0', bx), ('30', bx + aw), ('45', bx + bw)): o.append(label(xx, by + 46, t_, 22, '#A0A6B8'))
    o.append(gemv(bx + aw / 2, by - 14, 30, 2, P[0]) if False else '')
    save('tides', W, H, o)

def strike():  # 7: a beam that ends on another player's lamp: +2 to the shooter every turn
    c = 70; W, H = 400, 150; o = []
    for i in range(4): o.append(floor(20 + i * c, 20, c, i % 2))
    Y = 20 + c / 2
    o.append(beam([(20 + c * .9, Y), (20 + 3.5 * c - c * .42, Y)], 0, 9, c))
    o.append(lamp(0, 20 + c / 2, Y, c, 0))
    o.append(lamp(1, 20 + 3.5 * c, Y, c, 180))
    o.append(G.frame('spark_sheet', 64, 64, 1, 20 + 3.5 * c - c * .45, Y, c * .9, c * .9))
    o.append(label(20 + c / 2, 20 + c + 40, '+2', 32, P[0]))
    o.append(label(20 + 3.5 * c, 20 + c + 40, '0', 32, '#A0A6B8'))
    save('strike', W, H, o)

def order():  # 8: one turn, all players at once: supply, places, turns, beams, scoring, locks -1, pending -> active + wear
    W, H = 400, 230; o = []
    steps = [('icon_supply', P[0]), ('icon_place', '#E8EAF0'), ('icon_turn', '#E8EAF0'), ('lamp', None), ('icon_crystal', P[0]), ('lock', None), ('crack', None)]
    pos = [(40, 50), (130, 50), (220, 50), (310, 50), (85, 160), (190, 160), (295, 160)]
    for i, ((n, col), (X, Y)) in enumerate(zip(steps, pos)):
        o.append(f'<rect x="{X - 34}" y="{Y - 34}" width="68" height="68" rx="12" fill="#232736" stroke="#3A4054" stroke-width="2"/>')
        if n == 'lamp': o.append(lamp(0, X - 6, Y, 46, 0) + beam([(X + 8, Y), (X + 30, Y)], 0, 6, 46))
        elif n == 'lock':
            o.append(G.img('mirror_base', X, Y, 56, 56) + G.img('mirror_bar', X, Y, 56, 56))
            for k in range(5): o.append(G.img('lock_arc', X, Y, 56, 56, P[0] if k < 4 else '#55617A', rot=72 * k))
        elif n == 'crack': o.append(G.img('mirror_base', X, Y, 56, 56) + G.img('mirror_bar', X, Y, 56, 56) + G.img('mirror_crack_1', X, Y, 56, 56) + G.img('mirror_pending', X + 20, Y - 20, 26, 26, P[0]))
        else: o.append(G.img(n, X, Y, 44, 44, col))
        o.append(f'<circle cx="{X - 30}" cy="{Y - 30}" r="13" fill="{INK}" stroke="#E8EAF0" stroke-width="2"/>' + label(X - 30, Y - 24, str(i + 1), 17))
        if i in (0, 1, 2, 4, 5): o.append(arrow(X + 38, Y, X + 50, Y, '#6B7185', 3))
    o.append(f'<path d="M310,88 V112 H85 V122" fill="none" stroke="#6B7185" stroke-width="3"/><polygon points="79,120 85,128 91,120" fill="#6B7185"/>')
    save('order', W, H, o)

if __name__ == '__main__':
    import sys as _s
    names = _s.argv[1:] or ['mirrors', 'contested', 'streak', 'setup', 'stops', 'lock', 'build', 'wear', 'tides', 'strike', 'order']
    for n_ in names: globals()[n_]()
