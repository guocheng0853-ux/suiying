package com.guocheng.mediapocket;
import org.json.*;
public class StreamInfoTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  StreamInfo i=StreamInfo.parse(new JSONObject().put("name","Addon\n4K").put("title","Movie.2024.2160p.UHD.BluRay.DV.HDR10.HEVC.TrueHD.Atmos\n💾 10.3 GB 👤 42"));
  check(i.height==2160&&i.resolution().equals("4K"),"4k");check(i.hdr.equals("DV+HDR"),"dv+hdr "+i.hdr);check(i.codec.equals("HEVC"),"hevc");check(i.audio.equals("Atmos"),"atmos");check(i.bytes==10_300_000_000L,"size");check(i.seeds==42,"seeds");
  check(Math.abs(i.mbps(StreamInfo.minutes("1h 13m"))-18.8)<0.2,"bitrate "+i.mbps(73));check(i.tags(73).contains("≈18.8 Mbps")&&i.tags(73).contains("10.3 GB"),"tags "+i.tags(73));
  StreamInfo j=StreamInfo.parse(new JSONObject().put("name","Direct").put("title","show 1080p WEB-DL x264 60fps AAC").put("url","https://x/y.mkv").put("behaviorHints",new JSONObject().put("videoSize",2_500_000_000L).put("filename","show.mkv")));
  check(j.height==1080&&j.codec.equals("AVC")&&j.fps.equals("60")&&j.bytes==2_500_000_000L&&j.file.equals("show.mkv")&&!j.p2p,"1080 direct");
  check(StreamInfo.parse(new JSONObject().put("title","x 720p HDR10+")).hdr.equals("HDR10+"),"hdr10+");check(StreamInfo.parse(new JSONObject().put("title","plain")).tags(0).isEmpty(),"no tags");
  check(i.matches("4k","dv","all","all")&&!i.matches("1080","all","all","all")&&!i.matches("all","sdr","all","all"),"filters i");
  check(j.matches("1080","sdr","60","direct")&&!j.matches("all","all","all","p2p")&&!j.matches("4k","all","all","all"),"filters j");
  StreamInfo p=StreamInfo.parse(new JSONObject().put("infoHash","abc"));check(p.p2p&&StreamInfo.link(new JSONObject().put("infoHash","abc")).equals("magnet:?xt=urn:btih:abc"),"magnet");
  check(StreamInfo.minutes("134 min")==134&&StreamInfo.minutes("134")==134&&StreamInfo.minutes("")==0,"minutes");check(i.score(true)>j.score(true)&&j.score(true)>i.score(false),"score");
  check(StreamInfo.parse(new JSONObject().put("title","2 GiB")).bytes==2147483648L,"binary GiB size");
  JSONObject strayHdr=new JSONObject().put("name","HDR Streams").put("title","Movie.2024.2160p.DV.HEVC");
  StreamInfo pure=StreamInfo.parse(strayHdr);check("DV".equals(pure.hdr)&&pure.dvProfile<0,"addon name HDR does not invent a base layer: "+pure.hdr);
  JSONObject strayDv=new JSONObject().put("name","DV").put("title","Movie.2024.2160p.HDR10.HEVC");
  StreamInfo hdr10=StreamInfo.parse(strayDv);check("HDR10".equals(hdr10.hdr)&&!SourceCardRules.vlcRisky(hdr10),"addon name DV does not demote an HDR10 release");
  StreamInfo p5=StreamInfo.parse(new JSONObject().put("title","Movie Dolby Vision Profile 5 HDR10 HEVC"));check(p5.dvProfile==5&&"DV".equals(p5.hdr)&&SourceCardRules.vlcRisky(p5),"profile 5 stays risky even if the title also says HDR10");
  StreamInfo p8=StreamInfo.parse(new JSONObject().put("title","Movie Dolby Vision Profile 8.1 HEVC"));check(p8.dvProfile==8&&"DV+HDR".equals(p8.hdr)&&!SourceCardRules.vlcRisky(p8),"profile 8 is a real fallback, not pure DV");
  StreamInfo zh=StreamInfo.parse(new JSONObject().put("title","电影 2160p 杜比视界 HEVC"));check("DV".equals(zh.hdr)&&SourceCardRules.vlcRisky(zh),"Chinese Dolby Vision without HDR is pure DV");
  StreamInfo both=StreamInfo.parse(new JSONObject().put("title","Movie 2160p DV HDR HEVC"));check("DV+HDR".equals(both.hdr)&&!SourceCardRules.vlcRisky(both),"DV HDR on the release line stays a fallback");
  StreamInfo dvd=StreamInfo.parse(new JSONObject().put("title","Movie DVDRip x264"));check(dvd.hdr.isEmpty()&&!SourceCardRules.vlcRisky(dvd),"DVDRip is not Dolby Vision");
  StreamInfo avcProfile=StreamInfo.parse(new JSONObject().put("title","Movie 1080p x264 High Profile 5.1"));check(avcProfile.dvProfile<0&&avcProfile.hdr.isEmpty(),"High Profile 5.1 is not Dolby Profile 5");
  System.out.println("PASS: stream title parsing (resolution/HDR/DV/codec/audio/fps/size/seeds), bitrate estimate, filters, ranking, magnet link.");}
}
