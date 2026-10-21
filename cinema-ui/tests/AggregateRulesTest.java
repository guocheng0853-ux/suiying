package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public final class AggregateRulesTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a)throws Exception{
  JSONObject emby=new JSONObject().put("name","繁花").put("year","2023").put("emby.server","s1");
  JSONObject tv=new JSONObject().put("name","繁花 第一季").put("fusion.site","x");
  JSONObject st=new JSONObject().put("name","Oppenheimer").put("releaseInfo","2023");
  JSONObject st2=new JSONObject().put("name","繁花").put("releaseInfo","1990");
  check(AggregateRules.source(emby).equals("emby")&&AggregateRules.source(tv).equals("tvbox")&&AggregateRules.source(st).equals("stremio"),"source");
  check(AggregateRules.key("繁花 第一季").equals(AggregateRules.key("繁花"))&&AggregateRules.key("Dune: Part Two").equals("dunepasttwo".replace("pas","par")),"key");
  check(AggregateRules.year(st)==2023&&AggregateRules.year(new JSONObject())==0,"year");
  JSONArray merged=AggregateRules.merge(Arrays.asList(new JSONArray().put(emby),new JSONArray().put(tv).put(new JSONObject().put("name","漫长的季节").put("fusion.site","x")),new JSONArray().put(st).put(st2)),20);
  check(merged.length()==4,"merged count "+merged.length());
  check(merged.getJSONObject(0).optString("agg.source").equals("emby")&&merged.getJSONObject(0).optInt("agg.count")==2,"dedupe keeps emby, counts tvbox");
  check(merged.getJSONObject(1).optString("name").equals("漫长的季节")||merged.getJSONObject(1).optString("name").equals("Oppenheimer"),"round robin");
  boolean old=false;for(int i=0;i<merged.length();i++)if(merged.getJSONObject(i).optString("releaseInfo").equals("1990"))old=true;check(old,"different year not merged");
  check(AggregateRules.merge(Arrays.asList(null,new JSONArray()),20).length()==0,"empty");
  check(AggregateRules.rowForCategory("每日更新").equals("fresh")&&AggregateRules.rowForCategory("国产动漫").equals("anime")&&AggregateRules.rowForCategory("电视剧").equals("series")&&AggregateRules.rowForCategory("电影").equals("movie")&&AggregateRules.rowForCategory("纪录片").equals("doc")&&AggregateRules.rowForCategory("综艺").isEmpty(),"categories");
  JSONArray resume=AggregateRules.resume(new JSONArray().put(new JSONObject().put("id","e1").put("name","三体")),new JSONArray().put(new JSONObject().put("id","tt1").put("name","三体")).put(new JSONObject().put("id","tt2").put("name","奥本海默")));
  check(resume.length()==2&&resume.getJSONObject(0).optString("id").equals("e1"),"resume dedupe");
  check(AggregateRules.stale(0,1000)&&!AggregateRules.stale(1000,2000)&&AggregateRules.stale(0,AggregateRules.FRESH_MS+5),"stale");
  JSONObject dune=new JSONObject().put("name","沙丘").put("year","2021").put("poster","keep").put("emby.server","s");
  JSONObject dune2=new JSONObject().put("name","沙丘").put("year","2021").put("fusion.site","f");
  JSONArray once=AggregateRules.merge(Arrays.asList(new JSONArray().put(dune),new JSONArray().put(dune2)),5);
  check(once.length()==1&&once.getJSONObject(0).optInt("agg.count")==2&&"keep".equals(dune.optString("poster"))&&!dune.has("agg.source"),"dedupe keeps source object, count 2");
  JSONObject copy=AggregateRules.copy(dune);copy.put("poster","");check("keep".equals(dune.optString("poster"))&&"".equals(copy.optString("poster")),"shallow copy");
  List<JSONArray> bench=new ArrayList<>();
  for(int s=0;s<4;s++){JSONArray list=new JSONArray();for(int i=0;i<30;i++)list.put(new JSONObject().put("name","片"+s+"-"+i).put("year",String.valueOf(2000+(i%5))));bench.add(list);}
  long started=System.nanoTime();for(int n=0;n<80;n++)check(AggregateRules.merge(bench,20).length()==20,"bench size");
  long ms=(System.nanoTime()-started)/1000000L;check(ms<1500,"merge 80x4x30 took "+ms+"ms");
  System.out.println("PASS: aggregate home source detection, title key, year guard, round-robin merge with dedupe count, category mapping, resume merge, shallow copy, "+ms+"ms for 80 merges.");
 }
}
