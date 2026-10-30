package com.guocheng.mediapocket;
import android.app.Activity;import android.os.SystemClock;import java.util.*;import java.util.concurrent.*;import org.json.*;
/** Observe native torrent counters off the UI thread; let the original player perform any source change. */
final class TorrentStartup {
 static final Map<Activity,Attempt> LIVE=new WeakHashMap<>();
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"torrent-startup");t.setDaemon(true);return t;});
 static final class Attempt {final JSONObject stream;final Object token;long sampleAt,slowSince=-1;boolean pending,handled;Attempt(JSONObject s,Object t){stream=s;token=t;}}
 static final class Stats {final long rate,required,verified;final boolean metadata;Stats(long r,long need,long done,boolean ready){rate=r;required=need;verified=done;metadata=ready;}}
 private TorrentStartup(){}
 static boolean flag(Activity a,String key){return Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,key));}
 static boolean eligible(Activity a,JSONObject s){try{
  if(a==null||a.isFinishing()||a.isDestroyed()||!a.hasWindowFocus()||s==null||!s.has("infoHash")||flag(a,"rendered")||flag(a,"terminal")||!flag(a,"active")||!flag(a,"autoSwitch"))return false;
  Object video=OnlineSubtitleSearch.field(a,"video");if(video==null||Boolean.TRUE.equals(OnlineSubtitleSearch.call(video,"userPaused",new Class<?>[0])))return false;
  if(Boolean.TRUE.equals(OnlineSubtitleSearch.call(video,"isPlaying",new Class<?>[0]))){Object pos=OnlineSubtitleSearch.call(video,"getCurrentPosition",new Class<?>[0]);if(pos instanceof Number&&((Number)pos).longValue()>0)return false;}
  Object q=OnlineSubtitleSearch.field(a,"queue");return q!=null&&Boolean.TRUE.equals(OnlineSubtitleSearch.call(q,"hasNext",new Class<?>[0]));
 }catch(Exception ignored){return false;}}
 static void tick(Activity a){try{observe(a);}catch(RuntimeException ignored){LIVE.remove(a);}}
 static void observe(Activity a){
  JSONObject stream=PlaybackBackend.streamOf(a);if(!eligible(a,stream)){LIVE.remove(a);return;}
  Object token=OnlineSubtitleSearch.field(a,"attemptId");Attempt prior=LIVE.get(a);if(prior==null||prior.stream!=stream||!Objects.equals(prior.token,token)){prior=new Attempt(stream,token);LIVE.put(a,prior);}
  final Attempt attempt=prior;long now=SystemClock.elapsedRealtime();if(attempt.pending||attempt.handled||now-attempt.sampleAt<1000)return;
  final TorrentEngine engine=TorrentEngine.current;if(engine==null)return;
  Object meta=OnlineSubtitleSearch.field(a,"meta");final int minutes=meta instanceof JSONObject?StreamInfo.minutes(((JSONObject)meta).optString("runtime")):0;
  attempt.sampleAt=now;attempt.pending=true;
  IO.execute(()->{Stats stats=read(engine,minutes);a.runOnUiThread(()->{attempt.pending=false;try{if(TorrentEngine.current==engine)accept(a,attempt,stats,SystemClock.elapsedRealtime());}catch(RuntimeException ignored){LIVE.remove(a);}});});
 }
 static Stats read(TorrentEngine engine,int minutes){try{
  Object handle=engine.handle;if(handle==null||!engine.handle.isValid())return null;Object status=OnlineSubtitleSearch.call(handle,"status",new Class<?>[0]);
  boolean metadata=Boolean.TRUE.equals(OnlineSubtitleSearch.call(status,"hasMetadata",new Class<?>[0]));
  return new Stats(number(status,"downloadRate"),TorrentStartupRules.requiredRate(engine.fileSize,minutes),number(status,"totalWantedDone"),metadata);
 }catch(Throwable ignored){return null;}}
 static long number(Object o,String method)throws Exception{Object v=OnlineSubtitleSearch.call(o,method,new Class<?>[0]);return v instanceof Number?((Number)v).longValue():-1;}
 static void accept(Activity a,Attempt attempt,Stats stats,long now){
  if(LIVE.get(a)!=attempt||attempt.handled||PlaybackBackend.streamOf(a)!=attempt.stream||!Objects.equals(attempt.token,OnlineSubtitleSearch.field(a,"attemptId"))||!eligible(a,attempt.stream))return;
  if(stats==null||!stats.metadata||!TorrentStartupRules.slow(stats.rate,stats.required)){attempt.slowSince=-1;return;}
  if(attempt.slowSince<0)attempt.slowSince=now;
  Object opened=OnlineSubtitleSearch.field(a,"openedAt");if(!(opened instanceof Number))return;
  int timeout=a.getSharedPreferences("data",0).getInt("sourceTimeout",30);
  if(!TorrentStartupRules.switchSource(now-((Number)opened).longValue(),timeout,now-attempt.slowSince,stats.rate,stats.required,stats.verified))return;
  attempt.handled=true;DebugLog.event("P2P","开播持续低速 · 下载 "+stats.rate/1024+" KB/s · 预计需要 "+stats.required/1024+" KB/s · 尝试备用源");
  try{OnlineSubtitleSearch.call(a,"failed",new Class<?>[]{String.class},"种子下载持续低于影片所需速度，尝试其他片源");}catch(Exception ignored){}
 }
 static void dispose(Activity a){LIVE.remove(a);}
}
