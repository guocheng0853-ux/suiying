package com.guocheng.mediapocket;
import org.json.*;
public class HistoryDedupeTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject tv(String id,String video,String ep)throws Exception{return new JSONObject().put("id",id).put("name","蜘蛛侠 · "+ep).put("fusion.config","http://cfg").put("fusion.site","site1").put("fusion.video",video).put("lastEpisode",ep);}
 public static void main(String[] a)throws Exception{
  JSONArray h=new JSONArray().put(tv("tvbox:aaa","v1","2160p")).put(tv("tvbox:bbb","v1","1080p")).put(tv("tvbox:ccc","v2","第1集"))
   .put(new JSONObject().put("id","emby:s:e2").put("emby.server","s").put("emby.series","show").put("emby.item","e2"))
   .put(new JSONObject().put("id","emby:s:e1").put("emby.server","s").put("emby.series","show").put("emby.item","e1"))
   .put(new JSONObject().put("id","emby:s:m1").put("emby.server","s").put("emby.item","m1"))
   .put(new JSONObject().put("id","tt123").put("type","movie")).put(new JSONObject().put("id","tt123").put("type","movie"))
   .put(new JSONObject().put("id","tvbox:ddd").put("fusion.site","site2").put("fusion.video","v1"));
  JSONArray d=HomeRules.dedupeHistory(h);
  check(d.length()==6,"expected 6, got "+d.length()+" "+d);
  check(d.getJSONObject(0).getString("lastEpisode").equals("2160p"),"keeps most recent version of the same tvbox title");
  check(d.getJSONObject(1).getString("fusion.video").equals("v2"),"different vod_id stays");
  check(d.getJSONObject(2).getString("emby.item").equals("e2"),"emby series collapses to latest episode");
  check(d.getJSONObject(3).getString("emby.item").equals("m1"),"emby movie kept");
  check(d.getJSONObject(4).getString("id").equals("tt123"),"stremio id dedup");
  check(d.getJSONObject(5).getString("fusion.site").equals("site2"),"same vod_id on another site is a different entry");
  check(HomeRules.dedupeHistory(null).length()==0,"null safe");
  System.out.println("PASS: continue-watching keeps one entry per title (TVBox config+site+vod_id, Emby series/item, else id), newest first.");
 }
}
