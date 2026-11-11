import sys, zlib, struct
def read(p):
    d=open(p,'rb').read(); assert d[:8]==b'\x89PNG\r\n\x1a\n'; i=8; idat=b''
    while i<len(d):
        n,=struct.unpack('>I',d[i:i+4]); t=d[i+4:i+8]; c=d[i+8:i+8+n]; i+=12+n
        if t==b'IHDR': w,h,bd,ct,_,_,il=struct.unpack('>IIBBBBB',c)
        elif t==b'IDAT': idat+=c
    assert bd==8 and ct in (2,6) and il==0,(bd,ct,il)
    bpp=4 if ct==6 else 3; raw=zlib.decompress(idat); st=w*bpp; rows=[]; prev=bytearray(st); o=0
    for y in range(h):
        f=raw[o]; line=bytearray(raw[o+1:o+1+st]); o+=1+st
        for x in range(st):
            a=line[x-bpp] if x>=bpp else 0; b=prev[x]; c=prev[x-bpp] if x>=bpp else 0
            if f==1: line[x]=(line[x]+a)&255
            elif f==2: line[x]=(line[x]+b)&255
            elif f==3: line[x]=(line[x]+((a+b)>>1))&255
            elif f==4:
                p=a+b-c; pa,pb,pc=abs(p-a),abs(p-b),abs(p-c)
                line[x]=(line[x]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
        rows.append(line); prev=line
    return w,h,bpp,rows
def write(p,w,h,bpp,rows):
    ch=lambda t,c: struct.pack('>I',len(c))+t+c+struct.pack('>I',zlib.crc32(t+c)&0xffffffff)
    raw=b''.join(b'\x00'+bytes(r) for r in rows)
    open(p,'wb').write(b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6 if bpp==4 else 2,0,0,0))+ch(b'IDAT',zlib.compress(raw,9))+ch(b'IEND',b''))
if __name__=='__main__':
    src,dst,cw,chh=sys.argv[1],sys.argv[2],int(sys.argv[3]),int(sys.argv[4])
    w,h,bpp,rows=read(src); rows=[r[:cw*bpp] for r in rows[:chh]]; write(dst,cw,chh,bpp,rows)
    print(dst,cw,chh,'alpha00=',rows[0][3] if bpp==4 else None)
