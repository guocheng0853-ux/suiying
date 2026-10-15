package com.guocheng.mediapocket;
import org.json.*;
public class SniffRulesTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  check(SniffRules.isMedia("https://cdn.example.com/hls/abc/index.m3u8"),"m3u8");
  check(SniffRules.isMedia("https://cdn.example.com/v/abc.mp4?sign=1&t=2"),"signed mp4");
  check(SniffRules.isMedia("https://v26.douyinvod.com/obj/tos/xyz/123"),"tos object");
  check(!SniffRules.isMedia("https://cdn.example.com/hls/abc/seg001.ts"),"single TS segment is not a whole video");
  check(!SniffRules.isMedia("https://jx.example.com/?url=https://v.qq.com/x/a.mp4"),"parser wrapper url not media");
  check(!SniffRules.isMedia("https://cdn.example.com/player/app.js?x=.m3u8"),"script not media");
  check(!SniffRules.isMedia("https://cdn.example.com/ads/pre.mp4"),"ad path skipped");
  check(!SniffRules.isMedia("blob:https://x/1"),"blob skipped");
  JSONObject p=SniffRules.parseJson("{\"code\":200,\"url\":\"https://a.example/x.m3u8\",\"header\":{\"Referer\":\"https://r\"}}");
  check(p.getString("url").endsWith("x.m3u8")&&p.getJSONObject("header").getString("Referer").equals("https://r"),"json parser with header");
  p=SniffRules.parseJson("{\"data\":{\"url\":\"https://a.example/y.mp4\"},\"user-agent\":\"UA\"}");
  check(p.getString("url").endsWith("y.mp4")&&p.getJSONObject("header").getString("User-Agent").equals("UA"),"nested data.url and ua");
  try{SniffRules.parseJson("{\"url\":\"\"}");check(false,"empty url must fail");}catch(Exception expected){}
  JSONObject cfg=new JSONObject("{\"parses\":[{\"name\":\"Demo\",\"type\":0,\"url\":\"Demo\"},{\"name\":\"A\",\"type\":1,\"url\":\"https://jx.a/?url=\"},{\"name\":\"B\",\"type\":0,\"url\":\"https://jx.b/?v=\"}]}");
  check(SniffRules.parsers(cfg).size()==2&&SniffRules.parsers(cfg).get(0).optString("name").equals("A"),"parsers skip non-http demo");
  check(SniffRules.parsers(new JSONObject()).isEmpty(),"no parses");
  System.out.println("PASS: sniff media/noise rules, parser JSON url/header extraction, config parses selection.");
 }
}
