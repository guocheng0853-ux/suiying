package com.guocheng.mediapocket;
import org.json.*;
public class MagnetFilesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  String[] paths={"Show.S01.1080p/Show.S01E01.1080p.mkv","Show.S01.1080p/Show.S01E02.1080p.mkv","Show.S01.1080p/Show.S01E03.1080p.mkv","Show.S01.1080p/Sample/Show.S01E03.sample.mkv","Show.S01.1080p/readme.txt"};
  long[] sizes={2000,2100,2050,30,1};
  check(MagnetFiles.choose(paths,sizes,"第3集","Show 第1季")==2,"第3集 -> S01E03, larger than sample");
  check(MagnetFiles.choose(paths,sizes,"S01E02","")==1,"S01E02 by explicit tag");
  check(MagnetFiles.choose(paths,sizes,"S02E02","")==-1,"different season rejected");
  check(MagnetFiles.choose(paths,sizes,"第9集","")==-1,"missing episode keeps original behaviour");
  check(MagnetFiles.choose(new String[]{"Movie.2024.mkv","info.nfo"},new long[]{9,1},"第1集","")==-1,"single video file untouched");
  String[] cn={"某剧/第01集.mp4","某剧/第02集.mp4","某剧/第10集.mp4"};
  check(MagnetFiles.choose(cn,new long[]{5,5,5},"第10集","")==2,"Chinese episode file names");
  check(MagnetFiles.choose(cn,new long[]{5,5,5},"EP02","")==1,"EP02 matches 第02集");
  // 合集判断
  String h="0123456789abcdef0123456789abcdef01234567";
  check(MagnetFiles.packLikely(h,"Show.S01.COMPLETE.1080p",null),"S01 pack name");
  check(MagnetFiles.packLikely(h,"某剧 全30集",null),"全30集");
  check(!MagnetFiles.packLikely(h,"Show.S01E03.1080p.mkv",null),"single-episode name is not a pack");
  JSONObject detail=new JSONObject().put("vod_play_url","第1集$magnet:?xt=urn:btih:"+h+"#第2集$magnet:?xt=urn:btih:"+h.toUpperCase());
  check(MagnetFiles.packLikely(h,"",detail),"same hash across two episodes");
  JSONObject single=new JSONObject().put("vod_play_url","第1集$magnet:?xt=urn:btih:"+h+"#第2集$magnet:?xt=urn:btih:ffffffffffffffffffffffffffffffffffffffff");
  check(!MagnetFiles.packLikely(h,"",single),"distinct per-episode magnets are not a pack");
  // 磁力名
  MagnetLinks.Info info=MagnetLinks.parse("magnet:?xt=urn:btih:"+h+"&dn=Show.S01.COMPLETE&tr=udp%3A%2F%2Fa%3A1");
  check(info!=null&&info.name.equals("Show.S01.COMPLETE"),"dn parsed: "+(info==null?null:info.name));
  // 竖屏视频方向
  check(PlaybackOrientation.forVideo(1080,1920)==7,"9:16 -> sensor portrait");
  check(PlaybackOrientation.forVideo(1920,1080)==6,"16:9 -> sensor landscape");
  check(PlaybackOrientation.forVideo(1000,1020)==6,"near-square stays landscape");
  check(PlaybackOrientation.forVideo(0,0)==Integer.MIN_VALUE,"unknown size leaves orientation alone");
  check(PlaybackOrientation.remember(7)==-1&&PlaybackOrientation.hostShouldRelease(7),"sensor portrait never leaks to the rest of the app");
  System.out.println("PASS: magnet pack picks the selected episode file (S01E03/第3集/EP02, season-checked, larger than sample), single files and misses keep original behaviour; pack detection by name or shared hash; dn parsed; vertical videos request portrait.");
 }
}
