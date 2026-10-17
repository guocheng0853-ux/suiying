package com.guocheng.mediapocket;
public class PlayerSwitchRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args){
  check(PlayerSwitchRules.seekSeconds(10)==15&&PlayerSwitchRules.seekSeconds(30)==30&&PlayerSwitchRules.seekSeconds(60)==60,"seek steps are 15, 30 or 60");
  check(PlayerSwitchRules.seekMs(30)==30000,"seek milliseconds");
  check(PlayerGestureRules.tapDelta(PlayerGestureRules.RIGHT,15000)==15000&&PlayerGestureRules.tapDelta(PlayerGestureRules.LEFT,60000)==-60000,"gesture uses the configured step");
  check(PlayerGestureRules.burstDelta(PlayerGestureRules.RIGHT,PlayerGestureRules.RIGHT,15000,100,15000)==30000,"configured taps accumulate");
  check(PlayerGestureRules.tapDelta(PlayerGestureRules.LEFT)==-10000,"legacy ten second taps stay for existing gestures");
  int[] w={1920,1920,1280},h={1080,1080,720};float[] r={60f,120f,90f};
  check(PlayerSwitchRules.fastestMode(w,h,r,1920,1080,60f)==1,"same resolution picks the higher refresh");
  check(PlayerSwitchRules.fastestMode(w,h,r,1920,1080,120f)==-1,"already at the highest rate");
  check(PlayerSwitchRules.buffer(512*1024f).equals("缓冲 512 KB/s")&&PlayerSwitchRules.buffer(-1f).isEmpty(),"buffer speed label");
  check(PlayerSwitchRules.seekLabel(15).contains("15"),"seek label");
  System.out.println("PASS: high refresh choice, buffer label, seek seconds.");
 }
}
