package com.guocheng.mediapocket;
import android.app.*;import android.content.Intent;import android.os.SystemClock;import android.widget.Toast;import org.json.*;import java.util.*;import java.util.concurrent.*;
/** 开播、向 Emby 回报进度，以及剧集自动下一集。网络在后台线程。 */
final class EmbyPlayback {
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"emby-play");t.setDaemon(true);return t;});
 static final Map<Activity,State> ALL=new WeakHashMap<>();
 static final class State {String serverId,item,media,session;boolean direct,started,paused,marked,seeked,stopped;long resumeMs,runtimeTicks,lastReport,lastPos,maxPos;}
 private EmbyPlayback(){}
 static void play(Activity a,JSONObject meta){if(meta==null)return;JSONObject server=EmbyStore.get(a,meta.optString("emby.server"));String item=meta.optString("emby.item");if(item.isEmpty())item=EmbyApi.itemOf(meta.optString("id"));if(server==null||item.isEmpty()){Toast.makeText(a,"找不到这个 Emby 服务器",Toast.LENGTH_LONG).show();return;}playItem(a,server,item);}
 static void playItem(Activity a,JSONObject server,String itemId){
  if(a.isFinishing())return;ProgressDialog wait=ProgressDialog.show(a,"正在准备播放","正在向 Emby 请求播放信息…",true,true);final boolean[] cancel={false};wait.setOnCancelListener(d->cancel[0]=true);
  IO.execute(()->{try{JSONObject item=EmbyClient.item(a,server,itemId);JSONObject user=item.optJSONObject("UserData");long resume=EmbyApi.ticksToMs(user==null?0:user.optLong("PlaybackPositionTicks"));
   JSONObject info=EmbyClient.playbackInfo(a,server,itemId,EmbyApi.msToTicks(resume));EmbyApi.Choice choice=EmbyApi.select(server.optString("url"),itemId,server.optString("token"),info);
   JSONObject meta=EmbyApi.meta(server,item);String type=item.optString("Type");
   if("Episode".equals(type)&&!item.optString("SeriesId").isEmpty()){JSONArray episodes=EmbyClient.episodes(a,server,item.optString("SeriesId"),"").optJSONArray("Items");meta.put("videos",EmbyApi.videos(server,episodes));meta.put("type","series");String line=EmbyApi.line(item);if(!line.isEmpty())meta.put("name",meta.optString("name")+" · "+line);}
   JSONObject stream=EmbyApi.stream(server,itemId,choice,resume).put("emby.runtimeTicks",item.optLong("RunTimeTicks"));
   String watch=meta.optString("id");int pos=(int)Math.min(Integer.MAX_VALUE,resume),dur=(int)Math.min(Integer.MAX_VALUE,EmbyApi.ticksToMs(item.optLong("RunTimeTicks")));
   a.runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(cancel[0]||a.isFinishing())return;if(pos>10000)a.getSharedPreferences("data",0).edit().putInt("pos:"+watch,pos).putInt("dur:"+watch,dur).apply();
    Intent in=new Intent(a,PlayerActivity.class);in.putExtra("stream",stream.toString());in.putExtra("sources",new JSONArray().put(stream).toString());in.putExtra("id",watch);in.putExtra("type",meta.optString("type","movie"));in.putExtra("meta",meta.toString());in.putExtra("addons",a.getSharedPreferences("data",0).getString("addons","[]"));a.startActivity(in);});
  }catch(Exception e){a.runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(!a.isFinishing())Toast.makeText(a,EmbyApi.explain(e),Toast.LENGTH_LONG).show();});}});}
 static boolean openNext(Activity a,Object[] next){if(next==null||next.length<1||!(next[0] instanceof String)||!EmbyApi.isEmbyId((String)next[0]))return false;String id=(String)next[0];JSONObject server=EmbyStore.get(a,EmbyApi.serverOf(id));if(server==null){Toast.makeText(a,"找不到这个 Emby 服务器，无法播放下一集",Toast.LENGTH_LONG).show();return true;}playItem(a,server,EmbyApi.itemOf(id));return true;}
 static void tick(Activity a){try{JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty())return;State s;synchronized(ALL){s=ALL.get(a);if(s==null){s=new State();s.serverId=stream.optString("emby.server");s.item=stream.optString("emby.item");s.media=stream.optString("emby.mediaSource");s.session=stream.optString("emby.playSession");s.direct=stream.optBoolean("emby.direct",true);s.resumeMs=stream.optLong("emby.resumeMs");s.runtimeTicks=stream.optLong("emby.runtimeTicks");s.paused=true;ALL.put(a,s);}}
  int pos=position(a);boolean playing=playing(a);if(pos>s.maxPos)s.maxPos=pos;if(pos>=0)s.lastPos=pos;
  if(!s.seeked&&s.resumeMs>15000&&pos>=0&&pos<4000){s.seeked=true;if(!seek(a,(int)Math.min(s.resumeMs,Integer.MAX_VALUE)))s.seeked=false;}
  if(!s.started){if(playing||pos>1000){s.started=true;s.paused=!playing;send(a,s,"start",Math.max(pos,0),!playing);}}
  else{if(playing==s.paused){s.paused=!playing;send(a,s,playing?"unpause":"pause",Math.max(pos,0),!playing);}else if(SystemClock.elapsedRealtime()-s.lastReport>=10000)send(a,s,"progress",Math.max(pos,0),!playing);
   if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks)){s.marked=true;mark(a,s);}}
 }catch(Exception ignored){}}
 static void stop(Activity a){State s;synchronized(ALL){s=ALL.remove(a);}if(s==null||s.stopped||!s.started)return;s.stopped=true;long pos=s.maxPos<5000&&s.resumeMs>15000?s.resumeMs:s.lastPos;send(a,s,"stop",Math.max(0,pos),true);if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks))mark(a,s);}
 static void send(Activity a,State s,String kind,long posMs,boolean paused){final long ticks=EmbyApi.msToTicks(posMs);s.lastReport=SystemClock.elapsedRealtime();IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server==null)return;JSONObject body=EmbyApi.sessionBody(s.item,s.media,s.session,ticks,paused,s.direct);if("start".equals(kind))EmbyClient.playing(a,server,body);else if("stop".equals(kind))EmbyClient.stopped(a,server,body);else{body.put("EventName","pause".equals(kind)?"Pause":"unpause".equals(kind)?"Unpause":"TimeUpdate");EmbyClient.progress(a,server,body);}}catch(Exception ignored){}});}
 static void mark(Activity a,State s){IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server!=null)EmbyClient.played(a,server,s.item);}catch(Exception ignored){}});}
 static int position(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"getCurrentPosition",new Class<?>[0]);return r instanceof Integer?(Integer)r:-1;}catch(Exception e){return -1;}}
 static boolean playing(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"isPlaying",new Class<?>[0]);return Boolean.TRUE.equals(r);}catch(Exception e){return false;}}
 static boolean seek(Activity a,int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");if(video==null)return false;try{OnlineSubtitleSearch.call(video,"seekTo",new Class<?>[]{int.class},ms);}catch(Exception e){OnlineSubtitleSearch.call(video,"seek",new Class<?>[]{int.class},ms);}return true;}catch(Exception e){return false;}}
}
