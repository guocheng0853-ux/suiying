package com.guocheng.mediapocket;
import android.app.Activity;import android.content.SharedPreferences;import org.json.*;import java.util.*;
/**
 * 0.1.183：影视仓片源播到一半失败时自动重取地址并续播，规则见 {@link FusionReconnectRules}。
 * 由 PlayerOverlay.status 每次轮询调用（和 EmbyAutoRecovery.nativeTick 并排）；读取失败和 VLC「提前结束」两条路都会走到这里。
 * 取到新地址后用 PlaybackSitePicker.apply 原地换源，播放页不重开，宿主按 resumePos 续播。
 * 用户自己换源、选集、或其他站点的自动查找都会让 PlaybackSitePicker 的 token 前进，这里在路上的结果随之作废。
 */
final class FusionReconnect {
 private FusionReconnect(){}
 static final class Run {String id="";volatile int same,total,step,token=-1;volatile long attemptPos=-1;volatile boolean pending,exhausted;volatile String flag="";final Set<String> triedValues=Collections.synchronizedSet(new HashSet<String>());volatile List<FusionPlaybackRules.Route> alternatives;}
 static final Map<Activity,Run> ALL=new WeakHashMap<>();
 static Run run(Activity a){Run r=ALL.get(a);String id=String.valueOf(OnlineSubtitleSearch.field(a,"id"));if(r==null||!r.id.equals(id)){r=new Run();r.id=id;ALL.put(a,r);}return r;}
 static boolean pending(Activity a){Run r=ALL.get(a);return r!=null&&r.pending;}
 static boolean exhausted(Activity a){Run r=ALL.get(a);return r!=null&&r.exhausted&&!r.pending;}
 static String hint(Activity a){Run r=ALL.get(a);return r==null?"":FusionReconnectRules.hint(r.step,r.same,r.flag);}
 static void cancel(Activity a){Run r=ALL.get(a);if(r==null||!r.pending)return;r.pending=false;PlaybackSitePicker.state(a).token++;DebugLog.event("影视仓","自动重连已取消");}
 static void dispose(Activity a){ALL.remove(a);}
 /** 当前这条线路：先看 meta（播放页内换线路后更新的是它），没有再看片源本身（开播时 rememberLine 写进去的）。 */
 static JSONObject line(Activity a){JSONObject s=TrackSelection.stream(a),m=PlaybackSitePicker.meta(a);JSONObject out=new JSONObject();try{for(String k:new String[]{"fusion.config","fusion.site","fusion.flag","fusion.value"}){String v=m.optString(k);out.put(k,v.isEmpty()?s.optString(k):v);}out.put("emby.item",s.optString("emby.item"));}catch(JSONException ignored){}return out;}
 static boolean route(JSONObject l){return FusionReconnectRules.route(l.optString("fusion.config"),l.optString("fusion.site"),l.optString("fusion.flag"),l.optString("fusion.value"),l.optString("emby.item"));}
 static long position(Activity a,String name){Object v=OnlineSubtitleSearch.field(a,name);return v instanceof Number?((Number)v).longValue():0;}
 /** 这条片源归不归这里管（PlaybackSitePicker.onFailure 用它决定要不要先让同线路重连试一下）。 */
 static boolean covers(Activity a){try{Run r=run(a);if(r.exhausted)return false;SharedPreferences p=a.getSharedPreferences("data",0);return FusionReconnectRules.eligible(p.getBoolean("autoSwitch",true),route(line(a)),true,true,false,Math.max(position(a,"resumePos"),position(a,"lastPos")),r.total);}catch(Throwable t){return false;}}
 static void tick(Activity a,boolean failure,boolean blocked,String raw){
  try{
   Run r=run(a);
   if(!failure){if(!r.pending&&r.attemptPos>=0&&FusionReconnectRules.settled(position(a,"lastPos"),r.attemptPos)){r.same=0;r.attemptPos=-1;r.exhausted=false;r.triedValues.clear();r.alternatives=null;}return;}
   if(r.pending){if(PlaybackSitePicker.state(a).token!=r.token)r.pending=false;return;}
   if(r.exhausted)return;
   boolean active=Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"active"));
   if(!FusionReconnectRules.eligible(a.getSharedPreferences("data",0).getBoolean("autoSwitch",true),route(line(a)),true,active,blocked,position(a,"resumePos"),r.total))return;
   start(a,r,raw);
  }catch(Throwable ignored){}
 }
 /** 失败卡片上的「重新连接」：计数清零，重新走一遍。返回 false 表示这条片源不是影视仓线路，调用方按原来的方式重开。 */
 static boolean manual(Activity a){try{if(!route(line(a)))return false;Run r=run(a);r.same=0;r.total=0;r.exhausted=false;r.triedValues.clear();r.alternatives=null;start(a,r,"手动重新连接");return true;}catch(Throwable t){return false;}}
 static void start(final Activity a,final Run r,String raw)throws Exception{
  final JSONObject l=line(a);final JSONObject m=PlaybackSitePicker.meta(a);final PlaybackSitePicker.State state=PlaybackSitePicker.state(a);
  final long resume=position(a,"resumePos");r.pending=true;r.exhausted=false;r.attemptPos=resume;r.token=++state.token;state.automatic=false;final int token=r.token;
  r.step=FusionReconnectRules.SAME;r.flag=l.optString("fusion.flag");
  DebugLog.event("影视仓","播到 "+PlaybackEndRules.mmss(resume)+" 断开（"+(raw==null?"":raw.replace('\n',' '))+"），自动重新获取播放地址");
  final String episode=m.optString("subtitleEpisode");
  PlaybackSitePicker.IO.execute(()->{
   JSONObject got=null;FusionPlaybackRules.Route used=null;FusionCatalog.Session session=null;String last="";
   try{session=FusionCatalog.session(a,l.optString("fusion.config"),l.optString("fusion.site"));}catch(Exception e){last=SourceSearchClient.reason(e);}
   while(session!=null&&got==null&&state.token==token&&PlaybackSitePicker.alive(a)){
    int altCount=r.alternatives==null?-1:r.alternatives.size();int step=FusionReconnectRules.next(r.same,altTried(r),altCount,r.total);
    if(step==FusionReconnectRules.NONE)break;
    FusionPlaybackRules.Route route=null;
    if(step==FusionReconnectRules.SAME){r.same++;route=new FusionPlaybackRules.Route(l.optString("fusion.flag"),episode,l.optString("fusion.value"));r.triedValues.add(route.value);}
    else{
     if(r.alternatives==null)r.alternatives=alternatives(a,session,m,l.optString("fusion.value"));
     for(FusionPlaybackRules.Route c:r.alternatives)if(!r.triedValues.contains(c.value)){route=c;break;}
     if(route==null)break;r.triedValues.add(route.value);
    }
    r.total++;r.step=step;r.flag=route.flag;
    try{got=PlaybackSitePicker.stream(a,session,route,25000);used=route;DebugLog.event("影视仓",(step==FusionReconnectRules.SAME?"重新取到原线路地址":"换到线路「"+route.flag+"」")+"，从 "+PlaybackEndRules.mmss(resume)+" 续播");}
    catch(Exception e){last=SourceSearchClient.reason(e);DebugLog.event("影视仓",(step==FusionReconnectRules.SAME?"原线路重新获取失败：":"线路「"+route.flag+"」不可用：")+last);}
   }
   final JSONObject stream=got;final FusionPlaybackRules.Route route=used;final FusionCatalog.Session s=session;
   a.runOnUiThread(()->{
    if(state.token!=token||!PlaybackSitePicker.alive(a)){r.pending=false;return;}
    r.pending=false;
    if(stream==null||route==null){r.exhausted=true;DebugLog.event("影视仓","自动重连没有成功，交回其他站点查找 / 手动恢复");if(Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"active"))&&Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"autoSwitch")))PlaybackSitePicker.automatic(a);return;}
    if(!Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"terminal")))return;
    try{if(position(a,"resumePos")<resume)SourceSubtitleLoader.set(a,"resumePos",(int)Math.min(Integer.MAX_VALUE,resume));}catch(Exception ignored){}
    PlaybackSitePicker.apply(a,s,stream,episode,false);
    try{PlaybackSitePicker.meta(a).put("fusion.flag",route.flag).put("fusion.value",route.value);}catch(Exception ignored){}
    FullscreenMode.hud(a,FusionReconnectRules.resumed(resume));
   });
  });
 }
 static int altTried(Run r){if(r.alternatives==null)return 0;int n=0;for(FusionPlaybackRules.Route c:r.alternatives)if(r.triedValues.contains(c.value))n++;return n;}
 /** 同一站点详情里，和当前这一集对得上的其他线路；读不到详情就当没有。 */
 static List<FusionPlaybackRules.Route> alternatives(Activity a,FusionCatalog.Session session,JSONObject m,String current){
  ArrayList<FusionPlaybackRules.Route> out=new ArrayList<>();
  try{String id=m.optString("fusion.video");if(id.isEmpty())return out;JSONArray list=PlaybackSitePicker.request(a,session,"detail",id,"",1,20000).optJSONArray("list");JSONObject detail=list==null?null:list.optJSONObject(0);if(detail==null)return out;
   HashSet<String> seen=new HashSet<>();seen.add(current);for(FusionPlaybackRules.Route c:PlaybackSitePicker.routes(detail,m)){if(c.value.isEmpty()||MagnetLinks.isMagnet(c.value)||!seen.add(c.value))continue;out.add(c);if(out.size()>=FusionReconnectRules.ALT_MAX)break;}
  }catch(Exception e){DebugLog.event("影视仓","读取其他线路失败："+SourceSearchClient.reason(e));}
  return out;
 }
}
