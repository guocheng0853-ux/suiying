package com.guocheng.mediapocket;
public class ReviewPolishTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(PlayerOverlayRules.decoderDisplay("HW+").equals("硬解+")&&PlayerOverlayRules.decoderDisplay("HW").equals("硬解")&&PlayerOverlayRules.decoderDisplay("SW").equals("软解")&&PlayerOverlayRules.decoderDisplay("AUTO").equals("自动"),"decoder chip in Chinese");
  check(PlayerOverlayRules.decoderDisplay(PlayerOverlayRules.decoderLabel("software",false,false)).equals("软解"),"display over internal label");
  String g=PlayerOverlayRules.gestureGuide(15,2f,false);
  check(g.contains("15 秒")&&g.contains("2.0×")&&g.contains("恢复画面大小")&&g.contains("长按任意图标"),"guide reflects settings");
  check(PlayerOverlayRules.gestureGuide(10,3f,true).contains("播放 / 暂停"),"guide follows double-tap-center setting");
  System.out.println("PASS: decoder chip shows 硬解+/硬解/软解/自动; gesture guide follows seek seconds, hold speed and double-tap setting.");
 }
}
