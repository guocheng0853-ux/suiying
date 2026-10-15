"""Compile a UI overlay while retaining the exact stable APK engines/native code."""
from pathlib import Path
import subprocess,zipfile,hashlib,shutil,json,struct

root=Path(__file__).resolve().parent;tools=root.parent/'build-tools';bt=tools/'android-15';sdk=tools/'android-35/android.jar';work=root/'build';work.mkdir(exist_ok=True)
def run(*args):
 if str(args[0]).endswith('/d8'):
  with open(root/'build/d8-build.log','a') as log:result=subprocess.run([str(x) for x in args],stdout=log,stderr=log)
  if result.returncode:raise RuntimeError('D8 failed; see build/d8-build.log')
 else:subprocess.run([str(x) for x in args],check=True)
for folder in ['classes','stubs','runtime-classes','dex','host-dex']:
 if (work/folder).exists():shutil.rmtree(work/folder)
run('python3',root/'build_fusion_runtime.py')
libs=sorted((root/'runtime-libs').glob('*.jar'));runtimecp=':'.join(str(p) for p in libs)+':'+str(work/'runtime-classes')
boot=str(sdk)+':'+str(bt/'core-lambda-stubs.jar')
run('java','-XX:-UsePerfData','-jar',tools/'ecj.jar','-8','-proc:none','-nowarn','-bootclasspath',boot,'-d',work/'stubs',*list((root/'stubs').rglob('*.java')))
run('java','-XX:-UsePerfData','-jar',tools/'ecj.jar','-8','-proc:none','-nowarn','-bootclasspath',boot,'-classpath',str(work/'stubs')+':'+runtimecp,'-d',work/'classes',*list((root/'src').rglob('*.java')))
with zipfile.ZipFile(work/'ui.jar','w') as z:
 for p in (work/'classes').rglob('*.class'):z.write(p,str(p.relative_to(work/'classes')))
with zipfile.ZipFile(work/'stubs.jar','w') as z:
 for p in (work/'stubs').rglob('*.class'):z.write(p,str(p.relative_to(work/'stubs')))
(work/'dex').mkdir(exist_ok=True)
run(bt/'d8','--min-api','26','--lib',sdk,'--classpath',work/'stubs.jar','--classpath',work/'runtime.jar','--output',work/'dex',work/'ui.jar')
jadx=tools/'jadx/lib/jadx-1.5.3-all.jar'
run('java','-jar',tools/'ecj.jar','-17','-proc:none','-nowarn','-classpath',jadx,'-d',work/'patch-tools',root/'tools/PatchClawNet.java',root/'tools/VerifyClawPatch.java',root/'tools/PatchSubtitleSearch.java',root/'tools/PatchHeaderRoute.java',root/'tools/PatchTorrentBoost.java')
with zipfile.ZipFile(root/'base.apk') as base:
 (work/'original-main.dex').write_bytes(base.read('classes.dex'))
(work/'patched-main.dex').unlink(missing_ok=True)
run('java','-cp',str(work/'patch-tools')+':'+str(jadx),'PatchClawNet',work/'original-main.dex',work/'patched-main.dex')
run('java','-cp',str(work/'patch-tools')+':'+str(jadx),'VerifyClawPatch',work/'original-main.dex',work/'patched-main.dex')
patched_main=(work/'patched-main.dex').read_bytes()
assert patched_main[:4]==b'dex\n' and struct.unpack_from('<I',patched_main,96)[0]>0
subtitle_name=None
with zipfile.ZipFile(root/'base.apk') as base:
 for name in base.namelist():
  if not (name.startswith('classes') and name.endswith('.dex')):continue
  d=base.read(name);so=struct.unpack_from('<I',d,60)[0];to=struct.unpack_from('<I',d,68)[0];count,off=struct.unpack_from('<II',d,96)
  for i in range(count):
   t=struct.unpack_from('<I',d,off+i*32)[0];si=struct.unpack_from('<I',d,to+t*4)[0];at=struct.unpack_from('<I',d,so+si*4)[0]
   while d[at]&128:at+=1
   at+=1
   if d[at:d.index(b'\0',at)]==b'Lcom/guocheng/mediapocket/PlayerSession;':subtitle_name=name;(work/'original-subtitle.dex').write_bytes(d)
assert subtitle_name
if subtitle_name=='classes.dex':(work/'original-subtitle.dex').write_bytes(patched_main)
(work/'patched-subtitle.dex').unlink(missing_ok=True)
run('java','-cp',str(work/'patch-tools')+':'+str(jadx),'PatchSubtitleSearch',work/'original-subtitle.dex',work/'patched-subtitle.dex')
patched_subtitle=(work/'patched-subtitle.dex').read_bytes()
assert patched_subtitle[:4]==b'dex\n' and struct.unpack_from('<I',patched_subtitle,96)[0]>0
# 0.1.46：VlcPlayback.setSource 前插 HeaderRoute（VLC 3 不支持 :http-header，额外请求头经本机代理补齐）
final_dex={'classes.dex':patched_main};final_dex[subtitle_name]=patched_subtitle
vlc_name=None
with zipfile.ZipFile(root/'base.apk') as base:
 for name in base.namelist():
  if name.startswith('classes') and name.endswith('.dex') and b'Lcom/guocheng/mediapocket/VlcPlayback;' in base.read(name):vlc_name=name
