# Glintwatch sprites (M4). Original art drawn as SVG here, exported with macOS `sips`.
# Spec: ../../ux/UX.md section 9. Usage: python3 gen_sprites.py  (writes src/sprites/*.svg and png/*.png)
import math, os, random, subprocess, json

HERE = os.path.dirname(os.path.abspath(__file__))
SVG = os.path.join(HERE, 'sprites'); PNG = os.path.join(HERE, '..', 'png')
INK = '#0E1018'
K = 128 / 66  # line widths in UX.md are display px at cell ~66 (N=15); sprites are drawn at 128

def svg(w, h, body, defs=''):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
            f'<defs>{defs}</defs>{body}</svg>')

def star_pts(cx, cy, r, ri, n=4, rot=-90):
    return ' '.join(f'{cx + (r if k % 2 == 0 else ri) * math.cos(math.radians(rot + k * 180 / n)):.2f},'
                    f'{cy + (r if k % 2 == 0 else ri) * math.sin(math.radians(rot + k * 180 / n)):.2f}' for k in range(2 * n))

S = {}  # name -> (w, h, svg text)

# --- background 1920x1080: STYLE gradient + faint waves (no text)
def background():
    r = random.Random(7); waves = []
    for _ in range(34):
        y = r.uniform(0, 1080); x = r.uniform(-200, 1920); L = r.uniform(140, 420); a = r.uniform(.04, .10)
        waves.append(f'<path d="M{x:.0f},{y:.0f} q{L/4:.0f},-12 {L/2:.0f},0 t{L/2:.0f},0" fill="none" stroke="#3B6E9A" '
                     f'stroke-opacity="{a:.2f}" stroke-width="3" stroke-linecap="round"/>')
    d = ('<radialGradient id="g" cx="50%" cy="42%" r="78%"><stop offset="0" stop-color="#13284A"/>'
         '<stop offset="1" stop-color="#060C18"/></radialGradient>')
    return svg(1920, 1080, '<rect width="1920" height="1080" fill="url(#g)"/>' + ''.join(waves), d)
S['background'] = (1920, 1080, background())

# --- floor tiles 128, anchor 0,0: flat stone, soft bevel, 2 grain marks
def floor(base, light, dark, seed):
    r = random.Random(seed); g = ''
    for _ in range(3):
        x, y = r.uniform(24, 100), r.uniform(24, 100)
        g += f'<path d="M{x:.0f},{y:.0f} l{r.uniform(6,14):.0f},{r.uniform(-4,4):.0f}" stroke="{dark}" stroke-width="3" stroke-linecap="round" opacity=".5"/>'
    return svg(128, 128, f'<rect width="128" height="128" fill="{base}"/>'
               f'<path d="M0,0 H128 L124,4 H4 V124 L0,128 Z" fill="{light}" opacity=".55"/>'
               f'<path d="M128,128 H0 L4,124 H124 V4 L128,0 Z" fill="{dark}" opacity=".55"/>' + g)
S['floor_0'] = (128, 128, floor('#1B2D45', '#22385A', '#142236', 1))
S['floor_1'] = (128, 128, floor('#18283E', '#1F3350', '#111D2F', 2))
def _inner(s): return s[s.index('</defs>') + 7:-6]
S['floor_checker'] = (256, 256, svg(256, 256, ''.join(f'<g transform="translate({128*x},{128*y})">{_inner(S["floor_" + str((x + y) % 2)][2])}</g>' for y in (0, 1) for x in (0, 1))))  # 2x2 cells, TilingSprite

# --- rock 128: dark basalt body, light rim #808AA6 (>= 3.76:1 on both floors), top-left bevel, drop shadow
S['rock'] = (128, 128, svg(128, 128,
    '<rect x="16" y="20" width="100" height="100" rx="18" fill="#000" opacity=".45" filter="url(#b)"/>'
    '<path d="M30,12 H98 L116,30 V98 L98,116 H30 L12,98 V30 Z" fill="#0A111D" stroke="#6E7590" stroke-width="6" stroke-linejoin="round"/>'
    '<path d="M30,12 H98 L116,30 V98 L98,116 H30 L12,98 V30 Z" fill="none" stroke="' + INK + '" stroke-width="2" stroke-linejoin="round"/>'
    '<path d="M24,40 L40,22 H90 L64,48 Z" fill="#6E7590" opacity=".9"/>'
    '<path d="M64,48 L104,40 V92 L80,104 Z" fill="#05080F" opacity=".9"/>'
    '<path d="M24,40 L64,48 L80,104 H34 L24,92 Z" fill="#141E30" opacity=".9"/>',
    '<filter id="b" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="4"/></filter>'))

