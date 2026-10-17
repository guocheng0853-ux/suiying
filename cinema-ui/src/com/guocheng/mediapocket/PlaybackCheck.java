package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import org.json.*;import java.text.SimpleDateFormat;import java.util.*;
/** 播放前地址检查的界面与记录（最近 30 条，只记主机名，不记完整地址和请求头）。 */
final class PlaybackCheck {
 static final String KEY="playback.check.log.v1";
 static void log(Context c,String source,String url,String via,LinkProbe.Result r,String decision){DebugLog.event("地址检查",source+" · "+LinkProbe.host(url)+" · "+(via==null?"":via+" · ")+r.summary()+" · "+decision);
  try{SharedPreferences p=c.getSharedPreferences("data",0);JSONArray old=new JSONArray(p.getString(KEY,"[]")),out=new JSONArray();
   out.put(new JSONObject().put("t",System.currentTimeMillis()).put("source",source).put("host",LinkProbe.host(url)).put("via",via==null?"":via).put("result",r.summary()).put("media",r.media).put("decision",decision));
   for(int i=0;i<old.length()&&out.length()<30;i++)out.put(old.get(i));p.edit().putString(KEY,out.toString()).apply();}catch(Exception ignored){}
 }
 static String report(Context c){
  try{JSONArray a=new JSONArray(c.getSharedPreferences("data",0).getString(KEY,"[]"));if(a.length()==0)return "还没有播放地址检查记录。";
   SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA);StringBuilder s=new StringBuilder("“有视频数据”只表示地址返回了视频/分片数据，最终能否解码以播放器画面为准。\n\n");
   for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);s.append(f.format(new Date(x.optLong("t")))).append("  ").append(x.optString("source")).append("\n  ").append(x.optBoolean("media")?"✓ 有视频数据":"✗ 未确认").append(" · ").append(x.optString("result")).append("\n  ").append(x.optString("host"));if(!x.optString("via").isEmpty())s.append(" · ").append(x.optString("via"));s.append(" · ").append(x.optString("decision")).append("\n\n");}
   return s.toString().trim();}catch(Exception e){return "记录读取失败";}
 }
 /** 明确不是视频（HTTP 错误/网页/JSON/空响应）时让用户选择；其余情况直接播放。 */
 static void decide(Activity a,String source,String url,String via,LinkProbe.Result r,Runnable play){
  if(!r.definitive){log(a,source,url,via,r,r.media?"播放":"检查未确认，交给播放器");play.run();return;}
  if(a.isFinishing()||a.isDestroyed())return;
  new AlertDialog.Builder(a).setTitle("这个地址大概率放不出来").setMessage(r.summary()+"\n\n来源："+source+(via==null||via.isEmpty()?"":"\n取得方式："+via)+"\n\n可以返回换一条线路，或仍然交给播放器尝试。")
   .setPositiveButton("返回换源",(d,n)->log(a,source,url,via,r,"用户返回"))
   .setNegativeButton("仍然播放",(d,n)->{log(a,source,url,via,r,"用户仍然播放");play.run();})
   .setOnCancelListener(d->log(a,source,url,via,r,"用户返回")).show();
 }
}
