import math, sys
exec(open('state.py').read())
mir[(7,3)]=[1,0,0]; mir[(11,7)]=[0,2,1]; mir[(9,11)]=[0,0,2]; mir[(9,14)]=[0,3,2]; mir[(3,8)]=[0,5,3]
LIFE={(7,3):3,(11,7):14,(9,11):9,(9,14):18,(3,8):11}
PEND={(10,12):(2,1)}  # rival-view: placer, type
new={(10,12):(2,'place'),(3,8):(3,'turn')}
DORM={(5,1),(13,5),(9,13),(1,9)}
TIMER={(5,1):'↑2',(13,5):'↑2',(9,13):'↑2',(1,9):'↑2',(7,13):'↓3',(5,7):'↓1'}
DECAY={(8,4):(1,2)}
STREAK={(7,5):4,(7,9):2,(7,13):1,(9,7):3,(5,7):4,(1,7):1,(5,8):1,(10,8):1,(7,7):4}
COL=['#56B4E9','#E69F00','#009E73','#CC79A7']; DARK=['#1F5F86','#7A5300','#004D38','#6E3A58']
NAME=['Sky','Amber','Jade','Orchid']
INK='#0E1018'; TXT='#E8EAF0'; MUT='#A0A6B8'; PAN='#232736'; GRID='#3A4054'; WH='#F4F6FF'
cell=66; X0=465; Y0=45; W=9
out=[]
def a(s): out.append(s)
def cc(p): return X0+p[0]*cell+cell/2, Y0+p[1]*cell+cell/2
def emblem(s,x,y,r,fill='#FFFFFF',stroke=INK,sw=2):
  if s==0: return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/><circle cx="{x}" cy="{y}" r="{r*0.38:.1f}" fill="{stroke}"/>'
  if s==1: return f'<polygon points="{x},{y-r} {x+r*0.95:.1f},{y+r*0.75:.1f} {x-r*0.95:.1f},{y+r*0.75:.1f}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" stroke-linejoin="round"/>'
  if s==2: return f'<rect x="{x-r*0.85:.1f}" y="{y-r*0.85:.1f}" width="{r*1.7:.1f}" height="{r*1.7:.1f}" rx="{r*0.2:.1f}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/><rect x="{x-r*0.3:.1f}" y="{y-r*0.3:.1f}" width="{r*0.6:.1f}" height="{r*0.6:.1f}" fill="{stroke}"/>'
  pts=[];
  for k in range(8):
    rr=r if k%2==0 else r*0.42; an=-math.pi/2+k*math.pi/4; pts.append(f'{x+rr*math.cos(an):.1f},{y+rr*math.sin(an):.1f}')
  return f'<polygon points="{" ".join(pts)}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" stroke-linejoin="round"/>'
a('<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080" font-family="Helvetica, Arial, sans-serif">')
a('<defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1A1D29"/><stop offset="1" stop-color="#0E1018"/></linearGradient>'
  '<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter></defs>')
a('<rect width="1920" height="1080" fill="url(#bg)"/>')
# board
a(f'<rect x="{X0-6}" y="{Y0-6}" width="{N*cell+12}" height="{N*cell+12}" rx="10" fill="{INK}" stroke="{GRID}" stroke-width="2"/>')
for y in range(N):
  for x in range(N):
    a(f'<rect x="{X0+x*cell}" y="{Y0+y*cell}" width="{cell}" height="{cell}" fill="{"#1B2D45" if (x+y)%2==0 else "#18283E"}"/>')
# zones
for s,(lp,d) in lamps.items():
  xs=[x for x in range(lp[0]-2,lp[0]+3) if 0<=x<N]; ys=[y for y in range(lp[1]-2,lp[1]+3) if 0<=y<N]
  rx,ry=X0+xs[0]*cell,Y0+ys[0]*cell; rw,rh=len(xs)*cell,len(ys)*cell
  a(f'<rect x="{rx}" y="{ry}" width="{rw}" height="{rh}" fill="{COL[s]}" fill-opacity="0.16" stroke="{COL[s]}" stroke-width="3" stroke-dasharray="12 8" stroke-opacity="0.8"/>')