# --- crystal 128: grey-white faceted diamond (tint multiplies). Gem box: 92 wide x 124 high, centred.
GEM_W = 92
def gem(cx, cy, w, h, sw):
    t, b, l, r = (cx, cy - h / 2), (cx, cy + h / 2), (cx - w / 2, cy), (cx + w / 2, cy)
    m = (cx, cy - h * .12)
    P = lambda *p: ' '.join(f'{x:.1f},{y:.1f}' for x, y in p)
    return (f'<polygon points="{P(t, r, b, l)}" fill="#E4E8F0"/>'
            f'<polygon points="{P(t, m, l)}" fill="#FFFFFF"/>'
            f'<polygon points="{P(t, r, m)}" fill="#D2D7E2"/>'
            f'<polygon points="{P(l, m, b)}" fill="#C4CAD7"/>'
            f'<polygon points="{P(m, r, b)}" fill="#A9B0C0"/>'
            f'<polygon points="{P(t, r, b, l)}" fill="none" stroke="{INK}" stroke-width="{sw}" stroke-linejoin="round"/>'
            f'<path d="M{t[0] - w*.12:.1f},{t[1] + h*.2:.1f} l{w*.08:.1f},-{h*.08:.1f}" stroke="#FFFFFF" stroke-width="{sw*.9:.1f}" stroke-linecap="round"/>')
S['crystal'] = (128, 128, svg(128, 128, gem(64, 64, GEM_W, 124, 6)))
S['crystal_glow'] = (128, 128, svg(128, 128, '<circle cx="64" cy="64" r="64" fill="url(#r)"/>',
    '<radialGradient id="r"><stop offset="0" stop-color="#fff" stop-opacity="1"/><stop offset=".45" stop-color="#fff" stop-opacity=".45"/>'
    '<stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>'))
S['badge_zero'] = (32, 32, svg(32, 32, f'<rect x="1.5" y="1.5" width="29" height="29" rx="6" fill="{INK}" stroke="#F4F6FF" stroke-width="2"/>'
    '<text x="16" y="24" text-anchor="middle" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="22" fill="#F4F6FF">0</text>'))

# --- mirror: base r 0.30*cell, silver bar '/'
S['mirror_base'] = (128, 128, svg(128, 128,
    f'<circle cx="64" cy="66" r="40" fill="#000" opacity=".4"/><circle cx="64" cy="64" r="38" fill="#3A4054" stroke="{INK}" stroke-width="4"/>'
    '<path d="M36,52 A30,30 0 0 1 76,34" stroke="#5A6178" stroke-width="5" fill="none" stroke-linecap="round"/>'))
S['mirror_bar'] = (128, 128, svg(128, 128,
    f'<g transform="rotate(-45 64 64)"><rect x="8" y="53" width="112" height="22" rx="10" fill="url(#s)" stroke="{INK}" stroke-width="5"/>'
    '<rect x="16" y="57" width="96" height="5" rx="2.5" fill="#FFFFFF"/></g>',
    '<linearGradient id="s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F4F7FF"/><stop offset=".55" stop-color="#D8DEEA"/>'
    '<stop offset="1" stop-color="#9AA3BD"/></linearGradient>'))

