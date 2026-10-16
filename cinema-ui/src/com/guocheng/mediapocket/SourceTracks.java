package com.guocheng.mediapocket;
import android.content.Context;import android.net.Uri;import org.json.*;import java.util.*;
/** Reads actual embedded track metadata from the bundled LibVLC, never from a filename. */
final class SourceTracks {
 static JSONObject inspect(Context context,JSONObject stream)throws Exception{
  String url=stream.optString("url");if(!(url.startsWith("http://")||url.startsWith("https://")))throw new Exception("此片源需开始播放后，在播放器内选择真实轨道");
  Object lib=null,media=null;
  try{
   Class<?> libClass=Class.forName("org.videolan.libvlc.LibVLC"),mediaClass=Class.forName("org.videolan.libvlc.Media"),api=Class.forName("org.videolan.libvlc.interfaces.ILibVLC");
   lib=libClass.getConstructor(Context.class,List.class).newInstance(context,Arrays.asList("--no-video-title-show","--no-metadata-network-access"));
   JSONObject hints=stream.optJSONObject("behaviorHints"),proxy=hints==null?null:hints.optJSONObject("proxyHeaders"),request=proxy==null?null:proxy.optJSONObject("request");Map<String,String> headers=request==null?new HashMap<>():TvBoxPlay.map(request);
   String route=HeaderRoute.url(url,headers);media=mediaClass.getConstructor(api,Uri.class).newInstance(lib,Uri.parse(route));
   Map<String,String> supported=HeaderRoute.headers(headers);if(supported.containsKey("User-Agent"))OnlineSubtitleSearch.call(media,"addOption",new Class<?>[]{String.class},":http-user-agent="+supported.get("User-Agent"));if(supported.containsKey("Referer"))OnlineSubtitleSearch.call(media,"addOption",new Class<?>[]{String.class},":http-referrer="+supported.get("Referer"));
   OnlineSubtitleSearch.call(media,"parseAsync",new Class<?>[]{int.class,int.class},1,10000);long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(11);
   while(!Boolean.TRUE.equals(OnlineSubtitleSearch.call(media,"isParsed",new Class<?>[0]))&&System.nanoTime()<end){if(Thread.currentThread().isInterrupted())throw new InterruptedException();Thread.sleep(100);}
   if(!Boolean.TRUE.equals(OnlineSubtitleSearch.call(media,"isParsed",new Class<?>[0])))throw new Exception("轨道读取超时，可直接播放后在播放器内选择");
   JSONArray audio=new JSONArray(),subs=new JSONArray();int count=((Number)OnlineSubtitleSearch.call(media,"getTrackCount",new Class<?>[0])).intValue();
   for(int i=0;i<count;i++){Object track=OnlineSubtitleSearch.call(media,"getTrack",new Class<?>[]{int.class},i);if(track==null)continue;Object value=OnlineSubtitleSearch.field(track,"type");int type=value instanceof Number?((Number)value).intValue():-1;if(type!=0&&type!=2)continue;Object trackId=OnlineSubtitleSearch.field(track,"id");if(!(trackId instanceof Number))continue;String lang=string(track,"language"),codec=string(track,"codec"),desc=string(track,"description");ArrayList<String> details=new ArrayList<>();if(!lang.isEmpty())details.add(lang);if(!desc.isEmpty())details.add(desc);if(!codec.isEmpty())details.add(codec.toUpperCase(Locale.ROOT));Object channels=OnlineSubtitleSearch.field(track,"channels");if(type==0&&channels instanceof Number&&((Number)channels).intValue()>0)details.add(((Number)channels).intValue()+" 声道");JSONObject item=new JSONObject().put("id",((Number)trackId).intValue()).put("label",details.isEmpty()?"轨道 "+(i+1):String.join(" · ",details));(type==0?audio:subs).put(item);}
   return new JSONObject().put("audio",audio).put("subtitles",subs);
  }finally{release(media);release(lib);}
 }
 static String string(Object o,String field){Object value=OnlineSubtitleSearch.field(o,field);return value==null?"":String.valueOf(value);}
 static void release(Object o){if(o!=null)try{OnlineSubtitleSearch.call(o,"release",new Class<?>[0]);}catch(Exception ignored){}}
}
