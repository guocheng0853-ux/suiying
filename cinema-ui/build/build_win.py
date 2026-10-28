"""Windows 版 build_ui.py：工具用本机 Android SDK 与 jadx-1.5.6，逻辑与 build_ui.py 逐段对应。
2026-10-28 起随仓库维护（补丁链以 build_ui.py 为准，两条脚本必须一起改）。改完必须跑 verify_apk.py：它断言全部 7 个补丁标记（含 PatchPlaybackBackend），缺补丁的包过不了终检。
用法：uv.exe run --no-project python cinema-ui/build/build_win.py   （无密钥时加 --unsigned）
环境变量：ANDROID_SDK_BT 覆盖 build-tools 目录（默认 %LOCALAPPDATA%\\Android\\Sdk\\build-tools\\35.0.0）；
         REA_JADX_JAR 指向 jadx-1.5.6-all.jar（必填，仓库不带 jadx）；
         android.jar 取 <仓库>/build-tools/android-35/ 或 %LOCALAPPDATA%\\Android\\Sdk\\platforms\\android-35/。
"""
from pathlib import Path
import subprocess,zipfile,hashlib,shutil,json,struct,os,sys

root=Path(__file__).resolve().parent.parent    # cinema-ui（脚本在 build/ 下）
work=root/'build';work.mkdir(exist_ok=True)
SDK_BT=Path(os.environ.get('ANDROID_SDK_BT',str(Path(os.environ.get('LOCALAPPDATA',''))/'Android'/'Sdk'/'build-tools'/'35.0.0')))
JADX_JAR=Path(os.environ.get('REA_JADX_JAR',''))
if not JADX_JAR.is_file():sys.exit('REA_JADX_JAR 未设置或无效：需指向 jadx-1.5.6-all.jar（dexlib2 补丁依赖），例如 set REA_JADX_JAR=D:\\tools\\jadx-1.5.6\\lib\\jadx-1.5.6-all.jar')
AAPT2=SDK_BT/'aapt2.exe';ZIPALIGN=SDK_BT/'zipalign.exe';D8=SDK_BT/'lib'/'d8.jar';APKSIGNER=SDK_BT/'lib'/'apksigner.jar'
AJ=next((p for p in [root.parent/'build-tools'/'android-35'/'android.jar',Path(os.environ.get('LOCALAPPDATA',''))/'Android'/'Sdk'/'platforms'/'android-35'/'android.jar'] if p.is_file()),None)
if AJ is None:sys.exit('找不到 android.jar：放在 <仓库>/build-tools/android-35/ 或安装 Android SDK platforms;android-35')
def run(*args):
    print('>>',' '.join(str(a) for a in args)[:150],flush=True)
    subprocess.run([str(x) for x in args],check=True)
def d8(*args):
    with open(work/'d8-build.log','a') as log:
        result=subprocess.run(['java','-XX:-UsePerfData','-Xmx3g','-cp',str(D8),'com.android.tools.r8.D8',*[str(a) for a in args]],stdout=log,stderr=log)
    if result.returncode:raise RuntimeError('D8 failed; see build/d8-build.log')
def javac(cp,out,*files):
    subprocess.run(['javac','-nowarn','-encoding','UTF-8','-cp',str(cp),'-d',str(out),*[str(f) for f in files]],check=True)
def dex_classes(d):
    so=struct.unpack_from('<I',d,60)[0];to=struct.unpack_from('<I',d,68)[0];ct,co=struct.unpack_from('<II',d,96);out=set()
    for i in range(ct):
        t=struct.unpack_from('<I',d,co+i*32)[0];si=struct.unpack_from('<I',d,to+t*4)[0];at=struct.unpack_from('<I',d,so+si*4)[0]
        while d[at]&128:at+=1
        at+=1;c=d[at:d.index(b'\0',at)].decode();out.add(c[1:-1]+'.class')
    return out
def libs_cp():
    libs=sorted((root/'runtime-libs').glob('*.jar'))
    return ';'.join(str(p) for p in libs)

# 0. 清理旧产物（对齐 build_ui.py：每次全量重建）
for folder in ['classes','stubs','runtime-classes','dex','host-dex']:
    if (work/folder).exists():shutil.rmtree(work/folder)

# 1. fetch_media3：默认只校验本地 SHA-256，不联网
run(sys.executable,root/'fetch_media3.py')

# 2. runtime：api-only jar + javac（对应 build_fusion_runtime.py 的 ecj -17 段）
api=work/'android-api-only.jar'
with zipfile.ZipFile(AJ) as source,zipfile.ZipFile(api,'w') as target:
    for n in source.namelist():
        if not n.startswith(('java/','javax/','org/w3c/','org/xml/sax/','sun/','jdk/')):target.writestr(n,source.read(n))
