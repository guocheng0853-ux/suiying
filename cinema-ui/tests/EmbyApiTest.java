package com.guocheng.mediapocket;
import org.json.*;import java.net.*;
public class EmbyApiTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  String bare=EmbyApi.authorization("And\"roid","dev,1","0.1.72","");
  check(bare.equals("MediaBrowser Client=\"MediaPocket\", Device=\"Android\", DeviceId=\"dev1\", Version=\"0.1.72\""),"auth header without token");
  check(!bare.contains("Token=")&&!bare.contains("\n")&&!bare.contains("\""+"\r"),"header stays one line");
  String authed=EmbyApi.authorization("Android","dev-1",null,"abc");
  check(authed.endsWith(", Token=\"abc\"")&&authed.contains("Version=\"0.1.72\""),"token appended and default version");
  check(EmbyApi.server("http://192.168.1.8:8096/").equals("http://192.168.1.8:8096"),"port kept, slash trimmed");
  check(EmbyApi.server("https://media.example:8920/emby/").equals("https://media.example:8920/emby"),"https path prefix kept");
  check(EmbyApi.authenticateUrl("http://10.0.0.2:8096").equals("http://10.0.0.2:8096/Users/AuthenticateByName"),"authenticate path");
  JSONObject body=EmbyApi.authenticateBody("ada","p@ss");check(body.getString("Username").equals("ada")&&body.getString("Pw").equals("p@ss")&&body.getString("Password").equals("p@ss"),"Pw and Password");
  for(String bad:new String[]{"ftp://host/a","http://user:pw@host:8096","notaurl","http://","  "}){boolean rejected=false;try{EmbyApi.server(bad);}catch(IllegalArgumentException e){rejected=e.getMessage()!=null&&(e.getMessage().contains("http")||e.getMessage().contains("地址")||e.getMessage().contains("账号")||e.getMessage().contains("填写"));}check(rejected,"rejected "+bad);}
  String image=EmbyApi.imageUrl("http://h:8096","item 1","Primary","tok en",400,"tag/1");
  check(image.startsWith("http://h:8096/Items/")&&image.contains("/Images/Primary")&&image.contains("api_key=")&&image.contains("tag="),"image url");
  check(EmbyApi.msToTicks(1000)==10000000L&&EmbyApi.ticksToMs(10000000L)==1000L,"tick second");
  check(EmbyApi.msToTicks(0)==0&&EmbyApi.msToTicks(-5)==0&&EmbyApi.ticksToMs(-1)==0,"tick non-positive");
  check(EmbyApi.msToTicks(Long.MAX_VALUE)==Long.MAX_VALUE,"tick saturates");
  check(EmbyApi.ticksToMs(EmbyApi.msToTicks(123456))==123456L,"tick round trip");
  JSONObject direct=source("file-1",true,false,null,null);
  JSONObject trans=source("file-2",false,false,"/videos/abc/master.m3u8?id=1",null);
  JSONObject info=new JSONObject().put("PlaySessionId","play1").put("MediaSources",new JSONArray().put(trans).put(direct));
  EmbyApi.Choice chosen=EmbyApi.select("http://10.0.0.8:8096","item", "tok", info);
  check(chosen.direct&&chosen.url.contains("static=true")&&chosen.url.contains("MediaSourceId=file-1")&&chosen.url.contains("api_key=tok")&&chosen.url.contains("PlaySessionId=play1"),"direct static wins over earlier transcode");
  check(!chosen.url.contains("master.m3u8"),"transcode url not chosen");
  EmbyApi.Choice fallback=EmbyApi.select("http://10.0.0.8:8096","abc","tok",new JSONObject().put("PlaySessionId","p").put("MediaSources",new JSONArray().put(trans)));
  check(!fallback.direct&&fallback.url.equals("http://10.0.0.8:8096/videos/abc/master.m3u8?id=1&api_key=tok"),"transcode fallback absolute");
  JSONObject given=source("ms",true,true,null,"http://h:8096/videos/abc/stream.mkv?static=true");
  EmbyApi.Choice kept=EmbyApi.select("http://h:8096","abc","secret",new JSONObject().put("PlaySessionId","s").put("MediaSources",new JSONArray().put(given)));
  check(kept.direct&&kept.url.startsWith("http://h:8096/videos/abc/stream.mkv?static=true")&&kept.url.contains("api_key=secret"),"existing static url kept");
  check(kept.subtitles.length()==2&&kept.subtitles.getJSONObject(0).getString("name").contains("外挂")&&kept.subtitles.getJSONObject(1).getString("name").contains("内嵌"),"external and embedded subtitles");
  check(kept.subtitles.getJSONObject(0).getString("url").contains("api_key=secret")&&kept.subtitles.getJSONObject(1).getString("url").contains("/Subtitles/3/"),"subtitle urls");
  check(kept.audio.length()==1&&kept.audio.getJSONObject(0).getString("label").contains("6声道"),"audio track label");
  JSONObject session=EmbyApi.sessionBody("item","ms","s",EmbyApi.msToTicks(1500),false,true);
  check(session.getString("PlayMethod").equals("DirectStream")&&session.getLong("PositionTicks")==15000000L,"playing body ticks");
  check(EmbyApi.sessionBody("item","ms","s",0,true,false).getString("PlayMethod").equals("Transcode"),"transcode method");
  long second=10000000L;check(EmbyApi.played(900*second,1000*second)&&!EmbyApi.played(800*second,1000*second),"ninety percent");
  check(EmbyApi.played(80*second,100*second),"tail under 30s counts as played");
  check(!EmbyApi.played(0,1000*second)&&!EmbyApi.played(10,0),"no false played");
  check(EmbyApi.explain(new SocketTimeoutException("slow")).contains("超时"),"timeout message");
  check(EmbyApi.explain(new UnknownHostException("nope")).contains("找不到"),"dns message");
  check(EmbyApi.explain(new java.net.ConnectException("refused")).contains("无法连接"),"connect message");
  check(EmbyApi.explain(new java.io.IOException("HTTP 401")).contains("密码"),"password message");
  check(EmbyApi.explain(new IllegalArgumentException(messageOf("ftp://x"))).contains("http"),"bad scheme message");
  String[] applied=HomeRules.apply("provider:emby:srv-1");check(applied[0].equals("emby")&&applied[1].equals("srv-1"),"emby source id");
  check(HomeRules.apply("provider:fusion")[0].equals("fusion")&&HomeRules.selectedId("stremio","current").equals("provider:stremio"),"existing providers unchanged");
  check(HomeRules.selectedId("emby","srv-1").equals("provider:emby:srv-1"),"selected emby");
  check(EmbyApi.isEmbyId("emby:srv:item")&&EmbyApi.itemOf("emby:srv:item").equals("item")&&EmbyApi.serverOf("emby:srv:item").equals("srv"),"watch id");
  System.out.println("PASS: Emby auth header, server URL, ticks, PlaybackInfo direct-or-transcode selection, subtitles and audio.");
 }
 static String messageOf(String url){try{EmbyApi.server(url);return "";}catch(IllegalArgumentException e){return e.getMessage();}}
 static JSONObject source(String id,boolean direct,boolean play,String trans,String directUrl)throws Exception{
  JSONObject s=new JSONObject().put("Id",id).put("SupportsDirectStream",direct).put("SupportsDirectPlay",play);
  if(trans!=null)s.put("TranscodingUrl",trans);if(directUrl!=null)s.put("DirectStreamUrl",directUrl);
  JSONArray streams=new JSONArray();
  streams.put(new JSONObject().put("Index",1).put("Type","Audio").put("DisplayTitle","国语").put("Language","chi").put("Channels",6).put("Codec","aac"));
  streams.put(new JSONObject().put("Index",2).put("Type","Subtitle").put("DisplayTitle","简体").put("Language","chi").put("IsExternal",true).put("DeliveryMethod","External").put("DeliveryUrl","/Videos/abc/ms/Subtitles/2/Stream.srt"));
  streams.put(new JSONObject().put("Index",3).put("Type","Subtitle").put("DisplayTitle","繁体").put("Language","chi").put("IsExternal",false).put("DeliveryMethod","Embed").put("Codec","ass"));
  return s.put("MediaStreams",streams);
 }
}
