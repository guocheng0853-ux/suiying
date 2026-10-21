package com.guocheng.mediapocket;
import android.content.Context;import org.json.*;
/**
 * 0.1.121 记住最近一路还没回报「停止」的 Emby 播放，换片、换播放器、切版本之前先通知服务器停掉它，
 * 并结束它可能还在跑的转码。共享服务器常限制同时播放数，残留的播放记录会让下一次请求被拒（如 HTTP 409）。
 * 所有网络调用都在 EmbyPlayback.IO（单线程）里执行，和开始 / 停止回报保持先后顺序。
 */
final class EmbySessions {
 private EmbySessions(){}
 static JSONObject active;
 static synchronized void started(String serverId,String item,String media,String session,long ticks,boolean direct){try{active=new JSONObject().put("server",serverId).put("item",item).put("media",media).put("session",session).put("ticks",ticks).put("direct",direct);}catch(JSONException ignored){}}
 static synchronized void progress(String session,long ticks){if(active!=null&&session!=null&&session.equals(active.optString("session")))try{active.put("ticks",ticks);}catch(JSONException ignored){}}
 static synchronized void stopped(String session){if(active!=null&&(session==null||session.equals(active.optString("session"))))active=null;}
 static synchronized JSONObject take(){JSONObject a=active;active=null;return a;}
 /** 在 IO 线程里调用：把残留的播放会话停掉。没有残留时什么都不做。返回是否发了停止。 */
 static boolean release(Context c){JSONObject s=take();if(s==null)return false;try{JSONObject server=EmbyStore.get(c,s.optString("server"));if(server==null)return false;
   EmbyClient.stopped(c,server,EmbyApi.sessionBody(s.optString("item"),s.optString("media"),s.optString("session"),s.optLong("ticks"),true,s.optBoolean("direct",true)));
   if(!s.optBoolean("direct",true))try{EmbyClient.json(c,server,"DELETE","/Videos/ActiveEncodings?DeviceId="+EmbyApi.enc(EmbyStore.deviceId(c))+"&PlaySessionId="+EmbyApi.enc(s.optString("session")),null);}catch(Exception ignored){}
   DebugLog.event("Emby","已通知服务器结束上一路播放");return true;}catch(Exception e){return false;}}
}
