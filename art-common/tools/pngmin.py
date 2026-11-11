# Lossless PNG minifier (pure Python): palette PNG + tRNS when <= 256 RGBA colours, else RGBA with per-row best filter.
# Dimensions and pixels are unchanged (anchors stay valid). usage: pngmin.py file.png [...]
import sys, os, zlib, struct
sys.path.insert(0, os.path.dirname(__file__)); import pngcrop as P
def chunk(t, c): return struct.pack('>I', len(c)) + t + c + struct.pack('>I', zlib.crc32(t + c) & 0xffffffff)
def filt(rows, bpp):
    out = bytearray(); prev = bytearray(len(rows[0]))
    for r in rows:
        best = None
        for f in range(5):
            line = bytearray(len(r))
            for x in range(len(r)):
                a = r[x-bpp] if x >= bpp else 0; b = prev[x]; c = prev[x-bpp] if x >= bpp else 0
                if f == 0: v = r[x]
                elif f == 1: v = r[x] - a
                elif f == 2: v = r[x] - b
                elif f == 3: v = r[x] - ((a + b) >> 1)
                else:
                    p = a + b - c; pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
                    v = r[x] - (a if pa <= pb and pa <= pc else b if pb <= pc else c)
                line[x] = v & 255
            score = sum(v if v < 128 else 256 - v for v in line)
            if best is None or score < best[0]: best = (score, f, line)
        out += bytes([best[1]]) + best[2]; prev = r
    return bytes(out)
for f in sys.argv[1:]:
    before = os.path.getsize(f)
    if open(f, 'rb').read(26)[25] == 3: print(f'{os.path.basename(f)} {before} already palette'); continue
    w, h, bpp, rows = P.read(f)
    px = [bytes(r[i:i+bpp]) for r in rows for i in range(0, len(r), bpp)]
    if bpp == 3: px = [p + b'\xff' for p in px]
    cols = {}
    for p in px:
        if p not in cols:
            cols[p] = len(cols)
            if len(cols) > 256: break
    if len(cols) <= 256:
        order = sorted(cols, key=lambda c: c[3]); idx = {c: i for i, c in enumerate(order)}
        prow = [bytes(idx[px[y*w + x]] for x in range(w)) for y in range(h)]
        raw = b''.join(b'\x00' + r for r in prow)
        plte = b''.join(c[:3] for c in order); trns = bytes(c[3] for c in order).rstrip(b'\xff')
        data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 3, 0, 0, 0)) + chunk(b'PLTE', plte) + (chunk(b'tRNS', trns) if trns else b'') + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
        kind = f'pal{len(cols)}'
    else:
        rgba = [bytearray(b''.join(px[y*w:(y+1)*w])) for y in range(h)]
        data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(filt(rgba, 4), 9)) + chunk(b'IEND', b'')
        kind = 'rgba'
    if len(data) < before: open(f, 'wb').write(data)
    print(f'{os.path.basename(f)} {before} -> {min(len(data), before)} {kind}')
