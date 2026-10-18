package com.guocheng.mediapocket;
import org.json.*;
public class ExternalPlayRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(ExternalPlayRules.LABEL.equals("随影 (VLC)"),"player label");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","https://cdn.example/a.mkv","video/x-matroska"),"https video");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","content://media/1","video/mp4"),"content video");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","https://cdn.example/movie.mp4",null),"https file without mime");
  check(!ExternalPlayRules.playable("android.intent.action.VIEW","https://example.com/page",null),"plain page is not a video");
  check(!ExternalPlayRules.playable("android.intent.action.VIEW","file:///etc/passwd","text/plain"),"non-video mime");
  JSONObject stream=ExternalPlayRules.stream("https://cdn.example/a.mp4","示例",new String[]{"User-Agent","suiying","Authorization","secret","Cookie","a=b"});
  check(stream.getString("url").endsWith("a.mp4")&&stream.getJSONObject("header").has("User-Agent")&&!stream.getJSONObject("header").has("Authorization")&&!stream.getJSONObject("header").has("Cookie"),"credential headers stay out");
  check(ExternalPlayRules.title("https://cdn.example/path/%E7%89%87.mkv","").equals("片.mkv"),"title from path");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","file:///storage/emulated/0/Download/我的 视频.mkv","video/x-matroska"),"raw unencoded chinese path with mime");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","file:///storage/emulated/0/电影 第2集.mp4",null),"raw unencoded chinese path without mime");
  check(ExternalPlayRules.playable("android.intent.action.VIEW","content://providers/primary/我的 电影.mp4",null),"raw content uri without mime");
  check(ExternalPlayRules.title("file:///storage/emulated/0/Download/我的 视频.mkv","").equals("我的 视频.mkv"),"title from raw path");
  check(!ExternalPlayRules.playable("android.intent.action.VIEW","file:///storage/emulated/0/doc.txt",null),"raw path without video extension");
  check(ExternalPlayRules.importUrl("suiying://import?url=https%3A%2F%2Fexample.com%2Fa.json").equals("https://example.com/a.json"),"import url");
  check(ExternalPlayRules.importUrl("suiying://import?config=http://192.168.1.2:8080/box.json").startsWith("http://192.168.1.2"),"import config");
  check(ExternalPlayRules.importUrl("suiying://import?url=https://user:pass@example.com/a.json")==null,"import rejects credentials");
  check(ExternalPlayRules.importUrl("suiying://import?url=javascript:alert(1)")==null,"import rejects other schemes");
  check(ExternalPlayRules.importUrl("https://example.com/a.json")==null,"only the suiying scheme imports");
  JSONArray profiles=ExternalPlayRules.addProfile(new JSONArray().put("https://old.example/a.json"),"https://example.com/a.json");
  check(profiles.length()==2&&ExternalPlayRules.addProfile(profiles,"https://example.com/a.json").length()==2,"profile list keeps one copy");
  System.out.println("PASS: external video intent and suiying import url.");
 }
}
