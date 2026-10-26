package com.guocheng.mediapocket;
import android.app.Activity;import android.content.Intent;import android.media.*;import java.util.*;import org.json.*;
/** Keep a user-requested VLC route local to this playback, without changing their default. */
final class PlaybackBackend {
 static final String VLC="mp.playback.vlc";
 static final Map<Activity,Boolean> TRIED=new WeakHashMap<>();
 static volatile Set<String> audioTypes;
 private PlaybackBackend(){}
 static String mime(String codec){String c=codec==null?"":codec.toLowerCase(Locale.ROOT);switch(c){case "ac3":return "audio/ac3";case "eac3":return "audio/eac3";case "truehd":return "audio/true-hd";case "dts":return "audio/vnd.dts";case "dtshd":return "audio/vnd.dts.hd";case "aac":return "audio/mp4a-latm";case "flac":return "audio/flac";case "opus":return "audio/opus";case "mp3":return "audio/mpeg";default:return "";}}
 static Set<String> supported(){Set<String> cached=audioTypes;if(cached!=null)return cached;try{Set<String> found=new HashSet<>();for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos())if(!info.isEncoder())for(String type:info.getSupportedTypes())if(type.startsWith("audio/"))found.add(type.toLowerCase(Locale.ROOT));audioTypes=found;return found;}catch(RuntimeException e){return null;}}
 static boolean needsAudioFallback(Activity a,JSONObject stream){
  if(stream==null)return false;JSONArray tracks=stream.optJSONArray("emby.audio");JSONObject selected=null;String language=a.getSharedPreferences("data",0).getString("emby.audioLang","");
  for(int i=0;tracks!=null&&i<tracks.length();i++){JSONObject t=tracks.optJSONObject(i);if(t==null)continue;if(selected==null||t.optBoolean("isDefault"))selected=t;if(!language.isEmpty()&&language.equalsIgnoreCase(t.optString("language"))){selected=t;break;}}
  String type=mime(selected==null?"":selected.optString("codec"));Set<String> types=supported();
  return types!=null&&PlaybackBackendRules.softwareAudio(stream.optBoolean("emby.direct",true),DolbyRules.risky(stream.optString("dolby.kind")),a.getSharedPreferences("data",0).getBoolean("useVlc",false),!type.isEmpty(),types.contains(type));
 }
 static void decorate(Activity a,Intent in,boolean audioFallback,boolean sameItem){if((sameItem&&a.getIntent().getBooleanExtra(VLC,false))||audioFallback)in.putExtra(VLC,true);}
 static void apply(Activity a){
  Object video=OnlineSubtitleSearch.field(a,"video"),raw=OnlineSubtitleSearch.field(a,"url");boolean vlc=video!=null&&video.getClass().getName().equals("com.guocheng.mediapocket.VlcPlayback");
  if(!PlaybackBackendRules.switchOnce(a.getIntent().getBooleanExtra(VLC,false),TRIED.containsKey(a),vlc,raw instanceof String&&!((String)raw).isEmpty(),a.isFinishing()))return;
  TRIED.put(a,Boolean.TRUE);
  try{OnlineSubtitleSearch.call(a,"retryCurrentDecoder",new Class<?>[0]);Object switched=OnlineSubtitleSearch.field(a,"video");if(switched==null||!switched.getClass().getName().equals("com.guocheng.mediapocket.VlcPlayback"))throw new IllegalStateException();DebugLog.event("播放器","当前播放使用VLC兼容音频；默认播放器未修改");}catch(Exception e){PlayerOverlay.toast(a,"无法切到兼容播放，请重新打开片源");}
 }
}