# rocks
for p in walls:
  x,y=X0+p[0]*cell,Y0+p[1]*cell; m=cell*0.08
  a(f'<rect x="{x+m+3}" y="{y+m+4}" width="{cell-2*m}" height="{cell-2*m}" rx="{cell*0.12:.0f}" fill="{INK}" opacity="0.5"/>')
  a(f'<rect x="{x+m}" y="{y+m}" width="{cell-2*m}" height="{cell-2*m}" rx="{cell*0.12:.0f}" fill="#0A111D" stroke="#6E7590" stroke-width="3"/>')
  a(f'<path d="M{x+m+6},{y+cell*0.5} L{x+m+6},{y+m+6} L{x+cell*0.62},{y+m+6}" fill="none" stroke="#6E7590" stroke-width="5" stroke-linecap="round"/>')
  a(f'<path d="M{x+cell*0.3},{y+cell*0.72} L{x+cell*0.5},{y+cell*0.55} L{x+cell*0.7},{y+cell*0.66}" fill="none" stroke="{INK}" stroke-width="3"/>')
# beams
tr={s:trace(s) for s in range(4)}
litby={}
for s in range(4):
  for q in tr[s][1]: litby.setdefault(q,[]).append(s)
def beam_pts(s):
  pts,lit,end,d=tr[s]; o=(s-1.5)*(W+2)
  P=[]
  for p in pts[:-1]:
    x,y=cc(p); P.append((x+o,y+o))
  x,y=cc(pts[-1]); P.append((x-d[0]*cell/2+o*(d[0]==0), y-d[1]*cell/2+o*(d[1]==0)))
  # fix: offset applies perpendicular only for last point
  x,y=cc(pts[-1]); P[-1]=(x-d[0]*cell/2 + (o if d[0]==0 else 0), y-d[1]*cell/2 + (o if d[1]==0 else 0))
  return P
for s in range(4):
  P=beam_pts(s); pl=' '.join(f'{x:.1f},{y:.1f}' for x,y in P)
  a(f'<polyline points="{pl}" fill="none" stroke="{COL[s]}" stroke-width="{2*W+6}" stroke-opacity="0.22" stroke-linejoin="miter"/>')
for s in range(4):
  P=beam_pts(s); pl=' '.join(f'{x:.1f},{y:.1f}' for x,y in P)
  base=f'fill="none" stroke-linejoin="miter" points="{pl}"'
  if s==0: a(f'<polyline {base} stroke="{COL[s]}" stroke-width="{W}"/>')
  if s==1: a(f'<polyline {base} stroke="{COL[s]}" stroke-width="{W}" stroke-dasharray="22 10"/>')
  if s==2: a(f'<polyline {base} stroke="{COL[s]}" stroke-width="{W+2}" stroke-dasharray="0.1 15" stroke-linecap="round"/>')
  if s==3:
    a(f'<polyline {base} stroke="{COL[s]}" stroke-width="{W+1}"/>'); a(f'<polyline {base} stroke="{INK}" stroke-width="3"/>')
# crystals
FS=max(17,round(cell*0.32))
def chip(x,y,text,txt,stroke,dash,side,op=1):
  bw=FS*1.45; bh=FS*1.15; by=y+cell*0.48-bh
  bx=x+cell*0.48-bw if side=='R' else x-cell*0.48
  a(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bw:.1f}" height="{bh:.1f}" rx="{bh/2:.1f}" fill="{INK}" stroke="{stroke}" stroke-width="2" stroke-dasharray="{dash}" opacity="{op}"/>')
  a(f'<text x="{bx+bw/2:.1f}" y="{by+bh*0.78:.1f}" font-size="{FS}" font-weight="bold" fill="{txt}" text-anchor="middle" opacity="{op}">{text}</text>')
def tpill(p,x,y):
  if p in TIMER: chip(x,y,TIMER[p],WH,'#8E9BB3','none','L')
