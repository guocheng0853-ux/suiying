#!/usr/bin/env python3
"""最小 zipalign：支持 [-f] [-p] [-P <KB>] <align> in out，以及 -c 检查。
未压缩条目数据按 align 对齐；未压缩 .so 按页（-p 4KB / -P N KB）对齐。"""
import sys,struct,zlib
def parse(a):
    o={'f':False,'c':False,'page':None};i=0;pos=[]
    while i<len(a):
        x=a[i]
        if x=='-f':o['f']=True
        elif x=='-c':o['c']=True
        elif x=='-p':o['page']=4096
        elif x=='-P':i+=1;o['page']=int(a[i])*1024
        elif x in('-v','-z'):pass
        else:pos.append(x)
        i+=1
    return o,pos
def entries(d):
    e=d.rfind(b'PK\5\6');n,size,off=struct.unpack_from('<HII',d,e+10);out=[];p=off
    for _ in range(n):
        f=struct.unpack_from('<4s6H3I5H2I',d,p);nl,xl,cl=f[10],f[11],f[12]
        out.append(dict(cd=d[p:p+46],name=d[p+46:p+46+nl],cx=d[p+46+nl:p+46+nl+xl],cc=d[p+46+nl+xl:p+46+nl+xl+cl],method=f[4],csize=f[8],lho=f[16],flags=f[3]))
        p+=46+nl+xl+cl
    return out,d[e:]
def want(en,align,page):
    if en['method']!=0:return 1
    return page if (page and en['name'].endswith(b'.so')) else align
def main():
    o,pos=parse(sys.argv[1:]);align=int(pos[0]);d=open(pos[1],'rb').read();ents,eocd=entries(d)
    if o['c']:
        bad=0
        for en in ents:
            nl,xl=struct.unpack_from('<HH',d,en['lho']+26);data=en['lho']+30+nl+xl;w=want(en,align,o['page'])
            if data%w:print('BAD',en['name'].decode(errors='replace'),data);bad+=1
        print('Verification','FAILED' if bad else 'succesful');sys.exit(1 if bad else 0)
    out=bytearray();cd=bytearray()
    for en in ents:
        h=en['lho'];nl,xl=struct.unpack_from('<HH',d,h+26);fixed=bytearray(d[h:h+30]);name=d[h+30:h+30+nl]
        data=d[h+30+nl+xl:h+30+nl+xl+en['csize']];desc=b''
        if en['flags']&8:
            q=h+30+nl+xl+en['csize'];desc=d[q:q+16] if d[q:q+4]==b'PK\7\x08' else d[q:q+12]
        w=want(en,align,o['page']);start=len(out);base=start+30+nl;pad=(-base)%w if w>1 else 0
        extra=b'\0'*pad
        struct.pack_into('<H',fixed,28,len(extra));out+=fixed+name+extra+data+desc
        c=bytearray(en['cd']);struct.pack_into('<I',c,42,start);cd+=c+en['name']+en['cx']+en['cc']
    cdo=len(out);out+=cd;e=bytearray(eocd);struct.pack_into('<II',e,12,len(cd),cdo);out+=e
    open(pos[2],'wb').write(out)
main()
