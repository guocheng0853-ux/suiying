package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** 弹弹play HTTP。密钥由调用方传入，这里不保存。 */
final class DanDanPlayClient {
 private DanDanPlayClient(){}
 static JSONObject get(String appId,String secret,String pathAndQuery)throws Exception{return exchange("GET",appId,secret,pathAndQuery,null);}
 static JSONObject post(String appId,String secret,String path,String body)throws Exception{return exchange("POST",appId,secret,path,body);}
 static JSONObject exchange(String method,String appId,String secret,String pathAndQuery,String body)throws Exception{
  String why=DanDanPlayRules.reason(appId,secret);if(why!=null)throw new IllegalStateException(why);
  long timestamp=System.currentTimeMillis()/1000L;String path=DanDanPlayRules.pathOnly(pathAndQuery);String sign=DanDanPlayRules.signature(appId,timestamp,path,secret);
  HttpURLConnection c=(HttpURLConnection)new URL(DanDanPlayRules.HOST+pathAndQuery).openConnection();
  c.setConnectTimeout(8000);c.setReadTimeout(15000);c.setRequestMethod(method);c.setInstanceFollowRedirects(false);
  c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","suiying/0.1.82");
  c.setRequestProperty("X-AppId",appId.trim());c.setRequestProperty("X-Timestamp",Long.toString(timestamp));c.setRequestProperty("X-Signature",sign);c.setRequestProperty("X-Auth","1");
  try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
   int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();String text=read(in);if(text.isEmpty())throw new IOException(DanDanPlayRules.failure(null,status));
   JSONObject json=new JSONObject(text);if(status<200||status>=300)throw new IOException(DanDanPlayRules.failure(json,status));return json;
  }finally{c.disconnect();}
 }
 static String read(InputStream in)throws IOException{if(in==null)return "";try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n,total=0;while((n=src.read(b))!=-1){total+=n;if(total>2*1024*1024)throw new IOException("弹幕数据过大");out.write(b,0,n);}return out.toString("UTF-8");}}
}
