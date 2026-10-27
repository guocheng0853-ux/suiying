package com.guocheng.mediapocket;
import org.json.*;
public class SourceCardRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject s(String name,String title,String url)throws Exception{return new JSONObject().put("name",name).put("title",title).put("url",url);}
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
  check(SourceCardRules.poolSize(0)==1&&SourceCardRules.poolSize(5)==5&&SourceCardRules.poolSize(30)==SourceCardRules.MAX_PARALLEL,"parallel queries match addon count");
  check(SourceCardRules.FETCH_MS<=8000,"one addon waits at most 8 s");
  Object mine=new Object(),other=new Object();check(SourceCardRules.replaceAuto(null,null)&&SourceCardRules.replaceAuto(mine,mine)&&!SourceCardRules.replaceAuto(other,mine),"progressive pick never overrides a manual choice");
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
