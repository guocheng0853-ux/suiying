package com.guocheng.mediapocket;
import android.app.*;import android.content.Intent;import android.os.SystemClock;import android.widget.Toast;import org.json.*;import java.util.*;import java.util.concurrent.*;
/** 开播、向 Emby 回报进度，以及剧集自动下一集。网络在后台线程。 */
final class EmbyPlayback {
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"emby-play");t.setDaemon(true);return t;});
 /** 409 时不要在开播线程上 sleep，也不要再 release 一次（第一次 take 已经把会话清掉了）。 */
 static final ScheduledExecutorService RETRY=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"emby-play-delay");t.setDaemon(true);return t;});
 static final Map<Activity,State> ALL=new WeakHashMap<>();
 static final class State {String serverId,item,media,session;boolean direct,started,paused,marked,seeked,stopped;long resumeMs,runtimeTicks,lastReport,lastPos,maxPos;String lastProgress;}
 private EmbyPlayback(){}
 static final Map<Activity,Boolean> SWITCHING=new WeakHashMap<>();
 static int pendingAudio=-1,pendingSub=-1;static long pendingBitrate=-1;
 static void play(Activity a,JSONObject meta){if(meta==null)return;JSONObject server=EmbyStore.get(a,meta.optString("emby.server"));String item=meta.optString("emby.item");if(item.isEmpty())item=EmbyApi.itemOf(meta.optString("id"));if(server==null||item.isEmpty()){Toast.makeText(a,"找不到这个 Emby 服务器",Toast.LENGTH_LONG).show();return;}playItem(a,server,item,null,false);}
 /**
  * 0.1.133 首页「播放 / 继续播放」一键开播：电影、单集直接请求播放；剧集 / 季先找这部剧的下一集（Shows/NextUp?SeriesId），
  * 没有就取第一集没看完的，再没有就从第一集开始。找不到可播的集才退回详情页。
  */
 static void playSmart(Activity a,JSONObject meta){if(meta==null)return;String type=meta.optString("emby.type");
  if(!"Series".equals(type)&&!"Season".equals(type)){play(a,meta);return;}
  JSONObject server=EmbyStore.get(a,meta.optString("emby.server"));String series="Series".equals(type)?meta.optString("emby.item"):meta.optString("emby.series");if(series.isEmpty())series=EmbyApi.itemOf(meta.optString("id"));
  if(server==null||series.isEmpty()){EmbyLibraryActivity.open(a,meta);return;}final String sid=series;
  CinemaWait wait=CinemaWait.show(a,"正在准备播放","正在找要看的那一集…",true,true);final boolean[] cancel={false};wait.setOnCancelListener(d->cancel[0]=true);
  IO.execute(()->{String pick="";try{JSONArray next=EmbyClient.json(a,server,"GET","/Shows/NextUp?UserId="+EmbyApi.enc(server.optString("userId"))+"&SeriesId="+EmbyApi.enc(sid)+"&Limit=1",null).optJSONArray("Items");if(next!=null&&next.length()>0)pick=next.optJSONObject(0).optString("Id");
    if(pick.isEmpty()){JSONArray all=EmbyClient.episodes(a,server,sid,null).optJSONArray("Items");for(int i=0;all!=null&&i<all.length();i++){JSONObject ep=all.optJSONObject(i);if(ep==null)continue;if(pick.isEmpty())pick=ep.optString("Id");JSONObject ud=ep.optJSONObject("UserData");if(ud==null||!ud.optBoolean("Played")){pick=ep.optString("Id");break;}}}}
   catch(Exception ignored){}
   final String chosen=pick;a.runOnUiThread(()->{try{dismiss(wait);}catch(RuntimeException ignored){}if(cancel[0]||a.isFinishing())return;if(chosen.isEmpty())EmbyLibraryActivity.open(a,meta);else playItem(a,server,chosen,null,false);});});}
 static void playItem(Activity a,JSONObject server,String itemId){playItem(a,server,itemId,null,false);}
 static void replay(Activity a,int audioIndex,int subtitleIndex){JSONObject stream=TrackSelection.stream(a);JSONObject server=EmbyStore.get(a,stream.optString("emby.server"));if(server==null||stream.optString("emby.item").isEmpty())return;pendingAudio=audioIndex;pendingSub=subtitleIndex;playItem(a,server,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}
 /** 按新的 MaxStreamingBitrate 重新向 Emby 要一次播放地址。 */
 static void replayBitrate(Activity a,long bitrate){pendingBitrate=bitrate;replay(a,-1,-1);}
 static void playItem(Activity a,JSONObject server,String itemId,String mediaSourceId,boolean replace){EmbyAutoRecovery.cancel(a);playItem(a,server,itemId,mediaSourceId,replace,null);}
 static void playItem(Activity a,JSONObject server,String itemId,String mediaSourceId,boolean replace,EmbyAutoRecovery.Attempt recovery){
  if(a==null||a.isFinishing()||server==null||itemId==null||itemId.isEmpty()){handoffDone(a);return;}CinemaWait wait=recovery==null?CinemaWait.show(a,"正在准备播放","正在向 Emby 请求播放信息…",true,true):null;final boolean[] cancel={false};if(wait!=null)wait.setOnCancelListener(d->{cancel[0]=true;handoffDone(a);});
  final int recoveryCount=EmbyRecoveryRules.carry(replace,TrackSelection.stream(a).optString("emby.item"),itemId,EmbyAutoRecovery.count(a));
  final boolean wasTranscode=recovery!=null&&TrackSelection.stream(a).optBoolean("emby.transcodeFallback",false);
  final boolean episodeHandoff=SWITCHING.containsKey(a)||(a instanceof SystemPlayerActivity&&((SystemPlayerActivity)a).changing&&!TrackSelection.stream(a).optString("emby.item").equals(itemId));
  final int audio=pendingAudio,sub=pendingSub,keep=EmbyResumeRules.keep(replace,TrackSelection.stream(a).optString("emby.item"),itemId,replace?recoverPosition(a):position(a));final long bitrate=pendingBitrate;pendingAudio=-1;pendingSub=-1;pendingBitrate=-1;
  final boolean[] retried={false};
  IO.execute(new Runnable(){public void run(){try{
   if(cancel[0]||a.isFinishing()||(recovery!=null&&!EmbyAutoRecovery.valid(a,recovery))){a.runOnUiThread(()->{dismiss(wait);handoffDone(a);});return;}
   if(!retried[0])EmbySessions.release(a);
   String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)bases=new String[]{server.optString("url")};
   Exception last=null;JSONObject working=null,item=null,info=null;EmbyApi.Choice choice=null;
   for(String base:bases){JSONObject attempt=new JSONObject(server.toString());attempt.put("url",base);attempt.remove("backups");try{
     item=EmbyClient.item(a,attempt,itemId);JSONObject user=item.optJSONObject("UserData");long resume=keep>10000?keep:EmbyApi.ticksToMs(user==null?0:user.optLong("PlaybackPositionTicks"));
     try{info=EmbyClient.playbackInfo(a,attempt,itemId,EmbyApi.msToTicks(resume),mediaSourceId,audio,sub,bitrate);}catch(java.io.IOException busy){if(!EmbyApi.conflict(busy)||retried[0])throw busy;retried[0]=true;DebugLog.event("Emby","HTTP 409，2 秒后重试一次");RETRY.schedule(()->IO.execute(this),2,TimeUnit.SECONDS);return;}
     JSONArray versions=EmbyCue.versions(info.optJSONArray("MediaSources"));
     if(recovery!=null&&(mediaSourceId==null||mediaSourceId.isEmpty())&&versions.length()>1)throw new java.io.IOException("请选择要播放的版本");
     if((mediaSourceId==null||mediaSourceId.isEmpty())&&versions.length()>1&&!episodeHandoff){JSONObject chosenItem=item,chosenInfo=info,route=attempt;a.runOnUiThread(()->{dismiss(wait);handoffDone(a);if(cancel[0]||a.isFinishing()||(recovery!=null&&!EmbyAutoRecovery.valid(a,recovery)))return;pickVersion(a,route,itemId,versions,replace);});return;}
     choice=EmbyApi.select(attempt.optString("url"),itemId,attempt.optString("token"),info,mediaSourceId,EmbyApi.clientOf(attempt));working=attempt;break;
    }catch(Exception e){last=e;if(!EmbyCue.retryable(e))break;}}
   if(choice==null)throw last==null?new java.io.IOException("无法播放"):last;
   final JSONObject route=working,detail=item,playback=info;final EmbyApi.Choice chosen=choice;JSONObject user=detail.optJSONObject("UserData");long resume=keep>10000?keep:EmbyApi.ticksToMs(user==null?0:user.optLong("PlaybackPositionTicks"));
   JSONObject meta=EmbyApi.meta(route,detail);String type=detail.optString("Type");
   if("Episode".equals(type)&&!detail.optString("SeriesId").isEmpty()){JSONArray remembered=recovery==null?null:PlayerOverlay.meta(a).optJSONArray("videos");if(remembered!=null)meta.put("videos",new JSONArray(remembered.toString()));else{JSONArray episodes=EmbyClient.episodesAll(a,route,detail.optString("SeriesId"),"");meta.put("videos",EmbyApi.videos(route,episodes));}meta.put("type","series");String line=EmbyApi.line(detail);if(!line.isEmpty())meta.put("name",meta.optString("name")+" · "+line);}
   JSONObject stream=EmbyApi.stream(route,itemId,chosen,resume).put("emby.runtimeTicks",detail.optLong("RunTimeTicks"));
   if(detail.optJSONArray("Chapters")!=null)stream.put("emby.chapters",detail.optJSONArray("Chapters"));
   JSONArray versions=EmbyCue.versions(playback.optJSONArray("MediaSources"));if(versions.length()>1)stream.put("emby.versions",versions);
   String dk="sdr";{JSONArray ms=playback.optJSONArray("MediaSources");for(int i=0;ms!=null&&i<ms.length();i++){JSONObject src=ms.optJSONObject(i);if(src!=null&&chosen.mediaSourceId.equals(src.optString("Id")))dk=DolbyRules.kind(src);}}stream.put("dolby.kind",dk);final String dolby=dk;if(DolbyRules.risky(dolby))DebugLog.event("杜比视界","片源类型 "+dolby);
   String watch=meta.optString("id");int pos=(int)Math.min(Integer.MAX_VALUE,resume),dur=(int)Math.min(Integer.MAX_VALUE,EmbyApi.ticksToMs(detail.optLong("RunTimeTicks")));
   a.runOnUiThread(()->{dismiss(wait);handoffDone(a);if(cancel[0]||a.isFinishing()||(recovery!=null&&!EmbyAutoRecovery.valid(a,recovery)))return;if(pos>10000)a.getSharedPreferences("data",0).edit().putInt("pos:"+watch,pos).putInt("dur:"+watch,dur).apply();
    JSONArray sources;try{sources=EmbyApi.withTranscode(stream,route.optString("url"),route.optString("token"),EmbyStore.deviceId(a));}catch(Exception e){sources=new JSONArray().put(stream);}final JSONArray all=sources;
    java.util.function.Consumer<Boolean> vlc=transcodeFirst->{JSONObject first=stream;JSONArray list=all;if(transcodeFirst&&all.length()>1){first=all.optJSONObject(1);list=new JSONArray().put(all.opt(1)).put(all.opt(0));}
     Intent in=new Intent(a,PlayerActivity.class);in.putExtra("stream",first.toString());in.putExtra("sources",list.toString());in.putExtra("id",watch);in.putExtra("type",meta.optString("type","movie"));in.putExtra("meta",meta.toString());in.putExtra("addons",a.getSharedPreferences("data",0).getString("addons","[]"));in.putExtra(EmbyRecoveryRules.COUNT,recoveryCount);in.putExtra(EmbyRecoveryRules.ITEM,itemId);if(recovery!=null){if(!EmbyAutoRecovery.valid(a,recovery))return;if(!a.hasWindowFocus()){EmbyAutoRecovery.suspend(a);return;}EmbyAutoRecovery.complete(a,recovery);if(a instanceof SystemPlayerActivity){SystemPlayerActivity.open(a,first,watch,meta.optString("name"),pos,in);if(replace)a.finish();return;}}a.startActivity(in);a.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);if(replace)a.finish();};
    // 0.1.111：杜比视界 P5 在 VLC 里会发紫发绿，按设置改用系统解码 / 服务器转码，或询问
    if(recovery!=null)vlc.accept(wasTranscode);else if(DolbyRules.risky(dolby))DolbyChoice.decide(a,dolby,stream.optString("url"),()->{if(ExternalVideo.open(a,stream.optString("url"),meta.optString("name"),pos,ExternalVideo.headers(stream))){if(replace)a.finish();}else vlc.accept(false);},()->{Intent back=new Intent(a,PlayerActivity.class);back.putExtra(EmbyRecoveryRules.COUNT,recoveryCount);back.putExtra(EmbyRecoveryRules.ITEM,itemId);back.putExtra("stream",stream.toString());back.putExtra("sources",all.toString());back.putExtra("id",watch);back.putExtra("type",meta.optString("type","movie"));back.putExtra("meta",meta.toString());back.putExtra("addons",a.getSharedPreferences("data",0).getString("addons","[]"));SystemPlayerActivity.open(a,stream,watch,meta.optString("name"),pos,back);if(replace)a.finish();},()->vlc.accept(true),()->vlc.accept(false));else vlc.accept(false);});
  }catch(Exception e){a.runOnUiThread(()->{dismiss(wait);handoffDone(a);if(recovery!=null)EmbyAutoRecovery.failed(a,recovery);else if(!a.isFinishing())Toast.makeText(a,EmbyApi.explain(e),Toast.LENGTH_LONG).show();});}}});}
 static void dismiss(CinemaWait wait){if(wait!=null&&wait.isShowing())wait.dismiss();}
 static void handoffDone(Activity a){if(a==null)return;SWITCHING.remove(a);if(a instanceof SystemPlayerActivity)((SystemPlayerActivity)a).changing=false;}
 static void pickVersion(Activity a,JSONObject server,String itemId,JSONArray versions,boolean replace){String[] labels=new String[versions.length()];for(int i=0;i<versions.length();i++)labels[i]=versions.optJSONObject(i).optString("name","版本");new CinemaDialog(a).setTitle("选择版本").setItems(labels,(d,i)->{JSONObject v=versions.optJSONObject(i);if(v!=null)playItem(a,server,itemId,v.optString("id"),replace);}).setNegativeButton("取消",null).show();}
 /** True means the Emby request was handled, including failures that keep the old page. */
 static boolean switchEpisode(Activity a,String id){if(!EmbyApi.isEmbyId(id))return false;if(a instanceof SystemPlayerActivity){((SystemPlayerActivity)a).playEpisode(id);return true;}if(SWITCHING.containsKey(a))return true;JSONObject server=EmbyStore.get(a,EmbyApi.serverOf(id));if(server==null){Toast.makeText(a,"找不到这一集的 Emby 服务器，当前播放未关闭",Toast.LENGTH_LONG).show();return true;}SWITCHING.put(a,true);playItem(a,server,EmbyApi.itemOf(id),"",true);return true;}
 static boolean openNext(Activity a,Object[] next){if(next==null||next.length<1||!(next[0] instanceof String)||!EmbyApi.isEmbyId((String)next[0]))return false;String id=(String)next[0];JSONObject server=EmbyStore.get(a,EmbyApi.serverOf(id));if(server==null){Toast.makeText(a,"找不到这个 Emby 服务器，无法播放下一集",Toast.LENGTH_LONG).show();return true;}playItem(a,server,EmbyApi.itemOf(id));return true;}
 static void tick(Activity a){try{JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty())return;State s;synchronized(ALL){s=ALL.get(a);if(s!=null&&s.direct!=stream.optBoolean("emby.direct",true)&&!s.stopped){s.direct=stream.optBoolean("emby.direct",true);s.started=false;s.seeked=false;}if(s==null){s=new State();s.serverId=stream.optString("emby.server");s.item=stream.optString("emby.item");s.media=stream.optString("emby.mediaSource");s.session=stream.optString("emby.playSession");s.direct=stream.optBoolean("emby.direct",true);s.resumeMs=stream.optLong("emby.resumeMs");s.runtimeTicks=stream.optLong("emby.runtimeTicks");s.paused=true;ALL.put(a,s);}}
  int pos=position(a);boolean playing=playing(a);if(pos>s.maxPos)s.maxPos=pos;if(pos>=0)s.lastPos=pos;
  if(!s.seeked&&s.resumeMs>15000&&pos>=0&&pos<4000){s.seeked=true;if(!seek(a,(int)Math.min(s.resumeMs,Integer.MAX_VALUE)))s.seeked=false;}
  if(!s.started){if(playing||pos>1000){s.started=true;s.paused=!playing;send(a,s,"start",Math.max(pos,0),!playing);}}
  else{if(playing==s.paused){s.paused=!playing;send(a,s,playing?"unpause":"pause",Math.max(pos,0),!playing);}else if(SystemClock.elapsedRealtime()-s.lastReport>=10000)send(a,s,"progress",Math.max(pos,0),!playing);
   if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks)){s.marked=true;mark(a,s);}}
 }catch(Exception ignored){}}
 static void stop(Activity a){State s;synchronized(ALL){s=ALL.remove(a);}if(s==null||s.stopped||!s.started)return;s.stopped=true;long pos=s.maxPos<5000&&s.resumeMs>15000?s.resumeMs:s.lastPos;send(a,s,"stop",Math.max(0,pos),true);if(!s.marked&&EmbyApi.played(EmbyApi.msToTicks(s.maxPos),s.runtimeTicks))mark(a,s);}
 static void send(Activity a,State s,String kind,long posMs,boolean paused){final long ticks=EmbyApi.msToTicks(posMs);if(!"start".equals(kind)&&!"stop".equals(kind)){String key=EmbyApi.progressKey(s.session,paused,ticks);if(EmbyApi.sameProgress(s.lastProgress,key))return;s.lastProgress=key;}s.lastReport=SystemClock.elapsedRealtime();IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server==null)return;JSONObject body=EmbyApi.sessionBody(s.item,s.media,s.session,ticks,paused,s.direct);if("start".equals(kind)){EmbySessions.started(s.serverId,s.item,s.media,s.session,ticks,s.direct);EmbyClient.playing(a,server,body);}else if("stop".equals(kind)){EmbySessions.stopped(s.session);EmbyClient.stopped(a,server,body);}else{EmbySessions.progress(s.session,ticks);body.put("EventName","pause".equals(kind)?"Pause":"unpause".equals(kind)?"Unpause":"TimeUpdate");EmbyClient.progress(a,server,body);}}catch(Exception ignored){}});}
 static void mark(Activity a,State s){IO.execute(()->{try{JSONObject server=EmbyStore.get(a,s.serverId);if(server!=null)EmbyClient.played(a,server,s.item);}catch(Exception ignored){}});}
 static int recoverPosition(Activity a){Object saved=OnlineSubtitleSearch.field(a,"resumePos");long retained=saved instanceof Number?((Number)saved).longValue():a instanceof SystemPlayerActivity?((SystemPlayerActivity)a).start:0;return EmbyRecoveryRules.resume(position(a),retained,TrackSelection.stream(a).optLong("emby.resumeMs"));}
 static int position(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"getCurrentPosition",new Class<?>[0]);return r instanceof Integer?(Integer)r:-1;}catch(Exception e){return -1;}}
 static boolean playing(Activity a){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"isPlaying",new Class<?>[0]);return Boolean.TRUE.equals(r);}catch(Exception e){return false;}}
 static boolean seek(Activity a,int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");if(video==null)return false;try{OnlineSubtitleSearch.call(video,"seekTo",new Class<?>[]{int.class,boolean.class},ms,Boolean.TRUE);return true;}catch(NoSuchMethodException missing){}try{OnlineSubtitleSearch.call(video,"seekTo",new Class<?>[]{int.class},ms);}catch(Exception e){OnlineSubtitleSearch.call(video,"seek",new Class<?>[]{int.class},ms);}return true;}catch(Exception e){return false;}}
}