for p,v in crys.items():
  x,y=cc(p); hw=cell*(0.20+0.06*v); hh=hw*1.25; L=litby.get(p,[])
  if p in DORM:
    a(f'<polygon points="{x},{y-hh:.1f} {x+hw:.1f},{y} {x},{y+hh:.1f} {x-hw:.1f},{y}" fill="none" stroke="#8E9BB3" stroke-width="3" stroke-dasharray="5 4" stroke-linejoin="round"/>')
    a(f'<text x="{x}" y="{y+cell*0.11:.1f}" font-size="{cell*0.30:.0f}" font-weight="bold" fill="#8E9BB3" text-anchor="middle">{v}</text>')
    tpill(p,x,y)
    continue
  fill='#8E9BB3' if not L else (COL[L[0]] if len(L)==1 else WH)
  if len(L)==1: a(f'<circle cx="{x}" cy="{y}" r="{cell*0.42:.0f}" fill="{COL[L[0]]}" opacity="0.45" filter="url(#glow)"/>')
  if len(L)>=2:
    a(f'<circle cx="{x}" cy="{y}" r="{cell*0.44:.0f}" fill="none" stroke="{WH}" stroke-width="3" stroke-dasharray="6 6"/>')
  a(f'<polygon points="{x},{y-hh:.1f} {x+hw:.1f},{y} {x},{y+hh:.1f} {x-hw:.1f},{y}" fill="{fill}" stroke="{INK}" stroke-width="3" stroke-linejoin="round"/>')
  a(f'<polygon points="{x},{y-hh:.1f} {x+hw:.1f},{y} {x},{y}" fill="#FFFFFF" opacity="0.35"/>')
  a(f'<text x="{x}" y="{y+cell*0.11:.1f}" font-size="{cell*0.30:.0f}" font-weight="bold" fill="{INK}" text-anchor="middle">{v}</text>')
  if len(L)>=2:
    # crackle
    a(f'<path d="M{x-hw-6:.0f},{y-10} l8,6 l-6,6 l10,4 M{x+hw+6:.0f},{y-8} l-8,6 l6,6 l-10,4" fill="none" stroke="{WH}" stroke-width="3"/>')
    a(f'<rect x="{x+cell*0.12:.0f}" y="{y-cell*0.5+2:.0f}" width="26" height="22" rx="5" fill="{INK}" stroke="{WH}" stroke-width="2"/><text x="{x+cell*0.12+13:.0f}" y="{y-cell*0.5+19:.0f}" font-size="17" font-weight="bold" fill="{WH}" text-anchor="middle">0</text>')
  if len(L)==1:
    a(emblem(L[0], x+cell*0.30, y-cell*0.30, 12, fill=COL[L[0]], sw=2.5))
  tpill(p,x,y)
  def badge(k,txt,stroke,dash,op=1):
    if k>=2: chip(x,y,f'x{k}',txt,stroke,dash,'R',op)
  if not L and p in DECAY:
    o,k=DECAY[p]; badge(k,COL[o],COL[o],"3 2",0.6)
  if L and p in STREAK:
    k=STREAK[p]
    if len(L)==1: badge(k,COL[L[0]],COL[L[0]],"none")
    else: badge(k,WH,WH,"3 2")
# mirrors
for p,(own,t) in PEND.items():
  x,y=cc(p); r=cell*0.30
  s,kind=new[p]
  a(f'<rect x="{x-cell/2+3}" y="{y-cell/2+3}" width="{cell-6}" height="{cell-6}" rx="6" fill="{WH}" fill-opacity="0.18" stroke="{WH}" stroke-width="5"/>')
  a(f'<circle cx="{x}" cy="{y}" r="{r+4:.1f}" fill="none" stroke="{COL[own]}" stroke-width="4" stroke-dasharray="7 5"/>')
  L=cell*0.40; dx=L*math.cos(math.radians(45)); dy=-dx if t==0 else dx
  a(f'<line x1="{x-dx:.1f}" y1="{y-dy:.1f}" x2="{x+dx:.1f}" y2="{y+dy:.1f}" stroke="#D8DEEA" stroke-opacity="0.4" stroke-width="{cell*0.16:.1f}" stroke-linecap="round" stroke-dasharray="6 5"/>')
  for i in range(2): a(f'<rect x="{x+cell*0.18+i*9:.1f}" y="{y+cell*0.24:.1f}" width="6" height="12" rx="2" fill="{COL[own]}"/>')
  a(emblem(own,x-cell*0.32,y-cell*0.32,11,fill=COL[own],sw=2))
