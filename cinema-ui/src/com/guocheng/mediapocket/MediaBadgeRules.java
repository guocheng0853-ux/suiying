package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.122 详情页规格标签（参考 Findroid 的 VideoMetadataParser）：分辨率、HDR / 杜比视界、视频编码、声道、全景声、音频编码、中字、多音轨。
 * 只读 Emby / Jellyfin 返回的 MediaSources[0].MediaStreams，不依赖 Android。
 */
final class MediaBadgeRules {
 private MediaBadgeRules(){}
 static List<String> badges(JSONObject item){
  List<String> out=new ArrayList<>();JSONArray sources=item==null?null:item.optJSONArray("MediaSources");JSONObject source=sources==null?null:sources.optJSONObject(0);if(source==null)return out;
  JSONArray streams=source.optJSONArray("MediaStreams");JSONObject video=null,audio=null;int audioCount=0;boolean chinese=false;
  for(int i=0;streams!=null&&i<streams.length();i++){JSONObject s=streams.optJSONObject(i);if(s==null)continue;String t=s.optString("Type");
   if("Video".equalsIgnoreCase(t)&&video==null)video=s;
   else if("Audio".equalsIgnoreCase(t)){audioCount++;if(audio==null||s.optBoolean("IsDefault"))audio=s;}
   else if("Subtitle".equalsIgnoreCase(t)&&chineseSub(s))chinese=true;}
  if(video!=null){String res=resolution(video.optInt("Width"),video.optInt("Height"));if(!res.isEmpty())out.add(res);
   String kind=DolbyRules.kind(source);if(DolbyRules.P5.equals(kind)||DolbyRules.DV.equals(kind)||DolbyRules.COMPATIBLE.equals(kind))out.add("杜比视界");else if(DolbyRules.HDR.equals(kind))out.add(video.optString("VideoRangeType").toUpperCase(Locale.ROOT).contains("PLUS")?"HDR10+":"HDR");
   String vc=videoCodec(video.optString("Codec"));if(!vc.isEmpty())out.add(vc);}
  if(audio!=null){String ch=channels(audio.optInt("Channels"),audio.optString("ChannelLayout"));if(!ch.isEmpty())out.add(ch);
   String title=(audio.optString("DisplayTitle")+" "+audio.optString("Title")+" "+audio.optString("Profile")).toLowerCase(Locale.ROOT);if(title.contains("atmos"))out.add("全景声");
   String ac=audioCodec(audio.optString("Codec"),audio.optString("Profile"));if(!ac.isEmpty())out.add(ac);}
  if(chinese)out.add("中字");if(audioCount>1)out.add("多音轨");
  return out;
 }
 static String resolution(int w,int h){int s=Math.max(w,h*16/9);if(s<=0)return "";if(s>=3500)return "4K";if(s>=2400)return "2K";if(s>=1800)return "1080p";if(s>=1200)return "720p";return "标清";}
 static String videoCodec(String c){String v=c==null?"":c.toLowerCase(Locale.ROOT);if(v.equals("hevc")||v.equals("h265"))return "HEVC";if(v.equals("h264")||v.equals("avc"))return "H.264";if(v.equals("av1"))return "AV1";if(v.equals("vp9"))return "VP9";if(v.equals("mpeg2video"))return "MPEG-2";return "";}
 static String channels(int n,String layout){String l=layout==null?"":layout.toLowerCase(Locale.ROOT);if(l.startsWith("7.1")||n>=8)return "7.1";if(l.startsWith("5.1")||n==6)return "5.1";if(n==2||l.equals("stereo"))return "立体声";if(n==1||l.equals("mono"))return "单声道";return "";}
 static String audioCodec(String c,String profile){String v=c==null?"":c.toLowerCase(Locale.ROOT),p=profile==null?"":profile.toLowerCase(Locale.ROOT);
  if(v.equals("truehd"))return "TrueHD";if(v.equals("eac3"))return "DD+";if(v.equals("ac3"))return "杜比数字";if(v.startsWith("dts"))return p.contains("ma")||p.contains("hd")?"DTS-HD":"DTS";if(v.equals("flac"))return "FLAC";if(v.equals("aac"))return "AAC";if(v.equals("opus"))return "Opus";return "";}
 static boolean chineseSub(JSONObject s){String all=(s.optString("Language")+" "+s.optString("DisplayTitle")+" "+s.optString("Title")).toLowerCase(Locale.ROOT);return all.contains("chi")||all.contains("zho")||all.contains("zh")||all.contains("中")||all.contains("简")||all.contains("繁")||all.contains("chs")||all.contains("cht");}
 /** 剧集卡片右侧第二行：「45 分钟」「剩 12 分钟」「已看完」。 */
 static String episodeLine(long runtimeTicks,long positionTicks,boolean played){long total=runtimeTicks/600_000_000L;if(played)return total>0?total+" 分钟 · 已看完":"已看完";if(positionTicks>0&&runtimeTicks>positionTicks){long left=Math.max(1,(runtimeTicks-positionTicks)/600_000_000L);return "剩 "+left+" 分钟";}return total>0?total+" 分钟":"";}
 /** 进度条比例 0–1；没看过或看完返回 0。 */
 static float progress(long runtimeTicks,long positionTicks,boolean played){if(played||runtimeTicks<=0||positionTicks<=0)return 0f;return Math.max(0.02f,Math.min(1f,positionTicks/(float)runtimeTicks));}
 /** 「第 3 集 · 标题」；没有集号用原名。 */
 static String episodeTitle(JSONObject ep){int n=ep==null?0:ep.optInt("IndexNumber",0);String name=ep==null?"":ep.optString("Name");if(n>0&&!name.isEmpty()&&!name.matches("(?i)(第\\s*)?0*"+n+"\\s*集?|episode\\s*0*"+n))return "第 "+n+" 集 · "+name;if(n>0)return "第 "+n+" 集";return name.isEmpty()?"剧集":name;}
}
