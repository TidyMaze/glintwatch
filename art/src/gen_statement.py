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
            fs = 22; bw, bh = 1.45 * fs, 1.15 * fs; bx, by = x0 + c - bw / 2 - 3, 7 + c - bh / 2 - 3
            o.append(G.img('streak_badge', bx, by, bw, bh, P[0]) + G.text(bx, by + fs * .37, f'x{k+1}', fs, P[0], 'middle'))
        o.append(G.text(X, 132, f'x{k+1}', 28, '#E8EAF0', 'middle'))
        if k < 3: o.append(arrow(x0 + c + 1, 132 - 9, x0 + c + 7, 132 - 9, '#6B7185', 3))
    G.render_to(os.path.join(OUT, 'streak.png'), W, H, ''.join(o))

if __name__ == '__main__':
    mirrors(); contested(); streak()
