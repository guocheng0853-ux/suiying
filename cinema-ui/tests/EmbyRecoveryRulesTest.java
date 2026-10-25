package com.guocheng.mediapocket;
public class EmbyRecoveryRulesTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  check(EmbyRecoveryRules.eligible(true,true,true,true,false,0,"系统解码错误 1 / -1"),"unknown terminal connection failure gets one fresh PlaybackInfo");
  check(!EmbyRecoveryRules.eligible(true,true,true,true,false,1,"超时"),"refreshed activity cannot start a retry loop");
  check(!EmbyRecoveryRules.eligible(false,true,true,true,false,0,"超时"),"auto-switch off is respected");
  check(!EmbyRecoveryRules.eligible(true,false,true,true,false,0,"超时"),"non-Emby sources do not refresh credentials");
  check(!EmbyRecoveryRules.eligible(true,true,false,true,false,0,"播放结束"),"normal completion never refreshes");
  check(!EmbyRecoveryRules.eligible(true,true,true,false,false,0,"超时"),"background player never launches a recovery");
  check(!EmbyRecoveryRules.eligible(true,true,true,true,true,0,"超时"),"menu and source selection take priority");
  for(String raw:new String[]{"HTTP 401","HTTP 403","HTTP 409","密码错误","没有权限","解码器不支持","unsupported format"})check(!EmbyRecoveryRules.eligible(true,true,true,true,false,0,raw),"permanent/user-action failure: "+raw);
  check(EmbyRecoveryRules.carry(true,"same","same",1)==1,"same-film replacement preserves the one-shot budget");
  check(EmbyRecoveryRules.carry(true,"old","next",1)==0,"new episode gets a new budget");
  check(EmbyRecoveryRules.carry(false,"same","same",1)==0,"new user playback starts a fresh chain");
  check(EmbyRecoveryRules.resume(0,123456,10000)==123456,"stopped player keeps the last known position");
  check(EmbyRecoveryRules.resume(20000,123456,10000)==20000,"live position wins over retained state");
  check(EmbyRecoveryRules.resume(-1,0,10000)==10000,"startup failure retains original resume");
  System.out.println("PASS: one-shot recovery, user settings, background/menu guards, permanent failures and handoff budget.");
 }
}
