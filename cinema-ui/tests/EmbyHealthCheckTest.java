package com.guocheng.mediapocket;
import java.util.*;import org.json.*;
public class EmbyHealthCheckTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final String SECRET="fixture-token-do-not-copy",HOST="private.example",USER="user-secret",FILM="film-secret";
 static JSONObject server()throws Exception{return new JSONObject().put("url","https://"+HOST+"/emby").put("token",SECRET).put("userId",USER).put("username","private-user").put("password","private-password");}
 static class Fake implements EmbyHealthCheck.Backend {
  int calls,posts;boolean empty,authFailed,transcode,cancelled;String body;
  public int resolve(String host){check(host.equals(HOST),"configured host only");return 2;}
  public void cancel(){cancelled=true;}
  public EmbyHealthCheck.Response request(String method,String url,Map<String,String> h,String request)throws Exception{
   calls++;check(!url.contains("Sessions/")&&!url.contains("Authenticate"),"diagnosis does not create playing reports or refresh login");
   if(url.endsWith("/System/Info/Public")){check(!h.containsKey("X-Emby-Token"),"public endpoint has no token header");return new EmbyHealthCheck.Response(200,"{\"Version\":\"4.9\"}");}
   check(h.get("X-Emby-Token").equals(SECRET),"authenticated API uses existing token");
   if(url.endsWith("/Views"))return new EmbyHealthCheck.Response(authFailed?401:200,authFailed?"secret server body "+SECRET:"{\"Items\":[{\"Id\":\"lib-secret\",\"Name\":\"Movies\",\"CollectionType\":\"movies\"}]}");
   if(url.contains("/Items?"))return new EmbyHealthCheck.Response(200,empty?"{\"Items\":[]}":"{\"Items\":[{\"Id\":\""+FILM+"\"}]}");
   check(method.equals("POST")&&url.contains("/PlaybackInfo?"),"only metadata POST is allowed");posts++;body=request;JSONObject b=new JSONObject(request);check(!b.getBoolean("IsPlayback")&&!b.getBoolean("AutoOpenLiveStream")&&!b.getBoolean("EnableTranscoding"),"no live opening or transcoding requested");
   String source=transcode?"{\"Id\":\"media-secret\",\"TranscodingUrl\":\"/master.m3u8\"}":"{\"Id\":\"media-secret\",\"SupportsDirectStream\":true,\"DirectStreamUrl\":\"/video.mp4?static=true\"}";
   return new EmbyHealthCheck.Response(200,"{\"MediaSources\":["+source+"]}");
  }
 }
 static EmbyHealthCheck run(Fake f,boolean media)throws Exception{EmbyHealthCheck c=new EmbyHealthCheck(server(),"test-device",f,url->{check(!f.transcode,"transcoding URL must not be fetched");return new LinkProbe.Result(media,!media,media?"MP4":"HTTP 403",SECRET+" "+HOST);});c.run(null);return c;}
 public static void main(String[] args)throws Exception{
  Fake ok=new Fake();JSONObject original=server();String before=original.toString();EmbyHealthCheck all=new EmbyHealthCheck(original,"test-device",ok,url->new LinkProbe.Result(true,false,"MP4",""));all.run(null);for(EmbyHealthCheck.Row row:all.rows)check(row.state==EmbyHealthCheck.PASS,"all five stages pass");check(before.equals(original.toString()),"account snapshot is never written back");
  String report=all.report();for(String secret:new String[]{SECRET,HOST,USER,FILM,"media-secret","lib-secret","private-user","private-password","test-device"})check(!report.contains(secret),"redacted report excludes "+secret);
  Fake auth=new Fake();auth.authFailed=true;EmbyHealthCheck denied=run(auth,true);check(denied.rows[2].state==EmbyHealthCheck.FAIL&&denied.rows[3].state==EmbyHealthCheck.SKIP&&auth.posts==0,"401 skips media requests without reauth");check(!denied.report().contains(SECRET),"error bodies never enter report");
  Fake empty=new Fake();empty.empty=true;EmbyHealthCheck noSample=run(empty,true);check(noSample.rows[3].state==EmbyHealthCheck.SKIP&&noSample.rows[4].state==EmbyHealthCheck.SKIP&&empty.posts==0,"empty library is skipped, never reported as video success");
  EmbyHealthCheck badMedia=run(new Fake(),false);check(badMedia.rows[4].state==EmbyHealthCheck.FAIL&&!badMedia.report().contains(SECRET)&&badMedia.report().contains("HTTP 403"),"bad video gives a safe HTTP result");
  Fake trans=new Fake();trans.transcode=true;EmbyHealthCheck requires=run(trans,true);check(requires.rows[3].state==EmbyHealthCheck.PASS&&requires.rows[4].state==EmbyHealthCheck.SKIP,"transcoding metadata does not start a transcode");
  Fake stopped=new Fake();EmbyHealthCheck cancel=new EmbyHealthCheck(server(),"test-device",stopped,url->{throw new AssertionError("cancelled probe ran");});cancel.cancel();cancel.run(null);check(stopped.calls==0&&cancel.rows[0].state==EmbyHealthCheck.SKIP,"cancel before start performs no API requests");
  System.out.println("PASS: five-stage health check, bounded scope, no login/session writes, cancellation and credential-free reports.");
 }
}