assert vlc_name
(work/'original-vlc.dex').write_bytes(final_dex.get(vlc_name) or zipfile.ZipFile(root/'base.apk').read(vlc_name))
(work/'patched-vlc.dex').unlink(missing_ok=True)
run('java','-cp',str(work/'patch-tools')+':'+str(jadx),'PatchHeaderRoute',work/'original-vlc.dex',work/'patched-vlc.dex')
final_dex[vlc_name]=(work/'patched-vlc.dex').read_bytes()
assert final_dex[vlc_name][:4]==b'dex\n'
# 0.1.48：TorrentEngine 会话参数与磁力 tracker（TorrentBoost）
torrent_name=None
with zipfile.ZipFile(root/'base.apk') as base:
 for name in base.namelist():
  if name.startswith('classes') and name.endswith('.dex') and b'Lcom/guocheng/mediapocket/TorrentEngine;' in base.read(name) and b'acquireSession' in base.read(name):torrent_name=name
assert torrent_name
(work/'original-torrent.dex').write_bytes(final_dex.get(torrent_name) or zipfile.ZipFile(root/'base.apk').read(torrent_name))
(work/'patched-torrent.dex').unlink(missing_ok=True)
run('java','-cp',str(work/'patch-tools')+':'+str(jadx),'PatchTorrentBoost',work/'original-torrent.dex',work/'patched-torrent.dex')
final_dex[torrent_name]=(work/'patched-torrent.dex').read_bytes()
assert final_dex[torrent_name][:4]==b'dex\n'
# 真实默认配置含个人令牌，不入库；缺失时用脱敏样例（打包名仍为 fusion-default.json）
if not (root/'assets/fusion-default.json').exists():
 shutil.copy(root/'assets/fusion-default.example.json',root/'assets/fusion-default.json');print('WARNING: assets/fusion-default.json missing, using sanitized example')
manifest=(root/'AndroidManifest.xml').read_text();(work/'AndroidManifest.xml').write_text(manifest)
for folder in ['res/drawable','res/values']:(work/folder).mkdir(parents=True,exist_ok=True)
(work/'res/drawable/icon.xml').write_text('<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#315f76"/></shape>')
(work/'res/values/styles.xml').write_text('<resources><style name="AppTheme" parent="android:style/Theme.Material.NoActionBar"/></resources>')
run(bt/'aapt2','compile','--dir',work/'res','-o',work/'resources.zip')
run(bt/'aapt2','link','-o',work/'manifest.apk','-I',sdk,'--manifest',work/'AndroidManifest.xml',work/'resources.zip')
resources=subprocess.check_output([str(bt/'aapt2'),'dump','resources',str(work/'manifest.apk')]).decode()
assert '0x7f010000 drawable/icon' in resources and '0x7f020000 style/AppTheme' in resources
# Fail before packaging if an intermediate DEX is incomplete or has no class definitions.
for folder in [work/'dex',work/'host-dex']:
 for dex_path in folder.glob('classes*.dex'):
  data=dex_path.read_bytes();assert data[:4]==b'dex\n' and struct.unpack_from('<I',data,96)[0]>0,dex_path
for data in final_dex.values():
 assert data[:4]==b'dex\n' and struct.unpack_from('<I',data,96)[0]>0
with zipfile.ZipFile(root/'base.apk') as base,zipfile.ZipFile(work/'manifest.apk') as manifests,zipfile.ZipFile(work/'unsigned.apk','w',zipfile.ZIP_DEFLATED) as out:
 names=base.namelist();indices=[1 if n=='classes.dex' else int(n[7:-4]) for n in names if n.startswith('classes') and n.endswith('.dex')]
 for info in base.infolist():
  if info.filename.startswith('META-INF/') or info.filename=='AndroidManifest.xml':continue
  out.writestr(info,final_dex[info.filename] if info.filename in final_dex else base.read(info.filename))
 out.writestr('AndroidManifest.xml',manifests.read('AndroidManifest.xml'))
 next_index=max(indices)+1
 for folder in [work/'dex',work/'host-dex']:
  for p in sorted(folder.glob('classes*.dex')):out.write(p,'classes'+str(next_index)+'.dex');next_index+=1
 for folder,prefix in [(root/'runtime-assets','assets/'),(root/'assets','assets/'),(root/'runtime-native','lib/')]:
  for p in folder.rglob('*'):
   if p.is_file():
    if p.name.endswith('.example.json'):continue
    n=prefix+str(p.relative_to(folder))
    assert n not in names,'Cannot overwrite stable asset '+n
    out.write(p,n)
 out.write(root/'RUNTIME-LICENSE.md','assets/Fusion-runtime-license.txt')
run(bt/'zipalign','-P','16','-f','4',work/'unsigned.apk',work/'aligned.apk')
dest=root.parent/'deliverables';dest.mkdir(exist_ok=True);apk=dest/'MediaPocket-0.1.48-p2p-boost-arm64.apk'
run(bt/'apksigner','sign','--ks',root/'signing.jks','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,work/'aligned.apk')
run(bt/'apksigner','verify',apk)
with zipfile.ZipFile(root/'base.apk') as base,zipfile.ZipFile(apk) as updated:
 assert updated.testzip() is None
 preserved=[]
 for n in base.namelist():
  if n.startswith('lib/') or (n.startswith('classes') and n.endswith('.dex')) or n.startswith('assets/') or n=='resources.arsc':
   if n in final_dex:continue
   assert base.read(n)==updated.read(n),n;preserved.append(n)
 (root/'VALIDATION.json').write_text(json.dumps({'base':'0.1.20','ui_version':'0.1.48','android_device_tested':False,'original_code_assets_native_preserved':preserved,'base_sha256':hashlib.sha256((root/'base.apk').read_bytes()).hexdigest()},indent=2))
print(subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(apk)]).decode().splitlines()[0]);print(apk)
