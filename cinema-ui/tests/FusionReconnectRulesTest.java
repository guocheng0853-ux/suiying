package com.guocheng.mediapocket;
import java.util.Arrays;
import java.util.Collections;
public class FusionReconnectRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x){
  // 哪些片源归这里管
  check(FusionReconnectRules.route("http://c/config.json","site","线路一","https://v/1.m3u8",""),"fusion line with config, site, flag and value");
  check(!FusionReconnectRules.route("","site","线路一","v",""),"no config");
  check(!FusionReconnectRules.route("c","site","","v","")&&!FusionReconnectRules.route("c","site","f","",""),"flag and value are both needed to ask the site again");
  check(!FusionReconnectRules.route("c","site","f","magnet:?xt=urn:btih:abc","")&&!FusionReconnectRules.route("c","site","f"," MAGNET:?xt=1",""),"magnets never expire, they are not re-resolved");
  check(!FusionReconnectRules.route("c","site","f","v","emby-item"),"Emby keeps its own recovery");
  // 什么时候自动重连
  check(FusionReconnectRules.eligible(true,true,true,true,false,0),"failure halfway through");
  check(!FusionReconnectRules.eligible(false,true,true,true,false,0),"auto switch turned off");
  check(!FusionReconnectRules.eligible(true,false,true,true,false,0),"not a fusion line");
  check(!FusionReconnectRules.eligible(true,true,false,true,false,0),"no failure");
  check(!FusionReconnectRules.eligible(true,true,true,false,false,0),"player in the background");
  check(!FusionReconnectRules.eligible(true,true,true,true,true,0),"user has a panel or menu open");
  check(FusionReconnectRules.eligible(true,true,true,true,false,0),"startup failure may refresh stale episode details");
  long played=FusionReconnectRules.playedMs(0,20*60_000L);
  check(played==20*60_000L&&FusionReconnectRules.eligible(true,true,true,true,false,0),"resumePos stays 0 after a read failure, lastPos still makes it eligible and is the resume point");
  check(FusionReconnectRules.playedMs(90_000L,20_000L)==90_000L,"a stored resume ahead of lastPos is kept");
  check(FusionReconnectRules.holdOff(1_000,1_001)&&!FusionReconnectRules.holdOff(3_000,1_000+FusionReconnectRules.GRACE_MS),"grace window blocks the 250ms poll from starting again");
  check(!FusionReconnectRules.eligible(true,true,true,true,false,FusionReconnectRules.TOTAL_MAX),"per-episode cap");
  // 顺序：同线路两次，再换线路，最后放弃
  check(FusionReconnectRules.next(0,0,-1,0)==FusionReconnectRules.SAME&&FusionReconnectRules.next(1,0,-1,1)==FusionReconnectRules.SAME,"same line first, twice");
  check(FusionReconnectRules.next(2,0,-1,2)==FusionReconnectRules.ALT,"then look up other lines");
  check(FusionReconnectRules.next(2,0,0,2)==FusionReconnectRules.NONE,"site has no other line for this episode");
  check(FusionReconnectRules.next(2,1,2,3)==FusionReconnectRules.ALT&&FusionReconnectRules.next(2,2,2,4)==FusionReconnectRules.NONE,"each other line once");
  check(FusionReconnectRules.next(2,3,9,5)==FusionReconnectRules.NONE,"at most ALT_MAX other lines");
  check(FusionReconnectRules.next(0,0,-1,FusionReconnectRules.TOTAL_MAX)==FusionReconnectRules.NONE,"total cap wins");
  FusionPlaybackRules.Route old=new FusionPlaybackRules.Route("A","第2集","old");
  FusionPlaybackRules.Route refreshed=new FusionPlaybackRules.Route("A","第2集","fresh");
  FusionPlaybackRules.Route other=new FusionPlaybackRules.Route("B","第2集","fresh");
  FusionPlaybackRules.Route ambiguous=new FusionPlaybackRules.Route("A","第2集","another");
  FusionPlaybackRules.Route magnet=new FusionPlaybackRules.Route("C","第2集"," magnet:?xt=1");
  check(FusionReconnectRules.refreshSameRoute(old,Arrays.asList(other,refreshed))==refreshed,"retry uses the updated value for the same line");
  check(FusionReconnectRules.refreshSameRoute(old,Arrays.asList(refreshed,ambiguous))==old,"do not guess between ambiguous matches");
  check(FusionReconnectRules.refreshSameRoute(old,Collections.emptyList())==old,"retain the original when details are missing");
  check(FusionReconnectRules.alternatives(Arrays.asList(refreshed,other,other,magnet),"A").equals(Collections.singletonList(other)),"other flags may use the same value, exclude duplicates and magnets");
  FusionPlaybackRules.Route wrongSeason=new FusionPlaybackRules.Route("B","S03E02","wrong-season");
  FusionPlaybackRules.Route wrongEpisode=new FusionPlaybackRules.Route("A","第3集","wrong-episode");
  check(FusionReconnectRules.sameDetailRoutes(Arrays.asList(refreshed,wrongSeason,wrongEpisode),"S02E02","同一详情",true)
   .equals(Collections.singletonList(refreshed)),"allow an unnumbered season for this exact detail but reject the wrong season or episode");
  // 重连后稳定播放一段，计数清零
  check(!FusionReconnectRules.settled(600_000+29_999,600_000)&&FusionReconnectRules.settled(600_000+30_000,600_000),"30 s of progress after the attempt");
  check(!FusionReconnectRules.settled(10_000,600_000),"seeking back is not progress");
  check(!FusionReconnectRules.settled(900_000,-1),"no attempt yet");
  // 文案
  check(FusionReconnectRules.hint(FusionReconnectRules.SAME,1,"线路一").contains("1/2")&&FusionReconnectRules.hint(FusionReconnectRules.SAME,0,"").contains("1/2"),"same-line hint shows the attempt");
  check(FusionReconnectRules.hint(FusionReconnectRules.ALT,2,"量子").contains("「量子」")&&FusionReconnectRules.hint(FusionReconnectRules.ALT,2,"").contains("其他线路"),"other-line hint names the line");
  check(FusionReconnectRules.resumed(25*60_000L+7_000).contains("25:07"),"resume toast shows the position");
  System.out.println("PASS: fusion lines that fail halfway are re-resolved on the same line twice, then moved to up to three other lines, then handed back.");
 }
}
