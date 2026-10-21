package com.guocheng.mediapocket;
import org.json.*;
public class DolbyRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject src(JSONObject video,String name)throws Exception{return new JSONObject().put("Id","m").put("Name",name).put("MediaStreams",new JSONArray().put(new JSONObject().put("Type","Audio")).put(video.put("Type","Video")));}
 public static void main(String[] a)throws Exception{
  check(DolbyRules.kind(src(new JSONObject().put("VideoRangeType","DOVI").put("DvProfile",5),"x")).equals("p5"),"jellyfin P5");
  check(DolbyRules.kind(src(new JSONObject().put("VideoRangeType","DOVIWithHDR10").put("DvProfile",8),"x")).equals("dv-compatible"),"jellyfin P8.1");
  check(DolbyRules.kind(src(new JSONObject().put("ExtendedVideoType","DolbyVision").put("ExtendedVideoSubType","DoviProfile50"),"x")).equals("p5"),"emby P5");
  check(DolbyRules.kind(src(new JSONObject().put("ExtendedVideoType","DolbyVision").put("ExtendedVideoSubType","DoviProfile81"),"x")).equals("dv-compatible"),"emby P8.1");
  check(DolbyRules.kind(src(new JSONObject().put("ExtendedVideoType","DolbyVision"),"x")).equals("dv"),"emby DV unknown profile");
  check(DolbyRules.kind(src(new JSONObject().put("VideoRange","HDR").put("VideoRangeType","HDR10"),"x")).equals("hdr"),"hdr10");
  check(DolbyRules.kind(src(new JSONObject().put("VideoRange","SDR"),"Where.the.Mask.Ends.S01E01.2160p.WEB-DL.DV.HEVC")).equals("dv"),"file name DV");
  check(DolbyRules.kind(src(new JSONObject().put("VideoRange","SDR"),"Movie.DVDRip.x264")).equals("sdr")&&DolbyRules.kind(src(new JSONObject(),"DVB capture")).equals("sdr"),"DVD/DVB not DV");
  check(DolbyRules.kind(src(new JSONObject().put("DvProfile",8).put("VideoRangeType","DOVI"),"x")).equals("dv-compatible"),"explicit profile wins over generic range");
  check(DolbyRules.kind(null).equals("sdr")&&DolbyRules.kind(new JSONObject()).equals("sdr"),"empty");
  check(DolbyRules.risky("p5")&&DolbyRules.risky("dv")&&!DolbyRules.risky("dv-compatible")&&!DolbyRules.risky("hdr"),"risky");
  check(DolbyRules.profileIn("Dolby Vision Profile 8.1")==8&&DolbyRules.profileIn("DoviProfile50")==5&&DolbyRules.profileIn("none")==-1,"profile parse");
  check(DolbyRules.mode("external").equals("external")&&DolbyRules.mode("system").equals("system")&&DolbyRules.mode("weird").equals("ask")&&DolbyRules.mode(null).equals("ask"),"mode");
  check(DolbyRules.tag("p5").contains("P5")&&DolbyRules.tag("sdr").isEmpty(),"tags");
  JSONArray v=EmbyCue.versions(new JSONArray().put(src(new JSONObject().put("VideoRangeType","DOVI"),"2160p DV").put("Container","mkv")).put(new JSONObject().put("Id","n").put("Name","1080p").put("Container","mp4")));
  check(v.length()==2&&v.getJSONObject(0).optString("name").contains("杜比视界P5")&&!v.getJSONObject(1).optString("name").contains("杜比"),"version labels "+v);
  System.out.println("PASS: Dolby Vision detection (Jellyfin/Emby fields, P5 vs P8, name hints, DVD/DVB not DV), modes and version tags.");
 }
}
