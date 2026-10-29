package com.guocheng.mediapocket;
import android.app.Activity;import android.content.Intent;import android.media.*;import java.util.*;import org.json.*;
/** Keep a user-requested VLC route local to this playback, without changing their default. */
final class PlaybackBackend {
 static final String VLC="mp.playback.vlc";
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
 /** Called by the two verified base-dex selection sites, before player creation. */
 public static boolean preferVlc(Object owner){if(!(owner instanceof Activity))return false;Activity a=(Activity)owner;JSONObject stream=streamOf(a);Object name=OnlineSubtitleSearch.field(a,"name");String hint=PlaybackBackendRules.audioHint(stream==null?"":StreamInfo.text(stream),stream==null?"":stream.optString("mp.magnetName"),stream==null?"":stream.optString("url"),name==null?"":String.valueOf(name));boolean risky=stream!=null&&DolbyRules.risky(stream.optString("dolby.kind"));return PlaybackBackendRules.preferVlcForSource(a.getIntent().getBooleanExtra(VLC,false),a.getSharedPreferences("data",0).getBoolean("useVlc",false),hint,risky,supported());}
 static JSONObject streamOf(Activity a){Object field=OnlineSubtitleSearch.field(a,"stream");if(field instanceof JSONObject)return (JSONObject)field;try{String raw=a.getIntent().getStringExtra("stream");if(raw!=null&&!raw.isEmpty())return new JSONObject(raw);}catch(Exception ignored){}return null;}
}