javac(str(api)+';'+libs_cp(),work/'runtime-classes',*list((root/'runtime-src').rglob('*.java')))
javac(AJ,work/'stubs',*list((root/'stubs').rglob('*.java')))
javac(';'.join([str(work/'stubs'),str(work/'runtime-classes'),str(AJ)])+';'+libs_cp(),work/'classes',*list((root/'src').rglob('*.java')))

# 3. host-dex：runtime + libs，剔除 base 已有类（含 media3 通知类剔除，对齐 build_fusion_runtime.py）
base=set()
with zipfile.ZipFile(root/'base.apk') as z:
    for n in z.namelist():
        if n.startswith('classes') and n.endswith('.dex'):base.update(dex_classes(z.read(n)))
with zipfile.ZipFile(work/'runtime.jar','w') as z:
    for p in (work/'runtime-classes').rglob('*.class'):z.write(p,str(p.relative_to(work/'runtime-classes')))
libs=sorted((root/'runtime-libs').glob('*.jar'))
seen={};program=[]
for lib in [work/'runtime.jar']+libs:
    filtered=work/('program-'+lib.name)
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
(work/'host-dex').mkdir(exist_ok=True)
d8('--min-api','26','--lib',AJ,'--output',work/'host-dex',*program)

# 4. ui dex
with zipfile.ZipFile(work/'ui.jar','w') as z:
    for p in (work/'classes').rglob('*.class'):z.write(p,str(p.relative_to(work/'classes')))
with zipfile.ZipFile(work/'stubs.jar','w') as z:
    for p in (work/'stubs').rglob('*.class'):z.write(p,str(p.relative_to(work/'stubs')))
(work/'dex').mkdir(exist_ok=True)
d8('--min-api','26','--lib',AJ,'--classpath',work/'stubs.jar','--classpath',work/'runtime.jar','--output',work/'dex',work/'ui.jar')

# 5. dex 补丁（dexlib2 来自 jadx-all jar；Windows classpath 用分号）
patch_tools=work/'patch-tools'
shutil.rmtree(patch_tools,ignore_errors=True)
javac(JADX_JAR,patch_tools,*[root/'tools'/n for n in ['PatchClawNet.java','VerifyClawPatch.java','PatchSubtitleSearch.java','PatchHeaderRoute.java','PatchTorrentBoost.java','PatchPieceReads.java','PatchPlaybackOrientation.java','PatchPlaybackBackend.java']])
patchcp=str(patch_tools)+';'+str(JADX_JAR)
def patcher(cls,src,dst):
    (work/dst).unlink(missing_ok=True)
    run('java','-XX:-UsePerfData','-cp',patchcp,cls,work/src,work/dst)
    out=(work/dst).read_bytes()
    assert out[:4]==b'dex\n' and struct.unpack_from('<I',out,96)[0]>0,(cls,'empty dex')
    return out
with zipfile.ZipFile(root/'base.apk') as base:
    (work/'original-main.dex').write_bytes(base.read('classes.dex'))
patched_main=patcher('PatchClawNet','original-main.dex','patched-main.dex')
run('java','-XX:-UsePerfData','-cp',patchcp,'VerifyClawPatch',work/'original-main.dex',work/'patched-main.dex')
subtitle_name=None
with zipfile.ZipFile(root/'base.apk') as base:
    for name in base.namelist():
        if not (name.startswith('classes') and name.endswith('.dex')):continue
        if 'com/guocheng/mediapocket/PlayerSession.class' in dex_classes(base.read(name)):
            subtitle_name=name;(work/'original-subtitle.dex').write_bytes(base.read(name))
assert subtitle_name,'PlayerSession dex not found'
if subtitle_name=='classes.dex':(work/'original-subtitle.dex').write_bytes(patched_main)
patched_subtitle=patcher('PatchSubtitleSearch','original-subtitle.dex','patched-subtitle.dex')
# 0.1.46：VlcPlayback.setSource 前插 HeaderRoute（VLC 3 不支持 :http-header，额外请求头经本机代理补齐）
final_dex={'classes.dex':patched_main};final_dex[subtitle_name]=patched_subtitle
vlc_name=None
with zipfile.ZipFile(root/'base.apk') as base:
    for name in base.namelist():
        if name.startswith('classes') and name.endswith('.dex') and b'Lcom/guocheng/mediapocket/VlcPlayback;' in base.read(name):vlc_name=name
assert vlc_name,'VlcPlayback dex not found'
(work/'original-vlc.dex').write_bytes(final_dex.get(vlc_name) or zipfile.ZipFile(root/'base.apk').read(vlc_name))
final_dex[vlc_name]=patcher('PatchHeaderRoute','original-vlc.dex','patched-vlc.dex')
# 0.1.48：TorrentEngine 会话参数与磁力 tracker（TorrentBoost）
torrent_name=None
with zipfile.ZipFile(root/'base.apk') as base:
    for name in base.namelist():
        if name.startswith('classes') and name.endswith('.dex') and b'Lcom/guocheng/mediapocket/TorrentEngine;' in base.read(name) and b'acquireSession' in base.read(name):torrent_name=name
