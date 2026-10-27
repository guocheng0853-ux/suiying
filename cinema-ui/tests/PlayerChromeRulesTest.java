package com.guocheng.mediapocket;
public class PlayerChromeRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  check(PlayerChromeRules.legacySpeed("倍速","播放速度"),"base bottom button shows 倍速 but describes 播放速度");
  check(PlayerChromeRules.legacySpeed("播放速度","播放速度"),"unremapped base label");
  check(PlayerChromeRules.legacySpeed("1.25×","倍速"),"description still identifies a relabeled speed button");
  check(!PlayerChromeRules.legacySpeed("比例","画面比例"),"aspect button stays");
  check(!PlayerChromeRules.legacySpeed("字幕","字幕"),"subtitle button stays");
  check(!PlayerChromeRules.legacySpeed("播放器","播放器"),"player menu stays");
  check(!PlayerChromeRules.legacySpeed(null,null),"empty is not speed");
  check("画面比例 · 适应屏幕".equals(FullscreenRules.cycleHud("适应屏幕 · 完整画面，不裁切"))&&"画面比例 · 4:3".equals(FullscreenRules.cycleHud("4:3")),"aspect controls announce the selected mode concisely");
  check(PlayerLayoutMetrics.topY(1f,0,2f)>PlayerLayoutMetrics.topY(1f,0,1f)&&PlayerLayoutMetrics.statusH(1f,2f)>=36,"large text reserves height above the top controls");
  System.out.println("PASS: bottom speed button is the 倍速/播放速度 pair; aspect feedback is concise; status row accommodates large text.");
 }
}
