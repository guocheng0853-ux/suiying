package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class CmsApiTest {
 static void check(boolean c,String message){if(!c)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Map<String,String> params=new LinkedHashMap<>();params.put("ac","detail");params.put("wd","归还者 & 第二季");params.put("pg","2");
  String url=CmsApi.url("https://example.org/api.php?ac=list&token=ab%2Bc#unused",params);
  check(!url.contains("ac=list")&&!url.contains("#"),"old action replaced and fragment removed");check(url.contains("token=ab%2Bc"),"original query preserved");check(url.contains("wd=%E5%BD%92")&&url.contains("%26"),"Chinese search and delimiter encoded");
  JSONObject player=CmsApi.player("https://media.example/a/index.m3u8?token=x%2By");check(player.getInt("parse")==0&&player.getString("url").contains("x%2By"),"signed direct HLS forwarded intact");check(player.getJSONObject("header").getString("User-Agent").equals("Mozilla/5.0"),"same UA as segment test");
  for(String value:Arrays.asList("javascript:alert(1)","file:///tmp/a.mp4","https://media.example/watch?id=1","https://user:password@media.example/a.mp4")){boolean rejected=false;try{CmsApi.player(value);}catch(Exception expected){rejected=true;}check(rejected,"non-media address rejected");}
  JSONObject existing=new JSONObject("{\"sites\":[{\"key\":\"xiaoya\",\"ext\":\"private\"}],\"spider\":\"original.jar\"}");JSONObject add=new JSONObject().put("key","ruyi").put("api",CmsApi.MARKER);CmsApi.merge(existing,add);CmsApi.merge(existing,add);check(existing.getJSONArray("sites").length()==2,"idempotent source insertion");check(existing.getJSONArray("sites").getJSONObject(0).getString("ext").equals("private")&&existing.getString("spider").equals("original.jar"),"existing site and runtime retained");
  JSONArray additions=new JSONArray().put(add).put(new JSONObject().put("key","sony"));CmsApi.mergeAll(existing,additions);CmsApi.mergeAll(existing,additions);check(existing.getJSONArray("sites").length()==3,"multiple source merge is idempotent");
  System.out.println("PASS: CMS query construction, Chinese search encoding, signed direct media URLs, invalid URI rejection, browser UA, idempotent addition without replacing existing sources");
  if(args.length>0&&args[0].equals("--live")){
   JSONObject site=new JSONObject().put("ext",new JSONObject().put("api","https://cj.rycjapi.com/api.php/provide/vod"));
   JSONObject home=CmsApi.request(site,"home","","",1);check(home.optJSONArray("class")!=null,"live categories");JSONArray rows=home.getJSONArray("list");check(rows.length()>0,"live directory");JSONObject detail=CmsApi.request(site,"detail",rows.getJSONObject(0).get("vod_id").toString(),"",1);check(detail.getJSONArray("list").length()>0,"live detail");JSONObject m=detail.getJSONArray("list").getJSONObject(0);String group=m.getString("vod_play_url").split("\\$\\$\\$")[0];String first=group.split("#")[0].split("\\$",2)[1];check(CmsApi.player(first).getInt("parse")==0,"live direct player routing");System.out.println("LIVE PASS: category/directory/detail/direct-player request chain; Android playback not tested");
  }
 }
}
