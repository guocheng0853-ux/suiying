package com.guocheng.mediapocket;
import java.util.*;
public final class PlayerGlassTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  TimeZone z=TimeZone.getTimeZone("Asia/Shanghai");Calendar c=Calendar.getInstance(z);c.clear();c.set(2026,9,24,22,13,0);long now=c.getTimeInMillis();
  String r=PlayerGlassRules.remaining(23*60_000L+14_000,52*60_000L+8_000,1f,now,z);
  check(r.equals("剩 29 分钟，22:41 看完"),"remaining line: "+r);
  check(PlayerGlassRules.remaining(0,90*60_000L,1f,now,z).startsWith("剩 1 小时 30 分钟，23:43"),"hours: "+PlayerGlassRules.remaining(0,90*60_000L,1f,now,z));
  check(PlayerGlassRules.remaining(0,60*60_000L,2f,now,z).startsWith("剩 30 分钟"),"speed shortens remaining");
  check(PlayerGlassRules.remaining(59_000,60_000,1f,now,z).equals("马上看完"),"under a minute");
  check(PlayerGlassRules.remaining(0,0,1f,now,z).isEmpty()&&PlayerGlassRules.remaining(5,1,1f,now,z).isEmpty(),"unknown duration");
  System.out.println("PASS: player glass remaining-time line (minutes rounded up, finish clock, speed, hours, unknown duration).");
 }
}
