package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class MediaBadgeRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject item(JSONArray streams)throws Exception{return new JSONObject().put("MediaSources",new JSONArray().put(new JSONObject().put("Id","m").put("MediaStreams",streams)));}
 public static void main(String[] a)throws Exception{
  JSONArray s=new JSONArray()
   .put(new JSONObject().put("Type","Video").put("Width",3840).put("Height",2160).put("Codec","hevc").put("VideoRangeType","DOVIWithHDR10").put("DvProfile",8))
   .put(new JSONObject().put("Type","Audio").put("Codec","truehd").put("Channels",8).put("DisplayTitle","English TrueHD Atmos 7.1").put("IsDefault",true))
   .put(new JSONObject().put("Type","Audio").put("Codec","ac3").put("Channels",6).put("Language","chi"))
   .put(new JSONObject().put("Type","Subtitle").put("Language","chi").put("DisplayTitle","简体中文"));
  List<String> b=MediaBadgeRules.badges(item(s));check(b.equals(Arrays.asList("4K","杜比视界","HEVC","7.1","全景声","TrueHD","中字","多音轨")),"full uhd "+b);
  List<String> h=MediaBadgeRules.badges(item(new JSONArray().put(new JSONObject().put("Type","Video").put("Width",1920).put("Height",800).put("Codec","h264").put("VideoRange","HDR").put("VideoRangeType","HDR10Plus")).put(new JSONObject().put("Type","Audio").put("Codec","eac3").put("Channels",6))));
  check(h.equals(Arrays.asList("1080p","HDR10+","H.264","5.1","DD+")),"scope 2.40 film counts as 1080p: "+h);
  check(MediaBadgeRules.badges(item(new JSONArray().put(new JSONObject().put("Type","Video").put("Width",720).put("Height",480).put("Codec","mpeg2video")).put(new JSONObject().put("Type","Audio").put("Codec","aac").put("Channels",2)))).equals(Arrays.asList("标清","MPEG-2","立体声","AAC")),"sd");
  check(MediaBadgeRules.badges(new JSONObject()).isEmpty()&&MediaBadgeRules.badges(null).isEmpty(),"no sources");
  check(MediaBadgeRules.audioCodec("dts","DTS-HD MA").equals("DTS-HD")&&MediaBadgeRules.audioCodec("dts","").equals("DTS"),"dts");
  check(MediaBadgeRules.episodeLine(27_000_000_000L,0,false).equals("45 分钟")&&MediaBadgeRules.episodeLine(27_000_000_000L,19_800_000_000L,false).equals("剩 12 分钟")&&MediaBadgeRules.episodeLine(27_000_000_000L,0,true).equals("45 分钟 · 已看完"),"episode line");
  check(MediaBadgeRules.progress(100,50,false)==.5f&&MediaBadgeRules.progress(100,50,true)==0f&&MediaBadgeRules.progress(100,1,false)==.02f,"progress");
  check(MediaBadgeRules.episodeTitle(new JSONObject().put("IndexNumber",3).put("Name","重逢")).equals("第 3 集 · 重逢"),"titled");
  check(MediaBadgeRules.episodeTitle(new JSONObject().put("IndexNumber",3).put("Name","第3集")).equals("第 3 集"),"no duplicate number");
  check(MediaBadgeRules.episodeTitle(new JSONObject().put("IndexNumber",3).put("Name","Episode 3")).equals("第 3 集"),"english duplicate");
  check(MediaBadgeRules.episodeTitle(new JSONObject().put("Name","特别篇")).equals("特别篇"),"no index");
  System.out.println("PASS: media badges (resolution incl. scope, DV/HDR10+, codecs, channels, Atmos, 中字, 多音轨), episode lines, progress and titles.");
 }
}