for p,(t,lock,own) in mir.items():
  x,y=cc(p); r=cell*0.30
  if p in new:
    s,kind=new[p]
    a(f'<rect x="{x-cell/2+3}" y="{y-cell/2+3}" width="{cell-6}" height="{cell-6}" rx="6" fill="{WH}" fill-opacity="0.18" stroke="{WH}" stroke-width="5"/>')
    a(f'<rect x="{x-cell/2+3}" y="{y-cell/2+3}" width="{cell-6}" height="{cell-6}" rx="6" fill="none" stroke="{COL[s]}" stroke-width="3"/>')
  a(f'<circle cx="{x}" cy="{y}" r="{r:.1f}" fill="{GRID}" stroke="{INK}" stroke-width="3"/>')
  if lock>0:
    R0=cell*0.40
    for k in range(5):
      a0=math.radians(-90+k*72+9); a1=math.radians(-90+k*72+63)
      col=COL[own] if k<lock else '#4A5064'
      a(f'<path d="M{x+R0*math.cos(a0):.1f},{y+R0*math.sin(a0):.1f} A{R0:.1f},{R0:.1f} 0 0 1 {x+R0*math.cos(a1):.1f},{y+R0*math.sin(a1):.1f}" fill="none" stroke="{col}" stroke-width="6" stroke-linecap="round"/>')
  L=cell*0.40; dx=L*math.cos(math.radians(45)); dy=-dx if t==0 else dx
  a(f'<line x1="{x-dx:.1f}" y1="{y-dy:.1f}" x2="{x+dx:.1f}" y2="{y+dy:.1f}" stroke="{INK}" stroke-width="{cell*0.16+4:.1f}" stroke-linecap="round"/>')
  lf=LIFE.get(p,20); op=0.65 if lf<=3 else 1
  a(f'<line x1="{x-dx:.1f}" y1="{y-dy:.1f}" x2="{x+dx:.1f}" y2="{y+dy:.1f}" stroke="#D8DEEA" stroke-opacity="{op}" stroke-width="{cell*0.16:.1f}" stroke-linecap="round"/>')
  if lf<=7:
    a(f'<path d="M{x-4},{y-5} l6,4 l-5,4 l6,3" fill="none" stroke="{INK}" stroke-width="2.5"/>')
  if lf<=3:
    a(f'<path d="M{x+dx*0.5:.1f},{y+dy*0.5-6:.1f} l-5,5 l6,3 l-4,5 M{x-dx*0.5:.1f},{y-dy*0.5-4:.1f} l5,4 l-6,3" fill="none" stroke="{INK}" stroke-width="2.5"/>')
  if p in new:
    s,kind=new[p]; a(emblem(s,x-cell*0.32,y-cell*0.32,11,fill=COL[s],sw=2))
# lamps
for s,(lp,d) in lamps.items():
  x,y=cc(lp); r=cell*0.42
  ang=math.degrees(math.atan2(d[1],d[0]))
  a(f'<g transform="rotate({ang} {x} {y})"><polygon points="{x+r+8},{y} {x+r*0.4},{y-r*0.75} {x+r*0.4},{y+r*0.75}" fill="{COL[s]}" stroke="{INK}" stroke-width="3"/></g>')
  a(f'<circle cx="{x}" cy="{y}" r="{r:.1f}" fill="{DARK[s]}" stroke="{COL[s]}" stroke-width="5"/>')
  a(emblem(s,x,y,15,fill=COL[s],sw=2.5))
# strike spark at lamp 3
x,y=cc(lamps[3][0]); xr=x+cell*0.5
for k in range(8):
  an=math.radians(k*45+22); a(f'<line x1="{xr+10*math.cos(an):.1f}" y1="{y+10*math.sin(an):.1f}" x2="{xr+26*math.cos(an):.1f}" y2="{y+26*math.sin(an):.1f}" stroke="{WH}" stroke-width="4" stroke-linecap="round"/>')
def float_text(x,y,t,col):
  a(f'<text x="{x}" y="{y}" font-size="34" font-weight="bold" fill="{INK}" stroke="{INK}" stroke-width="8" stroke-linejoin="round" text-anchor="middle">{t}</text>')
  a(f'<text x="{x}" y="{y}" font-size="34" font-weight="bold" fill="{col}" text-anchor="middle">{t}</text>')
