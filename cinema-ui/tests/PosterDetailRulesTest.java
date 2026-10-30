package com.guocheng.mediapocket;
import org.json.*;
public class PosterDetailRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  check(!PosterDetailRules.wide(719)&&PosterDetailRules.wide(720),"wide starts at 720dp");
  check(PosterDetailRules.duration(1281L*10_000_000L).equals("21分21秒"),"21m21s");
  check(PosterDetailRules.duration(3_600L*10_000_000L).equals("1小时"),"one hour");
  check(PosterDetailRules.duration(0).isEmpty(),"no zero duration");
  check(PosterDetailRules.day("2026-10-28T21:21:00Z").equals("2026/10/28"),"day");
  check(PosterDetailRules.episodeMeta(1,165,"2026/10/28","21分21秒").equals("S1:E165｜第 165 集｜2026/10/28｜21分21秒"),"episode meta");
  check(PosterDetailRules.episodeMeta(0,0,"","").isEmpty(),"empty meta");
  check(PosterDetailRules.tileLabel(165,"第165集").equals("165. 第 165 集"),"tile");
  check(PosterDetailRules.bar(100,40,true)==1f&&PosterDetailRules.bar(100,50,false)==.5f,"played bar is full");
  check(PosterDetailRules.watchedMs(90,100)&&!PosterDetailRules.watchedMs(10,100)&&PosterDetailRules.barMs(20_000,0)==0.15f,"fusion progress");
  check(PosterDetailRules.fileName("D:\\media\\secret\\ep.mkv").equals("ep.mkv")&&PosterDetailRules.fileName("/media/secret/ep.mkv").equals("ep.mkv"),"leaf name");
  check(PosterDetailRules.bytes(656_100_000L).equals("656.1 MB")&&PosterDetailRules.bitrate(4_100_000L).equals("4.1 Mbps"),"size and rate");
  JSONArray streams=new JSONArray()
   .put(new JSONObject().put("Type","Video").put("Width",3840).put("Height",2160).put("Codec","hevc").put("Profile","Main 10").put("AverageFrameRate",23.976).put("BitRate",4100000))
   .put(new JSONObject().put("Type","Audio").put("Codec","aac").put("Profile","LC").put("Channels",2).put("ChannelLayout","stereo").put("DisplayTitle","AAC stereo").put("IsDefault",true))
   .put(new JSONObject().put("Type","Subtitle").put("Codec","ass").put("DisplayTitle","简体").put("IsExternal",false));
  JSONObject item=new JSONObject().put("MediaSources",new JSONArray().put(new JSONObject().put("Name","/media/secret/2160p.WEB-DL.H265.AAC-ADWeb.mkv").put("Path","/media/secret/2160p.WEB-DL.H265.AAC-ADWeb.mkv").put("Container","mkv").put("Size",656100000L).put("Bitrate",4100000).put("MediaStreams",streams)));
  PosterDetailRules.Card card=PosterDetailRules.emby(item,0);
  check(card.videoTitle.equals("2160p.WEB-DL.H265.AAC-ADWeb.mkv"),"title is the file leaf, got "+card.videoTitle);
  check(!card.videoTitle.contains("/media")&&!card.videoSub.contains("/media")&&!card.info.contains("/media")&&!card.info.contains("secret"),"path stays out");
  check(card.videoSub.contains("4K")&&card.videoSub.contains("HEVC")&&card.videoSub.contains("656.1 MB")&&card.videoSub.contains("4.1 Mbps"),"video sub "+card.videoSub);
  check(card.audioTitle.equals("AAC stereo（默认）")&&card.audioSub.equals("音轨"),"audio "+card.audioTitle);
  check(card.info.contains("容器 MKV")&&card.info.contains("H.265 Main 10")&&card.info.contains("3840×2160")&&card.info.contains("23.976 fps")&&card.info.contains("来源 Emby")&&card.info.contains("字幕 内封 ASS"),"info\n"+card.info);
  check(card.sources==1&&card.audios==1,"counts");
  check(PosterDetailRules.emby(new JSONObject(),0).visible()==false&&PosterDetailRules.line("量子","第 165 集").videoTitle.equals("量子"),"empty and fusion line");
  check(PosterDetailRules.names("叶秋，苏运韵、昊天").length==3,"cast split");
  check(PosterDetailRules.episodeMeta(1,65,"谛藏算计层出不穷","2024/11/28","20分54秒").equals("S1:E65｜谛藏算计层出不穷｜2024/11/28｜20分54秒"),"named episode meta");
  check(PosterDetailRules.episodeMeta(1,65,"第65集","2024/11/28","20分54秒").equals("S1:E65｜第 65 集｜2024/11/28｜20分54秒"),"generic episode name stays as 第 N 集");
  JSONArray eps=new JSONArray().put(new JSONObject().put("IndexNumber",165)).put(new JSONObject().put("IndexNumber",65)).put(new JSONObject().put("Name","无集号"));
  check(PosterDetailRules.embyJump(eps,65)==1&&PosterDetailRules.embyJump(eps,1)==-1&&PosterDetailRules.embyJump(eps,165)==0&&PosterDetailRules.embyJump(eps,0)==-1,"emby jump uses IndexNumber only");
  check(PosterDetailRules.fusionEpisodeNumber("S01E65")==65&&PosterDetailRules.fusionEpisodeNumber("第 12 集")==12&&PosterDetailRules.fusionEpisodeNumber("EP07")==7&&PosterDetailRules.fusionEpisodeNumber("08")==8&&PosterDetailRules.fusionEpisodeNumber("12. 标题")==12&&PosterDetailRules.fusionEpisodeNumber("正片")==-1,"fusion episode numbers");
  String[] named=new String[]{"第100集","第101集"};
  check(PosterDetailRules.fusionJump(named,100)==0&&PosterDetailRules.fusionJump(named,101)==1&&PosterDetailRules.fusionJump(named,1)==0&&PosterDetailRules.fusionJump(named,3)==-1,"parsed number wins, else 1-based index");
  String[] plain=new String[]{"正片","花絮"};
  check(PosterDetailRules.fusionJump(plain,2)==1&&PosterDetailRules.fusionJump(plain,3)==-1,"list index when names have no number");
  check(PosterDetailRules.fusionMeta("第65集","2024","更新至65集").equals("第65集｜2024｜更新至65集"),"fusion meta");
  check(PosterDetailRules.fusionStill("第1集","http://a/1.m3u8","http://a/vod.jpg").equals("http://a/vod.jpg"),"no per-episode art keeps vod_pic");
  check(PosterDetailRules.fusionStill("第1集 http://cdn/ep1.jpg","http://a/1.mp4","http://a/vod.jpg").equals("http://cdn/ep1.jpg"),"image in the episode name");
  check(PosterDetailRules.fusionStill("第1集","http://cdn/still.png?x=1","http://a/vod.jpg").equals("http://cdn/still.png?x=1"),"play url that is itself an image");
  check(PosterDetailRules.fusionStill("第2集","http://a/ep.mp4","http://a/vod.jpg").equals("http://a/vod.jpg"),"do not invent a still from the episode number");
  System.out.println("PASS: poster detail rules (wide, duration, episode meta, jump, fusion stills, tracks without paths).");
 }
}
