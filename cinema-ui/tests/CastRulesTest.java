package com.guocheng.mediapocket;
import org.json.*;
public class CastRulesTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static JSONObject source(String url)throws Exception{return new JSONObject().put("url",url);}
 public static void main(String[] args)throws Exception{
  check(CastRules.problem(source("https://media.example/file.mp4?sig=a&expires=1")).isEmpty(),"signed HTTPS URI preserved");check(CastRules.problem(source("http://192.168.1.2/video.mkv")).isEmpty(),"LAN NAS supported");
  for(String url:new String[]{"magnet:?xt=urn:btih:abc","http://127.0.0.1:1234/file","http://localhost/video","http://[::1]/file","http://0.0.0.0/video","file:///private/video","https://user:pass@example.com/file"})check(!CastRules.problem(source(url)).isEmpty(),"phone-only/auth URI not handed to TV: "+url);
  JSONObject protectedSource=source("https://media.example/private.mp4").put("behaviorHints",new JSONObject().put("proxyHeaders",new JSONObject().put("request",new JSONObject().put("Cookie","session=secret"))));check(CastRules.problem(protectedSource).contains("请求头"),"header-dependent source explained instead of fake cast success");
  check(CastRules.mime("https://media/a.M3U8?token=.mp4").equals("application/vnd.apple.mpegurl"),"MIME uses URI path");check(CastRules.mime("https://media/a.mkv").equals("video/x-matroska"),"mkv");
  System.out.println("PASS: cast URI capability checks, phone-only URLs, protected headers, signed links, LAN sources, path-based media type.");
 }
}
