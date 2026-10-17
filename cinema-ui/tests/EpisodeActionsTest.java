package com.guocheng.mediapocket;
 import org.json.*;import java.util.*;
public class EpisodeActionsTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  JSONObject a=new JSONObject().put("id","custom-ep-B").put("season",2).put("episode",3).put("title","第三集"),b=new JSONObject().put("id","custom-ep-A").put("season",1).put("episode",2);
  JSONObject meta=new JSONObject().put("videos",new JSONArray().put(a).put(b).put(a).put(new JSONObject()));
  check(EpisodeActions.belongs(meta,"show","custom-ep-B"),"provider-specific episode ID retained");check(!EpisodeActions.belongs(meta,"show","show"),"series ID is not an episode stream ID");check(!EpisodeActions.belongs(meta,"show","other:1:2"),"other show not adopted");
  check(EpisodeActions.belongs(meta,"show","show:1:2"),"standard episode fallback");check(!EpisodeActions.belongs(meta,"show",null),"unselected state safe");check(EpisodeActions.list(meta).size()==2&&EpisodeActions.list(meta).get(0)==b,"deduplicated and ordered by season/episode");
  check(EpisodeActions.label(a).contains("第 2 季 · 第 3 集")&&EpisodeActions.label(a).contains("第三集"),"action UI identifies selected episode");check(!EpisodeActions.key("series","show").equals(EpisodeActions.key("series","other")),"selection isolated per detail");
  JSONObject only=new JSONObject().put("id","e161").put("episode",161).put("title","离乡");check(EpisodeActions.label(only).startsWith("第 161 集")&&!EpisodeActions.label(only).contains("季"),"episode without a season");
  ArrayList<JSONObject> ordered=EpisodeActions.list(meta);check(EpisodeActions.indexOf(ordered,"custom-ep-A")==0&&EpisodeActions.seasons(ordered).size()==2&&EpisodeActions.inSeason(b,1)&&!EpisodeActions.inSeason(a,1)&&EpisodeActions.inSeason(a,-1),"season filter and current index");
  System.out.println("PASS: exact episode ownership, provider-specific IDs, series-ID rejection, dedup/order, selected-episode labels and detail isolation.");
 }
}
