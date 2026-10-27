package com.guocheng.mediapocket;
public class PlaybackEndRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x){
  long full=49*60000L+30000;
  check(PlaybackEndRules.premature(21000,full,0),"Emby transcode ending 21 s into a 49 min episode is premature even when VLC duration is 0");
  check(!PlaybackEndRules.premature(full-60000,full,0),"ending inside the credits is a real finish");
  check(PlaybackEndRules.premature(full-20*60000L,full,0),"20 min short is premature");
  check(!PlaybackEndRules.premature(full-4*60000L,full,0),"4 min short of a 50 min episode counts as finished (credits)");
  check(PlaybackEndRules.premature(30000,0,0)&&!PlaybackEndRules.premature(10*60000L,0,0),"unknown length: only a very early end is premature");
  check(!PlaybackEndRules.premature(118*60000L,0,120*60000L),"falls back to VLC duration when Emby runtime is missing");
  check(PlaybackEndRules.ticksToMs(29_700_000_000L)==2_970_000L&&PlaybackEndRules.ticksToMs(0)==0,"ticks");
  String t=PlaybackEndRules.prematureText(763000,full);check(t.contains("失败")&&t.contains("12:43")&&t.contains("49:30"),"premature text feeds the failure card: "+t);
  check(PlaybackEndRules.mmss(3723000).equals("1:02:03"),"hours");
  check(PlaybackEndRules.playingWithoutVout(false,false,true,1920,5000),"playing with a sized video track counts as rendered");
  check(!PlaybackEndRules.playingWithoutVout(false,false,true,0,5000)&&!PlaybackEndRules.playingWithoutVout(false,false,false,1920,5000)&&!PlaybackEndRules.playingWithoutVout(true,false,true,1920,5000)&&!PlaybackEndRules.playingWithoutVout(false,true,true,1920,5000),"no false render");
  check(PlaybackEndRules.imageSubtitle("PGSSUB","Chinese")&&PlaybackEndRules.imageSubtitle("","Chinese (默认 PGSSUB) · 内嵌")&&PlaybackEndRules.imageSubtitle("hdmv_pgs_subtitle","")&&PlaybackEndRules.imageSubtitle("dvd_subtitle","")&&PlaybackEndRules.imageSubtitle("","movie.zh.sup"),"image subtitles");
  check(!PlaybackEndRules.imageSubtitle("subrip","English (SUBRIP)")&&!PlaybackEndRules.imageSubtitle("ass","")&&!PlaybackEndRules.imageSubtitle("","Super 字幕组"),"text subtitles stay text");
  check(PlaybackEndRules.pathPattern("/Users/0123456789abcdef0123456789abcdef/HomeSections?api_key=secret").equals("/Users/{id}/HomeSections"),"no ids or query in logs");
  check(PlaybackEndRules.pathPattern("/Items/12345/PlaybackInfo?UserId=x").equals("/Items/{n}/PlaybackInfo"),"numeric ids");
  check(PlaybackEndRules.resetLegacyDecode("software",false)&&!PlaybackEndRules.resetLegacyDecode("software",true)&&!PlaybackEndRules.resetLegacyDecode("hardware",false),"legacy sticky software decode resets once");
  System.out.println("PASS: premature VLC end keeps resume, render without Vout, image subtitles to player, Emby log path pattern, per-playback decoder.");
 }
}
