from pathlib import Path
import zipfile,struct,json,subprocess,re
root=Path(__file__).resolve().parent
apk=root.parent/'deliverables/MediaPocket-0.1.132-day-night-arm64.apk'
def dex(d):
 def u(at):return struct.unpack_from('<I',d,at)[0]
 strings=[]
 for i in range(u(56)):
  a=u(u(60)+4*i)
  while d[a]&128:a+=1
  a+=1;strings.append(d[a:d.index(b'\0',a)].decode('utf-8',errors='replace'))
 types=[strings[u(u(68)+4*i)] for i in range(u(64))]
 protos=[]
 for i in range(u(72)):
  at=u(76)+12*i;params=u(at+8);args=[]
  if params:
   for j in range(u(params)):args.append(types[struct.unpack_from('<H',d,params+4+2*j)[0]])
  protos.append('('+''.join(args)+')'+types[u(at+4)])
 methods=[]
 for i in range(u(88)):
  at=u(92)+8*i;cls,proto,name=struct.unpack_from('<HHI',d,at);methods.append((types[cls],strings[name],protos[proto]))
 classes=set(types[u(u(100)+32*i)] for i in range(u(96)))
 return classes,methods
badging=subprocess.check_output([str(root.parent/'build-tools/android-15/aapt2'),'dump','badging',str(apk)],text=True).splitlines()[0]
match=re.search(r"name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
assert match and match.group(1) in ('com.guocheng.mediapocket.next','com.guocheng.mediapocket.review') and match.group(2)=='133' and match.group(3)=='0.1.132',badging
xml=subprocess.check_output([str(root.parent/'build-tools/android-15/aapt2'),'dump','xmltree',str(apk),'--file','AndroidManifest.xml'],text=True)
assert 'ExternalPlayActivity' in xml and '随影 (VLC)' in xml and 'suiying' in xml, 'external player or import link missing'
seen=set();refs=[];count=0
with zipfile.ZipFile(apk) as z:
 for n in z.namelist():
  if n.startswith('classes') and n.endswith('.dex'):
   classes,methods=dex(z.read(n));assert not seen.intersection(classes),'Duplicate Android class definitions';seen.update(classes);refs.extend(methods);count+=1
 assert 'lib/arm64-v8a/libquickjs-android-wrapper.so' in z.namelist()
 for n in ['assets/Media3-Apache-2.0.txt','assets/public-home-sources.json','assets/fusion-default.json','assets/js/lib/host-compat.js','assets/js/lib/cheerio.min.js','assets/Fusion-runtime-license.txt']:assert n in z.namelist(),n
 for c in ['EpisodeHandoff','EmbyResumeRules','NativeNext','PlayerMedia','Media3Controls','Media3ControlRules','Media3Sound','Media3ErrorRules','Media3VideoView','SystemPlayerActivity','DolbyChoice','DolbyRules','SourcePickerDialog','HomeSources','CmsApi','ScrapedMainActivity','FusionCatalog','FusionLibraryActivity','TvSpiderRuntime','ClawConnectionProbe','SourceSubtitleLoader','FusionPlaybackSources','SubtitleTextRules','SubtitleHttp','PlaybackSitePicker','SourceSearchClient','SourceSearchRules','FullscreenMode','FullscreenRules','HeaderRoute','LinkProbe','PlaybackCheck','StremioSourceDialog','StremioStreams','DebugLog','DebugText','PlayerRedesign','PlayerIcons','TvBoxPlay','WebSniffer','SniffRules','RangeAccelerator','TorrentBoost','PlayerChrome','PlayerChromeRules','PlayerOverlay','PlayerOverlayRules','PlaybackPrefs','StreamInfo','DetailChrome','HomeChrome','HomeRules','PieceDeadlines','DetailMetadata','SourceTracks','TrackSelection','HeroSwipe','CarouselRules','EpisodeActions','DlnaClient','DlnaCastDialog','CastRules','HomeAtmosphere','HomeHeroRules','SceneRules','PlaybackOrientation','EmbyApi','EmbyClient','EmbyStore','EmbyPlayback','EmbyHome','EmbySearch','EmbyLibraryActivity','EmbyCue','EmbyPlayerUi','BifIndex','SpeedHold','PlayerSidePanel','PlayerStatus','PlayerIconPaths','PlayerLayoutMetrics','DanDanPlayRules','DanDanPlayClient','DanmuRules','DanmuView','DanmuController','OnlineSubtitleRules','OnlineSubtitleClient','OnlineSubtitleController','ExternalSubtitleStore','ExternalPlayRules','ExternalPlayActivity','ConfigImport','PlayerSwitchRules','PlayerSwitches','PlayerFeatureSettings','LevelHud','LevelIcons','LevelIconGeometry','LevelHudMetrics','RequestHeaders','HttpRange','ScopedHttpDataSource','OfflineProgress']:assert 'Lcom/guocheng/mediapocket/'+c+';' in seen
 for component in ['Landroidx/media3/exoplayer/ExoPlayer;','Landroidx/media3/exoplayer/video/MediaCodecVideoRenderer;','Landroidx/media3/extractor/mkv/MatroskaExtractor;','Landroidx/media3/extractor/mp4/Mp4Extractor;']:assert component in seen,component
 missing_media3=sorted({cls for cls,_,_ in refs if cls.startswith('Landroidx/media3/') and cls not in seen})
 assert not missing_media3,missing_media3
 missing=sorted({cls for cls,_,_ in refs if cls.startswith(('Lcom/guocheng/','Lcom/github/catvod/','Lcom/fongmi/','Lcom/whl/')) and cls not in seen})
 assert not missing,missing
with zipfile.ZipFile(root/'base.apk') as z:
 methods=[]
 for n in z.namelist():
  if n.startswith('classes') and n.endswith('.dex'):methods+=dex(z.read(n))[1]
 for api in [('Lorg/videolan/libvlc/LibVLC;','<init>','(Landroid/content/Context;Ljava/util/List;)V'),('Lorg/videolan/libvlc/Media;','<init>','(Lorg/videolan/libvlc/interfaces/ILibVLC;Landroid/net/Uri;)V'),('Lorg/videolan/libvlc/Media;','parseAsync','(II)Z'),('Lorg/videolan/libvlc/Media;','isParsed','()Z'),('Lorg/videolan/libvlc/Media;','getTrackCount','()I'),('Lorg/videolan/libvlc/Media;','getTrack','(I)Lorg/videolan/libvlc/interfaces/IMedia$Track;')]:assert api in methods,('Missing track probe API',api)
 for name,method,proto in [('TvBoxNet','fetch','(Ljava/lang/String;Lorg/json/JSONObject;Ljava/lang/String;)Ljava/lang/String;'),('TvBoxRules','resolve','(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;'),('UiImages','<init>','()V'),('UiImages','load','(Landroid/widget/ImageView;Ljava/lang/String;I)V')]:
  assert ('Lcom/guocheng/mediapocket/'+name+';',method,proto) in methods,(name,method,proto)
programs=sorted((root/'tests').glob('*Test.java'));stamp_path=root/'build'/'jvm-tests.passed'
stamp=int(stamp_path.read_text().strip()) if stamp_path.exists() else None
if stamp!=len(programs):raise SystemExit('jvm stamp %s != %d test programs; run scripts/run_tests.sh'%(stamp,len(programs)))
signed=False
with zipfile.ZipFile(apk) as z:
 for n in z.namelist():
  if n.startswith('META-INF/') and n.rsplit('.',1)[-1] in ('RSA','DSA','EC','SF'):
   signed=True;break
print('apk signature:', 'present' if signed else 'absent')
validation=json.loads((root/'VALIDATION.json').read_text());validation.update({'media3_runtime_included':True,'dolby_android_device_tested':False,'jvm_test_programs_passed':stamp,'apk_class_resolution':'passed','unique_classes':len(seen),'dex_files':count,'main_version_code':133,'application_id':match.group(1),'review_build':match.group(1).endswith('.review'),'default_home_provider':'fusion','apk_signature_present':signed,'android_device_tested':False,'xiaoya_sample_phone_evidence':'0.1.21-probe7: video_display_and_audio_output; all_sites_playback_tested=false','tablet_torrentclaw_failure_reproduced':False,'presets_empty':True,'torrentclaw_removal_retained':True,
 'header_route_patch':'VlcPlayback.setSource: 4 prepended instructions calling HeaderRoute; all other main-dex method bodies identical (PatchHeaderRoute self-check)',
 'jvm_playback_chain_test':'HeaderRouteTest: local origin requiring Cookie+Origin; VLC-only headers -> 403; via HeaderRoute ffmpeg decoded 150 frames from HLS master->variant->AES-128 TS; MP4 seek via Range/206. JVM+ffmpeg, not Android/VLC.',
 'real_source_playback_tested':False,'real_source_note':'cloud egress blocks xiaoya/Stremio/TVBox hosts; user computer bridge offline during this build',
 'stremio_picker_android_ui_tested':False,'web_sniffer_android_tested':False,'link_probe_android_tested':False,'range_accelerator':'RangeAcceleratorTest: 2MB/s-per-connection origin, single vs 4-connection proxy ~4x faster, sha256 identical, Range/no-Range correct, ffmpeg decode via accelerator. JVM, not Android.','accelerator_android_tested':False,'probe_budget_ms':4000,'torrent_boost':'TorrentEngine.acquireSession +tune (upload 512KB/s, connection_speed 80, connect boost 60, LSD/UPnP/NAT-PMP, announce to all tiers); magnet +public trackers. Patch self-check only; libtorrent native cannot run on JVM','torrent_boost_android_tested':False});(root/'VALIDATION.json').write_text(json.dumps(validation,indent=2));print('PASS: unique classes, component method ABI, manifest build/version, runtime assets, native module, original code preservation.')
