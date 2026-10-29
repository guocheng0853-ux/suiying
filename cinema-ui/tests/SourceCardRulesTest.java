package com.guocheng.mediapocket;
import org.json.*;
public class SourceCardRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject s(String name,String title,String url)throws Exception{return new JSONObject().put("name",name).put("title",title).put("url",url);}
 static int n=0;
 static JSONObject t(String title)throws Exception{return new JSONObject().put("name","Torrentio").put("title",title).put("infoHash","h"+(n++));}
 public static void main(String[] x)throws Exception{
  JSONArray all=new JSONArray().put(s("A","Movie 1080p x264 2.1 GB","https://a/1.mp4")).put(s("B","Movie 2160p HDR HEVC EAC3 5.1 10 GB","https://a/2.mkv")).put(new JSONObject().put("name","C").put("title","Movie 2160p DV").put("ytId","x"));
  check(SourceCardRules.best(all,"auto")==1,"best playable 4K picked, unplayable 4K DV skipped");
  JSONArray lang=new JSONArray().put(s("A","Movie 1080p English","https://a/1")).put(s("B","Movie 1080p 国语 中字","https://a/2"));
  check(SourceCardRules.best(lang,"zh")==1,"audio preference breaks ties");check(SourceCardRules.best(lang,"en")==0,"english pref");
  JSONArray dv=new JSONArray().put(s("P5","Movie 2160p DV HEVC","https://a/p5.mkv")).put(s("P8","Movie 2160p DV HDR10+ HEVC","https://a/p8.mkv")).put(s("FHD","Movie 1080p x264","https://a/fhd.mp4"));
  check(SourceCardRules.best(dv,"auto")==1,"DV with HDR10 layer stays a top 4K pick");
  JSONArray p5=new JSONArray().put(s("P5","Movie 2160p DoVi HEVC","https://a/p5.mkv")).put(s("FHD","Movie 1080p x264","https://a/fhd.mp4"));
  check(SourceCardRules.best(p5,"auto")==1,"pure DV (likely P5) ranks after any other playable source on VLC");
  check(SourceCardRules.best(new JSONArray().put(s("P5","Movie 2160p DV","https://a/p5.mkv")),"auto")==0,"pure DV still picked when it is the only one");
  JSONArray mis=new JSONArray().put(new JSONObject().put("name","HDR").put("title","Movie 2160p DV HEVC").put("url","https://a/p5.mkv")).put(s("FHD","Movie 1080p x264","https://a/fhd.mp4"));
  check(SourceCardRules.best(mis,"auto")==1,"HDR written only on the addon name still ranks pure DV last");
  JSONArray profile5=new JSONArray().put(s("P5","Movie 2160p Dolby Vision Profile 5 HDR10","https://a/p5.mkv")).put(s("FHD","Movie 1080p x264","https://a/fhd.mp4"));
  check(SourceCardRules.best(profile5,"auto")==1,"explicit profile 5 ranks after any other playable source");
  JSONArray profile8=new JSONArray().put(s("P8","Movie 2160p Dolby Vision Profile 8.1 HEVC","https://a/p8.mkv")).put(s("FHD","Movie 1080p x264","https://a/fhd.mp4"));
  check(SourceCardRules.best(profile8,"auto")==0,"explicit profile 8 stays the top 4K pick");
  String detail=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/DetailChrome.java"));
  check(detail.contains("vlcRisky")&&detail.contains("没有可靠的 HDR10 兜底"),"auto-pick logs a demoted Dolby source");
  check(SourceCardRules.poolSize(0)==1&&SourceCardRules.poolSize(5)==5&&SourceCardRules.poolSize(30)==SourceCardRules.MAX_PARALLEL,"parallel queries match addon count");
  check(SourceCardRules.FETCH_MS<=8000,"one addon waits at most 8 s");
  Object mine=new Object(),other=new Object();check(SourceCardRules.replaceAuto(null,null)&&SourceCardRules.replaceAuto(mine,mine)&&!SourceCardRules.replaceAuto(other,mine),"progressive pick never overrides a manual choice");
  // 0.1.161：种子按做种人数和体积排，不再让人少的 4K / 原盘压过人多的 1080p。
  JSONArray few4k=new JSONArray().put(t("Movie 2160p HDR HEVC 💾 12 GB 👤 15")).put(t("Movie 1080p x264 💾 2.4 GB 👤 400"));
  check(SourceCardRules.best(few4k,"auto")==1,"few-seed 4K torrent loses to a well-seeded 1080p");
  JSONArray healthy4k=new JSONArray().put(t("Movie 2160p HDR HEVC 💾 15 GB 👤 150")).put(t("Movie 1080p x264 💾 2.4 GB 👤 150"));
  check(SourceCardRules.best(healthy4k,"auto")==0,"well-seeded 4K torrent still wins");
  JSONArray remux=new JSONArray().put(t("Movie 2160p REMUX HEVC 💾 62 GB 👤 300")).put(t("Movie 1080p x264 💾 3 GB 👤 200"));
  check(SourceCardRules.best(remux,"auto")==1,"remux-sized torrent is capped at 1080p and loses");
  JSONArray dead=new JSONArray().put(t("Movie 1080p x264 💾 2 GB 👤 0")).put(t("Movie 720p x264 💾 1 GB 👤 50"));
  check(SourceCardRules.best(dead,"auto")==1,"zero-seed torrent ranks after any seeded one");
  check(SourceCardRules.best(new JSONArray().put(t("Movie 1080p x264 👤 0")),"auto")==0,"zero-seed torrent is still picked when alone");
  JSONArray direct=new JSONArray().put(t("Movie 1080p x264 💾 2 GB 👤 2000")).put(s("D","Movie 1080p x264 2 GB","https://a/d.mkv"));
  check(SourceCardRules.best(direct,"auto")==1,"same-tier direct link beats a torrent");
  JSONArray zh=new JSONArray().put(t("Movie 1080p x264 English 👤 60")).put(t("Movie 1080p x264 国语 👤 40"));
  check(SourceCardRules.best(zh,"zh")==1,"audio preference still matters between similarly seeded torrents");
  check(StreamInfo.effectiveCap(5,false)==720&&StreamInfo.effectiveCap(60,false)==2160&&StreamInfo.effectiveCap(500,true)==1080&&StreamInfo.effectiveCap(1,false)==480,"seed caps");
  check(SourceCardRules.best(new JSONArray(),"auto")==-1,"none");check(SourceCardRules.best(null,"auto")==-1,"null");
  check(SourceCardRules.sourceLine(null,"loading",0,false).equals("正在查找片源…"),"loading");check(SourceCardRules.sourceLine(null,"none",0,false).equals("暂无片源"),"none text");check(SourceCardRules.sourceLine(null,"loading",0,true).equals("先选择剧集"),"series without episode");
  JSONObject b=all.getJSONObject(1).put("behaviorHints",new JSONObject().put("filename","Movie.2160p.HDR.mkv"));String line=SourceCardRules.sourceLine(b,"ready",120,false);
  check(line.startsWith("Movie.2160p.HDR.mkv\n")&&line.contains("4K")&&line.contains("HDR")&&line.contains("HEVC"),"name + tags: "+line);
  String audio=SourceCardRules.audioLine(b,"auto","自动");check(!audio.isEmpty()&&!audio.equals("自动"),"audio from metadata: "+audio);
  check(SourceCardRules.audioLine(lang.getJSONObject(1),"zh","国语").startsWith("国语"),"preferred language detected");
  check(SourceCardRules.audioLine(null,"zh","国语 / 普通话").equals("国语 / 普通话"),"pref fallback");
  check(SourceCardRules.audioLine(new JSONObject().put("mp.audioLabel","English EAC3 5.1"),"zh","x").equals("English EAC3 5.1"),"real track wins");
  check(SourceCardRules.subtitleLine(lang.getJSONObject(1),"中文优先").contains("中文字幕"),"embedded zh subs noted");
  check(SourceCardRules.subtitleLine(new JSONObject().put("subtitles",new JSONArray().put(1).put(2)),"中文优先").contains("2 条"),"addon subs counted");
  System.out.println("PASS: source card auto-pick ranking, language preference, loading/none/episode states, audio and subtitle lines.");
 }
}
