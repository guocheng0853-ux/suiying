package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.111 识别杜比视界片源（纯逻辑，不依赖 Android）。
 * 杜比视界 Profile 5 没有 HDR10 / SDR 兜底层，必须用杜比解码器还原颜色；VLC 3 直接按 HEVC 解码就会整体发紫发绿。
 * Profile 8.x（带 HDR10 兜底）用 VLC「硬解+」能正常显示，不需要处理。
 * 依据：Jellyfin 的 VideoRangeType / DvProfile，Emby 4.8 的 ExtendedVideoType / ExtendedVideoSubType，最后看文件名和标题。
 */
final class DolbyRules {
 private DolbyRules(){}
 static final String P5="p5",DV="dv",COMPATIBLE="dv-compatible",HDR="hdr",SDR="sdr";
 /** 一个 MediaSource 的视频类型。DV 表示是杜比视界但看不出 Profile（按可能有问题处理）。 */
 static String kind(JSONObject source){
  if(source==null)return SDR;JSONObject video=null;JSONArray streams=source.optJSONArray("MediaStreams");
  for(int i=0;streams!=null&&i<streams.length();i++){JSONObject s=streams.optJSONObject(i);if(s!=null&&"Video".equalsIgnoreCase(s.optString("Type"))){video=s;break;}}
  if(video!=null){
   int profile=video.optInt("DvProfile",-1);String range=video.optString("VideoRangeType"),ext=video.optString("ExtendedVideoType"),sub=video.optString("ExtendedVideoSubType")+" "+video.optString("ExtendedVideoSubTypeDescription");
   if(profile==5||range.equalsIgnoreCase("DOVI")||profileIn(sub)==5)return P5;
   if(profile>5||range.toUpperCase(Locale.ROOT).startsWith("DOVIWITH")||profileIn(sub)>5)return COMPATIBLE;
   if(ext.equalsIgnoreCase("DolbyVision")||mentionsDv(video.optString("DisplayTitle"))||mentionsDv(video.optString("Title")))return DV;
   String vr=video.optString("VideoRange");if(vr.equalsIgnoreCase("HDR")||range.toUpperCase(Locale.ROOT).startsWith("HDR")||range.equalsIgnoreCase("HLG")||ext.toLowerCase(Locale.ROOT).startsWith("hdr"))return HDR;
  }
  String name=source.optString("Name")+" "+source.optString("Path");if(mentionsDv(name))return DV;
  return SDR;
 }
 /** 「DoviProfile50」「Profile 5」「Dolby Vision Profile 8.1」→ 5 / 8；看不出返回 -1。 */
 static int profileIn(String text){if(text==null)return -1;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(?:dovi\\s*profile|profile)\\s*(\\d)").matcher(text);return m.find()?Integer.parseInt(m.group(1)):-1;}
 /** 文件名 / 标题里写了杜比视界（DV、DoVi、Dolby Vision、杜比视界）。避免把 DVD、DVB 这类词误判。 */
 static boolean mentionsDv(String text){if(text==null||text.isEmpty())return false;String t=text.toLowerCase(Locale.ROOT);if(t.contains("dolby vision")||t.contains("dolby.vision")||t.contains("dolbyvision")||t.contains("dovi")||t.contains("杜比视界"))return true;return java.util.regex.Pattern.compile("(?i)(^|[ ._\\-\\[(])dv([ ._\\-\\])]|$)").matcher(text).find();}
 /** VLC 播放会颜色异常的类型。 */
 static boolean risky(String kind){return P5.equals(kind)||DV.equals(kind);}
 /** 版本名后面的标记。 */
 static String tag(String kind){if(P5.equals(kind))return " · 杜比视界P5";if(DV.equals(kind))return " · 杜比视界";if(COMPATIBLE.equals(kind))return " · 杜比视界(兼容HDR10)";if(HDR.equals(kind))return " · HDR";return "";}
 /** 记住的处理方式：ask 每次问 / system 系统解码 / vlc 仍用 VLC / transcode 服务器转码。 */
 static String mode(String saved){if("external".equals(saved)||"system".equals(saved)||"vlc".equals(saved)||"transcode".equals(saved))return saved;return "ask";}
}
