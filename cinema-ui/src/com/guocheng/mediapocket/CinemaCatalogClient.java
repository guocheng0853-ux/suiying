package com.guocheng.mediapocket;
import java.net.*;import java.io.*;import org.json.JSONObject;
/** UI metadata only; does not alter stream requests or the stable player. */
final class CinemaCatalogClient {
 static JSONObject json(String url)throws Exception {
  HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(20000);connection.setRequestProperty("User-Agent","Mozilla/5.0 MediaPocket/0.1.23");connection.setRequestProperty("Accept","application/json");
  try{int code=connection.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[8192];int count;while((count=in.read(bytes))!=-1){if(out.size()+count>16*1024*1024)throw new IOException("Metadata response too large");out.write(bytes,0,count);}return new JSONObject(out.toString("UTF-8"));}}finally{connection.disconnect();}
 }
}
