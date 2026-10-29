package com.guocheng.mediapocket;
import android.app.Activity;
import android.media.*;
import android.view.View;
import android.widget.Toast;
import java.util.*;
import org.json.*;
/**
 * SystemPlayback（android.media.MediaPlayer）解不了 EAC3 / AC3 / TrueHD / DTS 时有画面无声音。
 * 从当前位置重开 VLC，不改用户的默认播放器。P2P、影视仓、Emby 都走 PlayerActivity 这条路径。
 * 杜比视界 P5 / 未知纯 DV 留在系统播放，避免 VLC 把画面解成发紫发绿。
 */
final class SystemAudioFallback {
 static final String TOAST="音轨格式系统不支持，已切换 VLC";
 static final int PLAYER_TYPE_MEDIAPLAYER=2,PLAYER_STATE_STARTED=2;
 static final Map<Activity,Boolean> DONE=new WeakHashMap<>();
 static final Map<Activity,Long> SHOWN=new WeakHashMap<>();
 static final Map<Activity,Integer> INFO=new WeakHashMap<>();
 static final Map<Object,Boolean> WATCHED=Collections.synchronizedMap(new WeakHashMap<>());
 private SystemAudioFallback(){}
 static void tick(Activity a){try{
  if(a==null||a.isFinishing()||DONE.containsKey(a))return;
  Object video=OnlineSubtitleSearch.field(a,"video");if(video==null)return;
  String type=video.getClass().getName();
  if(type.endsWith("VlcPlayback")||!type.endsWith("SystemPlayback"))return;
  Object player=OnlineSubtitleSearch.field(video,"player");watch(a,player);
  boolean rendered=Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"rendered"));
  if(rendered&&!SHOWN.containsKey(a))SHOWN.put(a,android.os.SystemClock.elapsedRealtime());
  int info=INFO.containsKey(a)?INFO.get(a):0;
  if(!rendered&&info!=PlaybackBackendRules.AUDIO_NOT_PLAYING)return;
  boolean playing=false;try{playing=Boolean.TRUE.equals(OnlineSubtitleSearch.call(video,"isPlaying",new Class<?>[0]));}catch(Exception ignored){}
  long since=SHOWN.containsKey(a)?android.os.SystemClock.elapsedRealtime()-SHOWN.get(a):0;
  JSONObject stream=TrackSelection.stream(a);Object name=OnlineSubtitleSearch.field(a,"name");
  String hint=PlaybackBackendRules.audioHint(stream==null?"":StreamInfo.text(stream),stream==null?"":stream.optString("mp.magnetName"),stream==null?"":stream.optString("url"),name==null?"":name.toString());
  boolean risky=(stream!=null&&DolbyRules.risky(stream.optString("dolby.kind")))||PlaybackBackendRules.riskyHint(hint);
  Probe probe=probe(player);boolean already=a.getIntent().getBooleanExtra(PlaybackBackend.VLC,false);
  if(!PlaybackBackendRules.runtimeAudioFallback(true,risky,already,playing,rendered,since,info,probe.known,probe.count,probe.selectionKnown,probe.selectedPlayable,probe.selectedUnsupported,probe.anyPlayable,probe.anyUnsupported,idleMediaPlayer(a),volume(a)))return;
  Object url=OnlineSubtitleSearch.field(a,"url");if(!(url instanceof String)||((String)url).isEmpty())return;
  DONE.put(a,Boolean.TRUE);reopen(a);
 }catch(RuntimeException ignored){}}
 /** 包住原来的 OnInfo，保留 3 / 701 / 702。部分机型解不了音轨时会给 804。 */
 /** 只在读到原来的 OnInfoListener 时才包一层。读不到字段就保持底座的 3 / 701 / 702，改靠音轨 MIME 和音频会话判断。 */
 static void watch(Activity a,Object player){if(!(player instanceof MediaPlayer)||WATCHED.containsKey(player))return;MediaPlayer mp=(MediaPlayer)player;MediaPlayer.OnInfoListener previous=null;try{java.lang.reflect.Field f=MediaPlayer.class.getDeclaredField("mOnInfoListener");f.setAccessible(true);Object cur=f.get(mp);if(cur instanceof MediaPlayer.OnInfoListener)previous=(MediaPlayer.OnInfoListener)cur;}catch(Throwable ignored){}if(previous==null)return;WATCHED.put(player,Boolean.TRUE);MediaPlayer.OnInfoListener prior=previous;mp.setOnInfoListener((p,what,extra)->{if(what==PlaybackBackendRules.AUDIO_NOT_PLAYING)INFO.put(a,what);boolean handled=false;try{handled=prior.onInfo(p,what,extra);}catch(Throwable ignored){}return handled;});}
 static int volume(Activity a){try{AudioManager am=(AudioManager)a.getSystemService(Activity.AUDIO_SERVICE);return am==null?1:am.getStreamVolume(AudioManager.STREAM_MUSIC);}catch(RuntimeException e){return 1;}}
 static final class Probe {boolean known,selectionKnown,selectedPlayable,selectedUnsupported,anyPlayable,anyUnsupported;int count;}
 static Probe probe(Object player){Probe p=new Probe();if(!(player instanceof MediaPlayer))return p;MediaPlayer mp=(MediaPlayer)player;Set<String> types=PlaybackBackend.supported();try{
  MediaPlayer.TrackInfo[] tracks=mp.getTrackInfo();p.known=true;int selected=-1;try{selected=mp.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);p.selectionKnown=true;}catch(RuntimeException ignored){}
  for(int i=0;tracks!=null&&i<tracks.length;i++){if(tracks[i].getTrackType()!=MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)continue;p.count++;String mime="";try{MediaFormat fmt=tracks[i].getFormat();if(fmt!=null){String raw=fmt.getString(MediaFormat.KEY_MIME);if(raw!=null)mime=raw;}}catch(RuntimeException ignored){}boolean playable=playable(mime,types),unsupported=PlaybackBackendRules.cinemaMime(mime)&&!playable;if(playable)p.anyPlayable=true;if(unsupported)p.anyUnsupported=true;if(p.selectionKnown&&i==selected){p.selectedPlayable=playable;p.selectedUnsupported=unsupported;}}
 }catch(RuntimeException ignored){}return p;}
 /** 空 mime 不算能播。普通 AAC / MP3 / Opus 不算影院音轨，留给系统播放器。 */
 static boolean playable(String mime,Set<String> types){if(mime==null||mime.isEmpty())return false;if(!PlaybackBackendRules.cinemaMime(mime))return true;return types!=null&&types.contains(mime.toLowerCase(Locale.ROOT));}
 /** 看到本应用的 MediaPlayer 音频会话，且没有一个处于 STARTED。方法用反射，避免 API 26 的类校验在 API 31 方法上失败。看不到会话不等于无声。 */
 static boolean idleMediaPlayer(Activity a){try{AudioManager am=(AudioManager)a.getSystemService(Activity.AUDIO_SERVICE);if(am==null)return false;boolean saw=false,started=false;for(AudioPlaybackConfiguration c:am.getActivePlaybackConfigurations()){Object type=c.getClass().getMethod("getPlayerType").invoke(c);if(!(type instanceof Integer)||((Integer)type)!=PLAYER_TYPE_MEDIAPLAYER)continue;saw=true;Object state=c.getClass().getMethod("getPlayerState").invoke(c);if(state instanceof Integer&&((Integer)state)==PLAYER_STATE_STARTED)started=true;}return saw&&!started;}catch(Throwable e){return false;}}
 static void reopen(Activity a){int pos=0;try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"getCurrentPosition",new Class<?>[0]);if(r instanceof Integer)pos=(Integer)r;}catch(Exception ignored){}Object id=OnlineSubtitleSearch.field(a,"id");if(pos>0&&id!=null)a.getSharedPreferences("data",0).edit().putInt("pos:"+id,pos).apply();try{a.getIntent().putExtra(PlaybackBackend.VLC,true);}catch(RuntimeException ignored){}Toast.makeText(a,TOAST,Toast.LENGTH_SHORT).show();DebugLog.event("音频",TOAST);try{OnlineSubtitleSearch.call(a,"retryCurrentDecoder",new Class<?>[0]);}catch(Exception e){DebugLog.event("音频","切换 VLC 失败");return;}if(pos<=1000)return;int keep=pos;View decor=a.getWindow()==null?null:a.getWindow().getDecorView();if(decor==null)return;decor.postDelayed(()->EmbyPlayback.seek(a,keep),700);decor.postDelayed(()->EmbyPlayback.seek(a,keep),1600);}
}
