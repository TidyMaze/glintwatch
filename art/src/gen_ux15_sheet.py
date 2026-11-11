# UX 15 check sheet: every new image, then a layout check that overlays the live Texts at the Viewer.java positions.
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import gen_sheet as G
from gen_ux15 import t36
G.VECTOR = False  # the check uses the exported PNGs
P = G.P; o = ['<rect width="1920" height="1080" fill="#14161F"/>']
for y in range(0, 1080, 24):
    for x in range(0, 1920, 24):
        if (x // 24 + y // 24) % 2: o.append(f'<rect x="{x}" y="{y}" width="24" height="24" fill="#1B1E2A"/>')
S = ['unlit', 'dormant', 'contested', 'lit0', 'lit1', 'lit2', 'lit3', 'decay0', 'decay1', 'decay2', 'decay3']
for v in (1, 2, 3):
    for i, s in enumerate(S): o.append(G.img(f'crystal_{s}_v{v}', 10 + i * 66, 20 + (v - 1) * 100, 64, 64, ax=0, ay=0))
for s in range(4):
    for i, d in enumerate('ESWN'): o.append(G.img(f'lamp_{s}_{d}', 740 + i * 70, 20 + s * 70, 64, 64, ax=0, ay=0))
for i, d in enumerate('ESWN'): o.append(G.img(f'lamp_out_{d}', 740 + i * 70, 300, 64, 64, ax=0, ay=0))
o.append(G.img('yard_h', 1040, 20, 320, 192, P[1], 0, 0) + G.img('yard_v', 1380, 20, 192, 320, P[2], 0, 0))
o.append(G.img('coords_top_N15', 20, 330, 660, 40 * 660 / 990, ax=0, ay=0))
for s in range(4): o.append(G.img(f'panel_{s}', 20 + s * 395, 400, 380, 310, ax=0, ay=0))
o.append(G.img('panel_0_wood', 20, 730, 380, 310, ax=0, ay=0) + G.img('boardbox', 415, 730, 380, 140, ax=0, ay=0))
o.append(G.img('legend', 1205, 730, 380, 300, ax=0, ay=0) + G.img('open_yard', 1600, 400, 300, 245, ax=0, ay=0))
o.append(G.img('coords_left_N15', 1600, 660, 42 * .4, 990 * .4, ax=0, ay=0))
G.render_to(os.path.join(HERE, '..', 'ux15-sheet.png'), 1920, 1080, ''.join(o))

# layout check: images at their viewer origins + live texts at Viewer.java positions (N = 15)
n = 15; cell = 1000 // n; x0 = (1920 - n * cell) // 2; y0 = 40 + (1000 - n * cell) // 2
o = [G.img('background', 0, 0, 1920, 1080, ax=0, ay=0), f'<pattern id="fc" patternUnits="userSpaceOnUse" x="{x0}" y="{y0}" width="{2*cell}" height="{2*cell}"><image width="{2*cell}" height="{2*cell}" href="{G.uri("floor_checker")}"/></pattern><rect x="{x0}" y="{y0}" width="{n*cell}" height="{n*cell}" fill="url(#fc)"/>']
o.append(G.img('coords_top_N15', x0, y0 - 40, n * cell, 40, ax=0, ay=0) + G.img('coords_left_N15', x0 - 48, y0, 42, n * cell, ax=0, ay=0))
C = lambda x, y: (x0 + x * cell + cell / 2, y0 + y * cell + cell / 2); k = cell / 128
o.append(G.img('yard_v', x0, y0 + 2 * cell, 3 * cell, 5 * cell, P[0], 0, 0) + G.img('yard_h', x0 + 8 * cell, y0, 5 * cell, 3 * cell, P[1], 0, 0))
o.append(G.img('lamp_0_E', *C(0, 4), cell, cell) + G.img('lamp_1_S', *C(10, 0), cell, cell) + G.img('lamp_out_W', *C(14, 10), cell, cell) + G.img('lamp_3_N', *C(4, 14), cell, cell))
for i, (st, v) in enumerate([('unlit', 1), ('lit0', 2), ('lit1', 3), ('contested', 3), ('dormant', 2), ('lit3', 1), ('lit2', 2), ('decay1', 2), ('decay0', 3)]):
    o.append(G.img(f'crystal_{st}_v{v}', *C(3 + i, 6), 160 * k, 160 * k))
for s, (px, py) in enumerate([(40, 40), (1500, 40), (1500, 730), (40, 730)]):
    o.append(f'<rect x="{px+28}" y="{py+24}" width="112" height="112" fill="#6B7185"/>')  # live avatar, under the panel image
    o.append(G.img(f'panel_{s}', px, py, 380, 310, ax=0, ay=0))
    o.append(t36(px + 156, py + 24, ['wait', 'p1', 'random', 'p2'][s]) + t36(px + 172, py + 70, str([498, 263, 0, 213][s]), P[s], size=72))
    o.append(t36(px + 66, py + 158, '3/6') + t36(px + 178, py + 158, '2') + t36(px + 282, py + 158, '1'))
    o.append(G.img('icon_place', px + 46, py + 252, 32, 32) + t36(px + 70, py + 234, '9 14') + t36(px + 352, py + 234, '+6', '#F4F6FF', 'end'))
    o.append(t36(px + 28, py + 268, 'hello', '#A0A6B8', weight='normal'))
o.append(G.img('boardbox', 40, 470, 380, 140, ax=0, ay=0))
for y_, s in ((28, '0'), (84, '16/25')): o.append(t36(120, 470 + y_, s))
o.append(G.img('legend', 1500, 390, 380, 300, ax=0, ay=0))
G.render_to(os.path.join(HERE, '..', 'ux15-layout-check.png'), 1920, 1080, ''.join(o))