float_text(xr+20, y-40, '+2', COL[2])
# panels
names=['NeonKeeper','amber_bot','jade.v3','OrchidAI']
scores=[402,388,251,317]; gains=[13,15,2,4]; stock=[3,1,4,0]; litn=[3,3,0,2]
acts=['WAIT','WAIT','PLACE 10 12 \\','TURN 3 8']; msgs=['holding col 7','','strike!','north road']
pos={0:(40,40),1:(1500,40),2:(1500,740),3:(40,740)}
for s in range(4):
  px,py=pos[s]; pw,ph=380,300
  a(f'<rect x="{px}" y="{py}" width="{pw}" height="{ph}" rx="12" fill="{PAN}" stroke="{COL[s]}" stroke-width="3"/>')
  a(f'<rect x="{px}" y="{py}" width="12" height="{ph}" rx="6" fill="{COL[s]}"/>')
  a(f'<rect x="{px+28}" y="{py+24}" width="112" height="112" rx="10" fill="{INK}" stroke="{COL[s]}" stroke-width="3"/>')
  a(f'<text x="{px+84}" y="{py+88}" font-size="18" fill="#6B7185" text-anchor="middle">avatar</text>')
  a(f'<circle cx="{px+136}" cy="{py+132}" r="24" fill="{PAN}" stroke="{COL[s]}" stroke-width="3"/>'); a(emblem(s,px+136,py+132,15,fill=COL[s],sw=2.5))
  a(f'<text x="{px+156}" y="{py+56}" font-size="30" font-weight="bold" fill="{TXT}">{names[s]}</text>')
  a(f'<text x="{px+172}" y="{py+130}" font-size="72" font-weight="bold" fill="{COL[s]}">{scores[s]}</text>')
  a(f'<text x="{px+352}" y="{py+254}" font-size="34" font-weight="bold" fill="{WH}" text-anchor="end">+{gains[s]}</text>')
  # stock row
  mx,my=px+44,py+182
  a(f'<circle cx="{mx}" cy="{my}" r="15" fill="{GRID}" stroke="{INK}" stroke-width="2"/><line x1="{mx-11}" y1="{my+11}" x2="{mx+11}" y2="{my-11}" stroke="#D8DEEA" stroke-width="6" stroke-linecap="round"/>')
  a(f'<text x="{mx+26}" y="{my+11}" font-size="32" font-weight="bold" fill="{TXT}">{stock[s]}</text><text x="{mx+50}" y="{my+10}" font-size="22" fill="{MUT}">/6  +1 in 3</text>')
  cx2=px+262
  a(f'<polygon points="{cx2},{my-17} {cx2+13},{my} {cx2},{my+17} {cx2-13},{my}" fill="{COL[s]}" stroke="{INK}" stroke-width="2"/>')
  a(f'<text x="{cx2+22}" y="{my+11}" font-size="32" font-weight="bold" fill="{TXT}">{litn[s]}</text><text x="{cx2+48}" y="{my+10}" font-size="22" fill="{MUT}">lit</text>')
  a(f'<line x1="{px+28}" y1="{py+214}" x2="{px+352}" y2="{py+214}" stroke="{GRID}" stroke-width="2"/>')
  a(f'<text x="{px+28}" y="{py+252}" font-size="28" font-weight="bold" fill="{TXT}">{acts[s]}</text>')
  if msgs[s]: a(f'<text x="{px+28}" y="{py+284}" font-size="24" font-style="italic" fill="{MUT}">“{msgs[s]}”</text>')
