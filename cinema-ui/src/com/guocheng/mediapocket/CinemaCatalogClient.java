package com.guocheng.mediapocket;
import java.net.*;import java.io.*;import org.json.JSONObject;
/** UI metadata only; does not alter stream requests or the stable player. */
final class CinemaCatalogClient {
 /** 解析接口等文本请求：带声明的请求头，1MB 上限。 */
 static String text(String url,JSONObject headers)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Mozilla/5.0");
  java.util.Iterator<String> k=headers.keys();while(k.hasNext()){String key=k.next();String v=headers.optString(key);if(!key.matches(".*[\\r\\n:].*")&&!v.matches("(?s).*[\\r\\n].*"))c.setRequestProperty(key,v);}
  try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("响应过大");out.write(b,0,n);}return out.toString("UTF-8");}}finally{c.disconnect();}
 }
 static JSONObject json(String url)throws Exception {
  HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(20000);connection.setRequestProperty("User-Agent","Mozilla/5.0 MediaPocket/0.1.23");connection.setRequestProperty("Accept","application/json");
  try{int code=connection.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[8192];int count;while((count=in.read(bytes))!=-1){if(out.size()+count>16*1024*1024)throw new IOException("Metadata response too large");out.write(bytes,0,count);}return new JSONObject(out.toString("UTF-8"));}}finally{connection.disconnect();}
 }
}
