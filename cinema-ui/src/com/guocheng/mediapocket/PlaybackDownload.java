package com.guocheng.mediapocket;
import android.app.Activity;import android.widget.Toast;import org.json.*;
/**
 * 0.1.164 播放页「更多 → 下载这一集」。只入队，不暂停、不关闭、不重开播放器。
 * VLC（含系统解码 SystemPlayback）走 PlayerOverlay，内置杜比 Media3 走 SystemPlayerActivity。
 */
final class PlaybackDownload {
 private PlaybackDownload(){}
 static final String LABEL="⬇ 下载这一集";
 static CinemaMenu.Item menu(Activity a,JSONObject stream,JSONObject meta,String watch,String title){
  JSONObject play=stream==null?new JSONObject():stream;
  JSONObject src=source(play,meta,watch);
  String url=play.optString("url");
  boolean emby=!play.optString("emby.item").isEmpty();
  boolean refetch=emby&&(!play.has("emby.direct")||play.optBoolean("emby.direct",true))||src!=null&&"fusion".equals(src.optString("kind"));
  String block=OfflineQueueRules.playerBlock(url,play.has("infoHash"),emby,play.has("emby.direct"),play.optBoolean("emby.direct",true),refetch);
  if(block==null)block=OfflineDownloads.blocked(a,src,url);
  if(block!=null)return CinemaMenu.muted(LABEL,block);
  final JSONObject useSrc=src,useStream=play,useMeta=meta==null?new JSONObject():meta;final String useWatch=watch==null?"":watch,useTitle=title;
  return CinemaMenu.item(LABEL,()->start(a,useStream,useMeta,useWatch,useTitle,useSrc));
 }
 static void start(Activity a,JSONObject stream,JSONObject meta,String watch,String title,JSONObject src){
  if(a==null||a.isFinishing())return;
  String url=stream.optString("url");
  boolean emby=!stream.optString("emby.item").isEmpty();
  boolean direct=!stream.has("emby.direct")||stream.optBoolean("emby.direct",true);
  boolean refetch=emby&&direct||src!=null&&"fusion".equals(src.optString("kind"));
  String block=OfflineQueueRules.playerBlock(url,stream.has("infoHash"),emby,stream.has("emby.direct"),direct,refetch);
  if(block==null)block=OfflineDownloads.blocked(a,src,url);
  if(block!=null){toast(a,block);return;}
  String name=name(meta,title);
  OfflineDownloads.confirm(a,name,()->{if(a.isFinishing())return;if(emby){if(!direct){toast(a,OfflineQueueRules.TRANSCODE);return;}JSONObject server=EmbyStore.get(a,stream.optString("emby.server"));if(server==null){toast(a,"找不到这个 Emby 服务器");return;}OfflineDownloads.embyNow(a,server,stream.optString("emby.item"),name,meta);return;}
   if(src!=null&&"fusion".equals(src.optString("kind"))){if(!OfflineQueueRules.loopback(url)&&OfflineRules.refuse(url)==null)enqueue(a,url,headers(stream),name,watch,meta,src);else fusion(a,src,name,watch);return;}
   if(OfflineQueueRules.loopback(url)||OfflineRules.refuse(url)!=null){toast(a,OfflineQueueRules.loopback(url)?OfflineQueueRules.LOOPBACK:OfflineRules.refuse(url));return;}
   JSONObject directSrc=src!=null?src:urlSource(url);enqueue(a,url,headers(stream),name,watch,meta,directSrc);});
 }
 static void enqueue(Activity a,String url,JSONObject headers,String name,String watch,JSONObject meta,JSONObject src){
  try{JSONObject m=new JSONObject();m.put("id",watch==null?"":watch).put("name",name).put("type",meta==null?"movie":meta.optString("type","movie"));if(meta!=null&&!meta.optString("poster").isEmpty())m.put("poster",meta.optString("poster"));
   OfflineDownloads.enqueue(a,url,headers,name,watch,OfflineRules.extension("",url),m,src);}catch(Exception e){toast(a,"无法下载："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
 }
 static void fusion(Activity a,JSONObject src,String name,String watch){OfflineDownloads.fusionNow(a,src.optString("config"),src.optString("site"),src.optString("flag"),src.optString("value"),name,watch);}
 /** 和长按下载同一套来源键，避免播放页和详情页各下一份。 */
 static JSONObject source(JSONObject stream,JSONObject meta,String watch){
  try{if(stream!=null&&!stream.optString("emby.item").isEmpty())return new JSONObject().put("kind","emby").put("server",stream.optString("emby.server")).put("item",stream.optString("emby.item"));
   JSONObject video=fusionVideo(meta,watch);if(video==null||meta==null)return stream!=null&&!stream.optString("url").isEmpty()?urlSource(stream.optString("url")):null;
   return new JSONObject().put("kind","fusion").put("config",meta.optString("fusion.config")).put("site",meta.optString("fusion.site")).put("flag",video.optString("fusion.flag")).put("value",video.optString("fusion.value"));
  }catch(JSONException e){return null;}
 }
 static JSONObject fusionVideo(JSONObject meta,String watch){JSONArray videos=meta==null?null:meta.optJSONArray("videos");if(videos==null||watch==null||watch.isEmpty())return null;for(int i=0;i<videos.length();i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&watch.equals(v.optString("id"))&&!v.optString("fusion.value").isEmpty())return v;}return null;}
 static JSONObject urlSource(String url){try{return new JSONObject().put("kind","url").put("url",url==null?"":url);}catch(JSONException e){return new JSONObject();}}
 static JSONObject headers(JSONObject stream){JSONObject req=ExternalVideo.headers(stream);if(req.length()>0)return req;JSONObject header=stream.optJSONObject("header");return header==null?new JSONObject():header;}
 static String name(JSONObject meta,String title){if(title!=null&&!title.trim().isEmpty())return title.trim();if(meta!=null&&!meta.optString("name").isEmpty())return meta.optString("name");return "离线影片";}
 static void toast(Activity a,String text){try{Toast.makeText(a,text,Toast.LENGTH_LONG).show();}catch(Exception ignored){}}
}
