package com.guocheng.mediapocket;
public class PlayerGestureRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(PlayerGestureRules.third(10,900)==PlayerGestureRules.LEFT&&PlayerGestureRules.third(450,900)==PlayerGestureRules.CENTER&&PlayerGestureRules.third(890,900)==PlayerGestureRules.RIGHT,"thirds");
  check(PlayerGestureRules.tapDelta(PlayerGestureRules.LEFT)==-10000&&PlayerGestureRules.tapDelta(PlayerGestureRules.RIGHT)==10000&&PlayerGestureRules.tapDelta(PlayerGestureRules.CENTER)==0,"tap delta");
  check(PlayerGestureRules.tapHint(-10000).equals("-10s")&&PlayerGestureRules.tapHint(10000).equals("+10s"),"tap hint");
  check(PlayerGestureRules.brightnessSide(100,1000)&&!PlayerGestureRules.brightnessSide(600,1000),"halves");
  check(PlayerGestureRules.axis(10,2,24)==PlayerGestureRules.NONE&&PlayerGestureRules.axis(60,10,24)==PlayerGestureRules.HORIZONTAL&&PlayerGestureRules.axis(5,-60,24)==PlayerGestureRules.VERTICAL&&PlayerGestureRules.axis(50,48,24)==PlayerGestureRules.NONE,"axis lock");
  check(PlayerGestureRules.seekSpan(45*60000)==675000&&PlayerGestureRules.seekSpan(30000)==7500&&PlayerGestureRules.seekSpan(10*3600000)==9000000&&PlayerGestureRules.seekSpan(0)==0,"span is a quarter of the duration");
  int dur=45*60000;check(PlayerGestureRules.seekTarget(600000,dur,500,1000)==937500,"half width is half of a quarter");check(PlayerGestureRules.seekTarget(1000,dur,-1000,1000)==0,"clamp start");check(PlayerGestureRules.seekTarget(dur-2000,dur,1000,1000)==dur-1000,"clamp end");
  check(PlayerGestureRules.time(3725000).equals("1:02:05")&&PlayerGestureRules.time(65000).equals("1:05"),"time");check(PlayerGestureRules.seekLabel(65000,0,dur).startsWith("⏩ +1:05")&&PlayerGestureRules.seekLabel(0,10000,dur).startsWith("⏪ -0:10"),"label");
  check(PlayerGestureRules.brightness(0.9f,0.5f)==1f&&PlayerGestureRules.brightness(0.1f,-1f)==0.01f&&PlayerGestureRules.volume(5,15,0.5f)==13&&PlayerGestureRules.volume(5,15,-1f)==0,"brightness/volume");
  check(LevelIconGeometry.volume(0)==LevelIconGeometry.MUTE&&LevelIconGeometry.volume(33)==LevelIconGeometry.LOW&&LevelIconGeometry.volume(40)==LevelIconGeometry.MID&&LevelIconGeometry.volume(66)==LevelIconGeometry.MID&&LevelIconGeometry.volume(70)==LevelIconGeometry.HIGH&&LevelIconGeometry.volume(160)==LevelIconGeometry.HIGH,"volume icon steps");
  check(LevelHudMetrics.x(1000,200,1f,80,0)>LevelHudMetrics.x(1000,200,1f,0,0)&&LevelHudMetrics.top(1f,40,true)>LevelHudMetrics.top(1f,40,false),"hud stays inside the cutout");
  System.out.println("PASS: gesture thirds, halves, axis lock, seek span/target clamp, labels, brightness/volume clamp, level icon and safe-area placement.");}
}
