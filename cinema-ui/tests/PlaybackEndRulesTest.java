package com.guocheng.mediapocket;
public class PlaybackEndRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x)throws Exception{
  long full=49*60000L+30000;
  check(PlaybackEndRules.premature(21000,full,0),"Emby transcode ending 21 s into a 49 min episode is premature even when VLC duration is 0");
  check(!PlaybackEndRules.premature(full-60000,full,0),"ending inside the credits is a real finish");
  check(PlaybackEndRules.premature(full-20*60000L,full,0),"20 min short is premature");
  check(!PlaybackEndRules.premature(full-4*60000L,full,0),"4 min short of a 50 min episode counts as finished (credits)");
  check(PlaybackEndRules.premature(30000,0,0)&&!PlaybackEndRules.premature(10*60000L,0,0),"unknown length: only a very early end is premature");
  check(PlaybackEndRules.premature(90_000,0,0)&&!PlaybackEndRules.premature(130_000,0,0),"unknown runtime: under 2 min stays premature (a real short clip cannot be told apart); after 2 min stays a natural finish");
  check(!PlaybackEndRules.premature(118*60000L,0,120*60000L),"falls back to VLC duration when Emby runtime is missing");
  check(PlaybackEndRules.endReached(PlaybackEndRules.VLC_ENDED)&&!PlaybackEndRules.endReached(3)&&!PlaybackEndRules.endReached(5)&&!PlaybackEndRules.endReached(7),"only libvlc Ended is EndReached");
  check(PlaybackEndRules.prematureFlag(true,PlaybackEndRules.VLC_ENDED,21000,full,0),"Ended far from Emby runtime sets the premature flag");
  check(!PlaybackEndRules.prematureFlag(true,PlaybackEndRules.VLC_ENDED,full-60000,full,0),"Ended inside the credits does not set the flag");
  check(!PlaybackEndRules.prematureFlag(true,5,21000,full,0),"a stopped player is not the premature path");
  check(!PlaybackEndRules.prematureFlag(false,PlaybackEndRules.VLC_ENDED,21000,full,0),"Ended before the player is terminal does not set the flag");
  check(PlaybackReviewRules.failure(true,true,"本集播放完毕",true),"premature flag opens the failure path without a 失败 substring");
  check(!PlaybackReviewRules.failure(true,true,"本集播放完毕",false),"completion copy alone is not a failure");
  check(PlaybackReviewRules.failure(true,true,"播放结束",true)&&!PlaybackReviewRules.failure(true,true,"播放结束",false),"failure card follows the flag, not the wording");
  check(PlaybackEndRules.naturalFinish(true,false)&&!PlaybackEndRules.naturalFinish(true,true)&&!PlaybackEndRules.naturalFinish(false,true),"auto-next only on a natural finish");
  check(PlaybackEndRules.ticksToMs(29_700_000_000L)==2_970_000L&&PlaybackEndRules.ticksToMs(0)==0,"ticks");
  String t=PlaybackEndRules.prematureText(763000,full);check(t.contains("12:43")&&t.contains("49:30")&&t.contains("提前结束"),"visible premature sentence keeps the position: "+t);
  String repair=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlaybackRepair.java"));
  String auto=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/AutoNext.java"));
  String overlay=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerOverlay.java"));
  check(!repair.contains("本集播放完毕")&&repair.contains("getPlayerState")&&repair.contains("prematureFlag"),"repair marks EndReached from player state");
  check(auto.contains("naturalFinish")&&auto.contains("PlaybackRepair.premature"),"auto-next consults the flag");
  check(overlay.contains("PlaybackRepair.premature")&&overlay.contains("failure(rendered,terminal,message,premature)")&&overlay.contains("nativeTick(a,failure,"),"failure card and Emby recovery take the flag");
  check(PlaybackEndRules.mmss(3723000).equals("1:02:03"),"hours");
  check(PlaybackEndRules.playingWithoutVout(false,false,true,1920,5000),"playing with a sized video track counts as rendered");
  check(!PlaybackEndRules.playingWithoutVout(false,false,true,0,5000)&&!PlaybackEndRules.playingWithoutVout(false,false,false,1920,5000)&&!PlaybackEndRules.playingWithoutVout(true,false,true,1920,5000)&&!PlaybackEndRules.playingWithoutVout(false,true,true,1920,5000),"no false render");
  check(PlaybackEndRules.imageSubtitle("PGSSUB","Chinese")&&PlaybackEndRules.imageSubtitle("","Chinese (默认 PGSSUB) · 内嵌")&&PlaybackEndRules.imageSubtitle("hdmv_pgs_subtitle","")&&PlaybackEndRules.imageSubtitle("dvd_subtitle","")&&PlaybackEndRules.imageSubtitle("","movie.zh.sup"),"image subtitles");
  check(!PlaybackEndRules.imageSubtitle("subrip","English (SUBRIP)")&&!PlaybackEndRules.imageSubtitle("ass","")&&!PlaybackEndRules.imageSubtitle("","Super 字幕组"),"text subtitles stay text");
  check(PlaybackEndRules.pathPattern("/Users/0123456789abcdef0123456789abcdef/HomeSections?api_key=secret").equals("/Users/{id}/HomeSections"),"no ids or query in logs");
  check(PlaybackEndRules.pathPattern("/Items/12345/PlaybackInfo?UserId=x").equals("/Items/{n}/PlaybackInfo"),"numeric ids");
  check(PlaybackEndRules.resetLegacyDecode("software",false)&&!PlaybackEndRules.resetLegacyDecode("software",true)&&!PlaybackEndRules.resetLegacyDecode("hardware",false),"legacy sticky software decode resets once");
  String[] back=PlaybackEndRules.unwindDecodeSession(true,"auto","");check(back!=null&&"auto".equals(back[0])&&back[1].isEmpty(),"killed session restores the previous decode mode");
  check(PlaybackEndRules.unwindDecodeSession(true,"","software")[0].isEmpty()&&"software".equals(PlaybackEndRules.unwindDecodeSession(true,"","software")[1]),"missing previous mode is an empty preference");
  check(PlaybackEndRules.unwindDecodeSession(false,"software","software")==null,"a finished session does not unwind on the next cold start");
  check(PlaybackEndRules.dropPremature("1|a","2|a")&&PlaybackEndRules.dropPremature("1|a","1|b")&&!PlaybackEndRules.dropPremature("1|a","1|a")&&!PlaybackEndRules.dropPremature(null,"1|a"),"a new attempt drops the premature flag; the first sight does not");
  String app=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/CinemaApp.java"));
  check(app.contains("unwindDecodeSession")&&app.contains("decodeSession.open"),"cold start clears a decode session left open by a killed player");
  check(overlay.contains("decodeSession.open")&&overlay.contains("rehydrateDecode")&&overlay.contains("isChangingConfigurations())return"),"per-play soft decode keeps a session marker and still skips restore across rotation");
  check(repair.contains("dropPremature")&&repair.contains("PREMATURE.remove")&&repair.contains("endReached")&&!repair.contains("本集播放完毕"),"attempt change clears PREMATURE without going back to status text");
  System.out.println("PASS: premature VLC end keeps resume, render without Vout, image subtitles to player, Emby log path pattern, per-playback decoder.");
 }
}
