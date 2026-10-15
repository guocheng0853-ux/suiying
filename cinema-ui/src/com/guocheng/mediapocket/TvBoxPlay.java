package com.guocheng.mediapocket;
import android.app.Activity;import org.json.*;import java.util.*;
/**
 * 影视仓 player 返回值 → 真正交给播放器的地址，并附带真实链接检查结果。
 * parse=0：直接地址；检查发现是网页时改用网页嗅探。parse=1：网页嗅探。jx=1：依次尝试配置 parses（JSON 接口 / 网页嗅探）。
 * 这里只说明“拿到了返回视频数据的地址”，最终能否解码仍以播放器为准。
 */
final class TvBoxPlay {
 static final class Resolved {final JSONObject stream;final LinkProbe.Result check;final String via;Resolved(JSONObject s,LinkProbe.Result c,String v){stream=s;check=c;via=v;}}
 static JSONObject headers(JSONObject r){JSONObject h=r.optJSONObject("header");if(h==null&&r.opt("header") instanceof String)try{h=new JSONObject(r.optString("header"));}catch(Exception ignored){}return h==null?new JSONObject():h;}
 static Map<String,String> map(JSONObject h){LinkedHashMap<String,String> m=new LinkedHashMap<>();Iterator<String> k=h.keys();while(k.hasNext()){String key=k.next();m.put(key,h.optString(key));}return m;}
 static Resolved resolve(Activity a,FusionCatalog.Session s,String flag,JSONObject r,long budgetMs)throws Exception{
  long deadline=System.currentTimeMillis()+budgetMs;
  String raw=r.optString("url");if(raw.isEmpty())throw new Exception("线路没有返回播放地址");
  String url=TvBoxRules.resolve(s.url,raw);JSONObject h=headers(r);
  int parse=r.optInt("parse",0),jx=r.optInt("jx",0);
  if(jx!=0){
   List<JSONObject> parsers=SniffRules.parsers(s.config);
   if(parsers.isEmpty())throw new Exception("此线路需要 VIP 解析接口，但当前配置没有 parses");
   Exception last=null;
   for(JSONObject p:parsers){
    try{
     String target=SniffRules.parserTarget(p.optString("url"),url);JSONObject ph=p.optJSONObject("ext")==null?null:p.optJSONObject("ext").optJSONObject("header");
     JSONObject got=p.optInt("type",0)==1?SniffRules.parseJson(CinemaCatalogClient.text(target,ph==null?new JSONObject():ph)):WebSniffer.sniff(a,target,ph==null?new JSONObject():ph,Math.min(20000,left(deadline)));
     return checked(s,flag,got.getString("url"),got.getJSONObject("header"),"解析接口 · "+p.optString("name","未命名"),deadline);
    }catch(Exception e){last=e;}
   }
   throw new Exception("解析接口均未返回视频地址："+LinkProbe.reason(last));
  }
  if(parse!=0){JSONObject got=WebSniffer.sniff(a,url,h,Math.min(20000,left(deadline)));return checked(s,flag,got.getString("url"),got.getJSONObject("header"),"网页嗅探",deadline);}
  Resolved direct=checked(s,flag,url,h,"直接地址",deadline);
  if(direct.check.definitive&&direct.check.kind.equals("返回网页")&&left(deadline)>5000){
   try{JSONObject got=WebSniffer.sniff(a,url,h,Math.min(20000,left(deadline)));return checked(s,flag,got.getString("url"),got.getJSONObject("header"),"直接地址是网页 → 网页嗅探",deadline);}catch(Exception ignored){}
  }
  return direct;
 }
 static long left(long deadline){return Math.max(1000,deadline-System.currentTimeMillis());}
 static Resolved checked(FusionCatalog.Session s,String flag,String url,JSONObject h,String via,long deadline){
  LinkProbe.Result check=LinkProbe.probe(url,map(h),(int)Math.min(12000,left(deadline)));
  try{JSONObject stream=new JSONObject().put("url",url).put("name",s.site.optString("name")+(flag==null||flag.isEmpty()?"":" · "+flag)).put("behaviorHints",new JSONObject().put("proxyHeaders",new JSONObject().put("request",h))).put("mediapocket.via",via).put("mediapocket.check",check.summary());return new Resolved(stream,check,via);}catch(JSONException e){throw new RuntimeException(e);}
 }
}
