package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** 0.1.106 七项新功能的纯逻辑：备份、追更、离线下载、检查更新、Emby 转码兜底、设置分组。 */
public class SevenFeaturesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  // ---- 备份 ----
  Map<String,Object> prefs=new HashMap<>();
  prefs.put("favorites","[{\"id\":\"a\",\"name\":\"甲\"}]");prefs.put("history","[{\"id\":\"h\"}]");
  prefs.put("emby.servers","[{\"id\":\"s1\",\"url\":\"https://e.example\",\"token\":\"T\",\"password\":\"P\",\"username\":\"u\"}]");
  prefs.put("whisper.token","W");prefs.put("subtitle.assrtToken","A");prefs.put("fusion.catalog.v3:x","{}");prefs.put("cinema.meta.movie.tt1","{}");prefs.put("cinema.hero.v2.emby","{}");prefs.put("emby.deviceId","D");
  prefs.put("sourceTimeout",30);prefs.put("accelerate.v1",true);prefs.put("speed",1.25f);prefs.put("big",5L);prefs.put("set",new HashSet<>(Arrays.asList("b","a")));
  JSONObject full=BackupRules.encode(prefs,true),lite=BackupRules.encode(prefs,false);
  check(full.has("whisper.token")&&full.optJSONObject("emby.servers").optString("v").contains("\"token\""),"full backup keeps secrets");
  check(!lite.has("whisper.token")&&!lite.has("subtitle.assrtToken"),"lite backup drops secret keys");
  String liteServers=lite.optJSONObject("emby.servers").optString("v");check(!liteServers.contains("\"token\"")&&!liteServers.contains("\"password\"")&&liteServers.contains("\"username\""),"lite backup strips nested token/password only");
  check(!full.has("fusion.catalog.v3:x")&&!full.has("cinema.meta.movie.tt1")&&!full.has("cinema.hero.v2.emby")&&!full.has("emby.deviceId"),"caches and device id skipped");
  check("i".equals(full.optJSONObject("sourceTimeout").optString("t"))&&"b".equals(full.optJSONObject("accelerate.v1").optString("t"))&&"f".equals(full.optJSONObject("speed").optString("t"))&&"l".equals(full.optJSONObject("big").optString("t"))&&"ss".equals(full.optJSONObject("set").optString("t")),"types preserved");
  Map<String,JSONObject> sec=new HashMap<>();sec.put("data",lite);JSONObject root=BackupRules.wrap("0.1.106",1L,false,sec);
  JSONObject opened=BackupRules.open(root.toString());String sum=BackupRules.summary(opened);check(sum.contains("Emby 服务器 1")&&sum.contains("收藏 1")&&sum.contains("观看记录 1")&&sum.contains("不含账号"),"summary: "+sum);
  boolean bad=false;try{BackupRules.open("{\"app\":\"other\"}");}catch(IllegalArgumentException e){bad=e.getMessage().contains("不是随影");}check(bad,"foreign file rejected");
  bad=false;try{BackupRules.open("not json");}catch(IllegalArgumentException e){bad=true;}check(bad,"garbage rejected");
  String merged=BackupRules.mergeSecrets(liteServers,"[{\"id\":\"s1\",\"token\":\"OLD\",\"password\":\"PW\"},{\"id\":\"s2\",\"token\":\"X\"}]");
  JSONArray m=new JSONArray(merged);check(m.length()==1&&"OLD".equals(m.getJSONObject(0).optString("token"))&&"PW".equals(m.getJSONObject(0).optString("password")),"restoring lite backup keeps phone's login");
  check(BackupRules.fileName("0.1.106",0).endsWith("-v0.1.106.json"),"file name");
  // ---- 收藏与追更 ----
  JSONObject series=new JSONObject().put("id","emby:s1:x").put("type","series").put("emby.server","s1").put("emby.item","x").put("emby.type","Series").put("emby.resumeMs",5000).put("emby.directPlay",true);
  JSONObject entry=FollowRules.favoriteEntry(series);check(!entry.has("emby.resumeMs")&&!entry.has("emby.directPlay"),"one-off fields removed");
  JSONArray fav=FollowRules.toggle(new JSONArray(),entry);check(FollowRules.contains(fav,"emby:s1:x"),"added");fav=FollowRules.toggle(fav,entry);check(fav.length()==0,"toggled off");
  check(FollowRules.trackable(entry)&&!FollowRules.trackable(new JSONObject().put("emby.server","s1").put("emby.type","Movie"))&&FollowRules.trackable(new JSONObject().put("fusion.site","k")),"trackable");
  check(FollowRules.fusionCount("第1集$u1#第2集$u2#坏$$$线路2$v")==2&&"第2集".equals(FollowRules.fusionLatest("第1集$u1#第2集$u2$$$x$y")),"fusion counting first route");
  JSONObject st=new JSONObject();check(!FollowRules.record(st,"k",10,"第10集",1),"first sight is baseline");check(!FollowRules.hasNew(st,"k"),"no badge on baseline");
  check(!FollowRules.record(st,"k",10,"第10集",2),"same count no alert");check(FollowRules.record(st,"k",12,"第12集",3)&&FollowRules.hasNew(st,"k")&&FollowRules.newCount(st,"k")==2,"growth alerts");
  check(!FollowRules.record(st,"k",12,"第12集",4),"no repeat alert");FollowRules.seen(st,"k",12);check(!FollowRules.hasNew(st,"k"),"opening detail clears badge");
  check(!FollowRules.record(st,"k",0,"",5),"failed check (0) ignored");
  String[] one=FollowRules.notice(Collections.singletonList(new String[]{"仙逆","第12集"}));check(one[0].equals("仙逆 有更新")&&one[1].equals("更新到 第12集"),"single notice");
  String[] many=FollowRules.notice(Arrays.asList(new String[]{"A",""},new String[]{"B",""},new String[]{"C",""},new String[]{"D",""},new String[]{"E",""}));check(many[0].startsWith("5 部")&&many[1].endsWith("等"),"multi notice");
  check(FollowRules.due(0,10,3)&&!FollowRules.due(1000,1000+3600_000L,3)&&FollowRules.due(1000,1000+3*3600_000L,3)&&FollowRules.due(5000,1000,3),"due");
  // ---- 离线下载 ----
  check(OfflineRules.refuse("https://x/a.mp4?k=1")==null&&OfflineRules.refuse("http://x/Videos/1/stream?static=true&api_key=k")==null,"direct ok");
  check(OfflineRules.refuse("")!=null&&OfflineRules.refuse("magnet:?xt=urn:btih:abc").contains("磁力")&&OfflineRules.refuse("https://x/index.m3u8").contains("m3u8")&&OfflineRules.refuse("https://x/Videos/1/master.m3u8?a=1").contains("m3u8")&&OfflineRules.refuse("ftp://x/a.mp4")!=null,"refusals");
  check(OfflineRules.extension("mkv","https://x/stream").equals("mkv")&&OfflineRules.extension("","https://x/movie.MP4?t=1").equals("mp4")&&OfflineRules.extension("mov,mp4,m4a","x").equals("mov")&&OfflineRules.extension("","https://x/stream").equals("mp4"),"extension");
  check(OfflineRules.fileName(7,"mkv").equals("suiying-7.mkv"),"ascii file name");
  check(OfflineRules.size(1536).equals("2 KB")&&OfflineRules.size(3L*1024*1024*1024).equals("3.00 GB"),"size "+OfflineRules.size(1536));
  check(OfflineRules.line(2,50,200,0).startsWith("下载中 25%")&&OfflineRules.line(8,0,1024*1024,0).startsWith("已下载")&&OfflineRules.line(16,0,0,404).contains("HTTP 404")&&OfflineRules.line(4,0,0,3).contains("Wi-Fi"),"progress lines");
  // ---- 检查更新 ----
  check(UpdateRules.newer("v0.1.107","0.1.106")&&!UpdateRules.newer("v0.1.106","0.1.106")&&!UpdateRules.newer("0.1.99","0.1.106")&&UpdateRules.newer("0.2","0.1.106")&&!UpdateRules.newer("nightly","0.1.106"),"version compare");
  JSONObject rel=new JSONObject().put("tag_name","v0.1.107").put("html_url","https://github.com/r/releases/tag/v0.1.107").put("body","修复").put("assets",new JSONArray().put(new JSONObject().put("name","notes.txt").put("browser_download_url","https://n")).put(new JSONObject().put("name","MediaPocket-0.1.107.apk").put("browser_download_url","https://dl/apk").put("size",33200403)));
  String[] r=UpdateRules.release(rel);check(r!=null&&r[0].equals("0.1.107")&&r[1].equals("https://dl/apk")&&r[2].equals("修复")&&r[3].endsWith("MB"),"release parse");
  check(UpdateRules.release(new JSONObject().put("tag_name","v1").put("prerelease",true))==null,"prerelease skipped");
  check(UpdateRules.release(new JSONObject().put("tag_name","v0.1.108").put("html_url","https://page"))[1].equals("https://page"),"no apk → release page");
  // ---- Emby 转码兜底 ----
  JSONObject stream=new JSONObject().put("url","https://e.example/Videos/i1/stream?static=true&api_key=K").put("emby.item","i1").put("emby.mediaSource","m1").put("emby.playSession","p1").put("emby.direct",true).put("emby.server","s1");
  JSONArray src=EmbyApi.withTranscode(stream,"https://e.example","K","dev");check(src.length()==2,"transcode appended");
  String tu=src.getJSONObject(1).optString("url");check(tu.startsWith("https://e.example/Videos/i1/master.m3u8?")&&tu.contains("VideoCodec=h264")&&tu.contains("AudioCodec=aac")&&tu.contains("MediaSourceId=m1")&&tu.contains("PlaySessionId=p1-tc")&&tu.contains("DeviceId=dev")&&tu.contains("api_key=K"),"transcode url "+tu);
  check(!src.getJSONObject(1).optBoolean("emby.direct",true)&&src.getJSONObject(0).optBoolean("emby.direct"),"flags");
  check(EmbyApi.withTranscode(new JSONObject(src.getJSONObject(1).toString()),"https://e.example","K","dev").length()==1,"already transcoding: no second");
  // ---- 设置分组 ----
  check(SettingsRules.place("备份与恢复")[0]==5&&SettingsRules.place("离线下载 · 2 个")[0]==5&&SettingsRules.place("追剧更新提醒：开（收藏的剧有新集时通知）")[0]==1&&SettingsRules.place("系统媒体控制：开（耳机线控、通知栏和锁屏控制）")[0]==2&&SettingsRules.place("检查更新：当前 0.1.106")[0]==6&&SettingsRules.place("自动检查更新：开（x）")[0]==6,"settings groups");
  check(SettingsRules.toggle("追剧更新提醒：开（收藏的剧有新集时通知）")!=null&&SettingsRules.toggle("检查更新：当前 0.1.106")==null,"toggle detection");
  System.out.println("PASS: backup encode/strip/merge/summary, follow-up baseline/alert/seen, offline refusals/ext/progress, update compare/release, Emby transcode fallback, settings groups.");
 }
}
