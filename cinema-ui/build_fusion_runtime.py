from pathlib import Path
import subprocess,zipfile,struct,hashlib
root=Path(__file__).resolve().parent;tools=root.parent/'build-tools';bt=tools/'android-15';sdk=tools/'android-35/android.jar';build=root/'build';build.mkdir(exist_ok=True)
def run(*args):
 if str(args[0]).endswith('/d8'):
  with open(root/'build/d8-build.log','a') as log:result=subprocess.run([str(x) for x in args],stdout=log,stderr=log)
  if result.returncode:raise RuntimeError('D8 failed; see build/d8-build.log')
 else:subprocess.run([str(x) for x in args],check=True)
def dex_classes(d):
 so=struct.unpack_from('<I',d,60)[0];to=struct.unpack_from('<I',d,68)[0];ct,co=struct.unpack_from('<II',d,96);out=set()
 for i in range(ct):
  t=struct.unpack_from('<I',d,co+i*32)[0];s=struct.unpack_from('<I',d,to+t*4)[0];a=struct.unpack_from('<I',d,so+s*4)[0]
  while d[a]&128:a+=1
  a+=1;c=d[a:d.index(b'\0',a)].decode();out.add(c[1:-1]+'.class')
 return out
api=build/'android-api-only.jar'
with zipfile.ZipFile(sdk) as source,zipfile.ZipFile(api,'w') as target:
 for n in source.namelist():
  if not n.startswith(('java/','javax/','org/w3c/','org/xml/sax/','sun/','jdk/')):target.writestr(n,source.read(n))
run('python3',root/'fetch_media3.py')
libs=sorted((root/'runtime-libs').glob('*.jar'));cp=':'.join(str(p) for p in libs)
run('java','-XX:-UsePerfData','-jar',tools/'ecj.jar','-17','-proc:none','-nowarn','-classpath',str(api)+':'+cp,'-d',build/'runtime-classes',*list((root/'runtime-src').rglob('*.java')))
base=set()
with zipfile.ZipFile(root/'base.apk') as z:
 for n in z.namelist():
  if n.startswith('classes') and n.endswith('.dex'):base.update(dex_classes(z.read(n)))
seen={};program=[]
with zipfile.ZipFile(build/'runtime.jar','w') as z:
 for p in (build/'runtime-classes').rglob('*.class'):z.write(p,str(p.relative_to(build/'runtime-classes')))
for lib in [build/'runtime.jar']+libs:
 filtered=build/('program-'+lib.name)
 with zipfile.ZipFile(lib) as source,zipfile.ZipFile(filtered,'w') as target:
  for n in source.namelist():
   if n=='module-info.class' or n.endswith('/module-info.class'):continue
   # Not used by our player; needs AAR string resources absent from the preserved base table.
   if n=='androidx/media3/exoplayer/offline/DownloadNotificationHelper.class':continue
   data=source.read(n)
   if n.endswith('.class'):
    if n in base:continue
    digest=hashlib.sha256(data).hexdigest()
    if n in seen:
     if seen[n]!=digest:raise RuntimeError('Conflicting class '+n)
     continue
    seen[n]=digest
   target.writestr(n,data)
 program.append(filtered)
(build/'host-dex').mkdir(exist_ok=True)
run(bt/'d8','--min-api','26','--lib',sdk,'--output',build/'host-dex',*program)
