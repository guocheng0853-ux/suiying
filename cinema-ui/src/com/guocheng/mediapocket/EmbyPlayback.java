package com.guocheng.mediapocket;
import android.app.*;import android.content.Intent;import android.os.SystemClock;import android.widget.Toast;import org.json.*;import java.util.*;import java.util.concurrent.*;
/** 开播、向 Emby 回报进度，以及剧集自动下一集。网络在后台线程。 */
final class EmbyPlayback {
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"emby-play");t.setDaemon(true);return t;});
 static final Map<Activity,State> ALL=new WeakHashMap<>();
 static final class State {String serverId,item,media,session;boolean direct,started,paused,marked,seeked,stopped;long resumeMs,runtimeTicks,lastReport,lastPos,maxPos;String lastProgress;}
 private EmbyPlayback(){}
 static int pendingAudio=-1,pendingSub=-1;static long pendingBitrate=-1;
 static void play(Activity a,JSONObject meta){if(meta==null)return;JSONObject server=EmbyStore.get(a,meta.optString("emby.server"));String item=meta.optString("emby.item");if(item.isEmpty())item=EmbyApi.itemOf(meta.optString("id"));if(server==null||item.isEmpty()){Toast.makeText(a,"找不到这个 Emby 服务器",Toast.LENGTH_LONG).show();return;}playItem(a,server,item,null,false);}
 static void playItem(Activity a,JSONObject server,String itemId){playItem(a,server,itemId,null,false);}
 static void replay(Activity a,int audioIndex,int subtitleIndex){JSONObject stream=TrackSelection.stream(a);JSONObject server=EmbyStore.get(a,stream.optString("emby.server"));if(server==null||stream.optString("emby.item").isEmpty())return;pendingAudio=audioIndex;pendingSub=subtitleIndex;playItem(a,server,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}
 /** 按新的 MaxStreamingBitrate 重新向 Emby 要一次播放地址。 */
 static void replayBitrate(Activity a,long bitrate){pendingBitrate=bitrate;replay(a,-1,-1);}
 static void playItem(Activity a,JSONObject server,String itemId,String mediaSourceId,boolean replace){
  if(a==null||a.isFinishing()||server==null||itemId==null||itemId.isEmpty())return;CinemaWait wait=CinemaWait.show(a,"正在准备播放","正在向 Emby 请求播放信息…",true,true);final boolean[] cancel={false};wait.setOnCancelListener(d->cancel[0]=true);
  final int audio=pendingAudio,sub=pendingSub,keep=replace?position(a):-1;final long bitrate=pendingBitrate;pendingAudio=-1;pendingSub=-1;pendingBitrate=-1;
  IO.execute(()->{try{
   String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)bases=new String[]{server.optString("url")};
   Exception last=null;JSONObject working=null,item=null,info=null;EmbyApi.Choice choice=null;
   for(String base:bases){JSONObject attempt=new JSONObject(server.toString());attempt.put("url",base);attempt.remove("backups");try{
     item=EmbyClient.item(a,attempt,itemId);JSONObject user=item.optJSONObject("UserData");long resume=keep>10000?keep:EmbyApi.ticksToMs(user==null?0:user.optLong("PlaybackPositionTicks"));
     info=EmbyClient.playbackInfo(a,attempt,itemId,EmbyApi.msToTicks(resume),mediaSourceId,audio,sub,bitrate);
     JSONArray versions=EmbyCue.versions(info.optJSONArray("MediaSources"));
     if((mediaSourceId==null||mediaSourceId.isEmpty())&&versions.length()>1){JSONObject chosenItem=item,chosenInfo=info,route=attempt;a.runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(cancel[0]||a.isFinishing())return;pickVersion(a,route,itemId,versions,replace);});return;}
     choice=EmbyApi.select(attempt.optString("url"),itemId,attempt.optString("token"),info,mediaSourceId,EmbyApi.clientOf(attempt));working=attempt;break;
    }catch(Exception e){last=e;if(!EmbyCue.retryable(e))break;}}
   if(choice==null)throw last==null?new java.io.IOException("无法播放"):last;
   final JSONObject route=working,detail=item,playback=info;final EmbyApi.Choice chosen=choice;JSONObject user=detail.optJSONObject("UserData");long resume=keep>10000?keep:EmbyApi.ticksToMs(user==null?0:user.optLong("PlaybackPositionTicks"));
   JSONObject meta=EmbyApi.meta(route,detail);String type=detail.optString("Type");
   if("Episode".equals(type)&&!detail.optString("SeriesId").isEmpty()){JSONArray episodes=EmbyClient.episodesAll(a,route,detail.optString("SeriesId"),"");meta.put("videos",EmbyApi.videos(route,episodes));meta.put("type","series");String line=EmbyApi.line(detail);if(!line.isEmpty())meta.put("name",meta.optString("name")+" · "+line);}
   JSONObject stream=EmbyApi.stream(route,itemId,chosen,resume).put("emby.runtimeTicks",detail.optLong("RunTimeTicks"));
   if(detail.optJSONArray("Chapters")!=null)stream.put("emby.chapters",detail.optJSONArray("Chapters"));
   JSONArray versions=EmbyCue.versions(playback.optJSONArray("MediaSources"));if(versions.length()>1)stream.put("emby.versions",versions);
   String dk="sdr";{JSONArray ms=playback.optJSONArray("MediaSources");for(int i=0;ms!=null&&i<ms.length();i++){JSONObject src=ms.optJSONObject(i);if(src!=null&&chosen.mediaSourceId.equals(src.optString("Id")))dk=DolbyRules.kind(src);}}final String dolby=dk;if(DolbyRules.risky(dolby))DebugLog.event("杜比视界","片源类型 "+dolby);
   String watch=meta.optString("id");int pos=(int)Math.min(Integer.MAX_VALUE,resume),dur=(int)Math.min(Integer.MAX_VALUE,EmbyApi.ticksToMs(detail.optLong("RunTimeTicks")));
   a.runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(cancel[0]||a.isFinishing())return;if(pos>10000)a.getSharedPreferences("data",0).edit().putInt("pos:"+watch,pos).putInt("dur:"+watch,dur).apply();
    JSONArray sources;try{sources=EmbyApi.withTranscode(stream,route.optString("url"),route.optString("token"),EmbyStore.deviceId(a));}catch(Exception e){sources=new JSONArray().put(stream);}final JSONArray all=sources;
    java.util.function.Consumer<Boolean> vlc=transcodeFirst->{JSONObject first=stream;JSONArray list=all;if(transcodeFirst&&all.length()>1){first=all.optJSONObject(1);list=new JSONArray().put(all.opt(1)).put(all.opt(0));}
     Intent in=new Intent(a,PlayerActivity.class);in.putExtra("stream",first.toString());in.putExtra("sources",list.toString());in.putExtra("id",watch);in.putExtra("type",meta.optString("type","movie"));in.putExtra("meta",meta.toString());in.putExtra("addons",a.getSharedPreferences("data",0).getString("addons","[]"));a.startActivity(in);a.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);if(replace)a.finish();};
    // 0.1.111：杜比视界 P5 在 VLC 里会发紫发绿，按设置改用系统解码 / 服务器转码，或询问
    if(DolbyRules.risky(dolby))DolbyChoice.decide(a,dolby,()->{Intent back=new Intent(a,PlayerActivity.class);back.putExtra("stream",stream.toString());back.putExtra("sources",all.toString());back.putExtra("id",watch);back.putExtra("type",meta.optString("type","movie"));back.putExtra("meta",meta.toString());back.putExtra("addons",a.getSharedPreferences("data",0).getString("addons","[]"));SystemPlayerActivity.open(a,stream,watch,meta.optString("name"),pos,back);if(replace)a.finish();},()->vlc.accept(true),()->vlc.accept(false));else vlc.accept(false);});
  }catch(Exception e){a.runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(!a.isFinishing())Toast.makeText(a,EmbyApi.explain(e),Toast.LENGTH_LONG).show();});}});}
 static void pickVersion(Activity a,JSONObject server,String itemId,JSONArray versions,boolean replace){String[] labels=new String[versions.length()];for(int i=0;i<versions.length();i++)labels[i]=versions.optJSONObject(i).optString("name","版本");new CinemaDialog(a).setTitle("选择版本").setItems(labels,(d,i)->{JSONObject v=versions.optJSONObject(i);if(v!=null)playItem(a,server,itemId,v.optString("id"),replace);}).setNegativeButton("取消",null).show();}
 static boolean openNext(Activity a,Object[] next){if(next==null||next.length<1||!(next[0] instanceof String)||!EmbyApi.isEmbyId((String)next[0]))return false;String id=(String)next[0];JSONObject server=EmbyStore.get(a,EmbyApi.serverOf(id));if(server==null){Toast.makeText(a,"找不到这个 Emby 服务器，无法播放下一集",Toast.LENGTH_LONG).show();return true;}playItem(a,server,EmbyApi.itemOf(id));return true;}
 static void tick(Activity a){try{JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty())return;State s;synchronized(ALL){s=ALL.get(a);if(s!=null&&s.direct!=stream.optBoolean("emby.direct",true)&&!s.stopped){s.direct=stream.optBoolean("emby.direct",true);s.started=false;s.seeked=false;}if(s==null){s=new State();s.serverId=stream.optString("emby.server");s.item=stream.optString("emby.item");s.media=stream.optString("emby.mediaSource");s.session=stream.optString("emby.playSession");s.direct=stream.optBoolean("emby.direct",true);s.resumeMs=stream.optLong("emby.resumeMs");s.runtimeTicks=stream.optLong("emby.runtimeTicks");s.paused=true;ALL.put(a,s);}}
  int pos=position(a);boolean playing=playing(a);if(pos>s.maxPos)s.maxPos=pos;if(pos>=0)s.lastPos=pos;
  if(!s.seeked&&s.resumeMs>15000&&pos>=0&&pos<4000){s.seeked=true;if(!seek(a,(int)Math.min(s.resumeMs,Integer.MAX_VALUE)))s.seeked=false;}
  if(!s.started){if(playing||pos>1000){s.started=true;s.paused=!playing;send(a,s,"start",Math.max(pos,0),!playing);}}
  else{if(playing==s.paused){s.paused=!playing;send(a,s,playing?"unpause":"pause",Math.max(pos,0),!playing);}else if(SystemClock.elapsedRealtime()-s.lastReport>=10000)send(a,s,"progress",Math.max(pos,0),!playing);
   if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks)){s.marked=true;mark(a,s);}}
 }catch(Exception ignored){}}
 static void stop(Activity a){State s;synchronized(ALL){s=ALL.remove(a);}if(s==null||s.stopped||!s.started)return;s.stopped=true;long pos=s.maxPos<5000&&s.resumeMs>15000?s.resumeMs:s.lastPos;send(a,s,"stop",Math.max(0,pos),true);if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks))mark(a,s);}
 static void send(Activity a,State s,String kind,long posMs,boolean paused){final long ticks=EmbyApi.msToTicks(posMs);if(!"start".equals(kind)&&!"stop".equals(kind)){String key=EmbyApi.progressKey(s.session,paused,ticks);if(EmbyApi.sameProgress(s.lastProgress,key))return;s.lastProgress=key;}s.lastReport=SystemClock.elapsedRealtime();IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server==null)return;JSONObject body=EmbyApi.sessionBody(s.item,s.media,s.session,ticks,paused,s.direct);if("start".equals(kind))EmbyClient.playing(a,server,body);else if("stop".equals(kind))EmbyClient.stopped(a,server,body);else{body.put("EventName","pause".equals(kind)?"Pause":"unpause".equals(kind)?"Unpause":"TimeUpdate");EmbyClient.progress(a,server,body);}}catch(Exception ignored){}});}
 static void mark(Activity a,State s){IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server!=null)EmbyClient.played(a,server,s.item);}catch(Exception ignored){}});}
 static int position(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"getCurrentPosition",new Class<?>[0]);return r instanceof Integer?(Integer)r:-1;}catch(Exception e){return -1;}}
 static boolean playing(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"isPlaying",new Class<?>[0]);return Boolean.TRUE.equals(r);}catch(Exception e){return false;}}
 static boolean seek(Activity a,int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");if(video==null)return false;try{OnlineSubtitleSearch.call(video,"seekTo",new Class<?>[]{int.class},ms);}catch(Exception e){OnlineSubtitleSearch.call(video,"seek",new Class<?>[]{int.class},ms);}return true;}catch(Exception e){return false;}}
}
