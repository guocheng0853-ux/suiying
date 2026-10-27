package com.guocheng.mediapocket;
import android.app.Activity;import android.content.SharedPreferences;import android.widget.TextView;import java.util.*;import org.json.JSONObject;
/** 0.1.155：在不改底座的前提下，补上 VLC 在这台机器上缺的两个信号（出画面、真正播完）。PlayerChrome 每 250ms 调一次。 */
final class PlaybackRepair {
 private PlaybackRepair(){}
 static final Map<Activity,String> HANDLED=new WeakHashMap<>();
 static final Map<Activity,Boolean> PREMATURE=new WeakHashMap<>();
 static void tick(Activity a){try{markRendered(a);}catch(Throwable ignored){}try{prematureEnd(a);}catch(Throwable ignored){}}
 static boolean premature(Activity a){prematureEnd(a);return Boolean.TRUE.equals(PREMATURE.get(a));}
 /** 视频轨已有尺寸、正在播、时间在走：替底座把 VLC 的「画面输出就绪」标上，下一次时间事件底座自己会走「出画面」流程。 */
 static void markRendered(Activity a)throws Exception{
  if(Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"rendered"))||Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"terminal")))return;
  Object video=OnlineSubtitleSearch.field(a,"video");if(video==null)return;Object ready=OnlineSubtitleSearch.field(video,"outputReady");if(!(ready instanceof Boolean)||(Boolean)ready)return;
  Object player=OnlineSubtitleSearch.field(video,"player");if(player==null)return;
  boolean playing=Boolean.TRUE.equals(OnlineSubtitleSearch.call(player,"isPlaying",new Class<?>[0]));Object time=OnlineSubtitleSearch.call(player,"getTime",new Class<?>[0]);long t=time instanceof Number?((Number)time).longValue():0;
  Object track=OnlineSubtitleSearch.call(player,"getCurrentVideoTrack",new Class<?>[0]);Object w=track==null?null:OnlineSubtitleSearch.field(track,"width");int width=w instanceof Integer?(Integer)w:0;
  if(!PlaybackEndRules.playingWithoutVout(false,false,playing,width,t))return;
  SourceSubtitleLoader.set(video,"outputReady",true);DebugLog.event("播放器","VLC 未发画面输出事件，按视频轨与时间推进确认已出画面");
 }
 /** 「本集播放完毕」但离结尾还远：恢复续播位置，改成失败状态，交给失败卡片和 Emby 自动恢复；不弹下一集。 */
 static void prematureEnd(Activity a){
  if(!Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"terminal")))return;Object state=OnlineSubtitleSearch.field(a,"state");if(!(state instanceof TextView))return;TextView label=(TextView)state;
  String text=String.valueOf(label.getText());if(!text.startsWith("本集播放完毕"))return;String key=String.valueOf(OnlineSubtitleSearch.field(a,"attemptId"))+"|"+String.valueOf(OnlineSubtitleSearch.field(a,"id"));if(key.equals(HANDLED.get(a)))return;HANDLED.put(a,key);
  Object lp=OnlineSubtitleSearch.field(a,"lastPos");long last=lp instanceof Number?((Number)lp).longValue():0;
  JSONObject stream=TrackSelection.stream(a);long runtime=PlaybackEndRules.ticksToMs(stream.optLong("emby.runtimeTicks"));
  if(runtime<=0){Object meta=OnlineSubtitleSearch.field(a,"meta");if(meta instanceof JSONObject)runtime=StreamInfo.minutes(((JSONObject)meta).optString("runtime"))*60000L;}
  long duration=0;try{Object video=OnlineSubtitleSearch.field(a,"video");Object d=video==null?null:OnlineSubtitleSearch.call(video,"getDuration",new Class<?>[0]);duration=d instanceof Integer?(Integer)d:0;}catch(Exception ignored){}
  if(!PlaybackEndRules.premature(last,runtime,duration)){PREMATURE.remove(a);return;}
  PREMATURE.put(a,true);String id=String.valueOf(OnlineSubtitleSearch.field(a,"id"));
  try{SourceSubtitleLoader.set(a,"resumePos",(int)Math.min(Integer.MAX_VALUE,last));}catch(Exception ignored){}
  if(last>10000){SharedPreferences p=a.getSharedPreferences("data",0);p.edit().putInt("pos:"+id,(int)Math.min(Integer.MAX_VALUE,last)).apply();}
  String why=PlaybackEndRules.prematureText(last,runtime>0?runtime:duration);label.setText(why);DebugLog.event("播放失败",why+"（VLC 报读到结尾，保留续播位置）");
 }
}