# --- lock arc: one 104-degree arc, radius 0.40*128 = 51, width 6 display px -> 12; centred on the top (rotate 0/120/240)
def arc(r, deg, w):
    a0, a1 = math.radians(-90 - deg / 2), math.radians(-90 + deg / 2)
    x0, y0, x1, y1 = 64 + r * math.cos(a0), 64 + r * math.sin(a0), 64 + r * math.cos(a1), 64 + r * math.sin(a1)
    d = f'M{x0:.2f},{y0:.2f} A{r},{r} 0 0 1 {x1:.2f},{y1:.2f}'
    return f'<path d="{d}" stroke="{INK}" stroke-width="{w + 4}" fill="none" stroke-linecap="round"/><path d="{d}" stroke="#FFFFFF" stroke-width="{w}" fill="none" stroke-linecap="round"/>'
S['lock_arc'] = (128, 128, svg(128, 128, arc(51, 54 - 12, 11)))  # UX 12: 5 arcs of 54 deg, gap 18; -12 deg leaves room for the round caps

# --- highlight: white 5 px outline (display) + 18 % fill; inner 3 px outline for the actor colour (tinted copy)
S['highlight_cell'] = (128, 128, svg(128, 128, '<rect x="5" y="5" width="118" height="118" rx="14" fill="#FFFFFF" fill-opacity=".18" stroke="#FFFFFF" stroke-width="10"/>'))
S['highlight_inner'] = (128, 128, svg(128, 128, '<rect x="15" y="15" width="98" height="98" rx="9" fill="none" stroke="#FFFFFF" stroke-width="6"/>'))

# --- lamp: disc r 0.42*cell; grey fill becomes the dark colour, white ring the main colour under one tint
S['lamp_disc'] = (128, 128, svg(128, 128,
    f'<circle cx="64" cy="67" r="56" fill="#000" opacity=".4"/><circle cx="64" cy="64" r="54" fill="{INK}"/>'
    '<circle cx="64" cy="64" r="47" fill="#5A5A5A" stroke="#FFFFFF" stroke-width="10"/>'
    '<path d="M30,50 A36,36 0 0 1 60,28" stroke="#FFFFFF" stroke-opacity=".35" stroke-width="5" fill="none" stroke-linecap="round"/>'))
S['lamp_nose'] = (128, 128, svg(128, 128,
    f'<path d="M104,46 L124,56 V72 L104,82 Z" fill="#FFFFFF" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'))

# --- emblems 48: white with ink outline (tinted); used at r 11-24
def emblem(kind, cx=24, cy=24, r=20, sw=4, fill='#FFFFFF'):
    s = f'fill="{fill}" stroke="{INK}" stroke-width="{sw}" stroke-linejoin="round"'
    if kind == 'ring': return f'<circle cx="{cx}" cy="{cy}" r="{r*.9:.2f}" {s}/><circle cx="{cx}" cy="{cy}" r="{r*.36:.2f}" fill="{INK}"/>'
    if kind == 'triangle': return f'<path d="M{cx},{cy-r} L{cx+r*.98:.2f},{cy+r*.78:.2f} L{cx-r*.98:.2f},{cy+r*.78:.2f} Z" {s}/>'
    if kind == 'square': return (f'<rect x="{cx-r*.8:.2f}" y="{cy-r*.8:.2f}" width="{r*1.6:.2f}" height="{r*1.6:.2f}" rx="{r*.15:.2f}" {s}/>'
                                 f'<rect x="{cx-r*.3:.2f}" y="{cy-r*.3:.2f}" width="{r*.6:.2f}" height="{r*.6:.2f}" fill="{INK}"/>')
    return f'<polygon points="{star_pts(cx, cy, r, r*.42)}" {s}/>'
EMBLEMS = ['ring', 'triangle', 'square', 'star']
for e in EMBLEMS: S['emblem_' + e] = (48, 48, svg(48, 48, emblem(e)))
S['emblem_cross'] = (48, 48, svg(48, 48,
    f'<path d="M10,10 L38,38 M38,10 L10,38" stroke="{INK}" stroke-width="12" stroke-linecap="round"/>'
    '<path d="M10,10 L38,38 M38,10 L10,38" stroke="#FF4D4D" stroke-width="6" stroke-linecap="round"/>'))

