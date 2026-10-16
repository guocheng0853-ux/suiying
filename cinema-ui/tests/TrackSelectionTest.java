package com.guocheng.mediapocket;
import org.json.*;
public class TrackSelectionTest {
 static class Track {public int id;Track(int n){id=n;}}
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  check(TrackSelection.contains(new Object[]{new Track(2),null,new Track(5)},5),"available track accepted");
  check(!TrackSelection.contains(new Object[]{new Track(2)},5),"missing track never selected");
  check(!TrackSelection.contains(null,5),"unready decoder retries");
  check(TrackSelection.contains(null,-1),"subtitle disable allowed");
  JSONObject one=new JSONObject().put("url","https://example/a.mkv");JSONObject other=new JSONObject().put("url","https://example/b.mkv");
  check(!TrackSelection.sameSource(one,other),"track selection does not cross sources");
  check(TrackSelection.sameSource(one,new JSONObject(one.toString())),"serialized source keeps selection");
  one.put("mp.audioTrack",2);check(TrackSelection.hasAudio(one)&&!TrackSelection.hasSubtitle(one),"explicit selections distinct");
  String[] labels={"无字幕","日本語","繁体中文","简体中文","English","Commentary 中文"};
  check(TrackSelection.pick(labels,null,"zho","unknown",null)==3,"simplified before traditional");
  check(TrackSelection.pick(labels,null,"zho","en",null)==3,"foreign audio selects chinese");
  check(TrackSelection.pick(labels,null,"zho","zh",null)==TrackSelection.DISABLE,"chinese audio does not force chinese subs");
  check(TrackSelection.pick(labels,null,"zho","yue",null)==TrackSelection.DISABLE,"cantonese audio does not force chinese subs");
  check(TrackSelection.pick(labels,null,"eng","unknown",null)==4,"english preference");
  check(TrackSelection.pick(labels,null,"eng","en",null)==TrackSelection.DISABLE,"english audio does not force english subs");
  check(TrackSelection.pick(labels,null,"off","en",null)==TrackSelection.DISABLE,"explicit off");
  check(TrackSelection.pick(new String[]{"无字幕","日本語"},null,"zho","unknown",null)==TrackSelection.UNCHANGED,"no chinese or english leaves the player alone");
  check(TrackSelection.pick(new String[]{"简体","简体"},new boolean[]{false,true},"zho","en",null)==1,"embedded track beats the same external language");
  check(TrackSelection.score("Forced 简体","zho")>TrackSelection.score("繁体","zho"),"simplified still beats traditional when marked forced");
  check(TrackSelection.pick(new String[]{"Disable","Track 1","Track 2"},null,"zho","unknown",new String[]{"简体中文","English"})==1,"stremio title order fills unnamed tracks");
  String[] langs=TrackSelection.languages("Movie\nChinese Simplified\nChinese Traditional\nEnglish");
  check(langs.length==3&&TrackSelection.script(langs[0]).equals("hans")&&TrackSelection.script(langs[1]).equals("hant")&&TrackSelection.script(langs[2]).equals("en"),"title language order");
  check(TrackSelection.audioLanguage("Movie 国语","auto").equals("zh")&&TrackSelection.audioLanguage("Movie\nChinese Simplified","auto").equals("unknown")&&TrackSelection.audioLanguage("Movie English 中字","auto").equals("en"),"audio hints ignore subtitle lines");
  check(TrackSelection.resolveAudio("unknown","zh",new String[]{"English","国语"},new int[]{1,2},1).equals("zh"),"preferred chinese audio wins once that track exists");
  check(TrackSelection.resolveAudio("zh","zh",new String[]{"English"},new int[]{1},1).equals("en"),"missing preferred audio follows the track that is actually playing");
  check(TrackSelection.rank("简体","zho")<TrackSelection.rank("繁体","zho")&&TrackSelection.rank("English","eng")<TrackSelection.rank("简体","eng")&&TrackSelection.rank("任何","all")==0,"rank prefers simplified, then traditional");
  JSONObject listed=new JSONObject().put("title","Movie\nChinese Simplified\nEnglish").put("subtitles",new JSONArray().put(new JSONObject().put("url","http://a/cht.srt").put("lang","繁体中文")).put(new JSONObject().put("url","http://a/chs.srt").put("lang","简体中文")));
  check(TrackSelection.bestUrl(listed,"zho","en").equals("http://a/chs.srt")&&TrackSelection.bestId(listed,"zho","en")==TrackSelection.UNCHANGED,"external simplified is used before embedded tracks exist");
  listed.put("mp.tracks",new JSONObject().put("subtitles",new JSONArray().put(new JSONObject().put("id",7).put("label","繁体")).put(new JSONObject().put("id",8).put("label","简体中文"))));
  check(TrackSelection.bestId(listed,"zho","en")==8&&TrackSelection.bestUrl(listed,"zho","en").isEmpty(),"source track simplified wins over an external file");
  check(TrackSelection.bestId(listed,"zho","zh")==TrackSelection.DISABLE,"metadata is not applied when chinese audio makes subtitles unnecessary");
  System.out.println("PASS: actual track ID validation, unready decoder, subtitle disable, exact-source selection scope, simplified-over-traditional subtitle match.");
 }
}
