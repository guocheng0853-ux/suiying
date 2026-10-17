package com.guocheng.mediapocket;
import android.content.Context;import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
/** Emby 请求都在调用方的后台线程里执行。连接 8 秒、读取 15 秒。 */
final class EmbyClient {
 static final int CONNECT_MS=8000,READ_MS=15000,LIMIT=8*1024*1024;
 private EmbyClient(){}
 static JSONObject authenticate(Context c,JSONObject server,String username,String password)throws Exception{
  String base=EmbyApi.server(server.optString("url"));String device=EmbyStore.deviceId(c);
  byte[] body=EmbyApi.authenticateBody(username,password).toString().getBytes(StandardCharsets.UTF_8);
  HttpResult r=exchange("POST",EmbyApi.authenticateUrl(base),EmbyApi.authorization("Android",device,EmbyApi.VERSION,""),null,body,false);
  if(r.code==401||r.code==403)throw new IOException("HTTP 401");
  if(r.code<200||r.code>=300)throw new IOException("HTTP "+r.code);
  JSONObject json=new JSONObject(r.body);String token=json.optString("AccessToken");JSONObject user=json.optJSONObject("User");
  if(token.isEmpty()||user==null||user.optString("Id").isEmpty())throw new IOException("服务器没有返回登录令牌");
  server.put("url",base).put("username",username).put("password",password==null?"":password).put("token",token).put("userId",user.optString("Id")).put("serverId",json.optString("ServerId"));
  if(server.optString("name").isEmpty()||server.optString("name").equals(server.optString("url"))){try{JSONObject info=publicInfo(base);if(!info.optString("ServerName").isEmpty())server.put("name",info.optString("ServerName"));}catch(Exception ignored){}}
  if(server.optString("name").isEmpty())server.put("name",host(base));
  return server;
 }
 static JSONObject publicInfo(String base)throws Exception{HttpResult r=exchange("GET",EmbyApi.join(EmbyApi.server(base),"/System/Info/Public"),EmbyApi.authorization("Android","mediapocket",EmbyApi.VERSION,""),null,null,false);if(r.code!=200)throw new IOException("HTTP "+r.code);return new JSONObject(r.body);}
 static JSONObject views(Context c,JSONObject server)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Views",null);}
 static final String FIELDS="Overview,ProductionYear,CommunityRating,RunTimeTicks,Genres,UserData,ImageTags,BackdropImageTags";
 static JSONObject resume(Context c,JSONObject server)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/Resume?Limit=16&MediaTypes=Video&Fields="+FIELDS+"&EnableImages=true",null);}
 static JSONObject nextUp(Context c,JSONObject server)throws Exception{return json(c,server,"GET","/Shows/NextUp?UserId="+EmbyApi.enc(server.optString("userId"))+"&Limit=16&Fields="+FIELDS+"&EnableImages=true",null);}
 static JSONObject homeSections(Context c,JSONObject server)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/HomeSections",null);}
 static JSONObject genres(Context c,JSONObject server,String parentId)throws Exception{return json(c,server,"GET","/Genres?UserId="+EmbyApi.enc(server.optString("userId"))+"&ParentId="+EmbyApi.enc(parentId)+"&Recursive=true&SortBy=SortName",null);}
 static JSONObject latest(Context c,JSONObject server,String parentId)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/Latest?ParentId="+EmbyApi.enc(parentId)+"&Limit=16&Fields="+FIELDS+"&EnableImages=true",null);}
 static final int PAGE=100;
 static JSONObject items(Context c,JSONObject server,String parentId,String types,int start)throws Exception{return items(c,server,parentId,types,start,"",false);}
 static JSONObject items(Context c,JSONObject server,String parentId,String types,int start,String genre,boolean favorite)throws Exception{return json(c,server,"GET",EmbyCue.libraryPath(server.optString("userId"),parentId,types,start,genre,favorite),null);}
 static JSONObject search(Context c,JSONObject server,String word)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items?SearchTerm="+EmbyApi.enc(word)+"&Recursive=true&IncludeItemTypes=Movie,Series&Limit=24&Fields=Overview,ProductionYear,CommunityRating,RunTimeTicks,Genres,UserData",null);}
 static JSONObject item(Context c,JSONObject server,String itemId)throws Exception{return json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/"+EmbyApi.enc(itemId)+"?Fields=Overview,ProductionYear,CommunityRating,RunTimeTicks,Genres,UserData,MediaSources,Chapters,ImageTags,BackdropImageTags",null);}
 static JSONObject seasons(Context c,JSONObject server,String seriesId)throws Exception{return json(c,server,"GET","/Shows/"+EmbyApi.enc(seriesId)+"/Seasons?UserId="+EmbyApi.enc(server.optString("userId")),null);}
 static JSONObject episodes(Context c,JSONObject server,String seriesId,String seasonId)throws Exception{String q="/Shows/"+EmbyApi.enc(seriesId)+"/Episodes?UserId="+EmbyApi.enc(server.optString("userId"))+"&Fields=Overview,RunTimeTicks,UserData";if(seasonId!=null&&!seasonId.isEmpty())q+="&SeasonId="+EmbyApi.enc(seasonId);return json(c,server,"GET",q,null);}
 static JSONObject playbackInfo(Context c,JSONObject server,String itemId,long startTicks)throws Exception{return playbackInfo(c,server,itemId,startTicks,null,-1,-1);}
 static JSONObject playbackInfo(Context c,JSONObject server,String itemId,long startTicks,String mediaSourceId,int audioIndex,int subtitleIndex)throws Exception{return json(c,server,"POST","/Items/"+EmbyApi.enc(itemId)+"/PlaybackInfo?UserId="+EmbyApi.enc(server.optString("userId"))+"&AutoOpenLiveStream=true",EmbyApi.playbackInfoBody(server.optString("userId"),startTicks,mediaSourceId,audioIndex,subtitleIndex).toString());}
 static byte[] bytes(Context c,JSONObject server,String path)throws Exception{
  String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));Exception last=null;
  for(String base:bases){try{return readBytes(EmbyApi.withKey(EmbyApi.join(base,path),server.optString("token")),EmbyApi.authorization("Android",EmbyStore.deviceId(c),EmbyApi.VERSION,server.optString("token")),server.optString("token"));}catch(Exception e){last=e;if(!EmbyCue.retryable(e))throw e;}}
  if(last instanceof Exception)throw (Exception)last;throw new java.io.IOException("无法下载");
 }
 static void playing(Context c,JSONObject server,JSONObject body)throws Exception{json(c,server,"POST","/Sessions/Playing",body.toString());}
 static void progress(Context c,JSONObject server,JSONObject body)throws Exception{json(c,server,"POST","/Sessions/Playing/Progress",body.toString());}
 static void stopped(Context c,JSONObject server,JSONObject body)throws Exception{json(c,server,"POST","/Sessions/Playing/Stopped",body.toString());}
 static void played(Context c,JSONObject server,String itemId)throws Exception{json(c,server,"POST","/Users/"+EmbyApi.enc(server.optString("userId"))+"/PlayedItems/"+EmbyApi.enc(itemId),"{}");}
 static JSONObject json(Context c,JSONObject server,String method,String path,String body)throws Exception{
  HttpResult r=call(c,server,method,path,body,true);
  if(r.body==null||r.body.isEmpty())return new JSONObject();
  String t=r.body.trim();if(t.startsWith("["))return new JSONObject().put("Items",new JSONArray(t));
  if(!t.startsWith("{"))return new JSONObject();
  return new JSONObject(t);
 }
 static HttpResult call(Context c,JSONObject server,String method,String path,String body,boolean retry)throws Exception{
  String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)bases=new String[]{server.optString("url")};
  byte[] bytes=body==null?null:body.getBytes(StandardCharsets.UTF_8);IOException last=new IOException("无法连接服务器");
  for(int i=0;i<bases.length;i++){
   try{HttpResult r=exchange(method,EmbyApi.join(bases[i],path),EmbyApi.authorization("Android",EmbyStore.deviceId(c),EmbyApi.VERSION,server.optString("token")),server.optString("token"),bytes,true);
    if(r.code==401&&retry){authenticate(c,server,server.optString("username"),server.optString("password"));EmbyStore.save(c,server);return call(c,server,method,path,body,false);}
    if(r.code==401)throw new IOException("HTTP 401");
    if(r.code>=500&&i+1<bases.length){last=new IOException("HTTP "+r.code);continue;}
    if(r.code<200||r.code>=300)throw new IOException("HTTP "+r.code);
    return r;
   }catch(SocketTimeoutException|UnknownHostException|ConnectException|javax.net.ssl.SSLException e){last=e instanceof IOException?(IOException)e:new IOException(e);}
  }
  throw last;
 }
 static byte[] readBytes(String url,String authorization,String token)throws Exception{
  String current=url;
  for(int hop=0;hop<4;hop++){HttpURLConnection conn=(HttpURLConnection)new URL(current).openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(CONNECT_MS);conn.setReadTimeout(READ_MS);
   conn.setRequestProperty("User-Agent","MediaPocket/"+EmbyApi.VERSION);if(authorization!=null)conn.setRequestProperty("X-Emby-Authorization",authorization);
   int code;try{code=conn.getResponseCode();}catch(Exception e){conn.disconnect();throw e;}
   if(code>=300&&code<400){String loc=conn.getHeaderField("Location");URL next=loc==null?null:new URL(conn.getURL(),loc);conn.disconnect();if(next==null||!next.getHost().equalsIgnoreCase(new URL(current).getHost()))throw new IOException("HTTP "+code);current=next.toString();continue;}
   if(code<200||code>=300){conn.disconnect();throw new IOException("HTTP "+code);}
   try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n,cap=24*1024*1024;while((n=in.read(buf))!=-1){if(out.size()+n>cap)throw new IOException("预览文件过大");out.write(buf,0,n);}return out.toByteArray();}finally{conn.disconnect();}
  }
  throw new IOException("服务器跳转次数过多");
 }
 static HttpResult exchange(String method,String url,String authorization,String token,byte[] body,boolean sendToken)throws Exception{
  String current=url;
  for(int hop=0;hop<4;hop++){
   URL u=new URL(current);if(!(u.getProtocol().equals("http")||u.getProtocol().equals("https")))throw new IOException("服务器地址需要以 http:// 或 https:// 开头");
   HttpURLConnection conn=(HttpURLConnection)u.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(CONNECT_MS);conn.setReadTimeout(READ_MS);conn.setRequestMethod(method);
   conn.setRequestProperty("Accept","application/json");conn.setRequestProperty("User-Agent","MediaPocket/"+EmbyApi.VERSION);
   if(authorization!=null&&!authorization.isEmpty())conn.setRequestProperty("X-Emby-Authorization",authorization);
   if(sendToken&&token!=null&&!token.isEmpty())conn.setRequestProperty("X-Emby-Token",token);
   if(body!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");try(OutputStream out=conn.getOutputStream()){out.write(body);}}
   int code;try{code=conn.getResponseCode();}catch(SocketTimeoutException e){conn.disconnect();throw e;}catch(UnknownHostException e){conn.disconnect();throw e;}catch(ConnectException e){conn.disconnect();throw e;}
   if(code>=300&&code<400){String loc=conn.getHeaderField("Location");conn.disconnect();if(loc==null||loc.isEmpty())throw new IOException("HTTP "+code);URL next=new URL(u,loc);if(!next.getHost().equalsIgnoreCase(u.getHost()))throw new IOException("服务器跳转到了其他主机");current=next.toString();continue;}
   InputStream in=code>=400?conn.getErrorStream():conn.getInputStream();String text="";
   if(in!=null)try(InputStream s=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=s.read(buf))!=-1){if(out.size()+n>LIMIT)throw new IOException("服务器响应过大");out.write(buf,0,n);}text=out.toString("UTF-8");}finally{conn.disconnect();}
   else conn.disconnect();
   return new HttpResult(code,text);
  }
  throw new IOException("服务器跳转次数过多");
 }
 static String host(String base){try{String h=new URI(base).getHost();return h==null||h.isEmpty()?base:h;}catch(Exception e){return base;}}
 static final class HttpResult {final int code;final String body;HttpResult(int code,String body){this.code=code;this.body=body==null?"":body;}}
}