# --- beam tiles 32x16, anchor 0,0.5, white (tinted); height maps to beam width w
S['beam_solid'] = (32, 16, svg(32, 16, '<rect width="32" height="16" fill="#FFFFFF"/>'))
S['beam_dash'] = (32, 16, svg(32, 16, '<rect x="0" y="0" width="22" height="16" rx="3" fill="#FFFFFF"/>'))
S['beam_dots'] = (32, 16, svg(32, 16, '<circle cx="8" cy="8" r="7" fill="#FFFFFF"/><circle cx="24" cy="8" r="7" fill="#FFFFFF"/>'))
S['beam_double'] = (32, 16, svg(32, 16, f'<rect width="32" height="16" fill="#FFFFFF"/><rect y="5.5" width="32" height="5" fill="{INK}"/>'))

# --- spark sheet 4 x 64: white rays, grow then fade
def spark(f):
    n, r0, r1, a = 8, [4, 8, 14, 20][f], [18, 28, 30, 30][f], [1, 1, .7, .35][f]
    o = f'<circle cx="32" cy="32" r="{[10, 8, 5, 2][f]}" fill="#FFFFFF" opacity="{a}"/>'
    for k in range(n):
        t = math.radians(k * 45 + 22.5 * (k % 2)); L = r1 if k % 2 == 0 else r1 * .7
        o += (f'<line x1="{32 + r0*math.cos(t):.1f}" y1="{32 + r0*math.sin(t):.1f}" x2="{32 + L*math.cos(t):.1f}" y2="{32 + L*math.sin(t):.1f}" '
              f'stroke="#FFFFFF" stroke-width="{[5, 4.5, 3.5, 2.5][f]}" stroke-linecap="round" opacity="{a}"/>')
    return o
S['spark_sheet'] = (256, 64, svg(256, 64, ''.join(f'<g transform="translate({64*f},0)">{spark(f)}</g>' for f in range(4))))

# --- flicker sheet 4 x 128: white crackle loop around a contested crystal
def crackle(f):
    r = random.Random(40 + f); o = ''
    for k in range(5):
        a = math.radians(k * 72 + f * 18 + r.uniform(-10, 10)); R0, R1 = 34, 58
        pts = []
        for j in range(5):
            rr = R0 + (R1 - R0) * j / 4; aa = a + math.radians(r.uniform(-12, 12))
            pts.append(f'{64 + rr*math.cos(aa):.1f},{64 + rr*math.sin(aa):.1f}')
        o += f'<polyline points="{" ".join(pts)}" fill="none" stroke="#FFFFFF" stroke-width="{4 if (k+f)%2 else 3}" stroke-linejoin="round" stroke-linecap="round" opacity="{.95 if (k+f)%2 else .7}"/>'
    return o
S['flicker_sheet'] = (512, 128, svg(512, 128, ''.join(f'<g transform="translate({128*f},0)">{crackle(f)}</g>' for f in range(4))))

# --- zones
S['zone_tile'] = (128, 128, svg(128, 128, '<rect width="128" height="128" fill="#FFFFFF"/>'))
S['zone_border'] = (20, 8, svg(20, 8, '<rect x="0" y="2.5" width="12" height="3" fill="#FFFFFF"/>'))  # dash 12 on 8 off: period 20

# --- panel 380x300: fill (not tinted) + frame (white border and strip, tinted)
S['panel_fill'] = (380, 300, svg(380, 300, '<rect x="1.5" y="1.5" width="377" height="297" rx="12" fill="#232736"/>'))
S['panel_frame'] = (380, 300, svg(380, 300,
    '<clipPath id="c"><rect x="0" y="0" width="380" height="300" rx="12"/></clipPath>'
    '<rect x="0" y="0" width="12" height="300" fill="#FFFFFF" clip-path="url(#c)"/>'
    '<rect x="1.5" y="1.5" width="377" height="297" rx="12" fill="none" stroke="#FFFFFF" stroke-width="3"/>'))

# --- HUD icons 32
S['icon_mirror'] = (32, 32, svg(32, 32, f'<circle cx="16" cy="16" r="10" fill="#3A4054" stroke="{INK}" stroke-width="1.5"/>'
    f'<g transform="rotate(-45 16 16)"><rect x="1.5" y="12.5" width="29" height="7" rx="3.5" fill="#D8DEEA" stroke="{INK}" stroke-width="2"/></g>'))
