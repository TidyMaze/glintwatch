N=15; c=7; e=3
R=lambda p:(N-1-p[1],p[0])
lamps={0:((0,e),(1,0)),1:((N-1-e,0),(0,1)),2:((N-1,N-1-e),(-1,0)),3:((e,N-1),(0,-1))}
crys={}; walls=set(); mir={}
base_c={(5,1):2,(6,5):1,(4,6):3,(1,7):1,(5,7):2,(2,8-1):0}
base_c={(5,1):2,(6,5):1,(4,6):3,(1,7):1,(5,7):2}
base_w=[(6,2),(2,6)]
base_m={(4,4):0}
for p,v in base_c.items():
  q=p
  for k in range(4): crys[q]=v; q=R(q)
for p in base_w:
  q=p
  for k in range(4): walls.add(q); q=R(q)
for p,t in base_m.items():
  q=p
  for k in range(4): mir[q]=[t if k%2==0 else 1-t,0,None]; q=R(q)
crys[(c,c)]=3
import sys
def reflect(t,d):
  dx,dy=d
  return (-dy,-dx) if t==0 else (dy,dx)
def trace(s):
  pos,d=lamps[s]; pts=[pos]; lit=[]; end='exit'
  while True:
    pos=(pos[0]+d[0],pos[1]+d[1])
    if not(0<=pos[0]<N and 0<=pos[1]<N): end='exit'; break
    if pos in walls: end='wall'; break
    if any(lamps[o][0]==pos for o in lamps): end=('strike',[o for o in lamps if lamps[o][0]==pos][0]); break
    if pos in crys and pos not in lit: lit.append(pos)
    if pos in mir:
      d=reflect(mir[pos][0],d); pts.append(pos)
  pts.append(pos)
  return pts,lit,end,d
