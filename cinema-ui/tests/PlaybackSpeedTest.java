package com.guocheng.mediapocket;
public class PlaybackSpeedTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(PlaybackPrefs.SPEEDS.length==6&&PlaybackPrefs.SPEEDS[0]==0.5f&&PlaybackPrefs.SPEEDS[5]==2f,"list");
  check(PlaybackPrefs.nextSpeed(1f)==1.25f&&PlaybackPrefs.nextSpeed(2f)==0.5f&&PlaybackPrefs.nextSpeed(0.5f)==0.75f,"cycle");
  check(PlaybackPrefs.normalizeSpeed(Float.NaN)==1f&&PlaybackPrefs.normalizeSpeed(-1)==1f&&PlaybackPrefs.normalizeSpeed(3f)==2f&&PlaybackPrefs.normalizeSpeed(1.3f)==1.25f,"normalize");
  check(PlaybackPrefs.speedLabel(1f).equals("1×")&&PlaybackPrefs.speedLabel(1.25f).equals("1.25×")&&PlaybackPrefs.speedLabel(2f).equals("2×"),"labels");
  float h=120f;
  check(PlaybackPrefs.speedFromTrack(0f,h)==2f&&PlaybackPrefs.indexFromTrack(0f,h)==5,"top of the bar is 2×");
  check(PlaybackPrefs.speedFromTrack(h-0.1f,h)==0.5f&&PlaybackPrefs.speedFromTrack(h,h)==0.5f,"bottom of the bar is 0.5×");
  check(PlaybackPrefs.speedFromTrack(-4f,h)==2f,"above the bar snaps to fastest");
  check(PlaybackPrefs.speedFromTrack(50f,h)==1.25f,"cell under the finger is 1.25×");
  check(PlaybackPrefs.speedFromTrack(60f,h)==1f&&PlaybackPrefs.speedFromTrack(70f,h)==1f,"1× occupies its own cell");
  check(PlaybackPrefs.indexFromTrack(0f,h)>PlaybackPrefs.indexFromTrack(h-1f,h),"higher on screen is a faster step");
  check(Float.isNaN(PlaybackPrefs.speedFromTrack(10f,0f))&&PlaybackPrefs.indexFromTrack(Float.NaN,h)==-1,"invalid track");
  System.out.println("PASS: speed list 0.5–2, cycle wrap, normalize, labels, vertical bar snaps higher=faster.");}
}
