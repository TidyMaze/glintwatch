# art/glow-compare.png for the owner: today's Line + glow (left) vs the baked glow tile (right), 4 patterns, real scale and 3x.
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import gen_sheet as G
P = G.P; PAT = ['beam_solid', 'beam_dash', 'beam_dots', 'beam_double']
def scene(ox, oy, z, L, pitch, tag):
    s = []; cell = 66 * z; w = 9 * z
    rows = 4 * pitch; cols = int((2 * L + 60 * z) / cell) + 2
    s.append(f'<clipPath id="c{tag}"><rect x="{ox}" y="{oy}" width="{2*L + 60*z}" height="{rows}"/></clipPath><g clip-path="url(#c{tag})">')
    for yy in range(int(rows / cell) + 2):
        for xx in range(cols): s.append(G.img(f'floor_{(xx + yy) % 2}', ox + xx * cell, oy + yy * cell, cell, cell, ax=0, ay=0))
    for k in range(4):
        Y = oy + pitch / 2 + k * pitch; x1 = ox + 10 * z; x2 = x1 + L + 40 * z
        # today: glow Line 2w+6 at alpha .22 + core tile of height w
        s.append(f'<line x1="{x1}" y1="{Y}" x2="{x1 + L}" y2="{Y}" stroke="{P[k]}" stroke-opacity=".22" stroke-width="{2*w + 6*z}"/>')
        s.append(f'<pattern id="o{k}{tag}" patternUnits="userSpaceOnUse" x="{x1}" y="{Y - w/2}" width="{2*w}" height="{w}"><image width="{2*w}" height="{w}" href="{G.uri(PAT[k])}" preserveAspectRatio="none"/></pattern>'
                 f'<rect x="{x1}" y="{Y - w/2}" width="{L}" height="{w}" fill="url(#o{k}{tag})"{G.tint(P[k])}/>')
        # baked: one tile, height 2w+6, tileScale = height/48 (the coder's spec)
        h = 2 * w + 6 * z
        s.append(f'<pattern id="n{k}{tag}" patternUnits="userSpaceOnUse" x="{x2}" y="{Y - h/2}" width="{32*h/48:.3f}" height="{h}"><image width="{32*h/48:.3f}" height="{h}" href="{G.uri(PAT[k] + "_glow")}" preserveAspectRatio="none"/></pattern>'
                 f'<rect x="{x2}" y="{Y - h/2}" width="{L}" height="{h}" fill="url(#n{k}{tag})"{G.tint(P[k])}/>')
    return ''.join(s) + '</g>'
W, H = 1200, 920
o = [G.img('background', 0, 0, W, H, ax=0, ay=0)]
o.append(G.text(40, 40, 'Real scale (N=15, beam width 9 px)', 24) + G.text(60, 78, 'today: line + glow', 20, '#A0A6B8', weight='normal') + G.text(640, 78, 'baked tile (one TilingSprite)', 20, '#A0A6B8', weight='normal'))
o.append(scene(40, 92, 1, 520, 66, 'a'))
o.append(G.text(40, 400, '3x zoom', 24) + G.text(60, 436, 'today', 20, '#A0A6B8', weight='normal') + G.text(640, 436, 'baked tile', 20, '#A0A6B8', weight='normal'))
o.append(scene(40, 450, 3, 470, 112, 'b'))
G.render_to(os.path.join(HERE, '..', 'glow-compare.png'), W, H, ''.join(o))
