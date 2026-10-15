package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.net.*;import java.util.concurrent.*;
/** Stremio /stream 请求与条目分类（纯 Java，可单测）。 */
final class StremioStreams {
 static final ScheduledExecutorService WATCHDOG=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"stremio-watchdog");t.setDaemon(true);return t;});
 /** 与原实现相同的 UA；超过预算主动断开连接，避免一个慢插件长期占住线程。最多 100 条，与原选源页一致。 */
 static JSONArray fetch(String url,int budgetMs)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(Math.min(budgetMs,12000));c.setReadTimeout(Math.min(budgetMs,20000));c.setRequestProperty("User-Agent","MediaPocket/0.1");
  ScheduledFuture<?> kill=WATCHDOG.schedule(c::disconnect,budgetMs,TimeUnit.MILLISECONDS);long start=System.currentTimeMillis();
  try{
   int code=c.getResponseCode();if(code!=200)throw new IOException("服务器返回 HTTP "+code);
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>16*1024*1024)throw new IOException("响应过大");}}
   JSONObject response=new JSONObject(out.toString("UTF-8"));if(response.has("error"))throw new IOException(response.optString("error","插件查询失败"));JSONArray streams=response.optJSONArray("streams");JSONArray result=new JSONArray();
   for(int i=0;streams!=null&&i<Math.min(100,streams.length());i++){JSONObject s=streams.optJSONObject(i);if(s!=null)result.put(s);}
   return result;
  }catch(IOException e){if(System.currentTimeMillis()-start>=budgetMs-50)throw new SocketTimeoutException("超过 "+budgetMs/1000+" 秒");throw e;}
  finally{kill.cancel(false);c.disconnect();}
 }
 static String episodeLabel(String id){String[] p=id.split(":");return p.length>=3?"第 "+p[p.length-2]+" 季 · 第 "+p[p.length-1]+" 集":"";}
 static boolean playable(JSONObject s){if(!s.optString("externalUrl").isEmpty())return true;if(s.has("infoHash"))return true;String u=s.optString("url");return u.matches("(?i)^(https?|rtmp|rtsp)://.+");}
 static String kind(JSONObject s){
  if(!s.optString("externalUrl").isEmpty())return "外部网页 · 不在应用内播放";
  if(s.has("infoHash"))return "P2P · 种子";
  String u=s.optString("url");
  if(u.matches("(?i)^https?://.+")){JSONObject h=s.optJSONObject("behaviorHints");boolean headers=h!=null&&h.optJSONObject("proxyHeaders")!=null;return "直链"+(headers?" · 需请求头":"");}
  if(u.matches("(?i)^(rtmp|rtsp)://.+"))return "直播流";
  if(!s.optString("ytId").isEmpty())return "YouTube · 本应用不支持";
  if(!s.optString("nzbUrl").isEmpty())return "Usenet · 本应用不支持";
  return "没有视频地址";
 }
}
