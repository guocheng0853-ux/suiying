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
  System.out.println("PASS: stream title parsing (resolution/HDR/DV/codec/audio/fps/size/seeds), bitrate estimate, filters, ranking, magnet link.");}
}
