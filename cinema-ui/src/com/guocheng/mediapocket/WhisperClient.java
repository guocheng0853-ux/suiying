package com.guocheng.mediapocket;
import org.json.*;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
/** Whisper 服务器桥的纯逻辑：请求组装、响应解析、状态中文。JVM 可测，不做网络。 */
final class WhisperClient {
 private WhisperClient(){}
 static String trim(String base){String s=base==null?"":base.trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
 /** 云端访问不到的地址：本机、局域网、链路本地。只看字面主机名，不做 DNS（在界面线程调用）。 */
 static boolean unreachableFromCloud(String url){try{String h=new URI(url).getHost();if(h==null)return true;h=h.toLowerCase(java.util.Locale.ROOT);if(h.startsWith("[")&&h.endsWith("]"))h=h.substring(1,h.length()-1);if(h.equals("localhost")||h.endsWith(".local")||h.equals("::1")||h.startsWith("fe80:")||h.startsWith("fc")||h.startsWith("fd"))return true;String[] p=h.split("\\.");if(p.length!=4)return false;int[] n=new int[4];for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;n[i]=Integer.parseInt(p[i]);}return n[0]==127||n[0]==10||n[0]==0||(n[0]==192&&n[1]==168)||(n[0]==172&&n[1]>=16&&n[1]<=31)||(n[0]==169&&n[1]==254)||(n[0]==100&&n[1]>=64&&n[1]<=127);}catch(Exception e){return true;}}
 static String jobUrl(String base){return trim(base)+"/job";}
 static String srtUrl(String base,String id){return trim(base)+"/srt/"+id;}
 static String jobBody(String mediaUrl,String model){
  JSONObject o=new JSONObject();
  try{o.put("url",mediaUrl==null?"":mediaUrl);o.put("model",model==null||model.trim().isEmpty()?"base":model.trim());}catch(JSONException ignored){}
  return o.toString();
 }
 static String jobId(String resp){return safe(resp).optString("job_id","");}
 static String status(String resp){return safe(resp).optString("status","");}
 static String error(String resp){return safe(resp).optString("error","");}
 static String statusLabel(String status){
  if(status==null)return "";
  switch(status){
   case "queued":return "排队中";
   case "downloading":return "云端下载视频";
   case "transcribing":return "语音识别中";
   case "done":return "完成";
   case "failed":return "失败";
  }
  return status;
 }
 static JSONObject safe(String resp){try{return new JSONObject(resp==null?"":resp);}catch(JSONException e){return new JSONObject();}}
 static String post(String url,String token,String body)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setRequestMethod("POST");c.setDoOutput(true);
  c.setRequestProperty("X-Token",token==null?"":token);c.setRequestProperty("Content-Type","application/json");
  try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}
  int code=c.getResponseCode();String text=read(code>=400?c.getErrorStream():c.getInputStream());
  if(code<200||code>=300)throw new IOException("HTTP "+code+(text.isEmpty()?"":"："+text));
  return text;
 }
 static String get(String url,String token)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setRequestProperty("X-Token",token==null?"":token);
  int code=c.getResponseCode();String text=read(code>=400?c.getErrorStream():c.getInputStream());
  if(code<200||code>=300)throw new IOException("HTTP "+code+(text.isEmpty()?"":"："+text));
  return text;
 }
 static String read(InputStream in)throws IOException{if(in==null)return "";try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=src.read(b))!=-1){out.write(b,0,n);}return out.toString("UTF-8");}}
}
