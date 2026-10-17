package com.guocheng.mediapocket;
import org.json.*;
public class HomeRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  check(HomeRules.navAlias("发现").equals("搜索"),"discover->search");
  check(HomeRules.navAlias("插件").equals("我的")&&HomeRules.navAlias("设置").equals("我的"),"settings alias");
  check(HomeRules.navAlias("首页").equals("首页")&&HomeRules.tabIndex("收藏")==1,"tabs");
  check(HomeRules.TABS.length==4&&HomeRules.tabIcon(2)==1,"icons");
  check(HomeRules.progress(0,1000,0)==0f,"no progress");
  check(Math.abs(HomeRules.progress(50000,100000,0)-0.5f)<0.01f,"half");
  check(HomeRules.progress(100,0,0)>=0.02f&&HomeRules.progress(100,0,0)<0.1f,"estimate floor");
  check(HomeRules.progress(9_000_000,100000,0)==0.98f,"cap");
  check(HomeRules.episodeLabel("tt1:1:161").equals("S1E161 · 第161集"),"ep");
  check(HomeRules.episodeLabel("").equals("最近观看")&&HomeRules.episodeLabel("movie").equals("续播"),"ep fallback");
  check(HomeRules.numericBadge("更新至48集").equals("48")&&HomeRules.numericBadge("2024").equals(""),"badge num");
  check(HomeRules.badge(new JSONObject().put("imdbRating","7.9")).startsWith("★"),"rating badge");
  check(HomeRules.badge(new JSONObject().put("vod_remarks","全16集")).equals("16"),"ep badge");
  JSONObject hero=new JSONObject().put("imdbRating","7.9").put("releaseInfo","2004").put("genres",new JSONArray().put("动画").put("喜剧"));
  check(HomeRules.heroFacts(hero).contains("★ 7.9")&&HomeRules.heroFacts(hero).contains("2004")&&HomeRules.heroFacts(hero).contains("动画"),"facts");
  check(HomeRules.heroDescription(new JSONObject().put("description","短")).equals("短"),"desc");
  check(HomeRules.heroDescription(new JSONObject()).contains("点开"),"desc empty");
  JSONArray opts=HomeRules.sourceOptions(new JSONArray().put(new JSONObject().put("id","fty").put("name","公开源")));
  check(opts.length()==5&&opts.getJSONObject(3).getString("id").equals("source:fty")&&opts.getJSONObject(4).getString("label").equals("添加 Emby 服务器")&&opts.getJSONObject(4).getString("id").equals(HomeRules.EMBY_MANAGE),"add emby when none");
  check(HomeRules.selectedId("stremio","current").equals("provider:stremio"),"sources");
  check(HomeRules.selectedId("tvbox","fty").equals("source:fty"),"source selected");
  String[] ap=HomeRules.apply("source:fty");check(ap[0].equals("tvbox")&&ap[1].equals("fty"),"apply source");
  check(HomeRules.apply("provider:fusion")[0].equals("fusion"),"apply fusion");
  check(HomeRules.apply(HomeRules.EMBY_MANAGE)==null&&HomeRules.keepsHomeProvider(HomeRules.EMBY_MANAGE)&&!HomeRules.keepsHomeProvider("provider:emby:a"),"manage keeps provider");
  JSONArray servers=new JSONArray().put(new JSONObject().put("id","a").put("name","客厅")).put(new JSONObject().put("name","无编号")).put(new JSONObject().put("id","b").put("name","卧室"));
  JSONArray with=HomeRules.sourceOptions(new JSONArray(),servers);
  check(with.length()==6&&with.getJSONObject(3).getString("id").equals("provider:emby:a")&&with.getJSONObject(3).getString("label").equals("客厅 · Emby")&&with.getJSONObject(4).getString("id").equals("provider:emby:b")&&with.getJSONObject(5).getString("label").equals("管理 Emby 服务器"),"servers then manage");
  check(HomeRules.embyServersLabel(null).equals("Emby 服务器 · 未添加")&&HomeRules.embyServersLabel(new JSONArray()).equals("Emby 服务器 · 未添加")&&HomeRules.embyServersLabel(servers).equals("Emby 服务器 · 2 个"),"emby count label");
  check(HomeRules.labelFor(opts,"provider:stremio").equals("Stremio 目录"),"label");
  check(HomeRules.libraryTiles().length==4&&HomeRules.libraryTiles()[0][0].equals("电影"),"tiles");
  check(HomeRules.runtimeMinutes(new JSONObject().put("runtime","1h 13m"))==73,"runtime");
  System.out.println("PASS: home nav aliases, progress fraction, episode labels, badges, hero facts, source options, library tiles.");
 }
}