assert torrent_name,'TorrentEngine dex not found'
(work/'original-torrent.dex').write_bytes(final_dex.get(torrent_name) or zipfile.ZipFile(root/'base.apk').read(torrent_name))
patcher('PatchTorrentBoost','original-torrent.dex','patched-torrent.dex')
final_dex[torrent_name]=patcher('PatchPieceReads','patched-torrent.dex','patched-piece-reads.dex')
# 0.1.63：播放页 onCreate 立刻传感器横屏，onPause/onDestroy 在离开时恢复进入前的方向
final_dex[torrent_name]=patcher('PatchPlaybackOrientation','patched-piece-reads.dex','patched-orientation.dex')
# 0.1.148：PlayerActivity 两处 useVlc 读取改走 PlaybackBackend.preferVlc（音频兼容/VLC 入口）
final_dex[torrent_name]=patcher('PatchPlaybackBackend','patched-orientation.dex','patched-backend.dex')

# 6. 真实默认配置含个人令牌，不入库；缺失时用脱敏样例（打包名仍为 fusion-default.json）
if not (root/'assets/fusion-default.json').exists():
    shutil.copy(root/'assets/fusion-default.example.json',root/'assets/fusion-default.json');print('WARNING: assets/fusion-default.json missing, using sanitized example')
manifest=(root/'AndroidManifest.xml').read_text(encoding='utf-8')
if os.environ.get('MEDIAPOCKET_REVIEW')=='1':manifest=manifest.replace('com.guocheng.mediapocket.next','com.guocheng.mediapocket.review').replace('android:label="随影新版"','android:label="随影测试版"')
(work/'AndroidManifest.xml').write_text(manifest,encoding='utf-8')
for folder in ['res/drawable','res/values']:(work/folder).mkdir(parents=True,exist_ok=True)
(work/'res/drawable/icon.xml').write_text('<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#315f76"/></shape>')
(work/'res/values/styles.xml').write_text('<resources><style name="AppTheme" parent="android:style/Theme.Material.NoActionBar"/></resources>')
subprocess.run([str(AAPT2),'compile','--dir','res','-o','resources.zip'],cwd=str(work),check=True)
subprocess.run([str(AAPT2),'link','-o','manifest.apk','-I',str(AJ),'--manifest',str(work/'AndroidManifest.xml'),'resources.zip'],cwd=str(work),check=True)
resources=subprocess.check_output([str(AAPT2),'dump','resources',str(work/'manifest.apk')]).decode()
assert '0x7f010000 drawable/icon' in resources and '0x7f020000 style/AppTheme' in resources

# 7. 组包（打包前对每个中间 DEX 做完整性断言）
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
                n=prefix+str(p.relative_to(folder)).replace('\\','/')
                assert n not in names,'Cannot overwrite stable asset '+n
                out.write(p,n)
    out.write(root/'RUNTIME-LICENSE.md','assets/Fusion-runtime-license.txt')
run(ZIPALIGN,'-P','16','-f','4',work/'unsigned.apk',work/'aligned.apk')

# 8. 签名 + 校验 + 稳定资产核对
dest=root.parent/'deliverables';dest.mkdir(exist_ok=True);apk=dest/'MediaPocket-0.1.158-tablet-fullscreen-arm64.apk'
ks=Path(os.environ.get('MEDIAPOCKET_KEYSTORE',str(root/'signing.jks')))
unsigned='--unsigned' in sys.argv
if ks.is_file() and not unsigned:
    run('java','-jar',APKSIGNER,'sign','--ks',ks,'--ks-pass','pass:android','--key-pass','pass:android','--out',apk,work/'aligned.apk')
    run('java','-jar',APKSIGNER,'verify',apk)
    print('signed apk')
elif unsigned:
    shutil.copy(work/'aligned.apk',apk);print('unsigned apk (--unsigned)')
else:
    raise SystemExit('refusing to write an unsigned APK as '+apk.name+'; pass --unsigned or set MEDIAPOCKET_KEYSTORE')
with zipfile.ZipFile(root/'base.apk') as base,zipfile.ZipFile(apk) as updated:
    assert updated.testzip() is None
    preserved=[]
    for n in base.namelist():
        if n.startswith('lib/') or (n.startswith('classes') and n.endswith('.dex')) or n.startswith('assets/') or n=='resources.arsc':
            if n in final_dex:continue
            assert base.read(n)==updated.read(n),n;preserved.append(n)
    (root/'VALIDATION.json').write_text(json.dumps({'base':'0.1.20','ui_version':'0.1.158','android_device_tested':False,'original_code_assets_native_preserved':preserved,'base_sha256':hashlib.sha256((root/'base.apk').read_bytes()).hexdigest()},indent=2),encoding='utf-8')
print(subprocess.check_output([str(AAPT2),'dump','badging',str(apk)]).decode().splitlines()[0]);print('APK:',apk)
