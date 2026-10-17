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
  check(FullscreenRules.cycleHud("适应屏幕 · 完整画面，不裁切")==null&&FullscreenRules.cycleHud("填满屏幕 · 保持比例，裁切边缘")==null&&FullscreenRules.cycleHud("16:9")==null&&FullscreenRules.cycleHud("4:3")==null,"aspect cycle has no HUD");
  System.out.println("PASS: bottom speed button is the 倍速/播放速度 pair; aspect cycle is silent.");
 }
}
