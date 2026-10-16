package com.guocheng.mediapocket;
public class PlaybackSpeedTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(PlaybackPrefs.SPEEDS.length==6&&PlaybackPrefs.SPEEDS[0]==0.5f&&PlaybackPrefs.SPEEDS[5]==2f,"list");
  check(PlaybackPrefs.nextSpeed(1f)==1.25f&&PlaybackPrefs.nextSpeed(2f)==0.5f&&PlaybackPrefs.nextSpeed(0.5f)==0.75f,"cycle");
  check(PlaybackPrefs.normalizeSpeed(Float.NaN)==1f&&PlaybackPrefs.normalizeSpeed(-1)==1f&&PlaybackPrefs.normalizeSpeed(3f)==2f&&PlaybackPrefs.normalizeSpeed(1.3f)==1.25f,"normalize");
  check(PlaybackPrefs.speedLabel(1f).equals("1×")&&PlaybackPrefs.speedLabel(1.25f).equals("1.25×")&&PlaybackPrefs.speedLabel(2f).equals("2×"),"labels");
  System.out.println("PASS: speed list 0.5–2, cycle wrap, normalize, labels.");}
}