S['icon_crystal'] = (32, 32, svg(32, 32, gem(16, 16, 22, 30, 2.5)))
S['icon_refused'] = (32, 32, svg(32, 32, f'<circle cx="16" cy="16" r="14" fill="{INK}" stroke="#FF4D4D" stroke-width="3"/>'
    '<path d="M10,10 L22,22 M22,10 L10,22" stroke="#FF4D4D" stroke-width="4" stroke-linecap="round"/>'))

# --- logo 600x200 (end screen): title + 4 beams crossing on a gem
P4 = ['#56B4E9', '#E69F00', '#009E73', '#CC79A7']
def logo():
    o = ''
    for i, (c, y) in enumerate(zip(P4, [52, 76, 124, 148])):
        o += (f'<rect x="0" y="{y-5}" width="600" height="10" rx="5" fill="{c}" opacity=".25" filter="url(#bl)"/>'
              f'<rect x="20" y="{y-2}" width="560" height="4" rx="2" fill="{c}" opacity=".75"/>')
    T = '<text x="300" y="124" text-anchor="middle" font-family="Helvetica Neue, Helvetica" font-weight="bold" font-size="86" letter-spacing="4" '
    o += T + f'fill="{INK}" stroke="{INK}" stroke-width="12" stroke-linejoin="round">GLINTWATCH</text>' + T + 'fill="#F4F6FF">GLINTWATCH</text>'  # sips ignores paint-order
    o += f'<g transform="translate(0,0)">{gem(300, 30, 26, 36, 3)}</g>'
    return svg(600, 200, o, '<filter id="bl" x="-5%" y="-100%" width="110%" height="300%"><feGaussianBlur stdDeviation="4"/></filter>')
S['logo'] = (600, 200, logo())


