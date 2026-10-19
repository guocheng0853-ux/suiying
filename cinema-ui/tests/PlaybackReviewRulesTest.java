package com.guocheng.mediapocket;
import org.json.*;
public class PlaybackReviewRulesTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  JSONObject film=new JSONObject().put("type","movie").put("videos",new JSONArray().put(new JSONObject().put("id","1080p")).put(new JSONObject().put("id","2160p")));
  check(!PlaybackReviewRules.series(film,"movie"),"two film files do not imply episodes");
  check(PlaybackReviewRules.series(new JSONObject().put("type","movie"),"series"),"TVBox normalized movie type must not override actual playback series type");
  check(!PlaybackReviewRules.series(new JSONObject().put("fusion.mediaType","movie"),"series"),"explicit media classification wins");
  check(PlaybackReviewRules.count(false,2).equals("共 2 个版本"),"movie version count");
  check(PlaybackReviewRules.listTitle(true).equals("选集")&&PlaybackReviewRules.listTitle(false).equals("版本"),"contextual tab");
  check(PlaybackReviewRules.version(new JSONObject().put("title","Film.2160p.WEB-DL.HDR.mkv")).equals("2160P · HDR"),"version quality without bogus episode label");
  check(PlaybackReviewRules.failure(false,true,"播放失败"),"startup failure");
  check(PlaybackReviewRules.failure(true,true,"读取失败"),"mid-playback failure");
  check(!PlaybackReviewRules.failure(true,true,"播放结束"),"normal end is not a failure");
  check(!PlaybackReviewRules.failure(false,false,"等待片段超时，正在重试"),"recoverable wait is not terminal");
  check(PlaybackReviewRules.loading("VLC 缓冲 35% · HTTP · 视频轨 0").equals("正在连接 · 已缓冲 35%"),"only useful progress is visible");
  check(!PlaybackReviewRules.failureMessage("java.io.IOException HTTP 403").contains("IOException"),"no internal exception in user-facing error");
  System.out.println("PASS: film versions, provider classification, failure versus completion, loading feedback.");
 }
}
