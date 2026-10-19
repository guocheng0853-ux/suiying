package com.guocheng.mediapocket;
import org.json.*;import java.util.regex.*;
/** Presentation and navigation rules shared by all playback providers. */
final class PlaybackReviewRules {
 static boolean series(JSONObject meta,String activityType){
  String explicit=meta==null?"":meta.optString("fusion.mediaType");
  if(explicit.isEmpty())explicit=activityType==null?"":activityType;
  if(explicit.isEmpty()&&meta!=null)explicit=meta.optString("type");
  return "series".equalsIgnoreCase(explicit)||"Episode".equalsIgnoreCase(explicit)||"Season".equalsIgnoreCase(explicit);
 }
 static String listTitle(boolean series){return series?"选集":"版本";}
 static String count(boolean series,int n){return "共 "+n+(series?" 集":" 个版本");}
 static String version(JSONObject video){
  if(video==null)return "版本";
  String title=video.optString("title",video.optString("name",""));
  String blob=title+" "+video.optString("resolution")+" "+video.optString("quality");
  Matcher m=Pattern.compile("(?i)(2160p|1080p|720p|480p|4K)").matcher(blob);
  String quality=m.find()?m.group(1).toUpperCase(java.util.Locale.ROOT):"";
  if(blob.toLowerCase(java.util.Locale.ROOT).contains("dolby vision"))quality+=" · Dolby Vision";
  else if(blob.toUpperCase(java.util.Locale.ROOT).contains("HDR"))quality+=" · HDR";
  return quality.isEmpty()?(title.isEmpty()?"版本":title):quality.replaceAll("^ · ","");
 }
 static boolean failure(boolean rendered,boolean terminal,String raw){
  String s=raw==null?"":raw.toLowerCase(java.util.Locale.ROOT);
  return terminal&&(!rendered||s.contains("失败")||s.contains("超时")||s.contains("无法")||s.contains("error"));
 }
 static String failureMessage(String raw){String s=raw==null?"":raw;if(s.contains("解码")||s.contains("格式"))return "这个视频暂时无法播放，可以换个片源试试";return "没连上这个片源，可以换一个试试";}
 static String loading(String raw){
  Matcher m=Pattern.compile("(?:缓冲|buffer)[^0-9]{0,8}([0-9]{1,3})%",Pattern.CASE_INSENSITIVE).matcher(raw==null?"":raw);
  return m.find()?"正在连接 · 已缓冲 "+Math.min(100,Integer.parseInt(m.group(1)))+"%":"正在连接…";
 }
}
