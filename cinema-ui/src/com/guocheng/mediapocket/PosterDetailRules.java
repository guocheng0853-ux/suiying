package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 详情页方向 A 的文案和断点。不依赖 Android。
 * 宽屏把片源卡放在标题右侧，窄屏叠在按钮下面。技术字段只在片源里真有时才出现，路径不展示。
 */
final class PosterDetailRules {
 private PosterDetailRules(){}
 static final int WIDE_DP=720;
 static boolean wide(int widthDp){return widthDp>=WIDE_DP;}
 /** 第一屏把标题压在海报下半。顶栏高度以下再留一点，避免和返回叠在一起。 */
 static int spacer(int screenPx,int headerPx,boolean wide){int want=Math.round(screenPx*(wide?0.26f:0.16f));int min=Math.max(0,headerPx)+Math.round(Math.max(0,screenPx)*0.02f);return Math.max(min,want);}
 static String join(String sep,String... parts){StringBuilder b=new StringBuilder();if(parts==null)return "";for(String p:parts){if(p==null)continue;String t=p.trim();if(t.isEmpty())continue;if(b.length()>0)b.append(sep);b.append(t);}return b.toString();}
 /** Emby 的 100 纳秒刻度。21 分 21 秒这种长度保留秒。 */
 static String duration(long ticks){if(ticks<=0)return "";long sec=ticks/10_000_000L;if(sec<=0)return "";long h=sec/3600,m=(sec%3600)/60,s=sec%60;if(h>0){String out=h+"小时";if(m>0)out+=m+"分";if(s>0)out+=s+"秒";return out;}if(m>0&&s>0)return m+"分"+s+"秒";if(m>0)return m+"分钟";return s+"秒";}
 /** 2026-10-28 或带时间的同一天，收成 2026/10/28。年份单独的不在这里处理。 */
 static String day(String raw){if(raw==null)return "";String s=raw.trim();if(s.length()>=10&&s.charAt(4)=='-'&&s.charAt(7)=='-')return s.substring(0,10).replace('-','/');return "";}
 static String episodeMeta(int season,int episode,String date,String duration){return episodeMeta(season,episode,"",date,duration);}
 /** 有独立集名时用集名，不再重复「第 N 集」。集名本身就是集号时仍显示「第 N 集」。 */
 static String episodeMeta(int season,int episode,String name,String date,String duration){String code=season>0&&episode>0?"S"+season+":E"+episode:(episode>0?"E"+episode:"");String title=episodeTitle(name,episode);String ep=title.isEmpty()?(episode>0?"第 "+episode+" 集":""):title;return join("｜",code,ep,date,duration);}
 static String episodeTitle(String name,int episode){if(name==null)return "";String t=name.trim();if(t.isEmpty())return "";if(episode>0){String n=String.valueOf(episode);if(t.matches("第\\s*0*"+n+"\\s*集"))return "";if(t.matches("0*"+n))return "";if(t.matches("(?i)S\\d{1,4}\\s*E0*"+n))return "";if(t.matches("(?i)(?:EP|E)\\s*0*"+n))return "";}return t;}
 /**
  * Emby 跳集只认 IndexNumber。没有这个字段、或没有这一集，返回 -1。
  * 不用列表序号顶替，避免把第 1 条当成第 1 集。
  */
 static int embyJump(JSONArray items,int episode){if(items==null||episode<=0)return -1;for(int i=0;i<items.length();i++){JSONObject ep=items.optJSONObject(i);if(ep!=null&&ep.optInt("IndexNumber",-1)==episode)return i;}return -1;}
 /**
  * 影视仓选集名里的集数。优先 SxxEyy，其次「第 N 集」，再是 EP / E 前缀，
  * 纯数字，以及「12. 标题」这种开头的序号。解析不到返回 -1。
  */
 static int fusionEpisodeNumber(String name){if(name==null)return -1;String s=name.trim();if(s.isEmpty())return -1;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)S\\d{1,4}\\s*E(\\d{1,5})").matcher(s);if(m.find())return positive(m.group(1));m=java.util.regex.Pattern.compile("第\\s*(\\d{1,5})\\s*集").matcher(s);if(m.find())return positive(m.group(1));m=java.util.regex.Pattern.compile("(?i)(?:EP|E)\\s*(\\d{1,5})").matcher(s);if(m.find())return positive(m.group(1));if(s.matches("\\d{1,5}"))return positive(s);m=java.util.regex.Pattern.compile("^(\\d{1,5})\\s*[.、:：\\-].*").matcher(s);if(m.find())return positive(m.group(1));return -1;}
 static int positive(String n){try{int v=Integer.parseInt(n);return v>0?v:-1;}catch(Exception e){return -1;}}
 /**
  * 先按选集名解析出的集数命中（同号取第一条）。没有再按 1 起的列表序号。
  * 都对不上返回 -1。
  */
 static int fusionJump(String[] names,int episode){if(names==null||episode<=0)return -1;for(int i=0;i<names.length;i++)if(fusionEpisodeNumber(names[i])==episode)return i;if(episode<=names.length)return episode-1;return -1;}
 /** 标题下面这一行：集名、年份、备注。空的段丢掉。 */
 static String fusionMeta(String episode,String year,String remarks){return join("｜",episode,year,remarks);}
 /**
  * 影视仓通常没有每集图，就用整部 vod_pic。
  * 选集名里单独出现的图片地址，或播放地址本身就是图片时，才当作这一集的图。
  * 不根据集数去猜文件名。
  */
 static String fusionStill(String name,String playUrl,String vodPic){String named=imageIn(name);if(!named.isEmpty())return named;if(isImageUrl(playUrl))return playUrl.trim();return vodPic==null?"":vodPic.trim();}
 static boolean isImageUrl(String raw){if(raw==null)return false;String s=raw.trim().toLowerCase(Locale.ROOT);int q=s.indexOf('?');if(q>=0)s=s.substring(0,q);int h=s.indexOf('#');if(h>=0)s=s.substring(0,h);return s.endsWith(".jpg")||s.endsWith(".jpeg")||s.endsWith(".png")||s.endsWith(".webp")||s.endsWith(".gif");}
 static String imageIn(String text){if(text==null||text.isEmpty())return "";java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)https?://[^\\s\"']+\\.(?:jpg|jpeg|png|webp|gif)(?:\\?[^\\s\"']*)?").matcher(text);return m.find()?m.group():"";}
 static String tileLabel(int index,String name){if(index>0)return index+". 第 "+index+" 集";return name==null||name.trim().isEmpty()?"剧集":name.trim();}
 static float bar(long runtimeTicks,long positionTicks,boolean played){if(played)return 1f;return MediaBadgeRules.progress(runtimeTicks,positionTicks,false);}
 /** 影视仓只有本地进度。看到 90% 算看完；开过但没有时长时给一小节，避免完全空白。 */
 static boolean watchedMs(int pos,int dur){return dur>0&&pos>0&&pos*100L>=dur*90L;}
 static float barMs(int pos,int dur){if(watchedMs(pos,dur))return 1f;if(pos<=10000)return 0f;if(dur<=0)return 0.15f;return Math.max(0.02f,Math.min(0.99f,pos/(float)dur));}
 static String fileName(String name){if(name==null)return "";String t=name.trim();int s=Math.max(t.lastIndexOf('/'),t.lastIndexOf('\\'));return (s>=0?t.substring(s+1):t).trim();}
 static String bytes(long n){if(n<=0)return "";if(n>=1_000_000_000L)return String.format(Locale.ROOT,"%.1f GB",n/1_000_000_000d);if(n>=1_000_000L)return String.format(Locale.ROOT,"%.1f MB",n/1_000_000d);if(n>=1_000L)return String.format(Locale.ROOT,"%.0f KB",n/1_000d);return n+" B";}
 static String bitrate(long bps){if(bps<=0)return "";if(bps>=1_000_000L)return String.format(Locale.ROOT,"%.1f Mbps",bps/1_000_000d);if(bps>=1_000L)return String.format(Locale.ROOT,"%.0f Kbps",bps/1_000d);return bps+" bps";}
 static String[] names(String raw){if(raw==null)return new String[0];ArrayList<String> out=new ArrayList<>();for(String bit:raw.split("[,，、/|]")){String t=bit.trim();if(t.isEmpty()||out.contains(t))continue;out.add(t);if(out.size()==20)break;}return out.toArray(new String[0]);}
 static final class Card {
  final String videoTitle,videoSub,audioTitle,audioSub,info;final int sources,audios;
  Card(String videoTitle,String videoSub,String audioTitle,String audioSub,String info,int sources,int audios){this.videoTitle=videoTitle==null?"":videoTitle;this.videoSub=videoSub==null?"":videoSub;this.audioTitle=audioTitle==null?"":audioTitle;this.audioSub=audioSub==null?"":audioSub;this.info=info==null?"":info;this.sources=sources;this.audios=audios;}
  boolean visible(){return !videoTitle.isEmpty()||!audioTitle.isEmpty();}
 }
 static Card empty(){return new Card("","","","","",0,0);}
 /** 影视仓没有编码时，卡片只显示当前线路名和这一集，不编造码率。 */
 static Card line(String flag,String episode){String title=flag==null?"":flag.trim();if(title.isEmpty())title="线路";String sub=episode==null?"":episode.trim();return new Card(title,sub,"","","",1,0);}
 /** 读 MediaSources[index]。Name 若是路径只留文件名。Path 字段不进入任何一行。 */
 static Card emby(JSONObject item,int sourceIndex){JSONArray sources=item==null?null:item.optJSONArray("MediaSources");if(sources==null||sources.length()==0)return empty();int index=sourceIndex<0||sourceIndex>=sources.length()?0:sourceIndex;JSONObject source=sources.optJSONObject(index);if(source==null)return empty();JSONArray streams=source.optJSONArray("MediaStreams");JSONObject video=null,audio=null;int audioCount=0;ArrayList<JSONObject> subs=new ArrayList<>();
  for(int i=0;streams!=null&&i<streams.length();i++){JSONObject s=streams.optJSONObject(i);if(s==null)continue;String type=s.optString("Type");if("Video".equalsIgnoreCase(type)&&video==null)video=s;else if("Audio".equalsIgnoreCase(type)){audioCount++;if(audio==null||s.optBoolean("IsDefault"))audio=s;}else if("Subtitle".equalsIgnoreCase(type))subs.add(s);}
  String videoTitle=fileName(source.optString("Name"));if(videoTitle.isEmpty()&&video!=null)videoTitle=fileName(video.optString("DisplayTitle"));if(videoTitle.isEmpty())videoTitle="视频";
  String res=video==null?"":MediaBadgeRules.resolution(video.optInt("Width"),video.optInt("Height"));String codec=video==null?"":MediaBadgeRules.videoCodec(video.optString("Codec"));if(codec.isEmpty()&&video!=null)codec=video.optString("Codec").toUpperCase(Locale.ROOT);
  String head=join(" ",res,codec);String sub=join(" / ",head,bytes(source.optLong("Size")),bitrate(source.optLong("Bitrate",video==null?0:video.optLong("BitRate"))));
  String audioTitle="";String audioSub="";if(audio!=null){audioTitle=audio.optString("DisplayTitle").trim();if(audioTitle.isEmpty())audioTitle=join(" ",MediaBadgeRules.audioCodec(audio.optString("Codec"),audio.optString("Profile")),MediaBadgeRules.channels(audio.optInt("Channels"),audio.optString("ChannelLayout")));if(audio.optBoolean("IsDefault")&&!audioTitle.isEmpty()&&!audioTitle.contains("默认"))audioTitle=audioTitle+"（默认）";if(!audioTitle.isEmpty())audioSub="音轨";}
  return new Card(videoTitle,sub,audioTitle,audioSub,info(source,video,audio,subs),sources.length(),audioCount);
 }
 static String info(JSONObject source,JSONObject video,JSONObject audio,List<JSONObject> subs){String container=source.optString("Container").trim();String videoPart=videoText(video);String dims="";String fps="";if(video!=null){int w=video.optInt("Width"),h=video.optInt("Height");if(w>0&&h>0)dims=w+"×"+h;double f=video.optDouble("AverageFrameRate",video.optDouble("RealFrameRate",0));if(f>1)fps=(Math.abs(f-Math.rint(f))<0.001?String.valueOf((int)Math.rint(f)):trimZeros(String.format(Locale.ROOT,"%.3f",f)))+" fps";}
  String line1=join("｜",container.isEmpty()?"":"容器 "+container.toUpperCase(Locale.ROOT),videoPart.isEmpty()?"":"视频 "+videoPart,dims,fps);
  String audioPart="";if(audio!=null){String ac=MediaBadgeRules.audioCodec(audio.optString("Codec"),audio.optString("Profile"));String ch=MediaBadgeRules.channels(audio.optInt("Channels"),audio.optString("ChannelLayout"));audioPart=join(" ",ac,ch);}
  String subPart=subtitleText(subs);String line2=join("｜",audioPart.isEmpty()?"":"音频 "+audioPart,subPart,line1.isEmpty()&&audioPart.isEmpty()&&subPart.isEmpty()?"":"来源 Emby");
  return join("\n",line1,line2);
 }
 static String videoText(JSONObject video){if(video==null)return "";String codec=video.optString("Codec").toLowerCase(Locale.ROOT);String show=codec.equals("hevc")||codec.equals("h265")?"H.265":codec.equals("h264")||codec.equals("avc")?"H.264":MediaBadgeRules.videoCodec(video.optString("Codec"));if(show.isEmpty())show=video.optString("Codec").toUpperCase(Locale.ROOT);String profile=video.optString("Profile").trim();if(!profile.isEmpty()&&profile.length()<=24)show=show.isEmpty()?profile:show+" "+profile;return show;}
 static String subtitleText(List<JSONObject> subs){if(subs==null||subs.isEmpty())return "";JSONObject first=subs.get(0);boolean external=first.optBoolean("IsExternal")||"External".equalsIgnoreCase(first.optString("DeliveryMethod"));String codec=first.optString("Codec").trim();String kind=(external?"外挂":"内封")+(codec.isEmpty()?"":" "+codec.toUpperCase(Locale.ROOT));if(subs.size()>1)kind+=" 等";return "字幕 "+kind;}
 static String trimZeros(String n){int dot=n.indexOf('.');if(dot<0)return n;int end=n.length();while(end>dot+1&&n.charAt(end-1)=='0')end--;if(n.charAt(end-1)=='.')end--;return n.substring(0,end);}
}
