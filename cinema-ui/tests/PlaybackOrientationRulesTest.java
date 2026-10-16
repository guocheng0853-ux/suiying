package com.guocheng.mediapocket;
public class PlaybackOrientationRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  check(PlaybackOrientation.playback()==6,"entering playback requests sensor landscape");
  check(PlaybackOrientation.remember(-1)==-1,"unspecified host orientation is restored");
  check(PlaybackOrientation.remember(6)==-1,"recreated player does not treat sensor landscape as the app orientation");
  check(PlaybackOrientation.remember(1)==1&&PlaybackOrientation.remember(0)==0,"explicit portrait or landscape requests are remembered");
  check(PlaybackOrientation.restoreOnPause(true)&&!PlaybackOrientation.restoreOnPause(false),"restore only when the player is finishing");
  check(PlaybackOrientation.duringPip()==-1,"picture-in-picture drops the landscape lock");
  check(PlaybackOrientation.afterPip(6)==6&&PlaybackOrientation.afterPip(1)==1,"leaving picture-in-picture restores the playback request, including rotate");
  check(PlaybackOrientation.hostShouldRelease(6)&&PlaybackOrientation.hostShouldRelease(0)&&PlaybackOrientation.hostShouldRelease(1),"hosts clear landscape locks the player may have applied");
  check(!PlaybackOrientation.hostShouldRelease(-1)&&!PlaybackOrientation.hostShouldRelease(4)&&!PlaybackOrientation.hostShouldRelease(13),"hosts keep unspecified, sensor, and user orientation");
  System.out.println("PASS: playback enters sensor landscape, restores the previous request on close, releases the lock for picture-in-picture, and does not reset the rest of the app.");
 }
}