# turn box (left middle)
bx,by=40,380
a(f'<rect x="{bx}" y="{by}" width="380" height="320" rx="12" fill="{PAN}" stroke="{GRID}" stroke-width="2"/>')
a(f'<text x="{bx+28}" y="{by+48}" font-size="24" font-weight="bold" fill="{MUT}">TURN</text>')
a(f'<text x="{bx+28}" y="{by+120}" font-size="72" font-weight="bold" fill="{TXT}">37<tspan font-size="36" fill="{MUT}"> / 150</tspan></text>')
a(f'<rect x="{bx+28}" y="{by+146}" width="324" height="14" rx="7" fill="{INK}"/><rect x="{bx+28}" y="{by+146}" width="{324*37/150:.0f}" height="14" rx="7" fill="{TXT}"/>')
a(f'<text x="{bx+28}" y="{by+206}" font-size="24" fill="{MUT}">Next mirror (all)</text><text x="{bx+352}" y="{by+206}" font-size="28" font-weight="bold" fill="{WH}" text-anchor="end">in 3</text>')
a(f'<text x="{bx+28}" y="{by+246}" font-size="24" fill="{MUT}">Contested now</text><text x="{bx+352}" y="{by+246}" font-size="28" font-weight="bold" fill="{WH}" text-anchor="end">1</text>')
a(f'<text x="{bx+28}" y="{by+286}" font-size="24" fill="{MUT}">Awake crystals</text><text x="{bx+352}" y="{by+286}" font-size="28" font-weight="bold" fill="{WH}" text-anchor="end">17 / 21</text>')
# legend (right middle)
lx,ly=1500,380
a(f'<rect x="{lx}" y="{ly}" width="380" height="330" rx="12" fill="{PAN}" stroke="{GRID}" stroke-width="2"/>')
a(f'<text x="{lx+28}" y="{ly+48}" font-size="24" font-weight="bold" fill="{MUT}">LEGEND</text>')
rows=[('crys','Crystal: size = 1-3 pts'),('lit','Lit alone: pays value x2-x4'),('cont','Lit by 2+: pays 0'),('mir','Mirror, arcs = lock 1-5'),('dorm','Dormant: pays nothing'),('zone','Yard: owner only')]
for i,(k,t) in enumerate(rows):
  ix,iy=lx+56,ly+80+i*34
  if k=='crys': a(f'<polygon points="{ix},{iy-15} {ix+12},{iy} {ix},{iy+15} {ix-12},{iy}" fill="#9AA0B4" stroke="{INK}" stroke-width="2"/>')
  if k=='lit': a(f'<polygon points="{ix},{iy-15} {ix+12},{iy} {ix},{iy+15} {ix-12},{iy}" fill="{COL[0]}" stroke="{INK}" stroke-width="2"/>'); a(emblem(0,ix+16,iy-12,7,fill=COL[0],sw=1.5))
  if k=='cont': a(f'<polygon points="{ix},{iy-15} {ix+12},{iy} {ix},{iy+15} {ix-12},{iy}" fill="{WH}" stroke="{INK}" stroke-width="2"/><circle cx="{ix}" cy="{iy}" r="18" fill="none" stroke="{WH}" stroke-width="2" stroke-dasharray="4 4"/>')
  if k=='mir':
    a(f'<circle cx="{ix}" cy="{iy}" r="11" fill="{GRID}" stroke="{INK}" stroke-width="2"/><circle cx="{ix}" cy="{iy}" r="16" fill="none" stroke="{COL[1]}" stroke-width="4" stroke-dasharray="20 6"/><line x1="{ix-10}" y1="{iy+10}" x2="{ix+10}" y2="{iy-10}" stroke="#D8DEEA" stroke-width="5" stroke-linecap="round"/>')
  if k=='dorm': a(f'<polygon points="{ix},{iy-15} {ix+12},{iy} {ix},{iy+15} {ix-12},{iy}" fill="none" stroke="#8E9BB3" stroke-width="2.5" stroke-dasharray="4 3"/>')
  if k=='rock': a(f'<rect x="{ix-14}" y="{iy-14}" width="28" height="28" rx="4" fill="#4A5064" stroke="{INK}" stroke-width="3"/>')
  if k=='zone': a(f'<rect x="{ix-14}" y="{iy-14}" width="28" height="28" fill="{COL[2]}" fill-opacity="0.15" stroke="{COL[2]}" stroke-width="3" stroke-dasharray="6 4"/>')
  a(f'<text x="{ix+36}" y="{iy+9}" font-size="24" fill="{TXT}">{t}</text>')
  if k=='zone': a(f'<text x="{ix+36}" y="{iy+33}" font-size="20" fill="{MUT}">empty seat: open to all</text>')
# beam pattern key
ky=ly+80+6*34+14
a(f'<text x="{lx+28}" y="{ky+16}" font-size="22" fill="{MUT}">beams:</text>')
for s in range(4):
  x1=lx+112+s*66; yy=ky+8
  if s==0: a(f'<line x1="{x1}" y1="{yy}" x2="{x1+50}" y2="{yy}" stroke="{COL[0]}" stroke-width="8"/>')
  if s==1: a(f'<line x1="{x1}" y1="{yy}" x2="{x1+50}" y2="{yy}" stroke="{COL[1]}" stroke-width="8" stroke-dasharray="14 6"/>')
  if s==2: a(f'<line x1="{x1+4}" y1="{yy}" x2="{x1+50}" y2="{yy}" stroke="{COL[2]}" stroke-width="10" stroke-dasharray="0.1 13" stroke-linecap="round"/>')
  if s==3: a(f'<line x1="{x1}" y1="{yy}" x2="{x1+50}" y2="{yy}" stroke="{COL[3]}" stroke-width="9"/><line x1="{x1}" y1="{yy}" x2="{x1+50}" y2="{yy}" stroke="{INK}" stroke-width="3"/>')
a('</svg>')
open(sys.argv[1],'w').write('\n'.join(out))
print(len(out),'elements', {k:v for k,v in litby.items() if len(v)>1})