# --- v3 states (UX.md 12)
def dashed_ring(r, w, dash, gap): return f'<circle cx="64" cy="64" r="{r}" fill="none" stroke="{INK}" stroke-width="{w+4}" stroke-dasharray="{dash} {gap}"/><circle cx="64" cy="64" r="{r}" fill="none" stroke="#FFFFFF" stroke-width="{w}" stroke-dasharray="{dash} {gap}"/>'
S['mirror_pending'] = (128, 128, svg(128, 128, dashed_ring(46, 6, 14, 9.5)))  # r 0.30*cell + 4 px; tinted placer colour
S['pending_tick'] = (6, 12, svg(6, 12, f'<rect x="0.5" y="0.5" width="5" height="11" rx="1.5" fill="#FFFFFF" stroke="{INK}" stroke-width="1"/>'))
CR1 = 'M50,70 L58,64 L55,58 L63,52'
CR2 = 'M74,46 L68,54 L75,58 L70,66'
def crack(paths): return ''.join(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="4.5" stroke-linejoin="round" stroke-linecap="round"/>' for d in paths)
S['mirror_crack_1'] = (128, 128, svg(128, 128, crack([CR1])))  # overlay on mirror_bar, same rotation
S['mirror_crack_2'] = (128, 128, svg(128, 128, crack([CR1, CR2, 'M86,36 L80,40'])))
def shards(f):
    r = random.Random(77); o = ''; d = [6, 22, 38, 52][f]; a = [1, .9, .6, .3][f]
    for k in range(7):
        t0 = math.radians(-45 + (k - 3) * 9 + r.uniform(-4, 4)) if k % 2 else math.radians(135 + (k - 3) * 9)
        ang = t0 + math.radians(r.uniform(-40, 40)); cx, cy = 64 + d * math.cos(ang) + (k - 3) * 6 * (1 - f / 4), 64 + d * math.sin(ang) - (k - 3) * 6 * (1 - f / 4)
        s = 7 - f; rot = r.uniform(0, 180) + f * 50
        o += (f'<g transform="translate({cx:.1f},{cy:.1f}) rotate({rot:.0f})" opacity="{a}"><polygon points="{-s*1.6:.1f},{-s*.5:.1f} {s*1.4:.1f},{-s*.8:.1f} {s*.4:.1f},{s*.9:.1f}" '
              f'fill="#D8DEEA" stroke="{INK}" stroke-width="2" stroke-linejoin="round"/></g>')
    return o
S['shatter_sheet'] = (512, 128, svg(512, 128, ''.join(f'<g transform="translate({128*f},0)">{shards(f)}</g>' for f in range(4))))
def gem_outline(cx, cy, w, h):
    pts = f'{cx},{cy-h/2} {cx+w/2},{cy} {cx},{cy+h/2} {cx-w/2},{cy}'
    return (f'<polygon points="{pts}" fill="none" stroke="#8E9BB3" stroke-width="10.5" stroke-dasharray="18 12" stroke-linejoin="round"/>')
S['crystal_outline'] = (128, 128, svg(128, 128, gem_outline(64, 64, GEM_W - 10, 114)))  # dormant; not tinted; same box as crystal.png
# streak / timer chip (UX 12, owner 2026-11-11): ink pill + white 2 px border, tinted (ink stays dark under multiply).
# Drawn at 2x of fs=22: 64x51 (ratio 1.45/1.15); scale to 1.45*fs x 1.15*fs. Text (x2..x4, arrows) is a Text entity.
S['streak_badge'] = (64, 51, svg(64, 51, f'<rect x="2" y="2" width="60" height="47" rx="16" fill="{INK}" stroke="#FFFFFF" stroke-width="4"/>'))
S['streak_badge_dashed'] = (64, 51, svg(64, 51, f'<rect x="2" y="2" width="60" height="47" rx="16" fill="{INK}"/>'
    '<rect x="2" y="2" width="60" height="47" rx="16" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-dasharray="9 6"/>'))  # contested (white) / decaying (60 %)

ANCHORS = {'background': (0, 0), 'floor_checker': (0, 0), 'floor_0': (0, 0), 'floor_1': (0, 0), 'zone_tile': (0, 0), 'panel_fill': (0, 0), 'panel_frame': (0, 0),
           'beam_solid': (0, .5), 'beam_dash': (0, .5), 'beam_dots': (0, .5), 'beam_double': (0, .5), 'zone_border': (0, .5)}
TINT = {'mirror_pending', 'streak_badge', 'streak_badge_dashed', 'crystal', 'crystal_glow', 'lock_arc', 'highlight_inner', 'lamp_disc', 'lamp_nose', 'zone_tile', 'zone_border', 'panel_frame',
        'icon_crystal', 'spark_sheet', 'flicker_sheet', 'beam_solid', 'beam_dash', 'beam_dots', 'beam_double'} | {'emblem_' + e for e in EMBLEMS}

def export():
    os.makedirs(SVG, exist_ok=True); os.makedirs(PNG, exist_ok=True); manifest = {}
    for name, (w, h, text) in S.items():
        sp = os.path.join(SVG, name + '.svg'); pp = os.path.join(PNG, name + '.png')
        open(sp, 'w').write(text)
        subprocess.run(['sips', '-s', 'format', 'png', sp, '--out', pp], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        manifest[name] = dict(w=w, h=h, anchor=ANCHORS.get(name, (.5, .5)), tint=name in TINT)
    for name, fw, n in (('spark_sheet', 64, 4), ('flicker_sheet', 128, 4), ('shatter_sheet', 128, 4)):  # PIXI spritesheet JSON
        fh = S[name][1]
        frames = {f'{name[:-6]}_{i}': dict(frame=dict(x=fw*i, y=0, w=fw, h=fh), sourceSize=dict(w=fw, h=fh),
                  spriteSourceSize=dict(x=0, y=0, w=fw, h=fh), rotated=False, trimmed=False) for i in range(n)}
        json.dump(dict(frames=frames, meta=dict(image=name + '.png', size=dict(w=fw*n, h=fh), scale='1')),
                  open(os.path.join(PNG, name + '.json'), 'w'), indent=1)
    json.dump(manifest, open(os.path.join(PNG, 'manifest.json'), 'w'), indent=1)
    print(len(S), 'sprites exported')

if __name__ == '__main__':
    export()
