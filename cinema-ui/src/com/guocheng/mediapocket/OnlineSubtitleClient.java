package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
/** 射手与 OpenSubtitles。Token / API Key 只从参数读取。 */
final class OnlineSubtitleClient {
 private OnlineSubtitleClient(){}
 static JSONObject assrt(String url)throws Exception{return get(url,null);}
 static JSONObject xunlei(String url)throws Exception{return get(url,null);}
 static JSONObject opensubtitles(String url,String apiKey)throws Exception{return get(url,apiKey);}
 static JSONObject openDownload(String apiKey,long fileId)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(OnlineSubtitleRules.OPEN_DOWNLOAD).openConnection();
  c.setConnectTimeout(8000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setDoOutput(true);
  c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json");
  c.setRequestProperty("Api-Key",apiKey.trim());c.setRequestProperty("User-Agent",OnlineSubtitleRules.UA);
  byte[] body=("{\"file_id\":"+fileId+"}").getBytes(StandardCharsets.UTF_8);
  try{try(OutputStream o=c.getOutputStream()){o.write(body);}int status=c.getResponseCode();String text=read(status>=400?c.getErrorStream():c.getInputStream());if(status<200||status>=300)throw new IOException(status==401||status==403?"OpenSubtitles 拒绝了 API Key":"OpenSubtitles 下载失败（HTTP "+status+"）");return new JSONObject(text);}finally{c.disconnect();}
 }
 static JSONObject get(String url,String apiKey)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(true);
  c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent",OnlineSubtitleRules.UA);if(apiKey!=null)c.setRequestProperty("Api-Key",apiKey.trim());
  try{int status=c.getResponseCode();String text=read(status>=400?c.getErrorStream():c.getInputStream());if(status<200||status>=300)throw new IOException("字幕接口返回 HTTP "+status);return text.isEmpty()?new JSONObject():new JSONObject(text);}finally{c.disconnect();}
 }
 static byte[] bytes(String url)throws Exception{return SubtitleHttp.download(url,null);}
 static String read(InputStream in)throws IOException{if(in==null)return "";try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n,total=0;while((n=src.read(b))!=-1){total+=n;if(total>1024*1024)throw new IOException("字幕列表过大");out.write(b,0,n);}return out.toString("UTF-8");}}
}
