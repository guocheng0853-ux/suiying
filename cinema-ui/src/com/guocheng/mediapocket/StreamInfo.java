package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.regex.*;
/** 从 Stremio 条目的 name/title/description/behaviorHints 中解析画质信息（纯 Java，可单测）。只是文字推断，不代表文件真实参数。 */
final class StreamInfo {
 int height;String hdr="",codec="",audio="",fps="";long bytes;int seeds=-1;String file="";boolean p2p;
 static final Pattern SIZE=Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(TB|GB|GiB|MB|MiB)\\b",Pattern.CASE_INSENSITIVE),SEEDS=Pattern.compile("(?:👤|seeds?|seeders?|做种)\\s*[:：]?\\s*(\\d+)",Pattern.CASE_INSENSITIVE),FPS=Pattern.compile("\\b(\\d{2,3}(?:\\.\\d+)?)\\s*fps\\b",Pattern.CASE_INSENSITIVE);
 static String text(JSONObject s){JSONObject h=s.optJSONObject("behaviorHints");return (s.optString("name")+" "+s.optString("title")+" "+s.optString("description")+" "+(h==null?"":h.optString("filename"))).replace('\n',' ');}
 static StreamInfo parse(JSONObject s){StreamInfo i=new StreamInfo();if(s==null)return i;String t=text(s),u=t.toUpperCase(Locale.ROOT);
  if(has(u,"2160P|\\b4K\\b|\\bUHD\\b"))i.height=2160;else if(has(u,"1440P|\\b2K\\b"))i.height=1440;else if(has(u,"1080P|\\bFHD\\b"))i.height=1080;else if(has(u,"720P"))i.height=720;else if(has(u,"576P|480P|\\bSD\\b|DVDRIP"))i.height=480;
  boolean dv=has(u,"DOLBY ?VISION|\\bDV\\b|\\bDOVI\\b"),hdr10p=has(u,"HDR10\\+|HDR10PLUS"),hdr10=has(u,"HDR10\\b"),hlg=has(u,"\\bHLG\\b"),hdr=has(u,"\\bHDR\\b");
  i.hdr=dv&&(hdr10p||hdr10||hdr)?"DV+HDR":dv?"DV":hdr10p?"HDR10+":hdr10?"HDR10":hlg?"HLG":hdr?"HDR":"";
  i.codec=has(u,"\\bAV1\\b")?"AV1":has(u,"HEVC|X265|H\\.?265")?"HEVC":has(u,"\\bAVC\\b|X264|H\\.?264")?"AVC":has(u,"\\bVP9\\b")?"VP9":"";
  i.audio=has(u,"ATMOS")?"Atmos":has(u,"TRUEHD")?"TrueHD":has(u,"DTS-?HD|DTS:?X")?"DTS-HD":has(u,"\\bDTS\\b")?"DTS":has(u,"DDP|E-?AC-?3|DD\\+")?"DD+":has(u,"\\bAC-?3\\b|\\bDD5")?"AC3":has(u,"\\bAAC\\b")?"AAC":"";
  Matcher f=FPS.matcher(t);if(f.find())i.fps=f.group(1).replaceAll("\\.0+$","");
  JSONObject h=s.optJSONObject("behaviorHints");long hinted=h==null?0:h.optLong("videoSize",0);if(hinted>0)i.bytes=hinted;else{Matcher m=SIZE.matcher(t);if(m.find()){double v=Double.parseDouble(m.group(1).replace(',','.'));String unit=m.group(2).toUpperCase(Locale.ROOT);double mul=unit.equals("GIB")?1073741824d:unit.equals("MIB")?1048576d:unit.startsWith("T")?1e12:unit.startsWith("G")?1e9:1e6;i.bytes=(long)(v*mul);}}
  Matcher sd=SEEDS.matcher(t);if(sd.find())try{i.seeds=Integer.parseInt(sd.group(1));}catch(NumberFormatException ignored){}
  i.file=h==null?"":h.optString("filename");i.p2p=s.has("infoHash")||s.optString("url").startsWith("magnet:");return i;}
 static boolean has(String u,String re){return Pattern.compile(re).matcher(u).find();}
 String resolution(){return height>=2160?"4K":height>0?height+"p":"";}
 /** runtime 形如 "2h 14m"/"134 min"/"134"；返回分钟，未知为 0。 */
 static int minutes(String runtime){if(runtime==null)return 0;Matcher h=Pattern.compile("(\\d+)\\s*h").matcher(runtime),m=Pattern.compile("(\\d+)\\s*m").matcher(runtime);int total=0;boolean any=false;if(h.find()){total+=Integer.parseInt(h.group(1))*60;any=true;}if(m.find()){total+=Integer.parseInt(m.group(1));any=true;}if(!any){Matcher n=Pattern.compile("^\\s*(\\d+)\\s*$").matcher(runtime);if(n.find())total=Integer.parseInt(n.group(1));}return total;}
 /** 估算码率（Mbps），大小或片长未知时返回 0。 */
 double mbps(int minutes){return bytes<=0||minutes<=0?0:bytes*8.0/(minutes*60.0)/1e6;}
 static String size(long b){if(b<=0)return "";return b>=1e9?String.format(Locale.ROOT,"%.1f GB",b/1e9):String.format(Locale.ROOT,"%d MB",Math.round(b/1e6));}
 List<String> tags(int minutes){ArrayList<String> t=new ArrayList<>();for(String x:new String[]{resolution(),hdr,codec,audio,fps.isEmpty()?"":fps+"fps",size(bytes)})if(!x.isEmpty())t.add(x);double r=mbps(minutes);if(r>0)t.add(String.format(Locale.ROOT,"≈%.1f Mbps",r));if(seeds>=0)t.add("做种 "+seeds);return t;}
 /** 筛选：quality=all|4k|1080|720；hdr=all|hdr|dv|sdr；fps=all|60；kind=all|direct|p2p。 */
 boolean matches(String quality,String hdrFilter,String fpsFilter,String kind){
  if(quality.equals("4k")&&height<2160)return false;if(quality.equals("1080")&&height!=1080&&height!=1440)return false;if(quality.equals("720")&&(height==0||height>720))return false;
  if(hdrFilter.equals("hdr")&&hdr.isEmpty())return false;if(hdrFilter.equals("dv")&&!hdr.startsWith("DV"))return false;if(hdrFilter.equals("sdr")&&!hdr.isEmpty())return false;
  if(fpsFilter.equals("60")){double v=0;try{v=Double.parseDouble(fps);}catch(Exception ignored){}if(v<50)return false;}
  if(kind.equals("direct")&&p2p)return false;if(kind.equals("p2p")&&!p2p)return false;return true;}
 /** 推荐分：能播 > 分辨率 > HDR > 做种。 */
 int score(boolean playable){return (playable?1_000_000:0)+height*100+(hdr.isEmpty()?0:50)+Math.min(seeds<0?0:seeds,49);}
 static String magnet(JSONObject s){String h=s.optString("infoHash");return h.isEmpty()?"":"magnet:?xt=urn:btih:"+h;}
 static String link(JSONObject s){String u=s.optString("url");return !u.isEmpty()?u:magnet(s);}
}
