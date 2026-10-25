package com.guocheng.mediapocket;
public final class PlaybackFailureRulesTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args){
  check(PlaybackFailureRules.width(320,0,0)==272,"compact phone keeps 24dp margins");
  check(PlaybackFailureRules.width(1143,0,0)==520,"tablet recovery panel does not stretch across the screen");
  check(PlaybackFailureRules.width(360,56,0)==256,"cutout width is excluded");
  check(PlaybackFailureRules.bottom(808,0,24,0,180)==48,"panel sits above the gesture area");
  check(PlaybackFailureRules.bottom(808,0,24,140,180)==200,"visible controls remain below the panel");
  int b=PlaybackFailureRules.bottom(320,24,0,180,180);check(320-b-180>=48,"short landscape keeps panel below the top safe region");
  check(PlaybackFailureRules.width(0,0,0)==0&&PlaybackFailureRules.TOUCH>=48,"zero-width layout and Android touch floor");
  check(!PlaybackFailureRules.hint().contains("密码")&&!PlaybackFailureRules.hint().contains("解码"),"recovery copy does not invent the cause");
  System.out.println("PASS: responsive recovery-panel bounds, safe insets, controls and short landscape.");
 }
}
