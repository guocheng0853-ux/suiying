package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.net.*;import java.util.*;
/** 0.1.112 离线下载的 HTTP 部分（不依赖 Android，便于测试）。 */
final class OfflineHttp {
 private OfflineHttp(){}
 static final String UA="MediaPocket/0.1.3";
 /** 打开连接；手动跟随重定向（http↔https 也跟），每一跳都带上片源要求的请求头。 */
 static HttpURLConnection open(String url,JSONObject headers,long from)throws IOException{
  String current=url;for(int hop=0;hop<8;hop++){HttpURLConnection c=(HttpURLConnection)new URL(current).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(30000);
   boolean ua=false;for(Iterator<String> it=headers.keys();it.hasNext();){String k=it.next();String v=headers.optString(k);if(k.isEmpty()||v.isEmpty()||k.equalsIgnoreCase("Range")||k.equalsIgnoreCase("Host"))continue;c.setRequestProperty(k,v);if(k.equalsIgnoreCase("User-Agent"))ua=true;}
   if(!ua)c.setRequestProperty("User-Agent",UA);if(from>0)c.setRequestProperty("Range","bytes="+from+"-");c.setRequestProperty("Accept-Encoding","identity");
   int code=c.getResponseCode();if(code==301||code==302||code==303||code==307||code==308){String loc=c.getHeaderField("Location");c.disconnect();if(loc==null||loc.isEmpty())throw new IOException("重定向没有目标地址");current=new URL(new URL(current),loc).toString();continue;}
   return c;}
  throw new IllegalStateException("重定向次数过多");}
}
