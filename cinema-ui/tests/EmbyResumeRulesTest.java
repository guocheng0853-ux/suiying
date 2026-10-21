package com.guocheng.mediapocket;
public final class EmbyResumeRulesTest {
 public static void main(String[] args){
  check(EmbyResumeRules.keep(true,"ep1","ep2",1800000)==-1,"next episode must not inherit previous end position");
  check(EmbyResumeRules.keep(true,"ep2","ep1",1800000)==-1,"previous episode must not inherit current progress");
  check(EmbyResumeRules.keep(true,"ep1","ep1",123456)==123456,"quality switch/reconnect preserves progress");
  check(EmbyResumeRules.keep(false,"ep1","ep1",123456)==-1,"fresh open uses server progress");
  check(EmbyResumeRules.keep(true,"","ep1",123456)==-1,"unknown item does not leak progress");
  System.out.println("PASS: episode changes use target progress; same-item replay preserves position.");
 }
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
